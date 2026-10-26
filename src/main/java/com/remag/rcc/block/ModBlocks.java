package com.remag.rcc.block;

import com.remag.rcc.RCC;
import com.remag.rcc.machine.BcePipeBlock;
import com.remag.rcc.machine.BceStorageBlock;
import com.remag.rcc.machine.CobblestoneGeneratorBlock;
import com.remag.rcc.machine.CompressionCoreBlock;
import com.remag.rcc.machine.CompressionAutoBlock;
import com.remag.rcc.machine.CompressionAutoDecompressorBlock;
import com.remag.rcc.machine.DecompressorCoreBlock;
import com.remag.rcc.machine.GeneratorCoreBlock;
import com.remag.rcc.machine.CompressionFrameBlock;
import com.remag.rcc.machine.CompressionGlassBlock;
import com.remag.rcc.machine.CompressionInterfaceBlock;
import com.remag.rcc.machine.CompressionPortBlock;
import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RCC.MODID);

    /**
     * The only compressed cobblestone block. Its block entity stores the level, so one registration
     * covers every tier instead of one block per {@link com.remag.rcc.registry.CompressionConfig#MANUAL_MAX_LEVEL}.
     */
    public static final DeferredBlock<Block> COMPRESSED_COBBLESTONE =
            BLOCKS.register("compressed_cobblestone", CompressedCobblestoneBlock::new);

    // --- Compression Manipulator multiblock ----------------------------------------------------

    /** Solid edge / corner cube of the 5x5x5 shell. */
    public static final DeferredBlock<CompressionFrameBlock> FRAME =
            BLOCKS.register("compression_frame",
                    () -> new CompressionFrameBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    /** Glass-like face panel of the shell (non-occluding so the interior reads through). */
    public static final DeferredBlock<CompressionGlassBlock> GLASS =
            BLOCKS.register("compression_glass",
                    () -> new CompressionGlassBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.3F)
                            .noOcclusion()));

    /** Connection point; the {@code port_type} state selects power / item-in / item-out behaviour. */
    public static final DeferredBlock<CompressionPortBlock> PORT =
            BLOCKS.register("compression_port",
                    () -> new CompressionPortBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()));

    /** The controller at the exact centre of the structure. */
    public static final DeferredBlock<CompressionCoreBlock> CORE =
            BLOCKS.register("compression_core",
                    () -> new CompressionCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            // The core uses a non-cube (anvil) model. Without noOcclusion its full-cube block
                            // shape culls the faces of adjacent blocks (e.g. the ground beneath), leaving a void
                            // where the model doesn't fill the cell. Match the glass: don't occlude neighbours.
                            .noOcclusion()));

    /**
     * The band-2 Compression Manipulator's core: the same block class and BE type as {@link #CORE}, but carrying
     * {@link CompressionBand#BAND_2} so its structure scan reaches a 7x7x7 shell and its ceilings read 10^15..10^27.
     * This is the whole point of the band-driven engine - a new band is one registration plus a table row, no new code.
     */
    public static final DeferredBlock<CompressionCoreBlock> COMPRESSION_CORE_2 =
            BLOCKS.register("compression_core_2",
                    () -> new CompressionCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion(),
                            CompressionBand.BAND_2));

    /** Face panel that opens the machine GUI; the core itself is sealed and not directly clickable. */
    public static final DeferredBlock<CompressionInterfaceBlock> INTERFACE =
            BLOCKS.register("compression_interface",
                    () -> new CompressionInterfaceBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()));

    /**
     * The tier-1 Decompression Manipulator's controller: the centre of the 3x3x3 multiblock that steps a compressed
     * seed down toward a goal tier, banking the released BCE. Shares the shell blocks (frame / glass / port / interface)
     * with the Compression Manipulator. Like {@link #CORE} it uses the non-cube core model, so it must not occlude.
     */
    public static final DeferredBlock<DecompressorCoreBlock> COMPRESSION_DECOMPRESSOR_CORE =
            BLOCKS.register("compression_decompressor_core",
                    () -> new DecompressorCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    /** The band-2 Decompression Manipulator's core: the same class, a 5x5x5 (radius-2) descent shell from BAND_2. */
    public static final DeferredBlock<DecompressorCoreBlock> COMPRESSION_DECOMPRESSOR_CORE_2 =
            BLOCKS.register("compression_decompressor_core_2",
                    () -> new DecompressorCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion(),
                            CompressionBand.BAND_2));

    /**
     * The single-block Auto-Compressor: automates the manual tier ladder up to tier 1000. Solid machine block that
     * is directly interactive (it is its own GUI entry) and exposes fuel/output/energy capabilities on every face.
     */
    public static final DeferredBlock<CompressionAutoBlock> COMPRESSION_AUTO =
            BLOCKS.register("compression_auto",
                    () -> new CompressionAutoBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    /**
     * The single-block Auto Decompressor: the reverse of the Auto-Compressor. Steps a compressed seed down to a
     * player-selected goal tier, banking the released mass into a BCE buffer and ejecting the downgraded block.
     */
    public static final DeferredBlock<CompressionAutoDecompressorBlock> COMPRESSION_AUTO_DECOMPRESSOR =
            BLOCKS.register("compression_auto_decompressor",
                    () -> new CompressionAutoDecompressorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    // --- BCE logistics -----------------------------------------------------------------------

    /**
     * BCE conduit segment. The tier and each end's direction live in the block state ({@code tier}, plus one
     * {@code *_end} property per side - see {@link BcePipeBlock}) behind <em>one</em> item: the tier is crafted and
     * carried on the stack's {@code block_state} component, and each of the six ends is pointed afterwards by
     * right-clicking <em>that end</em> with a wrench. Connected segments form one
     * {@link com.remag.rcc.energy.BceNetwork} bus.
     *
     * <p>{@code noOcclusion} is required by the art, not a preference: a pipe is a hub plus arms inside its own cube,
     * so it must not cull the faces of the blocks around it.
     */
    public static final DeferredBlock<BcePipeBlock> BCE_PIPE =
            BLOCKS.register("bce_pipe",
                    () -> new BcePipeBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    /** A standalone BCE tank; passive storage whose flow is driven entirely by the pipes touching it. */
    public static final DeferredBlock<BceStorageBlock> BCE_STORAGE =
            BLOCKS.register("bce_storage",
                    () -> new BceStorageBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    /**
     * The tier-two BCE tank: the same passive block and block-entity type as {@link #BCE_STORAGE}, but sized to the
     * multiblock band - one full tier-{@link CompressionConfig#MANIPULATOR_MAX_OUT} block's BCE, which is the kind of
     * mass the tier-1 Decompression Manipulator's descents release and the tier-II pipes move.
     */
    public static final DeferredBlock<BceStorageBlock> BCE_STORAGE_2 =
            BLOCKS.register("bce_storage_2",
                    () -> new BceStorageBlock(BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                            CompressionConfig.BCE_STORAGE_2_CAPACITY_LOG));

    /**
     * The tier-three BCE tank: the same passive block and block-entity type as {@link #BCE_STORAGE_2}, sized to the
     * band-2 multiblock - one full band-2 ({@code 10^27}-class) block's BCE. This is the vault the band-2 machines
     * were missing: their mass outranges both the tier-I (band-0) and tier-II (band-1) tanks and the tier-II pipe.
     */
    public static final DeferredBlock<BceStorageBlock> BCE_STORAGE_3 =
            BLOCKS.register("bce_storage_3",
                    () -> new BceStorageBlock(BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5.0F, 6.0F)
                                    .requiresCorrectToolForDrops(),
                            CompressionConfig.BCE_STORAGE_3_CAPACITY_LOG));

    // --- Cobblestone generators (progression gate) -------------------------------------------

    /**
     * The cobblestone generator. One block spans the whole output ladder through its {@code tier} state (see
     * {@link com.remag.rcc.machine.GeneratorTier}): low rungs drip cobblestone items, high rungs mint raw BCE, all
     * reaching automation through the item / {@code rcc:bce} capabilities.
     */
    public static final DeferredBlock<CobblestoneGeneratorBlock> COBBLESTONE_GENERATOR =
            BLOCKS.register("cobblestone_generator",
                    () -> new CobblestoneGeneratorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()));

    /**
     * The tier-1 Generation Manipulator's controller: the sealed heart of the 3x3x4 generator tower that mints BCE
     * on the {@code 9^n} curve from one tier-1000 block per second up to half a tier-{@code 10^15} block, burning
     * escalating FE while it runs. The {@code rung} state carries the ladder position (see
     * {@link com.remag.rcc.machine.GeneratorCoreTier}); like the other cores it uses a non-cube model, so it must
     * not occlude.
     */
    public static final DeferredBlock<GeneratorCoreBlock> GENERATOR_CORE =
            BLOCKS.register("generator_core",
                    () -> new GeneratorCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));

    /**
     * The band-2 Generation Manipulator's core: the same block and BE type as {@link #GENERATOR_CORE}, carrying
     * {@link CompressionBand#BAND_2} so its rung ladder is drawn from that row's {@code rungExponents()} (10^15 ..
     * 5*10^26, thirteen rungs - the same count band 1's ladder carries) and its tower is 5x5x6. The rung index rides
     * the shared {@code rung} state exactly as band 1's does.
     */
    public static final DeferredBlock<GeneratorCoreBlock> GENERATOR_CORE_2 =
            BLOCKS.register("generator_core_2",
                    () -> new GeneratorCoreBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(8.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .noOcclusion(),
                            CompressionBand.BAND_2));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
