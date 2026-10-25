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
 * this face panel instead: right-clicking it resolves the machine that owns the shell it is embedded in (one
 * interface block serves the Compression Manipulator, the Decompression Manipulator and the generator tower) and
 * opens that machine's menu. The tier-1 generator tower has no menu, so for it the same click prints a status
 * readout instead - rate, FE and buffer - which is also why the generator's blueprint requires exactly one of these
 * panels. It holds no storage or logic of its own and needs no block entity - it just routes the interaction to the
 * machine.
 *
 * <p>Resolution asks for the <em>nearest host of any kind</em> and only then checks whether that host has a GUI, so
 * the panel answers the structure it is actually part of. The reverse order - probe for a GUI-bearing controller and
 * fall back to the generator - is what the fixed ±2 lookup used to tolerate, and it stopped being safe once the scan
 * grew to the widest band in the table: a generator tower built near a 5x5x5 Manipulator had its own core skipped
 * over (a generator core is not a controller, so the controller probe ignores it) and walked outward until it found
 * the neighbour's core, opening the wrong machine's screen.
 *
 * <p>All three structures require <em>exactly one</em> of these (the {@code CompressionConfig.MIN_INTERFACES} /
 * {@code CompressionConfig.MAX_INTERFACES} contract every core's own scan enforces); a second one keeps the
 * structure from forming.
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
        // The same nearest-first walk the ports use, so the panel and the pipes around it agree on which machine
        // this cell belongs to; the dispatch on the result is what decides GUI versus chat.
        IMultiblockPortHost host = IMultiblockPortHost.findPortHost(level, pos);
        // A port host with no GUI of its own: the tier-1 generator core. Answered before the controller check so a
        // click on a generator panel can never fall through into a neighbouring machine's screen.
        if (host instanceof GeneratorCoreBlockEntity generator) {
            player.displayClientMessage(Component.translatable("chat.rcc.generator_core.status",
                    Component.literal(generator.rung().displaySuffix()).withStyle(ChatFormatting.GOLD),
                    Component.literal(generator.contentsSummary()).withStyle(ChatFormatting.GRAY)), false);
            return InteractionResult.SUCCESS;
        }
        // Covers "nothing here" and any future ports-only host alike: neither belongs to a machine this panel can open.
        if (!(host instanceof IMultiblockController controller)) {
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
        return InteractionResult.SUCCESS;
    }
}
