package com.remag.rcc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.CompressionCoreBlockEntity;
import com.remag.rcc.registry.CompressionConfig;
import com.remag.rcc.registry.ModDataComponents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the seed in the Compression Manipulator core as a large 3D cube of the compressed-cobblestone block,
 * floating where the (small anvil) core model sits. The cube is scaled big enough - and concentric with the core -
 * that it always occludes the core block no matter how it is turned (see {@link CompressionConfig#SEED_CUBE_SCALE}).
 * While a cycle is running the cube tumbles on all three axes at a slow, per-machine-random pace (governed by
 * {@link CompressionConfig#SEED_SPIN_SPEED}). The spin is driven by a machine-local clock that only advances while
 * crafting, so the cube keeps tumbling smoothly across back-to-back cycles (holding its orientation during the single
 * idle tick between them) rather than resetting; a brand-new seed that has never been run sits upright so you can see
 * the tier you loaded.
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
 *
 * <p>This renderer also draws the crafting lightning and the completion pulse. Both are emitted as jagged polylines by
 * {@link CompressionEffects} and drawn here, as camera-facing quad ribbons inside the block-entity pass, so the glass
 * panes composite over them
 * (see {@link CompressionEffects} for why the BE pass, and not any particle path, is the only way to make the bolts
 * read as being inside the sealed chamber). Bolts are kept in a short-lived per-machine list and fade out over
 * {@link CompressionConfig#BOLT_TTL} / {@link CompressionConfig#PULSE_TTL} ticks.
 */
public class CompressionCoreRenderer implements BlockEntityRenderer<CompressionCoreBlockEntity> {
    /** Bolt line colour (a pale electric blue) drawn at full RGB; brightness comes from the fade alpha. */
    private static final int BOLT_RED = 180;
    private static final int BOLT_GREEN = 230;
    private static final int BOLT_BLUE = 255;

    private final ItemRenderer itemRenderer;

    /**
     * Live bolts for this machine, in world space, with their birth render-time and lifetime. The renderer instance is
     * per block entity (created once by the {@link BlockEntityRendererProvider}), so this naturally keeps a separate
     * storm per machine. Pruned every frame in {@link #renderEffects}.
     */
    private final List<BoltGeometry> bolts = new ArrayList<>();

    /**
     * Live completion flashes for this machine. Unlike bolts a pulse stores only its spoke <em>directions</em>; its
     * radius and opacity are computed fresh each frame so the flash expands outward past the seed cube as it fades.
     */
    private final List<Pulse> pulses = new ArrayList<>();

    /**
     * Machine-local spin clock, in ticks. It advances only while a cycle runs (see {@link #applySpin}) so the cube
     * keeps tumbling smoothly across the single idle tick between back-to-back crafts instead of snapping upright.
     * Every rotation term is zero at {@code 0}, so a freshly loaded, never-run seed still sits perfectly upright.
     */
    private double spinTime = 0.0D;
    /** Previous frame's absolute render time, used to turn per-frame deltas into {@link #spinTime} increments. */
    private double lastSpinAbs = Double.NaN;

    public CompressionCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CompressionCoreBlockEntity core, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = core.getLevel();
        // Drive the crafting lightning and the completion pulse from synced machine state. This runs ahead of the
        // tier check below on purpose: when the queue empties, the cube disappears on the very sync that reports the
        // craft finished, so the pulse must not depend on there still being a cube to draw. The bolts themselves are
        // drawn AFTER the cube (see the end of this method) so they composite over it correctly.
        double renderTime = level != null ? level.getGameTime() + partialTick : 0.0D;
        if (level != null) {
            driveEffects(core, level, renderTime);
        }

        BigInteger tier = core.getRenderSeedTier();
        if (tier != null) {
            // A single seed block carrying the tier; the item colour handler derives the tint from this component.
            ItemStack seed = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
            seed.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);

            poseStack.pushPose();
            poseStack.translate(0.5D, 0.5D, 0.5D); // pivot at the block centre (ItemRenderer re-centres the 0..1 model)
            long gameTime = level != null ? level.getGameTime() : 0L;
            applySpin(poseStack, core.getBlockPos(), gameTime, partialTick, core.isCrafting());
            poseStack.scale(CompressionConfig.SEED_CUBE_SCALE, CompressionConfig.SEED_CUBE_SCALE, CompressionConfig.SEED_CUBE_SCALE);

            this.itemRenderer.renderStatic(seed, ItemDisplayContext.NONE, LightTexture.pack(15, 15),
                    OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0);
            poseStack.popPose();
        }

        // Draw the bolts and flash last, over the cube we just laid down: the opaque cube occludes the segments running
        // behind it while those in front blend over it (this fixes a faded bolt ghosting the seed cube). The completion
        // flash begins at a radius beyond the cube's corner reach, so it reads around the cube even when another seed is
        // still queued. The far solid frame can still show through a faded bolt - inherent to a translucent effect, and minor.
        if (level != null) {
            renderEffects(poseStack, bufferSource, core.getBlockPos(), renderTime);
        }
    }

    /**
     * Emits zap volleys at a fixed interval while a cycle runs, and a single completion pulse whenever the synced
     * finished-cycle counter advances. The -1 baseline keeps a client joining an already-used machine from replaying a
     * pulse, and the counter (rather than a crafting->idle edge) cannot be missed between two network syncs. Generated
     * geometry is stored with the current render-time so {@link #renderEffects} can age and fade it.
     */
    private void driveEffects(CompressionCoreBlockEntity core, Level level, double renderTime) {
        Vec3 centre = new Vec3(core.getBlockPos().getX() + 0.5D,
                core.getBlockPos().getY() + 0.5D, core.getBlockPos().getZ() + 0.5D);

        if (CompressionConfig.LIGHTNING_ENABLED && core.isCrafting()) {
            long now = level.getGameTime();
            if (now >= core.clientNextZapTick) {
                core.clientNextZapTick = now + CompressionConfig.ZAP_INTERVAL_TICKS;
                for (Vec3[] bolt : CompressionEffects.generateBolts(centre)) {
                    bolts.add(new BoltGeometry(bolt, renderTime, CompressionConfig.BOLT_TTL));
                }
            }
        }

        int done = core.getCraftsCompleted();
        if (core.clientPulseBaseline < 0) {
            core.clientPulseBaseline = done;
        } else if (done > core.clientPulseBaseline) {
            core.clientPulseBaseline = done;
            if (CompressionConfig.LIGHTNING_ENABLED) {
                // On finish: hold off NEW zap volleys for the length of the flash so it reads on its own (bolts already
                // spawned just fade out on their own TTL), then launch the expanding flash.
                core.clientNextZapTick = level.getGameTime() + CompressionConfig.PULSE_TTL;
                pulses.add(new Pulse(CompressionEffects.generatePulseDirections(), renderTime, CompressionConfig.PULSE_TTL));
            }
        }
    }

    /**
     * Draws every live bolt as camera-facing quad ribbons (width {@link CompressionConfig#BOLT_WIDTH}) and every live
     * flash as an expanding shell of thicker ribbons (width {@link CompressionConfig#PULSE_WIDTH}), pruning the expired
     * ones. Bolt vertices are converted from world space to this block's local space (the BER pose stack is already
     * translated to the block origin) by subtracting the corner; the flash is generated in local space directly about
     * the block centre. Each effect's alpha ramps down over its life so bolts and the flash fade rather than snap off.
     */
    private void renderEffects(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos corner, double now) {
        bolts.removeIf(bolt -> now - bolt.born >= bolt.ttl);
        pulses.removeIf(pulse -> now - pulse.born >= pulse.ttl);
        if (bolts.isEmpty() && pulses.isEmpty()) {
            return;
        }

        Vec3 camLocal = cameraLocal(corner);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.debugQuads());
        PoseStack.Pose pose = poseStack.last();

        double boltHalf = CompressionConfig.BOLT_WIDTH * 0.5D;
        for (BoltGeometry bolt : bolts) {
            float fade = (float) (1.0D - (now - bolt.born) / bolt.ttl);
            if (fade < 0.0F) {
                fade = 0.0F;
            }
            int alpha = (int) (255.0F * fade);
            Vec3[] points = bolt.points;
            for (int i = 0; i + 1 < points.length; i++) {
                ribbonQuad(vc, pose, toLocal(points[i], corner), toLocal(points[i + 1], corner), camLocal, boltHalf, alpha);
            }
        }

        // The flash: a shell of radial spokes whose radius grows from PULSE_START_RADIUS (beyond the cube) toward
        // PULSE_END_RADIUS as it fades, so it always presents a bright ring around the seed cube.
        Vec3 centre = new Vec3(0.5D, 0.5D, 0.5D); // block centre in local space; pulses are generated directly here
        double pulseHalf = CompressionConfig.PULSE_WIDTH * 0.5D;
        for (Pulse pulse : pulses) {
            float t = (float) ((now - pulse.born) / pulse.ttl);
            if (t < 0.0F) {
                t = 0.0F;
            } else if (t > 1.0F) {
                t = 1.0F;
            }
            float ease = 1.0F - (1.0F - t) * (1.0F - t); // ease-out: pops big fast, then settles
            double radius = CompressionConfig.PULSE_START_RADIUS
                    + (CompressionConfig.PULSE_END_RADIUS - CompressionConfig.PULSE_START_RADIUS) * ease;
            double outer = radius + CompressionConfig.PULSE_LEN;
            int alpha = (int) (255.0F * (1.0F - t));
            for (Vec3 dir : pulse.dirs) {
                Vec3 a = centre.add(dir.scale(radius));
                Vec3 b = centre.add(dir.scale(outer));
                ribbonQuad(vc, pose, a, b, camLocal, pulseHalf, alpha);
            }
        }

        // Flush our quads now so they are uploaded within the block-entity pass (before the translucent glass draws).
        if (bufferSource instanceof MultiBufferSource.BufferSource buffered) {
            buffered.endBatch(RenderType.debugQuads());
        }
    }

    /**
     * Emits one camera-facing ribbon between the local points {@code a} and {@code b}: the rectangle's side vector is the
     * segment direction crossed with the vector toward the camera, so the strip always presents its full width to the
     * viewer regardless of the bolt's orientation. Four position+colour vertices form one {@code debugQuads} quad.
     */
    private static void ribbonQuad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 camLocal, double halfWidth, int alpha) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        if (len < 1.0E-5D) {
            return;
        }
        dir = dir.scale(1.0D / len);
        Vec3 toCam = camLocal.subtract(a);
        Vec3 side = dir.cross(toCam);
        double sideLen = side.length();
        if (sideLen < 1.0E-6D) {
            // Segment points straight at/away from the camera: fall back to any perpendicular so the ribbon stays non-degenerate.
            side = toCam.cross(dir);
            sideLen = side.length();
            if (sideLen < 1.0E-6D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);
                sideLen = 1.0D;
            }
        }
        side = side.scale(halfWidth / sideLen);

        quadVertex(vc, pose, a.add(side), alpha);
        quadVertex(vc, pose, b.add(side), alpha);
        quadVertex(vc, pose, b.subtract(side), alpha);
        quadVertex(vc, pose, a.subtract(side), alpha);
    }

    /** One {@code debugQuads} vertex (that format is position + colour only, so no normal/light/uv). */
    private static void quadVertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 local, int alpha) {
        vc.addVertex(pose, (float) local.x, (float) local.y, (float) local.z)
                .setColor(BOLT_RED, BOLT_GREEN, BOLT_BLUE, alpha);
    }

    /** Converts a world-space bolt point into this block's local space by subtracting the corner. */
    private static Vec3 toLocal(Vec3 world, BlockPos corner) {
        return world.subtract(corner.getX(), corner.getY(), corner.getZ());
    }

    /**
     * The active camera position in this block's local space, used to orient the ribbons toward the viewer. Falls back
     * to a fixed offset above/behind the block if no camera is available (during a real frame it always is).
     */
    private Vec3 cameraLocal(BlockPos corner) {
        Vec3 camWorld = null;
        if (Minecraft.getInstance().gameRenderer != null) {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            if (camera != null) {
                camWorld = camera.getPosition();
            }
        }
        if (camWorld == null) {
            camWorld = new Vec3(corner.getX() + 0.5D, corner.getY() + 2.0D, corner.getZ() + 3.0D);
        }
        return camWorld.subtract(corner.getX(), corner.getY(), corner.getZ());
    }

    /**
     * Slow pseudo-random tumble. The rotation rates on each axis are derived from the core's position, so every
     * machine spins differently, plus a low-frequency wobble so the motion never looks like a fixed clock. The
     * global {@link CompressionConfig#SEED_SPIN_SPEED} multiplier scales all three rates together.
     *
     * <p>Driven by {@link #spinTime}, a machine-local clock that advances only while {@code crafting} is true. This
     * keeps the spin continuous across the one-tick idle gap between consecutive crafts (the cube holds its last
     * orientation rather than resetting to upright), while every angle term evaluates to {@code 0} at startup so a
     * brand-new seed renders upright for inspection.
     */
    private void applySpin(PoseStack poseStack, BlockPos pos, long gameTime, float partialTick, boolean crafting) {
        long raw = pos.asLong();
        int h = (int) (raw ^ (raw >>> 32));
        float speed = CompressionConfig.SEED_SPIN_SPEED;

        float rateX = (0.15F + (h & 0xF) * 0.02F) * speed;
        float rateY = (0.25F + ((h >>> 4) & 0xF) * 0.03F) * speed;
        float rateZ = (0.10F + ((h >>> 8) & 0xF) * 0.02F) * speed;
        float phase = (h & 0xFF) * 0.025F;

        double abs = gameTime + partialTick;
        if (crafting && !Double.isNaN(this.lastSpinAbs)) {
            double dt = abs - this.lastSpinAbs;
            // Ignore a first-sight or post-hitch frame (dt outside a sane per-frame window) so the clock never jumps.
            if (dt > 0.0D && dt < 20.0D) {
                this.spinTime += dt;
            }
        }
        this.lastSpinAbs = abs;

        double t = this.spinTime;
        // Each line is zero at t == 0 (the wobble subtracts its own starting value) so an untouched cube is upright.
        float ax = (float) (t * rateX) + (float) (Math.sin(t * 0.013D + phase) - Math.sin(phase)) * 12.0F;
        float ay = (float) (t * rateY);
        float az = (float) (t * rateZ) + (float) (Math.cos(t * 0.017D + phase) - Math.cos(phase)) * 8.0F;

        poseStack.mulPose(Axis.XP.rotationDegrees(ax));
        poseStack.mulPose(Axis.YP.rotationDegrees(ay));
        poseStack.mulPose(Axis.ZP.rotationDegrees(az));
    }

    /** An immutable, already-generated bolt polyline plus when it was spawned and how long it lives. */
    private static final class BoltGeometry {
        final Vec3[] points;
        final double born;
        final double ttl;

        BoltGeometry(Vec3[] points, double born, double ttl) {
            this.points = points;
            this.born = born;
            this.ttl = ttl;
        }
    }

    /**
     * A completion flash: fixed spoke directions plus birth render-time and lifetime. Its radius and alpha are derived
     * from age each frame (see {@link #renderEffects}), so it expands outward past the seed cube and fades.
     */
    private static final class Pulse {
        final Vec3[] dirs;
        final double born;
        final double ttl;

        Pulse(Vec3[] dirs, double born, double ttl) {
            this.dirs = dirs;
            this.born = born;
            this.ttl = ttl;
        }
    }
}
