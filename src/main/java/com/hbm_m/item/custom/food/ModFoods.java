package com.hbm_m.item.custom.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoods {
    public static final FoodProperties STRAWBERRY = new FoodProperties.Builder().nutrition(2).fast()
            .saturationMod(0.2f).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200), 0.1f).build();

    public static final FoodProperties CANNED_ASBESTOS = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.1F)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 100), 0.8F) // токсичное
            .build();

    public static final FoodProperties CANNED_ASS = new FoodProperties.Builder()
            .nutrition(2).saturationMod(0.2F)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60), 0.5F) // замедление
            .build();

    public static final FoodProperties CANNED_BARK = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.05F) // мало пользы, жёсткое
            .build();

    public static final FoodProperties CANNED_BEEF = new FoodProperties.Builder()
            .nutrition(8).saturationMod(0.8F).meat()
            .build();

    public static final FoodProperties CANNED_BHOLE = new FoodProperties.Builder()
            .nutrition(4).saturationMod(0.4F)
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200), 0.3F) // устойчивость к урону
            .build();

    public static final FoodProperties CANNED_CHEESE = new FoodProperties.Builder()
            .nutrition(5).saturationMod(0.6F)
            .build();

    public static final FoodProperties CANNED_CHINESE = new FoodProperties.Builder()
            .nutrition(6).saturationMod(0.5F)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100), 0.3F) // легкий ускоритель
            .build();

    public static final FoodProperties CANNED_DIESEL = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.1F)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 150), 1.0F) // очень опасно
            .build();

    public static final FoodProperties CANNED_FIST = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.3F)
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100), 0.4F) // усиление урона
            .build();

    public static final FoodProperties CANNED_FRIED = new FoodProperties.Builder()
            .nutrition(6).saturationMod(0.5F)
            .build();

    public static final FoodProperties CANNED_HOTDOGS = new FoodProperties.Builder()
            .nutrition(7).saturationMod(0.7F).meat()
            .build();

    public static final FoodProperties CANNED_JIZZ = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.05F)
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 120), 0.7F) // дезориентация
            .build();

    public static final FoodProperties CANNED_KEROSENE = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.1F)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 200), 1.0F)
            .build();

    public static final FoodProperties CANNED_LEFTOVERS = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.2F)
            .build();

    public static final FoodProperties CANNED_MILK = new FoodProperties.Builder()
            .nutrition(4).saturationMod(0.6F)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100), 0.4F)
            .build();

    public static final FoodProperties CANNED_MYSTERY = new FoodProperties.Builder()
            .nutrition(5).saturationMod(0.5F)
            .effect(() -> new MobEffectInstance(MobEffects.INVISIBILITY, 80), 0.2F) // неожиданный эффект
            .build();

    public static final FoodProperties CANNED_NAPALM = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.1F)
            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60), 1.0F)
            .build();

    public static final FoodProperties CANNED_OIL = new FoodProperties.Builder()
            .nutrition(1).saturationMod(0.05F)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 180), 1.0F)
            .build();

    public static final FoodProperties CANNED_PASHTET = new FoodProperties.Builder()
            .nutrition(6).saturationMod(0.7F)
            .build();

    public static final FoodProperties CANNED_PIZZA = new FoodProperties.Builder()
            .nutrition(8).saturationMod(0.8F)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120), 0.3F)
            .build();

    public static final FoodProperties CANNED_RECURSION = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.3F)
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 140), 0.7F)
            .build();

    public static final FoodProperties CANNED_SPAM = new FoodProperties.Builder()
            .nutrition(5).saturationMod(0.5F)
            .build();

    public static final FoodProperties CANNED_STEW = new FoodProperties.Builder()
            .nutrition(7).saturationMod(0.7F)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 80), 0.2F)
            .build();

    public static final FoodProperties CANNED_TOMATO = new FoodProperties.Builder()
            .nutrition(4).saturationMod(0.4F)
            .build();

    public static final FoodProperties CANNED_TUNA = new FoodProperties.Builder()
            .nutrition(7).saturationMod(0.7F).meat()
            .build();

    public static final FoodProperties CANNED_TUBE = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.3F)
            .build();

    public static final FoodProperties CANNED_YOGURT = new FoodProperties.Builder()
            .nutrition(5).saturationMod(0.6F)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100), 0.3F)
            .build();

}
