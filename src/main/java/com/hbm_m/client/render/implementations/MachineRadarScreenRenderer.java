package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadarScreenBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
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
public class MachineRadarScreenRenderer implements BlockEntityRenderer<MachineRadarScreenBlockEntity> {

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


        // Предыдущая ориентация была развернута на 90 градусов в сторону тыльной
        // поверхности. Добавляем ещё 180 градусов по Y (итого -90) и выдвигаем
        // плоскость на 0.02 блока вперед по её локальной нормали.
        pose.pushPose();
        pose.translate(0.5D, 1.0D, 0.5D);
        // После поворота вокруг Y экранная плоскость становится Z-плоскостью,
        // поэтому переворот изображения «вверх ногами» выполняется вокруг Z.
        // Оба вращения находятся внутри одного pivot, чтобы не сместить BER.
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180F));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90F));
        pose.translate(-0.5D, -1.0D, -0.5D);
        pose.translate(-0.8D, 0.0D, 0.0D);

        if (screen.linked || screen.showMap || !screen.entries.isEmpty()) {
            // Overlay должен участвовать в обычном depth-тесте мира и записывать
            // глубину. При depthMask(false) последующие сущности/BER могли пройти
            // поверх карты и индикаторов, тогда как шум записывал depth и выглядел
            // корректно.
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
        // Берём NORTH-вариант модели (без y-поворота из blockstate-варианта).
        // Иначе был бы ДВОЙНОЙ поворот: blockstate уже испёк y-ротацию в quads,
        // а applyFacingRotation крутит ещё раз → корпус смотрел криво/в одну сторону.
        // BER применяет поворот сам (как MachineRadarRenderer для тарелки).
        BlockState northState = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getBlock().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                : state;
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(northState);
        // Снимаем возможные обёртки (Continuity/FRAPI).
        model = com.hbm_m.client.render.AbstractPartBasedRenderer.unwrapFabricForwardingModels(model);
        if (model == null) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
        PoseStack.Pose matrix = pose.last();
        // quads по всем cull-граням + general (null).
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

    /** putBulkData с учётом платформы: Forge 9-arg (alpha + hasNormal), Fabric 7-arg. */
    private static void putQuad(VertexConsumer consumer, PoseStack.Pose matrix, BakedQuad quad,
                                int packedLight, int packedOverlay) {
        //? if forge {
        consumer.putBulkData(matrix, quad, 1F, 1F, 1F, 1F, packedLight, packedOverlay, false);
        //?}
        //? if fabric {
        /*consumer.putBulkData(matrix, quad, 1F, 1F, 1F, packedLight, packedOverlay);
        *///?}
    }

    /** Рисует карту HeightMap на лицевой плоскости экрана с прозрачными пустыми ячейками. */
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

        // Карта находится чуть перед внутренней поверхностью экрана (X=0.375),
        // поэтому может использовать обычный depth-test и корректно скрываться
        // сущностями или другими BER, расположенными перед экраном.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, heightMapTextureLocation);

        org.joml.Matrix4f matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float x = 0.381F;
        float yTop = 1.875F;
        float yBottom = 0.125F;
        float zLeft = -0.375F;
        float zRight = 1.375F;
        // V отображается «севером вверх»: v=0 — верх текстуры (mapZ=0, север)
        // попадает на верхнюю кромку экрана (yTop). Раньше V был перевёрнут
        // компенсацией под Z-флип оверлея; теперь оверлей не повёрнут, поэтому V
        // идёт напрямую. U как в оригинале: zLeft(-0.375)=восток=u1, zRight(1.375)=запад=u0.
        buffer.vertex(matrix, x, yTop, zRight).uv(0F, 0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x, yTop, zLeft).uv(1F, 0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x, yBottom, zLeft).uv(1F, 1F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x, yBottom, zRight).uv(0F, 1F).color(255, 255, 255, 255).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    /** Бегущая зелёная полоса развёртки (порт GL_QUADS-полосы из RenderRadarScreen). */
    private void renderSweep(PoseStack pose, long time, float partialTick) {
        // Точная формула оригинала: offset = ((time%56)+f)/30. С ростом времени
        // 2-offset убывает → полоса идёт СВЕРХУ ВНИЗ. Оверлей больше не перевёрнут
        // Z-вращением (см. render()), поэтому инверсия смещения, как прежде, не нужна.
        double offset = ((time % 56) + partialTick) / 30D;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        org.joml.Matrix4f matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float x = 0.38F;
        float yTop = (float) (2D - offset);
        float yBottom = (float) (2D - offset - 0.125D);

        // Полоса полупрозрачная, но под ней всегда должна быть непрозрачная
        // поверхность дисплея. Иначе alpha=50 смешивается уже с framebuffer мира
        // и через полосу видны блоки позади корпуса. Цвет совпадает с базовым
        // зелёным пикселем экрана radar_screen.png.
        buffer.vertex(matrix, x, yTop, 1.375F)
                .color(0, 31, 0, 255).endVertex();
        buffer.vertex(matrix, x, yTop, -0.375F)
                .color(0, 31, 0, 255).endVertex();
        buffer.vertex(matrix, x, yBottom, -0.375F)
                .color(0, 31, 0, 255).endVertex();
        buffer.vertex(matrix, x, yBottom, 1.375F)
                .color(0, 31, 0, 255).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // Точная форма оригинального BER: верхняя кромка полностью прозрачна,
        // нижняя имеет alpha=50. Поэтому видна мягкая горизонтальная полоса,
        // которая движется (после инверсии offset) сверху вниз и зацикливается.
        buffer.vertex(matrix, x, yTop, 1.375F)
                .color(0, 255, 0, 0).endVertex();
        buffer.vertex(matrix, x, yTop, -0.375F)
                .color(0, 255, 0, 0).endVertex();
        buffer.vertex(matrix, x, yBottom, -0.375F)
                .color(0, 255, 0, 50).endVertex();
        buffer.vertex(matrix, x, yBottom, 1.375F)
                .color(0, 255, 0, 50).endVertex();

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    /**
     * Метки целей (порт addVertexWithUV-цикла). UV как в GUI: U=216..224/256,
     * V=blipLevel*8..+8/256. Immediate-mode с bound текстурой gui_radar_nt.
     */
    private void renderBlips(MachineRadarScreenBlockEntity screen, PoseStack pose, MultiBufferSource bufferSource) {
        if (screen.entries.isEmpty() || screen.range <= 0) {
            return;
        }
        bindRadarTexture();
        org.joml.Matrix4f matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // x=0.38F — та же лицевая плоскость, что у sweep/noise (порт RenderRadarScreen:
        // все оверлеи рисуются на x=0.38). При 0.379F метки оказались позади корпуса
        // и depth-test их отсекал.
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
            buffer.vertex(matrix, x, cy + (float) size, cz + (float) size).uv(u0, v1).color(255, 255, 255, 255).endVertex();
            buffer.vertex(matrix, x, cy + (float) size, cz - (float) size).uv(u1, v1).color(255, 255, 255, 255).endVertex();
            buffer.vertex(matrix, x, cy - (float) size, cz - (float) size).uv(u1, v0).color(255, 255, 255, 255).endVertex();
            buffer.vertex(matrix, x, cy - (float) size, cz + (float) size).uv(u0, v0).color(255, 255, 255, 255).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
    }

    /** Статичный шум когда экран не слинкован (порт else-ветки RenderRadarScreen). */
    private void renderNoise(PoseStack pose, MultiBufferSource bufferSource, long time) {
        bindRadarTexture();
        org.joml.Matrix4f matrix = pose.last().pose();
        int offset = 118 + (int) (time % 81);
        float u0 = 216F / 256F;
        float u1 = 256F / 256F;
        float v0 = offset / 256F;
        float v1 = (offset + 40F) / 256F;
        float x = 0.38F;
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, x, 1.875F, 1.375F).uv(u0, v1).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x, 1.875F, -0.375F).uv(u1, v1).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x, 0.125F, -0.375F).uv(u1, v0).color(255, 255, 255, 255).endVertex();
        buffer.vertex(matrix, x, 0.125F, 1.375F).uv(u0, v0).color(255, 255, 255, 255).endVertex();
        BufferUploader.drawWithShader(buffer.end());
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
        // Таблица поворотов как у MachineRadarRenderer (радар-тарелка):
        // NORTH→0, SOUTH→180, EAST→270, WEST→90.
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
