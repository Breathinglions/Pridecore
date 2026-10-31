package com.prideregion.scenes.client;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

/**
 * Non-stop, arc-length-paced cinematic camera track through captured poses.
 * All waypoints are passed without easing/stopping at each individual point.
 * Only the very beginning and end of the ENTIRE tour gently accelerate and
 * decelerate. Each intermediate point keeps its captured yaw and pitch.
 *
 * This renderer does not move the player, NPCs, blocks or encounter state.
 */
public final class CinematicFlythroughPath {
    public record Keyframe(Vec3d position, float yaw, float pitch) {}
    public record Sample(Vec3d position, float yaw, float pitch) {}
    private record Marker(double distance, int segment, double t) {}

    private static final int MIN_SAMPLES_PER_SEGMENT = 32;
    private static final int MAX_SAMPLES_PER_SEGMENT = 128;
    private static final double END_RAMP = 0.10;

    private final List<Keyframe> points;
    private final double[] continuousYaw;
    private final List<Marker> markers;
    private final double length;

    public CinematicFlythroughPath(List<Keyframe> waypoints) {
        if (waypoints == null || waypoints.size() < 2 || waypoints.size() > 32)
            throw new IllegalArgumentException("Flythrough needs 2-32 waypoints");
        this.points = List.copyOf(waypoints);
        for (Keyframe key : points) {
            if (key == null || key.position() == null
                || !Double.isFinite(key.yaw()) || !Double.isFinite(key.pitch()))
                throw new IllegalArgumentException("Invalid cinematic flythrough waypoint");
        }
        continuousYaw = new double[points.size()];
        continuousYaw[0] = points.getFirst().yaw();
        for (int i = 1; i < points.size(); i++)
            continuousYaw[i] = continuousYaw[i - 1]
                + MathHelper.wrapDegrees(points.get(i).yaw()
                    - points.get(i - 1).yaw());

        ArrayList<Marker> measured = new ArrayList<>();
        measured.add(new Marker(0, 0, 0));
        Vec3d last = points.getFirst().position();
        double traversed = 0;
        for (int segment = 0; segment < points.size() - 1; segment++) {
            double chord = points.get(segment).position()
                .distanceTo(points.get(segment + 1).position());
            // Use dense spatial samples for reliable distance pacing even
            // with unevenly spaced capture points.
            int steps = (int) MathHelper.clamp(Math.ceil(chord * 5),
                MIN_SAMPLES_PER_SEGMENT, MAX_SAMPLES_PER_SEGMENT);
            for (int j = 1; j <= steps; j++) {
                double t = (double) j / steps;
                Vec3d next = position(segment, t);
                traversed += next.distanceTo(last);
                measured.add(new Marker(traversed, segment, t));
                last = next;
            }
        }
        if (!Double.isFinite(traversed) || traversed < 0.1)
            throw new IllegalArgumentException(
                "Flythrough waypoints need distinct positions, at least 0.1 blocks apart");
        this.markers = List.copyOf(measured);
        this.length = traversed;
    }

    public double distanceBlocks() { return length; }

    /** Deterministic path frame for 0..1 progress in time. */
    public Sample sample(double fraction) {
        double time = MathHelper.clamp(fraction, 0.0, 1.0);
        double distance = easeAtEndsOnly(time) * length;
        int lo = 0, hi = markers.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (markers.get(mid).distance() < distance) lo = mid + 1;
            else hi = mid;
        }
        if (lo == 0) return frame(0, 0);
        Marker after = markers.get(lo);
        Marker before = markers.get(lo - 1);
        double interval = after.distance() - before.distance();
        double w = interval <= 1e-9 ? 1.0
            : MathHelper.clamp((distance - before.distance()) / interval, 0, 1);
        // Adjacent arc samples generally share the same segment; on the
        // exact waypoint boundary, the predecessor ends the prior segment.
        if (before.segment() != after.segment())
            return frame(after.segment(), after.t() * w);
        return frame(after.segment(), MathHelper.lerp(w, before.t(), after.t()));
    }

    private Sample frame(int segment, double t) {
        int n = points.size();
        int i = Math.min(Math.max(0, segment), n - 2);
        double u = MathHelper.clamp(t, 0, 1);
        double yaw = scalar(continuousYaw, i, u);
        double[] pitch = new double[n];
        for (int k = 0; k < n; k++) pitch[k] = points.get(k).pitch();
        return new Sample(position(i, u), (float) yaw,
            MathHelper.clamp((float) scalar(pitch, i, u), -85, 85));
    }

    /** Cubic Hermite path with continuous tangents and reduced corner overshoot. */
    private Vec3d position(int segment, double t) {
        Vec3d a = points.get(segment).position();
        Vec3d b = points.get(segment + 1).position();
        Vec3d m0 = tangent(segment);
        Vec3d m1 = tangent(segment + 1);
        double t2 = t * t, t3 = t2 * t;
        return a.multiply(2 * t3 - 3 * t2 + 1)
            .add(m0.multiply(t3 - 2 * t2 + t))
            .add(b.multiply(-2 * t3 + 3 * t2))
            .add(m1.multiply(t3 - t2));
    }

    private Vec3d tangent(int index) {
        int last = points.size() - 1;
        Vec3d center = points.get(index).position();
        if (index == 0)
            return points.get(1).position().subtract(center).multiply(0.75);
        if (index == last)
            return center.subtract(points.get(last - 1).position()).multiply(0.75);
        Vec3d in = center.subtract(points.get(index - 1).position());
        Vec3d out = points.get(index + 1).position().subtract(center);
        double a = in.length(), b = out.length();
        if (a < 1e-7 || b < 1e-7) return Vec3d.ZERO;
        Vec3d average = in.multiply(1 / a).add(out.multiply(1 / b));
        if (average.lengthSquared() < 1e-7) return Vec3d.ZERO;
        // Nonzero tangents pass waypoints without the per-shot full stop.
        return average.normalize().multiply(Math.min(a, b) * 0.65);
    }

    private static double scalar(double[] values, int i, double t) {
        double a = values[i], b = values[i + 1];
        double in = i == 0 ? b - a
            : 0.5 * (values[i + 1] - values[i - 1]);
        double out = i + 2 >= values.length ? b - a
            : 0.5 * (values[i + 2] - values[i]);
        double t2 = t * t, t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * a
            + (t3 - 2 * t2 + t) * in
            + (-2 * t3 + 3 * t2) * b
            + (t3 - t2) * out;
    }

    private static double easeAtEndsOnly(double time) {
        if (time <= END_RAMP)
            return time * time / (2 * END_RAMP * (1 - END_RAMP));
        if (time >= 1 - END_RAMP) {
            double remaining = 1 - time;
            return 1 - remaining * remaining
                / (2 * END_RAMP * (1 - END_RAMP));
        }
        return (time - END_RAMP / 2) / (1 - END_RAMP);
    }
}
