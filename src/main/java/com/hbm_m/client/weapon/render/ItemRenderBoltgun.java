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
 * 1:1 {@code com.hbm.render.item.ItemRenderBoltgun}: Nietpistole, in erster Person faehrt der Lauf mit der
 * {@code RECOIL}-Animation zurueck (waehrenddessen kein Vanilla-Ausholen).
 */
public class ItemRenderBoltgun extends ItemRenderWeaponBase {

    public static final SimpleObjModel boltgun = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/weapons/boltgun.obj"));
    public static final ResourceLocation boltgun_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/boltgun.png");

    @Override public boolean customFirstPerson() { return false; }

    @Override
    public void renderFirstPerson(ItemStack stack) { }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GunGL.pushMatrix();

        Player player = Minecraft.getInstance().player;

        GunGL.enableCull();
        GunGL.bindTexture(boltgun_tex);

        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {
                double s0 = 0.15D;
                GunGL.translate(0.5, 0.35, -0.25F);
                GunGL.rotate(15, 0, 0, 1);
                GunGL.rotate(80, 0, 1, 0);
                GunGL.scale(s0, s0, s0);

                GunGL.pushMatrix();
                double[] anim = HbmAnimations.getRelevantTransformation("RECOIL");
                GunGL.translate(0, 0, -anim[0]);
                if (anim[0] != 0 && player != null) player.swinging = false;
                GunGL.renderPart(boltgun, "Barrel");
                GunGL.popMatrix();
            }

            case EQUIPPED -> {
                double scale = 0.25D;
                GunGL.scale(scale, scale, scale);
                GunGL.rotate(10, 0, 1, 0);
                GunGL.rotate(10, 0, 0, 1);
                GunGL.rotate(10, 1, 0, 0);
                GunGL.translate(1.5, -0.25, 1);
            }

            case ENTITY -> {
                double s1 = 0.1D;
                GunGL.scale(s1, s1, s1);
            }

            case INVENTORY -> {
                double s = 1.75D;
                GunGL.translate(7, 10, 0);
                GunGL.rotate(-90, 0, 1, 0);
                GunGL.rotate(-135, 1, 0, 0);
                GunGL.scale(s, s, -s);
            }

            default -> { }
        }

        GunGL.renderPart(boltgun, "Gun");
        if (type != ItemRenderType.EQUIPPED_FIRST_PERSON) {
            GunGL.renderPart(boltgun, "Barrel");
        }

        GunGL.popMatrix();
    }
}
