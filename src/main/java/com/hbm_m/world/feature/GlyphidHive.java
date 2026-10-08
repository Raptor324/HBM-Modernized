package com.hbm_m.world.feature;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.GlyphidBlock;
import com.hbm_m.block.generic.GlyphidSpawnerBlock;
import com.hbm_m.blockentity.decorations.DecoLootBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code GlyphidHive}: das kleine Nest (11x5x11) aus Nestmasse mit Eierkammer und einer Schicht Schaedel,
 * Knochenhaufen und Beute. Dazu {@code LootGenerator.lootBones}/{@code lootGlyphidHive}.
 *
 * <p>Die Waffen des Nestpools ({@code gun_maresleg}, {@code gun_light_revolver}, Universalgranaten, SEDNA-Munition)
 * existieren im Port noch nicht und fehlen deshalb im Pool (Waffenrunde).</p>
 */
public class GlyphidHive {

    public static final int[][][] schematicSmall = new int[][][] {
        {
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
        },
        {
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,0,1,1,1,1,1,0,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,0,1,1,1,1,1,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
        },
        {
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,1,1,1,3,3,3,1,1,1,0},
            {0,1,1,1,3,3,3,1,1,1,0},
            {0,1,1,1,3,3,3,1,1,1,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
        },
        {
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,1,1,2,2,2,1,1,0,0},
            {0,1,1,2,2,2,2,2,1,1,0},
            {0,1,1,2,2,2,2,2,1,1,0},
            {0,1,1,2,2,2,2,2,1,1,0},
            {0,0,1,1,2,2,2,1,1,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
        },
        {
            {0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,1,1,1,1,1,1,1,1,1,0},
            {0,1,1,1,1,1,1,1,1,1,0},
            {0,1,1,1,1,1,1,1,1,1,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,1,1,1,1,1,1,1,0,0},
            {0,0,0,0,1,1,1,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0},
        }
    };

    public static void generateSmall(LevelAccessor world, int x, int y, int z, RandomSource rand, boolean infected, boolean loot) {
        int overrideMeta = infected ? 1 : 0;
        BlockState base = ((GlyphidBlock) ModBlocks.GLYPHID_BASE.get()).withType(overrideMeta);
        BlockState spawner = ((GlyphidSpawnerBlock) ModBlocks.GLYPHID_SPAWNER.get()).withType(overrideMeta);

        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 11; k++) {

                    int block = schematicSmall[4 - j][i][k];
                    int iX = x + i - 5;
                    int iY = y + j - 2;
                    int iZ = z + k - 5;
                    BlockPos pos = new BlockPos(iX, iY, iZ);

                    switch (block) {
                        case 1: world.setBlock(pos, base, 2); break;
                        case 2: world.setBlock(pos, rand.nextInt(3) == 0 ? spawner : base, 2); break;
                        case 3:
                            int r = rand.nextInt(3);
                            if (r == 0) {
                                world.setBlock(pos, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rand.nextInt(16)), 3);
                            } else if (r == 1) {
                                world.setBlock(new BlockPos(iX, iY, z + k - 5), ModBlocks.DECO_LOOT.get().defaultBlockState(), 2);
                                lootBones(world, pos, rand);
                            } else if (r == 2) {
                                if (loot) {
                                    world.setBlock(pos, ModBlocks.DECO_LOOT.get().defaultBlockState(), 2);
                                    lootGlyphidHive(world, pos, rand);
                                } else {
                                    world.setBlock(pos, base, 2);
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    // ═══════════════════════════ LootGenerator ═══════════════════════════

    private record Weighted(ItemStack stack, int min, int max, int weight) {
        Weighted(Item item, int min, int max, int weight) { this(new ItemStack(item), min, max, weight); }
    }

    private static Item mat(ModMaterials m, MaterialShape s) { return ModMaterialItems.item(m, s); }

    /** {@code ItemPoolsPile.POOL_PILE_HIVE}. */
    private static Weighted[] hivePool() {
        return new Weighted[] {
                //Materials
                new Weighted(Items.IRON_INGOT, 1, 3, 10),
                new Weighted(mat(ModMaterials.STEEL, MaterialShape.INGOT), 1, 2, 10),
                new Weighted(mat(ModMaterials.ALUMINUM, MaterialShape.INGOT), 1, 2, 10),
                new Weighted(mat(ModMaterials.SCRAP, MaterialShape.SCRAP), 3, 6, 10),
                //Armor
                new Weighted(ModItems.GAS_MASK_M65.get(), 1, 1, 10),
                new Weighted(ModItems.STEEL_PLATE.get(), 1, 1, 5),
                new Weighted(ModItems.STEEL_LEGS.get(), 1, 1, 5),
                //Gear
                new Weighted(ModItems.STEEL_PICKAXE.get(), 1, 1, 5),
                new Weighted(ModItems.STEEL_SHOVEL.get(), 1, 1, 5),
                //Weapons
                new Weighted(WeaponItems.gun("gun_maresleg"), 1, 1, 5),
                new Weighted(WeaponItems.gun("gun_light_revolver"), 1, 1, 1),
                new Weighted(ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE), 1, 2, 5),
                new Weighted(ItemGrenadeUniversal.make(EnumGrenadeShell.STICK, EnumGrenadeFilling.DEMO, EnumGrenadeFuze.IMPACT), 1, 2, 3),
                new Weighted(WeaponItems.ammo(EnumAmmo.G12), 4, 4, 10),
                new Weighted(WeaponItems.ammo(EnumAmmo.M357_SP), 6, 12, 10),
                new Weighted(WeaponItems.ammo(EnumAmmo.G40_HE), 1, 1, 2),
                //Consumables
                new Weighted(ModItems.BOTTLE_NUKA.get(), 1, 2, 20),
                new Weighted(ModItems.BOTTLE_QUANTUM.get(), 1, 2, 1),
                new Weighted(ModItems.DEFINITELYFOOD.get(), 5, 12, 20),
                new Weighted(ModItems.EGG_GLYPHID.get(), 1, 3, 30),
                new Weighted(ModItems.SYRINGE_METAL_STIMPAK.get(), 1, 1, 5),
                new Weighted(ModItems.IV_BLOOD.get(), 1, 1, 10),
                new Weighted(Items.EXPERIENCE_BOTTLE, 1, 3, 5),
        };
    }

    /** {@code ItemPoolsPile.POOL_PILE_BONES}. */
    private static Weighted[] bonesPool() {
        return new Weighted[] {
                new Weighted(Items.BONE, 1, 1, 10),
                new Weighted(Items.ROTTEN_FLESH, 1, 1, 5),
                new Weighted(ModItems.BIOMASS.get(), 1, 1, 2),
        };
    }

    /** {@code ItemPool.getStack}: gewichtete Ziehung, Menge zwischen min und max. */
    private static ItemStack getStack(Weighted[] pool, RandomSource rand) {
        int total = 0;
        for (Weighted w : pool) total += w.weight();
        int roll = rand.nextInt(total);
        Weighted pick = pool[0];
        for (Weighted w : pool) {
            roll -= w.weight();
            if (roll < 0) { pick = w; break; }
        }
        ItemStack stack = pick.stack().copy();
        stack.setCount(pick.min() + rand.nextInt(pick.max() - pick.min() + 1));
        return stack;
    }

    private static void fill(LevelAccessor world, BlockPos pos, RandomSource rand, Weighted[] pool) {
        if (world.getBlockEntity(pos) instanceof DecoLootBlockEntity loot && loot.getItems().isEmpty()) {
            int limit = rand.nextInt(3) + 3;
            for (int i = 0; i < limit; i++) {
                // addItemWithDeviation
                loot.addItem(getStack(pool, rand), rand.nextDouble() - 0.5 + rand.nextGaussian() * 0.02, i * 0.03125, rand.nextDouble() - 0.5 + rand.nextGaussian() * 0.02);
            }
        }
    }

    public static void lootBones(LevelAccessor world, BlockPos pos, RandomSource rand) {
        fill(world, pos, rand, bonesPool());
    }

    public static void lootGlyphidHive(LevelAccessor world, BlockPos pos, RandomSource rand) {
        fill(world, pos, rand, hivePool());
    }

    /** Bequemlichkeit fuer Aufrufer mit {@link Level}. */
    public static void generateSmall(Level world, int x, int y, int z, RandomSource rand, boolean infected, boolean loot) {
        generateSmall((LevelAccessor) world, x, y, z, rand, infected, loot);
    }
}
