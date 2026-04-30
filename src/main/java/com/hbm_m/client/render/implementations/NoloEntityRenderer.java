package com.hbm_m.client.render.implementations;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Fox;

public class NoloEntityRenderer extends FoxRenderer {

    private static final ResourceLocation NOLO_TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/entity/nolo.png");

    public NoloEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Fox fox) {
        return NOLO_TEXTURE;
    }
}