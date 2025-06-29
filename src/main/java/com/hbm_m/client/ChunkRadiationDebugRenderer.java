package com.hbm_m.client;

// Этот класс отвечает за отрисовку радиации в чанках в режиме отладки
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.hbm_m.config.ModClothConfig;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ChunkRadiationDebugRenderer {

    // Карта для хранения радиации чанков на клиенте
    private static final java.util.Map<net.minecraft.world.level.ChunkPos, Float> chunkRadiationData = new java.util.concurrent.ConcurrentHashMap<>();

    // Метод для обновления данных о радиации чанка
    public static void updateChunkRadiation(int chunkX, int chunkZ, float radiationValue) {
        net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);
        if (radiationValue <= 1e-4f) {
            chunkRadiationData.remove(pos);
        } else {
            chunkRadiationData.put(pos, radiationValue);
        }
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        if (!ModClothConfig.get().enableDebugRender || !mc.options.renderDebug) return;
        if (mc.player != null) {
            boolean isCreativeOrSpectator = mc.player.isCreative() || mc.player.isSpectator();
            if (!ModClothConfig.get().debugRenderInSurvival && !isCreativeOrSpectator) return;
        }

        Level level = mc.level;
        if (level == null) return;

        Vec3 camPos = event.getCamera().getPosition();
        // Используем значения из ClothConfig:
        int radius = ModClothConfig.get().debugRenderDistance; // Сколько чанков вокруг игрока показывать

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        float scale = ModClothConfig.get().debugRenderTextSize;

        int playerChunkX = ((int)camPos.x) >> 4;
        int playerChunkZ = ((int)camPos.z) >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);
                
                // Получаем значение радиации из локальной карты
                float value = chunkRadiationData.getOrDefault(chunkPos, 0.0f);

                // Центр чанка
                double x = (chunkX << 4) + 8;
                double z = (chunkZ << 4) + 8;
                double y = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, new BlockPos((int)x, 0, (int)z)).getY() + 1.5;

                String text = String.format("Rad: %.2f", value);

                poseStack.pushPose();
                poseStack.translate(x - camPos.x, y - camPos.y, z - camPos.z);
                poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                poseStack.scale(-scale, -scale, scale);

                font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
                poseStack.popPose();
            }
        }
    }
}
