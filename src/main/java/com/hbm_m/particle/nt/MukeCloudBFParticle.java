package com.hbm_m.particle.nt;

import com.hbm_m.lib.RefStrings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

public class MukeCloudBFParticle extends MukeCloudParticle {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/particle/explosion_bf.png");

    public MukeCloudBFParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}
