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
}
