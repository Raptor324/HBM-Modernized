package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}


import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Renderer for logical/invisible entities (EntityNukeExplosionMK5, AirstrikeAgentEntity, etc.).
 * Draws nothing; it exists only so the EntityRenderDispatcher never receives a null renderer.
 */
@OnlyIn(Dist.CLIENT)
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
