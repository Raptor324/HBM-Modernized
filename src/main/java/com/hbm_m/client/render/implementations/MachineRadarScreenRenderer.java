package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadarScreenBlockEntity;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * BER экрана радара (порт {@code RenderRadarScreen} из 1.7.10).
 *
 * Рисует:
 *   1. ТЕЛО — OBJ-каркас 2×2×1 (порт {@code ResourceManager.radar_screen.renderAll()}),
 *      берётся из {@link MachineRadarScreenBakedModel} (часть {@code Plane}) и рендерится
 *      через VertexConsumer в позиции контроллера с поворотом по FACING.
 *   2. ОВЕРЛЕЙ — на лицевой грани:
 *           - слинкован с радаром — бегущая зелёная полоса развёртки + метки целей
 *        (UV как в GUI: 216/256, blipLevel*8);
 *      - не слинкован — статичный «шум» из текстуры gui_radar_nt.
 *
 * Блок имеет {@code RenderShape.INVISIBLE}, поэтому ваниль block-model не рисуется —
 * всё тело отдаёт BER.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineRadarScreenRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineRadarScreenBlockEntity> {

    private static final ResourceLocation RADAR_TEX =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_radar_nt.png");

    private static final RandomSource RAND = RandomSource.create(42L);
    private DynamicTexture heightMapTexture;
    private ResourceLocation heightMapTextureLocation;
    private final byte[] renderedHeightMap = new byte[MachineRadarBlockEntity.MAP_LENGTH];
    private boolean heightMapInitialized;

    public MachineRadarScreenRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(MachineRadarScreenBlockEntity screen, float partialTick, PoseStack pose,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 1. ТЕЛО — OBJ-каркас (порт ResourceManager.radar_screen.renderAll()).
        pose.pushPose();
        pose.translate(0.5D, 0.0D, 0.5D);
        applyFacingRotation(screen.getBlockState(), pose);
        renderBody(pose, bufferSource, packedLight, packedOverlay, screen.getBlockState());

        // ВАЖНО: тело идёт через deferred cutout-буфер, а оверлей — immediate-mode.
        // Без flush тело рисуется ПОСЛЕ оверлея и перекрывает его. Сбрасываем буфер,
        // чтобы тело (с записью depth) отрисовалось до оверлея (порт glDepthMask(false)
        // в оригинале — оверлей тестировал depth уже записанного тела).
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(RenderType.cutout());
        }

        long time = screen.getLevel() != null ? screen.getLevel().getGameTime() : 0L;

        pose.pushPose();
        pose.translate(0.5D, 1.0D, 0.5D);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180F));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90F));
        pose.translate(-0.5D, -1.0D, -0.5D);
        pose.translate(-0.8D, 0.0D, 0.0D);

        if (screen.linked || screen.showMap || !screen.entries.isEmpty()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            if (screen.showMap) {
                renderHeightMap(screen, pose);
            }
            renderSweep(pose, time, partialTick);
            renderBlips(screen, pose, bufferSource);
            pose.popPose();
            RenderSystem.depthMask(true);
        } else {
            renderNoise(pose, bufferSource, time);
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * Рендер OBJ-тела (модель {@code forge:obj} из {@code radar_screen.json}).
     * Порт {@code bindTexture(radar_screen_tex); radar_screen.renderAll();} —
     * геометрия и UV запечены в стандартной BakedModel, рендерим все quads.
     */
    private void renderBody(PoseStack pose, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay, BlockState state) {
        BlockState northState = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getBlock().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                : state;
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(northState);
        model = com.hbm_m.client.render.AbstractPartBasedRenderer.unwrapFabricForwardingModels(model);
        if (model == null) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        PoseStack.Pose matrix = pose.last();
        for (Direction dir : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(null, dir, RAND);
            if (quads == null || quads.isEmpty()) {
                continue;
            }
            for (BakedQuad quad : quads) {
                putQuad(consumer, matrix, quad, packedLight, packedOverlay);
            }
        }
        List<BakedQuad> general = model.getQuads(null, null, RAND);
        if (general != null) {
            for (BakedQuad quad : general) {
                putQuad(consumer, matrix, quad, packedLight, packedOverlay);
            }
        }
    }

    private static void putQuad(VertexConsumer consumer, PoseStack.Pose matrix, BakedQuad quad,
                                int packedLight, int packedOverlay) {
        RenderHooks.putBulkData(consumer, matrix, quad, 1F, 1F, 1F, 1F, packedLight, packedOverlay, false);
    }

    private void renderHeightMap(MachineRadarScreenBlockEntity screen, PoseStack pose) {
        if (screen.heightMap == null || screen.heightMap.length < MachineRadarBlockEntity.MAP_LENGTH) {
            return;
        }
        if (heightMapTexture == null) {
            heightMapTexture = new DynamicTexture(new NativeImage(
                    MachineRadarBlockEntity.MAP_DIM, MachineRadarBlockEntity.MAP_DIM, true));
            heightMapTextureLocation = Minecraft.getInstance().getTextureManager().register(
                    "hbm_m_radar_screen_heightmap", heightMapTexture);
        }

        boolean changed = !heightMapInitialized;
        if (!changed) {
            for (int i = 0; i < MachineRadarBlockEntity.MAP_LENGTH; i++) {
                if (renderedHeightMap[i] != screen.heightMap[i]) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            NativeImage image = heightMapTexture.getPixels();
            for (int i = 0; i < MachineRadarBlockEntity.MAP_LENGTH; i++) {
                int height = Byte.toUnsignedInt(screen.heightMap[i]);
                int mapX = i % MachineRadarBlockEntity.MAP_DIM;
                int mapZ = i / MachineRadarBlockEntity.MAP_DIM;
                if (height == 0) {
                    image.setPixelRGBA(mapX, mapZ, 0x00000000);
                } else {
                    int green = net.minecraft.util.Mth.clamp((height - 50) * 255 / 78, 0, 255);
                    image.setPixelRGBA(mapX, mapZ, 0xFF000000 | green << 8);
                }
            }
            heightMapTexture.upload();
            System.arraycopy(screen.heightMap, 0, renderedHeightMap, 0, MachineRadarBlockEntity.MAP_LENGTH);
            heightMapInitialized = true;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, heightMapTextureLocation);

        org.joml.Matrix4f matrix = pose.last().pose();
        BufferBuilder buffer = RenderHooks.beginTesselator(Tesselator.getInstance(), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float x = 0.381F;
        float yTop = 1.875F;
        float yBottom = 0.125F;
        float zLeft = -0.375F;
        float zRight = 1.375F;
        
        RenderHooks.vertexTexColor(buffer, matrix, x, yTop, zRight, 0F, 0F, 255, 255, 255, 255);
        RenderHooks.vertexTexColor(buffer, matrix, x, yTop, zLeft, 1F, 0F, 255, 255, 255, 255);
        RenderHooks.vertexTexColor(buffer, matrix, x, yBottom, zLeft, 1F, 1F, 255, 255, 255, 255);
        RenderHooks.vertexTexColor(buffer, matrix, x, yBottom, zRight, 0F, 1F, 255, 255, 255, 255);
        
        RenderHooks.drawWithShader(buffer);
        RenderSystem.disableBlend();
    }

    private void renderSweep(PoseStack pose, long time, float partialTick) {
        double offset = ((time % 56) + partialTick) / 30D;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        org.joml.Matrix4f matrix = pose.last().pose();
        
        BufferBuilder buffer = RenderHooks.beginTesselator(Tesselator.getInstance(), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float x = 0.38F;
        float yTop = (float) (2D - offset);
        float yBottom = (float) (2D - offset - 0.125D);

        RenderHooks.vertexColor(buffer, matrix, x, yTop, 1.375F, 0, 31, 0, 255);
        RenderHooks.vertexColor(buffer, matrix, x, yTop, -0.375F, 0, 31, 0, 255);
        RenderHooks.vertexColor(buffer, matrix, x, yBottom, -0.375F, 0, 31, 0, 255);
        RenderHooks.vertexColor(buffer, matrix, x, yBottom, 1.375F, 0, 31, 0, 255);
        RenderHooks.drawWithShader(buffer);

        buffer = RenderHooks.beginTesselator(Tesselator.getInstance(), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderHooks.vertexColor(buffer, matrix, x, yTop, 1.375F, 0, 255, 0, 0);
        RenderHooks.vertexColor(buffer, matrix, x, yTop, -0.375F, 0, 255, 0, 0);
        RenderHooks.vertexColor(buffer, matrix, x, yBottom, -0.375F, 0, 255, 0, 50);
        RenderHooks.vertexColor(buffer, matrix, x, yBottom, 1.375F, 0, 255, 0, 50);

        RenderHooks.drawWithShader(buffer);
        RenderSystem.disableBlend();
    }

    private void renderBlips(MachineRadarScreenBlockEntity screen, PoseStack pose, MultiBufferSource bufferSource) {
        if (screen.entries.isEmpty() || screen.range <= 0) {
            return;
        }
        bindRadarTexture();
        org.joml.Matrix4f matrix = pose.last().pose();
        BufferBuilder buffer = RenderHooks.beginTesselator(Tesselator.getInstance(), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float x = 0.38F;
        double denom = screen.range + 1;
        double size = 0.0625D;

        for (int[] e : screen.entries) {
            if (e == null || e.length < 5) continue;
            double sX = (e[0] - screen.refX) / denom * 0.875D;
            double sZ = (e[2] - screen.refZ) / denom * 0.875D;
            int blip = Math.max(0, e[4]);
            float u0 = 216F / 256F;
            float u1 = 224F / 256F;
            float v0 = blip * 8F / 256F;
            float v1 = (blip * 8F + 8F) / 256F;

            float cy = (float) (1D - sZ);
            float cz = (float) (0.5D - sX);
            RenderHooks.vertexTexColor(buffer, matrix, x, cy + (float) size, cz + (float) size, u0, v1, 255, 255, 255, 255);
            RenderHooks.vertexTexColor(buffer, matrix, x, cy + (float) size, cz - (float) size, u1, v1, 255, 255, 255, 255);
            RenderHooks.vertexTexColor(buffer, matrix, x, cy - (float) size, cz - (float) size, u1, v0, 255, 255, 255, 255);
            RenderHooks.vertexTexColor(buffer, matrix, x, cy - (float) size, cz + (float) size, u0, v0, 255, 255, 255, 255);
        }
        RenderHooks.drawWithShader(buffer);
    }

    private void renderNoise(PoseStack pose, MultiBufferSource bufferSource, long time) {
        bindRadarTexture();
        org.joml.Matrix4f matrix = pose.last().pose();
        int offset = 118 + (int) (time % 81);
        float u0 = 216F / 256F;
        float u1 = 256F / 256F;
        float v0 = offset / 256F;
        float v1 = (offset + 40F) / 256F;
        float x = 0.38F;
        
        BufferBuilder buffer = RenderHooks.beginTesselator(Tesselator.getInstance(), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        RenderHooks.vertexTexColor(buffer, matrix, x, 1.875F, 1.375F, u0, v1, 255, 255, 255, 255);
        RenderHooks.vertexTexColor(buffer, matrix, x, 1.875F, -0.375F, u1, v1, 255, 255, 255, 255);
        RenderHooks.vertexTexColor(buffer, matrix, x, 0.125F, -0.375F, u1, v0, 255, 255, 255, 255);
        RenderHooks.vertexTexColor(buffer, matrix, x, 0.125F, 1.375F, u0, v0, 255, 255, 255, 255);
        RenderHooks.drawWithShader(buffer);
    }

    private static void bindRadarTexture() {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, RADAR_TEX);
    }

    private static void applyFacingRotation(BlockState state, PoseStack pose) {
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float rot = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 270F;
            case WEST -> 90F;
            default -> 0F;
        };
        if (rot != 0F) {
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rot));
        }
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(MachineRadarScreenBlockEntity be) {
        return true;
    }
}