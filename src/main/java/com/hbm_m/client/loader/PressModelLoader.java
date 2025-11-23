package com.hbm_m.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.hbm_m.client.model.PressBakedModel;
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
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PressModelLoader implements IGeometryLoader<PressModelLoader.PressGeometry> {

    @Override
    public PressGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        ResourceLocation baseModel = ResourceLocation.parse(GsonHelper.getAsString(jsonObject, "base_model"));
        ResourceLocation headModel = ResourceLocation.parse(GsonHelper.getAsString(jsonObject, "head_model"));
        boolean flipV = GsonHelper.getAsBoolean(jsonObject, "flip_v", true);

        Vector3f headTranslation = new Vector3f(0.0F, 0.0F, 0.0F);
        float headTravel = 0.8F;
        if (jsonObject.has("head_transform")) {
            JsonObject headTransform = GsonHelper.getAsJsonObject(jsonObject, "head_transform");
            headTranslation = parseTranslation(headTransform);
            if (headTransform.has("travel")) {
                headTravel = headTransform.get("travel").getAsFloat();
            }
        }

        return new PressGeometry(baseModel, headModel, flipV, headTranslation, headTravel);
    }

    private static Vector3f parseTranslation(JsonObject headTransform) {
        if (!headTransform.has("translation")) {
            return new Vector3f(0.0F, 0.0F, 0.0F);
        }
        var array = GsonHelper.getAsJsonArray(headTransform, "translation");
        float x = array.size() > 0 ? array.get(0).getAsFloat() : 0.0F;
        float y = array.size() > 1 ? array.get(1).getAsFloat() : 0.0F;
        float z = array.size() > 2 ? array.get(2).getAsFloat() : 0.0F;
        return new Vector3f(x, y, z);
    }

    public static class PressGeometry implements IUnbakedGeometry<PressGeometry> {
        private final ResourceLocation baseModelLocation;
        private final ResourceLocation headModelLocation;
        private final boolean flipV;
        private final Vector3f headRestOffset;
        private final float headTravel;

        public PressGeometry(ResourceLocation baseModelLocation, ResourceLocation headModelLocation,
                             boolean flipV, Vector3f headRestOffset, float headTravel) {
            this.baseModelLocation = baseModelLocation;
            this.headModelLocation = headModelLocation;
            this.flipV = flipV;
            this.headRestOffset = headRestOffset;
            this.headTravel = headTravel;
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) { }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides,
                               ResourceLocation modelLocation) {
            Map<String, BakedModel> bakedParts = new HashMap<>();
            bakedParts.put("Base", bakeObjPart(baseModelLocation, context, baker, spriteGetter, overrides, modelLocation));
            bakedParts.put("Head", bakeObjPart(headModelLocation, context, baker, spriteGetter, overrides, modelLocation));

            ItemTransforms transforms = context.getTransforms();
            return new PressBakedModel(bakedParts, transforms, headRestOffset, headTravel);
        }

        private BakedModel bakeObjPart(ResourceLocation modelLocation, IGeometryBakingContext context,
                                       ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                                       ItemOverrides overrides, ResourceLocation rootModel) {
            try {
                ObjModel model = ObjLoader.INSTANCE.loadModel(
                    new ObjModel.ModelSettings(modelLocation, flipV, false, true, true, null)
                );
                return model.bake(context, baker, spriteGetter, identityState(), overrides, rootModel);
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to bake OBJ part for {}", modelLocation, e);
                throw new RuntimeException("Failed to bake press model part: " + modelLocation, e);
            }
        }

        private ModelState identityState() {
            return new ModelState() {
                @Override
                public @NotNull Transformation getRotation() {
                    return Transformation.identity();
                }
            };
        }
    }
}

