package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.factory.XFactoryTool;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.render.item.weapon.ItemRenderFireExt}: klassischer IItemRenderer ohne eigene Erstperson-Projektion. */
public class ItemRenderFireExt extends ItemRenderWeaponBase {

    public ItemRenderFireExt() { }

    @Override public boolean customFirstPerson() { return false; }

    @Override
    @SuppressWarnings("rawtypes")
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {

        GunGL.pushMatrix();

        GunGL.enableCull();

        ItemGunBaseNT gun = (ItemGunBaseNT) item.getItem();
        IMagazine mag = gun.getConfig(item, 0).getReceivers(item)[0].getMagazine(item);
        ResourceLocation tex = WeaponResources.fireext_tex;
        if (mag.getType(item, null) == XFactoryTool.fext_foam) tex = WeaponResources.fireext_foam_tex;
        if (mag.getType(item, null) == XFactoryTool.fext_sand) tex = WeaponResources.fireext_sand_tex;
        GunGL.bindTexture(tex);

        switch (type) {

            case EQUIPPED_FIRST_PERSON -> {
                double s0 = 0.35D;
                GunGL.rotate(25, 0, 0, 1);
                GunGL.translate(0.5, -0.5, -0.5F);
                GunGL.rotate(80, 0, 1, 0);
                GunGL.scale(s0, s0, s0);
            }

            case EQUIPPED -> {
                double scale = 0.5D;
                GunGL.scale(scale, scale, scale);
                GunGL.rotate(20F, 0.0F, 0.0F, 1.0F);
                GunGL.rotate(-5, 0.0F, 1.0F, 1.0F);
                GunGL.rotate(10, 0.0F, 1.0F, 0.0F);
                GunGL.rotate(15F, 1.0F, 0.0F, 0.0F);
                GunGL.translate(0.75F, -2.75F, 0.5F);
            }

            case ENTITY -> {
                double s1 = 0.3D;
                GunGL.scale(s1, s1, s1);
            }

            case INVENTORY -> {
                GunGL.enableLighting();

                double s = 4.5D;
                GunGL.translate(2, 14, 0);
                GunGL.rotate(-90, 0, 1, 0);
                GunGL.rotate(-135, 1, 0, 0);
                GunGL.rotate(System.currentTimeMillis() / 10 % 360, 0, 1, 0);
                GunGL.scale(s, s, -s);
            }

            default -> { }
        }

        GunGL.renderAll(WeaponResources.fireext);

        GunGL.popMatrix();
    }

    @Override public void renderFirstPerson(ItemStack stack) { }
    @Override public void renderOther(ItemStack stack, ItemRenderType type, Object... data) { }
}
