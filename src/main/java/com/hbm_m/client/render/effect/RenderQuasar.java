package com.hbm_m.client.render.effect;

import com.hbm_m.entity.effect.BlackHoleEntity;
import com.hbm_m.lib.RefStrings;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Порт {@code com.hbm.render.entity.effect.RenderQuasar}.
 */
public class RenderQuasar extends RenderBlackHole<BlackHoleEntity> {

    private static final ResourceLocation QUASAR_DISC = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/bhole_d.png");

    public RenderQuasar(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected ResourceLocation discTex() {
        return QUASAR_DISC;
    }

    @Override
    protected int[] colorFromIteration(int iteration, float alpha) {
        int a = (int) (alpha * 255);
        float g = (float) Math.pow(iteration / 15F, 2);
        float b = (float) Math.pow(iteration / 15F, 2);
        return new int[]{255, (int) (g * 255), (int) (b * 255), a};
    }
}
