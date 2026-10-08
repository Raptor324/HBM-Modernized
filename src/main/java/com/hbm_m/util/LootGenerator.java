package com.hbm_m.util;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.decorations.DecoLootBlockEntity;
import com.hbm_m.item.special.ItemBookLore;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.itempool.ItemPool;
import com.hbm_m.itempool.ItemPoolsPile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;

/**
 * 1:1 {@code com.hbm.util.LootGenerator}: befuellt Lootstapel ({@code deco_loot}, {@code BlockLoot.TileEntityLoot})
 * mit festen Arrangements bzw. Ziehungen aus den {@code ItemPoolsPile}-Pools.
 */
public class LootGenerator {

    public static final String LOOT_BOOKLET = "LOOT_BOOKLET";
    public static final String LOOT_CAPNUKE = "LOOT_CAPNUKE";
    public static final String LOOT_MEDICINE = "LOOT_MEDICINE";
    public static final String LOOT_CAPSTASH = "LOOT_CAPSTASH";
    public static final String LOOT_MAKESHIFT_GUN = "LOOT_MAKESHIFT_GUN";
    public static final String LOOT_NUKE_STORAGE = "LOOT_NUKE_STORAGE";
    public static final String LOOT_BONES = "LOOT_BONES";
    public static final String LOOT_GLYPHID_HIVE = "LOOT_GLYPHID_HIVE";
    public static final String LOOT_METEOR = "LOOT_METEOR";
    public static final String LOOT_FLAREGUN = "LOOT_FLAREGUN";
    public static final String LOOT_SHIT = "LOOT_SHIT";
    public static final String LOOT_MECHANICAL = "LOOT_MECHANICAL";
    public static final String LOOT_GEAR = "LOOT_GEAR";
    public static final String LOOT_SUPPLIES = "LOOT_SUPPLIES";

    /** Original: switch ohne break - jeder Fall faellt bis zum default durch (alle folgenden Arrangements). */
    public static void applyLoot(LevelAccessor world, int x, int y, int z, String name) {
        switch (name) {
            case LOOT_BOOKLET: lootBooklet(world, x, y, z);
            case LOOT_CAPNUKE: lootCapNuke(world, x, y, z);
            case LOOT_MEDICINE: lootMedicine(world, x, y, z);
            case LOOT_CAPSTASH: lootCapStash(world, x, y, z);
            case LOOT_MAKESHIFT_GUN: lootMakeshiftGun(world, x, y, z);
            case LOOT_NUKE_STORAGE: lootNukeStorage(world, x, y, z);
            case LOOT_BONES: lootBones(world, x, y, z);
            case LOOT_GLYPHID_HIVE: lootGlyphidHive(world, x, y, z);
            case LOOT_METEOR: lootBookMeteor(world, x, y, z);
            case LOOT_FLAREGUN: lootFlareGun(world, x, y, z);
            case LOOT_SHIT: lootShit(world, x, y, z);
            case LOOT_MECHANICAL: lootMechanical(world, x, y, z);
            case LOOT_GEAR: lootGear(world, x, y, z);
            case LOOT_SUPPLIES: lootSupplies(world, x, y, z);
            default: lootBones(world, x, y, z); break;
        }
    }

    public static String[] getLootNames() {
        return new String[] {
                LOOT_BOOKLET,
                LOOT_CAPNUKE,
                LOOT_MEDICINE,
                LOOT_CAPSTASH,
                LOOT_MAKESHIFT_GUN,
                LOOT_NUKE_STORAGE,
                LOOT_BONES,
                LOOT_GLYPHID_HIVE,
                LOOT_METEOR,
                LOOT_FLAREGUN,
                LOOT_MECHANICAL,
                LOOT_SHIT,
                LOOT_GEAR,
                LOOT_SUPPLIES
        };
    }

    public static void setBlock(LevelAccessor world, int x, int y, int z) {
        world.setBlock(new BlockPos(x, y, z), ModBlocks.DECO_LOOT.get().defaultBlockState(), 3);
    }

    private static DecoLootBlockEntity loot(LevelAccessor world, int x, int y, int z) {
        return world.getBlockEntity(new BlockPos(x, y, z)) instanceof DecoLootBlockEntity l ? l : null;
    }

    private static ItemStack stack(String id, int count) {
        return BuiltInRegistries.ITEM.getOptional(new ResourceLocation("hbm_m", id)).map(i -> new ItemStack(i, count)).orElse(ItemStack.EMPTY);
    }

    public static void addItemWithDeviation(DecoLootBlockEntity loot, RandomSource rand, ItemStack stack, double x, double y, double z) {
        if (stack.isEmpty()) return;
        loot.addItem(stack, x + rand.nextGaussian() * 0.02, y, z + rand.nextGaussian() * 0.02);
    }

    public static void lootBooklet(LevelAccessor world, int x, int y, int z) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        if (loot != null && loot.getItems().isEmpty()) {
            loot.addItem(ItemBookLore.generateBeaconBook(), 0, 0, 0);
        }
    }

    public static void lootCapNuke(LevelAccessor world, int x, int y, int z) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        RandomSource rand = world.getRandom();
        if (loot != null && loot.getItems().isEmpty()) {
            if (rand.nextInt(5) == 0)
                loot.addItem(new ItemStack(WeaponItems.ammo(EnumAmmo.NUKE_STANDARD)), -0.25, 0, -0.125);
            else
                loot.addItem(new ItemStack(WeaponItems.ammo(EnumAmmo.ROCKET_HEAT)), -0.25, 0, -0.25);

            for (int i = 0; i < 4; i++) addItemWithDeviation(loot, rand, stack("cap_nuka", 2), 0.125, i * 0.03125, 0.25);
            for (int i = 0; i < 2; i++) addItemWithDeviation(loot, rand, stack("syringe_metal_stimpak", 1), -0.25, i * 0.03125, 0.25);
            for (int i = 0; i < 6; i++) addItemWithDeviation(loot, rand, stack("cap_nuka", 2), 0.125, i * 0.03125, -0.25);
        }
    }

    public static void lootMedicine(LevelAccessor world, int x, int y, int z) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        RandomSource rand = world.getRandom();
        if (loot != null && loot.getItems().isEmpty()) {
            for (int i = 0; i < 4; i++) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPoolsPile.POOL_PILE_MED_SYRINGE, rand), 0.125, i * 0.03125, 0.25);
            addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPoolsPile.POOL_PILE_MED_PILLS, rand), -0.25, 0, -0.125);
        }
    }

    public static void lootCapStash(LevelAccessor world, int x, int y, int z) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        RandomSource rand = world.getRandom();
        if (loot != null && loot.getItems().isEmpty()) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int count = rand.nextInt(5) + 3;
                    for (int k = 0; k < count; k++) {
                        addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPoolsPile.POOL_PILE_CAPS, rand), i * 0.3125, k * 0.03125, j * 0.3125);
                    }
                }
            }
        }
    }

    public static void lootMakeshiftGun(LevelAccessor world, int x, int y, int z) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        RandomSource rand = world.getRandom();
        if (loot != null && loot.getItems().isEmpty()) {
            boolean r = rand.nextBoolean();
            if (r) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPoolsPile.POOL_PILE_MAKESHIFT_GUN, rand), 0.125, 0.025, 0.25);

            if (!r || rand.nextBoolean()) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPoolsPile.POOL_PILE_MAKESHIFT_WRENCH, rand), -0.25, 0, -0.28125);

            int count = rand.nextInt(2) + 1;
            for (int i = 0; i < count; i++) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPoolsPile.POOL_PILE_MAKESHIFT_PLATES, rand), -0.25, i * 0.03125, 0.3125);

            count = rand.nextInt(2) + 2;
            for (int i = 0; i < count; i++) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPoolsPile.POOL_PILE_MAKESHIFT_WIRE, rand), 0.25, i * 0.03125, 0.1875);
        }
    }

    public static void lootNukeStorage(LevelAccessor world, int x, int y, int z) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        RandomSource rand = world.getRandom();
        if (loot != null && loot.getItems().isEmpty()) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (rand.nextBoolean()) {
                        loot.addItem(ItemPool.getStack(ItemPoolsPile.POOL_PILE_NUKE_STORAGE, rand), -0.375 + i * 0.25, 0, -0.375 + j * 0.25);
                    }
                }
            }
        }
    }

    private static void randomPile(LevelAccessor world, int x, int y, int z, String pool, int base, int extra) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        RandomSource rand = world.getRandom();
        if (loot != null && loot.getItems().isEmpty()) {
            int limit = rand.nextInt(extra) + base;
            for (int i = 0; i < limit; i++) {
                addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPool.getPool(pool), rand), rand.nextDouble() - 0.5, i * 0.03125, rand.nextDouble() - 0.5);
            }
        }
    }

    public static void lootBones(LevelAccessor world, int x, int y, int z) {
        randomPile(world, x, y, z, ItemPoolsPile.POOL_PILE_BONES, 3, 3);
    }

    public static void lootGlyphidHive(LevelAccessor world, int x, int y, int z) {
        randomPile(world, x, y, z, ItemPoolsPile.POOL_PILE_HIVE, 3, 3);
    }

    public static void lootBookMeteor(LevelAccessor world, int x, int y, int z) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        if (loot != null && loot.getItems().isEmpty() && world instanceof net.minecraft.world.level.WorldGenLevel wgl) {
            RandomSource rand = world.getRandom();
            net.minecraft.world.item.Item mkuItem = com.hbm_m.recipe.MKUCraftingRecipe.getMKUItem(rand);
            ItemStack mkuBook = com.hbm_m.recipe.MKUCraftingRecipe.generateBook(wgl.getSeed(), mkuItem);

            addItemWithDeviation(loot, rand, new ItemStack(mkuItem), 0, 0, 0.25);
            addItemWithDeviation(loot, rand, mkuBook, 0, 0, -0.25);
        }
    }

    public static void lootBookLore(LevelAccessor world, int x, int y, int z, ItemStack book) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        RandomSource rand = world.getRandom();
        if (loot != null && loot.getItems().isEmpty()) {
            addItemWithDeviation(loot, rand, book, 0, 0, -0.25);

            int count = rand.nextInt(3) + 2;
            for (int k = 0; k < count; k++) addItemWithDeviation(loot, rand, new ItemStack(Items.BOOK), -0.25, k * 0.03125, 0.25);

            count = rand.nextInt(2) + 1;
            for (int k = 0; k < count; k++) addItemWithDeviation(loot, rand, new ItemStack(Items.PAPER), 0.25, k * 0.03125, 0.125);
        }
    }

    public static void lootFlareGun(LevelAccessor world, int x, int y, int z) {
        DecoLootBlockEntity loot = loot(world, x, y, z);
        RandomSource rand = world.getRandom();
        if (loot != null && loot.getItems().isEmpty()) {
            addItemWithDeviation(loot, rand, stack("gun_flaregun", 1), 0, 0, -0.25);

            int count = rand.nextInt(3) + 2;
            for (int k = 0; k < count; k++)
                addItemWithDeviation(loot, rand, new ItemStack(WeaponItems.ammo(EnumAmmo.G26_FLARE)), -0.25, k * 0.03125, 0.25);

            count = rand.nextInt(1) + 1;
            for (int k = 0; k < count; k++)
                addItemWithDeviation(loot, rand, new ItemStack(WeaponItems.ammo(rand.nextBoolean() ? EnumAmmo.G26_FLARE_SUPPLY : EnumAmmo.G26_FLARE_WEAPON)), 0.25, k * 0.03125, 0.125);
        }
    }

    public static void lootShit(LevelAccessor world, int x, int y, int z) {
        randomPile(world, x, y, z, ItemPoolsPile.POOL_PILE_OF_GARBAGE, 3, 3);
    }

    public static void lootMechanical(LevelAccessor world, int x, int y, int z) {
        randomPile(world, x, y, z, ItemPoolsPile.POOL_PILE_MECHANICAL, 1, 6);
    }

    public static void lootGear(LevelAccessor world, int x, int y, int z) {
        randomPile(world, x, y, z, ItemPoolsPile.POOL_PILE_GEAR, 1, 6);
    }

    public static void lootSupplies(LevelAccessor world, int x, int y, int z) {
        randomPile(world, x, y, z, ItemPoolsPile.POOL_PILE_SUPPLIES, 4, 3);
    }

}
