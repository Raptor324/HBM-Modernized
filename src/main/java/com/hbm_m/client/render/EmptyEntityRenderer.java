package com.hbm_m.client.render;


import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

/**
 * Рендерер для логических/невидимых сущностей (NukeExplosionMK5Entity, AirstrikeAgentEntity и т.д.).
 * Ничего не рисует; нужен только чтобы EntityRenderDispatcher не получал null.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class EmptyEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation DUMMY = new ResourceLocation("minecraft", "textures/block/stone.png");
    *///?} else {
        private static final ResourceLocation DUMMY = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
    //?}


    public EmptyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return DUMMY;
    }
}
