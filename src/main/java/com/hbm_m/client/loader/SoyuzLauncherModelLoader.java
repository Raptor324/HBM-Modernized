package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.SoyuzLauncherBakedModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.LoaderHooks;
import com.mojang.math.Transformation;
import org.jetbrains.annotations.NotNull;

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

//? if < 1.21.1 {
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.obj.ObjModel;

/**
 * Loads the 6 separate Soyuz launcher OBJ parts (Table, TowerBase, Tower,
 * SupportBase, Support, Legs - each its own .obj file, unlike other multipart
 * machines here which use one .obj with named sub-groups) into a single
 * {@link SoyuzLauncherBakedModel}. See that class for why world-rendering of
 * baked quads is skipped entirely (16-bit chunk mesh overflow on the ~60
 * block tall masts).
 */
//?} else {
/*import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.obj.ObjModel;
*///?}

public class SoyuzLauncherModelLoader implements IGeometryLoader<SoyuzLauncherModelLoader.Geometry> {

    private record PartDef(String name, ResourceLocation model, ResourceLocation texture) {}

    private static final String MODEL_DIR = "models/soyuz/";
    private static final String TEX_DIR = "block/soyuz/";

    private static ResourceLocation model(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, MODEL_DIR + path);
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, TEX_DIR + path);
    }

    private static final PartDef[] PARTS = new PartDef[] {
        new PartDef(SoyuzLauncherBakedModel.TABLE, model("soyuz_launcher_table.obj"), tex("launcher_table")),
        new PartDef(SoyuzLauncherBakedModel.TOWER_BASE, model("soyuz_launcher_tower_base.obj"), tex("launcher_tower_base")),
        new PartDef(SoyuzLauncherBakedModel.TOWER, model("soyuz_launcher_tower.obj"), tex("launcher_tower")),
        new PartDef(SoyuzLauncherBakedModel.SUPPORT_BASE, model("soyuz_launcher_support_base.obj"), tex("launcher_support_base")),
        new PartDef(SoyuzLauncherBakedModel.SUPPORT, model("soyuz_launcher_support.obj"), tex("launcher_support")),
        new PartDef(SoyuzLauncherBakedModel.LEGS, model("soyuz_launcher_legs.obj"), tex("launcher_leg")),
    };

    @Override
    public Geometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        return new Geometry();
    }

    public static class Geometry implements IUnbakedGeometry<Geometry> {

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {}

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
            HashMap<String, BakedModel> bakedParts = new LinkedHashMap<>();
            ModelState identityState = new ModelState() {
                @Override
                public @NotNull Transformation getRotation() {
                    return Transformation.identity();
                }
            };

            for (PartDef part : PARTS) {
                try {
                    ObjModel objModel = LoaderHooks.loadObjModel(part.model(), true);
                    IGeometryBakingContext partContext = new PartContext(context, part.texture());
                    BakedModel baked = LoaderHooks.bakeObjModel(objModel, partContext, baker, spriteGetter, identityState, overrides, modelName);
                    bakedParts.put(part.name(), baked);
                } catch (Exception e) {
                    MainRegistry.LOGGER.error("SoyuzLauncherModelLoader: failed to load/bake part '{}'", part.name(), e);
                }
            }

            return new SoyuzLauncherBakedModel(bakedParts, context.getTransforms());
        }
    }

    /** Wraps the outer model context, overriding material resolution so every part's
     * "default" (or "particle") material resolves to that part's own texture. */
    private static class PartContext implements IGeometryBakingContext {
        private final IGeometryBakingContext parent;
        private final Material material;

        PartContext(IGeometryBakingContext parent, ResourceLocation texture) {
            this.parent = parent;
            this.material = new Material(TextureAtlas.LOCATION_BLOCKS, texture);
        }

        @Override public String getModelName() { return parent.getModelName(); }
        @Override public boolean hasMaterial(String name) { return true; }
        @Override public Material getMaterial(String name) { return material; }
        @Override public boolean isGui3d() { return parent.isGui3d(); }
        @Override public boolean useBlockLight() { return parent.useBlockLight(); }
        @Override public boolean useAmbientOcclusion() { return parent.useAmbientOcclusion(); }
        @Override public ItemTransforms getTransforms() { return parent.getTransforms(); }
        @Override public Transformation getRootTransform() { return Transformation.identity(); }
        @Override public ResourceLocation getRenderTypeHint() { return parent.getRenderTypeHint(); }
        @Override public boolean isComponentVisible(String component, boolean fallback) { return true; }
    }
}