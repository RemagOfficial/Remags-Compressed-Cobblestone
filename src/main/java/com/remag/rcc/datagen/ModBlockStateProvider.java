package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeMode;
import com.remag.rcc.machine.BcePipeTier;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.machine.PortType;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    // The one texture every level shares; the per-level colour is applied at runtime by a block/item colour handler
    // reading the compression level, so a single model covers all levels. It is the mod's own brightened cobblestone
    // sprite - deliberately <em>not</em> vanilla's - because a tint can only ever multiply a texture darker, and the
    // band ramp needs real headroom to reach white and the saturated hues (see CompressionConfig#TINT_BASE_GREY).
    private static final String COBBLE_TEX = "block/compressed_cobblestone";
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

    /**
     * The texture layer every hand-authored pipe base model names its metal. Blockbench exports a single-texture model
     * as layer {@code "0"} and bakes the faces against {@code #0}, so that is what the per-tier wrappers override - it
     * is the entire mechanism by which both tiers share one set of geometry. If a base model's metal layer is ever
     * renamed, this constant must follow it or the wrapper silently does nothing and every tier falls back to the
     * base's own baked texture (tier 1).
     */
    private static final String PIPE_MARKER = "0";

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RCC.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        tintedCubeWithItem(ModBlocks.COMPRESSED_COBBLESTONE.get());
        simpleCubeWithItem(ModBlocks.FRAME.get(), "compression_frame", FRAME_TEX);
        simpleCubeWithItem(ModBlocks.GLASS.get(), "compression_glass", GLASS_TEX);
        simpleCubeWithItem(ModBlocks.INTERFACE.get(), "compression_interface", INTERFACE_TEX);
        autoCompressorWithItem();
        autoDecompressorWithItem();
        portBlock();
        coreBlock();
        decompressorCoreBlock();
        generatorCoreBlock();
        bcePipe();
        simpleCubeWithItem(ModBlocks.BCE_STORAGE.get(), "bce_storage", INTERFACE_TEX);
        // Tier-two tank: same placeholder art for now (the size difference is a number, not a silhouette - dedicated
        // art follows the same deferral as the tank ladder's other assets).
        simpleCubeWithItem(ModBlocks.BCE_STORAGE_2.get(), "bce_storage_2", INTERFACE_TEX);
        cobblestoneGenerator();
        wrenchItem();
    }

    /**
     * The wrench is a pure item, so it needs a flat {@code item/generated} sprite rather than a block model - the one
     * piece of RCC art that is not a tinted cube. Its texture is the generated 16x16 icon under
     * {@code textures/item/wrench.png}; swap the PNG for better art and nothing else changes.
     */
    private void wrenchItem() {
        itemModels().basicItem(ModItems.WRENCH.get());
    }

    private void tintedCubeWithItem(Block block) {
        // Faces carry tintindex 0 (not a baked colour) so the runtime BlockColor/ItemColor handlers
        // (see ClientEvents) multiply every quad by tintArgb(level). Without a tint index the quads
        // default to NO_TINT (-1) and the handler's colour is ignored on the block/item (only particles
        // tint, since they query the block colour unconditionally).
        ResourceLocation cobble = modLoc(COBBLE_TEX);
        BlockModelBuilder model = models().getBuilder("compressed_cobblestone")
                .parent(models().getExistingFile(VANILLA_BLOCK_PARENT))
                .texture("cobble", cobble)
                .texture("particle", cobble)
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

    /**
     * Auto-Compressor model: the frame texture on every face except the {@code front}, which carries the interface
     * panel. Built from the vanilla {@code block/orientable} template (front + all other faces + top/bottom) and
     * mapped to the block's {@code HORIZONTAL_FACING} state with a Y rotation, so the front turns to face the way
     * the machine was placed.
     */
    private void autoCompressorWithItem() {
        ResourceLocation frame = modLoc(FRAME_TEX);
        ModelFile model = models().orientable("compression_auto", frame, modLoc(INTERFACE_TEX), frame);
        getVariantBuilder(ModBlocks.COMPRESSION_AUTO.get()).forAllStates(state -> {
            int rotationY = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0; // NORTH
            };
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        });
        simpleBlockItem(ModBlocks.COMPRESSION_AUTO.get(), model);
    }

    /**
     * Auto Decompressor model: identical art to the {@link #autoCompressorWithItem() Auto-Compressor} for now (frame
     * everywhere, interface panel on the {@code front}), built from the same {@code block/orientable} template and
     * keyed on {@code HORIZONTAL_FACING}. Dedicated art is deferred, but the block needs its own model name so the
     * generated asset paths do not collide.
     */
    private void autoDecompressorWithItem() {
        ResourceLocation frame = modLoc(FRAME_TEX);
        ModelFile model = models().orientable("compression_auto_decompressor", frame, modLoc(INTERFACE_TEX), frame);
        getVariantBuilder(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get()).forAllStates(state -> {
            int rotationY = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0; // NORTH
            };
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        });
        simpleBlockItem(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get(), model);
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

    /**
     * The Decompression Manipulator's core reuses the {@link #coreBlock() Compression Manipulator's hand-authored
     * core model} (the same anvil-like controller art) - it is the same kind of sealed centre block, so there is no
     * reason for a second model file. Only the generated blockstate and the item model name are new, so the asset
     * paths stay unique and {@code runData} never overwrites the hand-authored model.
     */
    private void decompressorCoreBlock() {
        ModelFile core = models().getExistingFile(modLoc("block/compression_core"));
        simpleBlock(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get(), core);
        itemModels().getBuilder("compression_decompressor_core").parent(core);
    }

    /**
     * The tier-1 Generation Manipulator's core reuses the {@link #coreBlock() hand-authored core model} like the
     * decompressor's does - it is the same kind of sealed centre block - so only the blockstate and the item model
     * name are new and {@code runData} never touches the shared model file.
     */
    private void generatorCoreBlock() {
        ModelFile core = models().getExistingFile(modLoc("block/compression_core"));
        simpleBlock(ModBlocks.GENERATOR_CORE.get(), core);
        itemModels().getBuilder("generator_core").parent(core);
    }

    /**
     * BCE pipe: a multipart model set, one arm per linked end, so the geometry now says everything the removed
     * runtime tint used to - {@code tier} picks the metal texture and each side's own {@code *_end} state picks both
     * whether that end exists and which cap it wears, so a single segment can show an extract mouth on one face and an
     * insert nozzle on another.
     *
     * <p>Exactly five files are authored by hand (hub, extract arm, insert arm, passive arm, inventory run) and they
     * live in {@code src/main/resources/assets/rcc/models/block/bce_pipe_*_base.json}, referenced through
     * {@link ModelFile.ExistingModelFile} so {@code runData} can never overwrite them - the same arrangement as
     * {@link #coreBlock()}. Everything generated here is a wrapper or a selector:
     *
     * <ul>
     *   <li>2 tiers x 5 bases = 10 wrapper models, each four lines, each just pointing the metal layer {@code 0} at
     *       its tier's texture - which is why per-tier art costs no geometry;</li>
     *   <li>one multipart block state: an unconditional hub per tier, plus 6 directions x 3 modes x 2 tiers of arms.
     *       Each arm is selected by the {@code *_end} property of the side it points along, so a side reading
     *       {@code off} contributes no part at all (that is what makes the end state double as the connection flag),
     *       and the mode selects which of the three caps gets drawn. The directions are the <em>same arm model
     *       rotated</em> (the arm bases are authored pointing north), and every arm is placed with {@code uvLock}
     *       <em>off</em> so the skin rotates together with the geometry and stays wrapped the same way on the tube in
     *       all six directions. This must stay off: {@code uvLock} on would lock the UVs to the world, which only
     *       happens to read right on the north/south arms (the directional gradient is 180-degree symmetric) and puts
     *       the east/west tops a quarter-turn out. So the combinations never become one file per combination.</li>
     * </ul>
     *
     * <p>The rotations below are the vanilla convention read straight off {@code blockstates/piston.json}: north is
     * rotation 0, the horizontals step by 90 in {@code y}, and the verticals use {@code x} 90 for down, 270 for up.
     * Only one axis is ever non-zero, so the x/y composition order cannot matter.
     */
    private void bcePipe() {
        BcePipeBlock pipe = ModBlocks.BCE_PIPE.get();
        BcePipeTier[] tiers = BcePipeTier.values();
        ModelFile[] core = new ModelFile[tiers.length];
        ModelFile[] extract = new ModelFile[tiers.length];
        ModelFile[] insert = new ModelFile[tiers.length];
        ModelFile[] passive = new ModelFile[tiers.length];
        ModelFile[] run = new ModelFile[tiers.length];
        for (int i = 0; i < tiers.length; i++) {
            String tierName = tiers[i].getSerializedName();
            ResourceLocation texture = modLoc("block/bce_pipe_" + tierName);
            core[i] = pipeWrapper("bce_pipe_core_" + tierName, "block/bce_pipe_core_base", texture);
            extract[i] = pipeWrapper("bce_pipe_arm_extract_" + tierName, "block/bce_pipe_arm_extract_base", texture);
            insert[i] = pipeWrapper("bce_pipe_arm_insert_" + tierName, "block/bce_pipe_arm_insert_base", texture);
            passive[i] = pipeWrapper("bce_pipe_arm_passive_" + tierName, "block/bce_pipe_arm_passive_base", texture);
            run[i] = pipeWrapper("bce_pipe_item_" + tierName, "block/bce_pipe_item_base", texture);
        }

        MultiPartBlockStateBuilder parts = getMultipartBuilder(pipe);
        for (int i = 0; i < tiers.length; i++) {
            parts.part().modelFile(core[i]).addModel()
                    .condition(BcePipeBlock.TIER, tiers[i])
                    .end();
        }
        for (Direction dir : Direction.values()) {
            EnumProperty<BcePipeMode> end = BcePipeBlock.END_BY_DIRECTION.get(dir);
            int rotationX = switch (dir) {
                case DOWN -> 90;
                case UP -> 270;
                default -> 0;
            };
            int rotationY = switch (dir) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0; // NORTH, the orientation the arm bases are authored in
            };
            // The three attached modes each have art; OFF means "no arm on this side", so it contributes no part.
            for (BcePipeMode mode : new BcePipeMode[]{BcePipeMode.EXTRACT, BcePipeMode.INSERT, BcePipeMode.PASSIVE}) {
                ModelFile[] arms = switch (mode) {
                    case EXTRACT -> extract;
                    case INSERT -> insert;
                    case PASSIVE -> passive;
                    default -> throw new IllegalStateException(mode + " has no arm art");
                };
                for (int i = 0; i < tiers.length; i++) {
                    // The arm model index and the tier index are the same array position, which is what ties one
                    // tier's skin to that tier's arm; the multipart condition then re-states the tie.
                    parts.part().modelFile(arms[i])
                            // uvLock(false): let the skin turn with the model so the tube wraps identically in every
                            // direction. Setting this true world-locks the UVs and mis-orients the east/west tops.
                            .rotationX(rotationX).rotationY(rotationY).uvLock(false)
                            .addModel()
                            .condition(end, mode)
                            .condition(BcePipeBlock.TIER, tiers[i])
                            .end();
                }
            }
        }

        // One item carries two tiers (the tier is a data component), and in 1.21.1 that component only influences
        // placement - it does not select the item model, unlike the placed block. So the inventory look is chosen by
        // an item-model override on the rcc:pipe_tier predicate, whose value is the tier ordinal and whose handler
        // lives in ClientEvents. The parent is tier 1, which is also what renders if nothing matches.
        ItemModelBuilder item = itemModels().getBuilder("bce_pipe").parent(run[0]);
        for (int i = 0; i < tiers.length; i++) {
            item.override().model(run[i])
                    .predicate(BcePipeBlock.TIER_ITEM_PROPERTY, (float) i)
                    .end();
        }
    }

    /** Re-parents a hand-authored pipe base model onto one tier's texture; the geometry is untouched. */
    private ModelFile pipeWrapper(String name, String baseModel, ResourceLocation texture) {
        // Override the metal layer (PIPE_MARKER) and the break-particle layer together, so a tier-2 pipe both skins
        // and shatters as tier 2 rather than falling back to the base's baked tier-1 texture.
        return models().withExistingParent(name, modLoc(baseModel))
                .texture(PIPE_MARKER, texture)
                .texture("particle", texture);
    }

    /**
     * Cobblestone generator: one block whose {@code tier} state spans the whole output ladder, but it reads as just
     * <em>two</em> looks keyed off {@link GeneratorTier#outputIsItems()} - the item rungs (the {@code BASE} drip and
     * anything still under the int ceiling) wear the frame metal, the BCE rungs wear the interface panel. That
     * visual split IS the handover the mechanic makes at the int limit, so a player can see which generators push
     * items and which push pipe-movable BCE, and it costs zero new art (both textures already exist). The state
     * builder walks every rung and picks its model by mode, so adding a tier to the enum needs no change here.
     *
     * <p>The single item shows the frame (item) look via a {@code block/cube} parent so it has correct GUI/hand
     * transforms; per-tier creative entries already exist and place the right rung through the {@code block_state}
     * component, so no item-model overrides are needed to reach a rung for testing.
     */
    private void cobblestoneGenerator() {
        ModelFile itemModel = models().cubeAll("cobblestone_generator_item", modLoc(FRAME_TEX));
        ModelFile bceModel = models().cubeAll("cobblestone_generator_bce", modLoc(INTERFACE_TEX));
        getVariantBuilder(ModBlocks.COBBLESTONE_GENERATOR.get()).forAllStates(state -> {
            GeneratorTier tier = state.getValue(CobblestoneGeneratorBlock.TIER);
            ModelFile model = tier.outputIsItems() ? itemModel : bceModel;
            return new ConfiguredModel[]{new ConfiguredModel(model)};
        });
        itemModels().getBuilder("cobblestone_generator")
                .parent(models().getExistingFile(VANILLA_BLOCK_PARENT))
                .texture("all", modLoc(FRAME_TEX))
                .element().from(0f, 0f, 0f).to(16f, 16f, 16f)
                .allFaces((side, face) -> face.texture("#all").cullface(side)).end();
    }
}
