//? if fabric {
/*package com.hbm_m.client.model.loading;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
final class TransformWrappingUnbakedModel implements ForgeLikeUnbakedModel {

    private final ForgeLikeUnbakedModel delegate;
    private final ItemTransforms overrideTransforms;
    private final boolean hasOverride;

    TransformWrappingUnbakedModel(ForgeLikeUnbakedModel delegate, ItemTransforms overrideTransforms) {
        this.delegate = delegate;
        this.overrideTransforms = overrideTransforms;
        this.hasOverride = overrideTransforms != null;
    }

    @Override
    public Map<String, ResourceLocation> textures() {
        return delegate.textures();
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
        delegate.resolveParents(modelGetter);
    }

    @Override
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation) {
        BakedModel baked = delegate.bake(baker, spriteGetter, modelState, modelLocation);
        if (baked == null) return null;
        if (!hasOverride) {
            return baked; // keep delegate transforms
        }
        return new BakedModelTransformOverride(baked, overrideTransforms);
    }
}
*///?}

