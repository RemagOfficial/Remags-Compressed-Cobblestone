package com.remag.rcc.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A Compression Manipulator connection point. One block serves all three {@link PortType}s via the
 * {@code PORT_TYPE} state: power in, item in (fuel), item out (finished product). The port itself holds
 * no storage — its {@link CompressionPortBlockEntity} forwards to the multiblock core.
 */
public class CompressionPortBlock extends Block implements EntityBlock {
    public static final EnumProperty<PortType> PORT_TYPE =
            EnumProperty.create("port_type", PortType.class);

    public CompressionPortBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PORT_TYPE, PortType.POWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT_TYPE);
    }

    /**
     * Place the port with the {@link PortType} carried on the held stack's {@code block_state} component,
     * so the three creative-tab variants (and any automation that copies them) place as the intended kind.
     */
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
        return this.defaultBlockState().setValue(PORT_TYPE, type);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressionPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Ports are passive; the core drives all ticking.
        return null;
    }
}
