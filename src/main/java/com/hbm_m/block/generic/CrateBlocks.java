package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hbm_m.block.ModBlocks;
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
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

/** Beutekisten des Originals: {@code BlockCrate}, {@code BlockAmmoCrate}, {@code BlockJungleCrate}. */
public final class CrateBlocks {

    private CrateBlocks() {}

    /** Gegenstand nach Registriername; Luft, solange er (noch) nicht portiert ist - dann faellt der Eintrag weg. */
    static Item id(String name) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", name));
    }

    static void add(List<ItemStack> list, Item item, int weight) {
        if (item == null || item == Items.AIR) return;
        for (int i = 0; i < weight; i++) list.add(new ItemStack(item));
    }

    /** Original {@code addToListWithWeight(list, ItemStack, weight)} (z.B. Baukastengranaten mit NBT). */
    static void add(List<ItemStack> list, ItemStack stack, int weight) {
        if (stack == null || stack.isEmpty()) return;
        for (int i = 0; i < weight; i++) list.add(stack.copy());
    }

    static void add(List<ItemStack> list, String name, int weight) { add(list, id(name), weight); }

    static void mat(List<ItemStack> list, ModMaterials m, MaterialShape s, int weight) { add(list, ModMaterialItems.item(m, s), weight); }

    /** {@code "hbm:block.crateBreak"}: eine der fuenf Varianten. */
    static void breakSound(Level world, BlockPos pos) {
        var sounds = List.of(ModSounds.CRATEBREAK1, ModSounds.CRATEBREAK2, ModSounds.CRATEBREAK3, ModSounds.CRATEBREAK4, ModSounds.CRATEBREAK5);
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sounds.get(world.random.nextInt(sounds.size())).get(), SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    // ================================================================================================

    /**
     * 1:1 {@code BlockCrate} ({@code crate}, {@code crate_weapon}, {@code crate_lead}, {@code crate_metal},
     * {@code crate_red}): faellt wie Sand; mit der Brechstange aufgehebelt verteilt sie 3-5 gewichtete Zufallsfunde
     * (Waffenkiste 1-2, mit 1 % Chance 25), die rote Kiste ihren kompletten Inhalt.
     */
    public static class Supply extends FallingBlock {

        public enum Kind { SUPPLY, WEAPON, LEAD, METAL, RED }

        private final Kind kind;

        public Supply(Properties p, Kind kind) {
            super(p);
            this.kind = kind;
        }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return pry(world, pos, player.getItemInHand(hand));
        }
        //?} else {
        /*@Override
        protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return pry(world, pos, stack) == InteractionResult.PASS ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        public static final com.mojang.serialization.MapCodec<Supply> CODEC = simpleCodec(p -> new Supply(p, Kind.SUPPLY));
        @Override protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() { return CODEC; }
        *///?}

        private InteractionResult pry(Level world, BlockPos pos, ItemStack held) {
            if (!held.isEmpty() && held.is(ModItems.CROWBAR.get())) {
                if (!world.isClientSide) {
                    dropItems(world, pos);
                    world.removeBlock(pos, false);
                    breakSound(world, pos);
                }
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
            return InteractionResult.PASS;
        }

        private List<ItemStack> list() {
            List<ItemStack> l = new ArrayList<>();
            switch (kind) {
                case SUPPLY -> {
                    add(l, "syringe_metal_stimpak", 10);
                    add(l, "syringe_antidote", 5);
                    add(l, ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE), 8);
                    add(l, ItemGrenadeUniversal.make(EnumGrenadeShell.STICK, EnumGrenadeFilling.HE, EnumGrenadeFuze.IMPACT), 6);
                    add(l, ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.INC, EnumGrenadeFuze.S7), 4);
                    add(l, "ammo_container", 2);
                }
                case WEAPON -> {
                    add(l, "gun_light_revolver", 10);
                    add(l, "gun_maresleg", 7);
                    add(l, "gun_heavy_revolver", 5);
                    add(l, "gun_greasegun", 5);
                    add(l, "gun_liberator", 2);
                    add(l, "gun_flaregun", 8);
                    add(l, "gun_panzerschreck", 1);
                }
                case LEAD -> {
                    mat(l, ModMaterials.URANIUM, MaterialShape.INGOT, 10);
                    mat(l, ModMaterials.URANIUM238, MaterialShape.INGOT, 8);
                    mat(l, ModMaterials.PLUTONIUM, MaterialShape.INGOT, 7);
                    mat(l, ModMaterials.PLUTONIUM240, MaterialShape.INGOT, 6);
                    mat(l, ModMaterials.NEPTUNIUM, MaterialShape.INGOT, 7);
                    mat(l, ModMaterials.URANIUM_FUEL, MaterialShape.INGOT, 8);
                    mat(l, ModMaterials.PLUTONIUM_FUEL, MaterialShape.INGOT, 7);
                    mat(l, ModMaterials.MOX_FUEL, MaterialShape.INGOT, 6);
                    mat(l, ModMaterials.URANIUM, MaterialShape.NUGGET, 10);
                    mat(l, ModMaterials.URANIUM238, MaterialShape.NUGGET, 8);
                    mat(l, ModMaterials.PLUTONIUM, MaterialShape.NUGGET, 7);
                    mat(l, ModMaterials.PLUTONIUM240, MaterialShape.NUGGET, 6);
                    mat(l, ModMaterials.NEPTUNIUM, MaterialShape.NUGGET, 7);
                    mat(l, ModMaterials.URANIUM_FUEL, MaterialShape.NUGGET, 8);
                    mat(l, ModMaterials.PLUTONIUM_FUEL, MaterialShape.NUGGET, 7);
                    mat(l, ModMaterials.MOX_FUEL, MaterialShape.NUGGET, 6);
                    add(l, "cell_deuterium", 8);
                    add(l, "cell_tritium", 8);
                    add(l, "cell_uf6", 8);
                    add(l, "cell_puf6", 8);
                    add(l, "pellet_rtg", 6);
                    add(l, "pellet_rtg_weak", 7);
                    add(l, ModItems.POWDER_YELLOWCAKE.get(), 10);
                }
                case METAL -> {
                    add(l, ModItems.PRESS.get(), 10);
                    add(l, ModItems.BREEDER.get(), 6);
                    add(l, ModItems.WOOD_BURNER.get(), 10);
                    add(l, ModBlocks.DIESELGEN.get().asItem(), 8);
                    add(l, ModBlocks.MACHINE_RTG.get().asItem(), 4);
                    add(l, ModBlocks.RED_PYLON.get().asItem(), 9);
                    add(l, "battery_pack_battery_lead", 10);
                    add(l, ModBlocks.ELECTRIC_FURNACE.get().asItem(), 8);
                    add(l, ModItems.MACHINE_ASSEMBLER.get(), 10);
                    add(l, ModItems.FLUID_TANK.get(), 7);
                    add(l, "centrifuge_element", 6);
                    add(l, "motor", 8);
                    add(l, "coil_tungsten", 7);
                    add(l, "photo_panel", 3);
                    add(l, "coil_copper", 10);
                    add(l, "blade_titanium", 3);
                    add(l, "piston_selenium", 6);
                }
                case RED -> {
                    add(l, "mysteryshovel", 1);
                    add(l, "gun_heavy_revolver_lilmac", 1);
                    add(l, "gun_autoshotgun_sexy", 1);
                    add(l, "gun_maresleg_broken", 1);
                    add(l, WeaponItems.ammo(EnumAmmoSecret.M44_EQUESTRIAN), 1);
                    add(l, WeaponItems.ammo(EnumAmmoSecret.G12_EQUESTRIAN), 1);
                    add(l, WeaponItems.ammo(EnumAmmoSecret.BMG50_EQUESTRIAN), 1);
                    add(l, "battery_spark", 1);
                    add(l, "bottle_sparkle", 1);
                    add(l, "bottle_rad", 1);
                    add(l, "ring_starmetal", 1);
                    add(l, "flame_pony", 1);
                    add(l, ModBlocks.NTM_DIRT.get().asItem(), 1);
                    add(l, ModBlocks.BROADCASTER_PC.get().asItem(), 1);
                }
            }
            return l;
        }

        public void dropItems(Level world, BlockPos pos) {
            Random rand = new Random();
            List<ItemStack> pool = list();
            List<ItemStack> out = new ArrayList<>();

            int i = rand.nextInt(3) + 3;
            if (kind == Kind.WEAPON) {
                i = 1 + rand.nextInt(2);
                if (rand.nextInt(100) == 34) i = 25;
            }
            if (!pool.isEmpty()) for (int j = 0; j < i; j++) out.add(pool.get(rand.nextInt(pool.size())));
            if (kind == Kind.RED) {
                out.clear();
                out.addAll(pool);
            }

            for (ItemStack stack : out) {
                float f = rand.nextFloat() * 0.8F + 0.1F;
                float f1 = rand.nextFloat() * 0.8F + 0.1F;
                float f2 = rand.nextFloat() * 0.8F + 0.1F;
                ItemEntity e = new ItemEntity(world, pos.getX() + f, pos.getY() + f1, pos.getZ() + f2, stack.copy());
                float f3 = 0.05F;
                e.setDeltaMovement((float) rand.nextGaussian() * f3, (float) rand.nextGaussian() * f3 + 0.2F, (float) rand.nextGaussian() * f3);
                world.addFreshEntity(e);
            }
        }
    }

    // ================================================================================================

    /** 1:1 {@code BlockAmmoCrate} ({@code crate_ammo}): Brechstange -> Kronkorken, Stimpaks und eine Munitionsauswahl. */
    public static class Ammo extends Block {
        public Ammo(Properties p) { super(p); }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return pry(world, pos, player.getItemInHand(hand));
        }
        //?} else {
        /*@Override
        protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return pry(world, pos, stack) == InteractionResult.PASS ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        *///?}

        private InteractionResult pry(Level world, BlockPos pos, ItemStack held) {
            if (!held.isEmpty() && held.is(ModItems.CROWBAR.get())) {
                if (!world.isClientSide) {
                    for (ItemStack s : getContents(world.random)) popResource(world, pos, s);
                    world.removeBlock(pos, false);
                    breakSound(world, pos);
                }
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
            return InteractionResult.PASS;
        }

        /** Original {@code new ItemStack(ModItems.ammo_standard, count, EnumAmmo.X.ordinal())} -> {@code ammo_standard_<x>}. */
        private static void ammo(List<ItemStack> ret, EnumAmmo type, int count) {
            Item it = WeaponItems.ammo(type);
            if (it != null && it != Items.AIR) ret.add(new ItemStack(it, count));
        }

        public static List<ItemStack> getContents(net.minecraft.util.RandomSource rand) {
            List<ItemStack> ret = new ArrayList<>();
            ret.add(new ItemStack(id("cap_nuka"), 12 + rand.nextInt(21)));
            ret.add(new ItemStack(id("syringe_metal_stimpak"), 1 + rand.nextInt(3)));

            if (rand.nextBoolean()) ammo(ret, EnumAmmo.P9_SP, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.P9_FMJ, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.M357_SP, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.M357_FMJ, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.M44_SP, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.M44_FMJ, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.R556_SP, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.R556_FMJ, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.R762_SP, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.R762_FMJ, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.G12, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.G12_SLUG, 16 + rand.nextInt(17));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.G40_HE, 2 + rand.nextInt(3));
            if (rand.nextBoolean()) ammo(ret, EnumAmmo.ROCKET_HE, 2 + rand.nextInt(3));
            if (rand.nextInt(10) == 0) ret.add(new ItemStack(id("syringe_metal_super"), 2));
            ret.removeIf(ItemStack::isEmpty);
            return ret;
        }
    }

    // ================================================================================================

    /** 1:1 {@code BlockJungleCrate} ({@code crate_jungle}): zerbricht immer zu Gold in allen Formen. */
    public static class Jungle extends Block {
        public Jungle(Properties p) { super(p); }

        private static void gold(List<ItemStack> ret, MaterialShape shape, int count) {
            Item it = ModMaterialItems.item(ModMaterials.GOLD, shape);
            if (it != null) ret.add(new ItemStack(it, count));
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            var rand = params.getLevel().random;
            List<ItemStack> ret = new ArrayList<>();
            ret.add(new ItemStack(Items.GOLD_INGOT, 4 + rand.nextInt(4)));
            ret.add(new ItemStack(Items.GOLD_NUGGET, 8 + rand.nextInt(10)));
            gold(ret, MaterialShape.POWDER, 2 + rand.nextInt(3));
            gold(ret, MaterialShape.WIRE, 4 + rand.nextInt(5));
            gold(ret, MaterialShape.WIRE_DENSE, 1 + rand.nextInt(2));
            if (rand.nextInt(2) == 0) gold(ret, MaterialShape.PLATE, 1 + rand.nextInt(2));
            if (rand.nextInt(3) == 0) gold(ret, MaterialShape.CRYSTAL, 1);
            return ret;
        }
    }
}
