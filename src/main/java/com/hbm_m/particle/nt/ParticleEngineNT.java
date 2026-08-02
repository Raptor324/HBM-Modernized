package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders NT particles after weather so mushroom cloud etc. are not clipped by depth.
 * Uses the level render pose stack so effects appear at correct world position (e.g. explosion center).
 */
public class ParticleEngineNT {

    public static final ParticleEngineNT INSTANCE = new ParticleEngineNT();

    // Батчи, отсортированные ОДИН раз при создании частиц.
    private final Map<net.minecraft.client.renderer.RenderType, List<ParticleNT>> particlesByType = new HashMap<>();
    private final List<net.minecraft.client.renderer.RenderType> renderOrderNormal = new ArrayList<>();
    private final List<net.minecraft.client.renderer.RenderType> renderOrderAshes = new ArrayList<>();

    // Очередь для новых частиц. Спасает от ConcurrentModificationException, если частица спавнит другую частицу внутри tick().
    private final List<ParticleNT> newParticles = new ArrayList<>();

    public void add(ParticleNT effect) {
        this.newParticles.add(effect);
    }

    public void clear() {
        this.particlesByType.clear();
        this.renderOrderNormal.clear();
        this.renderOrderAshes.clear();
        this.newParticles.clear();
    }

    public void render(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack) {
        renderBatches(this.renderOrderNormal, buffer, camera, partialTick, levelPoseStack);
        renderBatches(this.renderOrderAshes, buffer, camera, partialTick, levelPoseStack);
    }

    private void renderBatches(List<net.minecraft.client.renderer.RenderType> order, MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack) {
        for (int i = 0, size = order.size(); i < size; i++) {
            net.minecraft.client.renderer.RenderType type = order.get(i);
            List<ParticleNT> list = this.particlesByType.get(type);
            if (list == null || list.isEmpty()) continue;

            VertexConsumer consumer = buffer.getBuffer(type);
            for (int j = 0, lSize = list.size(); j < lSize; j++) {
                list.get(j).render(consumer, camera, partialTick, levelPoseStack);
            }
        }
    }

    public void renderFlashOnly(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack) {
        for (List<ParticleNT> list : this.particlesByType.values()) {
            if (list.isEmpty()) continue;
            for (int i = 0, size = list.size(); i < size; i++) {
                list.get(i).renderFlashOnly(buffer, camera, partialTick, levelPoseStack);
            }
        }
    }

    public void tick() {
        // 1. Распределяем новые частицы в правильные батчи (один раз за всю их жизнь)
        if (!this.newParticles.isEmpty()) {
            for (int i = 0, size = this.newParticles.size(); i < size; i++) {
                ParticleNT p = this.newParticles.get(i);
                if (p == null || p.dead) continue;
                net.minecraft.client.renderer.RenderType type = p.getRenderType();
                if (type == null) continue;

                List<ParticleNT> list = this.particlesByType.get(type);
                if (list == null) {
                    list = new ArrayList<>();
                    this.particlesByType.put(type, list);
                    
                    // Сохраняем RenderType сразу в нужный список для рендера, чтобы вообще избежать динамической сортировки
                    if (p instanceof ParticleAshesNT) {
                        this.renderOrderAshes.add(type);
                    } else {
                        this.renderOrderNormal.add(type);
                    }
                }
                list.add(p);
            }
            this.newParticles.clear();
        }

        // 2. Тикаем все батчи (in-place очистка мертвых частиц, как у вас и было, но теперь без сложных сдвигов массивов)
        for (List<ParticleNT> list : this.particlesByType.values()) {
            if (list.isEmpty()) continue;

            int alive = 0;
            int originalSize = list.size();

            for (int i = 0; i < originalSize; i++) {
                ParticleNT particle = list.get(i);
                if (particle == null) continue;

                particle.tick();

                if (!particle.dead) {
                    list.set(alive++, particle);
                }
            }

            if (originalSize > alive) {
                list.subList(alive, originalSize).clear();
            }
        }
    }
}
