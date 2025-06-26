// src/main/java/com/hbm_m/item/ModToolTiers.java
package com.hbm_m.item;

import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.Item; // Добавлен импорт для Item
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
//import net.minecraft.tags.TagKey; // Добавлен импорт для TagKey
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.util.Lazy; // Добавлен импорт для Lazy

import java.util.List;

public class ModToolTiers {
    public static final Tier ALLOY = TierSortingRegistry.registerTier(
            new ForgeTier(
                    5, // Уровень копания (dig level) - Netherite level
                    1000, // Прочность (durability)
                    20.0f, // Скорость копания (mining speed)
                    20.0f, // Урон атаки (attack damage bonus)
                    25, // Зачаровываемость (enchantment value)
                    // !!! ИСПРАВЛЕННЫЕ ПАРАМЕТРЫ !!!
                    // Первый параметр: TagKey<Item> для ингредиента починки. Если нет, используем null или пустой тэг.
                    // Для простоты, пока что не будем использовать тэг для починки.
                    // Если хочешь, чтобы починка была невозможна, можно использовать Tiers.NETHERITE.getRepairIngredient().getTag()
                    null, // Пока нет тэга для починки
                    // Второй параметр: Lazy<Ingredient> для ингредиента починки
                    // Используем Lazy.of(() -> Ingredient.of()) если нет конкретного ингредиента
                    Lazy.of(() -> Ingredient.of()) // Нет конкретного ингредиента для починки
            ),
            // Это параметры для TierSortingRegistry.registerTier
            // Первый: ResourceLocation идентификатор твоего уровня
            ResourceLocation.fromNamespaceAndPath("hbm_m", "alloy"), // ИСПРАВЛЕНО: использование fromNamespaceAndPath
            // Второй: список уровней, выше которых находится твой уровень
            List.of(Tiers.NETHERITE),
            // Третий: список уровней, ниже которых находится твой уровень (обычно пустой)
            List.of()
    );
}