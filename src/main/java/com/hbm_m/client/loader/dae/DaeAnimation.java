package com.hbm_m.client.loader.dae;

import java.util.HashMap;
import java.util.Map;

/**
 * A named animation clip: a set of curves targeting node transforms. The clip is a
 * series of channels, each targeting a node name and a transform property
 * (e.g. {@code "spin.ANGLE"}, {@code "location.X"}, {@code "matrix"}).
 */
public class DaeAnimation {

    public final String name;
    private final Map<String, Map<String, DaeCurve>> channels = new HashMap<>();

    private float startTime = Float.MAX_VALUE;
    private float endTime = -Float.MAX_VALUE;

    public DaeAnimation(String name) {
        this.name = name;
    }

    public void addChannel(String nodeName, String property, DaeCurve curve) {
        channels.computeIfAbsent(nodeName, k -> new HashMap<>()).put(property, curve);
        startTime = Math.min(startTime, curve.getStartTime());
        endTime = Math.max(endTime, curve.getEndTime());
    }

    public boolean isEmpty() {
        return channels.isEmpty();
    }

    public Map<String, DaeCurve> getChannels(String nodeName) {
        return channels.get(nodeName);
    }

    public float getStartTime() {
        return startTime == Float.MAX_VALUE ? 0F : startTime;
    }

    public float getEndTime() {
        return endTime == -Float.MAX_VALUE ? 0F : endTime;
    }

    public float getDuration() {
        return Math.max(0F, getEndTime() - getStartTime());
    }
}
