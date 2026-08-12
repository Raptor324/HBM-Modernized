package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.LoaderHooks;
import com.mojang.math.Transformation;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

//? if < 1.21.1 {
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.obj.ObjModel;

/**
 * Loads soyuz.obj (single multi-material mesh, ~52 blocks tall) into a
 * {@link SoyuzRocketBakedModel}. Materials (booster, boosterside, les, ...)
 * are resolved straight from the outer model json's own "textures" map
 * (unlike the launcher, this is one part with many materials, not many
 * parts with one material each - so no per-part texture override needed).
 */
//?} else {
/*import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.obj.ObjModel;
*///?}

public class SoyuzRocketModelLoader implements IGeometryLoader<SoyuzRocketModelLoader.Geometry> {

    //? if < 1.21.1 {
    private static final ResourceLocation MODEL = new ResourceLocation(RefStrings.MODID, "models/soyuz/soyuz.obj");
    //?} else {
    /*private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/soyuz/soyuz.obj");
    *///?}

    @Override
    public Geometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        return new Geometry();
    }

    public static class Geometry implements IUnbakedGeometry<Geometry> {

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {}

        //? if < 1.21.1 {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            return doBake(context, baker, spriteGetter, modelState, overrides, modelLocation);
        }
        //?} else {
        /*@Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            ResourceLocation modelLocation = ResourceLocation.parse(context.getModelName());
            return doBake(context, baker, spriteGetter, modelState, overrides, modelLocation);
        }
        *///?}

        private BakedModel doBake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelName) {
            HashMap<String, BakedModel> bakedParts = new HashMap<>();
            ModelState identityState = new ModelState() {
                @Override
                public @NotNull Transformation getRotation() {
                    return Transformation.identity();
                }
            };

            try {
                ObjModel objModel = LoaderHooks.loadObjModel(MODEL, true);
                BakedModel baked = LoaderHooks.bakeObjModel(objModel, context, baker, spriteGetter, identityState, overrides, modelName);
                bakedParts.put(SoyuzRocketBakedModel.ROCKET, baked);
            } catch (Exception e) {
                MainRegistry.LOGGER.error("SoyuzRocketModelLoader: failed to load/bake soyuz.obj", e);
            }

            return new SoyuzRocketBakedModel(bakedParts, context.getTransforms());
        }
    }
}