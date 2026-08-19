package com.hbm_m.platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Платформенный и версионный слой для сглаживания различий работы с предметами,
 * зачарованиями и их прочностью между 1.20.1 и 1.21.1.
 */
public final class ItemHooks {
    private ItemHooks() {}

    /**
     * Кросс-версионное нанесение урона предмету (замена stack.hurtAndBreak).
     * В 1.20.1 принимал Consumer с броадкастом, в 1.21.1 принимает EquipmentSlot.
     */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        //? if < 1.21.1 {
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(hand));
        //?} else {
        /*stack.hurtAndBreak(amount, entity, hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        *///?}
    }

    /**
     * Кросс-версионное чтение уровня зачарования.
     * Скрывает переход от Enchantment (1.20.1) к Holder<Enchantment> и динамическому реестру (1.21.1).
     */
    public static int getEnchantmentLevel(ItemStack stack, Level level, String enchName) {
        //? if < 1.21.1 {
        net.minecraft.world.item.enchantment.Enchantment ench = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(net.minecraft.resources.ResourceLocation.tryParse(enchName));
        if (ench == null) return 0;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
        //?} else {
        /*net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var holder = registry.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, net.minecraft.resources.ResourceLocation.parse(enchName))).orElse(null);
        if (holder == null) return 0;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        *///?}
    }

    /**
     * Кросс-версионная установка уровня зачарования (обновляет или добавляет).
     * Скрывает работу с Map в 1.20.1 и ItemEnchantments.Mutable в 1.21.1.
     */
    public static void setEnchantmentLevel(ItemStack stack, Level level, String enchName, int enchLevel) {
        //? if < 1.21.1 {
        net.minecraft.world.item.enchantment.Enchantment ench = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(net.minecraft.resources.ResourceLocation.tryParse(enchName));
        if (ench != null) {
            java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> map = new java.util.HashMap<>(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack));
            map.put(ench, enchLevel);
            net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(map, stack);
        }
        //?} else {
        /*net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var holder = registry.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, net.minecraft.resources.ResourceLocation.parse(enchName))).orElse(null);
        if (holder != null) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack, m -> m.set(holder, enchLevel));
        }
        *///?}
    }

    /**
     * Кросс-версионное удаление зачарования.
     */
    public static void removeEnchantment(ItemStack stack, Level level, String enchName) {
        //? if < 1.21.1 {
        net.minecraft.world.item.enchantment.Enchantment ench = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(net.minecraft.resources.ResourceLocation.tryParse(enchName));
        if (ench != null) {
            java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> map = new java.util.HashMap<>(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack));
            map.remove(ench);
            net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(map, stack);
        }
        //?} else {
        /*net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var holder = registry.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, net.minecraft.resources.ResourceLocation.parse(enchName))).orElse(null);
        if (holder != null) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack, m -> m.set(holder, 0));
        }
        *///?}
    }

    /**
     * Кросс-версионное получение максимального размера стака для предмета (Item).
     * 1.20.1: item.getMaxStackSize()
     * 1.21.1: item.getDefaultMaxStackSize()
     */
    public static int getItemMaxStackSize(net.minecraft.world.item.Item item) {
        //? if < 1.21.1 {
        return item.getMaxStackSize();
        //?} else {
        /*return item.getDefaultMaxStackSize();
        *///?}
    }
}