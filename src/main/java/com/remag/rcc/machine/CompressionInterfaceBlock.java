package com.remag.rcc.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Compression Manipulator's GUI entry point. Because the core sits sealed in the centre of the cube, the
 * player interacts with this face panel instead: right-clicking it resolves the owning
 * {@link CompressionCoreBlockEntity} and opens the machine menu. It holds no storage or logic of its own and
 * needs no block entity — it just routes the interaction to the core.
 *
 * <p>The blueprint requires <em>exactly one</em> of these (see {@link CompressionConfig#MIN_INTERFACES} /
 * {@link CompressionConfig#MAX_INTERFACES}); a second one keeps the structure from forming.
 */
public class CompressionInterfaceBlock extends Block {
    public CompressionInterfaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS; // no-op on the client; the server opens the menu
        }
        CompressionCoreBlockEntity core = CompressionCoreBlockEntity.findCore(level, pos);
        if (core == null) {
            player.displayClientMessage(Component.translatable("container.rcc.compression_core.detached"), true);
            return InteractionResult.FAIL;
        }
        if (!core.isFormed()) {
            player.displayClientMessage(Component.translatable("container.rcc.compression_core.unformed"), true);
            return InteractionResult.FAIL;
        }
        player.openMenu(core, core.getBlockPos());
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
