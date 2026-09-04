package com.hbm_m.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.BoxCableBakedModel;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
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

/**
 * Лоадер модели box-кабеля (порт PowerCableBox): читает "size" (0-4) и отдаёт
 * {@link BoxCableBakedModel}, рисующий короб с per-face спрайтами по маске подключений.
 */
public class BoxCableModelLoader implements IGeometryLoader<BoxCableModelLoader.BoxCableGeometry> {

    @Override
    public BoxCableGeometry read(JsonObject json, JsonDeserializationContext context) {
        int size = json.has("size") ? json.get("size").getAsInt() : 0;
        return new BoxCableGeometry(size);
    }

    public static class BoxCableGeometry implements IUnbakedGeometry<BoxCableGeometry> {
        private final int size;

        public BoxCableGeometry(int size) {
            this.size = Math.max(0, Math.min(4, size));
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        }

        //? if < 1.21.1 {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            return new BoxCableBakedModel(this.size, context.getTransforms());
        }
        //?} else {
        /*@Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            return new BoxCableBakedModel(this.size, context.getTransforms());
        }
        *///?}
    }
}
