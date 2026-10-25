package com.hbm_m.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;

@OnlyIn(Dist.CLIENT)
public final class LoaderHooks {
    private LoaderHooks() {}

    // Собственный вечный OBJ-кеш УДАЛЁН: ObjLoader.INSTANCE сам кеширует по ModelSettings
    // (включая flipV) и сбрасывает кеш на resource reload (F3+T) — наш ConcurrentHashMap
    // удерживал старые модели навсегда и ломал hot-reload .obj-ассетов.

    /**
     * Кросс-версионная загрузка OBJ модели.
     * Исправляет баг 1.20.1 Forge, где flipV ошибочно передавался вместо automaticCulling.
     */
    public static ObjModel loadObjModel(ResourceLocation modelLocation, boolean flipV) {
        ObjModel model = ObjLoader.INSTANCE.loadModel(
            new ObjModel.ModelSettings(modelLocation, false, true, flipV, true, null)
        );
        com.hbm_m.main.MainRegistry.LOGGER.debug("LoaderHooks: parsed OBJ model {}", modelLocation);
        return model;
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

    // =====================================================================================
    //  Model Registration & Handling Bridge (бывшие PlatformHooks-методы).
    //
    //  1.20.1: ModelResourceLocation extends ResourceLocation.
    //  1.21.1: ModelResourceLocation is a record (does NOT extend ResourceLocation).
    //          RegisterGeometryLoaders требует ResourceLocation, RegisterAdditional требует ModelResourceLocation.
    // =====================================================================================

    /** Кросс-версионный ключ «инвентарной» модели сета (единая точка для register/get). */
    private static net.minecraft.client.resources.model.ModelResourceLocation itemModelLocation(ResourceLocation id) {
        //? if < 1.21.1 {
        return new net.minecraft.client.resources.model.ModelResourceLocation(id, "inventory");
        //?} else {
        /*return new net.minecraft.client.resources.model.ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "item/" + id.getPath()), "standalone");
        *///?}
    }

    /** Создание ModelResourceLocation. Возвращает Object, чтобы не зависеть от изменения иерархии наследования. */
    public static Object createModelLocation(ResourceLocation id, String variant) {
        return new net.minecraft.client.resources.model.ModelResourceLocation(id, variant);
    }

    /** Извлечение ResourceLocation (ID) из ModelResourceLocation. */
    public static ResourceLocation getModelId(Object modelResourceLocation) {
        //? if < 1.21.1 {
        return (ResourceLocation) modelResourceLocation;
        //?} else {
        /*return ((net.minecraft.client.resources.model.ModelResourceLocation) modelResourceLocation).id();
        *///?}
    }

    /** Регистрация дополнительной standalone-модели. */
    public static void registerAdditionalModel(Object event, ResourceLocation loc) {
        //? if < 1.21.1 {
        ((net.minecraftforge.client.event.ModelEvent.RegisterAdditional) event).register(loc);
        //?} else {
        /*((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional) event).register(
                new net.minecraft.client.resources.model.ModelResourceLocation(loc, "standalone"));
        *///?}
    }

    /**
     * Регистрация "инвентарной" модели сета по её базовому id (например hbm_m:t51_armor).
     * Файл модели ищется в {@code models/item/<path>.json} на обеих версиях.
     */
    public static void registerItemModel(Object event, ResourceLocation id) {
        //? if < 1.21.1 {
        ((net.minecraftforge.client.event.ModelEvent.RegisterAdditional) event).register(
                (net.minecraft.client.resources.model.ModelResourceLocation) itemModelLocation(id));
        //?} else {
        /*((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional) event).register(itemModelLocation(id));
        *///?}
    }

    /** Запечённая "инвентарная" модель сета, зарегистрированная через {@link #registerItemModel}. */
    public static BakedModel getItemModel(net.minecraft.client.resources.model.ModelManager manager, ResourceLocation id) {
        // ModelManager.getModel(ModelResourceLocation) одинаков на обеих версиях,
        // itemModelLocation() уже возвращает ModelResourceLocation — гейт и каст избыточны.
        return manager.getModel(itemModelLocation(id));
    }

    /** Регистрация Geometry Loader. */
    public static void registerGeometryLoader(Object event, String name, Object loader) {
        //? if < 1.21.1 {
        ((net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders) event).register(
                name, (net.minecraftforge.client.model.geometry.IGeometryLoader<?>) loader);
        //?} else {
        /*((net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders) event).register(
                ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, name),
                (net.minecraftforge.client.model.geometry.IGeometryLoader<?>) loader);
        *///?}
    }

    /** BakedModel из ModelManager по ResourceLocation (скрывает смену сигнатуры getModel() в 1.21.1). */
    public static BakedModel getModel(net.minecraft.client.resources.model.ModelManager manager, ResourceLocation loc) {
        //? if < 1.21.1 {
        return manager.getModel(loc);
        //?} else {
        /*return manager.getModel(new net.minecraft.client.resources.model.ModelResourceLocation(loc, "standalone"));
        *///?}
    }
}