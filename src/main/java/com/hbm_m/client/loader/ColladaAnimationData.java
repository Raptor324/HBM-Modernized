package com.hbm_m.client.loader;

import com.hbm_m.main.MainRegistry;
import org.joml.Matrix4f;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ColladaAnimationData {

    private static final Map<ResourceLocation, ColladaAnimationData> CACHE = new ConcurrentHashMap<>();
    
    private final Map<String, List<ColladaAnimationParser.AnimationChannel>> channels;
    private final Map<String, Matrix4f> inverseBindMatrices = new HashMap<>(); 
    private final Map<String, String> hierarchy;
    private final Map<String, List<String>> childrenMap = new HashMap<>();
    private final boolean zUp;
    private final float duration;
    
    private ColladaAnimationData(ColladaAnimationParser.Result result) {
        this.channels = result.animations;
        this.hierarchy = result.hierarchy;
        this.zUp = result.zUp;
        this.duration = result.duration;

        // Build children map
        for (Map.Entry<String, String> entry : hierarchy.entrySet()) {
            childrenMap.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        
        // Compute delta basis (Fix for exploding models)
        for (String objName : channels.keySet()) {
            Matrix4f firstFrame = getRawTransform(objName, 0.0f);
            if (firstFrame != null) {
                inverseBindMatrices.put(objName, new Matrix4f(firstFrame).invert());
            }
        }
    }

    // Default empty constructor
    private ColladaAnimationData() {
        this.channels = Collections.emptyMap();
        this.hierarchy = Collections.emptyMap();
        this.zUp = false;
        this.duration = 0f;
    }

    public static ColladaAnimationData getOrLoad(ResourceManager manager, ResourceLocation loc) {
        return CACHE.computeIfAbsent(loc, l -> load(manager, l));
    }

    private static ColladaAnimationData load(ResourceManager manager, ResourceLocation loc) {
        try {
            Optional<Resource> res = manager.getResource(loc);
            if (res.isPresent()) {
                ColladaAnimationParser.Result r = ColladaAnimationParser.parse(res.get().open());
                return new ColladaAnimationData(r);
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Anim Load Fail: " + loc, e);
        }
        return new ColladaAnimationData();
    }

    public Matrix4f getTransformMatrix(String object, float time) {
        Matrix4f current = getRawTransform(object, time);
        if (current == null) return null;

        Matrix4f invBind = inverseBindMatrices.get(object);
        if (invBind != null) {
            Matrix4f delta = new Matrix4f(current);
            delta.mul(invBind);
            return delta;
        }
        return current;
    }

    private Matrix4f getRawTransform(String object, float time) {
        List<ColladaAnimationParser.AnimationChannel> chs = channels.get(object);
        if (chs == null || chs.isEmpty()) return null;
        return chs.get(0).getInterpolatedMatrix(time);
    }

    // --- Missing methods required by DoorRenderer ---

    public boolean isZUp() {
        return zUp;
    }

    public String getParent(String childName) {
        return hierarchy.get(childName);
    }

    public List<String> getChildren(String parentName) {
        return childrenMap.getOrDefault(parentName, Collections.emptyList());
    }

    public float getDurationSeconds() {
        return duration;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}