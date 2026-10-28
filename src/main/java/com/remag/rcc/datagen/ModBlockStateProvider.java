package com.remag.rcc.datagen;

import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BcePipeMode;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.machine.GeneratorTier;
import com.remag.rcc.machine.PortType;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    private static final String COBBLE_TEX = "block/compressed_cobblestone";
    private static final ResourceLocation VANILLA_BLOCK_PARENT = ResourceLocation.withDefaultNamespace("block/block");

    private static final String FRAME_TEX = "block/compression_frame";
    private static final String GLASS_TEX = "block/compression_glass";
    private static final String PORT_POWER_TEX = "block/compression_port_power";
    private static final String PORT_ITEM_IN_TEX = "block/compression_port_item_in";
    private static final String PORT_ITEM_OUT_TEX = "block/compression_port_item_out";
    private static final String PORT_SEED_TEX = "block/compression_port_seed";
    private static final String INTERFACE_TEX = "block/compression_interface";

    
    private static final String PIPE_MARKER = "0";

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RCC.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        tintedCubeWithItem(ModBlocks.COMPRESSED_COBBLESTONE.get());
        simpleCubeWithItem(ModBlocks.FRAME.get(), "compression_frame", FRAME_TEX);
        simpleCubeWithItem(ModBlocks.GLASS.get(), "compression_glass", GLASS_TEX);
        machineFrontWithItem(ModBlocks.INTERFACE.get(), "compression_interface");
        autoCompressorWithItem();
        autoDecompressorWithItem();
        compressionEditorWithItem();
        portBlock();
        coreBlock();
        decompressorCoreBlock();
        generatorCoreBlock();
        coreBlock(ModBlocks.COMPRESSION_CORE_2.get(), "compression_core_2");
        coreBlock(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get(), "compression_decompressor_core_2");
        coreBlock(ModBlocks.GENERATOR_CORE_2.get(), "generator_core_2");
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            int n = band.index();
            coreBlock(ModBlocks.COMPRESSION_CORES_BY_BAND.get(band).get(), "compression_core_" + n);
            coreBlock(ModBlocks.GENERATOR_CORES_BY_BAND.get(band).get(), "generator_core_" + n);
            if (band.hasDecompressor()) {
                coreBlock(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_3.get(), "compression_decompressor_core_3");
            }
        }
        bcePipe();
        paneledCubeWithItem(ModBlocks.BCE_STORAGE.get(), "bce_storage");
        paneledCubeWithItem(ModBlocks.BCE_STORAGE_2.get(), "bce_storage_2");
        paneledCubeWithItem(ModBlocks.BCE_STORAGE_3.get(), "bce_storage_3");
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 3) {
                continue;
            }
            paneledCubeWithItem(ModBlocks.BCE_STORAGES_BY_BAND.get(band).get(),
                    "bce_storage_" + (band.index() + 1));
        }
        paneledCubeWithItem(ModBlocks.CREATIVE_BCE_SOURCE.get(), "creative_bce_source");
        cobblestoneGenerator();
        wrenchItem();
    }

    
    private void wrenchItem() {
        itemModels().basicItem(ModItems.WRENCH.get());
    }

    private void tintedCubeWithItem(Block block) {
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

    
    private void simpleCubeWithItem(Block block, String name, String texture) {
        ModelFile model = models().cubeAll(name, modLoc(texture));
        simpleBlockWithItem(block, model);
    }

    
    private void paneledCubeWithItem(Block block, String name) {
        machineFrontWithItem(block, name);
    }

    
    private void machineFrontWithItem(Block block, String name) {
        ResourceLocation frame = modLoc(FRAME_TEX);
        ModelFile model = models().orientable(name, frame, modLoc(INTERFACE_TEX), frame);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(model)
                .rotationY(horizontalRotationY(state.getValue(BlockStateProperties.HORIZONTAL_FACING)))
                .build());
        simpleBlockItem(block, model);
    }

    
    private static int horizontalRotationY(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    
    private static int[] sixWayRotation(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case UP -> new int[]{270, 0};
            case DOWN -> new int[]{90, 0};
            default -> new int[]{0, horizontalRotationY(facing)};
        };
    }

    
    private void autoCompressorWithItem() {
        ResourceLocation frame = modLoc(FRAME_TEX);
        ModelFile model = models().orientable("compression_auto", frame, modLoc(INTERFACE_TEX), frame);
        getVariantBuilder(ModBlocks.COMPRESSION_AUTO.get()).forAllStates(state -> {
            int rotationY = horizontalRotationY(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        });
        simpleBlockItem(ModBlocks.COMPRESSION_AUTO.get(), model);
    }

    
    private void autoDecompressorWithItem() {
        ResourceLocation frame = modLoc(FRAME_TEX);
        ModelFile model = models().orientable("compression_auto_decompressor", frame, modLoc(INTERFACE_TEX), frame);
        getVariantBuilder(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get()).forAllStates(state -> {
            int rotationY = horizontalRotationY(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        });
        simpleBlockItem(ModBlocks.COMPRESSION_AUTO_DECOMPRESSOR.get(), model);
    }

    
    private void compressionEditorWithItem() {
        ResourceLocation frame = modLoc(FRAME_TEX);
        ModelFile model = models().orientable("compression_editor", frame, modLoc(INTERFACE_TEX), frame);
        getVariantBuilder(ModBlocks.COMPRESSION_EDITOR.get()).forAllStates(state -> {
            int rotationY = horizontalRotationY(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        });
        simpleBlockItem(ModBlocks.COMPRESSION_EDITOR.get(), model);
    }

    
    private void portBlock() {
        ResourceLocation frame = modLoc(FRAME_TEX);
        ModelFile power = portFace("compression_port_power", PORT_POWER_TEX, frame);
        ModelFile itemIn = portFace("compression_port_item_in", PORT_ITEM_IN_TEX, frame);
        ModelFile itemOut = portFace("compression_port_item_out", PORT_ITEM_OUT_TEX, frame);
        ModelFile seed = portFace("compression_port_seed", PORT_SEED_TEX, frame);
        getVariantBuilder(ModBlocks.PORT.get()).forAllStates(state -> portState(
                switch (state.getValue(CompressionPortBlock.PORT_TYPE)) {
                    case POWER -> power;
                    case ITEM_IN -> itemIn;
                    case ITEM_OUT -> itemOut;
                    case SEED -> seed;
                },
                state.getValue(BlockStateProperties.FACING)));
        itemModels().getBuilder("compression_port").parent(power);
    }

    
    private static ConfiguredModel[] portState(ModelFile model, net.minecraft.core.Direction facing) {
        int[] rotation = sixWayRotation(facing);
        return ConfiguredModel.builder().modelFile(model)
                .rotationX(rotation[0]).rotationY(rotation[1]).build();
    }

    
    private ModelFile portFace(String name, String faceTexture, ResourceLocation frame) {
        return models().orientable(name, frame, modLoc(faceTexture), frame);
    }

    
    private void coreBlock() {
        ModelFile core = models().getExistingFile(modLoc("block/compression_core"));
        simpleBlock(ModBlocks.CORE.get(), core);
        itemModels().getBuilder("compression_core").parent(core);
    }

    
    private void coreBlock(Block block, String name) {
        ModelFile core = models().getExistingFile(modLoc("block/compression_core"));
        simpleBlock(block, core);
        itemModels().getBuilder(name).parent(core);
    }

    
    private void decompressorCoreBlock() {
        ModelFile core = models().getExistingFile(modLoc("block/compression_core"));
        simpleBlock(ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get(), core);
        itemModels().getBuilder("compression_decompressor_core").parent(core);
    }

    
    private void generatorCoreBlock() {
        ModelFile core = models().getExistingFile(modLoc("block/compression_core"));
        simpleBlock(ModBlocks.GENERATOR_CORE.get(), core);
        itemModels().getBuilder("generator_core").parent(core);
    }

    
    private void bcePipe() {
        BcePipeBlock pipe = ModBlocks.BCE_PIPE.get();
        ModelFile core = pipeWrapper("bce_pipe_core", "block/bce_pipe_core_base", modLoc(FRAME_TEX));
        ModelFile extract = models().getExistingFile(modLoc("block/bce_pipe_arm_extract_base"));
        ModelFile insert = models().getExistingFile(modLoc("block/bce_pipe_arm_insert_base"));
        ModelFile passive = models().getExistingFile(modLoc("block/bce_pipe_arm_passive_base"));
        ModelFile run = models().getExistingFile(modLoc("block/bce_pipe_item_base"));

        MultiPartBlockStateBuilder parts = getMultipartBuilder(pipe);
        parts.part().modelFile(core).addModel().end();
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
                default -> 0;
            };
            for (BcePipeMode mode : new BcePipeMode[]{BcePipeMode.EXTRACT, BcePipeMode.INSERT, BcePipeMode.PASSIVE}) {
                ModelFile arm = switch (mode) {
                    case EXTRACT -> extract;
                    case INSERT -> insert;
                    case PASSIVE -> passive;
                    default -> throw new IllegalStateException(mode + " has no arm art");
                };
                parts.part().modelFile(arm)
                        .rotationX(rotationX).rotationY(rotationY).uvLock(false)
                        .addModel()
                        .condition(end, mode)
                        .end();
            }
        }

        itemModels().getBuilder("bce_pipe").parent(run);
    }

    
    private ModelFile pipeWrapper(String name, String baseModel, ResourceLocation texture) {
        return models().withExistingParent(name, modLoc(baseModel))
                .texture(PIPE_MARKER, texture)
                .texture("particle", texture);
    }

    
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
