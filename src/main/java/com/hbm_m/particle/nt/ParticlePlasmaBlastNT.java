package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1:1-Port von {@code ParticlePlasmaBlast} ("plasmablast"): eine flache, additive Stosswellenscheibe,
 * um {@code yaw} und {@code pitch} gekippt, die sich exponentiell auf {@code particleScale} oeffnet.
 */
public class ParticlePlasmaBlastNT extends ParticleQuadNT {

    private static final ResourceLocation SHOCKWAVE = tex("shockwave");
    private final float pitch;
    private final float yaw;

    public ParticlePlasmaBlastNT(ClientLevel level, double x, double y, double z, float r, float g, float b, float pitch, float yaw) {
        super(level, x, y, z, SHOCKWAVE);
        this.lifetime = 20;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.pitch = pitch;
        this.yaw = yaw;
        this.fullBright = true;
    }

    public void setMaxAge(int maxAge) { this.lifetime = maxAge; }
    public void setScale(float scale) { this.particleScale = scale; }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.dead = true;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Vec3 p = camPos(camera, pt);
        float a = 1 - ((this.age + pt) / (float) this.lifetime);
        float scale = (1 - (float) Math.pow(Math.E, (this.age + pt) * -0.125)) * this.particleScale;
        float[][] v = { { -1, -1, 1, 1 }, { -1, 1, 1, 0 }, { 1, 1, 0, 0 }, { 1, -1, 0, 1 } };
        for (float[] corner : v) {
            Vector3f vec = new Vector3f(corner[0] * scale, 0, corner[1] * scale)
                    .rotateX((float) Math.toRadians(pitch))
                    .rotateY((float) Math.toRadians(yaw));
            vertex(c, (float) p.x + vec.x, (float) p.y + vec.y, (float) p.z + vec.z, rCol, gCol, bCol, a, corner[2], corner[3], 0xF000F0);
        }
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.ADDITIVE_PARTICLES.apply(texture);
    }
}
