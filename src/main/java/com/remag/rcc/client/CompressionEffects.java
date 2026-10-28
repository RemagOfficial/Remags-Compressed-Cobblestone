package com.remag.rcc.client;

import com.remag.rcc.registry.CompressionBand;
import com.remag.rcc.registry.CompressionConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CompressionEffects {
    private static final double WALL_INSET = 0.55D;

    private static final Random RANDOM = new Random();

    private CompressionEffects() {
    }

    public static List<Vec3[]> generateBolts(Vec3 centre, CompressionBand band, double seedScale) {
        int boltsPerZap = CompressionConfig.boltsPerZap(band);
        int steps = CompressionConfig.boltSteps(band);
        double jag = CompressionConfig.boltJag(band);
        List<Vec3[]> bolts = new ArrayList<>(boltsPerZap);
        double cubeRadius = seedScale * 0.5D;
        double wallHalf = band.compressorRadius() - WALL_INSET;
        for (int i = 0; i < boltsPerZap; i++) {
            Vec3 wall = randomPointOnCubeShell(centre, wallHalf);
            Vec3 outward = wall.subtract(centre);
            double outwardLen = outward.length();
            if (outwardLen < 1.0E-4D) {
                continue;
            }
            Vec3 start = centre.add(outward.scale(cubeRadius / outwardLen));
            bolts.add(jaggedBolt(start, wall, steps, jag));
        }
        return bolts;
    }

    public static Vec3[] generatePulseDirections(CompressionBand band) {
        int count = CompressionConfig.pulseBurst(band);
        Vec3[] dirs = new Vec3[count];
        for (int i = 0; i < count; i++) {
            dirs[i] = randomUnitVector();
        }
        return dirs;
    }

    private static Vec3[] jaggedBolt(Vec3 start, Vec3 end, int steps, double jag) {
        Vec3 axis = end.subtract(start);
        double total = axis.length();
        if (total < 1.0E-4D) {
            return new Vec3[]{start, end};
        }
        int segs = Math.max(1, steps);
        Vec3 dir = axis.scale(1.0D / total);
        Vec3 perp1 = perpendicular(dir, 0);
        Vec3 perp2 = perpendicular(dir, 1);

        Vec3[] points = new Vec3[segs + 1];
        points[0] = start;
        for (int s = 1; s <= segs; s++) {
            double t = (double) s / segs;
            Vec3 point = start.add(axis.scale(t));
            if (s < segs && jag > 0.0D) {
                double amt = jag * Math.sin(t * Math.PI);
                point = point
                        .add(perp1.scale((RANDOM.nextDouble() * 2.0D - 1.0D) * amt))
                        .add(perp2.scale((RANDOM.nextDouble() * 2.0D - 1.0D) * amt));
            }
            points[s] = point;
        }
        return points;
    }

    private static Vec3 perpendicular(Vec3 d, int choice) {
        Vec3 a = choice == 0
                ? (Math.abs(d.x) < 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0))
                : (Math.abs(d.y) < 0.9D ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
        Vec3 p = d.cross(a);
        double len = p.length();
        return len < 1.0E-6D ? new Vec3(0, 0, 0) : p.scale(1.0D / len);
    }

    private static Vec3 randomUnitVector() {
        double z = RANDOM.nextDouble() * 2.0D - 1.0D;
        double theta = RANDOM.nextDouble() * Math.PI * 2.0D;
        double r = Math.sqrt(Math.max(0.0D, 1.0D - z * z));
        return new Vec3(r * Math.cos(theta), z, r * Math.sin(theta));
    }

    private static Vec3 randomPointOnCubeShell(Vec3 centre, double half) {
        int axis = RANDOM.nextInt(3);
        double sign = RANDOM.nextBoolean() ? 1.0D : -1.0D;
        double a = (RANDOM.nextDouble() * 2.0D - 1.0D) * half;
        double b = (RANDOM.nextDouble() * 2.0D - 1.0D) * half;
        double x = axis == 0 ? sign * half : a;
        double y = axis == 1 ? sign * half : (axis == 0 ? a : b);
        double z = axis == 2 ? sign * half : b;
        return centre.add(x, y, z);
    }
}
