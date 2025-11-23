package com.hbm_m.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.main.MainRegistry;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractObjPartModelLoader<T extends BakedModel> implements IGeometryLoader<AbstractObjPartModelLoader.ObjPartGeometry<T>> {

    protected abstract Set<String> getPartNames(JsonObject jsonObject);
    protected abstract T createBakedModel(HashMap<String, BakedModel> bakedParts, 
                                          ItemTransforms transforms,
                                          ResourceLocation modelLocation);

    @Override
    public ObjPartGeometry<T> read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        String modelStr = GsonHelper.getAsString(jsonObject, "model");
        MainRegistry.LOGGER.debug("{}: model string='{}'", this.getClass().getSimpleName(), modelStr);
        ResourceLocation model = ResourceLocation.parse(modelStr);
        Set<String> partNames = getPartNames(jsonObject);
        return new ObjPartGeometry<>(model, partNames, this);
    }

    public static class ObjPartGeometry<T extends BakedModel> implements IUnbakedGeometry<ObjPartGeometry<T>> {
        private final ResourceLocation modelLocation;
        private final Set<String> partNames;
        private final AbstractObjPartModelLoader<T> loader;

        public ObjPartGeometry(ResourceLocation modelLocation, Set<String> partNames, AbstractObjPartModelLoader<T> loader) {
            this.modelLocation = modelLocation;
            this.partNames = partNames;
            this.loader = loader;
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                             Function<Material, TextureAtlasSprite> spriteGetter,
                             ModelState modelState, ItemOverrides overrides,
                             ResourceLocation modelName) {
            ObjModel model = loadObjModel();
            HashMap<String, BakedModel> bakedParts = bakeParts(model, context, baker, spriteGetter, overrides, modelName);
            ensureBasePart(model, bakedParts, context, baker, spriteGetter, overrides, modelName);

            MainRegistry.LOGGER.info("{}: Total baked parts: {}", loader.getClass().getSimpleName(), bakedParts.size());
            return loader.createBakedModel(bakedParts, context.getTransforms(), modelLocation);
        }

        private ObjModel loadObjModel() {
            try {
                ObjModel model = ObjLoader.INSTANCE.loadModel(
                    new ObjModel.ModelSettings(modelLocation, true, false, true, true, null)
                );
                MainRegistry.LOGGER.info("{}: Successfully loaded OBJ model: {}",
                    loader.getClass().getSimpleName(), modelLocation);
                return model;
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to load OBJ model: " + modelLocation, e);
                throw new RuntimeException("Не удалось загрузить OBJ модель: " + modelLocation, e);
            }
        }

        private HashMap<String, BakedModel> bakeParts(ObjModel model, IGeometryBakingContext context,
                                                      ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                                                      ItemOverrides overrides, ResourceLocation modelName) {
            HashMap<String, BakedModel> bakedParts = new HashMap<>();
            ModelState identityState = createIdentityState();

            for (String partName : partNames) {
                SinglePartBakingContext partContext = new SinglePartBakingContext(context, partName);
                BakedModel bakedPart = model.bake(partContext, baker, spriteGetter,
                    identityState, overrides, modelName);
                bakedParts.put(partName, bakedPart);
            }

            return bakedParts;
        }

        private void ensureBasePart(ObjModel model, HashMap<String, BakedModel> bakedParts,
                                   IGeometryBakingContext context, ModelBaker baker,
                                   Function<Material, TextureAtlasSprite> spriteGetter,
                                   ItemOverrides overrides, ResourceLocation modelName) {
            if (!bakedParts.containsKey("Base")) {
                bakedParts.put("Base", model.bake(
                    new SinglePartBakingContext(context, "Base"),
                    baker, spriteGetter, createIdentityState(), overrides, modelName
                ));
            }
        }

        private ModelState createIdentityState() {
            return new ModelState() {
                @Override
                public @NotNull Transformation getRotation() {
                    return Transformation.identity();
                }
            };
        }
    }

    protected static class SinglePartBakingContext implements IGeometryBakingContext {
        private final IGeometryBakingContext parent;
        private final String visiblePart;

        public SinglePartBakingContext(IGeometryBakingContext parent, String visiblePart) {
            this.parent = parent;
            this.visiblePart = visiblePart;
        }

        @Override public String getModelName() { return parent.getModelName(); }
        @Override public boolean isGui3d() { return parent.isGui3d(); }
        @Override public boolean useBlockLight() { return parent.useBlockLight(); }
        @Override public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }
        @Override public ItemTransforms getTransforms() { return parent.getTransforms(); }
        @Override public Material getMaterial(String name) { return parent.getMaterial(name); }
        @Override public Transformation getRootTransform() { return parent.getRootTransform(); }
        @Override public boolean hasMaterial(String name) { return parent.hasMaterial(name); }
        @Override public ResourceLocation getRenderTypeHint() { return parent.getRenderTypeHint(); }

        @Override
        public boolean isComponentVisible(String component, boolean fallback) {
            return component.equals(this.visiblePart) ||
                   component.startsWith(this.visiblePart + "/");
        }
    }
}
