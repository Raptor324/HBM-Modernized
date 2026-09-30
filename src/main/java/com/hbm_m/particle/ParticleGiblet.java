package com.hbm_m.particle;

import java.util.function.Function;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.nt.ParticleNT;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.Util;

/**
 * Порт {@code ParticleGiblet} (1.7.10): куски мяса/слизи/металла. Вращаются в полёте
 * (momentumYaw/Pitch), гравитация 2 (металл 4), время жизни 140+rand(20); пока летят,
 * не-металл оставляет пыль блока (мясо - редстоун, слизь - дыня).
 */
public class ParticleGiblet extends ParticleNT {

    public static final int TYPE_MEAT = 0;
    public static final int TYPE_SLIME = 1;
    public static final int TYPE_METAL = 2;

    private static final ResourceLocation TEXTURE_MEAT = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/meat.png");
    private static final ResourceLocation TEXTURE_SLIME = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/slime.png");
    private static final ResourceLocation TEXTURE_METAL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/metal.png");

    private static final Function<ResourceLocation, RenderType> TYPE = Util.memoize(
            texture -> GibletRenderTypes.create(texture));

    private final int gibType;
    private final float momentumYaw;
    private final float momentumPitch;

    public ParticleGiblet(ClientLevel level, double x, double y, double z,
                          double mx, double my, double mz, int gibType) {
        super(level, x, y, z);
        this.xd = mx;
        this.yd = my;
        this.zd = mz;
        this.gibType = gibType;
        this.lifetime = 140 + this.random.nextInt(20);
        this.gravity = gibType == TYPE_METAL ? 4.0F : 2.0F;
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
        this.momentumYaw = (float) (this.random.nextGaussian() * 15.0);
        this.momentumPitch = (float) (this.random.nextGaussian() * 15.0);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();

        // Вращение в полёте; на земле кусок лежит.
        if (!this.onGround) {
            this.roll += (Math.abs(this.xd) + Math.abs(this.zd) + Math.abs(this.yd))
                    * (this.momentumYaw + this.momentumPitch) * 0.01F;
        }

        // Пыльной след не-металла, пока летит.
        if (this.gibType != TYPE_METAL && !this.onGround && this.age < this.lifetime - 1) {
            var state = this.gibType == TYPE_SLIME ? Blocks.MELON.defaultBlockState() : Blocks.REDSTONE_BLOCK.defaultBlockState();
            this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    this.x, this.y, this.z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        Vec3 off = virtualizedOffset(
                Mth.lerp(partialTicks, this.xo, this.x),
                Mth.lerp(partialTicks, this.yo, this.y),
                Mth.lerp(partialTicks, this.zo, this.z),
                camera);
        float pX = (float) off.x;
        float pY = (float) off.y;
        float pZ = (float) off.z;

        float scale = this.quadSize;
        int light = getLightColor();
        float roll = Mth.lerp(partialTicks, this.oRoll, this.roll);

        org.joml.Quaternionf camQ = new org.joml.Quaternionf(camera.rotation());
        camQ.mul(Axis.ZP.rotationDegrees(roll));
        org.joml.Vector3f left = new org.joml.Vector3f(-1, 0, 0).rotate(camQ);
        org.joml.Vector3f up = new org.joml.Vector3f(0, 1, 0).rotate(camQ);

        int r = Mth.clamp((int) (this.rCol * 255F), 0, 255);
        int g = Mth.clamp((int) (this.gCol * 255F), 0, 255);
        int b = Mth.clamp((int) (this.bCol * 255F), 0, 255);
        int a = Mth.clamp((int) (this.alpha * 255F), 0, 255);

        float[][] corners = {
                { -1, -1, 0, 1 },
                { -1,  1, 0, 0 },
                {  1,  1, 1, 0 },
                {  1, -1, 1, 1 },
        };
        for (float[] cn : corners) {
            float ox = left.x * cn[0] * scale + up.x * cn[1] * scale;
            float oy = left.y * cn[0] * scale + up.y * cn[1] * scale;
            float oz = left.z * cn[0] * scale + up.z * cn[1] * scale;
            float u = cn[2];
            float v = cn[3];
            //? if < 1.21.1 {
            consumer.vertex(pX + ox, pY + oy, pZ + oz)
                    .color(r, g, b, a)
                    .uv(u, v)
                    .uv2(light)
                    .endVertex();
            //?} else {
            /*consumer.addVertex(pX + ox, pY + oy, pZ + oz)
                    .setColor(r, g, b, a)
                    .setUv(u, v)
                    .setLight(light);
            *///?}
        }
    }

    @Override
    public RenderType getRenderType() {
        return TYPE.apply(switch (this.gibType) {
            case TYPE_SLIME -> TEXTURE_SLIME;
            case TYPE_METAL -> TEXTURE_METAL;
            default -> TEXTURE_MEAT;
        });
    }

    /** Кусок: непрозрачный квад со светом мира. */
    private static final class GibletRenderTypes extends RenderType {
        static RenderType create(ResourceLocation texture) {
            return create("hbm_m_giblet", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS, 512, false, true,
                    RenderType.CompositeState.builder()
                            .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                            .setLightmapState(LIGHTMAP)
                            .setCullState(NO_CULL)
                            .createCompositeState(false));
        }

        private GibletRenderTypes(String s, VertexFormat v, VertexFormat.Mode m,
                                  int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }
    }
}
