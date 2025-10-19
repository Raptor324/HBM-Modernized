package com.hbm_m.entity.grenades;

import com.hbm_m.item.ModItems;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

public enum GrenadeType {
    STANDARD(3, 0.3f, 5.0f, false, false, () -> ModItems.GRENADE.get()),
    HE(3, 0.3f, 8.0f, false, false, () -> ModItems.GRENADEHE.get()),
    FIRE(3, 0.3f, 6.0f, true, false, () -> ModItems.GRENADEFIRE.get()),
    SLIME(4, 0.51f, 6.5f, false, false, () -> ModItems.GRENADESLIME.get()),
    SMART(3, 0.3f, 6.5f, true, true, () -> ModItems.GRENADESMART.get()),
    IF(3, 0.3f, 7.0f, true, false, () -> ModItems.GRENADEIF.get()); // IF - Incendiary Grenade

    private final int maxBounces;
    private final float bounceMultiplier;
    private final float explosionPower;
    private final boolean causesFire;
    private final boolean explodesOnEntity;
    private final Supplier<Item> itemSupplier;

    GrenadeType(int maxBounces, float bounceMultiplier, float explosionPower, boolean causesFire, boolean explodesOnEntity, Supplier<Item> itemSupplier) {
        this.maxBounces = maxBounces;
        this.bounceMultiplier = bounceMultiplier;
        this.explosionPower = explosionPower;
        this.causesFire = causesFire;
        this.explodesOnEntity = explodesOnEntity;
        this.itemSupplier = itemSupplier;
    }

    public int getMaxBounces() { return maxBounces; }
    public float getBounceMultiplier() { return bounceMultiplier; }
    public float getExplosionPower() { return explosionPower; }
    public boolean causesFire() { return causesFire; }
    public boolean explodesOnEntity() { return explodesOnEntity; }
    public Item getItem() { return itemSupplier.get(); }
}
