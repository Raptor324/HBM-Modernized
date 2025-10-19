package com.hbm_m.client.model.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.main.MainRegistry;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

public class DoorModelLoader implements IGeometryLoader<DoorModelLoader.Geometry> {
    
    @Override
    public Geometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        String modelStr = GsonHelper.getAsString(jsonObject, "model");
        MainRegistry.LOGGER.debug("DoorModelLoader.read: model string='{}'", modelStr);
        ResourceLocation model = ResourceLocation.parse(modelStr);
        
        String[] partNames = null;
        if (jsonObject.has("parts")) {
            var partsArray = jsonObject.getAsJsonArray("parts");
            partNames = new String[partsArray.size()];
            for (int i = 0; i < partsArray.size(); i++) {
                partNames[i] = partsArray.get(i).getAsString();
            }
        }
        
        return new Geometry(model, partNames);
    }
    
    public static class Geometry implements IUnbakedGeometry<Geometry> {
        private final ResourceLocation modelLocation;
        private final String[] customPartNames;
        
        private static final Set<String> DEFAULT_PART_NAMES = Set.of(
            "frame", "doorLeft", "doorRight"
        );
        
        public Geometry(ResourceLocation modelLocation, String[] customPartNames) {
            this.modelLocation = modelLocation;
            this.customPartNames = customPartNames;
        }
        
        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter,
                                   IGeometryBakingContext context) {
        }
        
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                            Function<Material, TextureAtlasSprite> spriteGetter,
                            ModelState modelState, ItemOverrides overrides,
                            ResourceLocation modelName) {
            
            ObjModel model;
            try {
                model = ObjLoader.INSTANCE.loadModel(
                    new ObjModel.ModelSettings(this.modelLocation, true, false, true, true, null)
                );
                MainRegistry.LOGGER.info("DoorModelLoader: Successfully loaded OBJ model: {}", this.modelLocation);
            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to load OBJ door model: " + this.modelLocation, e);
                throw new RuntimeException("Не удалось загрузить OBJ модель двери: " + this.modelLocation, e);
            }
            
            Set<String> partNames = customPartNames != null 
                ? Set.of(customPartNames) 
                : DEFAULT_PART_NAMES;
            
            var bakedParts = new HashMap<String, BakedModel>();
            ModelState identityState = new ModelState() {
                @Override
                public @NotNull Transformation getRotation() {
                    return Transformation.identity();
                }
            };
            
            for (String partName : partNames) {
                var partContext = new SinglePartBakingContext(context, partName);
                BakedModel bakedPart = model.bake(partContext, baker, spriteGetter,
                    identityState, overrides, modelName);
                bakedParts.put(partName, bakedPart);
                MainRegistry.LOGGER.info("DoorModelLoader: Baked part '{}', quads count: {}", 
                    partName, bakedPart.getQuads(null, null, RandomSource.create()).size());
            }
            
            if (!bakedParts.containsKey("Base")) {
                bakedParts.put("Base", model.bake(
                    new SinglePartBakingContext(context, "Base"),
                    baker, spriteGetter, identityState, overrides, modelName
                ));
            }
            
            MainRegistry.LOGGER.info("DoorModelLoader: Total baked parts: {}", bakedParts.size());
            return new DoorBakedModel(bakedParts, context.getTransforms());
        }
        
        private static class SinglePartBakingContext implements IGeometryBakingContext {
            private final IGeometryBakingContext parent;
            private final String visiblePart;
            
            public SinglePartBakingContext(IGeometryBakingContext parent, String visiblePart) {
                this.parent = parent;
                this.visiblePart = visiblePart;
            }
            
            @Override public String getModelName() { return parent.getModelName(); }
            @Override public boolean isGui3d() { return parent.isGui3d(); }
            @Override public boolean useBlockLight() { return parent.useBlockLight(); }
            @Override public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }
            @Override public ItemTransforms getTransforms() { return parent.getTransforms(); }
            @Override public Material getMaterial(String name) { return parent.getMaterial(name); }
            @Override public Transformation getRootTransform() { return parent.getRootTransform(); }
            @Override public boolean hasMaterial(String name) { return parent.hasMaterial(name); }
            @Override public ResourceLocation getRenderTypeHint() { return parent.getRenderTypeHint(); }
            
            @Override
            public boolean isComponentVisible(String component, boolean fallback) {
                return component.equals(this.visiblePart) || 
                       component.startsWith(this.visiblePart + "/");
            }
        }
    }
}
