package com.hbm_m.client.render.mob;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.mob.EntityParasiteMaggot;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderMaggot}: Silberfischmodell mit eigener Textur, kippt beim Tod um 180 Grad. */
public class ParasiteMaggotRenderer extends MobRenderer<EntityParasiteMaggot, SilverfishModel<EntityParasiteMaggot>> {

    public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/parasite_maggot.png");

    public ParasiteMaggotRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SilverfishModel<>(ctx.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    protected float getFlipDegrees(EntityParasiteMaggot entity) {
        return 180.0F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(EntityParasiteMaggot entity) {
        return texture;
    }
}
