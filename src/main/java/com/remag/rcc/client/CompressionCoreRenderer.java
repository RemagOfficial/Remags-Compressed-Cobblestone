package com.remag.rcc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.remag.rcc.item.ModItems;
import com.remag.rcc.machine.CompressionCoreBlockEntity;
import com.remag.rcc.registry.CompressionBand;
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
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompressionCoreRenderer implements BlockEntityRenderer<CompressionCoreBlockEntity> {
    private static final int BOLT_RED = 180;
    private static final int BOLT_GREEN = 230;
    private static final int BOLT_BLUE = 255;

    private static final long PRUNE_INTERVAL = 100L;

    private final ItemRenderer itemRenderer;

    private final Map<BlockPos, MachineFx> machines = new HashMap<>();
    private long nextPruneTime = 0L;

    public CompressionCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CompressionCoreBlockEntity core, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = core.getLevel();
        long gameTime = level != null ? level.getGameTime() : 0L;
        double renderTime = level != null ? gameTime + partialTick : 0.0D;
        pruneStale(gameTime);
        MachineFx fx = machines.computeIfAbsent(core.getBlockPos(), p -> new MachineFx());
        fx.lastSeen = gameTime;
        float seedScale = CompressionConfig.seedCubeScale(core.band());
        float pulseGrowth = CompressionConfig.effectGrowth(core.band());
        if (level != null) {
            driveEffects(core, level, renderTime, seedScale, fx);
        }

        BigInteger tier = core.getRenderSeedTier();
        if (tier != null) {
            ItemStack seed = new ItemStack(ModItems.COMPRESSED_COBBLESTONE.get());
            seed.set(ModDataComponents.COMPRESSION_LEVEL.get(), tier);

            poseStack.pushPose();
            poseStack.translate(0.5D, 0.5D, 0.5D);
            applySpin(poseStack, core.getBlockPos(), gameTime, partialTick, core.isCrafting(), fx);
            poseStack.scale(seedScale, seedScale, seedScale);

            this.itemRenderer.renderStatic(seed, ItemDisplayContext.NONE, LightTexture.pack(15, 15),
                    OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0);
            poseStack.popPose();
        }

        if (level != null) {
            renderEffects(poseStack, bufferSource, core.getBlockPos(), renderTime, pulseGrowth, core.band(), fx);
        }
    }

    private void pruneStale(long gameTime) {
        if (gameTime < nextPruneTime) {
            return;
        }
        nextPruneTime = gameTime + PRUNE_INTERVAL;
        machines.entrySet().removeIf(entry -> gameTime - entry.getValue().lastSeen > PRUNE_INTERVAL);
    }

    private void driveEffects(CompressionCoreBlockEntity core, Level level, double renderTime, float seedScale, MachineFx fx) {
        Vec3 centre = new Vec3(core.getBlockPos().getX() + 0.5D,
                core.getBlockPos().getY() + 0.5D, core.getBlockPos().getZ() + 0.5D);

        if (CompressionConfig.LIGHTNING_ENABLED && core.isCrafting()) {
            long now = level.getGameTime();
            if (now >= core.clientNextZapTick) {
                core.clientNextZapTick = now + CompressionConfig.ZAP_INTERVAL_TICKS;
                for (Vec3[] bolt : CompressionEffects.generateBolts(centre, core.band(), seedScale)) {
                    fx.bolts.add(new BoltGeometry(bolt, renderTime, CompressionConfig.BOLT_TTL));
                }
            }
        }

        int done = core.getCraftsCompleted();
        if (core.clientPulseBaseline < 0) {
            core.clientPulseBaseline = done;
        } else if (done > core.clientPulseBaseline) {
            core.clientPulseBaseline = done;
            if (CompressionConfig.LIGHTNING_ENABLED) {
                core.clientNextZapTick = level.getGameTime() + CompressionConfig.PULSE_TTL;
                fx.pulses.add(new Pulse(CompressionEffects.generatePulseDirections(core.band()), renderTime, CompressionConfig.PULSE_TTL));
            }
        }
    }

    private void renderEffects(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos corner, double now, float pulseGrowth, CompressionBand band, MachineFx fx) {
        fx.bolts.removeIf(bolt -> now - bolt.born >= bolt.ttl);
        fx.pulses.removeIf(pulse -> now - pulse.born >= pulse.ttl);
        if (fx.bolts.isEmpty() && fx.pulses.isEmpty()) {
            return;
        }

        Vec3 camLocal = cameraLocal(corner);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.debugQuads());
        PoseStack.Pose pose = poseStack.last();

        double boltHalf = CompressionConfig.boltWidth(band) * 0.5D;
        for (BoltGeometry bolt : fx.bolts) {
            float fade = (float) (1.0D - (now - bolt.born) / bolt.ttl);
            if (fade < 0.0F) {
                fade = 0.0F;
            }
            int alpha = (int) (255.0F * fade);
            Vec3[] pts = bolt.points;
            Vec3[] local = new Vec3[pts.length];
            for (int i = 0; i < pts.length; i++) {
                local[i] = toLocal(pts[i], corner);
            }
            emitRibbon(vc, pose, local, camLocal, boltHalf, alpha);
        }

        Vec3 centre = new Vec3(0.5D, 0.5D, 0.5D);
        double pulseHalf = CompressionConfig.pulseWidth(band) * 0.5D;
        for (Pulse pulse : fx.pulses) {
            float t = (float) ((now - pulse.born) / pulse.ttl);
            if (t < 0.0F) {
                t = 0.0F;
            } else if (t > 1.0F) {
                t = 1.0F;
            }
            float ease = 1.0F - (1.0F - t) * (1.0F - t);
            double radius = (CompressionConfig.PULSE_START_RADIUS
                    + (CompressionConfig.PULSE_END_RADIUS - CompressionConfig.PULSE_START_RADIUS) * ease) * pulseGrowth;
            double outer = radius + CompressionConfig.PULSE_LEN;
            int alpha = (int) (255.0F * (1.0F - t));
            for (Vec3 dir : pulse.dirs) {
                Vec3 a = centre.add(dir.scale(radius));
                Vec3 b = centre.add(dir.scale(outer));
                ribbonQuad(vc, pose, a, b, camLocal, pulseHalf, alpha);
            }
        }

        if (bufferSource instanceof MultiBufferSource.BufferSource buffered) {
            buffered.endBatch(RenderType.debugQuads());
        }
    }

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

    private static final double BOLT_MITER_LIMIT = 4.0D;

    private static void emitRibbon(VertexConsumer vc, PoseStack.Pose pose, Vec3[] pts, Vec3 camLocal, double halfWidth, int alpha) {
        int n = pts.length;
        if (n < 2) {
            return;
        }
        Vec3[] offsets = new Vec3[n];
        for (int i = 0; i < n; i++) {
            Vec3 toCam = normalize(pts[i].subtract(camLocal).reverse());
            Vec3 sPrev = i > 0 ? ribbonSide(pts[i].subtract(pts[i - 1]), toCam) : null;
            Vec3 sNext = i + 1 < n ? ribbonSide(pts[i + 1].subtract(pts[i]), toCam) : null;
            Vec3 side;
            if (sPrev == null) {
                side = sNext;
            } else if (sNext == null) {
                side = sPrev;
            } else {
                side = normalize(sPrev.add(sNext));
                if (side == null) {
                    side = sNext;
                }
            }
            Vec3 ref = sNext != null ? sNext : sPrev;
            double cos = side.dot(ref);
            if (cos < 1.0D / BOLT_MITER_LIMIT) {
                cos = 1.0D / BOLT_MITER_LIMIT;
            }
            offsets[i] = side.scale(halfWidth / cos);
        }
        for (int i = 0; i + 1 < n; i++) {
            Vec3 a = pts[i];
            Vec3 b = pts[i + 1];
            Vec3 ao = offsets[i];
            Vec3 bo = offsets[i + 1];
            quadVertex(vc, pose, a.add(ao), alpha);
            quadVertex(vc, pose, b.add(bo), alpha);
            quadVertex(vc, pose, b.subtract(bo), alpha);
            quadVertex(vc, pose, a.subtract(ao), alpha);
        }
    }

    @Nullable
    private static Vec3 ribbonSide(Vec3 dir, Vec3 toCam) {
        Vec3 d = normalize(dir);
        if (d == null) {
            return null;
        }
        Vec3 side = d.cross(toCam);
        return normalize(side);
    }

    @Nullable
    private static Vec3 normalize(Vec3 v) {
        double len = v.length();
        return len < 1.0E-6D ? null : v.scale(1.0D / len);
    }

    private static void quadVertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 local, int alpha) {
        vc.addVertex(pose, (float) local.x, (float) local.y, (float) local.z)
                .setColor(BOLT_RED, BOLT_GREEN, BOLT_BLUE, alpha);
    }

    private static Vec3 toLocal(Vec3 world, BlockPos corner) {
        return world.subtract(corner.getX(), corner.getY(), corner.getZ());
    }

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

    private void applySpin(PoseStack poseStack, BlockPos pos, long gameTime, float partialTick, boolean crafting, MachineFx fx) {
        long raw = pos.asLong();
        int h = (int) (raw ^ (raw >>> 32));
        float speed = CompressionConfig.SEED_SPIN_SPEED;

        float rateX = (0.15F + (h & 0xF) * 0.02F) * speed;
        float rateY = (0.25F + ((h >>> 4) & 0xF) * 0.03F) * speed;
        float rateZ = (0.10F + ((h >>> 8) & 0xF) * 0.02F) * speed;
        float phase = (h & 0xFF) * 0.025F;

        double abs = gameTime + partialTick;
        if (crafting && !Double.isNaN(fx.lastSpinAbs)) {
            double dt = abs - fx.lastSpinAbs;
            if (dt > 0.0D && dt < 20.0D) {
                fx.spinTime += dt;
            }
        }
        fx.lastSpinAbs = abs;

        double t = fx.spinTime;
        float ax = (float) (t * rateX) + (float) (Math.sin(t * 0.013D + phase) - Math.sin(phase)) * 12.0F;
        float ay = (float) (t * rateY);
        float az = (float) (t * rateZ) + (float) (Math.cos(t * 0.017D + phase) - Math.cos(phase)) * 8.0F;

        poseStack.mulPose(Axis.XP.rotationDegrees(ax));
        poseStack.mulPose(Axis.YP.rotationDegrees(ay));
        poseStack.mulPose(Axis.ZP.rotationDegrees(az));
    }

    private static final class MachineFx {
        final List<BoltGeometry> bolts = new ArrayList<>();
        final List<Pulse> pulses = new ArrayList<>();
        double spinTime = 0.0D;
        double lastSpinAbs = Double.NaN;
        long lastSeen = Long.MIN_VALUE;
    }

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
