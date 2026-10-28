package com.remag.rcc.machine;

import com.remag.rcc.block.entity.ModBlockEntities;
import com.remag.rcc.energy.IBceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CompressionPortBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos cachedCorePos = null;

    public CompressionPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSION_PORT.get(), pos, state);
    }

    private PortType portType() {
        return getBlockState().getValue(CompressionPortBlock.PORT_TYPE);
    }

    private boolean facesFront(@Nullable Direction side) {
        return side == null || getBlockState().getValue(BlockStateProperties.FACING) == side;
    }

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
        if (!facesFront(side) || portType() != PortType.POWER) {
            return null;
        }
        Level level = this.level;
        if (level == null) {
            return null;
        }
        List<IMultiblockPortHost> genHosts = IMultiblockPortHost.findGeneratorPowerHosts(level, worldPosition);
        if (genHosts.size() == 1) {
            return genHosts.get(0).getEnergy();
        }
        if (genHosts.size() > 1) {
            return new SharedPowerStorage();
        }
        IMultiblockPortHost host = resolveHost();
        return host == null ? null : host.getEnergy();
    }

    @Nullable
    public IItemHandler getItemCapability(@Nullable Direction side) {
        if (!facesFront(side)) {
            return null;
        }
        IMultiblockPortHost host = resolveHost();
        return host == null ? null : host.portItemHandler(portType());
    }

    @Nullable
    public IBceStorage getBceCapability(@Nullable Direction side) {
        if (!facesFront(side)) {
            return null;
        }
        IMultiblockPortHost host = resolveHost();
        return host == null ? null : host.portBceHandler(portType());
    }

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
            return 0;
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
