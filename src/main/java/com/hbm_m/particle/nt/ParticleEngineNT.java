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

    /**
     * Изолированный BufferSource движка: NT-частицы не должны флашиться
     * через общий bufferSource (внутри DH-прохода чужие батчи улетели бы
     * в DH FBO, а с ним — в композит).
     */
    private static MultiBufferSource.BufferSource ownBuffer;

    public static synchronized MultiBufferSource.BufferSource buffer() {
        if (ownBuffer == null) {
            // ВАЖНО: НЕ MultiBufferSource.immediate()! Под ImmediatelyFast фабрика
            // редиректится и подменяет источник на BatchableBufferSource, который
            // крашится «Sorting state uninitialized» на пустых sortOnUpload-батчах
            // (наши nuke_clouds/nuke_flash при пустом фильтре). PlainBufferSource —
            // честный ванильный BufferSource, созданный мимо фабрики.
            //? if < 1.21.1 {
            ownBuffer = new com.hbm_m.client.render.PlainBufferSource(
                    new com.mojang.blaze3d.vertex.BufferBuilder(256));
            //?} else {
            /*ownBuffer = new com.hbm_m.client.render.PlainBufferSource(
                    new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
            *///?}
        }
        return ownBuffer;
    }

    /**
     * Отрисовка ДАЛЬНЕГО контента (вызывается из DH-моста: DH FBO забинден,
     * флаг FAR_PASS_ACTIVE переключает рендертайпы на DH-варианты).
     * Рисуются только FarCapableParticle ЗА пределами ванильной прорисовки
     * (maxSq): ближе рисует ванильный путь с нативным depth-тестом.
     */
    public void renderFarContent(Camera camera, float partialTick,
            net.minecraft.world.phys.Vec3 camPos, double maxSq) {
        com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
        int farLists = 0, farParticles = 0;
        for (List<net.minecraft.client.renderer.RenderType> order : java.util.List.of(this.renderOrderNormal, this.renderOrderAshes)) {
            for (int i = 0, size = order.size(); i < size; i++) {
                List<ParticleNT> list = this.particlesByType.get(order.get(i));
                if (list == null || list.isEmpty() || !(list.get(0) instanceof FarCapableParticle)) continue;
                VertexConsumer consumer = buffer().getBuffer(order.get(i)); // игнорируется классами — сами берут nukeClouds()
                boolean drewAny = false;
                for (int j = 0, lSize = list.size(); j < lSize; j++) {
                    ParticleNT p = list.get(j);
                    if (!(p instanceof FarCapableParticle)) continue;
                    double dx = p.x - camPos.x, dy = p.y - camPos.y, dz = p.z - camPos.z;
                    if (dx * dx + dy * dy + dz * dz <= maxSq) continue;
                    p.render(consumer, camera, partialTick, pose);
                    drewAny = true;
                    farParticles++;
                    if (this.lastFarParticlePos == null) {
                        this.lastFarParticlePos = new net.minecraft.world.phys.Vec3(p.x, p.y, p.z);
                    }
                }
                if (drewAny) farLists++;
            }
        }
        this.lastFarParticles = farParticles;
        if (farParticles == 0) this.lastFarParticlePos = null;
    }

    private int lastFarParticles = 0;
    /** Мировая позиция первого дальнего партикла текущего кадра (для readback-диагностики). */
    public net.minecraft.world.phys.Vec3 lastFarParticlePos;

    public int lastFarParticleCount() {
        return lastFarParticles;
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

    public void renderFiltered(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack, java.util.function.Predicate<ParticleNT> filter) {
        renderBatchesFiltered(this.renderOrderNormal, buffer, camera, partialTick, levelPoseStack, filter);
        renderBatchesFiltered(this.renderOrderAshes, buffer, camera, partialTick, levelPoseStack, filter);
    }

    private void renderBatches(List<net.minecraft.client.renderer.RenderType> order, MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack) {
        // Межбатчевый порядок тоже динамический: батчи (типы = разные
        // текстуры/частицы гриба — кольца, ножка, шляпка) рисуются от дальних
        // к ближним относительно камеры, а не в порядке регистрации.
        List<net.minecraft.client.renderer.RenderType> ordered = orderDistanceSorted(order, camera);
        for (int i = 0, size = ordered.size(); i < size; i++) {
            net.minecraft.client.renderer.RenderType type = ordered.get(i);
            List<ParticleNT> list = this.particlesByType.get(type);
            if (list == null || list.isEmpty()) continue;

            // Порядок отрисовки ВНУТРИ батча: от дальних к ближним (painter).
            // sortOnUpload у наших RenderType выключен (нестабилен в модпаке —
            // «Sorting state uninitialized»), поэтому порядок квайдов задаём
            // сами — сортировкой частиц. Данные почти отсортированы между
            // кадрами (частицы движутся медленно), TimSort почти O(n).
            sortFarToNear(list, camera);

            VertexConsumer consumer = buffer().getBuffer(type);
            for (int j = 0, lSize = list.size(); j < lSize; j++) {
                list.get(j).render(consumer, camera, partialTick, levelPoseStack);
            }
        }
    }

    private void renderBatchesFiltered(List<net.minecraft.client.renderer.RenderType> order, MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack, java.util.function.Predicate<ParticleNT> filter) {
        List<net.minecraft.client.renderer.RenderType> ordered = orderDistanceSorted(order, camera);
        for (int i = 0, size = ordered.size(); i < size; i++) {
            net.minecraft.client.renderer.RenderType type = ordered.get(i);
            List<ParticleNT> list = this.particlesByType.get(type);
            if (list == null || list.isEmpty()) continue;

            sortFarToNear(list, camera);

            VertexConsumer consumer = null;
            for (int j = 0, lSize = list.size(); j < lSize; j++) {
                ParticleNT p = list.get(j);
                if (!filter.test(p)) continue;
                if (consumer == null) consumer = buffer().getBuffer(type);
                p.render(consumer, camera, partialTick, levelPoseStack);
            }
        }
    }

    /**
     * Копия списка типов, отсортированная по дистанции центроида частиц
     * батча от камеры (дальние раньше). Типы без живых частиц — в конец.
     */
    private List<net.minecraft.client.renderer.RenderType> orderDistanceSorted(List<net.minecraft.client.renderer.RenderType> order, Camera camera) {
        double cx = camera.getPosition().x;
        double cy = camera.getPosition().y;
        double cz = camera.getPosition().z;
        List<net.minecraft.client.renderer.RenderType> sorted = new ArrayList<>(order);
        sorted.sort((ta, tb) -> {
            double da = batchDistance(ta, cx, cy, cz);
            double db = batchDistance(tb, cx, cy, cz);
            return Double.compare(db, da);
        });
        return sorted;
    }

    /** Дистанция центроида частиц батча от камеры; MAX для пустых батчей. */
    private double batchDistance(net.minecraft.client.renderer.RenderType type, double cx, double cy, double cz) {
        List<ParticleNT> list = this.particlesByType.get(type);
        if (list == null || list.isEmpty()) {
            return Double.MAX_VALUE;
        }
        double sx = 0, sy = 0, sz = 0;
        for (int i = 0, size = list.size(); i < size; i++) {
            ParticleNT p = list.get(i);
            sx += p.x;
            sy += p.y;
            sz += p.z;
        }
        double n = list.size();
        double dx = sx / n - cx;
        double dy = sy / n - cy;
        double dz = sz / n - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Сортировка частиц батча от дальних к ближним относительно камеры. */
    private static void sortFarToNear(List<ParticleNT> list, Camera camera) {
        double cx = camera.getPosition().x;
        double cy = camera.getPosition().y;
        double cz = camera.getPosition().z;
        list.sort((a, b) -> {
            double da = (a.x - cx) * (a.x - cx) + (a.y - cy) * (a.y - cy) + (a.z - cz) * (a.z - cz);
            double db = (b.x - cx) * (b.x - cx) + (b.y - cy) * (b.y - cy) + (b.z - cz) * (b.z - cz);
            return Double.compare(db, da);
        });
    }

    public void renderFlashOnly(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack) {
        for (List<ParticleNT> list : this.particlesByType.values()) {
            if (list.isEmpty()) continue;
            for (int i = 0, size = list.size(); i < size; i++) {
                list.get(i).renderFlashOnly(buffer, camera, partialTick, levelPoseStack);
            }
        }
    }

    public void renderFlashOnlyFiltered(MultiBufferSource.BufferSource buffer, Camera camera, float partialTick, PoseStack levelPoseStack, java.util.function.Predicate<ParticleNT> filter) {
        for (List<ParticleNT> list : this.particlesByType.values()) {
            if (list.isEmpty()) continue;
            for (int i = 0, size = list.size(); i < size; i++) {
                ParticleNT p = list.get(i);
                if (!filter.test(p)) continue;
                p.renderFlashOnly(buffer, camera, partialTick, levelPoseStack);
            }
        }
    }

    public java.util.Map<net.minecraft.client.renderer.RenderType, List<ParticleNT>> debugBatches() {
        return this.particlesByType;
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
