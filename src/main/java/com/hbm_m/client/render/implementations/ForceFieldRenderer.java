package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.ForceFieldBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

/**
 * 1:1-Port von {@code RenderMachineForceField} (1.7.10).
 *
 * <p>Der Block selbst ist unsichtbar; was man sieht, entsteht hier: unten der Sockel, oben der
 * Kopf - und solange das Feld steht, dazwischen die <b>Drahtgitterkugel</b>. Ihre Feinheit waechst
 * mit dem Radius ({@code 16 + radius/8} Segmente), ihre Farbe kommt aus dem Kern: gruen im
 * Normalbetrieb, kurz rot, wenn ein schwerer Treffer eingeschlagen ist.</p>
 *
 * <p>Der Kopf dreht sich nur, solange das Feld laeuft - im Original haengt die Drehung an derselben
 * Bedingung wie die Kugel und ist damit die Anzeige, ob die Anlage arbeitet.</p>
 */
public class ForceFieldRenderer implements BlockEntityRenderer<ForceFieldBlockEntity> {

    private static final String MODEL_BASE = "models/forcefield/forcefield_base.obj";
    private static final String MODEL_TOP = "models/forcefield/forcefield_top.obj";
    private static final String TEX_BASE = "block/machine/forcefield_base";
    private static final String TEX_TOP = "block/machine/forcefield_top";

    public ForceFieldRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(ForceFieldBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light, int overlay) {

        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(180F));

        drawObj(ps, buffer, MODEL_BASE, TEX_BASE, light, overlay);

        ps.translate(0D, 0.5D, 0D);

        boolean fieldUp = be.isFieldOn() && be.getEnergyStored() > 0;

        if (fieldUp) {
            int segments = (int) (16 + be.getRadius() * 0.125F);
            drawSphere(ps, buffer, segments, segments * 2, be.getRadius(), be.getColor());

            // Original: der Kopf dreht sich mit der Systemzeit, aber nur bei stehendem Feld.
            double rot = (System.currentTimeMillis() / 10D) % 360D;
            ps.mulPose(Axis.YP.rotationDegrees((float) -rot));
        }

        ps.translate(0D, 0.5D, 0D);
        drawObj(ps, buffer, MODEL_TOP, TEX_TOP, light, overlay);

        ps.popPose();
    }

    private static void drawObj(PoseStack ps, MultiBufferSource buffer, String model, String texture,
                                int light, int overlay) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(model);
        if (obj.isEmpty()) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, texture);
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());

        for (List<float[]> mesh : obj.values()) {
            RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), mesh, sprite,
                    1F, 1F, 1F, light, overlay);
        }
    }

    /**
     * 1:1-Port von {@code generateSphere} und {@code generateSphere2}: erst die Laengengrade, dann
     * die Breitengrade - zusammen ergibt das das Gitternetz.
     */
    private static void drawSphere(PoseStack ps, MultiBufferSource buffer, int l, int s, float rad, int hex) {
        if (l <= 0 || s <= 0) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        Matrix4f m = ps.last().pose();

        float r = ((hex >> 16) & 0xFF) / 255F;
        float g = ((hex >> 8) & 0xFF) / 255F;
        float b = (hex & 0xFF) / 255F;

        float sRotDeg = 360F / s;
        float lRot = (float) Math.PI / l;

        // Laengengrade: je Segment eine Halbkreislinie, um die Y-Achse weitergedreht.
        for (int k = 0; k < s; k++) {
            float yaw = (float) Math.toRadians(sRotDeg * (k + 1));
            Vec3 vec = new Vec3(0, rad, 0);

            for (int i = 0; i < l; i++) {
                Vec3 next = rotateX(vec, lRot);
                line(vc, m, rotateY(vec, yaw), rotateY(next, yaw), r, g, b);
                vec = next;
            }
        }

        // Breitengrade: je Hoehenring eine geschlossene Linie.
        float sRot = (float) Math.PI * 2F / s;
        Vec3 ring = new Vec3(0, rad, 0);

        for (int k = 0; k < l; k++) {
            ring = rotateZ(ring, lRot);
            Vec3 vec = ring;

            for (int i = 0; i < s; i++) {
                Vec3 next = rotateY(vec, sRot);
                line(vc, m, vec, next, r, g, b);
                vec = next;
            }
        }
    }

    private static void line(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b,
                             float r, float g, float bl) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        float nx = len > 1.0E-6D ? (float) (dir.x / len) : 0F;
        float ny = len > 1.0E-6D ? (float) (dir.y / len) : 1F;
        float nz = len > 1.0E-6D ? (float) (dir.z / len) : 0F;

        //? if < 1.21.1 {
        vc.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, 1F).normal(nx, ny, nz).endVertex();
        vc.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, 1F).normal(nx, ny, nz).endVertex();
        //?} else {
        /*vc.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, 1F).setNormal(nx, ny, nz);
        vc.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, 1F).setNormal(nx, ny, nz);
        *///?}
    }

    private static Vec3 rotateX(Vec3 v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
    }

    private static Vec3 rotateY(Vec3 v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s);
    }

    private static Vec3 rotateZ(Vec3 v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z);
    }

    @Override
    public boolean shouldRenderOffScreen(ForceFieldBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
