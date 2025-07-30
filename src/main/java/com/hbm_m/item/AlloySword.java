package com.hbm_m.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;

public class AlloySword extends SwordItem {
    public AlloySword() {
        // ModToolTiers.ALLOY - наш пользовательский уровень материала
        // new Item.Properties() - базовые свойства предмета
        // Примечание: в Minecraft 1.20.1 творческие вкладки регистрируются иначе, не через .tab()
        // .stacksTo(1) - меч не складывается в стаки (можно добавить)
        // .fireResistant() - делает предмет огнестойким (по желанию)
        super(ModToolTiers.ALLOY, 0, -2.5f, new Item.Properties()); // -2.5f для скорости атаки 1.5
        // Первый параметр (Attack Damage) в конструкторе SwordItem уже не используется для определения урона
        // с использованием ForgeTier, урон берется из ModToolTiers.ALLOY.
        // Однако, Minecraft все равно требует его для совместимости, можно поставить 0.
    }
}
