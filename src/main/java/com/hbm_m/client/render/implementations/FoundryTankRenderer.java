package com.hbm_m.client.render.implementations;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFoundryTankBlockEntity;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.joml.Matrix4f;

/**
 * Порт металлической части {@code RenderFoundryTank} (1.7.10, ISBRH): расплав в
 * литейном танке — fullbright-квады (оригинал setBrightness(240)), цвет
 * {@code 255-(255-c)*0.7} от moltenColor; уровень 0.75 (+0.125 за каждый танк
 * снизу/сверху в колонне); боковые квады рисуются только там, где стоит соседний
 * танк (сплошная колонна). Оболочка танка — обычная block-модель.
 */
//? if < 1.21.1 {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} else {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class FoundryTankRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFoundryTankBlockEntity> {

    private static final ResourceLocation LAVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava_gray.png");

    private static final RenderType METAL_RT = TankRenderTypes.METAL;

    public FoundryTankRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(MachineFoundryTankBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (be.getFillLevel() <= 0f) return;
        Level level = be.getLevel();
        if (level == null) return;

        var pos = be.getBlockPos();
        Block self = ModBlocks.FOUNDRY_TANK.get();
        boolean conNegY = level.getBlockState(pos.below()).getBlock() == self;
        boolean conPosY = level.getBlockState(pos.above()).getBlock() == self;
        boolean conPosX = level.getBlockState(pos.east()).getBlock() == self;
        boolean conNegX = level.getBlockState(pos.west()).getBlock() == self;
        boolean conPosZ = level.getBlockState(pos.south()).getBlock() == self;
        boolean conNegZ = level.getBlockState(pos.north()).getBlock() == self;

        float max = 0.75f + (conNegY ? 0.125f : 0f) + (conPosY ? 0.125f : 0f);
        float levelY = be.getFillLevel() * max;

        // Оригинал: 255-(255-c)*0.7 — «экранное» осветление к белому.
        int raw = be.getFillColor() & 0xFFFFFF;
        int cr = 255 - (int) ((255 - (raw >> 16 & 0xFF)) * 0.7f);
        int cg = 255 - (int) ((255 - (raw >> 8 & 0xFF)) * 0.7f);
        int cb = 255 - (int) ((255 - (raw & 0xFF)) * 0.7f);
        float r = cr / 255f;
        float g = cg / 255f;
        float b = cb / 255f;

        VertexConsumer vc = bufferSource.getBuffer(METAL_RT);
        Matrix4f m = poseStack.last().pose();
        int fullbright = 0xF000F0;

        // Верхняя поверхность расплава.
        hQuad(vc, m, 0f, 1f, 0f, 1f, levelY, r, g, b, fullbright);

        // Боковые квады колонны — только на соединениях с соседними танками.
        if (conPosX) vQuad(vc, m, 1f, true, 0f, 1f, levelY, r, g, b, fullbright);
        if (conNegX) vQuad(vc, m, 0f, true, 0f, 1f, levelY, r, g, b, fullbright);
        if (conPosZ) vQuad(vc, m, 1f, false, 0f, 1f, levelY, r, g, b, fullbright);
        if (conNegZ) vQuad(vc, m, 0f, false, 0f, 1f, levelY, r, g, b, fullbright);
    }

    private static void hQuad(VertexConsumer vc, Matrix4f m,
                              float x0, float x1, float z0, float z1, float y,
                              float r, float g, float b, int light) {
        RenderHooks.vertexColorUvLightNormal(vc, m, x0, y, z0, color(r), color(g), color(b), 0xFF, 0, 0, light, 0, 1, 0);
        RenderHooks.vertexColorUvLightNormal(vc, m, x0, y, z1, color(r), color(g), color(b), 0xFF, 0, 1, light, 0, 1, 0);
        RenderHooks.vertexColorUvLightNormal(vc, m, x1, y, z1, color(r), color(g), color(b), 0xFF, 1, 1, light, 0, 1, 0);
        RenderHooks.vertexColorUvLightNormal(vc, m, x1, y, z0, color(r), color(g), color(b), 0xFF, 1, 0, light, 0, 1, 0);
    }


    /** float 0..1 -> byte для int-цвета хуков. */
    private static int color(float c) {
        return (int) (c * 255.0f + 0.5f);
    }

    /**
     * Вертикальный квад колонны расплава высотой 0..y.
     * axisX=true — плоскость X=fixed (грань восток/запад), иначе Z=fixed (север/юг).
     */
    private static void vQuad(VertexConsumer vc, Matrix4f m,
                              float fixed, boolean axisX, float u0, float u1, float y,
                              float r, float g, float b, int light) {
        if (axisX) {
            RenderHooks.vertexColorUvLightNormal(vc, m, fixed, 0f, u0, color(r), color(g), color(b), 0xFF, 0, 1, light, 1, 0, 0);
            RenderHooks.vertexColorUvLightNormal(vc, m, fixed, y,  u0, color(r), color(g), color(b), 0xFF, 0, 0, light, 1, 0, 0);
            RenderHooks.vertexColorUvLightNormal(vc, m, fixed, y,  u1, color(r), color(g), color(b), 0xFF, 1, 0, light, 1, 0, 0);
            RenderHooks.vertexColorUvLightNormal(vc, m, fixed, 0f, u1, color(r), color(g), color(b), 0xFF, 1, 1, light, 1, 0, 0);
        } else {
            RenderHooks.vertexColorUvLightNormal(vc, m, u0, 0f, fixed, color(r), color(g), color(b), 0xFF, 0, 1, light, 0, 0, 1);
            RenderHooks.vertexColorUvLightNormal(vc, m, u0, y,  fixed, color(r), color(g), color(b), 0xFF, 0, 0, light, 0, 0, 1);
            RenderHooks.vertexColorUvLightNormal(vc, m, u1, y,  fixed, color(r), color(g), color(b), 0xFF, 1, 0, light, 0, 0, 1);
            RenderHooks.vertexColorUvLightNormal(vc, m, u1, 0f, fixed, color(r), color(g), color(b), 0xFF, 1, 1, light, 0, 0, 1);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(MachineFoundryTankBlockEntity blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    private static final class TankRenderTypes extends RenderType {
        private static final RenderType METAL = create("hbm_m_foundry_tank_metal",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(LAVA_TEXTURE, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .createCompositeState(false));

        private TankRenderTypes(String s, VertexFormat v, VertexFormat.Mode m,
                                int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }
    }
}
