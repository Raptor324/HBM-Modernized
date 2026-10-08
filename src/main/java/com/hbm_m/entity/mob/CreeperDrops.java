package com.hbm_m.entity.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Drops der HBM-Creeper wie in 1.7.10: die Port-Typen haben keine Loot-Tabelle, also werden
 * {@code EntityLiving.dropFewItems} (Standard ueber {@code getDropItem}) und die Schallplatte aus
 * {@code EntityCreeper.onDeath} hier nachgebildet.
 */
public final class CreeperDrops {

    /** {@code Items.record_13} .. {@code Items.record_wait} in ID-Reihenfolge (1.7.10). */
    private static final Item[] RECORDS = {
            Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT
    };

    private CreeperDrops() { }

    /** {@code EntityLiving.dropFewItems}: rand(3) Stueck, mit Pluenderung + rand(looting + 1). */
    public static void dropDefault(LivingEntity mob, ItemLike item, int looting) {
        int j = mob.getRandom().nextInt(3);
        if (looting > 0) j += mob.getRandom().nextInt(looting + 1);
        for (int k = 0; k < j; ++k) drop(mob, new ItemStack(item));
    }

    /** {@code EntityCreeper.onDeath}: von einem Skelett getoetet -> eine zufaellige Schallplatte. */
    public static void dropRecord(LivingEntity mob, DamageSource source) {
        if (source.getEntity() instanceof AbstractSkeleton) {
            drop(mob, new ItemStack(RECORDS[mob.getRandom().nextInt(RECORDS.length)]));
        }
    }

    /** {@code entityDropItem(stack, 0F)}. */
    public static void drop(LivingEntity mob, ItemStack stack) {
        if (stack.isEmpty() || mob.level().isClientSide()) return;
        ItemEntity e = new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), stack);
        e.setDefaultPickUpDelay();
        mob.level().addFreshEntity(e);
    }
}
