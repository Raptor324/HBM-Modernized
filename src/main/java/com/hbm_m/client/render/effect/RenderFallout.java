package com.hbm_m.client.render.effect;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.entity.effect.EntityFalloutRain;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public class RenderFallout extends EntityRenderer<EntityFalloutRain> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation FALLOUT = new ResourceLocation(RefStrings.MODID, "textures/entity/fallout.png");
    *///?} else {
        private static final ResourceLocation FALLOUT = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/fallout.png");
    //?}


    /** Максимальный радиус рендера fallout-колонн (в блоках). Уменьшен для оптимизации. */
    private static final int MAX_RENDER_RADIUS = 32;
    /**
     * Высота «шторы» осадков над поверхностью (блоки). Уменьшена для оптимизации.
     */
    private static final int MAX_VISUAL_HEIGHT_ABOVE_SURFACE = 24;
    /**
     * Горизонтальная дистанция камеры до сущности (эпицентра осадков). Согласовано с {@code clientTrackingRange(6)} чанков (~96 бл.).
     */
    private static final double MAX_EPICENTER_CAMERA_HORIZONTAL_DIST_SQ = 88.0 * 88.0;
    /**
     * Колонны только рядом с игроком (как у ванильного дождя вокруг камеры). Уменьшено для оптимизации.
     */
    private static final double MAX_COLUMN_CAMERA_HORIZONTAL_DIST_SQ = 48.0 * 48.0;
    /** Радиус кэш-сетки для колонн (всегда >= MAX_RENDER_RADIUS). */
    private static final int CACHE_RADIUS = MAX_RENDER_RADIUS;
    /** Ширина кэш-сетки: (2 * CACHE_RADIUS + 1)^2 ячеек. */
    private static final int CACHE_WIDTH = CACHE_RADIUS * 2 + 1;

    /** Кэш данных по колонне fallout. Привязан к центру и игровому тику. */
    private static final class FalloutColumnCacheEntry {
        int height;
        int lastUpdateTick;
        int centerX;
        int centerZ;
    }

    private FalloutColumnCacheEntry[] columnCache;

    private float[] rainXCoords;
    private float[] rainYCoords;

    public RenderFallout(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityFalloutRain entity, float entityYaw, float partialTick,
                    PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity camera = mc.player;
        ClientLevel world = mc.level;
        if (camera == null || world == null) return;

        // Сглаживаем внутреннюю анимацию по времени (swayLoop и т.п.)
        float interp = partialTick;

        double ex = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        double hdx = camera.getX() - ex;
        double hdz = camera.getZ() - ez;
        if (hdx * hdx + hdz * hdz > MAX_EPICENTER_CAMERA_HORIZONTAL_DIST_SQ) {
            return;
        }

        poseStack.pushPose();
        // Отменяем entity offset, оставляем только camera-relative матрицу уровня.
        poseStack.translate(-ex, -ey, -ez);
        renderRainSnow(entity, interp, poseStack, bufferSource, world, camera);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFalloutRain entity) {
        return FALLOUT;
    }

    @Override
    public boolean shouldRender(EntityFalloutRain entity, Frustum frustum, double camX, double camY, double camZ) {
        double dx = camX - entity.getX();
        double dz = camZ - entity.getZ();
        return dx * dx + dz * dz <= MAX_EPICENTER_CAMERA_HORIZONTAL_DIST_SQ;
    }

    private void renderRainSnow(EntityFalloutRain entity, float interp, PoseStack pose, MultiBufferSource buffers, ClientLevel level, LivingEntity camera) {
        int timer = camera.tickCount;
        float intensity = 1.0F;

        int centerX = Mth.floor(entity.getX());
        int centerZ = Mth.floor(entity.getZ());
        int scale = entity.getScale();
        if (scale < 1) return;
        if (rainXCoords == null) {
            rainXCoords = new float[1024];
            rainYCoords = new float[1024];
            for (int i = 0; i < 32; ++i) {
                for (int j = 0; j < 32; ++j) {
                    float f2 = j - 16;
                    float f3 = i - 16;
                    float f4 = Mth.sqrt(f2 * f2 + f3 * f3);
                    if (f4 < 1.0E-5F) {
                        rainXCoords[i << 5 | j] = 0;
                        rainYCoords[i << 5 | j] = 1;
                    } else {
                        rainXCoords[i << 5 | j] = -f3 / f4;
                        rainYCoords[i << 5 | j] = f2 / f4;
                    }
                }
            }
        }

        GraphicsStatus graphics = Minecraft.getInstance().options.graphicsMode().get();
        boolean fancy = graphics == GraphicsStatus.FANCY || graphics == GraphicsStatus.FABULOUS;
        int verticalLayers = fancy ? 10 : 5;

        // quality / LOD: на fast режем радиус и делаем шаг выборки больше
        int baseRadius = Math.min(scale, MAX_RENDER_RADIUS);
        int step = fancy ? 1 : 2;
        int renderRadius = Math.max(4, baseRadius);

        if (columnCache == null) {
            columnCache = new FalloutColumnCacheEntry[CACHE_WIDTH * CACHE_WIDTH];
        }

        ResourceLocation texture = FALLOUT;
        VertexConsumer consumer = buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ENTITY_SMOOTH.apply(texture));
        var matrix = pose.last().pose();

        int currentTick = camera.tickCount;

        for (int relZ = -renderRadius; relZ <= renderRadius; relZ += step) {
            int layerZ = centerZ + relZ;
            for (int relX = -renderRadius; relX <= renderRadius; relX += step) {
                int layerX = centerX + relX;

                double distSq = (layerX + 0.5 - entity.getX()) * (layerX + 0.5 - entity.getX()) + (layerZ + 0.5 - entity.getZ()) * (layerZ + 0.5 - entity.getZ());
                if (distSq > (double) (scale * scale)) continue;

                int idxRain = ((relZ + 16) & 31) * 32 + ((relX + 16) & 31);
                float rainCoordX = rainXCoords[idxRain] * 0.5F;
                float rainCoordY = rainYCoords[idxRain] * 0.5F;

                int cacheIndex = (relZ + CACHE_RADIUS) * CACHE_WIDTH + (relX + CACHE_RADIUS);
                if (cacheIndex < 0 || cacheIndex >= columnCache.length) {
                    continue;
                }

                FalloutColumnCacheEntry entry = columnCache[cacheIndex];
                if (entry == null) {
                    entry = new FalloutColumnCacheEntry();
                    columnCache[cacheIndex] = entry;
                }

                boolean needsUpdate =
                        entry.centerX != centerX ||
                        entry.centerZ != centerZ ||
                        currentTick - entry.lastUpdateTick > 5 ||
                        entry.height == 0;

                if (needsUpdate) {
                    entry.height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, layerX, layerZ);
                    entry.centerX = centerX;
                    entry.centerZ = centerZ;
                    entry.lastUpdateTick = currentTick;
                }

                int rainHeight = entry.height;
                int minHeight = rainHeight;
                int maxHeight = Math.min(level.getMaxBuildHeight(), minHeight + MAX_VISUAL_HEIGHT_ABOVE_SURFACE + verticalLayers);
                if (maxHeight <= minHeight) maxHeight = minHeight + verticalLayers;
                if (minHeight == maxHeight) continue;

                double distX = layerX + 0.5 - camera.getX();
                double distZ = layerZ + 0.5 - camera.getZ();
                double horizCamSq = distX * distX + distZ * distZ;
                if (horizCamSq > MAX_COLUMN_CAMERA_HORIZONTAL_DIST_SQ) {
                    continue;
                }

                float swayLoop = ((timer & 511) + interp) / 512.0F;
                int hash = columnHash(layerX, layerZ);
                float fallVariation = 0.4F + ((hash & 0xFF) / 255.0F) * 0.2F;
                float swayVariation = ((hash >>> 8) & 0xFF) / 255.0F;

                float distToCam = Mth.sqrt((float) horizCamSq);
                float intensityMod = Math.min(1.0F, distToCam / (float) (renderRadius + 1));
                float alpha = ((1.0F - intensityMod * 0.5F) * 0.4F + 0.4F) * intensity;
                alpha = Mth.clamp(alpha, 0.05F, 0.9F);
                float colorMod = 0.85F;

                float u0 = 0.0F + fallVariation;
                float u1 = 1.0F + fallVariation;
                float vMin = minHeight / 4.0F + swayLoop + swayVariation;
                float vMax = maxHeight / 4.0F + swayLoop + swayVariation;

                float vx0 = (float) (layerX - rainCoordX + 0.5);
                float vz0 = (float) (layerZ - rainCoordY + 0.5);
                float vx1 = (float) (layerX + rainCoordX + 0.5);
                float vz1 = (float) (layerZ + rainCoordY + 0.5);

                ImmediateVertexWriter.texColor(consumer, matrix, vx0, minHeight, vz0, colorMod, colorMod, colorMod, alpha, u0, vMin);
                ImmediateVertexWriter.texColor(consumer, matrix, vx1, minHeight, vz1, colorMod, colorMod, colorMod, alpha, u1, vMin);
                ImmediateVertexWriter.texColor(consumer, matrix, vx1, maxHeight, vz1, colorMod, colorMod, colorMod, alpha, u1, vMax);
                ImmediateVertexWriter.texColor(consumer, matrix, vx0, maxHeight, vz0, colorMod, colorMod, colorMod, alpha, u0, vMax);
            }
        }
    }

    private static int columnHash(int x, int z) {
        int h = x * 73428767 ^ z * 912931;
        h ^= (h >>> 13);
        h *= 1274126177;
        h ^= (h >>> 16);
        return h;
    }
}
