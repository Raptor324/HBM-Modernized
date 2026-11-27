package com.hbm_m.platform.legacy.client.model;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.resources.model.cuboid.ItemTransforms;

/**
 * [26.x] Ersatz für {@code com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat}.
 * ItemTransformHelper gibt es für 26.x nicht, und BakedModel-Wrapper existieren nicht mehr;
 * die Methoden liefern daher das Modell bzw. dessen eigene Transforms direkt.
 */
public final class ItemTransformHelperCompat {

    private ItemTransformHelperCompat() {}

    public static boolean isLoaded() { return false; }

    public static <K> void installDisplayTransformGuards(Map<K, BakedModel> models) {}

    public static @Nullable BakedModel unwrapToDelegate(@Nullable BakedModel model) { return model; }

    public static @Nullable BakedModel peelIthWrapper(@Nullable BakedModel model) { return model; }

    public static ItemTransforms resolveDisplayTransforms(@Nullable BakedModel displayModel, BakedModel meshDelegate) {
        return displayModel != null ? displayModel.getTransforms() : meshDelegate.getTransforms();
    }

    public static boolean pipelineAppliesDisplayTransforms(@Nullable BakedModel displayModel) { return false; }

    public static ItemTransforms resolveLiveTransforms(BakedModel wrapperInstance) { return wrapperInstance.getTransforms(); }

    public static @Nullable MissileBakedModel unwrapMissileDelegate(@Nullable BakedModel model) {
        return model instanceof MissileBakedModel m ? m : null;
    }
}
