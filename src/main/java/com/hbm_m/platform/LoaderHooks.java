package com.hbm_m.platform;

import java.util.function.Function;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;

//? if < 1.21.1 {
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;
//?} else {
/*import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.obj.ObjLoader;
import net.neoforged.neoforge.client.model.obj.ObjModel;
*///?}

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class LoaderHooks {
    private LoaderHooks() {}

    /**
     * Кросс-версионная загрузка OBJ модели.
     * Исправляет баг 1.20.1 Forge, где flipV ошибочно передавался вместо automaticCulling.
     */
    public static ObjModel loadObjModel(ResourceLocation modelLocation, boolean flipV) {
        return ObjLoader.INSTANCE.loadModel(
            new ObjModel.ModelSettings(modelLocation, false, true, flipV, true, null)
        );
    }

    /**
     * Кросс-версионное запекание OBJ модели.
     * Сглаживает удаление ResourceLocation из сигнатуры bake() в 1.21.1.
     */
    public static BakedModel bakeObjModel(ObjModel model, IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelName) {
        //? if < 1.21.1 {
        return model.bake(context, baker, spriteGetter, modelState, overrides, modelName);
        //?} else {
        /*return model.bake(context, baker, spriteGetter, modelState, overrides);
        *///?}
    }

    /**
     * Кросс-версионное запекание ванильной BlockModel.
     * В 1.21.1 из метода bake() вырезали параметр ResourceLocation modelLocation.
     */
    public static BakedModel bakeBlockModel(BlockModel blockModel, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation, boolean guiLight3d) {
        //? if < 1.21.1 {
        return blockModel.bake(baker, blockModel, spriteGetter, modelState, modelLocation, guiLight3d);
        //?} else {
        /*return blockModel.bake(baker, blockModel, spriteGetter, modelState, guiLight3d);
        *///?}
    }
}