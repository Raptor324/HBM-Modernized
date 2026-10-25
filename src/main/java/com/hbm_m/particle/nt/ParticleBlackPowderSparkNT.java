package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

/** 1:1-Port von {@code ParticleBlackPowderSpark}: gelber Funke aus dem Mündungsfeuer, voll hell. */
public class ParticleBlackPowderSparkNT extends ParticleQuadNT {

    public ParticleBlackPowderSparkNT(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ) {
        super(level, x, y, z, TextureAtlas.LOCATION_PARTICLES);
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        float f = this.random.nextFloat() * 0.1F + 0.2F;
        this.rCol = f + 0.7F;
        this.gCol = f + 0.5F;
        this.bCol = f;
        this.bbWidth = 0.02F;
        this.bbHeight = 0.02F;
        setPos(x, y, z);
        this.particleScale = (random.nextFloat() * 0.5F + 0.5F) * 2.0F;
        this.particleScale *= this.random.nextFloat() * 0.6F + 0.5F;
        this.lifetime = 15 + this.random.nextInt(5);
        this.gravity = 0.01F;
        this.fullBright = true;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.yd -= gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.95D;
        this.yd *= 0.95D;
        this.zd *= 0.95D;
        if (this.lifetime-- <= 0) this.dead = true;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        TextureAtlasSprite sprite = ParticleSmokeFXNT.genericSprite(0);
        Vec3 p = camPos(camera, pt);
        quad(buffer(), (float) p.x, (float) p.y, (float) p.z, left(camera), up(camera), 0.1F * particleScale,
                rCol, gCol, bCol, alpha, 0xF000F0, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
    }
}
