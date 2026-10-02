package com.hbm_m.client.render.util;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code com.hbm.render.util.HorsePronter}: Pferdemodell ({@code models/mobs/horse.obj}) mit posierbaren Teilen
 * (Drehung um Y, X, Z um feste Gelenkpunkte). Der Aufrufer bindet die Textur ueber den uebergebenen Puffer
 * (Original: Culling aus).
 */
public final class HorsePronter {

    public static final SimpleObjModel HORSE = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/mobs/horse.obj"));
    public static final ResourceLocation TEX_DEMOHORSE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/horse/horse_demo.png");

    public static final int id_head = 0;
    public static final int id_lfl = 1;
    public static final int id_rfl = 2;
    public static final int id_lbl = 3;
    public static final int id_rbl = 4;
    public static final int id_tail = 5;
    public static final int id_body = 6;
    public static final int id_position = 7;

    private static final double[][] pose = new double[8][3];
    private static final double[][] offsets = {
            { 0, 1.125, 0.375 },    // head
            { 0.125, 0.75, 0.3125 }, // left front leg
            { -0.125, 0.75, 0.3125 }, // right front leg
            { 0.125, 0.75, -0.25 },  // left back leg
            { -0.125, 0.75, -0.25 }, // right back leg
            { 0, 1.125, -0.4375 },   // tail
            { 0, 0, 0 },             // body
            { 0, 0, 0 }              // body offset
    };

    private static boolean wings = false;
    private static boolean horn = false;
    private static boolean maleSnoot = false;

    private HorsePronter() {}

    public static void reset() {
        wings = false;
        horn = false;
        for (double[] angles : pose) { angles[0] = 0; angles[1] = 0; angles[2] = 0; }
    }

    public static void enableHorn() { horn = true; }
    public static void enableWings() { wings = true; }
    public static void setMaleSnoot() { maleSnoot = true; }

    public static void setAlicorn() {
        enableHorn();
        enableWings();
    }

    public static void poseStandardSit() {
        double r = 60;
        pose(id_body, 0, -r, 0);
        pose(id_tail, 0, 45, 90);
        pose(id_lbl, 0, -90 + r, 35);
        pose(id_rbl, 0, -90 + r, -35);
        pose(id_lfl, 0, r - 10, 5);
        pose(id_rfl, 0, r - 10, -5);
        pose(id_head, 0, r, 0);
    }

    public static void pose(int id, double yaw, double pitch, double roll) {
        pose[id][0] = yaw;
        pose[id][1] = pitch;
        pose[id][2] = roll;
    }

    public static void pront(PoseStack ps, VertexConsumer vc, int light) {
        ps.pushPose();
        doTransforms(ps, id_body);

        HORSE.renderPart("Body", ps, vc, light);

        if (horn) {
            renderWithTransform(ps, vc, light, id_head, "Head", "Mane", maleSnoot ? "NoseMale" : "NoseFemale", "HornPointy");
        } else {
            renderWithTransform(ps, vc, light, id_head, "Head", "Mane", maleSnoot ? "NoseMale" : "NoseFemale");
        }

        renderWithTransform(ps, vc, light, id_lfl, "LeftFrontLeg");
        renderWithTransform(ps, vc, light, id_rfl, "RightFrontLeg");
        renderWithTransform(ps, vc, light, id_lbl, "LeftBackLeg");
        renderWithTransform(ps, vc, light, id_rbl, "RightBackLeg");
        renderWithTransform(ps, vc, light, id_tail, "Tail");

        if (wings) {
            HORSE.renderPart("LeftWing", ps, vc, light);
            HORSE.renderPart("RightWing", ps, vc, light);
        }
        ps.popPose();
    }

    private static void doTransforms(PoseStack ps, int id) {
        double[] rotation = pose[id];
        double[] offset = offsets[id];
        ps.translate(offset[0], offset[1], offset[2]);
        ps.mulPose(Axis.YP.rotationDegrees((float) rotation[0]));
        ps.mulPose(Axis.XP.rotationDegrees((float) rotation[1]));
        ps.mulPose(Axis.ZP.rotationDegrees((float) rotation[2]));
        ps.translate(-offset[0], -offset[1], -offset[2]);
    }

    private static void renderWithTransform(PoseStack ps, VertexConsumer vc, int light, int id, String... parts) {
        ps.pushPose();
        doTransforms(ps, id);
        for (String part : parts) HORSE.renderPart(part, ps, vc, light);
        ps.popPose();
    }
}
