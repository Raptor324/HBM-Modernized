package com.hbm_m.client.loader;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.hbm_m.client.loader.dae.DaeAnimation;
import com.hbm_m.client.loader.dae.DaeModel;
import com.hbm_m.client.loader.dae.DaeQuadBaker;
import com.hbm_m.client.model.DaeBakedModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

//? if < 1.21.1 {
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
//?} else {
/*import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
*///?}

/**
 * Bakes a COLLADA (.dae) model into a static item model. The model is sampled at the
 * given {@code time} (seconds) of its "animation" clip, so the item can show the door
 * in its closed (t=0, default) or open pose. Used by the transition seal block model,
 * which is only rendered as an item / particle — the in-world block goes through the
 * block entity renderer.
 */

public class DaeModelLoader implements IGeometryLoader<DaeModelLoader.DaeUnbakedGeometry> {

    private static final String CLIP_NAME = "animation";
    private static final String FALLBACK_TEXTURE = "hbm_m:block/doors/transition_seal";

    @Override
    public DaeUnbakedGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        ResourceLocation model = ResourceLocation.tryParse(GsonHelper.getAsString(jsonObject, "model"));
        float time = GsonHelper.getAsFloat(jsonObject, "time", 0F);
        return new DaeUnbakedGeometry(model, time);
    }

    public static class DaeUnbakedGeometry implements IUnbakedGeometry<DaeUnbakedGeometry> {
        private final ResourceLocation modelLocation;
        private final float time;

        public DaeUnbakedGeometry(ResourceLocation modelLocation, float time) {
            this.modelLocation = modelLocation;
            this.time = time;
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) { }

        //? if < 1.21.1 {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelName) {
            return doBake(context, baker, spriteGetter, modelState, overrides, modelName);
        }
        //?} else {
        /*@Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            ResourceLocation modelName = ResourceLocation.parse(context.getModelName());
            return doBake(context, baker, spriteGetter, modelState, overrides, modelName);
        }
        *///?}

        private BakedModel doBake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelName) {
            Material material = context.hasMaterial("default")
                    ? context.getMaterial("default")
                    : new Material(TextureAtlas.LOCATION_BLOCKS, RefStrings.resourceLocation(FALLBACK_TEXTURE));
            TextureAtlasSprite sprite = spriteGetter.apply(material);

            DaeModel model = DaeModel.load(modelLocation);
            DaeAnimation clip = model.animations.get(CLIP_NAME);
            if (clip == null && !model.animations.isEmpty()) {
                clip = model.animations.values().iterator().next();
            }

            List<BakedQuad> quads = DaeQuadBaker.bakeScene(model, clip, time, sprite);
            MainRegistry.LOGGER.info("DaeModelLoader: baked {} quads for {}", quads.size(), modelLocation);
            return new DaeBakedModel(quads, sprite, context.getTransforms());
        }
    }
}