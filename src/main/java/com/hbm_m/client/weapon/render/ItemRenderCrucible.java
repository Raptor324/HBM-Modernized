package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.render.item.weapon.ItemRenderCrucible}: Griff, zwei klappbare Parierstangen ({@code GUARD_ROT})
 * und die leuchtende Klinge (nur mit Ladung); erste Person mit Schwung-Animation ({@code SWING_ROT}/{@code SWING_TRANS}).
 * Der Original-Trick mit {@code equippedProgress} (kein Ausholen beim Wechseln) entfaellt: der Port-Renderer fuer
 * IAnimatedItem setzt keine Ausholbewegung.
 */
public class ItemRenderCrucible extends ItemRenderWeaponBase {

    public static final SimpleObjModel crucible = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/weapons/crucible.obj"));
    public static final ResourceLocation crucible_hilt = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/crucible_hilt.png");
    public static final ResourceLocation crucible_guard = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/crucible_guard.png");
    public static final ResourceLocation crucible_blade = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/crucible_blade.png");

    @Override public boolean customFirstPerson() { return false; }

    @Override
    public void renderFirstPerson(ItemStack stack) { }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GunGL.pushMatrix();

        Player player = Minecraft.getInstance().player;

        boolean isOn = item.getDamageValue() < item.getMaxDamage();

        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {

                GunGL.translate(1.5, -0.3, 0);

                boolean blocking = player != null && player.isBlocking();
                if (blocking) {
                    GunGL.translate(-0.125, -0.25, 0);
                }

                GunGL.scale(0.3, 0.3, 0.3);

                GunGL.rotate(45, 0, 0, 1);
                GunGL.rotate(90, 0, 1, 0);

                boolean isSwing = false;

                if (!blocking) {
                    double[] sRot = HbmAnimations.getRelevantTransformation("SWING_ROT");
                    double[] sTrans = HbmAnimations.getRelevantTransformation("SWING_TRANS");
                    GunGL.translate(sTrans[0], sTrans[1], sTrans[2]);
                    GunGL.rotate(sRot[0], 1, 0, 0);
                    GunGL.rotate(sRot[2], 0, 0, 1);
                    GunGL.rotate(sRot[1], 0, 1, 0);

                    if (sRot[0] != 0)
                        isSwing = true;
                }

                double[] rot = HbmAnimations.getRelevantTransformation("GUARD_ROT");
                GunGL.bindTexture(crucible_hilt);
                GunGL.renderPart(crucible, "Hilt");

                GunGL.bindTexture(crucible_guard);
                double rotGuard = rot[0];

                if (!isSwing && !isOn)
                    rotGuard = 90;

                GunGL.pushMatrix();
                GunGL.translate(0, 3, 0.5);
                GunGL.rotate(rotGuard, -1, 0, 0);
                GunGL.translate(0, -3, -0.5);
                GunGL.renderPart(crucible, "GuardLeft");
                GunGL.popMatrix();

                GunGL.pushMatrix();
                GunGL.translate(0, 3, -0.5);
                GunGL.rotate(rotGuard, 1, 0, 0);
                GunGL.translate(0, -3, 0.5);
                GunGL.renderPart(crucible, "GuardRight");
                GunGL.popMatrix();

                if (rot[2] == 0 && (isSwing || isOn)) {
                    renderBlade();
                }
            }

            case ENTITY, EQUIPPED -> {
                if (type == ItemRenderType.ENTITY) {
                    GunGL.translate(-0.75, 0.6, 0);
                    GunGL.rotate(-45, 0, 0, 1);
                }

                GunGL.rotate(45, 0, 0, 1);
                GunGL.translate(0.75, -0.4, 0);
                GunGL.rotate(90, 0, 1, 0);
                GunGL.scale(0.15, 0.15, 0.15);

                renderHiltAndGuards(isOn);

                if (isOn) {
                    renderBlade();
                }
            }

            case INVENTORY -> {
                GunGL.translate(2, 14, 0);
                GunGL.rotate(-135, 0, 0, 1);
                GunGL.rotate(90, 0, 1, 0);
                double scale = 1.5D;
                GunGL.scale(scale, scale, scale);

                renderHiltAndGuards(isOn);
                GunGL.translate(0.005, 0, 0);
                if (isOn) {
                    GunGL.bindTexture(crucible_blade);
                    GunGL.renderPart(crucible, "Blade");
                }
            }

            default -> { }
        }

        GunGL.popMatrix();
    }

    private static void renderHiltAndGuards(boolean isOn) {
        GunGL.bindTexture(crucible_hilt);
        GunGL.renderPart(crucible, "Hilt");
        GunGL.bindTexture(crucible_guard);
        GunGL.pushMatrix();
        GunGL.translate(0, 3, 0.5);
        GunGL.rotate(isOn ? 0 : 90, -1, 0, 0);
        GunGL.translate(0, -3, -0.5);
        GunGL.renderPart(crucible, "GuardLeft");
        GunGL.popMatrix();

        GunGL.pushMatrix();
        GunGL.translate(0, 3, -0.5);
        GunGL.rotate(isOn ? 0 : 90, 1, 0, 0);
        GunGL.translate(0, -3, 0.5);
        GunGL.renderPart(crucible, "GuardRight");
        GunGL.popMatrix();
    }

    /** Klinge mit voller Helligkeit und ohne Backface-Culling. */
    private static void renderBlade() {
        GunGL.pushMatrix();
        GunGL.pushAttrib();

        GunGL.disableLighting();
        GunGL.disableCull();
        GunGL.fullbright(true);
        GunGL.translate(0.005, 0, 0);
        GunGL.bindTexture(crucible_blade);
        GunGL.renderPart(crucible, "Blade");

        GunGL.popAttrib();
        GunGL.popMatrix();
    }
}
