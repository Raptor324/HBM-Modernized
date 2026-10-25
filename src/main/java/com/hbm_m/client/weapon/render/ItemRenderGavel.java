package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.render.item.weapon.ItemRenderGavel}: 3D-Richterhammer in der Hand und am Boden
 * (Inventar bleibt das flache Symbol, siehe Itemmodell mit {@code forge:separate_transforms}).
 */
public class ItemRenderGavel extends ItemRenderWeaponBase {

    public static final SimpleObjModel gavel = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/weapons/gavel.obj"));
    public static final ResourceLocation gavel_wood = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/gavel_wood.png");
    public static final ResourceLocation gavel_lead = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/gavel_lead.png");
    public static final ResourceLocation gavel_diamond = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/gavel_diamond.png");
    public static final ResourceLocation gavel_mese = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/gavel_mese.png");

    @Override public boolean customFirstPerson() { return false; }

    @Override public void renderFirstPerson(ItemStack stack) { }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GunGL.pushMatrix();
        GunGL.enableCull();

        Player player = Minecraft.getInstance().player;
        Item it = item.getItem();
        boolean mese = it == ModItems.MESE_GAVEL.get();

        if (it == ModItems.WOOD_GAVEL.get()) GunGL.bindTexture(gavel_wood);
        if (it == ModItems.LEAD_GAVEL.get()) GunGL.bindTexture(gavel_lead);
        if (it == ModItems.DIAMOND_GAVEL.get()) GunGL.bindTexture(gavel_diamond);
        if (mese) GunGL.bindTexture(gavel_mese);

        switch (type) {
            case EQUIPPED_FIRST_PERSON -> {
                GunGL.translate(1, 0.5, 0);
                if (player != null && player.isBlocking()) {
                    GunGL.translate(-0.5, 0, 0);
                }
                GunGL.rotate(45, 0, 0, 1);
                GunGL.rotate(90, 0, 1, 0);
                if (mese) GunGL.scale(2, 2, 2);
            }
            case ENTITY, EQUIPPED -> {
                if (type == ItemRenderType.ENTITY) GunGL.translate(-0.5, 0, 0);
                GunGL.scale(0.5, 0.5, 0.5);
                GunGL.rotate(45, 0, 0, 1);
                GunGL.translate(1.375, 0, 0);
                GunGL.rotate(90, 0, 1, 0);
                if (mese) {
                    GunGL.scale(2, 2, 2);
                    GunGL.translate(0, 0.25, 0);
                }
            }
            default -> {
                GunGL.popMatrix();
                return;
            }
        }

        GunGL.renderAll(gavel);
        GunGL.popMatrix();
    }
}
