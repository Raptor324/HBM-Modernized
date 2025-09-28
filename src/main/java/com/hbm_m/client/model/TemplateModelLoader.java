package com.hbm_m.client.model;

// Загрузчик модели, который читает JSON, десериализует его как стандартную BlockModel,
// а затем оборачивает в наш кастомный TemplateBakedModel.
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

public class TemplateModelLoader implements IGeometryLoader<TemplateModelLoader.TemplateGeometry> {

    // JsonObject jsonObject - это наш assembly_template.json
    @Override
    public TemplateGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {

        // Перед тем, как передать json дальше, мы удаляем из него ключ "loader".
        // Это предотвращает бесконечную рекурсию, так как стандартный десериализатор
        // больше не будет видеть наш кастомный загрузчик и не вызовет нас снова.
        jsonObject.remove("loader");

        // Теперь мы можем безопасно десериализовать "очищенный" json как стандартную модель.
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

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            BakedModel originalModel = this.baseModel.bake(baker, this.baseModel, spriteGetter, modelState, modelLocation, true);
            return new TemplateBakedModel(originalModel);
        }
    }
}