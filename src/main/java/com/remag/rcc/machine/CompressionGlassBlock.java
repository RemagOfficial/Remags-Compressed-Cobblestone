package com.remag.rcc.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glass-like panel used for the Compression Manipulator's face interiors (alongside the ports). Opaque
 * enough to read as a containment window but non-occluding so the interior is visible. No logic.
 */
public class CompressionGlassBlock extends Block {
    public CompressionGlassBlock(Properties properties) {
        super(properties);
    }

    /**
     * Cull the shared face between two adjacent panes, exactly like vanilla {@code GlassBlock}. Being
     * {@code noOcclusion}, the renderer would otherwise draw every interior face of a multi-pane wall, so the
     * box looks busy with doubled/overlapping quads. Only glass-against-glass is hidden; faces toward air,
     * frame or ports still render (or are culled by those opaque neighbours) as normal.
     */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }
}
