package com.remag.rcc.client;

import com.remag.rcc.registry.CompressionConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates the Compression Manipulator's crafting lightning and completion-pulse <em>geometry</em> - jagged bolt
 * polylines and a radial starburst - as plain lists of world-space points. This class draws nothing and knows nothing
 * about rendering; {@link CompressionCoreRenderer} consumes these points and renders them inside its block-entity
 * render pass.
 *
 * <p>Why render in the BER rather than as particles (vanilla, Lodestone or AAA): {@code LevelRenderer.renderLevel}
 * composites in the fixed order opaque -> cutout -> entities -> <em>block entities</em> -> <em>translucent</em> (our
 * glass) -> particles. Anything depth-tested that draws after the glass - which is every particle path - is culled by
 * the glass's depth. Block entities draw <em>before</em> the glass, so whatever they put in the framebuffer is
 * blended over by the translucent panes afterwards (the seed cube is visible through the glass for exactly this
 * reason). Drawing the bolts as BE-pass geometry is therefore the one shader-free, mixin-free, dependency-free way to
 * get them to read as being <em>inside</em> the sealed chamber, while the opaque frame and core still occlude them.
 *
 * <p>A bolt is a random jagged polyline: {@link CompressionConfig#BOLT_STEPS} points walked from the seed cube surface
 * to a random spot on the chamber wall, each interior point pushed sideways so every volley is re-shaped differently -
 * the "random every bolt" quality we wanted to keep. All generation is deterministic-free (uses its own {@link Random});
 * it is called from the client render loop only.
 */
public final class CompressionEffects {
    /** Half-extent of the zap target shell: the inner face of the glass layer (interior air is 3x3x3 about the core). */
    private static final double WALL_HALF = 1.45D;

    private static final Random RANDOM = new Random();

    private CompressionEffects() {
    }

    /**
     * Builds one volley of jagged bolts from the spinning seed cube out to random points on the chamber walls.
     *
     * @param centre world-space centre of the core (block centre)
     * @return one polyline per bolt, in world space; each array is the ordered vertices of that bolt
     */
    public static List<Vec3[]> generateBolts(Vec3 centre) {
        List<Vec3[]> bolts = new ArrayList<>(CompressionConfig.BOLTS_PER_ZAP);
        double cubeRadius = CompressionConfig.SEED_CUBE_SCALE * 0.5D;
        for (int i = 0; i < CompressionConfig.BOLTS_PER_ZAP; i++) {
            // Pick a point on the inner wall, then start the bolt on the cube surface along that same ray so it reads
            // as the cube discharging outward rather than two unrelated random points being connected.
            Vec3 wall = randomPointOnCubeShell(centre, WALL_HALF);
            Vec3 outward = wall.subtract(centre);
            double outwardLen = outward.length();
            if (outwardLen < 1.0E-4D) {
                continue;
            }
            Vec3 start = centre.add(outward.scale(cubeRadius / outwardLen));
            bolts.add(jaggedBolt(start, wall));
        }
        return bolts;
    }

    /**
     * Builds the direction set for one completion flash: {@link CompressionConfig#PULSE_BURST} unit vectors radiating
     * from the core. Only the (fixed) directions are generated here; {@link CompressionCoreRenderer} turns them into an
     * expanding shell of spokes each frame, so the flash grows outward past the seed cube as it fades.
     *
     * @return an array of unit directions, one per flash spoke
     */
    public static Vec3[] generatePulseDirections() {
        int count = Math.max(1, CompressionConfig.PULSE_BURST);
        Vec3[] dirs = new Vec3[count];
        for (int i = 0; i < count; i++) {
            dirs[i] = randomUnitVector();
        }
        return dirs;
    }

    /**
     * A jagged polyline between {@code start} and {@code end} with {@link CompressionConfig#BOLT_STEPS} segments. Interior
     * vertices are pushed sideways by up to {@link CompressionConfig#BOLT_JAG} blocks (tapered to zero at both endpoints so
     * the bolt stays anchored cube->wall); that random re-shaping every call is what makes each bolt unique.
     */
    private static Vec3[] jaggedBolt(Vec3 start, Vec3 end) {
        Vec3 axis = end.subtract(start);
        double total = axis.length();
        if (total < 1.0E-4D) {
            return new Vec3[]{start, end};
        }
        int steps = Math.max(1, CompressionConfig.BOLT_STEPS);
        Vec3 dir = axis.scale(1.0D / total);
        Vec3 perp1 = perpendicular(dir, 0);
        Vec3 perp2 = perpendicular(dir, 1);
        double jag = CompressionConfig.BOLT_JAG;

        Vec3[] points = new Vec3[steps + 1];
        points[0] = start;
        for (int s = 1; s <= steps; s++) {
            double t = (double) s / steps;
            Vec3 point = start.add(axis.scale(t));
            // Only jitter interior points; the sin taper keeps both ends exactly on the cube / wall.
            if (s < steps && jag > 0.0D) {
                double amt = jag * Math.sin(t * Math.PI);
                point = point
                        .add(perp1.scale((RANDOM.nextDouble() * 2.0D - 1.0D) * amt))
                        .add(perp2.scale((RANDOM.nextDouble() * 2.0D - 1.0D) * amt));
            }
            points[s] = point;
        }
        return points;
    }

    /**
     * A unit vector perpendicular to {@code d}. {@code choice} picks one of the two reference axes so callers get two
     * independent perpendicular directions spanning the jitter plane.
     */
    private static Vec3 perpendicular(Vec3 d, int choice) {
        Vec3 a = choice == 0
                ? (Math.abs(d.x) < 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0))
                : (Math.abs(d.y) < 0.9D ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
        Vec3 p = d.cross(a);
        double len = p.length();
        return len < 1.0E-6D ? new Vec3(0, 0, 0) : p.scale(1.0D / len);
    }

    /** A uniformly distributed direction on the unit sphere (for the pulse starburst). */
    private static Vec3 randomUnitVector() {
        double z = RANDOM.nextDouble() * 2.0D - 1.0D;
        double theta = RANDOM.nextDouble() * Math.PI * 2.0D;
        double r = Math.sqrt(Math.max(0.0D, 1.0D - z * z));
        return new Vec3(r * Math.cos(theta), z, r * Math.sin(theta));
    }

    /** A uniformly distributed point on the axis-aligned cube shell of the given half-extent centred on {@code centre}. */
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
