package com.hbm_m.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * vI: Inventar-Kette des Original-{@code ItemRenderBase} (Fall INVENTORY) fuer BEWLR-Gegenstaende, deren
 * builtin/entity-Modell vom Anzeige-Waechter keine Display-Transformation bekommt.
 *
 * <p>Eingang: Pose von {@code renderByItem} im GUI = T(x+8, y+8) S(16,-16,16) T(-0.5). Danach steht die Pose genau
 * dort, wo das Original nach {@code GL11.glTranslated(8, 10, 0); glRotated(-30, 1,0,0); glRotated(45, 0,1,0);
 * glScaled(-1,-1,-1)} stand (Ursprung = linke obere Slotecke); es folgen {@code renderInventory()} und
 * {@code renderCommon*()} wie im Original.</p>
 */
public final class OrigInventoryTransform {

    private OrigInventoryTransform() {}

    public static void apply(PoseStack ps) {
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.scale(1F / 16F, -1F / 16F, 1F / 16F);
        ps.translate(-8F, -8F, 0F);
        // ItemRenderBase.renderItem, INVENTORY
        ps.translate(8F, 10F, 0F);
        ps.mulPose(Axis.XP.rotationDegrees(-30F));
        ps.mulPose(Axis.YP.rotationDegrees(45F));
        ps.scale(-1F, -1F, -1F);
    }
}
