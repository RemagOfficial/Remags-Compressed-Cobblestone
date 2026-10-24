package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.BceNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A single {@link BcePipeBlock} segment. It holds no BCE and persists nothing - all that lives here is a reference
 * to the shared {@link BceNetwork} the segment belongs to, which is rebuilt lazily whenever the local topology is
 * suspect (see {@link BceNetwork#networkFor}). Because nothing is stored, breaking a pipe mid-transfer cannot lose
 * mass: a transfer is planned and executed within one server tick, source store to sink store.
 */
public class BcePipeBlockEntity extends BlockEntity {
    /** {@code null} means "not yet bound (or deliberately dropped)"; the next tick re-floods the run. */
    @Nullable
    private BceNetwork network;

    public BcePipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BCE_PIPE.get(), pos, state);
    }

    @Nullable
    public BceNetwork getNetwork() {
        return this.network;
    }

    public void setNetwork(@Nullable BceNetwork network) {
        this.network = network;
    }

    /**
     * Something around this pipe changed (a pipe placed/broken, a store added or removed). Tell the shared network
     * it is no longer trustworthy - which propagates to every pipe in the run, since they hold the same object - and
     * drop the local reference so this segment re-floods from itself on its next tick.
     */
    public void markNetworkStale() {
        if (this.network != null) {
            this.network.markStale();
        }
        this.network = null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BcePipeBlockEntity pipe) {
        BceNetwork network = BceNetwork.networkFor(pipe);
        if (network != null) {
            network.tick();
        }
    }

    /**
     * A broken pipe must drop the tier it actually was, not the block's default tier-1 state. The tier lives only in
     * the block state, so it is published here as the {@code block_state} component that the loot table's
     * {@code copy_components} function reads off this block entity (the same route the compressed cobblestone's level
     * takes in {@link com.remag.rcc.block.entity.CompressionBlockEntity}), and that
     * {@link BcePipeBlock#getStateForPlacement} reads back when the drop is placed again.
     *
     * <p>The six per-end modes are deliberately left off: the drop goes back to plain passive ends and the player
     * wrenches round whichever ends they want. Carrying them would cost the upgrade path, because the ladder recipe
     * matches its input on the <em>exact</em> {@code block_state} value ({@code {tier}} vs {@code {tier, north_end}}
     * are different values), so every pipe that had ever been placed would stop qualifying for the next tier.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        builder.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                .with(BcePipeBlock.TIER, getBlockState().getValue(BcePipeBlock.TIER)));
    }
}
