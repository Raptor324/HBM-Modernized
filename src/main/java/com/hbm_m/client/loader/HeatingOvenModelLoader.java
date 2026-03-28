package com.hbm_m.client.loader;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.hbm_m.client.model.HeatingOvenBakedModel;
import com.hbm_m.main.MainRegistry;
import com.mojang.math.Transformation;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Model loader for HeatingOven that loads an OBJ file with multiple parts:
 * Main, Door, Inner, InnerBurning
 */
public class HeatingOvenModelLoader implements IGeometryLoader<HeatingOvenModelLoader.HeatingOvenGeometry> {

    private static final Set<String> PART_NAMES = Set.of(
        HeatingOvenBakedModel.MAIN,
        HeatingOvenBakedModel.DOOR,
        HeatingOvenBakedModel.INNER,
        HeatingOvenBakedModel.INNER_BURNING
    );

    @Override
    public HeatingOvenGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        ResourceLocation model = ResourceLocation.parse(GsonHelper.getAsString(jsonObject, "model"));
        boolean flipV = GsonHelper.getAsBoolean(jsonObject, "flip_v", true);
        return new HeatingOvenGeometry(model, flipV);
    }

    public static class HeatingOvenGeometry implements IUnbakedGeometry<HeatingOvenGeometry> {
        private final ResourceLocation modelLocation;
        private final boolean flipV;

        public HeatingOvenGeometry(ResourceLocation modelLocation, boolean flipV) {
            this.modelLocation = modelLocation;
            this.flipV = flipV;
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) { }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides,
                               ResourceLocation modelLocation) {
            try {
                ObjModel model = ObjLoader.INSTANCE.loadModel(
                    new ObjModel.ModelSettings(this.modelLocation, flipV, false, true, true, null)
                );

                Map<String, BakedModel> bakedParts = new HashMap<>();
                ModelState identityState = createIdentityState();

                for (String partName : PART_NAMES) {
                    SinglePartBakingContext partContext = new SinglePartBakingContext(context, partName);
                    BakedModel bakedPart = model.bake(partContext, baker, spriteGetter,
                        identityState, overrides, modelLocation);
                    bakedParts.put(partName, bakedPart);
                    MainRegistry.LOGGER.debug("HeatingOvenModelLoader: Baked part '{}'", partName);
                }

                ItemTransforms transforms = context.getTransforms();
                MainRegistry.LOGGER.info("HeatingOvenModelLoader: Successfully baked {} parts", bakedParts.size());
                return new HeatingOvenBakedModel(bakedParts, transforms);

            } catch (Exception e) {
                MainRegistry.LOGGER.error("Failed to bake HeatingOven model: {}", this.modelLocation, e);
                throw new RuntimeException("Failed to bake heating oven model: " + this.modelLocation, e);
            }
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
     * Context that filters which part is visible during baking
     */
    private static class SinglePartBakingContext implements IGeometryBakingContext {
        private final IGeometryBakingContext parent;
        private final String visiblePart;

        public SinglePartBakingContext(IGeometryBakingContext parent, String visiblePart) {
            this.parent = parent;
            this.visiblePart = visiblePart;
        }

        @Override
        public String getModelName() { return parent.getModelName(); }

        @Override
        public boolean hasMaterial(String name) { return parent.hasMaterial(name); }

        @Override
        public Material getMaterial(String name) { return parent.getMaterial(name); }

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
        public boolean isComponentVisible(String component, boolean fallback) {
            // Only show the part we're currently baking
            return component.equalsIgnoreCase(visiblePart);
        }

        @Override
        public @Nullable ResourceLocation getRenderTypeHint() {
            return parent.getRenderTypeHint();
        }
    }
}
