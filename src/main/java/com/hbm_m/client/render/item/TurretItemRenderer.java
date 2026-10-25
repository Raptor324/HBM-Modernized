package com.hbm_m.client.render.item;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * vJ: Gegenstandsdarstellung der Geschuetztuerme 1:1 nach {@code ItemRenderLibrary} (Chekhov, Friendly, Jeremy, Tauon,
 * Richard, Howard, Howard beschaedigt, Maxwell, Fritz, Arty) bzw. {@code RenderTurretSentry/RenderTurretHIMARS#getRenderer}:
 * Inventar = {@code ItemRenderBase}-Kette ({@link OrigInventoryTransform}) + {@code renderInventory()} + {@code renderCommon()}
 * mit Sockel, Lafette und Waffe in den Original-Texturen. Ausserhalb des Inventars dieselbe Gruppe auf Blockgroesse.
 */
public final class TurretItemRenderer {

    private TurretItemRenderer() {}

    private static final Map<String, SimpleObjModel> MODELS = new HashMap<>();

    private static SimpleObjModel obj(String name) {
        return MODELS.computeIfAbsent(name, n -> new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/turrets/" + n + ".obj")));
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/turrets/" + name + ".png");
    }

    public static boolean handles(String id) {
        return switch (id) {
            case "turret_chekhov", "turret_friendly", "turret_jeremy", "turret_tauon", "turret_richard", "turret_howard",
                 "turret_howard_damaged", "turret_maxwell", "turret_fritz", "turret_arty", "turret_himars", "turret_sentry" -> true;
            default -> false;
        };
    }

    private static void part(PoseStack ps, MultiBufferSource buf, int light, String model, String part, ResourceLocation texture) {
        obj(model).renderPart(part, ps, buf.getBuffer(RenderType.entityCutoutNoCull(texture)), light);
    }

    public static void render(String id, ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        // renderInventory(): Versatz und Massstab je Turm
        double ty;
        double tx = 0;
        double s;
        switch (id) {
            case "turret_jeremy" -> { ty = -2; s = 2.5; }
            case "turret_tauon", "turret_richard" -> { ty = -2; s = 5; }
            case "turret_howard", "turret_howard_damaged" -> { ty = -4.5; s = 4; }
            case "turret_maxwell" -> { tx = -1; ty = -3; s = 4; }
            case "turret_fritz" -> { ty = -2; s = 4; }
            case "turret_arty" -> { tx = -3; ty = -4; s = 3.5; }
            case "turret_himars" -> { ty = -2; s = 3.5; }
            case "turret_sentry" -> { ty = -4; s = 7; }
            default -> { ty = -3; s = 4; } // Chekhov, Friendly
        }
        if (ctx == ItemDisplayContext.GUI) {
            OrigInventoryTransform.apply(ps);
            ps.translate(tx, ty, 0);
            ps.scale((float) s, (float) s, (float) s);
        } else {
            // ausserhalb des Inventars: gleiche Groesse wie im Slot (16 Pixel = 1 Block), auf der Blockmitte stehend
            RBMKColumnItemRenderer.applyDisplay(stack, ctx, ps);
            ps.translate(0.5, 0, 0.5);
            float k = (float) (s / 16D);
            ps.scale(k, k, k);
            ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        renderCommon(id, ps, buf, light);
        ps.popPose();
    }

    /** renderCommon() / renderCommonWithStack() der Original-Itemrenderer */
    private static void renderCommon(String id, PoseStack ps, MultiBufferSource buf, int light) {
        switch (id) {
            case "turret_chekhov" -> {
                ps.translate(-0.75, 0, 0);
                part(ps, buf, light, "turret_chekhov", "Base", tex("base"));
                part(ps, buf, light, "turret_chekhov", "Carriage", tex("carriage"));
                part(ps, buf, light, "turret_chekhov", "Body", tex("chekhov"));
                part(ps, buf, light, "turret_chekhov", "Barrels", tex("chekhov_barrels"));
            }
            case "turret_friendly" -> {
                ps.translate(-0.75, 0, 0);
                part(ps, buf, light, "turret_chekhov", "Base", tex("base_friendly"));
                part(ps, buf, light, "turret_chekhov", "Carriage", tex("carriage_friendly"));
                part(ps, buf, light, "turret_chekhov", "Body", tex("chekhov"));
                part(ps, buf, light, "turret_chekhov", "Barrels", tex("chekhov_barrels"));
            }
            case "turret_jeremy" -> {
                ps.translate(-0.5, 0, 0);
                part(ps, buf, light, "turret_chekhov", "Base", tex("base"));
                part(ps, buf, light, "turret_chekhov", "Carriage", tex("carriage"));
                part(ps, buf, light, "turret_jeremy", "Gun", tex("jeremy"));
            }
            case "turret_tauon" -> {
                part(ps, buf, light, "turret_chekhov", "Base", tex("base"));
                part(ps, buf, light, "turret_chekhov", "Carriage", tex("carriage"));
                part(ps, buf, light, "turret_tauon", "Cannon", tex("tauon"));
                part(ps, buf, light, "turret_tauon", "Rotor", tex("tauon"));
            }
            case "turret_richard" -> {
                part(ps, buf, light, "turret_chekhov", "Base", tex("base"));
                part(ps, buf, light, "turret_chekhov", "Carriage", tex("carriage"));
                part(ps, buf, light, "turret_richard", "Launcher", tex("richard"));
            }
            case "turret_howard" -> {
                ps.translate(-0.75, 0, 0);
                part(ps, buf, light, "turret_chekhov", "Base", tex("base"));
                part(ps, buf, light, "turret_howard", "Carriage", tex("carriage_ciws"));
                part(ps, buf, light, "turret_howard", "Body", tex("howard"));
                part(ps, buf, light, "turret_howard", "BarrelsTop", tex("howard_barrels"));
                part(ps, buf, light, "turret_howard", "BarrelsBottom", tex("howard_barrels"));
            }
            case "turret_howard_damaged" -> {
                ps.translate(-0.75, 0, 0);
                part(ps, buf, light, "turret_chekhov", "Base", tex("rusted_base"));
                part(ps, buf, light, "turret_howard", "Carriage", tex("rusted_carriage_ciws"));
                part(ps, buf, light, "turret_howard_damaged", "Body", tex("rusted_howard"));
                part(ps, buf, light, "turret_howard_damaged", "BarrelsTop", tex("rusted_howard_barrels"));
                part(ps, buf, light, "turret_howard_damaged", "BarrelsBottom", tex("rusted_howard_barrels"));
            }
            case "turret_maxwell" -> {
                part(ps, buf, light, "turret_chekhov", "Base", tex("base"));
                part(ps, buf, light, "turret_howard", "Carriage", tex("carriage_ciws"));
                part(ps, buf, light, "turret_microwave", "Microwave", tex("maxwell"));
            }
            case "turret_fritz" -> {
                part(ps, buf, light, "turret_chekhov", "Base", tex("base"));
                part(ps, buf, light, "turret_chekhov", "Carriage", tex("carriage"));
                part(ps, buf, light, "turret_fritz", "Gun", tex("fritz"));
            }
            case "turret_arty" -> {
                ps.mulPose(Axis.YP.rotationDegrees(-90));
                ps.scale(0.5F, 0.5F, 0.5F);
                part(ps, buf, light, "turret_arty", "Base", tex("arty"));
                part(ps, buf, light, "turret_arty", "Carriage", tex("arty"));
                ps.translate(0, 3, 0);
                ps.mulPose(Axis.XP.rotationDegrees(45));
                ps.translate(0, -3, 0);
                part(ps, buf, light, "turret_arty", "Cannon", tex("arty"));
                part(ps, buf, light, "turret_arty", "Barrel", tex("arty"));
            }
            case "turret_himars" -> {
                ps.mulPose(Axis.YP.rotationDegrees(-90));
                ps.scale(0.5F, 0.5F, 0.5F);
                part(ps, buf, light, "turret_arty", "Base", tex("arty"));
                part(ps, buf, light, "turret_himars", "Carriage", tex("himars"));
                part(ps, buf, light, "turret_himars", "Launcher", tex("himars"));
                part(ps, buf, light, "turret_himars", "Crane", tex("himars"));
                part(ps, buf, light, "turret_himars", "TubeStandard",
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/projectiles/himars_standard.png"));
            }
            case "turret_sentry" -> {
                ps.mulPose(Axis.YP.rotationDegrees(90));
                for (String p : new String[] {"Base", "Pivot", "Body", "Drum", "BarrelL", "BarrelR"}) {
                    part(ps, buf, light, "turret_sentry", p, tex("sentry"));
                }
            }
            default -> { }
        }
    }
}
