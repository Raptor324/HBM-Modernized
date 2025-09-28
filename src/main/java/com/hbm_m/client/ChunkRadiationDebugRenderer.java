package com.hbm_m.client;

// Этот класс отвечает за отрисовку радиации в чанках в режиме отладки. 
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

import com.hbm_m.config.ModClothConfig;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ChunkRadiationDebugRenderer {

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        // Проверяем, что игрок и уровень существуют.
        if (player == null || level == null) return;
        
        // Объявляем переменную dimension и получаем ее из текущего уровня.
        final ResourceLocation dimension = level.dimension().location();

        if (!ModClothConfig.get().enableDebugRender || !mc.options.renderDebug) return;

        boolean isCreativeOrSpectator = player.isCreative() || player.isSpectator();
        if (!ModClothConfig.get().debugRenderInSurvival && !isCreativeOrSpectator) return;

        Vec3 camPos = event.getCamera().getPosition();
        // Используем значения из ClothConfig:
        int radius = ModClothConfig.get().debugRenderDistance; // Сколько чанков вокруг игрока показывать
        float scale = ModClothConfig.get().debugRenderTextSize; // Размер текста

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        int playerChunkX = player.chunkPosition().x;
        int playerChunkZ = player.chunkPosition().z;

        final boolean hasCeiling = level.dimensionType().hasCeiling();

        try {
            poseStack.pushPose(); // Сохраняем текущую матрицу
            RenderSystem.disableDepthTest(); // Отключаем тест глубины - текст будет поверх всего.

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;
                    net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);
                    
                    float value = ClientRadiationData.getRadiationForChunk(dimension, chunkPos);
                    if (value <= 1e-4f) continue; // Не рендерим нулевые значения для чистоты

                    double x = (chunkX << 4) + 8;
                    double z = (chunkZ << 4) + 8;
                    double y;
                    
                    if (hasCeiling) {
                        y = player.getY() + 1.0; 
                    } else {
                        y = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, BlockPos.containing(x, 0, z)).getY() + 1.5;
                    }

                    String text = String.format("Rad: %.2f", value);

                    poseStack.pushPose();
                    poseStack.translate(x - camPos.x, y - camPos.y, z - camPos.z);
                    poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                    poseStack.scale(-scale, -scale, scale);

                    font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
                    
                    poseStack.popPose();
                }
            }
            buffer.endBatch(); // Завершаем отрисовку
            
        } finally {
            // Возвращаем все как было, даже если произошла ошибка.
            RenderSystem.enableDepthTest(); // Включаем тест глубины обратно!
            poseStack.popPose();
        }
    }
}