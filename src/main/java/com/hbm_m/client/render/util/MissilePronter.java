package com.hbm_m.client.render.util;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.item.missile.ItemCustomMissilePart;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartType;
import com.hbm_m.item.missile.MissilePartItems;
import com.hbm_m.item.missile.MissileStruct;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code MissilePronter} + {@code MissilePart}: zeichnet eine Baukasten-Rakete von unten nach oben - Triebwerk,
 * dann Leitwerk und Rumpf auf derselben Hoehe, darueber der Sprengkopf. Jede Stufe rueckt um ihre Hoehe nach oben.
 */
public final class MissilePronter {

    private MissilePronter() { }

    private static final Map<String, SimpleObjModel> MODELS = new HashMap<>();

    private static SimpleObjModel model(String path) {
        return MODELS.computeIfAbsent(path, p -> new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p)));
    }

    @Nullable
    public static MissilePartItems.RenderInfo info(@Nullable ItemCustomMissilePart part, PartType expected) {
        if (part == null) return null;
        MissilePartItems.RenderInfo info = MissilePartItems.render(part);
        return info != null && info.type() == expected ? info : null;
    }

    /** Einzelnes Teil (Montage, Item-Vorschau). */
    public static void prontPart(MissilePartItems.RenderInfo info, PoseStack ps, MultiBufferSource buffers, int light) {
        model(info.model()).renderAll(ps, buffers.getBuffer(RenderType.entityCutout(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, info.texture()))), light);
    }

    public static void prontMissile(MissileStruct missile, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();

        MissilePartItems.RenderInfo thruster = info(missile.thruster, PartType.THRUSTER);
        if (thruster != null) {
            prontPart(thruster, ps, buffers, light);
            ps.translate(0, thruster.height(), 0);
        }

        MissilePartItems.RenderInfo fuselage = info(missile.fuselage, PartType.FUSELAGE);
        if (fuselage != null) {
            MissilePartItems.RenderInfo fins = info(missile.fins, PartType.FINS);
            if (fins != null) prontPart(fins, ps, buffers, light);

            prontPart(fuselage, ps, buffers, light);
            ps.translate(0, fuselage.height(), 0);
        }

        MissilePartItems.RenderInfo warhead = info(missile.warhead, PartType.WARHEAD);
        if (warhead != null) prontPart(warhead, ps, buffers, light);

        ps.popPose();
    }

    /** Gesamthoehe (fuer GUI-Skalierung): Summe der GUI-Hoehen. */
    public static double guiHeight(MissileStruct missile) {
        double h = 0;
        MissilePartItems.RenderInfo t = info(missile.thruster, PartType.THRUSTER);
        MissilePartItems.RenderInfo f = info(missile.fuselage, PartType.FUSELAGE);
        MissilePartItems.RenderInfo w = info(missile.warhead, PartType.WARHEAD);
        if (t != null) h += t.height();
        if (f != null) h += f.height();
        if (w != null) h += w.height();
        return h;
    }
}
