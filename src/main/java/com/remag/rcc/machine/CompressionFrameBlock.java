package com.remag.rcc.machine;

import net.minecraft.world.level.block.Block;

/**
 * Solid structural cube used on the Compression Manipulator's edges and corners. Purely decorative /
 * load-bearing: it carries no logic. The multiblock scan requires exactly this block wherever the
 * blueprint calls for a frame cell.
 */
public class CompressionFrameBlock extends Block {
    public CompressionFrameBlock(Properties properties) {
        super(properties);
    }
}
