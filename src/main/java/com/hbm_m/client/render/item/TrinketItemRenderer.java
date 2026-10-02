package com.hbm_m.client.render.item;

import com.hbm_m.block.decorations.TrinketBlock;
import com.hbm_m.block.decorations.TrinketTypes;
import com.hbm_m.block.decorations.TrinketTypes.BobbleType;
import com.hbm_m.block.decorations.TrinketTypes.PlushieType;
import com.hbm_m.block.decorations.TrinketTypes.SnowglobeType;
import com.hbm_m.client.render.implementations.TrinketRenderer;
import com.hbm_m.item.TrinketBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Gegenstandsdarstellung der Sammelfiguren ({@code ItemRenderLibrary}/{@code IItemRendererProvider} des Originals):
 * dieselben Renderroutinen wie im Block, auf die Blockgroesse der Gegenstandsansicht skaliert.
 */
public class TrinketItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static TrinketItemRenderer instance;

    public static TrinketItemRenderer instance() {
        if (instance == null) instance = new TrinketItemRenderer();
        return instance;
    }

    private TrinketItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof TrinketBlock block)) return;
        int type = TrinketBlockItem.getType(stack);

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        ps.mulPose(Axis.YN.rotationDegrees(270));
        switch (block.kind) {
            case BOBBLE -> {
                ps.scale(0.35F, 0.35F, 0.35F);
                TrinketRenderer.renderBobble(ps, buf, TrinketTypes.safe(BobbleType.class, type), light);
            }
            case SNOWGLOBE -> {
                ps.translate(0, 0.125, 0);
                ps.scale(1.5F, 1.5F, 1.5F);
                TrinketRenderer.renderSnowglobe(ps, buf, TrinketTypes.safe(SnowglobeType.class, type), light);
            }
            case PLUSHIE -> {
                PlushieType p = TrinketTypes.safe(PlushieType.class, type);
                switch (p) {
                    case YOMI -> ps.scale(0.6F, 0.6F, 0.6F);
                    case NUMBERNINE -> ps.scale(0.6F, 0.6F, 0.6F);
                    case HUNDUN -> ps.scale(0.8F, 0.8F, 0.8F);
                    case DERG -> ps.scale(1.2F, 1.2F, 1.2F);
                    default -> { }
                }
                TrinketRenderer.renderPlushie(ps, buf, p, false, light);
            }
        }
        ps.popPose();
    }
}
