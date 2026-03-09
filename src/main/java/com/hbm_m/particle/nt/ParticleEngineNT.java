package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders NT particles after weather so mushroom cloud etc. are not clipped by depth.
 * Uses the level render pose stack so effects appear at correct world position (e.g. explosion center).
 */
public class ParticleEngineNT {

    public static final ParticleEngineNT INSTANCE = new ParticleEngineNT();

    private final List<ParticleNT> particles = new ArrayList<>();

    public void add(ParticleNT effect) {
        this.particles.add(effect);
    }

    public void clear() {
        this.particles.clear();
    }

    public void render(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack) {
        for (ParticleNT particle : particles) {
            VertexConsumer consumer = buffer.getBuffer(particle.getRenderType());
            particle.render(consumer, camera, partialTick, levelPoseStack);
        }
    }

    /** Рендер только flash-частиц (NukeTorex) поверх всех остальных частиц. */
    public void renderFlashOnly(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack) {
        for (ParticleNT particle : particles) {
            particle.renderFlashOnly(buffer, camera, partialTick, levelPoseStack);
        }
    }

    public void tick() {
        List<ParticleNT> list = this.particles;
        if (list.isEmpty()) return;
        for (ParticleNT particle : new ArrayList<>(list)) {
            if (particle == null) continue;
            particle.tick();
            if (particle.dead) {
                list.remove(particle);
            }
        }
    }
}
