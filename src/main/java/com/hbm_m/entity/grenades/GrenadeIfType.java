package com.hbm_m.entity.grenades;

import com.hbm_m.item.ModItems;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

public enum GrenadeIfType {
    // Добавили параметр урона: 50.0f (25 сердец).
    // 5.0f - сила разрушения блоков (радиус взрыва), 50.0f - урон сущностям.
    GRENADE_IF(0.3f, 5.0f, 45.0f, false, ModItems.GRENADE_IF::get),
    GRENADE_IF_HE(0.3f, 8.0f, 80.0f, false, ModItems.GRENADE_IF_HE::get),
    GRENADE_IF_SLIME(0.45f, 6.0f, 60.0f, false, ModItems.GRENADE_IF_SLIME::get),
    GRENADE_IF_FIRE(0.3f, 6.0f, 60.0f, true, ModItems.GRENADE_IF_FIRE::get);


    private final float bounceMultiplier;
    private final float explosionPower;
    private final float customDamage; // Новый параметр для урона
    private final boolean causesFire;
    private final Supplier<Item> itemSupplier;

    GrenadeIfType(float bounceMultiplier, float explosionPower, float customDamage, boolean causesFire, Supplier<Item> itemSupplier) {
        this.bounceMultiplier = bounceMultiplier;
        this.explosionPower = explosionPower;
        this.customDamage = customDamage;
        this.causesFire = causesFire;
        this.itemSupplier = itemSupplier;
    }

    public float getBounceMultiplier() {
        return bounceMultiplier;
    }

    public float getExplosionPower() {
        return explosionPower;
    }

    public float getCustomDamage() {
        return customDamage;
    }

    public boolean causesFire() {
        return causesFire;
    }

    public Item getItem() {
        return itemSupplier.get();
    }
}
