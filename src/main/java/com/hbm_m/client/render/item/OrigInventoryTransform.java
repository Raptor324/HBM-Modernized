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
        org.joml.Matrix4f baseP = new org.joml.Matrix4f(ps.last().pose());
        org.joml.Matrix3f baseN = new org.joml.Matrix3f(ps.last().normal());
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.scale(1F / 16F, -1F / 16F, 1F / 16F);
        ps.translate(-8F, -8F, 0F);
        // ItemRenderBase.renderItem, INVENTORY
        ps.translate(8F, 10F, 0F);
        ps.mulPose(Axis.XP.rotationDegrees(-30F));
        ps.mulPose(Axis.YP.rotationDegrees(45F));
        ps.scale(-1F, -1F, -1F);
        fixNormals(ps, baseP, baseN);
    }

    /**
     * vJ: Normalenmatrix exakt aus der eigenen Kette (Eingangsnormalen * inverse Transponierte der lokalen Matrix).
     * {@code PoseStack#scale} verliert bei gespiegelten nicht-uniformen Skalierungen (hier S(1/16,-1/16,1/16))
     * das Vorzeichen (fastInvCubeRoot einer negativen Zahl) -> Flaechen wurden von der falschen Seite beleuchtet.
     */
    public static void fixNormals(PoseStack ps, org.joml.Matrix4f baseP, org.joml.Matrix3f baseN) {
        org.joml.Matrix4f local = new org.joml.Matrix4f(baseP).invert().mul(ps.last().pose());
        ps.last().normal().set(new org.joml.Matrix3f(baseN).mul(new org.joml.Matrix3f(local).invert().transpose()));
    }

    /**
     * vJ: {@code PoseStack#scale} mit korrekten Normalen auch fuer gespiegelte, nicht-uniforme Skalierungen.
     * 1.20.1 rechnet dort {@code fastInvCubeRoot} einer negativen Zahl (~1e25) -> Normalen saettigen beim Schreiben
     * (je Komponente +-1), Flaechen erscheinen schwarz/zu dunkel. Sonst identisch mit {@code ps.scale}.
     */
    public static void scaleSafe(PoseStack ps, float x, float y, float z) {
        if ((x == y && y == z) || x * y * z > 0) {
            ps.scale(x, y, z);
            return;
        }
        ps.last().pose().scale(x, y, z);
        float k = (float) Math.cbrt(Math.abs(x * y * z));
        ps.last().normal().scale(k / x, k / y, k / z);
    }
}
