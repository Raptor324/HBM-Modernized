package com.hbm_m.client.render.effect;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.entity.effect.FalloutRain;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

public class RenderFallout extends EntityRenderer<FalloutRain> {

    private static final ResourceLocation FALLOUT = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/fallout.png");

    private final Random random = new Random();
    private float[] rainXCoords;
    private float[] rainYCoords;
    private long lastTime = System.nanoTime();

    public RenderFallout(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FalloutRain entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity camera = mc.player;
        ClientLevel world = mc.level;
        if (camera == null || world == null) return;
        long time = System.nanoTime();
        float dt = (time - lastTime) / 50_000_000f;
        float interp = Math.min(dt, 1.0f);
        lastTime = time;

        poseStack.pushPose();
        renderRainSnow(entity, interp, poseStack, bufferSource, world, camera);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FalloutRain entity) {
        return FALLOUT;
    }

    @Override
    public boolean shouldRender(FalloutRain entity, Frustum frustum, double camX, double camY, double camZ) {
        int scale = entity.getScale();
        double dx = camX - entity.getX();
        double dz = camZ - entity.getZ();
        return dx * dx + dz * dz <= (double) (scale * scale) + 256;
    }

    private void renderRainSnow(FalloutRain entity, float interp, PoseStack pose, MultiBufferSource buffers, ClientLevel level, LivingEntity camera) {
        int timer = camera.tickCount;
        float intensity = 1.0F;

        double camX = camera.getX();
        double camY = camera.getY();
        double camZ = camera.getZ();
        pose.translate(-camX, -camY, -camZ);

        int centerX = Mth.floor(entity.getX());
        int centerZ = Mth.floor(entity.getZ());
        int centerY = Mth.floor(entity.getY());
        int scale = entity.getScale();
        if (scale < 1) return;

        int renderRadius = Math.min(scale, 64);
        if (this.rainXCoords == null) {
            this.rainXCoords = new float[1024];
            this.rainYCoords = new float[1024];
            for (int i = 0; i < 32; ++i) {
                for (int j = 0; j < 32; ++j) {
                    float f2 = j - 16;
                    float f3 = i - 16;
                    float f4 = Mth.sqrt(f2 * f2 + f3 * f3);
                    if (f4 < 1.0E-5F) {
                        this.rainXCoords[i << 5 | j] = 0;
                        this.rainYCoords[i << 5 | j] = 1;
                    } else {
                        this.rainXCoords[i << 5 | j] = -f3 / f4;
                        this.rainYCoords[i << 5 | j] = f2 / f4;
                    }
                }
            }
        }

        GraphicsStatus graphics = Minecraft.getInstance().options.graphicsMode().get();
        boolean fancy = graphics == GraphicsStatus.FANCY || graphics == GraphicsStatus.FABULOUS;
        int verticalLayers = fancy ? 10 : 5;

        ResourceLocation texture = FALLOUT;
        VertexConsumer consumer = buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ENTITY_SMOOTH.apply(texture));
        int overlay = OverlayTexture.NO_OVERLAY;
        var matrix = pose.last().pose();

        for (int layerZ = centerZ - renderRadius; layerZ <= centerZ + renderRadius; ++layerZ) {
            for (int layerX = centerX - renderRadius; layerX <= centerX + renderRadius; ++layerX) {
                double distSq = (layerX + 0.5 - entity.getX()) * (layerX + 0.5 - entity.getX()) + (layerZ + 0.5 - entity.getZ()) * (layerZ + 0.5 - entity.getZ());
                if (distSq > (double) (scale * scale)) continue;

                int relX = layerX - centerX;
                int relZ = layerZ - centerZ;
                int idx = ((relZ + 16) & 31) * 32 + ((relX + 16) & 31);
                float rainCoordX = this.rainXCoords[idx] * 0.5F;
                float rainCoordY = this.rainYCoords[idx] * 0.5F;

                int rainHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, layerX, layerZ);
                int minHeight = rainHeight;
                int maxHeight = Math.min(level.getMaxBuildHeight(), centerY + scale + verticalLayers);
                if (maxHeight <= minHeight) maxHeight = minHeight + verticalLayers;
                if (minHeight == maxHeight) continue;

                this.random.setSeed(layerX * 3121L + layerX * 45238971L ^ layerZ * 418711L + layerZ * 13761L);

                float swayLoop = ((timer & 511) + interp) / 512.0F;
                float fallVariation = 0.4F + this.random.nextFloat() * 0.2F;
                float swayVariation = this.random.nextFloat();

                double distX = layerX + 0.5 - camera.getX();
                double distZ = layerZ + 0.5 - camera.getZ();
                float distToCam = Mth.sqrt((float) (distX * distX + distZ * distZ));
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

                int light = getLightColor(level, vx0, minHeight, vz0);

                consumer.vertex(matrix, vx0, minHeight, vz0).color(colorMod, colorMod, colorMod, alpha).uv(u0, vMin).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
                consumer.vertex(matrix, vx1, minHeight, vz1).color(colorMod, colorMod, colorMod, alpha).uv(u1, vMin).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
                consumer.vertex(matrix, vx1, maxHeight, vz1).color(colorMod, colorMod, colorMod, alpha).uv(u1, vMax).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
                consumer.vertex(matrix, vx0, maxHeight, vz0).color(colorMod, colorMod, colorMod, alpha).uv(u0, vMax).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
            }
        }
    }

    private int getLightColor(Level level, double x, double y, double z) {
        BlockPos blockpos = BlockPos.containing(x, y, z);
        return level.isLoaded(blockpos) ? LevelRenderer.getLightColor(level, blockpos) : 0;
    }
}
