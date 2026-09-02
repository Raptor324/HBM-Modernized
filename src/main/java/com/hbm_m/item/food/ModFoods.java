package com.hbm_m.item.food;

import com.hbm_m.platform.PlatformHooks;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoods {
    public static final FoodProperties STRAWBERRY = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(2, 0.2f).fast(), 
            MobEffects.MOVEMENT_SPEED, 200, 0.1f)
            .build();

    public static final FoodProperties CANNED_ASBESTOS = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(1, 0.1F),
            MobEffects.POISON, 100, 0.8F) // токсичное
            .build();

    public static final FoodProperties CANNED_ASS = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(2, 0.2F),
            MobEffects.MOVEMENT_SLOWDOWN, 60, 0.5F) // замедление
            .build();

    public static final FoodProperties CANNED_BARK = PlatformHooks.foodBuilder(1, 0.05F) // мало пользы, жёсткое
            .build();

    public static final FoodProperties CANNED_BEEF = PlatformHooks.setMeat(
            PlatformHooks.foodBuilder(8, 0.8F))
            .build();

    public static final FoodProperties CANNED_BHOLE = PlatformHooks.foodBuilder(10, 1.0F)
            .build();

    public static final FoodProperties CANNED_CHEESE = PlatformHooks.foodBuilder(5, 0.6F)
            .build();

    public static final FoodProperties CANNED_CHINESE = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(6, 0.5F),
            MobEffects.MOVEMENT_SPEED, 100, 0.3F) // легкий ускоритель
            .build();

    public static final FoodProperties CANNED_DIESEL = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(1, 0.1F),
            MobEffects.POISON, 150, 1.0F) // очень опасно
            .build();

    public static final FoodProperties CANNED_FIST = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(3, 0.3F),
            MobEffects.DAMAGE_BOOST, 100, 0.4F) // усиление урона
            .build();

    public static final FoodProperties CANNED_FRIED = PlatformHooks.foodBuilder(6, 0.5F)
            .build();

    public static final FoodProperties CANNED_HOTDOGS = PlatformHooks.setMeat(
            PlatformHooks.foodBuilder(7, 0.7F))
            .build();

    public static final FoodProperties CANNED_JIZZ = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(1, 0.05F),
            MobEffects.CONFUSION, 120, 0.7F) // дезориентация
            .build();

    public static final FoodProperties CANNED_KEROSENE = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(1, 0.1F),
            MobEffects.POISON, 200, 1.0F)
            .build();

    public static final FoodProperties CANNED_LEFTOVERS = PlatformHooks.foodBuilder(3, 0.2F)
            .build();

    public static final FoodProperties CANNED_MILK = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(4, 0.6F),
            MobEffects.REGENERATION, 100, 0.4F)
            .build();

    public static final FoodProperties CANNED_MYSTERY = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(5, 0.5F),
            MobEffects.INVISIBILITY, 80, 0.2F) // неожиданный эффект
            .build();

    public static final FoodProperties CANNED_NAPALM = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(1, 0.1F),
            MobEffects.FIRE_RESISTANCE, 60, 1.0F)
            .build();

    public static final FoodProperties CANNED_OIL = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(1, 0.05F),
            MobEffects.POISON, 180, 1.0F)
            .build();

    public static final FoodProperties CANNED_PASHTET = PlatformHooks.foodBuilder(6, 0.7F)
            .build();

    public static final FoodProperties CANNED_PIZZA = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(8, 0.8F),
            MobEffects.MOVEMENT_SPEED, 120, 0.3F)
            .build();

    public static final FoodProperties CANNED_RECURSION = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(3, 0.3F),
            MobEffects.CONFUSION, 140, 0.7F)
            .build();

    public static final FoodProperties CANNED_SPAM = PlatformHooks.foodBuilder(5, 0.5F)
            .build();

    public static final FoodProperties CANNED_STEW = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(7, 0.7F),
            MobEffects.REGENERATION, 80, 0.2F)
            .build();

    public static final FoodProperties CANNED_TOMATO = PlatformHooks.foodBuilder(4, 0.4F)
            .build();

    public static final FoodProperties CANNED_TUNA = PlatformHooks.setMeat(
            PlatformHooks.foodBuilder(7, 0.7F))
            .build();

    public static final FoodProperties CANNED_TUBE = PlatformHooks.foodBuilder(3, 0.3F)
            .build();

    public static final FoodProperties CANNED_YOGURT = PlatformHooks.addFoodEffect(
            PlatformHooks.foodBuilder(5, 0.6F),
            MobEffects.REGENERATION, 100, 0.3F)
            .build();
}