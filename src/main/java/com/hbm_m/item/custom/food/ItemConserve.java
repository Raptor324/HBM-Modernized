package com.hbm_m.item.custom.food;// Для того чтобы после употребления напитка в инвентаре появлялась пустая банка,
// нужно создать кастомный класс Item для напитков с переопределением метода finishUsingItem

import com.hbm_m.item.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemConserve extends Item {

    public ItemConserve(Properties properties) {
        super(properties);
    }

    // Вместо вызова метода isCreative() у LivingEntity, нужно проверить является ли entity игроком и вызвать isCreative у Player

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, world, entity);
        if (entity instanceof Player player && !player.isCreative()) {
            ItemStack emptyCan = new ItemStack(ModItems.CAN_KEY.get());
            if (stack.isEmpty()) {
                return emptyCan;
            } else {
                if (!player.getInventory().add(emptyCan)) {
                    player.drop(emptyCan, false);
                }
            }
        }
        return result;
    }
}
