package com.hbm_m.item.special;

import java.util.List;

import com.hbm_m.item.LoreTooltipItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.items.special.ItemNuclearWaste}: fallengelassen wird der Stapel zum
 * {@code EntityItemWaste} - unverwundbar (kein Feuer, keine Lava, keine Explosion) und ohne
 * Verschwinde-Timer ({@code getEntityLifespan = Integer.MAX_VALUE}). Im Port setzt der erste
 * Item-Tick beides am normalen {@link ItemEntity}. Traeger: nuclear_waste(_tiny/_vitrified),
 * trinitite, die Langzeit-Abfaelle ({@code ItemWasteLong}) und abgebrannte Brennstoffe
 * ({@code ItemDepletedFuel}).
 */
public class ItemNuclearWaste extends LoreTooltipItem {

    public ItemNuclearWaste(Properties properties) {
        this(List.of(), properties);
    }

    public ItemNuclearWaste(List<Component> lore, Properties properties) {
        super(lore, properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.isInvulnerable()) {
            entity.setInvulnerable(true);
            //? if forge {
            entity.lifespan = Integer.MAX_VALUE;
            //?}
        }
        return false;
    }
}
