package com.integral.littlevecx.animation;

import java.util.List;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.littletiles.common.structure.animation.AnimationKey;
import com.creativemd.littletiles.common.structure.animation.AnimationTimeline;
import com.creativemd.littletiles.common.structure.animation.ValueTimeline;

public final class LittleVecXAnimationLayerCompiler {

    public static final int DEFAULT_DURATION = 10;
    public static final int DEFAULT_OFFGRID = 16;
    private static final int DEFAULT_INTERPOLATION = 0;
    private static final int MAX_COMPILED_OFFGRID = 4096;

    private LittleVecXAnimationLayerCompiler() {}

    public static final class CompiledAnimation {
        public final boolean hasLayers;
        public final int duration;
        public final int offGrid;
        public final AnimationTimeline timeline;
        @Nullable
        public final ValueTimeline rotX;
        @Nullable
        public final ValueTimeline rotY;
        @Nullable
        public final ValueTimeline rotZ;
        @Nullable
        public final ValueTimeline offX;
        @Nullable
        public final ValueTimeline offY;
        @Nullable
        public final ValueTimeline offZ;

        private CompiledAnimation(boolean hasLayers, int duration, int offGrid, AnimationTimeline timeline,
                @Nullable ValueTimeline rotX, @Nullable ValueTimeline rotY, @Nullable ValueTimeline rotZ,
                @Nullable ValueTimeline offX, @Nullable ValueTimeline offY, @Nullable ValueTimeline offZ) {
            this.hasLayers = hasLayers;
            this.duration = duration;
            this.offGrid = offGrid;
            this.timeline = timeline;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
        }
    }

    public static CompiledAnimation compile(@Nullable List<LittleVecXAnimationLayer> layers) {
        TreeMap<Integer, Double> rotXPoints = new TreeMap<>();
        TreeMap<Integer, Double> rotYPoints = new TreeMap<>();
        TreeMap<Integer, Double> rotZPoints = new TreeMap<>();
        TreeMap<Integer, Double> offXPoints = new TreeMap<>();
        TreeMap<Integer, Double> offYPoints = new TreeMap<>();
        TreeMap<Integer, Double> offZPoints = new TreeMap<>();

        boolean hasLayers = false;
        int duration = 1;
        int compiledOffGrid = DEFAULT_OFFGRID;
        int tickOffset = 0;

        if (layers != null) {
            for (LittleVecXAnimationLayer layer : layers) {
                hasLayers = true;
                duration = Math.max(duration, tickOffset + layer.getSafeDuration());
                compiledOffGrid = lcmCapped(compiledOffGrid, layer.getSafeOffGrid());
                tickOffset += layer.getSafeDuration();
            }
        }

        if (compiledOffGrid <= 0)
            compiledOffGrid = DEFAULT_OFFGRID;

        tickOffset = 0;
        if (layers != null) {
            for (LittleVecXAnimationLayer layer : layers) {
                double offScale = compiledOffGrid / (double) layer.getSafeOffGrid();

                addPoints(layer.rotX, tickOffset, 1.0D, rotXPoints);
                addPoints(layer.rotY, tickOffset, 1.0D, rotYPoints);
                addPoints(layer.rotZ, tickOffset, 1.0D, rotZPoints);

                addPoints(layer.offX, tickOffset, offScale, offXPoints);
                addPoints(layer.offY, tickOffset, offScale, offYPoints);
                addPoints(layer.offZ, tickOffset, offScale, offZPoints);
                tickOffset += layer.getSafeDuration();
            }
        }

        PairList<AnimationKey, ValueTimeline> values = new PairList<>();

        ValueTimeline rotX = createTimeline(DEFAULT_INTERPOLATION, rotXPoints);
        ValueTimeline rotY = createTimeline(DEFAULT_INTERPOLATION, rotYPoints);
        ValueTimeline rotZ = createTimeline(DEFAULT_INTERPOLATION, rotZPoints);
        ValueTimeline offX = createTimeline(DEFAULT_INTERPOLATION, offXPoints);
        ValueTimeline offY = createTimeline(DEFAULT_INTERPOLATION, offYPoints);
        ValueTimeline offZ = createTimeline(DEFAULT_INTERPOLATION, offZPoints);

        if (rotX != null)
            values.add(AnimationKey.rotX, rotX);
        if (rotY != null)
            values.add(AnimationKey.rotY, rotY);
        if (rotZ != null)
            values.add(AnimationKey.rotZ, rotZ);
        if (offX != null)
            values.add(AnimationKey.offX, offX);
        if (offY != null)
            values.add(AnimationKey.offY, offY);
        if (offZ != null)
            values.add(AnimationKey.offZ, offZ);

        return new CompiledAnimation(
                hasLayers,
                duration,
                compiledOffGrid,
                new AnimationTimeline(duration, values),
                rotX,
                rotY,
                rotZ,
                offX,
                offY,
                offZ
        );
    }

    private static void addPoints(@Nullable ValueTimeline timeline, int tickOffset, double factor, TreeMap<Integer, Double> target) {
        if (timeline == null)
            return;

        PairList<Integer, Double> points = timeline.getPointsCopy();
        if (points == null || points.isEmpty())
            return;

        for (int i = 0; i < points.size(); i++) {
            int tick = points.get(i).key + tickOffset;
            double value = points.get(i).value * factor;
            target.put(tick, value);
        }
    }

    @Nullable
    private static ValueTimeline createTimeline(int interpolation, TreeMap<Integer, Double> points) {
        if (points.isEmpty())
            return null;

        PairList<Integer, Double> pairs = new PairList<>();
        for (Integer tick : points.keySet())
            pairs.add(tick, points.get(tick));
        return ValueTimeline.create(interpolation, pairs);
    }

    private static int lcmCapped(int a, int b) {
        if (a <= 0)
            return b <= 0 ? DEFAULT_OFFGRID : b;
        if (b <= 0)
            return a;
        int gcd = gcd(a, b);
        long value = (long) a / gcd * (long) b;
        if (value > MAX_COMPILED_OFFGRID)
            return Math.max(a, b);
        return (int) value;
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int tmp = a % b;
            a = b;
            b = tmp;
        }
        return a == 0 ? 1 : a;
    }
}
