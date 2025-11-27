package com.hbm_m.entity.grenades;

import com.hbm_m.item.ModItems;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

public enum GrenadeType {
    // Добавлено значение урона (4-й параметр):
    // STANDARD: 5.0 взрыв -> 50.0 урона (25 сердец)
    STANDARD(3, 0.3f, 2.0f, 20.0f, false, false, () -> ModItems.GRENADE.get()),

    // HE: 8.0 взрыв -> 80.0 урона (40 сердец)
    HE(3, 0.3f, 4.0f, 40.0f, false, false, () -> ModItems.GRENADEHE.get()),

    // FIRE: 6.0 взрыв -> 60.0 урона
    FIRE(3, 0.3f, 3.0f, 30.0f, true, false, () -> ModItems.GRENADEFIRE.get()),

    // SLIME: 6.5 взрыв -> 65.0 урона
    SLIME(4, 0.51f, 3.5f, 30.0f, false, false, () -> ModItems.GRENADESLIME.get()),

    // SMART: 6.5 взрыв -> 65.0 урона
    SMART(3, 0.3f, 3.5f, 30.0f, true, true, () -> ModItems.GRENADESMART.get());

    private final int maxBounces;
    private final float bounceMultiplier;
    private final float explosionPower;
    private final float customDamage;  // ✅ Новый параметр урона
    private final boolean causesFire;
    private final boolean explodesOnEntity;
    private final Supplier<Item> itemSupplier;

    GrenadeType(int maxBounces, float bounceMultiplier, float explosionPower, float customDamage,
                boolean causesFire, boolean explodesOnEntity, Supplier<Item> itemSupplier) {
        this.maxBounces = maxBounces;
        this.bounceMultiplier = bounceMultiplier;
        this.explosionPower = explosionPower;
        this.customDamage = customDamage;  // ✅ Сохранено
        this.causesFire = causesFire;
        this.explodesOnEntity = explodesOnEntity;
        this.itemSupplier = itemSupplier;
    }

    public int getMaxBounces() { return maxBounces; }
    public float getBounceMultiplier() { return bounceMultiplier; }
    public float getExplosionPower() { return explosionPower; }
    public float getCustomDamage() { return customDamage; }  // ✅ Новый геттер
    public boolean causesFire() { return causesFire; }
    public boolean explodesOnEntity() { return explodesOnEntity; }
    public Item getItem() { return itemSupplier.get(); }
}
