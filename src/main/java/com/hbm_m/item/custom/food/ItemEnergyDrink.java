package com.hbm_m.item.custom.food;// Для того чтобы после употребления напитка в инвентаре появлялась пустая банка,
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




    public static final FoodProperties CAN_BEPIS = new FoodProperties.Builder()
            .nutrition(4).saturationMod(0.4F)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 450), 1F)
            .alwaysEat()
            .build();

    public static final FoodProperties CAN_BREEN = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.3F)
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300), 1F)
            .alwaysEat()
            .build();

    public static final FoodProperties CAN_CREATURE = new FoodProperties.Builder()
            .nutrition(5).saturationMod(0.5F)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 225), 1F)
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 225), 1F)
            .alwaysEat()
            .build();

    public static final FoodProperties CAN_LUNA = new FoodProperties.Builder()
            .nutrition(6).saturationMod(0.6F)
            .effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 225), 1F)
            .alwaysEat()
            .build();

    public static final FoodProperties CAN_MRSUGAR = new FoodProperties.Builder()
            .nutrition(4).saturationMod(0.3F)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 180), 1F)
            .alwaysEat()
            .build();

    public static final FoodProperties CAN_MUG = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.35F)
            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 225), 1F)
            .alwaysEat()
            .build();

    public static final FoodProperties CAN_OVERCHARGE = new FoodProperties.Builder()
            .nutrition(7).saturationMod(0.7F)
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 180), 1F)
            .alwaysEat()
            .build();

    public static final FoodProperties CAN_REDBOMB = new FoodProperties.Builder()
            .nutrition(5).saturationMod(0.5F)
            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 225), 1F)
            .alwaysEat()
            .build();

    public static final FoodProperties CAN_SMART = new FoodProperties.Builder()
            .nutrition(4).saturationMod(0.35F)
            .effect(() -> new MobEffectInstance(MobEffects.JUMP, 270), 1F)
            .alwaysEat()
            .build();

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
