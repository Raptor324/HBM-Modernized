//? if forge {
package com.hbm_m.client.compat;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Item Transform Helper wraps every {@link BakedModel} in a thin delegate that does not forward
 * Forge extensions ({@code getQuads(..., ModelData, RenderType)}, etc.), which makes HBM OBJ /
 * multipart models invisible. This class re-wraps {@code hbm_m} {@code isCustomRenderer} models
 * after baking (always, not only when ITH is loaded) so BEWLR display transforms behave the same
 * with or without the helper mod.
 */
public final class ItemTransformHelperCompat {

    private static final String MOD_ID = "itemtransformhelper";

    private static Field ithOriginalModelField;
    private static boolean ithOriginalModelFieldChecked;

    private static Field forgeWrapperOriginalField;
    private static boolean forgeWrapperOriginalFieldChecked;

    private static Object updateLink;
    private static boolean updateLinkChecked;
    private static Field updateLinkModelField;
    private static Field updateLinkTransformsField;

    private ItemTransformHelperCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Run on {@link net.minecraftforge.client.event.ModelEvent.BakingCompleted} (LOWEST), after ITH.
     */
    public static void installDisplayTransformGuards(Map<ResourceLocation, BakedModel> models) {
        Map<ResourceLocation, BakedModel> replacements = new HashMap<>();
        for (Map.Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
            if (!RefStrings.MODID.equals(entry.getKey().getNamespace())) {
                continue;
            }
            BakedModel baked = entry.getValue();
            if (baked instanceof HbmItemDisplayWrapper) {
                continue;
            }

            BakedModel delegate = unwrapToDelegate(baked);
            if (!delegate.isCustomRenderer()) {
                continue;
            }

            replacements.put(entry.getKey(), new HbmItemDisplayWrapper(delegate));
        }

        if (!replacements.isEmpty()) {
            models.putAll(replacements);
            MainRegistry.LOGGER.info(
                    "[HBM] Installed display transform guards for {} custom item model(s).",
                    replacements.size());
        }
    }

    /**
     * Peel ITH / prior HBM display wrappers down to the real baked model.
     */
    @Nullable
    public static BakedModel unwrapToDelegate(@Nullable BakedModel model) {
        if (model == null) {
            return null;
        }

        BakedModel current = model;
        for (int depth = 0; depth < 12; depth++) {
            if (current instanceof HbmItemDisplayWrapper) {
                BakedModel inner = peelForgeWrapper(current);
                if (inner == null || inner == current) {
                    break;
                }
                current = inner;
                continue;
            }

            BakedModel ithInner = peelIthWrapper(current);
            if (ithInner != current) {
                current = ithInner;
                continue;
            }

            BakedModel frapiInner = com.hbm_m.client.render.AbstractPartBasedRenderer
                    .unwrapFabricForwardingModels(current);
            if (frapiInner != current) {
                current = frapiInner;
                continue;
            }

            if (current instanceof BakedModelWrapper<?>) {
                BakedModel inner = peelForgeWrapper(current);
                if (inner == null || inner == current) {
                    break;
                }
                current = inner;
                continue;
            }

            break;
        }
        return current;
    }

    /**
     * {@code display} for BEWLR: live ITH edits from the registry wrapper, otherwise JSON on the mesh delegate.
     */
    public static ItemTransforms resolveDisplayTransforms(@Nullable BakedModel displayModel, BakedModel meshDelegate) {
        if (displayModel != null) {
            ItemTransforms live = resolveLiveTransforms(displayModel);
            if (live != null) {
                return live;
            }
        }
        if (meshDelegate instanceof com.hbm_m.client.model.MissileBakedModel missile
                && missile.usesJsonDisplayInBewlr()) {
            return missile.getBewlrDisplayTransforms();
        }
        return meshDelegate.getTransforms();
    }

    @Nullable
    public static ItemTransforms resolveLiveTransforms(BakedModel wrapperInstance) {
        if (!isLoaded() || !ensureUpdateLinkFields()) {
            return null;
        }
        try {
            Object link = updateLink;
            if (link == null) {
                return null;
            }
            BakedModel target = (BakedModel) updateLinkModelField.get(link);
            ItemTransforms forced = (ItemTransforms) updateLinkTransformsField.get(link);
            if (target == wrapperInstance && forced != null) {
                return forced;
            }
        } catch (ReflectiveOperationException e) {
            MainRegistry.LOGGER.debug("[HBM] Item Transform Helper live transform lookup failed", e);
        }
        return null;
    }

    /**
     * Peel one Item Transform Helper wrapper layer, if present.
     */
    @Nullable
    public static BakedModel peelIthWrapper(@Nullable BakedModel model) {
        if (model == null || !isLoaded()) {
            return model;
        }
        if (!ensureIthOriginalModelField()) {
            return model;
        }

        Class<?> cls = model.getClass();
        while (cls != null && cls != Object.class) {
            if (isIthFlexibleCamera(cls)) {
                try {
                    return (BakedModel) ithOriginalModelField.get(model);
                } catch (ReflectiveOperationException e) {
                    return model;
                }
            }
            cls = cls.getSuperclass();
        }
        return model;
    }

    /**
     * Walk known wrapper layers until a {@link com.hbm_m.client.model.MissileBakedModel} is found.
     */
    @Nullable
    public static com.hbm_m.client.model.MissileBakedModel unwrapMissileDelegate(@Nullable BakedModel model) {
        if (model == null) {
            return null;
        }

        BakedModel current = unwrapToDelegate(model);
        for (int depth = 0; depth < 8; depth++) {
            if (current instanceof com.hbm_m.client.model.MissileBakedModel missileModel) {
                return missileModel;
            }

            BakedModel unwrapped = com.hbm_m.client.render.AbstractPartBasedRenderer
                    .unwrapFabricForwardingModels(current);
            if (unwrapped == current) {
                break;
            }
            current = unwrapped;
        }

        return current instanceof com.hbm_m.client.model.MissileBakedModel missileModel ? missileModel : null;
    }

    @Nullable
    private static BakedModel peelForgeWrapper(BakedModel model) {
        if (!ensureForgeWrapperOriginalField()) {
            return model;
        }
        try {
            return (BakedModel) forgeWrapperOriginalField.get(model);
        } catch (ReflectiveOperationException e) {
            return model;
        }
    }

    private static boolean ensureForgeWrapperOriginalField() {
        if (forgeWrapperOriginalFieldChecked) {
            return forgeWrapperOriginalField != null;
        }
        forgeWrapperOriginalFieldChecked = true;
        try {
            Field field = BakedModelWrapper.class.getDeclaredField("originalModel");
            field.setAccessible(true);
            forgeWrapperOriginalField = field;
        } catch (ReflectiveOperationException e) {
            MainRegistry.LOGGER.warn("[HBM] Could not access BakedModelWrapper.originalModel: {}", e.toString());
        }
        return forgeWrapperOriginalField != null;
    }

    private static boolean isIthFlexibleCamera(Class<?> cls) {
        return cls.getName().startsWith("itemtransformhelper.")
                && cls.getSimpleName().contains("FlexibleCamera");
    }

    private static boolean ensureIthOriginalModelField() {
        if (ithOriginalModelFieldChecked) {
            return ithOriginalModelField != null;
        }
        ithOriginalModelFieldChecked = true;
        try {
            Class<?> base = Class.forName("itemtransformhelper.ItemModelFlexibleCamera");
            Field field = base.getDeclaredField("originalModel");
            field.setAccessible(true);
            ithOriginalModelField = field;
        } catch (ReflectiveOperationException e) {
            MainRegistry.LOGGER.warn("[HBM] Could not access Item Transform Helper originalModel field: {}", e.toString());
        }
        return ithOriginalModelField != null;
    }

    private static boolean ensureUpdateLinkFields() {
        if (updateLinkChecked) {
            return updateLinkModelField != null && updateLinkTransformsField != null;
        }
        updateLinkChecked = true;
        try {
            Class<?> startup = Class.forName("itemtransformhelper.StartupClientOnly");
            Field handlerField = startup.getDeclaredField("modelBakeEventHandler");
            handlerField.setAccessible(true);
            Object handler = handlerField.get(null);

            Method getLink = handler.getClass().getMethod("getItemOverrideLink");
            updateLink = getLink.invoke(handler);

            Class<?> linkClass = Class.forName("itemtransformhelper.ItemModelFlexibleCamera$UpdateLink");
            updateLinkModelField = linkClass.getDeclaredField("itemModelToOverride");
            updateLinkModelField.setAccessible(true);
            updateLinkTransformsField = linkClass.getDeclaredField("forcedTransform");
            updateLinkTransformsField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            MainRegistry.LOGGER.warn("[HBM] Could not access Item Transform Helper UpdateLink: {}", e.toString());
        }
        return updateLinkModelField != null && updateLinkTransformsField != null;
    }

    /**
     * Forge {@link BakedModelWrapper} / {@code IForgeBakedModel#applyTransform} applies
     * {@link #getTransforms()} to the {@link PoseStack}. HBM BEWLR items ({@code isCustomRenderer})
     * apply the same transforms again in {@code renderByItem} — delegate a no-op to the inner model.
     */
    static final class HbmItemDisplayWrapper extends BakedModelWrapper<BakedModel> {

        HbmItemDisplayWrapper(BakedModel delegate) {
            super(delegate);
        }

        @Override
        public ItemTransforms getTransforms() {
            if (originalModel instanceof com.hbm_m.client.model.MissileBakedModel missile
                    && missile.usesJsonDisplayInBewlr()) {
                // BEWLR owns JSON display; Forge must not pre-apply (see resolveDisplayTransforms).
                return ItemTransforms.NO_TRANSFORMS;
            }
            ItemTransforms live = ItemTransformHelperCompat.resolveLiveTransforms(this);
            return live != null ? live : originalModel.getTransforms();
        }

        @Override
        public boolean isCustomRenderer() {
            return originalModel.isCustomRenderer();
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack,
                                         boolean applyLeftHandTransform) {
            // BEWLR applies JSON display; never let Forge/BakedModelWrapper apply it up-stack.
            if (originalModel.isCustomRenderer()) {
                return this;
            }
            getTransforms().getTransform(transformType).apply(applyLeftHandTransform, poseStack);
            return this;
        }
    }
}
//?}
