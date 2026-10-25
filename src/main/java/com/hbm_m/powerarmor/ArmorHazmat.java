package com.hbm_m.powerarmor;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.powerarmor.overlay.FSBHelmetOverlay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.armor.ArmorHazmat}: Schutzanzug; die Hauben hazmat_helmet/hazmat_paa_helmet trueben die Sicht. */
public class ArmorHazmat extends ModArmorFSB {

    private final ResourceLocation hazmatBlur = ResourceLocation.tryParse("hbm_m:textures/misc/overlay_hazmat.png");

    public ArmorHazmat(ModArmorMaterials material, Type type, Properties properties, String texture) {
        super(material, type, properties, texture);
    }

    @Override
    public void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTicks) {
        if (this != ModItems.HAZMAT_HELMET.get() && this != ModItems.HAZMAT_PAA_HELMET.get())
            return;
        FSBHelmetOverlay.render(hazmatBlur, width, height);
    }
}
