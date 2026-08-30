package com.hbm_m.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.TemplateBakedModel;
import com.hbm_m.platform.LoaderHooks;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

//? if < 1.21.1 {
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
//?} else {
/*import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
*///?}

public class TemplateModelLoader implements IGeometryLoader<TemplateModelLoader.TemplateGeometry> {

    @Override
    public TemplateGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        jsonObject.remove("loader");
        BlockModel baseModel = deserializationContext.deserialize(jsonObject, BlockModel.class);
        return new TemplateGeometry(baseModel);
    }

    public static class TemplateGeometry implements IUnbakedGeometry<TemplateGeometry> {
        private final BlockModel baseModel;

        public TemplateGeometry(BlockModel baseModel) {
            this.baseModel = baseModel;
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
            this.baseModel.resolveParents(modelGetter);
        }

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

        private BakedModel doBake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            BakedModel originalModel = LoaderHooks.bakeBlockModel(this.baseModel, baker, spriteGetter, modelState, modelLocation, true);
            return new TemplateBakedModel(originalModel, baker, this.baseModel);
        }
    }
}