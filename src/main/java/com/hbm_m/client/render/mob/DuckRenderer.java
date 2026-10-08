package com.hbm_m.client.render.mob;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.lib.RefStrings;

import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;

/** 1:1 {@code RenderDuck}: Huhnmodell mit Entenhaut. */
public class DuckRenderer extends ChickenRenderer {

    public static final ResourceLocation DUCC = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/duck.png");

    public DuckRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Chicken entity) {
        return DUCC;
    }
}
