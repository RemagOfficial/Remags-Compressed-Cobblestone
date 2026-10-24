package com.remag.rcc.machine;

import com.remag.rcc.registry.ModItemTags;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
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

    /**
     * Right-click with any wrench (the shared {@code c:tools/wrench} convention, see
     * {@link com.remag.rcc.registry.ModItemTags}) cycles this port in place through Power &#8594; Item Input &#8594;
     * Item Output, with no break and no item consumed - the same contract as {@link BcePipeBlock}'s wrench handling.
     * The block entity re-reads {@link #PORT_TYPE} from its live state on every capability query, and both cores
     * rescan their structure periodically, so a re-pointed port starts forwarding the new kind (and the formed flag
     * notices if the swap broke the "at least one of each kind" rule) without anything else having to be touched.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!ModItemTags.isWrench(stack)) {
            // Not a wrench: no special behaviour, ordinary placement against this port still works.
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

    /** Advance to the next kind (wrapping at the top): POWER -> ITEM_IN -> ITEM_OUT -> POWER. */
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
        // Ports are passive; the core drives all ticking.
        return null;
    }
}
