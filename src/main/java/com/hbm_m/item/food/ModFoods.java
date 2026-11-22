package com.hbm_m.item.food;

import com.hbm_m.platform.PlatformHooks;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import com.hbm_m.platform.ItemHooks;

public class ModFoods {
    public static final FoodProperties STRAWBERRY = ItemHooks.addFoodEffect(
            ItemHooks.foodBuilder(2, 0.2f).fast(), 
            MobEffects.MOVEMENT_SPEED, 200, 0.1f)
            .build();

    // Die Konserven (canned_*) nutzen 1:1 ItemConserve.EnumFoodType (Hunger/Saettigung des Originals);
    // die frueher hier stehenden, frei erfundenen CANNED_*-FoodProperties waren unbenutzt und sind entfernt.
}