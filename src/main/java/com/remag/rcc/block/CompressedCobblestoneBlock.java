package com.remag.rcc.block;

import com.remag.rcc.block.entity.CompressionBlockEntity;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

/**
 * The one and only compressed cobblestone block. Its {@link BlockEntity} carries the level, so a
 * single registered block covers every compression tier. When placed it copies the level from the
 * item's data component; pick-block reads it back so middle-click gives the correct tier.
 */
public class CompressedCobblestoneBlock extends Block implements EntityBlock {

    public CompressedCobblestoneBlock() {
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)
                .strength(2f).requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressionBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof CompressionBlockEntity blockEntity) {
            blockEntity.setLevel(stack.getOrDefault(ModDataComponents.COMPRESSION_LEVEL.get(), BigInteger.ONE));
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
                                       LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof CompressionBlockEntity blockEntity) {
            stack.set(ModDataComponents.COMPRESSION_LEVEL.get(), blockEntity.getCompressionLevel());
        }
        return stack;
    }
}
