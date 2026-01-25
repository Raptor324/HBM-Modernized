package com.hbm_m.client.render;

import com.hbm_m.util.explosions.nuclear.CraterGenerator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RayVisualizationRenderer {

    // ОПТИМИЗАЦИЯ: Рисуем каждый 5-й луч
    private static final int RENDER_STEP = 5;

    // ПРОЗРАЧНОСТЬ: Вернули 0.25 (для debugLineStrip это оптимально)
    private static final float RAY_ALPHA_BASE = 0.25f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !mc.options.renderDebug || mc.level == null || mc.player == null) return;

        List<CraterGenerator.RayData> rays = CraterGenerator.getAllDebugRays();
        if (rays.isEmpty()) return;

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // ВАЖНОЕ ИЗМЕНЕНИЕ: Используем debugLineStrip(2.0)
        // Это делает линии толстыми (2 пикселя) и видимыми с ЛЮБОГО угла
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugLineStrip(2.0));

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        int counter = 0;

        for (CraterGenerator.RayData ray : rays) {
            // Прореживание
            if (counter++ % RENDER_STEP != 0) continue;

            float remaining = ray.getRemainingSeconds();
            if (remaining <= 0) continue;

            float fadeFactor = remaining > 5.0f ? 1.0f : remaining / 5.0f;
            float rayAlpha = RAY_ALPHA_BASE * fadeFactor;

            float x1 = (float) ray.startX;
            float y1 = (float) ray.startY;
            float z1 = (float) ray.startZ;

            float x2, y2, z2;

            // Логика длины луча
            if (!ray.hitBlocks.isEmpty()) {
                BlockPos lastHit = ray.hitBlocks.get(ray.hitBlocks.size() - 1);
                x2 = lastHit.getX() + 0.5f;
                y2 = lastHit.getY() + 0.5f;
                z2 = lastHit.getZ() + 0.5f;
            } else {
                x2 = (float) (ray.startX + ray.dirX * 100.0);
                y2 = (float) (ray.startY + ray.dirY * 100.0);
                z2 = (float) (ray.startZ + ray.dirZ * 100.0);
            }

            // Рисуем линию
            consumer.vertex(matrix, x1, y1, z1).color(0.0f, 1.0f, 1.0f, rayAlpha).normal(0, 1, 0).endVertex();
            consumer.vertex(matrix, x2, y2, z2).color(0.0f, 1.0f, 1.0f, rayAlpha).normal(0, 1, 0).endVertex();
        }

        poseStack.popPose();

        // Завершаем батч именно для debugLineStrip
        bufferSource.endBatch(RenderType.debugLineStrip(2.0));
    }
}
