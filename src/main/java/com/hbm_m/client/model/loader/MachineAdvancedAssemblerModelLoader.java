package com.hbm_m.client.model.loader;

// Загрузчик модели для продвинутой сборочной машины, использующий OBJ модель с частями.
// Каждая часть (Base, Ring, ArmLower1, ArmUpper1, Head1, Spike1, ArmLower2, ArmUpper2, Head2, Spike2)
// запекается отдельно и передается в AdvancedAssemblyMachineBakedModel для рендеринга.
// Это позволяет анимировать части независимо (вращение кольца, движение рук и т.д.).
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.mojang.math.Transformation;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

public class MachineAdvancedAssemblerModelLoader implements IGeometryLoader<MachineAdvancedAssemblerModelLoader.Geometry> {

    @Override
    public Geometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        String modelStr = GsonHelper.getAsString(jsonObject, "model");
        MainRegistry.LOGGER.debug("AdvancedAssemblyMachineModelLoader.read: model string='{}'", modelStr);
        ResourceLocation model = ResourceLocation.parse(modelStr);
        return new Geometry(model);
    }

    public static class Geometry implements IUnbakedGeometry<Geometry> {
        private final ResourceLocation modelLocation;

        // Определяем "чистые" имена частей, которые мы используем в рендерере
        private static final Set<String> PART_NAMES = Set.of(
            "Base", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
            "ArmLower2", "ArmUpper2", "Head2", "Spike2"
        );

        public Geometry(ResourceLocation modelLocation) {
            this.modelLocation = modelLocation;
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelName) {
            ObjModel model;
            try {
                model = ObjLoader.INSTANCE.loadModel(new ObjModel.ModelSettings(this.modelLocation, true, false, true, true, null));
            } catch (Exception e) {
                throw new RuntimeException("Не удалось загрузить OBJ модель: " + this.modelLocation, e);
            }
            
            var bakedParts = new HashMap<String, BakedModel>();
            
            ModelState identityState = new ModelState() {
                @Override
                public @NotNull Transformation getRotation() {
                    return Transformation.identity();
                }
            };
            
            for(String partName : PART_NAMES) {
                var partContext = new SinglePartBakingContext(context, partName);
                // Используем наш пустой identityState вместо modelState из параметров
                bakedParts.put(partName, model.bake(partContext, baker, spriteGetter, identityState, overrides, modelName));
            }
            
            if (!bakedParts.containsKey("Base")) {
                // Используем наш пустой identityState и здесь
                bakedParts.put("Base", model.bake(new SinglePartBakingContext(context, "Base"), baker, spriteGetter, identityState, overrides, modelName));
            }

            // Мы передаем в конструктор не только части, но и ItemTransforms из контекста
            return new MachineAdvancedAssemblerBakedModel(bakedParts, context.getTransforms());
        }
    }

    private static class SinglePartBakingContext implements IGeometryBakingContext {
        private final IGeometryBakingContext parent;
        private final String visiblePart;

        public SinglePartBakingContext(IGeometryBakingContext parent, String visiblePart) {
            this.parent = parent;
            this.visiblePart = visiblePart;
        }
        
        // --- Делегируем все вызовы родительскому контексту ---
        @Override public String getModelName() { return parent.getModelName(); }
        @Override public boolean isGui3d() { return parent.isGui3d(); }
        @Override public boolean useBlockLight() { return parent.useBlockLight(); }
        @Override public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }
        @Override public ItemTransforms getTransforms() { return parent.getTransforms(); }
        @Override public Material getMaterial(String name) { return parent.getMaterial(name); }
        @Override public Transformation getRootTransform() { return parent.getRootTransform(); }
        @Override public boolean hasMaterial(String name) { return parent.hasMaterial(name); }
        @Override public ResourceLocation getRenderTypeHint() { return parent.getRenderTypeHint(); }


        @Override public boolean isComponentVisible(String component, boolean fallback) {
            // Делаем видимым компонент, если его имя НАЧИНАЕТСЯ с имени нашей части.
            // Это включает дочерние объекты (например, "Base/Base_default" будет видим при запекании "Base").
            return component.equals(this.visiblePart) || component.startsWith(this.visiblePart + "/");
        }
    }
}