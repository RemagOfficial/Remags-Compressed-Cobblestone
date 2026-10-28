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

public class BcePipeBlock extends PipeBlock implements EntityBlock {
    public static final MapCodec<BcePipeBlock> CODEC = simpleCodec(BcePipeBlock::new);
    public static final EnumProperty<BcePipeTier> TIER = EnumProperty.create("tier", BcePipeTier.class);

    public static final EnumProperty<BcePipeMode> NORTH_END = EnumProperty.create("north_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> EAST_END = EnumProperty.create("east_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> SOUTH_END = EnumProperty.create("south_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> WEST_END = EnumProperty.create("west_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> UP_END = EnumProperty.create("up_end", BcePipeMode.class);
    public static final EnumProperty<BcePipeMode> DOWN_END = EnumProperty.create("down_end", BcePipeMode.class);

    public static final Map<Direction, EnumProperty<BcePipeMode>> END_BY_DIRECTION =
            ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), ends -> {
                ends.put(Direction.NORTH, NORTH_END);
                ends.put(Direction.EAST, EAST_END);
                ends.put(Direction.SOUTH, SOUTH_END);
                ends.put(Direction.WEST, WEST_END);
                ends.put(Direction.UP, UP_END);
                ends.put(Direction.DOWN, DOWN_END);
            }));

    public static final ResourceLocation TIER_ITEM_PROPERTY =
            ResourceLocation.fromNamespaceAndPath(RCC.MODID, "pipe_tier");

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

    public boolean canConnectTo(BlockGetter level, BlockPos pos, Direction dir) {
        BlockState neighbour = level.getBlockState(pos.relative(dir));
        return neighbour.getBlock() instanceof BcePipeBlock || neighbour.is(ModBlockTags.BCE_CONNECTABLE);
    }

    public BcePipeMode endFor(BlockGetter level, BlockPos pos, Direction dir, @Nullable BcePipeMode configured) {
        if (!canConnectTo(level, pos, dir)) {
            return BcePipeMode.OFF;
        }
        return configured != null && configured.isLinked() ? configured : BcePipeMode.PASSIVE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                    LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        EnumProperty<BcePipeMode> end = END_BY_DIRECTION.get(facing);
        return state.setValue(end, endFor(level, currentPos, facing, state.getValue(end)));
    }

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

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!ModItemTags.isWrench(stack)) {
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

    private static Component sideName(Direction dir) {
        return Component.translatable("side.rcc." + dir.getSerializedName());
    }

    private Direction targetedEnd(BlockPos pos, BlockHitResult hit) {
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return player.isSecondaryUseActive() ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
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
