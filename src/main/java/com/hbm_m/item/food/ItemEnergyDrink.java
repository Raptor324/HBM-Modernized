package com.hbm_m.item.food;// Для того чтобы после употребления напитка в инвентаре появлялась пустая банка,
// нужно создать кастомный класс Item для напитков с переопределением метода finishUsingItem

import com.hbm_m.item.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class ItemEnergyDrink extends Item {




    private static FoodProperties.Builder makeAlwaysEat(FoodProperties.Builder builder) {
        //? if < 1.21.1 {
        return builder.alwaysEat();
        //?} else {
        /*return builder.alwaysEdible();
        *///?}
    }

    public static final FoodProperties CAN_BEPIS = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(4, 0.4F)),
            MobEffects.MOVEMENT_SPEED, 450, 1F).build();

    public static final FoodProperties CAN_BREEN = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(3, 0.3F)),
            MobEffects.DAMAGE_RESISTANCE, 300, 1F).build();

    public static final FoodProperties CAN_CREATURE = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            com.hbm_m.platform.PlatformHooks.addFoodEffect(
                    makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(5, 0.5F)),
                    MobEffects.REGENERATION, 225, 1F),
            MobEffects.ABSORPTION, 225, 1F).build();

    public static final FoodProperties CAN_LUNA = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(6, 0.6F)),
            MobEffects.NIGHT_VISION, 225, 1F).build();

    public static final FoodProperties CAN_MRSUGAR = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(4, 0.3F)),
            MobEffects.MOVEMENT_SPEED, 180, 1F).build();

    public static final FoodProperties CAN_MUG = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(3, 0.35F)),
            MobEffects.DIG_SPEED, 225, 1F).build();

    public static final FoodProperties CAN_OVERCHARGE = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(7, 0.7F)),
            MobEffects.DAMAGE_BOOST, 180, 1F).build();

    public static final FoodProperties CAN_REDBOMB = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(5, 0.5F)),
            MobEffects.FIRE_RESISTANCE, 225, 1F).build();

    public static final FoodProperties CAN_SMART = com.hbm_m.platform.PlatformHooks.addFoodEffect(
            makeAlwaysEat(com.hbm_m.platform.PlatformHooks.foodBuilder(4, 0.35F)),
            MobEffects.JUMP, 270, 1F).build();

    public ItemEnergyDrink(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK; // Use potion drinking animation
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK; // Potion drinking sound
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, world, entity);
        if (entity instanceof Player player && !player.isCreative()) {
            ItemStack emptyCan = new ItemStack(ModItems.CAN_EMPTY.get());
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
