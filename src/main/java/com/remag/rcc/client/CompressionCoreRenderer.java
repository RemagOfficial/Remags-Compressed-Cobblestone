package com.remag.rcc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.CompressionCoreBlockEntity;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.math.BigInteger;

/**
 * Renders the seed in the Compression Manipulator core as a large 3D cube of the compressed-cobblestone block,
 * floating where the (small anvil) core model sits. The cube is scaled big enough - and concentric with the core -
 * that it always occludes the core block no matter how it is turned (see {@link CompressionConfig#SEED_CUBE_SCALE}).
 * While a cycle is running the cube tumbles on all three axes at a slow, per-machine-random pace (governed by
 * {@link CompressionConfig#SEED_SPIN_SPEED}); when idle it simply sits upright so you can see the tier you loaded.
 *
 * <p>The cube is drawn through the {@link ItemRenderer} using {@link ItemDisplayContext#NONE}, which renders the
 * block item as a real 3D cube. Its per-tier colour comes from the registered item colour handler
 * ({@link com.remag.rcc.event.ClientEvents}): the seed's {@code tintindex} quads are multiplied by
 * {@link com.remag.rcc.registry.CompressionConfig#tintArgb(java.math.BigInteger)}, so higher tiers render darker.
 * Note this is a grayscale multiply, so a low tier (e.g. tier 1000, brightness ~0.75) looks only slightly darker
 * than plain cobblestone while very high tiers darken dramatically. We render through the ordinary item path with
 * the real {@link MultiBufferSource} - do NOT wrap the buffer source to force a colour, since the game's
 * {@code BufferSource} is stateful and its batch bookkeeping relies on getting its own consumer back.
 *
 * <p>The tier is read from the client-synced {@link CompressionCoreBlockEntity#getRenderSeedTier()}, which the core
 * captures at craft start (and keeps frozen across the cycle), so the cube shows whether the machine is idle or busy.
 */
public class CompressionCoreRenderer implements BlockEntityRenderer<CompressionCoreBlockEntity> {
    private final ItemRenderer itemRenderer;

    public CompressionCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CompressionCoreBlockEntity core, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BigInteger tier = core.getRenderSeedTier();
        if (tier == null) {
            return;
        }
        Level level = core.getLevel();

        // A single seed block carrying the tier; the item colour handler derives the tint from this component.
        ItemStack seed = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
        seed.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D); // pivot at the block centre (ItemRenderer re-centres the 0..1 model)
        if (core.isCrafting()) {
            long gameTime = level != null ? level.getGameTime() : 0L;
            applySpin(poseStack, core.getBlockPos(), gameTime, partialTick);
        }
        poseStack.scale(CompressionConfig.SEED_CUBE_SCALE, CompressionConfig.SEED_CUBE_SCALE, CompressionConfig.SEED_CUBE_SCALE);

        this.itemRenderer.renderStatic(seed, ItemDisplayContext.NONE, LightTexture.pack(15, 15),
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0);
        poseStack.popPose();
    }

    /**
     * Slow pseudo-random tumble. The rotation rates on each axis are derived from the core's position, so every
     * machine spins differently, plus a low-frequency wobble so the motion never looks like a fixed clock. The
     * global {@link CompressionConfig#SEED_SPIN_SPEED} multiplier scales all three rates together.
     */
    private void applySpin(PoseStack poseStack, BlockPos pos, long gameTime, float partialTick) {
        long raw = pos.asLong();
        int h = (int) (raw ^ (raw >>> 32));
        double t = gameTime + partialTick;
        float speed = CompressionConfig.SEED_SPIN_SPEED;

        float rateX = (0.15F + (h & 0xF) * 0.02F) * speed;
        float rateY = (0.25F + ((h >>> 4) & 0xF) * 0.03F) * speed;
        float rateZ = (0.10F + ((h >>> 8) & 0xF) * 0.02F) * speed;
        float phase = (h & 0xFF) * 0.025F;

        float ax = (float) (t * rateX) + (float) Math.sin(t * 0.013D + phase) * 12.0F;
        float ay = (float) (t * rateY);
        float az = (float) (t * rateZ) + (float) Math.cos(t * 0.017D + phase) * 8.0F;

        poseStack.mulPose(Axis.XP.rotationDegrees(ax));
        poseStack.mulPose(Axis.YP.rotationDegrees(ay));
        poseStack.mulPose(Axis.ZP.rotationDegrees(az));
    }
}
