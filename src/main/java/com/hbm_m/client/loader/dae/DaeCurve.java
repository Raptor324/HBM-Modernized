package com.hbm_m.client.loader.dae;

/**
 * A single animation curve: a list of (time, value) keyframes sampled from a COLLADA
 * sampler source. LINEAR segments are interpolated, everything else is stepped and the
 * last keyframe is held indefinitely once the curve end is reached. This gives the
 * "hold last frame" behavior for free.
 */
public class DaeCurve {

    private final float[] times;
    private final float[][] values;
    /** Per segment, true = hold the previous value (non-linear interpolation) */
    private final boolean[] step;
    private final int stride;

    public DaeCurve(float[] times, float[][] values, boolean[] step, int stride) {
        this.times = times;
        this.values = values;
        this.step = step;
        this.stride = stride;
    }

    public float getStartTime() {
        return times.length > 0 ? times[0] : 0F;
    }

    public float getEndTime() {
        return times.length > 0 ? times[times.length - 1] : 0F;
    }

    public float[] sample(float time) {
        float[] result = new float[stride];

        if(times.length == 0) return result;
        if(times.length == 1) return values[0].clone();

        if(time <= times[0]) return values[0].clone();
        if(time >= times[times.length - 1]) return values[times.length - 1].clone();

        for(int i = 0; i < times.length - 1; i++) {
            if(time >= times[i] && time <= times[i + 1]) {
                if(step[i] || times[i + 1] <= times[i]) return values[i].clone();

                float t = (time - times[i]) / (times[i + 1] - times[i]);
                for(int j = 0; j < stride; j++) {
                    result[j] = values[i][j] + (values[i + 1][j] - values[i][j]) * t;
                }
                return result;
            }
        }

        return values[times.length - 1].clone();
    }
}
