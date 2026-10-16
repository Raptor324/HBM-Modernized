package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MukeFlashParticle extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/particle/flare.png");

    private final boolean balefire;

    public MukeFlashParticle(ClientLevel level, double x, double y, double z, boolean balefire) {
        super(level, x, y, z);
        this.balefire = balefire;
        this.lifetime = 20;
        this.noClip = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.age == 15) {
            for (double d = 0.0D; d <= 1.8D; d += 0.1) {
                ParticleEngineNT.INSTANCE.add(getCloud(
                        level, x, y, z,
                        random.nextGaussian() * 0.05,
                        d + random.nextGaussian() * 0.02,
                        random.nextGaussian() * 0.05));
            }

            for (int i = 0; i < 100; i++) {
                ParticleEngineNT.INSTANCE.add(getCloud(
                        level, x, y + 0.5, z,
                        random.nextGaussian() * 0.5,
                        random.nextInt(5) == 0 ? 0.02 : 0,
                        random.nextGaussian() * 0.5));
            }

            for (int i = 0; i < 75; i++) {
                double dx = random.nextGaussian() * 0.5;
                double dz = random.nextGaussian() * 0.5;

                if (dx * dx + dz * dz > 1.5) {
                    dx *= 0.5;
                    dz *= 0.5;
                }

                double dy = 1.8 + (random.nextDouble() * 3 - 1.5) * (0.75 - (dx * dx + dz * dz)) * 0.5;

                ParticleEngineNT.INSTANCE.add(getCloud(
                        level, x, y, z,
                        dx, dy + random.nextGaussian() * 0.02, dz));
            }
        }
    }

    private MukeCloudParticle getCloud(ClientLevel level, double x, double y, double z,
                                       double mx, double my, double mz) {
        if (this.balefire) {
            return new MukeCloudBFParticle(level, x, y, z, mx, my, mz);
        }
        return new MukeCloudParticle(level, x, y, z, mx, my, mz);
    }

    /**
     * В обычном проходе частиц вспышка НЕ рисуется: она обязана быть
     * поверх всех остальных частиц, а те идут в том же батче раньше.
     * Вся отрисовка — в {@link #renderFlashOnly}, который EngineHandler
     * вызывает отдельным проходом ПОСЛЕ частиц и мешей.
     */
    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTicks, PoseStack levelPoseStack) {
    }

    @Override
    public void renderFlashOnly(MultiBufferSource buffer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        FogRenderer.setupNoFog();

        RandomSource rand = RandomSource.create();
        Vec3 off = virtualizedOffset(
                Mth.lerp(partialTicks, this.xo, this.x),
                Mth.lerp(partialTicks, this.yo, this.y),
                Mth.lerp(partialTicks, this.zo, this.z),
                camera);
        float dX = (float) off.x;
        float dY = (float) off.y;
        float dZ = (float) off.z;

        this.alpha = Mth.clamp(1F - ((this.age + partialTicks) / (float) this.lifetime), 0F, 1F);
        // Fallback-виртуализация: сжатие размера вместе со смещением (см. ParticleNT.virtualScale).
        float vScale = virtualScale(
                Mth.lerp(partialTicks, this.xo, this.x),
                Mth.lerp(partialTicks, this.yo, this.y),
                Mth.lerp(partialTicks, this.zo, this.z),
                camera);
        float scale = ((this.age + partialTicks) * 3F + 1F) * vScale;

        Vector3f left = new Vector3f(camera.getLeftVector()).mul(scale);
        Vector3f up = new Vector3f(camera.getUpVector()).mul(scale);

        // ИЗОЛИРОВАННЫЙ буфер движка (правило CustomRenderTypes): sortOnUpload-типы
        // не должны регистрироваться в общем bufferSource под ImmediatelyFast.
        VertexConsumer consumer = ParticleEngineNT.buffer()
                .getBuffer(getRenderType());

        // Внешнее свечение: 24 лепестка, тёплый белый. Блендинг NUKE_FLASH
        // аддитивный (LIGHTNING), поэтому «ослепительность» = альфа: 0.85
        // вместо прежних 0.5.
        for (int i = 0; i < 24; i++) {
            rand.setSeed(i * 31L + 1L);

            float pX = (float) (dX + (rand.nextDouble() * 15 - 7.5) * vScale);
            float pY = (float) (dY + (rand.nextDouble() * 7.5 - 3.75) * vScale);
            float pZ = (float) (dZ + (rand.nextDouble() * 15 - 7.5) * vScale);

            ImmediateVertexWriter.billboardQuad(consumer, null, pX, pY, pZ, left, up,
                    1.0F, 0.9F, 0.75F, alpha * 0.85F, 1F, 1F, 0F, 0F);
        }

        // Раскалённое ядро: чистый белый, насыщает аддитивный блендинг
        // до сплошного белого в центре вспышки.
        Vector3f coreLeft = new Vector3f(camera.getLeftVector()).mul(scale * 0.45F);
        Vector3f coreUp = new Vector3f(camera.getUpVector()).mul(scale * 0.45F);
        ImmediateVertexWriter.billboardQuad(consumer, null, dX, dY, dZ, coreLeft, coreUp,
                1.0F, 1.0F, 1.0F, alpha, 1F, 1F, 0F, 0F);
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.NUKE_FLASH.apply(TEXTURE);
    }
}
