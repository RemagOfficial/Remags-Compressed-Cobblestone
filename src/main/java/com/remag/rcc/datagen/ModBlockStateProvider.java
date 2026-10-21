package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.machine.PortType;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    // The one texture every level shares; the per-level darkness is applied at runtime by a block/item
    // colour handler reading the compression level, so a single model covers all levels.
    private static final ResourceLocation VANILLA_COBBLESTONE = ResourceLocation.withDefaultNamespace("block/cobblestone");
    // Root vanilla block model: carries the standard display transforms (isometric GUI,
    // hand/ground scaling). Without it a raw cube model renders oversized in hand and
    // flat/unrotated in inventory slots, since it would bake with no item transforms.
    private static final ResourceLocation VANILLA_BLOCK_PARENT = ResourceLocation.withDefaultNamespace("block/block");

    // Structure-block textures now live in the mod namespace (rcc:textures/block/<name>.png).
    private static final String FRAME_TEX = "block/compression_frame";
    private static final String GLASS_TEX = "block/compression_glass";
    private static final String PORT_POWER_TEX = "block/compression_port_power";
    private static final String PORT_ITEM_IN_TEX = "block/compression_port_item_in";
    private static final String PORT_ITEM_OUT_TEX = "block/compression_port_item_out";
    private static final String INTERFACE_TEX = "block/compression_interface";

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RCC.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        tintedCubeWithItem(ModBlocks.COMPRESSED_COBBLESTONE.get());
        simpleCubeWithItem(ModBlocks.FRAME.get(), "compression_frame", FRAME_TEX);
        simpleCubeWithItem(ModBlocks.GLASS.get(), "compression_glass", GLASS_TEX);
        simpleCubeWithItem(ModBlocks.INTERFACE.get(), "compression_interface", INTERFACE_TEX);
        portBlock();
        coreBlock();
    }

    private void tintedCubeWithItem(Block block) {
        // Faces carry tintindex 0 (not a baked colour) so the runtime BlockColor/ItemColor handlers
        // (see ClientEvents) multiply every quad by tintArgb(level). Without a tint index the quads
        // default to NO_TINT (-1) and the handler's colour is ignored on the block/item (only particles
        // tint, since they query the block colour unconditionally).
        BlockModelBuilder model = models().getBuilder("compressed_cobblestone")
                .parent(models().getExistingFile(VANILLA_BLOCK_PARENT))
                .texture("cobble", VANILLA_COBBLESTONE)
                .texture("particle", VANILLA_COBBLESTONE)
                .element()
                    .from(0f, 0f, 0f)
                    .to(16f, 16f, 16f)
                    .allFaces((side, face) -> face.texture("#cobble").cullface(side).tintindex(0))
                .end();
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }

    /** A plain cube_all model in a mod texture plus its item, for the non-variant structure blocks. */
    private void simpleCubeWithItem(Block block, String name, String texture) {
        ModelFile model = models().cubeAll(name, modLoc(texture));
        simpleBlockWithItem(block, model);
    }

    /** One block, three visual variants chosen by the {@code port_type} state. */
    private void portBlock() {
        ModelFile power = models().cubeAll("compression_port_power", modLoc(PORT_POWER_TEX));
        ModelFile itemIn = models().cubeAll("compression_port_item_in", modLoc(PORT_ITEM_IN_TEX));
        ModelFile itemOut = models().cubeAll("compression_port_item_out", modLoc(PORT_ITEM_OUT_TEX));
        getVariantBuilder(ModBlocks.PORT.get()).forAllStates(state -> {
            ModelFile model = switch (state.getValue(CompressionPortBlock.PORT_TYPE)) {
                case POWER -> power;
                case ITEM_IN -> itemIn;
                case ITEM_OUT -> itemOut;
            };
            return new ConfiguredModel[]{new ConfiguredModel(model)};
        });
        // Representative item model (the power variant); port kind is chosen from the creative tab.
        itemModels().getBuilder("compression_port").parent(power);
    }

    /** The core has no formed/idle variant; it always renders the hand-authored model. */
    private void coreBlock() {
        ModelFile core = models().getExistingFile(modLoc("block/compression_core"));
        simpleBlock(ModBlocks.CORE.get(), core);
        itemModels().getBuilder("compression_core").parent(core);
    }
}
