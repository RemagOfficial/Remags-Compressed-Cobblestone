package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.IBceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The block entity behind a {@link CompressionPortBlock}. It stores nothing itself: it locates whichever multiblock
 * {@link IMultiblockPortHost} seals the centre of its structure (the core is always within the maximum structure
 * radius of any shell cell) and forwards, per {@link PortType}, the capability the host offers for that kind -
 * energy (POWER), an item handler (ITEM_IN / ITEM_OUT), and BCE (whatever the host exposes, e.g. the compressor's
 * fuel sink on ITEM_IN, the decompressor's buffer on its item ports, or the tier-1 generator's one-way mint on
 * ITEM_OUT). One port block therefore serves the 5x5x5 Compression Manipulator, the 3x3x3 Decompression Manipulator,
 * and the 3x3x4 generator tower alike.
 */
public class CompressionPortBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos cachedCorePos = null;

    public CompressionPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_PORT.get(), pos, state);
    }

    private PortType portType() {
        return getBlockState().getValue(CompressionPortBlock.PORT_TYPE);
    }

    /** Find (and cache) the host this port belongs to, re-scanning if the cached one is gone. */
    @Nullable
    public IMultiblockPortHost resolveHost() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        if (cachedCorePos != null && level.getBlockEntity(cachedCorePos) instanceof IMultiblockPortHost cached) {
            return cached;
        }
        cachedCorePos = null;
        IMultiblockPortHost host = IMultiblockPortHost.findPortHost(level, worldPosition);
        if (host instanceof BlockEntity be) {
            cachedCorePos = be.getBlockPos();
        }
        return host;
    }

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        if (portType() != PortType.POWER) {
            return null;
        }
        Level level = this.level;
        if (level == null) {
            return null;
        }
        // Generator wall sharing (MASTER_PLAN.md D24): a POWER port standing on a wall two same-band towers share is
        // claimed by both, so its incoming FE splits evenly between them. One claimer behaves exactly as before.
        List<IMultiblockPortHost> genHosts = IMultiblockPortHost.findGeneratorPowerHosts(level, worldPosition);
        if (genHosts.size() == 1) {
            return genHosts.get(0).getEnergy();
        }
        if (genHosts.size() > 1) {
            return new SharedPowerStorage();
        }
        // No generator core claims this cell: a compressor / decompressor POWER port (or a loose port) stays on the
        // ordinary single-host path.
        IMultiblockPortHost host = resolveHost();
        return host == null ? null : host.getEnergy();
    }

    @Nullable
    public IItemHandler getItemCapability(@Nullable Direction side) {
        IMultiblockPortHost host = resolveHost();
        return host == null ? null : host.portItemHandler(portType());
    }

    /** The BCE store this port forwards to its host, or {@code null} when that kind carries none. */
    @Nullable
    public IBceStorage getBceCapability(@Nullable Direction side) {
        IMultiblockPortHost host = resolveHost();
        return host == null ? null : host.portBceHandler(portType());
    }

    /**
     * The FE view of a POWER port standing on a wall shared by two or more generator towers. It owns no storage: it
     * fans whatever a cable pushes into it out to the co-owners, each taking an even share (the leftover from an odd
     * amount going to the first owners in order), and reports the aggregate of their buffers for display. The owner
     * list is re-resolved live on every call, so a stale NeoForge capability cache can never under- or over-split;
     * the generator core's {@code invalidateCapabilities} on a claim-set change is what prompts the cable to re-query
     * in the first place. Extract is refused, matching every generator's receive-only buffer.
     */
    private final class SharedPowerStorage implements IEnergyStorage {
        private List<IMultiblockPortHost> owners() {
            Level level = CompressionPortBlockEntity.this.level;
            return level == null
                    ? List.of()
                    : IMultiblockPortHost.findGeneratorPowerHosts(level, worldPosition);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            List<IMultiblockPortHost> owners = owners();
            int n = owners.size();
            if (n == 0) {
                return 0;
            }
            int base = maxReceive / n;
            int remainder = maxReceive % n;
            int accepted = 0;
            for (int i = 0; i < n; i++) {
                int share = base + (i < remainder ? 1 : 0);
                if (share <= 0) {
                    continue;
                }
                IEnergyStorage target = owners.get(i).getEnergy();
                if (target != null) {
                    accepted += target.receiveEnergy(share, simulate);
                }
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // shared generator power is never pumped back out through the port
        }

        @Override
        public int getEnergyStored() {
            int sum = 0;
            for (IMultiblockPortHost owner : owners()) {
                IEnergyStorage target = owner.getEnergy();
                if (target != null) {
                    sum += target.getEnergyStored();
                }
            }
            return sum;
        }

        @Override
        public int getMaxEnergyStored() {
            int sum = 0;
            for (IMultiblockPortHost owner : owners()) {
                IEnergyStorage target = owner.getEnergy();
                if (target != null) {
                    sum += target.getMaxEnergyStored();
                }
            }
            return sum;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
