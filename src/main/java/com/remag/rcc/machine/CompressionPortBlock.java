package com.remag.rcc.machine;

import com.remag.rcc.registry.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CompressionPortBlock extends Block implements EntityBlock {
    public static final EnumProperty<PortType> PORT_TYPE =
            EnumProperty.create("port_type", PortType.class);

    public CompressionPortBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PORT_TYPE, PortType.POWER)
                .setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT_TYPE, BlockStateProperties.FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        PortType type = PortType.POWER;
        ItemStack stack = context.getItemInHand();
        net.minecraft.world.item.component.BlockItemStateProperties stateProps =
                stack.get(DataComponents.BLOCK_STATE);
        if (stateProps != null) {
            PortType fromItem = stateProps.get(PORT_TYPE);
            if (fromItem != null) {
                type = fromItem;
            }
        }
        Player placer = context.getPlayer();
        return this.defaultBlockState()
                .setValue(PORT_TYPE, type)
                .setValue(BlockStateProperties.FACING, placer != null ? facingToward(placer, context.getClickedPos())
                        : context.getNearestLookingDirection().getOpposite());
    }

    private static Direction facingToward(Player player, BlockPos pos) {
        Vec3 toPlayer = player.getEyePosition().subtract(Vec3.atCenterOf(pos));
        double ax = Math.abs(toPlayer.x);
        double ay = Math.abs(toPlayer.y);
        double az = Math.abs(toPlayer.z);
        if (ay >= ax && ay >= az) {
            return toPlayer.y > 0.0D ? Direction.UP : Direction.DOWN;
        } else if (ax >= az) {
            return toPlayer.x > 0.0D ? Direction.EAST : Direction.WEST;
        } else {
            return toPlayer.z > 0.0D ? Direction.SOUTH : Direction.NORTH;
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!ModItemTags.isWrench(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            PortType next = cycle(state.getValue(PORT_TYPE));
            level.setBlockAndUpdate(pos, state.setValue(PORT_TYPE, next));
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("chat.rcc.port.cycled", next.displayName()), false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static PortType cycle(PortType current) {
        PortType[] all = PortType.values();
        return all[(current.ordinal() + 1) % all.length];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressionPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
