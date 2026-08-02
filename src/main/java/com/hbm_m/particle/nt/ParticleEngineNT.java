package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
        if (particles.isEmpty()) return;

        // Группируем частицы по RenderType, чтобы минимизировать смену буфера.
        Map<net.minecraft.client.renderer.RenderType, List<ParticleNT>> batches =
                new HashMap<>();

        for (ParticleNT particle : particles) {
            if (particle == null || particle.dead) continue;
            net.minecraft.client.renderer.RenderType type = particle.getRenderType();
            if (type == null) continue;
            batches.computeIfAbsent(type, t -> new ArrayList<>()).add(particle);
        }

        // Детерминированный порядок: батчи (кости, облака) сначала; пепел — всегда последним.
        // Раньше HashMap давал случайный порядок: пепел, нарисованный раньше костей, писал
        // глубину и перекрывал их. Теперь пепел тестируется против глубины костей и корректно
        // скрывается за ними, а перед костями — блендится поверх.
        List<Map.Entry<net.minecraft.client.renderer.RenderType, List<ParticleNT>>> ordered =
                new ArrayList<>(batches.entrySet());
        ordered.sort(Comparator.comparingInt(e -> containsAshes(e.getValue()) ? 1 : 0));

        for (Map.Entry<net.minecraft.client.renderer.RenderType, List<ParticleNT>> entry : ordered) {
            net.minecraft.client.renderer.RenderType type = entry.getKey();
            VertexConsumer consumer = buffer.getBuffer(type);
            for (ParticleNT particle : entry.getValue()) {
                particle.render(consumer, camera, partialTick, levelPoseStack);
            }
        }
    }

    /** Батч с пеплом (ParticleAshesNT) рисуется последним: его depth-test видит глубину костей. */
    private static boolean containsAshes(List<ParticleNT> list) {
        return !list.isEmpty() && list.get(0) instanceof ParticleAshesNT;
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

        for (int i = list.size() - 1; i >= 0; i--) {
            ParticleNT particle = list.get(i);
            if (particle == null) {
                list.remove(i);
                continue;
            }
            particle.tick();
            if (particle.dead) {
                list.remove(i);
            }
        }
    }
}
