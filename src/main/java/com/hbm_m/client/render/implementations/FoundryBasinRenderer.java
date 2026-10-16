package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Matrix4f;

/**
 * Порт {@code RenderFoundry} (1.7.10) — общий TESR бассейна и изложницы:
 *  - поверхность расплава: серая текстура {@code lava_gray} с тинтом moltenColor,
 *    полный яр свет (оригинал: lightmap 240/240, lighting off) + второй аддитивный
 *    проход (белый, alpha 0.3, SRC_ALPHA/ONE, depthMask off) — свечение раскалённого
 *    металла; поверхность двусторонняя (оригинал glDisable(GL_CULL_FACE));
 *  - установленная форма (слот 0) — плоским предметом на moldHeight (0.13);
 *  - отлитый предмет (слот 1) — плоским предметом на outHeight, а блочные отливки —
 *    полупрозрачным (alpha 0.3) верхним квадом с текстурой блока.
 * Геометрия высот 1:1 через {@link MachineFoundryBasinBlockEntity} (basin 0.75/0.875,
 * mold 0.25/0.25).
 */
//? if < 1.21.1 {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} else {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class FoundryBasinRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFoundryBasinBlockEntity> {

    private static final ResourceLocation LAVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava_gray.png");

    private static final float INNER = 2f / 16f;
    private static final float INNER_MAX = 1f - INNER;

    /** Базовый проход: полупрозрачный, без отсечения задних граней (оригинал double-sided). */
    private static final RenderType METAL_RT = MetalRenderTypes.METAL;
    /** Аддитивный проход свечения: белый, alpha 0.3, SRC_ALPHA/ONE, без записи глубины. */
    private static final RenderType GLOW_RT = MetalRenderTypes.GLOW;
    /** Отлитый блок: СПЛОШНОЙ квад из блочного атласа (оригинал: blend выключен → alpha 0.3 игнорировался). */
    private static final RenderType BLOCK_GHOST_RT = RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS);

    public FoundryBasinRenderer(BlockEntityRendererProvider.Context context) { }

    private static final RandomSource RANDOM = RandomSource.create();

    @Override
    public void render(MachineFoundryBasinBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // ── Установленная форма (плоский предмет, оригинал drawItem) ──
        ItemStack mold = be.getMoldSlot();
        if (!mold.isEmpty()) {
            drawFlatItem(be, bufferSource, poseStack, mold, be.getMoldHeight(), packedLight, packedOverlay);
        }

        // ── Отлитый предмет ──
        ItemStack out = be.getOutputSlot();
        if (!out.isEmpty()) {
            if (out.getItem() instanceof BlockItem blockItem) {
                drawGhostBlockTop(bufferSource, poseStack, blockItem, be.getOutputHeight(), packedLight);
            } else {
                drawFlatItem(be, bufferSource, poseStack, out, be.getOutputHeight(), packedLight, packedOverlay);
            }
        }

        // ── Поверхность расплава ──
        float fill = be.getFillLevel();
        if (fill <= 0f) return;

        float surfaceY = 0.125f + fill * be.getSurfaceRange();

        int argb = be.getFillColor();
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;

        Matrix4f m = poseStack.last().pose();
        int fullbright = 0xF000F0;

        // Проход 1: тонированный металл (полный яр свет, двусторонний).
        VertexConsumer base = bufferSource.getBuffer(METAL_RT);
        metalQuad(base, m, surfaceY, r, g, b, 1f, fullbright);

        // Проход 2: аддитивное свечение цветом металла (оригинал добавлял белый 0.3,
        // но на fullbright-базе это клипало в чистый белый — тинт сохраняет оттенок).
        VertexConsumer glow = bufferSource.getBuffer(GLOW_RT);
        metalQuad(glow, m, surfaceY, r, g, b, 0.3f, fullbright);
    }


    /** float 0..1 -> byte для int-цвета хуков. */
    private static int color(float c) {
        return (int) (c * 255.0f + 0.5f);
    }

    private static void metalQuad(VertexConsumer vc, Matrix4f m, float y, float r, float g, float b, float a, int light) {
        int cr = color(r), cg = color(g), cb = color(b), ca = color(a);
        RenderHooks.vertexColorUvLight(vc, m, INNER,     y, INNER,     cr, cg, cb, ca, 0, 0, light);
        RenderHooks.vertexColorUvLight(vc, m, INNER,     y, INNER_MAX, cr, cg, cb, ca, 0, 1, light);
        RenderHooks.vertexColorUvLight(vc, m, INNER_MAX, y, INNER_MAX, cr, cg, cb, ca, 1, 1, light);
        RenderHooks.vertexColorUvLight(vc, m, INNER_MAX, y, INNER,     cr, cg, cb, ca, 1, 0, light);
    }

    /**
     * Позиционирование плоского предмета (форма или отливка) по центру чаши.
     * Модели предметов центрированы вокруг (0,0,0), поэтому трансляция в (0.5, h, 0.5)
     * со скейлом 0.75 точно центрирует предмет в каверне чаши (0.125..0.875).
     * Вращение -90° вокруг X кладёт предмет лицом вверх без зеркалирования текстуры.
     */
    private static void drawFlatItem(MachineFoundryBasinBlockEntity be, MultiBufferSource bufferSource,
                                     PoseStack poseStack, ItemStack stack, float height,
                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, height + 0.001D, 0.5D);
        float scale = 0.75F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90F));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, net.minecraft.world.item.ItemDisplayContext.NONE,
                packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);
        poseStack.popPose();
    }

    /** Оригинал drawBlock: верхний квад блока на высоте отливки с обычным освещением мира (не fullbright). */
    private static void drawGhostBlockTop(MultiBufferSource bufferSource, PoseStack poseStack,
                                          BlockItem blockItem, float height, int packedLight) {
        BlockState state = blockItem.getBlock().defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer()
                .getBlockModelShaper().getBlockModel(state);
        // Спрайт верхней грани (оригинал b.getIcon(1, meta)); фоллбек — particle icon.
        TextureAtlasSprite sprite = model.getParticleIcon();
        for (BakedQuad quad : model.getQuads(state, Direction.UP, RANDOM)) {
            sprite = quad.getSprite();
            break;
        }

        VertexConsumer vc = bufferSource.getBuffer(BLOCK_GHOST_RT);
        Matrix4f m = poseStack.last().pose();

        // NEW_ENTITY требует нормаль на вершину (иначе IllegalStateException — краш 1.21.1).
        RenderHooks.vertexFull(vc, m, INNER,     height, INNER,     0xFF, 0xFF, 0xFF, 0xFF, sprite.getU(0f), sprite.getV(1f), OverlayTexture.NO_OVERLAY, packedLight, 0, 1, 0);
        RenderHooks.vertexFull(vc, m, INNER,     height, INNER_MAX, 0xFF, 0xFF, 0xFF, 0xFF, sprite.getU(0f), sprite.getV(0f), OverlayTexture.NO_OVERLAY, packedLight, 0, 1, 0);
        RenderHooks.vertexFull(vc, m, INNER_MAX, height, INNER_MAX, 0xFF, 0xFF, 0xFF, 0xFF, sprite.getU(1f), sprite.getV(0f), OverlayTexture.NO_OVERLAY, packedLight, 0, 1, 0);
        RenderHooks.vertexFull(vc, m, INNER_MAX, height, INNER,     0xFF, 0xFF, 0xFF, 0xFF, sprite.getU(1f), sprite.getV(1f), OverlayTexture.NO_OVERLAY, packedLight, 0, 1, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(MachineFoundryBasinBlockEntity blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    /** Кастомные слои: металл (translucent no-cull), свечение (additive, без глубины), призрак блока. */
    private static final class MetalRenderTypes extends RenderType {
        private static final RenderType METAL = create("hbm_m_foundry_metal",
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

        private static final RenderType GLOW = create("hbm_m_foundry_glow",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(LAVA_TEXTURE, false, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));

        private MetalRenderTypes(String s, VertexFormat v, VertexFormat.Mode m,
                                 int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }
    }
}
