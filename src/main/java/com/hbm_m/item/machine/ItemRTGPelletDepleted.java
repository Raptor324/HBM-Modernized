package com.hbm_m.item.machine;

import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRTGPelletDepleted} (je Meta ein Gegenstand): beim Verarbeiten bleibt wie im Original eine Eisenplatte zurueck ({@code setContainerItem(plate_iron)}). */
public class ItemRTGPelletDepleted extends Item {

    public ItemRTGPelletDepleted(Properties props) {
        super(props);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        Item plate = ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE);
        return plate != null ? new ItemStack(plate) : ItemStack.EMPTY;
    }
}
