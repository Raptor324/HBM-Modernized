package com.hbm_m.client;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.hbm_m.block.ModBlocks;
// Этот класс отвечает за подсветку блоков, если те мешают установке многоблочной структуры
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.IMultiblockPart;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientRenderHandler {

    private static final Map<BlockPos, Long> highlightedBlocks = new HashMap<>();
    // Постоянная подсветка для осиротевших фантомных блоков (потерявших связь с контроллером)
    private static final Map<BlockPos, Boolean> orphanedPhantomBlocks = new HashMap<>();
    
    // Счетчик тиков для периодической проверки осиротевших блоков (проверяем раз в 60 тиков = 1 раз в 3 секунды)
    private static int tickCounter = 0;
    
    // Кэш известных фантомных блоков для оптимизации (чтобы не проверять их каждый раз)
    private static final Map<BlockPos, Long> knownPhantomBlocks = new HashMap<>();

    private static class CustomRenderTypes extends RenderType {
        private CustomRenderTypes(String s, VertexFormat v, VertexFormat.Mode m, int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }

        public static final RenderType HIGHLIGHT_BOX_FILL = create("highlight_box_fill",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }

    public static void highlightBlocks(List<BlockPos> positions) {
        long duration = ModClothConfig.get().obstructionHighlight.obstructionHighlightDuration * 1000L;
        long expiryTime = System.currentTimeMillis() + duration;
        highlightedBlocks.clear(); // Очищаем старые, чтобы не было дубликатов
        for (BlockPos pos : positions) {
            highlightedBlocks.put(pos, expiryTime);
        }
    }

    /**
     * Добавляет блок в список осиротевших (потерявших связь с контроллером) для постоянной подсветки фиолетовым.
     */
    public static void addOrphanedPhantomBlock(BlockPos pos) {
        orphanedPhantomBlocks.put(pos, true);
    }

    /**
     * Удаляет блок из списка осиротевших.
     */
    public static void removeOrphanedPhantomBlock(BlockPos pos) {
        orphanedPhantomBlocks.remove(pos);
    }

    /**
     * Проверяет, осиротел ли фантомный блок (потерял связь с контроллером).
     */
    private static boolean isOrphaned(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IMultiblockPart part)) {
            return false;
        }

        BlockPos controllerPos = part.getControllerPos();
        if (controllerPos == null) {
            return true; // Нет контроллера = осиротел
        }

        // Проверяем, существует ли контроллер и является ли он валидным
        if (!level.isLoaded(controllerPos)) {
            return false; // Чанк не загружен, не можем проверить
        }

        var controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController)) {
            return true; // Контроллер не существует или не является контроллером = осиротел
        }

        return false; // Контроллер валиден
    }

    /**
     * Автоматически сканирует область вокруг игрока и находит осиротевшие фантомные блоки.
     * Вызывается из клиентского тика.
     * Оптимизировано: проверяет только загруженные чанки в небольшом радиусе.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // Проверяем раз в 3 секунды (60 тиков) для оптимизации производительности
        tickCounter++;
        if (tickCounter < 60) {
            return;
        }
        tickCounter = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        var level = mc.level;
        BlockPos playerPos = mc.player.blockPosition();
        
        // Оптимизированный радиус сканирования: 16 блоков во все стороны (32x32x32 область)
        // Это покрывает область видимости игрока без излишней нагрузки
        int scanRadius = 16;
        
        // Очищаем старые записи из кэша (старше 5 секунд)
        long currentTime = System.currentTimeMillis();
        knownPhantomBlocks.entrySet().removeIf(entry -> currentTime - entry.getValue() > 5000);
        
        // Сканируем только загруженные чанки вокруг игрока
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;
        int chunkRadius = (scanRadius + 15) >> 4; // Радиус в чанках
        
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int currentChunkX = chunkX + dx;
                int currentChunkZ = chunkZ + dz;
                
                // Проверяем, загружен ли чанк
                var chunk = level.getChunkSource().getChunk(currentChunkX, currentChunkZ, false);
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }
                
                // Сканируем только блоки в этом чанке в пределах радиуса
                int minX = Math.max(playerPos.getX() - scanRadius, currentChunkX << 4);
                int maxX = Math.min(playerPos.getX() + scanRadius, (currentChunkX << 4) + 15);
                int minZ = Math.max(playerPos.getZ() - scanRadius, currentChunkZ << 4);
                int maxZ = Math.min(playerPos.getZ() + scanRadius, (currentChunkZ << 4) + 15);
                int minY = Math.max(playerPos.getY() - scanRadius, level.getMinBuildHeight());
                int maxY = Math.min(playerPos.getY() + scanRadius, level.getMaxBuildHeight());
                
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos checkPos = new BlockPos(x, y, z);
                            
                            // Быстрая проверка: является ли блок фантомной частью
                            if (!level.getBlockState(checkPos).is(ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
                                continue;
                            }
                            
                            // Добавляем в кэш известных фантомных блоков
                            knownPhantomBlocks.put(checkPos, currentTime);
                            
                            // Проверяем, осиротел ли блок
                            if (isOrphaned(level, checkPos)) {
                                // Блок осиротел - добавляем в подсветку
                                orphanedPhantomBlocks.put(checkPos, true);
                            } else {
                                // Блок валиден - удаляем из подсветки (если был там)
                                orphanedPhantomBlocks.remove(checkPos);
                            }
                        }
                    }
                }
            }
        }
        
        // Также проверяем уже известные фантомные блоки (на случай, если они изменили статус)
        knownPhantomBlocks.keySet().removeIf(pos -> {
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
                orphanedPhantomBlocks.remove(pos);
                return true; // Удаляем из кэша
            }
            
            // Проверяем статус известного блока
            if (isOrphaned(level, pos)) {
                orphanedPhantomBlocks.put(pos, true);
            } else {
                orphanedPhantomBlocks.remove(pos);
            }
            return false; // Оставляем в кэше
        });
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        long currentTime = System.currentTimeMillis();
        VertexConsumer fillConsumer = mc.renderBuffers().bufferSource().getBuffer(CustomRenderTypes.HIGHLIGHT_BOX_FILL);
        float alpha = ModClothConfig.get().obstructionHighlight.obstructionHighlightAlpha / 100.0f;

        poseStack.pushPose();
        // Переводим в camera-relative координаты один раз
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // 1. Рендерим временные красные подсветки (препятствия при установке)
        if (!highlightedBlocks.isEmpty() && alpha > 0) {
            Color redColor = Color.RED;
            highlightedBlocks.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                if (currentTime > entry.getValue()) {
                    return true;
                }

                // Проверяем соседей
                boolean drawDown = !highlightedBlocks.containsKey(pos.below());
                boolean drawUp = !highlightedBlocks.containsKey(pos.above());
                boolean drawNorth = !highlightedBlocks.containsKey(pos.north());
                boolean drawSouth = !highlightedBlocks.containsKey(pos.south());
                boolean drawWest = !highlightedBlocks.containsKey(pos.west());
                boolean drawEast = !highlightedBlocks.containsKey(pos.east());

                // Раздуваем в camera-relative пространстве
                AABB boundingBox = new AABB(pos).inflate(0.002D);
                
                renderFilledBox(poseStack, fillConsumer, boundingBox, redColor, alpha, 
                            drawDown, drawUp, drawNorth, drawSouth, drawWest, drawEast);
                return false;
            });
        }

        // 2. Рендерим постоянные фиолетовые подсветки (осиротевшие фантомные блоки)
        if (!orphanedPhantomBlocks.isEmpty()) {
            Color purpleColor = new Color(128, 0, 128); // Фиолетовый цвет
            float purpleAlpha = 0.6f; // Полупрозрачность для постоянной подсветки
            
            // Проверяем, что блоки всё ещё существуют в мире
            var level = mc.level;
            if (level != null) {
                orphanedPhantomBlocks.entrySet().removeIf(entry -> {
                    BlockPos pos = entry.getKey();
                    // Если блока больше нет в мире - удаляем из списка
                    if (!level.getBlockState(pos).is(com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
                        return true;
                    }

                    // Проверяем соседей (только среди осиротевших блоков)
                    boolean drawDown = !orphanedPhantomBlocks.containsKey(pos.below());
                    boolean drawUp = !orphanedPhantomBlocks.containsKey(pos.above());
                    boolean drawNorth = !orphanedPhantomBlocks.containsKey(pos.north());
                    boolean drawSouth = !orphanedPhantomBlocks.containsKey(pos.south());
                    boolean drawWest = !orphanedPhantomBlocks.containsKey(pos.west());
                    boolean drawEast = !orphanedPhantomBlocks.containsKey(pos.east());

                    // Раздуваем в camera-relative пространстве
                    AABB boundingBox = new AABB(pos).inflate(0.002D);
                    
                    renderFilledBox(poseStack, fillConsumer, boundingBox, purpleColor, purpleAlpha, 
                                drawDown, drawUp, drawNorth, drawSouth, drawWest, drawEast);
                    return false;
                });
            }
        }

        poseStack.popPose();
        mc.renderBuffers().bufferSource().endBatch(CustomRenderTypes.HIGHLIGHT_BOX_FILL);
    }

    // Рендерим только те грани куба, которые не примыкают к другим подсвеченным блокам.
    private static void renderFilledBox(PoseStack poseStack, VertexConsumer consumer, AABB box, Color color, float alpha,
                                        boolean drawDown, boolean drawUp, boolean drawNorth, boolean drawSouth, boolean drawWest, boolean drawEast) {
        Matrix4f matrix = poseStack.last().pose();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        
        float minX = (float)box.minX; float minY = (float)box.minY; float minZ = (float)box.minZ;
        float maxX = (float)box.maxX; float maxY = (float)box.maxY; float maxZ = (float)box.maxZ;
        
        // Низ (Y-)
        if (drawDown) {
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
        }
        // Верх (Y+)
        if (drawUp) {
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
        }
        // Север (Z-)
        if (drawNorth) {
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
        }
        // Юг (Z+)
        if (drawSouth) {
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
        }
        // Запад (X-)
        if (drawWest) {
            consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, alpha).endVertex();
        }
        // Восток (X+)
        if (drawEast) {
            consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
        }
    }
}