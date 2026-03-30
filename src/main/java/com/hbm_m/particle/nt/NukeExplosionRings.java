package com.hbm_m.particle.nt;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Расширяющиеся огненные/дымовые кольца и мягкое свечение ядерного взрыва.
 * Портировано из explosiveideas (NukeExplosionEmitterParticle), без вспышки и тряски камеры.
 * Все эффекты затухают со временем; рендер только на клиенте.
 */
public class NukeExplosionRings extends ParticleNT {

    private static final ResourceLocation TEXTURE_FLARE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/nuke_explosion_flare.png");
    private static final ResourceLocation TEXTURE_FIRE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/nuke_explosion.png");
    private static final ResourceLocation TEXTURE_SMOKE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/nuke_explosion_smoke.png");

    private static final int MAX_SUB_PARTICLES = 1_500;
    private static final int FLARE_LIFETIME_MUL = 4;
    private static final int FIRE_LIFETIME_MUL = 40;
    private static final int SMOKE_LIFETIME_MUL = 600;

    private float scale = 1f;
    private boolean initialized;
    private final List<Ring> rings = new ArrayList<>();
    private int maxLifetime;

    public NukeExplosionRings(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.noClip = true;
        this.lifetime = 100_000;
    }

    public NukeExplosionRings setScale(float scale) {
        this.scale = Mth.clamp(scale, 0.5f, 5f);
        return this;
    }

    private double getLodFactor() {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (cam == null) return 0.35;
        Vec3 camPos = cam.getPosition();
        double dist = Math.sqrt(camPos.distanceToSqr(this.x, this.y, this.z));
        if (dist < 128) return 1.0;
        if (dist < 256) return 0.75;
        if (dist < 384) return 0.5;
        return 0.35;
    }

    private void initRings() {
        if (initialized) return;
        initialized = true;

        int pow = (int) Mth.clamp(scale * 2.5, 1, 50);
        double lodFactor = getLodFactor();
        int numPoints = Mth.clamp((int) (pow * lodFactor), 1, 30);

        int added = 0;
        for (int p = 0; p < numPoints && added < MAX_SUB_PARTICLES; p++) {
            double ox = this.x + (random.nextDouble() - 0.5) * 2.0 * scale * pow;
            double oy = this.y + (random.nextDouble() - 0.5) * 2.0 * scale * pow;
            double oz = this.z + (random.nextDouble() - 0.5) * 2.0 * scale * pow;

            // Flare: одна на точку, большой размер, альфа 0 -> 0.5 -> 0
            if (added < MAX_SUB_PARTICLES) {
                float flareSize = 20f * scale * (float) pow * 0.1f;
                int life = FLARE_LIFETIME_MUL * Math.max(1, pow);
                rings.add(new Ring(ox, oy, oz, 0, 0, 0, 0, flareSize, 0f, life, RingType.FLARE));
                added++;
            }

            // Fire и smoke из одной точки
            float sizeBase = random.nextFloat() * (4 * pow) * 0.5f + (2 * pow);
            float motionX = (float) (random.nextGaussian() * pow / 20.0);
            float motionY = pow <= 10 ? random.nextFloat() * pow / 10f * 0.5f : 0f;
            float motionZ = (float) (random.nextGaussian() * pow / 20.0);

            for (int i = 0; i < 8 && added < MAX_SUB_PARTICLES; i++) {
                float size = sizeBase * (0.7f + random.nextFloat() * 0.6f);
                int life = FIRE_LIFETIME_MUL * Math.max(1, pow) + i * Math.max(1, pow);
                float ax = (float) (random.nextDouble() * 0.1);
                float ay = (float) (random.nextDouble() * 0.1);
                float az = (float) (random.nextDouble() * 0.1);
                rings.add(new Ring(ox, oy, oz, motionX + ax, motionY + ay, motionZ + az, 0, size, 0.1f, life, RingType.FIRE));
                added++;
                if (added < MAX_SUB_PARTICLES && i % 2 == 0) {
                    rings.add(new Ring(ox, oy, oz, motionX + ax, motionY + ay, motionZ + az, 0, size * 2f, 0.005f, life, RingType.FIRE));
                    added++;
                }
            }
            for (int i = 0; i < 5 && added < MAX_SUB_PARTICLES; i++) {
                float size = sizeBase * (0.5f + random.nextFloat() * 0.5f);
                int life = SMOKE_LIFETIME_MUL * Math.max(1, pow) + i * Math.max(1, pow) * 3;
                float mx = motionX / 5f + (float) (random.nextGaussian() * 0.02);
                float my = motionY + 0.02f;
                float mz = motionZ / 5f + (float) (random.nextGaussian() * 0.02);
                rings.add(new Ring(ox, oy, oz, mx, my, mz, 0, size, 0.1f, life, RingType.SMOKE));
                added++;
            }
        }

        maxLifetime = 0;
        for (Ring r : rings) {
            if (r.lifetime > maxLifetime) maxLifetime = r.lifetime;
        }
        maxLifetime += 20;
    }

    @Override
    public void tick() {
        this.age++;
        if (!initialized) {
            initRings();
        }

        for (Ring r : rings) {
            r.age++;
        }
        rings.removeIf(r -> r.age >= r.lifetime);

        if (rings.isEmpty() || age > maxLifetime) {
            remove();
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        if (!initialized) return;

        Vec3 camPos = camera.getPosition();
        PoseStack localPose = new PoseStack();
        localPose.translate(this.x - camPos.x, this.y - camPos.y, this.z - camPos.z);
        Matrix4f matrix = localPose.last().pose();

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        renderRings(buffer.getBuffer(ClientRenderHandler.CustomRenderTypes.NUKE_CLOUDS.apply(TEXTURE_FLARE)), matrix, camera, partialTicks, RingType.FLARE);
        renderRings(buffer.getBuffer(ClientRenderHandler.CustomRenderTypes.NUKE_CLOUDS.apply(TEXTURE_FIRE)), matrix, camera, partialTicks, RingType.FIRE);
        renderRings(buffer.getBuffer(ClientRenderHandler.CustomRenderTypes.NUKE_CLOUDS.apply(TEXTURE_SMOKE)), matrix, camera, partialTicks, RingType.SMOKE);
    }

    private void renderRings(VertexConsumer consumer, Matrix4f matrix, Camera camera, float partialTicks, RingType type) {
        Vector3f leftV = camera.getLeftVector();
        Vector3f upV = camera.getUpVector();
        int light = 240;
        int overlay = OverlayTexture.NO_OVERLAY;

        for (Ring r : rings) {
            if (r.type != type) continue;

            float ageF = r.age + partialTicks;
            if (ageF >= r.lifetime) continue;

            float t = ageF / (float) r.lifetime;
            float scaleNow = getScaleExpoOut(t, r.scaleStart, r.scaleEnd);
            float alpha = getAlphaForType(r, t);

            float posX = (float) (r.posX - this.x + r.motionX * ageF);
            float posY = (float) (r.posY - this.y + r.motionY * ageF);
            float posZ = (float) (r.posZ - this.z + r.motionZ * ageF);

            Vector3f l = new Vector3f(leftV).mul(scaleNow);
            Vector3f u = new Vector3f(upV).mul(scaleNow);

            float rCol = 1f, gCol = 1f, bCol = 1f;
            if (type == RingType.FIRE) {
                gCol = 0.9f;
                bCol = 0.5f;
            } else if (type == RingType.SMOKE) {
                rCol = gCol = bCol = 0.85f;
            }

            consumer.vertex(matrix, posX - l.x - u.x, posY - l.y - u.y, posZ - l.z - u.z).color(rCol, gCol, bCol, alpha).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
            consumer.vertex(matrix, posX - l.x + u.x, posY - l.y + u.y, posZ - l.z + u.z).color(rCol, gCol, bCol, alpha).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
            consumer.vertex(matrix, posX + l.x + u.x, posY + l.y + u.y, posZ + l.z + u.z).color(rCol, gCol, bCol, alpha).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
            consumer.vertex(matrix, posX + l.x - u.x, posY + l.y - u.y, posZ + l.z - u.z).color(rCol, gCol, bCol, alpha).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        }
    }

    /** EXPO_OUT: scale от scaleStart к scaleEnd */
    private static float getScaleExpoOut(float t, float start, float end) {
        float x = 1f - (float) Math.pow(2, -10 * t);
        return start + (end - start) * x;
    }

    /** Альфа в зависимости от типа: FLARE — 0 -> 0.5 -> 0 (CIRC_OUT), FIRE/SMOKE — SINE_OUT к 0 */
    private static float getAlphaForType(Ring r, float t) {
        if (r.type == RingType.FLARE) {
            return 2f * t * (1f - t);
        }
        return r.alphaStart * (float) Math.sin((1 - t) * Math.PI / 2);
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.NUKE_CLOUDS.apply(TEXTURE_FLARE);
    }

    private enum RingType { FLARE, FIRE, SMOKE }

    private static final class Ring {
        final double posX, posY, posZ;
        final float motionX, motionY, motionZ;
        final float scaleStart, scaleEnd;
        final float alphaStart;
        final int lifetime;
        final RingType type;
        int age;

        Ring(double posX, double posY, double posZ, float motionX, float motionY, float motionZ,
             float scaleStart, float scaleEnd, float alphaStart, int lifetime, RingType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.scaleStart = scaleStart;
            this.scaleEnd = scaleEnd;
            this.alphaStart = alphaStart;
            this.lifetime = lifetime;
            this.type = type;
        }
    }
}
