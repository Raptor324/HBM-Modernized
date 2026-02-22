package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineAssemblerBakedModel;
import com.hbm_m.main.MainRegistry;
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
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;

/**
 * Загрузчик модели для MachineAssembler: загружает несколько OBJ-файлов
 * (Body, Slider, Arm, Cog) как отдельные части с индивидуальными текстурами.
 */
public class MachineAssemblerModelLoader implements IGeometryLoader<MachineAssemblerModelLoader.MultiObjGeometry> {

    @Override
    public MultiObjGeometry read(JsonObject json, JsonDeserializationContext ctx) {
        JsonObject parts = GsonHelper.getAsJsonObject(json, "parts");
        boolean flipV = GsonHelper.getAsBoolean(json, "flip_v", true);

        Map<String, PartDef> partDefs = new LinkedHashMap<>();
        for (var entry : parts.entrySet()) {
            JsonObject partJson = entry.getValue().getAsJsonObject();
            String model = GsonHelper.getAsString(partJson, "model");
            String texture = GsonHelper.getAsString(partJson, "texture");
            partDefs.put(entry.getKey(), new PartDef(ResourceLocation.parse(model), ResourceLocation.parse(texture)));
        }

        MainRegistry.LOGGER.debug("MachineAssemblerModelLoader: read {} parts", partDefs.size());
        return new MultiObjGeometry(partDefs, flipV);
    }

    record PartDef(ResourceLocation model, ResourceLocation texture) {}

    static class MultiObjGeometry implements IUnbakedGeometry<MultiObjGeometry> {
        private final Map<String, PartDef> partDefs;
        private final boolean flipV;

        MultiObjGeometry(Map<String, PartDef> partDefs, boolean flipV) {
            this.partDefs = partDefs;
            this.flipV = flipV;
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides,
                               ResourceLocation modelName) {
            HashMap<String, BakedModel> bakedParts = new HashMap<>();
            ModelState identityState = new ModelState() {
                @Override
                public @NotNull Transformation getRotation() {
                    return Transformation.identity();
                }
            };

            for (var entry : partDefs.entrySet()) {
                String partName = entry.getKey();
                PartDef def = entry.getValue();

                try {
                    ObjModel objModel = ObjLoader.INSTANCE.loadModel(
                            new ObjModel.ModelSettings(def.model(), flipV, false, true, true, null));

                    IGeometryBakingContext partContext = new PartTextureContext(context, def.texture());
                    BakedModel baked = objModel.bake(partContext, baker, spriteGetter,
                            identityState, overrides, modelName);
                    bakedParts.put(partName, baked);

                    if (baked != null) {
                        int quadCount = 0;
                        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                            quadCount += baked.getQuads(null, dir, net.minecraft.util.RandomSource.create(42)).size();
                        }
                        quadCount += baked.getQuads(null, null, net.minecraft.util.RandomSource.create(42)).size();
                        MainRegistry.LOGGER.info("MachineAssemblerModelLoader: Baked part '{}' -> {} quads", partName, quadCount);
                    }
                } catch (Exception e) {
                    MainRegistry.LOGGER.error("MachineAssemblerModelLoader: Failed to bake part '{}'", partName, e);
                }
            }

            MainRegistry.LOGGER.info("MachineAssemblerModelLoader: Total baked parts: {}", bakedParts.size());
            return new MachineAssemblerBakedModel(bakedParts, context.getTransforms());
        }
    }

    /**
     * Контекст запекания, перенаправляющий все запросы текстур на заданную текстуру части.
     */
    static class PartTextureContext implements IGeometryBakingContext {
        private final IGeometryBakingContext parent;
        private final ResourceLocation overrideTexture;

        PartTextureContext(IGeometryBakingContext parent, ResourceLocation texture) {
            this.parent = parent;
            this.overrideTexture = texture;
        }

        @Override
        public String getModelName() { return parent.getModelName(); }

        @Override
        public boolean isGui3d() { return parent.isGui3d(); }

        @Override
        public boolean useBlockLight() { return parent.useBlockLight(); }

        @Override
        public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }

        @Override
        public ItemTransforms getTransforms() { return parent.getTransforms(); }

        @Override
        public Transformation getRootTransform() { return parent.getRootTransform(); }

        @Override
        public ResourceLocation getRenderTypeHint() { return parent.getRenderTypeHint(); }

        @Override
        public boolean isComponentVisible(String component, boolean fallback) {
            return true;
        }

        @Override
        public boolean hasMaterial(String name) {
            return true;
        }

        @Override
        public Material getMaterial(String name) {
            return new Material(TextureAtlas.LOCATION_BLOCKS, overrideTexture);
        }
    }
}
