package com.hbm_m.client.render.entity.mob;

import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Слой «брони» крипера с настраиваемой текстурой (аналог {@code shouldRenderPass} + overlay в 1.7.10).
 */
public class CreeperUniversalPowerLayer extends EnergySwirlLayer<Creeper, CreeperModel<Creeper>> {

    private final ResourceLocation armorTexture;
    private final CreeperModel<Creeper> armorModel;

    public CreeperUniversalPowerLayer(
            RenderLayerParent<Creeper, CreeperModel<Creeper>> renderer,
            EntityModelSet modelSet,
            ResourceLocation armorTexture) {
        super(renderer);
        this.armorTexture = armorTexture;
        this.armorModel = new CreeperModel<>(modelSet.bakeLayer(ModelLayers.CREEPER_ARMOR));
    }

    @Override
    protected float xOffset(float tickCount) {
        return tickCount * 0.01F;
    }

    @Override
    protected ResourceLocation getTextureLocation() {
        return this.armorTexture;
    }

    @Override
    protected EntityModel<Creeper> model() {
        return this.armorModel;
    }
}
