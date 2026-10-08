package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.item.tool.ItemToolAbilityFueled;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.render.item.weapon.ItemRenderChainsaw}: Saegenkoerper plus 20 umlaufende Zaehne (laufen nur
 * mit Treibstoff), erste Person mit Schwung-Animation ({@code SWING_ROT}/{@code SWING_TRANS}) statt Vanilla-Ausholen.
 */
public class ItemRenderChainsaw extends ItemRenderWeaponBase {

    public static final SimpleObjModel chainsaw = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/weapons/chainsaw.obj"));
    public static final ResourceLocation chainsaw_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/chainsaw.png");

    @Override public boolean customFirstPerson() { return false; }

    @Override
    public void renderFirstPerson(ItemStack stack) { }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GunGL.pushMatrix();

        Player player = Minecraft.getInstance().player;

        GunGL.enableCull();
        GunGL.bindTexture(chainsaw_tex);

        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {

                if (player != null) player.swinging = false;

                double s0 = 0.35D;
                GunGL.translate(0.5, 0.25, -0.25F);
                GunGL.rotate(45, 0, 0, 1);
                GunGL.rotate(80, 0, 1, 0);
                GunGL.scale(s0, s0, s0);

                if (player == null || !player.isBlocking()) {
                    double[] sRot = HbmAnimations.getRelevantTransformation("SWING_ROT");
                    double[] sTrans = HbmAnimations.getRelevantTransformation("SWING_TRANS");
                    GunGL.translate(sTrans[0], sTrans[1], sTrans[2]);
                    GunGL.rotate(sRot[2], 0, 0, 1);
                    GunGL.rotate(sRot[1], 0, 1, 0);
                    GunGL.rotate(sRot[0], 1, 0, 0);
                }
            }

            case EQUIPPED -> {
                double scale = -0.375D;
                GunGL.scale(scale, scale, scale);
                GunGL.rotate(85, 0, 1, 0);
                GunGL.rotate(135D, 1.0D, 0.0D, 0.0D);
                GunGL.translate(-0.125, -2.0, 1.75);
            }

            case ENTITY -> {
                double s1 = 0.5D;
                GunGL.scale(s1, s1, s1);
            }

            case INVENTORY -> {
                double s = 4D;
                GunGL.translate(8, 10, 0);
                GunGL.rotate(-90, 0, 1, 0);
                GunGL.rotate(-135, 1, 0, 0);
                GunGL.scale(s, s, -s);
            }

            default -> { }
        }

        GunGL.renderPart(chainsaw, "Saw");

        boolean canOperate = item.getItem() instanceof ItemToolAbilityFueled fueled && fueled.canOperate(item);

        for (int i = 0; i < 20; i++) {

            double run = canOperate ? System.currentTimeMillis() % 100D * 0.25D / 100D : 0.0625D;
            double forward = i * 0.25 + (run) - 2.0625;

            GunGL.pushMatrix();

            GunGL.translate(0, 0, 1.9375);
            GunGL.translate(0, 0.375, 0.5625);
            double angle = Mth.clamp(forward, 0, 0.25 * Math.PI);
            GunGL.rotate(angle * 180D / (Math.PI * 0.25), 1, 0, 0);
            GunGL.translate(0, -0.375, -0.5625);
            if (forward < 0) GunGL.translate(0, 0, forward);
            if (forward > Math.PI * 0.25) GunGL.translate(0, 0, forward - Math.PI * 0.25);
            GunGL.renderPart(chainsaw, "Tooth");
            GunGL.popMatrix();
        }

        GunGL.popMatrix();
    }
}
