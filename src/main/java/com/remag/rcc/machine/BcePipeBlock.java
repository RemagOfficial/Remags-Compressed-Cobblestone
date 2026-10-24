package com.remag.rcc.machine;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.remag.rcc.RCC;
import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.BceNetwork;
import com.remag.rcc.registry.DisplayNames;
import com.remag.rcc.registry.ModBlockTags;
import com.remag.rcc.registry.ModItemTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A BCE conduit segment. One block covers every variant with seven state properties - {@code tier} (throughput,
 * see {@link BcePipeTier}) and one {@code *_end} state per side (see {@link BcePipeMode}) - following the same
 * single-block-multi-role pattern as {@link CompressionPortBlock}: the held stack's {@code block_state} component
 * selects what gets placed, so one registration replaces the whole tier ladder.
 *
 * <p>Unlike the tier, the {@code *_end} states are not bought at the crafting table - each of the six ends is set in
 * world by right-clicking <em>that end</em> with a wrench (any mod's, matched through the convention tag; see
 * {@link ModItemTags} and {@link #useItemOn}). A segment is therefore a small hub with individually re-pointable
 * faces, not a one-way device: the north end can drain a tank while the west end feeds a machine, and a line can be
 * re-pointed without breaking it.
 *
 * <p>Every block update around a pipe marks its network stale, which is the only topology invalidation the system
 * needs: placement and removal both fire neighbour updates on the six surrounding blocks. Sneak-right-clicking a pipe
 * prints what its run currently sees, which is how you tell "nothing to move" apart from "blocked by the
 * concentration floor".
 *
 * <p><b>Connection arms.</b> The block extends {@link PipeBlock} for its pre-built 64-entry shape table (a hub cube
 * plus one arm per connected side, derived from {@link #APOTHEM} alone) and overrides {@link #getAABBIndex} to read
 * that table off the six {@code *_end} properties instead of the boolean connections it normally uses - an end is
 * linked when it is anything other than {@link BcePipeMode#OFF}. Which ends exist is decided by
 * {@link #canConnectTo} and which cap each wears by that end's own mode, so the models are assembled by multipart
 * selectors keyed on {@code tier} (texture), the six end properties (arm presence and shape) - and the art never
 * multiplies into 8 x 64 files, with the tier and each direction read off the geometry rather than a runtime tint.
 *
 * <p>That same shape table is what makes an end <em>targetable</em>: {@link #targetedEnd} works out which arm the
 * player is pointing at from the hit position rather than the hit face, because the arms are the only parts of the
 * pipe that reach past the hub's {@link #APOTHEM} radius. Needed, not merely nicer - a store bolted to an end hides
 * that end's own face, and a wrench that trusted the face normal would re-point whatever machine was in the way.
 */
public class BcePipeBlock extends PipeBlock implements EntityBlock {
    public static final MapCodec<BcePipeBlock> CODEC = simpleCodec(BcePipeBlock::new);
    public static final EnumProperty<BcePipeTier> TIER = EnumProperty.create("tier", BcePipeTier.class);

    /**
     * What one side of the pipe currently is: {@code off} (nothing attachable there), {@code extract}, {@code insert}
     * or {@code passive} (attached but inert). One property per side, so a segment holds six independent directions
     * rather than one for the whole block - and {@code off} doubles as "no arm here", which is why there is no separate
     * connection boolean.
     */
    public static final EnumProperty<BcePipeMode> NORTH_END = EnumProperty.create("north_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> EAST_END = EnumProperty.create("east_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> SOUTH_END = EnumProperty.create("south_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> WEST_END = EnumProperty.create("west_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> UP_END = EnumProperty.create("up_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> DOWN_END = EnumProperty.create("down_end", BcePipeMode.class);

    /** The same six properties indexed by the side they describe, replacing {@code PipeBlock.PROPERTY_BY_DIRECTION}. */
    public static final Map<Direction, EnumProperty<BcePipeMode>> END_BY_DIRECTION =
            ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), ends -> {
                ends.put(Direction.NORTH, NORTH_END);
                ends.put(Direction.EAST, EAST_END);
                ends.put(Direction.SOUTH, SOUTH_END);
                ends.put(Direction.WEST, WEST_END);
                ends.put(Direction.UP, UP_END);
                ends.put(Direction.DOWN, DOWN_END);
            }));

    /**
     * The item-model override predicate that picks which tier's model the single {@code rcc:bce_pipe} item renders.
     * There is no block-state-driven item model in 1.21.1 (the {@code block_state} component only affects placement),
     * so the tiers need this: {@link com.remag.rcc.event.ClientEvents} registers it off the stack's component and
     * datagen writes the {@code overrides} that consume it.
     */
    public static final ResourceLocation TIER_ITEM_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "pipe_tier");

    /**
     * Half-width of the hub, in blocks. Everything about the pipe's size hangs off this one number: {@link PipeBlock}
     * builds the hub corner at {@code (0.5 - apothem) * 16} and the arms out to the block face, so the collider's
     * cross-section is {@code apothem * 16} voxels wide either side of centre. It has to equal the art, and the base
     * models are drawn as a {@code [5,5,5]} to {@code [11,11,11]} core - a 3-voxel half-width, so
     * {@code apothem = 3 / 16 = 0.1875}.
     *
     * <p>This is not cosmetic: it is what makes {@link #targetedEnd} work. A hub collider <em>wider</em> than the drawn
     * core (the old {@code 0.375}, a {@code [2..14]} box) swallows every click that visually lands on an arm - the ray
     * hits the fat invisible core first, so the coordinate never clears the radius and the wrench falls back to the
     * flat face normal. Matching the collider to the tube means the whole visible arm sticks out past the core and is
     * clickable along its length, the way clicking a connector reads in Pipez.
     */
    public static final float APOTHEM = 0.1875F;

    public BcePipeBlock(BlockBehaviour.Properties properties) {
        super(APOTHEM, properties);
        BlockState base = this.stateDefinition.any().setValue(TIER, BcePipeTier.TIER_1);
        for (EnumProperty<BcePipeMode> end : END_BY_DIRECTION.values()) {
            base = base.setValue(end, BcePipeMode.OFF);
        }
        this.registerDefaultState(base);
    }

    @Override
    public MapCodec<BcePipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER, NORTH_END, EAST_END, SOUTH_END, WEST_END, UP_END, DOWN_END);
    }

    /**
     * Place as the variant carried on the held stack's {@code block_state} component. Normally that is just a tier
     * (the creative-tab entries and every crafted pipe), so every side that can attach to something starts as a
     * passive end - present but inert until a wrench points it; a stack that names sides explicitly - a command, a
     * datapack, another mod's copying - places those.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BcePipeTier tier = BcePipeTier.TIER_1;
        BlockItemStateProperties stateProps = context.getItemInHand().get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            BcePipeTier fromItem = stateProps.get(TIER);
            if (fromItem != null) {
                tier = fromItem;
            }
        }
        BlockState placed = this.defaultBlockState().setValue(TIER, tier);
        for (Direction dir : Direction.values()) {
            EnumProperty<BcePipeMode> end = END_BY_DIRECTION.get(dir);
            placed = placed.setValue(end, endFor(context.getLevel(), context.getClickedPos(), dir,
                    stateProps == null ? null : stateProps.get(end)));
        }
        return placed;
    }

    /**
     * Whether this pipe bolts an arm to the block on the other side of {@code dir}. Only two things qualify, and both
     * are cases where BCE can genuinely move, so a drawn arm is never a lie:
     *
     * <ul>
     *   <li><b>another pipe</b>, at any tier and whatever either side's ends are configured to - the run is one
     *       {@link BceNetwork} bus and each extract end applies its own tier's slice, which is why the arm is
     *       symmetric even between mismatched tiers;</li>
     *   <li><b>a store</b>, listed by {@link ModBlockTags#BCE_CONNECTABLE} rather than by asking for the {@code BCE}
     *       capability, because a state update runs on both sides and must not depend on whether a neighbouring block
     *       entity has synced yet (see the tag's javadoc).</li>
     * </ul>
     *
     * <p>Note that this decides whether the two blocks are <em>joined</em>, not which way the BCE goes: each side
     * picks its own end state, so a link can be extract at one pipe and insert at the other without either one
     * refusing the connection.
     */
    public boolean canConnectTo(BlockGetter level, BlockPos pos, Direction dir) {
        BlockState neighbour = level.getBlockState(pos.relative(dir));
        return neighbour.getBlock() instanceof BcePipeBlock || neighbour.is(ModBlockTags.BCE_CONNECTABLE);
    }

    /**
     * The state one side should have: {@link BcePipeMode#OFF} with nothing attachable there, and otherwise the mode
     * already configured for that side - a brand-new link (or a stack that said nothing about it) starts as
     * {@link BcePipeMode#PASSIVE}, so a freshly placed run shows its arms but moves no BCE until a wrench turns an
     * end into extract or insert. An already-configured side keeps its direction, which is what makes swapping a
     * machine in one action leave the pipe's settings intact.
     */
    public BcePipeMode endFor(BlockGetter level, BlockPos pos, Direction dir, @Nullable BcePipeMode configured) {
        if (!canConnectTo(level, pos, dir)) {
            return BcePipeMode.OFF;
        }
        return configured != null && configured.isLinked() ? configured : BcePipeMode.PASSIVE;
    }

    /**
     * A block appeared or vanished on {@code facing}: re-evaluate just that one end. Fired on both sides by the same
     * {@code setBlock} call, which is what keeps the client's arms in step with the server's without any syncing.
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                    LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        EnumProperty<BcePipeMode> end = END_BY_DIRECTION.get(facing);
        return state.setValue(end, endFor(level, currentPos, facing, state.getValue(end)));
    }

    /**
     * {@link PipeBlock} builds its 64 shapes keyed off six connection booleans; a side here is connected when its end
     * is anything other than {@link BcePipeMode#OFF}, so the inherited hitbox table is reused with the bits read off
     * the end properties. Bit {@code i} is {@code Direction.values()[i]}, exactly how the superclass ordered it.
     */
    @Override
    protected int getAABBIndex(BlockState state) {
        int index = 0;
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            if (state.getValue(END_BY_DIRECTION.get(directions[i])).isLinked()) {
                index |= 1 << i;
            }
        }
        return index;
    }

    /**
     * Middle-click gives back the tier that was actually there (vanilla would hand back the default, tier 1) and
     * nothing else: the six end states stay off the stack, so a picked pipe still matches the upgrade recipe's exact
     * {@code block_state} input, exactly like a broken one
     * (see {@link BcePipeBlockEntity#collectImplicitComponents}).
     */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this.asItem());
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(TIER, state.getValue(TIER)));
        return stack;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BcePipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.BCE_PIPE.get(), BcePipeBlockEntity::serverTick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BcePipeBlockEntity pipe) {
            pipe.markNetworkStale();
        }
    }

    /**
     * Right-click with a wrench cycles <em>the end you are aiming at</em> through extract, insert and passive, in place,
     * with no block break. Which end that is comes from {@link #targetedEnd}, not from the face the ray happened to
     * strike. Only a linked end can be re-pointed: an {@code off} one has nothing attached, so there is no flow to
     * reverse and the click says so instead of silently arming a stub into the air.
     *
     * <p>This hook is the block phase of the interaction, which NeoForge fires <em>before</em> the held item gets its
     * own say, so a wrench from any mod reaches us here even when that mod's tool has no idea RCC exists - and
     * symmetrically, our wrench reaches their machines through the same tag. Everything happens server-side; the
     * client just swings its arm and waits for the block update to swap that end's model.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!ModItemTags.isWrench(stack)) {
            // Not a wrench: fall through to useWithoutItem (the sneak readout) and then to ordinary placement.
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            Direction side = targetedEnd(pos, hitResult);
            EnumProperty<BcePipeMode> end = END_BY_DIRECTION.get(side);
            BcePipeMode current = state.getValue(end);
            if (!current.isLinked()) {
                player.displayClientMessage(
                        Component.translatable("chat.rcc.bce_pipe.no_link", sideName(side)), false);
            } else {
                BcePipeMode mode = current.toggle();
                level.setBlockAndUpdate(pos, state.setValue(end, mode));
                // An end that changed direction is a different endpoint: the run has to rediscover its sources and sinks.
                if (level.getBlockEntity(pos) instanceof BcePipeBlockEntity pipe) {
                    pipe.markNetworkStale();
                }
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 1.0F);
                player.displayClientMessage(Component.translatable("chat.rcc.bce_pipe.mode",
                        DisplayNames.roman(state.getValue(TIER).number()), sideName(side), mode.displayName()), false);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** {@code "North"} / {@code "Up"} / ... for the click feedback; vanilla ships no translatable direction names. */
    private static Component sideName(Direction dir) {
        return Component.translatable("side.rcc." + dir.getSerializedName());
    }

    /**
     * Which of the pipe's six ends the player is pointing at, judged from <em>where on the block they aimed</em>
     * rather than from which face the ray struck.
     *
     * <p>{@link BlockHitResult#getDirection()} only reports the normal of the collision surface that was hit, which
     * makes an end unreachable exactly when it matters: with a store bolted to the north end, the only way to click
     * that arm at all is from the side or above, where its surface normal reads {@code east}/{@code up} - so a wrench
     * that trusted the normal would re-point the machine's own face or an unrelated end.
     *
     * <p>The collision shape carries the information the normal throws away. It is a hub cube of half-width
     * {@link #APOTHEM} unioned with one arm per linked end, each arm reaching from the hub out to the block face while
     * keeping the hub's cross-section - so <strong>only the arms</strong> contain points whose distance from the block
     * centre exceeds {@code APOTHEM} along any axis. Whichever of the three local coordinates clears that radius
     * therefore identifies the targeted arm; nothing can clear it twice, since a linked arm's perpendicular coordinates
     * stay within the hub's half-width.
     *
     * <p>A click that stays inside the hub targets no arm at all, and the face normal is used as it would be for any
     * plain block - which is also what happens for an {@code off} side, so the caller's no-link feedback still fires.
     */
    private Direction targetedEnd(BlockPos pos, BlockHitResult hit) {
        // Block-local, centred on the cube: each component spans -0.5 (the face) to +0.5 (the opposite face).
        Vec3 aim = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()).add(-0.5, -0.5, -0.5);
        double x = Math.abs(aim.x);
        double y = Math.abs(aim.y);
        double z = Math.abs(aim.z);
        if (x >= y && x >= z) {
            return x > APOTHEM ? (aim.x > 0 ? Direction.EAST : Direction.WEST) : hit.getDirection();
        }
        if (y >= z) {
            return y > APOTHEM ? (aim.y > 0 ? Direction.UP : Direction.DOWN) : hit.getDirection();
        }
        return z > APOTHEM ? (aim.z > 0 ? Direction.SOUTH : Direction.NORTH) : hit.getDirection();
    }

    /** Sneak-right-click: report the run's endpoint counts, last moved volume, and precision-floor refusals. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return player.isSecondaryUseActive() ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS; // leave the block alone so pipes can still be placed against it
        }
        if (level.getBlockEntity(pos) instanceof BcePipeBlockEntity pipe) {
            BceNetwork network = BceNetwork.networkFor(pipe);
            if (network != null) {
                player.displayClientMessage(network.statusReport(), false);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> shouldBeType, BlockEntityType<E> isType, BlockEntityTicker<? super E> ticker) {
        return isType == shouldBeType ? (BlockEntityTicker<A>) ticker : null;
    }
}
