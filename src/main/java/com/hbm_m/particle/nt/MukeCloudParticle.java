package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MukeCloudParticle extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/particle/explosion.png");

    public MukeCloudParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        if (yd > 0) {
            this.friction = 0.9F;
            if (yd > 0.1) {
                this.lifetime = 92 + random.nextInt(11) + (int) (yd * 20);
            } else {
                this.lifetime = 72 + random.nextInt(11);
            }
        } else if (yd == 0) {
            this.friction = 0.95F;
            this.lifetime = 52 + random.nextInt(11);
        } else {
            this.friction = 0.85F;
            this.lifetime = 122 + random.nextInt(31);
            this.age = 80;
        }
    }

    @Override
    public void tick() {
        this.noClip = this.age <= 2;

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime - 2) {
            this.dead = true;
            return;
        }

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= friction;
        this.yd *= friction;
        this.zd *= friction;

        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        if (this.age > this.lifetime) return;

        int texIndex = this.age * 25 / this.lifetime;
        float f0 = 1F / 5F;
        float uMin = texIndex % 5 * f0;
        float uMax = uMin + f0;
        float vMin = texIndex / 5 * f0;
        float vMax = vMin + f0;

        this.alpha = 1F;
        this.quadSize = 3F;

        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        Vector3f left = new Vector3f(camera.getLeftVector()).mul(quadSize);
        Vector3f up = new Vector3f(camera.getUpVector()).mul(quadSize);

        VertexConsumer consumer = net.minecraft.client.Minecraft.getInstance()
                .renderBuffers().bufferSource()
                .getBuffer(getRenderType());

        ImmediateVertexWriter.billboardQuad(consumer, null, pX, pY, pZ, left, up,
                rCol, gCol, bCol, alpha, uMax, vMax, uMin, vMin);
    }

    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.NUKE_CLOUDS.apply(getTexture());
    }
}
