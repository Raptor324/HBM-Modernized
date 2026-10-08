package com.hbm_m.client.render.item;

import com.hbm_m.client.render.util.MissilePronter;
import com.hbm_m.item.missile.ItemCustomMissile;
import com.hbm_m.item.missile.MissileStruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemRenderMissile}: in der Hand/am Boden mit Faktor 0.2 und 2 Einheiten seitlich versetzt, im Inventar
 * auf die Gesamthoehe skaliert, schraeg gestellt und langsam um die eigene Achse drehend.
 */
public class ItemRenderMissileCustom extends BlockEntityWithoutLevelRenderer {

    public static final ItemRenderMissileCustom INSTANCE = new ItemRenderMissileCustom();

    private ItemRenderMissileCustom() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        MissileStruct missile = ItemCustomMissile.getStruct(stack);
        if (missile == null) return;

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);

        if (ctx == ItemDisplayContext.GUI) {
            double height = MissilePronter.guiHeight(missile);
            if (height == 0D) height = 4D;
            float scale = (float) (1.4 / height);
            ps.translate(0, -0.2, 0);
            ps.mulPose(Axis.YN.rotationDegrees(225));
            ps.mulPose(Axis.XN.rotationDegrees(30));
            ps.mulPose(Axis.ZP.rotationDegrees(-45));
            ps.translate(0, -0.7, 0);
            ps.scale(scale, scale, scale);
            ps.mulPose(Axis.YN.rotationDegrees(System.currentTimeMillis() / 25 % 360));
        } else {
            float s = 0.2F;
            ps.scale(s, s, s);
            ps.translate(2, 0, 0);
        }

        MissilePronter.prontMissile(missile, ps, buffers, light);
        ps.popPose();
    }
}
