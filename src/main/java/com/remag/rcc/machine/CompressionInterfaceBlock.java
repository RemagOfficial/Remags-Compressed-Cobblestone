package com.remag.rcc.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The multiblock GUI entry point. Because the core sits sealed in the centre of the cube, the player interacts with
 * this face panel instead: right-clicking it resolves the owning {@link IMultiblockController} (a Compression or a
 * Decompression Manipulator core - one interface block serves both) and opens that machine's menu. The tier-1
 * generator tower has no menu, so for it the same click prints a status readout instead - rate, FE and buffer -
 * which is also why the generator's blueprint requires exactly one of these panels. It holds no storage or logic
 * of its own and needs no block entity - it just routes the interaction to the machine.
 *
 * <p>All three structures require <em>exactly one</em> of these (see {@link CompressionConfig#MIN_INTERFACES} /
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
        IMultiblockController controller = IMultiblockController.findController(level, pos);
        if (controller == null) {
            // No compression machine here - the panel may instead belong to a tier-1 generator tower, whose core
            // is a plain IMultiblockPortHost with no menu: answer a click on it with the chat status readout.
            if (IMultiblockPortHost.findPortHost(level, pos) instanceof GeneratorCoreBlockEntity generator) {
                player.displayClientMessage(Component.translatable("chat.rcc.generator_core.status",
                        Component.literal(generator.rung().displaySuffix()).withStyle(ChatFormatting.GOLD),
                        Component.literal(generator.contentsSummary()).withStyle(ChatFormatting.GRAY)), false);
                return InteractionResult.SUCCESS;
            }
            player.displayClientMessage(Component.translatable("container.rcc.compression_core.detached"), true);
            return InteractionResult.FAIL;
        }
        if (!controller.isFormed()) {
            player.displayClientMessage(Component.translatable("container.rcc.compression_core.unformed"), true);
            return InteractionResult.FAIL;
        }
        // Write the controller position, plus the current goal tier when the machine has one (the decompressor), so
        // the client screen can prefill the goal-tier field with the server-authoritative value.
        BlockPos corePos = ((BlockEntity) controller).getBlockPos();
        player.openMenu(controller, buf -> {
            buf.writeBlockPos(corePos);
            if (controller instanceof IGoalTierTarget target) {
                buf.writeUtf(target.goalTierString());
            }
        });
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
