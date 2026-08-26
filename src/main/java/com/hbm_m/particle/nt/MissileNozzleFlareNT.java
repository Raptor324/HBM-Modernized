package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler.CustomRenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Порт {@link com.hbm_m.particle.custom.MissileNozzleFlareParticle} на NT-движок:
 * блики двигателя у сопла ракеты (flash + flare), жёлтый оттенок, мерцание.
 * Рендерится аддитивно ({@link CustomRenderTypes#nukeGlowAdd}).
 */
public class MissileNozzleFlareNT extends MissileTrailNT {

    /** SpriteSet прокидывается из Provider при регистрации (ClientParticleHandler). */
    public static volatile SpriteSet sprites;

    public static void setSprites(SpriteSet set) { sprites = set; }

    /** 0 = flash.png, 1 = nuke_explosion_flare.png */
    private final int textureLayer;
    private final float flickerPhase;
    private final float sizePhase;
    private final float baseQuadSize;
    private final float baseR;
    private final float baseG;
    private final float baseB;
    private final float alphaMul;
    private final Vec3 carryVelocity;

    public MissileNozzleFlareNT(ClientLevel level, double x, double y, double z,
                                double dx, double dy, double dz,
                                float scale, int layer) {
        super(level, x, y, z);
        this.textureLayer = Mth.clamp(layer, 0, 1);
        this.carryVelocity = new Vec3(dx, dy, dz);
        this.lifetime = 6 + this.random.nextInt(4);
        cacheSpriteUv(sprites.get(this.textureLayer, 2));
        this.flickerPhase = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.sizePhase = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.baseQuadSize = (0.28F + this.random.nextFloat() * 0.18F) * scale;

        if (this.textureLayer == 0) {
            this.baseR = 1.0F;
            this.baseG = 0.9F + this.random.nextFloat() * 0.1F;
            this.baseB = 0.4F + this.random.nextFloat() * 0.12F;
            this.alphaMul = 0.95F;
        } else {
            this.baseR = 1.0F;
            this.baseG = 0.78F + this.random.nextFloat() * 0.12F;
            this.baseB = 0.12F + this.random.nextFloat() * 0.15F;
            this.alphaMul = 0.81F;
        }
        this.rCol = this.baseR;
        this.gCol = this.baseG;
        this.bCol = this.baseB;
        this.alpha = 0.75F + this.random.nextFloat() * 0.2F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float pulse = 0.42F + 0.58F * Mth.sin(this.age * 1.35F + this.flickerPhase);
        float sizePulse = 0.8F + 0.35F * Mth.sin(this.age * 1.05F + this.sizePhase);
        this.alpha = pulse * this.alphaMul;
        this.quadSize = this.baseQuadSize * sizePulse;

        float brighten = 0.88F + 0.12F * pulse;
        this.rCol = Math.min(1.0F, this.baseR * brighten);
        this.gCol = Math.min(1.0F, this.baseG * brighten);
        this.bCol = Math.min(1.0F, this.baseB * (0.92F + 0.08F * pulse));

        this.move(this.carryVelocity.x, this.carryVelocity.y, this.carryVelocity.z);
    }

    @Override
    public RenderType getRenderType() {
        return CustomRenderTypes.nukeGlowAdd();
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks,
                       com.mojang.blaze3d.vertex.PoseStack levelPoseStack) {
        double wx = Mth.lerp(partialTicks, this.xo, this.x);
        double wy = Mth.lerp(partialTicks, this.yo, this.y);
        double wz = Mth.lerp(partialTicks, this.zo, this.z);
        Vec3 rel = virtualize(wx, wy, wz, camera);

        emitBillboard(consumer, camera,
                (float) rel.x, (float) rel.y, (float) rel.z,
                this.quadSize,
                this.rCol, this.gCol, this.bCol, this.alpha);
    }
}
