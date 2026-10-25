package com.hbm_m.client.render.item;

import java.util.List;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * vJ: Inventar-Icons der RBMK-Saeulen 1:1 nach den ISBRH-Inventarpfaden des Originals
 * ({@code RenderRBMKRod/RenderRBMKReflector/RenderRBMKControl#renderInventoryBlock}) statt ueber den Welt-BER:
 * vier gestapelte Segmente (S 0.35), Seiten-/Deckeltexturen wie {@code RBMKBase#getIcon}, bei Brennelementen
 * OBJ "Inner"/"Cap" und gruen getoente Brennstaebe (0x304825), bei Steuerstaeben/Kesseln die vier Rohrstutzen
 * ({@code _pipe_top/_pipe_side}) und der Deckel {@code rbmk_rods "Lid"} (ohne Kessel/Heizer).
 * Die Lage im Slot entspricht {@code RenderItem#renderItemIntoGUI} fuer 3D-Bloecke:
 * T(x-2, y+3) S(10) T(1, 0.5, 1) S(1, 1, -1) Rx(210) Ry(45) Ry(-90).
 */
public final class RBMKColumnInventoryRender {

    private RBMKColumnInventoryRender() {}

    public enum Kind { ROD, PASSIVE, CONTROL }

    /** Original-Rendertyp je Port-ID, null = kein ISBRH-Saeulenblock */
    public static Kind kind(String id) {
        if (id.startsWith("rbmk_element")) return Kind.ROD;
        switch (id) {
            case "rbmk_blank", "rbmk_reflector", "rbmk_absorber", "rbmk_moderator", "rbmk_outgasser", "rbmk_storage", "rbmk_cooler":
                return Kind.PASSIVE;
            case "rbmk_control", "rbmk_control_mod", "rbmk_control_auto", "rbmk_control_reasim", "rbmk_control_reasim_auto",
                 "rbmk_boiler", "rbmk_heater":
                return Kind.CONTROL;
            default:
                return null;
        }
    }

    /** GUI-Pose von renderByItem (T(x+8,y+8) S(16,-16,16) T(-0.5)) -> Lage des Originals vor renderInventoryBlock */
    /** Pose/Normalenmatrix beim Eintritt (renderByItem); Normalen werden exakt ueber die eigene Kette gerechnet,
     * weil PoseStack#scale bei gespiegelten nicht-uniformen Skalierungen das Normalenvorzeichen verliert */
    private static final Matrix4f BASE_POSE = new Matrix4f();
    private static final Matrix3f BASE_NORMAL = new Matrix3f();

    public static void applyGuiBlockTransform(PoseStack ps) {
        BASE_POSE.set(ps.last().pose());
        BASE_NORMAL.set(ps.last().normal());
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.scale(1F / 16F, -1F / 16F, 1F / 16F);
        ps.translate(-8F, -8F, 0F);
        // RenderItem#renderItemIntoGUI, 3D-Block
        ps.translate(-2F, 3F, -3F);
        ps.scale(10F, 10F, 10F);
        ps.translate(1F, 0.5F, 1F);
        ps.scale(1F, 1F, -1F);
        ps.mulPose(Axis.XP.rotationDegrees(210F));
        ps.mulPose(Axis.YP.rotationDegrees(45F));
        ps.mulPose(Axis.YP.rotationDegrees(-90F));
    }

    private static TextureAtlasSprite tex(String name) {
        return RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + name);
    }

    public static void render(String id, Kind kind, PoseStack ps, MultiBufferSource buffers, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        TextureAtlasSprite top = tex(id + "_top");
        TextureAtlasSprite side = tex(id + "_side");
        ps.pushPose();
        switch (kind) {
            case ROD -> {
                Map<String, List<float[]>> element = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_element.obj");
                Map<String, List<float[]>> rods = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_element_rods.obj");
                TextureAtlasSprite inner = tex(id + "_inner");
                TextureAtlasSprite fuel = tex(id + "_fuel");
                ps.translate(0, -0.675, 0);
                ps.scale(0.35F, 0.35F, 0.35F);
                for (int i = 0; i < 4; i++) {
                    obj(vc, ps, element.get("Inner"), inner, 255, 255, 255, false, light);
                    obj(vc, ps, element.get("Cap"), top, 255, 255, 255, false, light);
                    box(vc, ps, 0, 0, 0, 1, 1, 1, null, null, side, light);
                    obj(vc, ps, rods.get("Rods"), fuel, 0x30, 0x48, 0x25, false, light);
                    ps.translate(0, 1, 0);
                }
            }
            case PASSIVE -> {
                ps.translate(0, -0.675, 0);
                ps.scale(0.35F, 0.35F, 0.35F);
                for (int i = 0; i < 4; i++) {
                    box(vc, ps, 0, 0, 0, 1, 1, 1, top, top, side, light);
                    ps.translate(0, 1, 0);
                }
            }
            case CONTROL -> {
                ps.translate(0, -0.75, 0);
                ps.scale(0.35F, 0.35F, 0.35F);
                for (int i = 0; i < 4; i++) {
                    box(vc, ps, 0, 0, 0, 1, 1, 1, top, top, side, light);
                    if (i < 3) ps.translate(0, 1, 0);
                }
                // RBMKPipedBase.renderPipes: vier Stutzen ueber dem obersten Segment, ohne Unterseite
                TextureAtlasSprite pTop = tex(id + "_pipe_top");
                TextureAtlasSprite pSide = tex(id + "_pipe_side");
                double[][] pipes = {
                        {0.0625, 0.0625, 0.4375, 0.4375}, {0.0625, 0.5625, 0.4375, 0.9375},
                        {0.5625, 0.5625, 0.9375, 0.9375}, {0.5625, 0.0625, 0.9375, 0.4375}};
                for (double[] p : pipes) {
                    box(vc, ps, p[0], 1, p[1], p[2], 1.125, p[3], pTop, null, pSide, light);
                }
                if (!id.equals("rbmk_boiler") && !id.equals("rbmk_heater")) {
                    Map<String, List<float[]>> lid = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_rods.obj");
                    obj(vc, ps, lid.get("Lid"), top, 255, 255, 255, true, light);
                }
            }
        }
        ps.popPose();
    }

    /**
     * Quader in Blockkoordinaten mit Ursprung (-0.5, 0, -0.5) wie {@code renderer.renderFace*(block, -0.5, y, -0.5, icon)};
     * UV wie RenderBlocks (Teilflaechen nehmen den passenden Texturausschnitt). null = Flaeche weglassen.
     */
    private static void box(VertexConsumer vc, PoseStack ps, double x0d, double y0d, double z0d, double x1d, double y1d, double z1d,
                            TextureAtlasSprite up, TextureAtlasSprite down, TextureAtlasSprite side, int light) {
        float x0 = (float) x0d, y0 = (float) y0d, z0 = (float) z0d, x1 = (float) x1d, y1 = (float) y1d, z1 = (float) z1d;
        float yy0 = y0 - (float) Math.floor(y0d), yy1 = y1 - (float) Math.floor(y0d); // Texturhoehe innerhalb des Blocks
        float ox = -0.5F, oz = -0.5F;
        if (up != null) {
            quad(vc, ps, up, light, 0, 1, 0,
                    ox + x1, y1, oz + z1, x1, z1, ox + x1, y1, oz + z0, x1, z0,
                    ox + x0, y1, oz + z0, x0, z0, ox + x0, y1, oz + z1, x0, z1);
        }
        if (down != null) {
            quad(vc, ps, down, light, 0, -1, 0,
                    ox + x0, y0, oz + z1, x0, z1, ox + x0, y0, oz + z0, x0, z0,
                    ox + x1, y0, oz + z0, x1, z0, ox + x1, y0, oz + z1, x1, z1);
        }
        if (side != null) {
            float v0 = 1 - yy1, v1 = 1 - yy0;
            // Z- (Norden)
            quad(vc, ps, side, light, 0, 0, -1,
                    ox + x0, y1, oz + z0, x1, v0, ox + x1, y1, oz + z0, x0, v0,
                    ox + x1, y0, oz + z0, x0, v1, ox + x0, y0, oz + z0, x1, v1);
            // Z+ (Sueden)
            quad(vc, ps, side, light, 0, 0, 1,
                    ox + x0, y1, oz + z1, x0, v0, ox + x0, y0, oz + z1, x0, v1,
                    ox + x1, y0, oz + z1, x1, v1, ox + x1, y1, oz + z1, x1, v0);
            // X- (Westen)
            quad(vc, ps, side, light, -1, 0, 0,
                    ox + x0, y1, oz + z1, z1, v0, ox + x0, y1, oz + z0, z0, v0,
                    ox + x0, y0, oz + z0, z0, v1, ox + x0, y0, oz + z1, z1, v1);
            // X+ (Osten)
            quad(vc, ps, side, light, 1, 0, 0,
                    ox + x1, y0, oz + z1, z0, v1, ox + x1, y0, oz + z0, z1, v1,
                    ox + x1, y1, oz + z0, z1, v0, ox + x1, y1, oz + z1, z0, v0);
        }
    }

    /** vier Ecken (x, y, z, u, v) mit u/v in 0..1 des Sprites */
    private static void quad(VertexConsumer vc, PoseStack ps, TextureAtlasSprite s, int light, float nx, float ny, float nz,
                             float... c) {
        Matrix4f m = ps.last().pose();
        Vector3f n = new Vector3f(nx, ny, nz).mul(normalMatrix(ps)).normalize();
        for (int i = 0; i < 4; i++) {
            int b = i * 5;
            com.hbm_m.platform.RenderHooks.vertexFull(vc, m, c[b], c[b + 1], c[b + 2], 255, 255, 255, 255,
                    com.hbm_m.platform.RenderHooks.spriteU(s, c[b + 3]), com.hbm_m.platform.RenderHooks.spriteV(s, c[b + 4]), OverlayTexture.NO_OVERLAY, light, n.x, n.y, n.z);
        }
    }

    /** {@code ObjUtil.renderPartWithIcon}: Flaechen mit Sprite-UV; shadow = Helligkeit aus der Flaechennormalen */
    private static void obj(VertexConsumer vc, PoseStack ps, List<float[]> tris, TextureAtlasSprite s,
                            int r, int g, int b, boolean shadow, int light) {
        if (tris == null || s == null) return;
        Matrix4f m = ps.last().pose();
        Matrix3f nm = normalMatrix(ps);
        Vector3f n = new Vector3f();
        for (float[] t : tris) {
            float fx = t[5] + t[13] + t[21], fy = t[6] + t[14] + t[22], fz = t[7] + t[15] + t[23];
            float len = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
            if (len > 0) { fx /= len; fy /= len; fz /= len; }
            float br = 1F;
            if (shadow) {
                br = (fy * 0.3F + 0.7F) - Math.abs(fx) * 0.1F + Math.abs(fz) * 0.1F;
                if (br < 0.45F) br = 0.45F;
            }
            n.set(fx, fy, fz).mul(nm).normalize();
            int cr = (int) (r * br), cg = (int) (g * br), cb = (int) (b * br);
            for (int pass = 0; pass < 4; pass++) {
                int base = Math.min(pass, 2) * 8;
                com.hbm_m.platform.RenderHooks.vertexFull(vc, m, t[base], t[base + 1], t[base + 2], cr, cg, cb, 255,
                        com.hbm_m.platform.RenderHooks.spriteU(s, t[base + 3]), com.hbm_m.platform.RenderHooks.spriteV(s, 1F - t[base + 4]), OverlayTexture.NO_OVERLAY, light, n.x, n.y, n.z);
            }
        }
    }
    private static Matrix3f normalMatrix(PoseStack ps) {
        Matrix4f local = new Matrix4f(BASE_POSE).invert().mul(ps.last().pose());
        return new Matrix3f(BASE_NORMAL).mul(new Matrix3f(local).invert().transpose());
    }

}
