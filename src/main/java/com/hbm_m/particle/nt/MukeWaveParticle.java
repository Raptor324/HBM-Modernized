package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class MukeWaveParticle extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/particle/shockwave.png");

    private float waveScale = 45F;

    public MukeWaveParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 25;
        this.noClip = true;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        FogRenderer.setupNoFog();

        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        this.alpha = Mth.clamp(1F - ((this.age + partialTicks) / (float) this.lifetime), 0F, 1F);
        float scale = (1F - (float) Math.pow(Math.E, (this.age + partialTicks) * -0.125D)) * waveScale;

        VertexConsumer consumer = net.minecraft.client.Minecraft.getInstance()
                .renderBuffers().bufferSource()
                .getBuffer(getRenderType());

        ImmediateVertexWriter.worldQuad(consumer,
                pX - scale, pY - 0.25F, pZ - scale,
                pX - scale, pY - 0.25F, pZ + scale,
                pX + scale, pY - 0.25F, pZ + scale,
                pX + scale, pY - 0.25F, pZ - scale,
                1F, 1F, 1F, alpha,
                1F, 1F, 0F, 0F);
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.NUKE_FLASH.apply(TEXTURE);
    }
}
