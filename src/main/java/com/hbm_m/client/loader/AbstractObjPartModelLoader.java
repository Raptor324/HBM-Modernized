package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.LoaderHooks;
import com.mojang.math.Transformation;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
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
import net.minecraftforge.client.model.obj.ObjModel;
//?} else {
/*import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.obj.ObjModel;
*///?}

public abstract class AbstractObjPartModelLoader<T extends BakedModel> implements IGeometryLoader<AbstractObjPartModelLoader.ObjPartGeometry<T>> {

    protected abstract Set<String> getPartNames(JsonObject jsonObject);
    protected abstract T createBakedModel(HashMap<String, BakedModel> bakedParts, 
                                          ItemTransforms transforms,
                                          ResourceLocation modelLocation);
    
    protected boolean flipV() { return true; }

    protected ResourceLocation mapAtlasForTexture(ResourceLocation texture) { return null; }

    /**
     * Дополнительный transform в пространстве модели для конкретной части
     * (применяется при запекании через ModelState): сдвиги/повороты, которые
     * не хочет хранить сам OBJ. По умолчанию — identity.
     */
    protected com.mojang.math.Transformation partTransform(String part) {
        return com.mojang.math.Transformation.identity();
    }

    /**
     * Запекание ОДНОЙ части модели. Дефолт — прямой {@code ObjModel.bake}.
     * Переопределяется загрузчиками с дедупликацией (двери: текстурные скины
     * шарят canonical-запекание через {@link DoorModelLoader}).
     */
    protected BakedModel bakePart(ObjModel model, SinglePartBakingContext partContext, ModelBaker baker,
                                  Function<Material, TextureAtlasSprite> spriteGetter, ModelState identityState,
                                  ItemOverrides overrides, ResourceLocation modelName) {
        return LoaderHooks.bakeObjModel(model, partContext, baker, spriteGetter, identityState, overrides, modelName);
    }

    @Override
    public ObjPartGeometry<T> read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        String modelStr = GsonHelper.getAsString(jsonObject, "model");
        MainRegistry.LOGGER.debug("{}: model string='{}'", this.getClass().getSimpleName(), modelStr);
        ResourceLocation model = ResourceLocation.tryParse(modelStr);
        Set<String> partNames = getPartNames(jsonObject);
        boolean flipV = GsonHelper.getAsBoolean(jsonObject, "flip_v", true);
        return new ObjPartGeometry<>(model, partNames, flipV, this);
    }

    public static class ObjPartGeometry<T extends BakedModel> implements IUnbakedGeometry<ObjPartGeometry<T>> {
        private final ResourceLocation modelLocation;
        /** Пустой set = АВТОрежим: запечь все корневые группы OBJ (обнаруживаются при запекании). */
        private final Set<String> partNames;
        private final boolean flipV;
        private final AbstractObjPartModelLoader<T> loader;
        /**
         * Спецификации частей (имя части -> {группа OBJ, текстура-оверрайд}).
         * Пустая карта = классический режим по {@link #partNames}. Позволяет одной OBJ-группе
         * выступать несколькими частями с разными текстурами (hot/cold варианты).
         */
        private final Map<String, PartSpec> partSpecs;

        public ObjPartGeometry(ResourceLocation modelLocation, Set<String> partNames, boolean flipV,
                               AbstractObjPartModelLoader<T> loader) {
            this(modelLocation, partNames, flipV, loader, Map.of());
        }

        public ObjPartGeometry(ResourceLocation modelLocation, Set<String> partNames, boolean flipV,
                               AbstractObjPartModelLoader<T> loader, Map<String, PartSpec> partSpecs) {
            this.modelLocation = modelLocation;
            this.partNames = partNames;
            this.flipV = flipV;
            this.loader = loader;
            this.partSpecs = partSpecs;
        }

        // Парсинг OBJ дедуплицируется глобально в LoaderHooks (по modelLocation + flipV)
        private ObjModel getOrLoadObjModel() {
            try {
                return LoaderHooks.loadObjModel(modelLocation, flipV);
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to load OBJ model: " + modelLocation, e);
                throw new RuntimeException("Не удалось загрузить OBJ модель: " + modelLocation, e);
            }
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        }

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
            ObjModel model = getOrLoadObjModel();

            // Режим спецификаций: имена и группы частей заданы картой (возможны алиасы групп).
            if (!partSpecs.isEmpty()) {
                HashMap<String, BakedModel> specParts = new HashMap<>();
                for (var entry : partSpecs.entrySet()) {
                    PartSpec spec = entry.getValue();
                    String group = spec.group() != null ? spec.group() : entry.getKey();
                    BakedModel baked = LoaderHooks.bakeObjModel(model,
                            new SinglePartBakingContext(context, group, loader, spec.texture()),
                            baker, spriteGetter, stateForPart(entry.getKey()), overrides, modelName);
                    if (baked != null) {
                        specParts.put(entry.getKey(), baked);
                    } else {
                        MainRegistry.LOGGER.warn("{}: Part '{}' (group '{}') baked to NULL!",
                                loader.getClass().getSimpleName(), entry.getKey(), group);
                    }
                }
                return loader.createBakedModel(specParts, context.getTransforms(), modelName);
            }

            // АВТОрежим: пустой список частей = взять все корневые группы OBJ.
            boolean auto = partNames.isEmpty();
            Set<String> names = auto ? model.getRootComponentNames() : partNames;
            if (auto && names.isEmpty()) {
                MainRegistry.LOGGER.error("{}: OBJ {} has no root components!", loader.getClass().getSimpleName(), modelLocation);
            }

            HashMap<String, BakedModel> bakedParts = bakeParts(model, names, context, baker, spriteGetter, overrides, modelName);
            if (!auto) {
                ensureBasePart(model, bakedParts, context, baker, spriteGetter, overrides, modelName);
            }

            MainRegistry.LOGGER.info("{}: Total baked parts: {}", loader.getClass().getSimpleName(), bakedParts.size());
            // Диагностика невидимых мешей: сколько квадов запеклось в каждой части.
            var rand = net.minecraft.util.RandomSource.create();
            for (var entry : bakedParts.entrySet()) {
                int quads = 0;
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                    quads += entry.getValue().getQuads(null, d, rand).size();
                }
                quads += entry.getValue().getQuads(null, null, rand).size();
                if (quads == 0) {
                    MainRegistry.LOGGER.warn("{}: Part '{}' of {} baked with 0 quads! Root OBJ components: {}",
                            loader.getClass().getSimpleName(), entry.getKey(), modelName, model.getRootComponentNames());
                } else {
                    MainRegistry.LOGGER.debug("{}: Part '{}' of {} baked with {} quads",
                            loader.getClass().getSimpleName(), entry.getKey(), modelName, quads);
                }
            }
            return loader.createBakedModel(bakedParts, context.getTransforms(), modelName);
        }

        private HashMap<String, BakedModel> bakeParts(ObjModel model, Set<String> names, IGeometryBakingContext context,
                                                      ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                                                      ItemOverrides overrides, ResourceLocation modelName) {
            ConcurrentHashMap<String, BakedModel> bakedParts = new ConcurrentHashMap<>();
            ModelState identityState = createIdentityState();

            MainRegistry.LOGGER.info("{}: Baking {} parts in parallel: {}", loader.getClass().getSimpleName(), names.size(), names);

            // Параллельное запекание геометрии всех составных частей модели
            names.parallelStream().forEach(partName -> {
                SinglePartBakingContext partContext = new SinglePartBakingContext(context, partName, loader);
                BakedModel bakedPart = loader.bakePart(model, partContext, baker, spriteGetter, identityState, overrides, modelName);
                if (bakedPart != null) {
                    bakedParts.put(partName, bakedPart);
                } else {
                    MainRegistry.LOGGER.warn("{}: Part '{}' baked to NULL!", loader.getClass().getSimpleName(), partName);
                }
            });

            return new HashMap<>(bakedParts);
        }

        private void ensureBasePart(ObjModel model, HashMap<String, BakedModel> bakedParts,
                                   IGeometryBakingContext context, ModelBaker baker,
                                   Function<Material, TextureAtlasSprite> spriteGetter,
                                   ItemOverrides overrides, ResourceLocation modelName) {
            if (!bakedParts.containsKey("Base")) {
                MainRegistry.LOGGER.info("{}: Creating fallback 'Base' part", loader.getClass().getSimpleName());
                BakedModel baseModel = LoaderHooks.bakeObjModel(model, new SinglePartBakingContext(context, "Base", loader), baker, spriteGetter, createIdentityState(), overrides, modelName);
                if (baseModel != null) {
                    bakedParts.put("Base", baseModel);
                    MainRegistry.LOGGER.info("{}: Fallback 'Base' part baked successfully", loader.getClass().getSimpleName());
                } else {
                    MainRegistry.LOGGER.warn("{}: Fallback 'Base' part is NULL!", loader.getClass().getSimpleName());
                }
            }
        }

        private ModelState stateForPart(String part) {
            com.mojang.math.Transformation t = loader.partTransform(part);
            if (t.isIdentity()) return createIdentityState();
            return new ModelState() {
                @Override
                public @NotNull Transformation getRotation() {
                    return t;
                }
            };
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

    /** Часть в map-режиме "parts": группа OBJ + опциональная навязанная текстура. */
    protected record PartSpec(@org.jetbrains.annotations.Nullable String group,
                              @org.jetbrains.annotations.Nullable ResourceLocation texture) { }

    protected static class SinglePartBakingContext implements IGeometryBakingContext {
        private final IGeometryBakingContext parent;
        private final String visiblePart;
        private final AbstractObjPartModelLoader<?> loader;
        @org.jetbrains.annotations.Nullable
        private final ResourceLocation textureOverride;

        public SinglePartBakingContext(IGeometryBakingContext parent, String visiblePart, AbstractObjPartModelLoader<?> loader) {
            this(parent, visiblePart, loader, null);
        }

        public SinglePartBakingContext(IGeometryBakingContext parent, String visiblePart,
                                       AbstractObjPartModelLoader<?> loader,
                                       @org.jetbrains.annotations.Nullable ResourceLocation textureOverride) {
            this.parent = parent;
            this.visiblePart = visiblePart;
            this.loader = loader;
            this.textureOverride = textureOverride;
        }

        @Override public String getModelName() { return parent.getModelName(); }
        /** Имя части (visiblePart), заданное этому контексту запекания. */
        public String visiblePartName() { return visiblePart; }
        @Nullable
        public ResourceLocation textureOverride() { return textureOverride; }
        @Override public boolean isGui3d() { return parent.isGui3d(); }
        @Override public boolean useBlockLight() { return parent.useBlockLight(); }
        @Override public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }
        @Override public ItemTransforms getTransforms() { return parent.getTransforms(); }
        @Override public Transformation getRootTransform() { return parent.getRootTransform(); }
        @Override public boolean hasMaterial(String name) { return parent.hasMaterial(name); }
        @Override public ResourceLocation getRenderTypeHint() { return parent.getRenderTypeHint(); }

        @Override
        public Material getMaterial(String name) {
            if (this.textureOverride != null) {
                return new Material(TextureAtlas.LOCATION_BLOCKS, this.textureOverride);
            }
            Material mat = parent.getMaterial(name);

            if (this.visiblePart.equalsIgnoreCase("Label")) {
                if (parent.hasMaterial("label")) mat = parent.getMaterial("label");
            } else if (this.visiblePart.equalsIgnoreCase("Frame") || this.visiblePart.equalsIgnoreCase("Door")) {
                if (parent.hasMaterial("default")) {
                    mat = parent.getMaterial("default");
                } else {
                    String lowerPart = this.visiblePart.toLowerCase(java.util.Locale.ROOT);
                    if (parent.hasMaterial(lowerPart)) mat = parent.getMaterial(lowerPart);
                }
            } else {
                String lowerPart = this.visiblePart.toLowerCase(java.util.Locale.ROOT);
                if (parent.hasMaterial(lowerPart)) {
                    mat = parent.getMaterial(lowerPart);
                } else if (parent.hasMaterial("default")) {
                    mat = parent.getMaterial("default");
                }
            }

            if (mat == null) return null;
            ResourceLocation overrideAtlas = loader.mapAtlasForTexture(mat.texture());
            if (overrideAtlas != null && !overrideAtlas.equals(mat.atlasLocation())) {
                return new Material(overrideAtlas, mat.texture());
            }
            return mat;
        }

        @Override
        public boolean isComponentVisible(String component, boolean fallback) {
            return component.equals(this.visiblePart) || component.startsWith(this.visiblePart + "/");
        }
    }
}