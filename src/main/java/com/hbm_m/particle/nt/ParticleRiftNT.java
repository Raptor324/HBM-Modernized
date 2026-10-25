package com.hbm_m.particle.nt;

import java.util.List;
import java.util.Map;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code ParticleRift} ("rift"): fuenf ineinanderliegende Kugelschalen
 * ({@code sphere_uv.obj}), untexturiert und farbinvertierend geblendet, die zehn Ticks lang mit
 * {@code (age + interp) * 0.5} wachsen. Die Schalenfaktoren 1.02/1.05 multiplizieren sich wie die
 * aufeinanderfolgenden {@code glScalef} des Originals.
 */
public class ParticleRiftNT extends ParticleNT {

    private static final float[] SHELLS = { 1F, 1.02F, 1.02F * 1.05F, 1.02F * 1.05F * 1.02F, 1.02F * 1.05F * 1.02F * 1.05F };

    public ParticleRiftNT(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 10;
        this.xd = this.yd = this.zd = 0;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj("models/sphere_uv.obj");
        if (obj.isEmpty()) return;
        VertexConsumer c = ParticleEngineNT.buffer().getBuffer(getRenderType());
        Vec3 p = virtualizedOffset(Mth.lerp(pt, xo, x), Mth.lerp(pt, yo, y), Mth.lerp(pt, zo, z), camera);
        float scale = (this.age + pt) * 0.5F;

        for (float shell : SHELLS) {
            float s = scale * shell;
            for (List<float[]> tris : obj.values()) {
                for (float[] t : tris) {
                    for (int i = 0; i < 3; i++) {
                        float vx = (float) p.x + t[i * 8] * s;
                        float vy = (float) p.y + t[i * 8 + 1] * s;
                        float vz = (float) p.z + t[i * 8 + 2] * s;
                        //? if < 1.21.1 {
                        c.vertex(vx, vy, vz).color(255, 255, 255, 255).endVertex();
                        //?} else {
                        /*c.addVertex(vx, vy, vz).setColor(255, 255, 255, 255);
                        *///?}
                    }
                }
            }
        }
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.INVERT_TRIANGLES;
    }
}
