package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.ConfiguredMultipartBakedModel;
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
import net.minecraftforge.client.model.obj.ObjModel;

//?} else {
/*import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.obj.ObjModel;
*///?}

/**
 * ЕДИНЫЙ лоадер OBJ-моделей станков ({@code "loader": "hbm_m:machine_parts_loader"}).
 *
 * <p>Группы частей ОБНАРУЖИВАЮТСЯ САМИ: если в JSON нет {@code "parts"}, запекаются
 * все корневые группы OBJ как отдельные части (доступны BER через
 * {@code AbstractMultipartBakedModel.getPart}). Никаких ручных списков групп.
 *
 * <p>Вариант «каждая часть — свой OBJ» (Assembler/Press): карта
 * {@code "parts": {"Body": {"model": "...", "texture": "..."}}, ...}.
 *
 * <p>Модель результата — {@link ConfiguredMultipartBakedModel}; её поведение
 * (мир в BER / item-части / render types) вливается из спеки станка в конце
 * запекания моделей ({@code MachineRenderRegistry.bindBakedModels}).
 * Подклассы (Press) могут подменить тип модели.
 */
public class MachinePartsModelLoader<T extends BakedModel> extends AbstractObjPartModelLoader<T> {

    @Override
    public ObjPartGeometry<T> read(JsonObject json, JsonDeserializationContext ctx) {
        if (json.has("parts") && json.get("parts").isJsonObject()) {
            return new MultiObjGeometry<>(parsePartDefs(json), flipV(json), this::createMultiObjModel);
        }
        // Одиночный OBJ, части — все корневые группы (авто). Базовый flow.
        return super.read(json, ctx);
    }

    @Override
    protected Set<String> getPartNames(JsonObject json) {
        // АВТО: пустой список = базовый лоадер берёт все корневые группы OBJ.
        return Set.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final T createBakedModel(HashMap<String, BakedModel> bakedParts,
                                       ItemTransforms transforms,
                                       ResourceLocation modelLocation) {
        return (T) createConfiguredModel(bakedParts, transforms, modelLocation);
    }

    /** Модель по умолчанию для варианта «части из отдельных OBJ»; подклассы могут переопределить. */
    @SuppressWarnings("unchecked")
    protected T createMultiObjModel(HashMap<String, BakedModel> bakedParts,
                                    ItemTransforms transforms,
                                    ResourceLocation modelLocation) {
        return (T) new ConfiguredMultipartBakedModel(bakedParts, transforms, modelLocation.getPath());
    }

    private ConfiguredMultipartBakedModel createConfiguredModel(HashMap<String, BakedModel> bakedParts,
                                                                ItemTransforms transforms,
                                                                ResourceLocation modelLocation) {
        return new ConfiguredMultipartBakedModel(bakedParts, transforms, modelLocation.getPath());
    }

    private static boolean flipV(JsonObject json) {
        return GsonHelper.getAsBoolean(json, "flip_v", true);
    }

    private static Map<String, PartDef> parsePartDefs(JsonObject json) {
        JsonObject parts = GsonHelper.getAsJsonObject(json, "parts");
        Map<String, PartDef> partDefs = new LinkedHashMap<>();
        for (var entry : parts.entrySet()) {
            JsonObject partJson = entry.getValue().getAsJsonObject();
            String model = GsonHelper.getAsString(partJson, "model");
            String texture = GsonHelper.getAsString(partJson, "texture");
            partDefs.put(entry.getKey(), new PartDef(ResourceLocation.tryParse(model), ResourceLocation.tryParse(texture)));
        }
        return partDefs;
    }

    /** Фабрика итоговой модели из запечённых частей (для подклассов вроде Press). */
    @FunctionalInterface
    public interface PartModelFactory<T extends BakedModel> {
        T create(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation);
    }

    /** @param texture навязанная текстура части; null — материалы разрешаются контекстом (MTL/JSON). */
    protected record PartDef(ResourceLocation model, @Nullable ResourceLocation texture) {}

    /**
     * Вариант «каждая часть — свой OBJ с одной текстурой на часть» (Assembler/Press).
     * Наследует {@link ObjPartGeometry}, чтобы подходить под тип возврата {@link #read}.
     */
    protected static final class MultiObjGeometry<T extends BakedModel> extends ObjPartGeometry<T> {
        private final Map<String, PartDef> partDefs;
        private final boolean flipV;
        private final PartModelFactory<T> factory;

        public MultiObjGeometry(Map<String, PartDef> partDefs, boolean flipV, PartModelFactory<T> factory) {
            super(ResourceLocation.fromNamespaceAndPath("hbm_m", "multi_obj_parts"),
                  partDefs.keySet(), flipV, null);
            this.partDefs = partDefs;
            this.flipV = flipV;
            this.factory = factory;
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

        private BakedModel doBake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelName) {
            HashMap<String, BakedModel> bakedParts = new HashMap<>();
            ModelState identityState = createIdentityState();

            for (var entry : partDefs.entrySet()) {
                String partName = entry.getKey();
                PartDef def = entry.getValue();
                try {
                    ObjModel objModel = LoaderHooks.loadObjModel(def.model(), flipV);
                    IGeometryBakingContext partContext = def.texture() != null
                            ? new PartTextureContext(context, def.texture())
                            : context;
                    BakedModel baked = LoaderHooks.bakeObjModel(objModel, partContext, baker, spriteGetter, identityState, overrides, modelName);
                    bakedParts.put(partName, baked);
                } catch (Exception e) {
                    MainRegistry.LOGGER.error("MachinePartsModelLoader: Failed to bake part '{}' of {}", partName, modelName, e);
                }
            }

            return factory.create(bakedParts, context.getTransforms(), modelName);
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

    /**
     * Контекст запекания, навязывающий всей части одну текстуру.
     */
    private static final class PartTextureContext implements IGeometryBakingContext {
        private final IGeometryBakingContext parent;
        private final ResourceLocation overrideTexture;

        PartTextureContext(IGeometryBakingContext parent, ResourceLocation texture) {
            this.parent = parent;
            this.overrideTexture = texture;
        }

        @Override public String getModelName() { return parent.getModelName(); }
        @Override public boolean isGui3d() { return parent.isGui3d(); }
        @Override public boolean useBlockLight() { return parent.useBlockLight(); }
        @Override public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }
        @Override public ItemTransforms getTransforms() { return parent.getTransforms(); }
        @Override public Transformation getRootTransform() { return parent.getRootTransform(); }
        @Override public ResourceLocation getRenderTypeHint() { return parent.getRenderTypeHint(); }
        @Override public boolean isComponentVisible(String component, boolean fallback) { return true; }
        @Override public boolean hasMaterial(String name) { return true; }
        @Override public Material getMaterial(String name) {
            return new Material(TextureAtlas.LOCATION_BLOCKS, overrideTexture);
        }
    }
}
