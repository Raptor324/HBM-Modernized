package com.hbm_m.client.render;


import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Рендерер для логических/невидимых сущностей (EntityNukeExplosionMK5, AirstrikeAgentEntity и т.д.).
 * Ничего не рисует; нужен только чтобы EntityRenderDispatcher не получал null.
 */
//? if forge {
/*@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
*///?} elif neoforge {
@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
//?}
public class EmptyEntityRenderer<T extends Entity> extends EntityRenderer<T> {

        private static final ResourceLocation DUMMY = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");


    public EmptyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return DUMMY;
    }
}
