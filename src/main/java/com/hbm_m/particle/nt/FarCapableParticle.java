package com.hbm_m.particle.nt;

import net.minecraft.resources.ResourceLocation;

/**
 * Частица, которую можно рисовать в дальнем контенте (DH FBO):
 * шейдер nuke_cloud + текстура NUKE_CLOUDS_DH.
 */
public interface FarCapableParticle {
    ResourceLocation hbm$getFarTexture();
}
