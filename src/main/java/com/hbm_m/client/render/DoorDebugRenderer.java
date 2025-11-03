package com.hbm_m.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.block.entity.DoorBlockEntity;
import com.hbm_m.main.MainRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class DoorDebugRenderer {
    private static final Map<BlockPos, CachedCollisionInfo> collisionCache = new HashMap<>();
    private static boolean enabled = false;
    private static long lastToggleTime = 0;
    private static final long DEBOUNCE_MS = 200;
    
    private static final int COLOR_COLLISION_CLOSED = 0xFF0000FF;
    private static final int COLOR_COLLISION_OPEN = 0xFFFF0000;
    private static final int COLOR_COLLISION_PARTIAL = 0xFFFFFF00;

    public static class CachedCollisionInfo {
        public List<AABB> boxes;
        public float lastProgress;
        public Direction facing;

        public CachedCollisionInfo(List<AABB> boxes, float progress, Direction facing) {
            this.boxes = boxes;
            this.lastProgress = progress;
            this.facing = facing;
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleKeyInput(event.getKey());
        }
    }

    public static void handleKeyInput(int key) {
        long currentTime = System.currentTimeMillis();
        
        // F3 + B комбинация
        if (key == GLFW.GLFW_KEY_B && isKeyDown(GLFW.GLFW_KEY_F3)) {
            if (currentTime - lastToggleTime > DEBOUNCE_MS) {
                enabled = !enabled;
                lastToggleTime = currentTime;
                String status = enabled ? "Отладочный рендер дверей: ВКЛ" : "Отладочный рендер дверей: ВЫКЛ";
                Minecraft.getInstance().gui.setOverlayMessage(
                    net.minecraft.network.chat.Component.literal(status), false);
                if (!enabled) {
                    collisionCache.clear();
                }
            }
        }
    }

    private static boolean isKeyDown(int key) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                             double cameraX, double cameraY, double cameraZ) {
        if (!enabled) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.isClientSide) return;

        try {
            ClientLevel level = (ClientLevel) mc.level;
            ClientChunkCache chunkSource = level.getChunkSource();

            // Получаем загруженные чанки в радиусе рендера
            int renderDistance = Math.min(mc.options.renderDistance().get(), 8); // Ограничиваем для производительности
            
            for (int x = -renderDistance; x <= renderDistance; x++) {
                for (int z = -renderDistance; z <= renderDistance; z++) {
                    ChunkPos chunkPos = new ChunkPos(
                        (int) Math.floor(cameraX / 16.0) + x,
                        (int) Math.floor(cameraZ / 16.0) + z
                    );
                    
                    LevelChunk chunk = chunkSource.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
                    if (chunk == null) continue;

                    // Итерируем через BlockEntity в чанке
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (!(be instanceof DoorBlockEntity doorBe)) continue;
                        if (!doorBe.isController()) continue;

                        renderDoor(doorBe, poseStack, bufferSource, cameraX, cameraY, cameraZ);
                    }
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Ошибка при отладочном рендере дверей", e);
        }
    }

    private static void renderDoor(DoorBlockEntity doorBe, PoseStack poseStack, MultiBufferSource bufferSource,
                                  double cameraX, double cameraY, double cameraZ) {
        Direction facing = doorBe.getFacing();
        float progress = doorBe.getOpenProgress(0);
        List<AABB> collisionBoxes = getCollisionBoxes(doorBe, progress, facing);

        if (collisionBoxes == null || collisionBoxes.isEmpty()) return;

        BlockPos pos = doorBe.getBlockPos();
        int color = getColorForProgress(progress);

        // ИСПРАВЛЕНО: Используем стандартные RenderType
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
        VertexConsumer quadConsumer = bufferSource.getBuffer(RenderType.debugQuads());

        poseStack.pushPose();
        poseStack.translate(
            pos.getX() - cameraX,
            pos.getY() - cameraY,
            pos.getZ() - cameraZ
        );

        for (AABB aabb : collisionBoxes) {
            renderBoxOutline(lineConsumer, poseStack, aabb, color);
            renderBoxFaces(quadConsumer, poseStack, aabb, color);
        }

        poseStack.popPose();
    }

    private static List<AABB> getCollisionBoxes(DoorBlockEntity doorBe, float progress, Direction facing) {
        BlockPos pos = doorBe.getBlockPos();
        
        if (collisionCache.containsKey(pos)) {
            CachedCollisionInfo cached = collisionCache.get(pos);
            if (Math.abs(cached.lastProgress - progress) < 0.01f && cached.facing == facing) {
                return cached.boxes;
            }
        }

        List<AABB> boxes = doorBe.getDynamicCollisionShape(facing).toAabbs();
        collisionCache.put(pos, new CachedCollisionInfo(boxes, progress, facing));
        return boxes;
    }

    private static int getColorForProgress(float progress) {
        if (progress >= 0.99f) {
            return COLOR_COLLISION_CLOSED;
        } else if (progress <= 0.01f) {
            return COLOR_COLLISION_OPEN;
        } else {
            return COLOR_COLLISION_PARTIAL;
        }
    }

    // ИСПРАВЛЕНО: Используем PoseStack для трансформаций
    private static void renderBoxOutline(VertexConsumer consumer, PoseStack poseStack, AABB box, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // 12 линий для wireframe куба
        addLine(consumer, poseStack, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        addLine(consumer, poseStack, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        addLine(consumer, poseStack, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        addLine(consumer, poseStack, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);
        
        addLine(consumer, poseStack, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        addLine(consumer, poseStack, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        addLine(consumer, poseStack, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        addLine(consumer, poseStack, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        
        addLine(consumer, poseStack, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        addLine(consumer, poseStack, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        addLine(consumer, poseStack, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        addLine(consumer, poseStack, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
    }

    private static void renderBoxFaces(VertexConsumer consumer, PoseStack poseStack, AABB box, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.3f; // Полупрозрачность

        // 6 граней куба
        renderQuad(consumer, poseStack,
            box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
            box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ,
            r, g, b, a);
        renderQuad(consumer, poseStack,
            box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ,
            box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ,
            r, g, b, a);
        renderQuad(consumer, poseStack,
            box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
            box.minX, box.maxY, box.maxZ, box.minX, box.minY, box.maxZ,
            r, g, b, a);
        renderQuad(consumer, poseStack,
            box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ,
            box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ,
            r, g, b, a);
        renderQuad(consumer, poseStack,
            box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ,
            box.maxX, box.minY, box.maxZ, box.maxX, box.minY, box.minZ,
            r, g, b, a);
        renderQuad(consumer, poseStack,
            box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ,
            box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
            r, g, b, a);
    }

    // ИСПРАВЛЕНО: Используем стандартный подход с PoseStack
    private static void addLine(VertexConsumer consumer, PoseStack poseStack, 
                               double x1, double y1, double z1,
                               double x2, double y2, double z2, 
                               float r, float g, float b, float a) {
        consumer.vertex(poseStack.last().pose(), (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a)
                .endVertex();
    }

    // ИСПРАВЛЕНО: Используем стандартный подход с PoseStack
    private static void renderQuad(VertexConsumer consumer, PoseStack poseStack,
                                  double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  double x3, double y3, double z3,
                                  double x4, double y4, double z4,
                                  float r, float g, float b, float a) {
        consumer.vertex(poseStack.last().pose(), (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), (float)x3, (float)y3, (float)z3)
                .color(r, g, b, a)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), (float)x4, (float)y4, (float)z4)
                .color(r, g, b, a)
                .endVertex();
    }

    public static void clearCache() {
        collisionCache.clear();
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
