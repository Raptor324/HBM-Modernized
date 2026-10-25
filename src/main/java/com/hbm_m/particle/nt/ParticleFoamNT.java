package com.hbm_m.particle.nt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 1:1-Port von {@code ParticleFoam} ("smoke"/"foamSplash"): Schaumblasen mit Schleppe. */
public class ParticleFoamNT extends ParticleQuadNT {

    private float baseScale = 1.0F;
    private float maxScale = 1.5F;
    private final List<double[]> trail = new ArrayList<>();
    private int trailLength = 15;
    private float buoyancy = 0.05F;
    private final float jitter = 0.15F;
    private final float drag = 0.96F;
    private int explosionPhase;

    public ParticleFoamNT(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, PARTICLE_BASE);
        this.lifetime = 60 + random.nextInt(60);
        this.gravity = 0.005F + random.nextFloat() * 0.015F;
        float initialVelocity = 2.0F + random.nextFloat() * 3.0F;
        this.yd = initialVelocity;
        double angle = random.nextDouble() * Math.PI * 2;
        double strength = random.nextDouble() * 0.5;
        this.xd = Math.cos(angle) * strength;
        this.zd = Math.sin(angle) * strength;
        this.explosionPhase = 0;
        this.particleScale = 0.3F + random.nextFloat() * 0.7F;
    }

    public void setBaseScale(float f) { this.baseScale = f; }
    public void setMaxScale(float f) { this.maxScale = f; }
    public void setTrailLength(int length) { this.trailLength = length; }
    public void setBuoyancy(float buoyancy) { this.buoyancy = buoyancy; }
    public void setMaxAge(int maxAge) { this.lifetime = maxAge; }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        trail.add(0, new double[] { x, y, z });
        while (trail.size() > trailLength) trail.remove(trail.size() - 1);

        ++this.age;
        if (this.age == this.lifetime) this.dead = true;

        float phaseRatio = (float) age / (float) lifetime;
        if (phaseRatio < 0.3F) {
            explosionPhase = 0;
            if (phaseRatio < 0.15F) {
                yd += buoyancy * 6.0F;
            } else {
                yd += buoyancy * (1.0F - (phaseRatio / 0.3F)) * 2.0F;
            }
            particleScale = baseScale + (maxScale - baseScale) * (phaseRatio / 0.3F);
        } else if (phaseRatio < 0.6F) {
            explosionPhase = 1;
            yd *= 0.98F;
            particleScale = maxScale;
        } else {
            explosionPhase = 2;
            yd -= gravity;
            particleScale = maxScale * (1.0F - ((phaseRatio - 0.6F) / 0.4F) * 0.7F);
        }

        alpha = 0.8F * (1.0F - phaseRatio * phaseRatio);
        xd += (random.nextFloat() - 0.5F) * jitter;
        zd += (random.nextFloat() - 0.5F) * jitter;
        xd *= drag;
        yd *= drag;
        zd *= drag;
        this.move(xd, yd, zd);
        if (this.onGround) this.dead = true;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Vector3f l = left(camera);
        Vector3f u = up(camera);
        Vec3 cam = camera.getPosition();
        int light = light();

        bubbles(c, l, u, cam, light, x, y, z, particleScale, alpha);
        for (int i = 1; i < trail.size(); i++) {
            double[] point = trail.get(i);
            float trailScale = particleScale * (1.0F - (float) i / trailLength);
            float trailAlpha = alpha * (1.0F - (float) i / trailLength) * 0.7F;
            bubbles(c, l, u, cam, light, point[0], point[1], point[2], trailScale, trailAlpha);
        }
    }

    private void bubbles(VertexConsumer c, Vector3f l, Vector3f u, Vec3 cam, int light,
                         double x, double y, double z, float scale, float alpha) {
        Random urandom = new Random(this.seed + (long) (x * 100) + (long) (y * 10) + (long) z);
        int bubbleCount = explosionPhase == 0 ? 8 : (explosionPhase == 1 ? 6 : 4);
        for (int i = 0; i < bubbleCount; i++) {
            float whiteness = 0.9F + urandom.nextFloat() * 0.1F;
            float bubbleScale = scale * (urandom.nextFloat() * 0.5F + 0.75F);
            float offset = explosionPhase == 0 ? 0.4F : (explosionPhase == 1 ? 0.6F : 0.9F);
            float pX = (float) ((x - cam.x) + urandom.nextGaussian() * offset);
            float pY = (float) ((y - cam.y) + urandom.nextGaussian() * offset * 0.7F);
            float pZ = (float) ((z - cam.z) + urandom.nextGaussian() * offset);
            quad(c, pX, pY, pZ, l, u, bubbleScale, whiteness, whiteness, whiteness, alpha, light, 0, 0, 1, 1);
        }
    }
}
