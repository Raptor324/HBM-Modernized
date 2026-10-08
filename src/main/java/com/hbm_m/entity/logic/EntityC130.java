package com.hbm_m.entity.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.item.EntityParachuteCrate;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.ItemFluidTank;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityC130}: Transportflugzeug, das 100 Bloecke ueber dem Ziel einfliegt und zur Haelfte seiner
 * Lebensdauer eine Fallschirmkiste abwirft (Versorgung: 5 Ziehungen aus {@code POOL_SUPPLIES}; Waffen: 1-2 Waffen
 * und 6 Munitionsziehungen). Ausgeloest wird es im Original nur von der 40-mm-Signalgranate.
 */
public class EntityC130 extends EntityPlaneBase {

    public C130PayloadType payload = C130PayloadType.SUPPLIES;

    public EntityC130(EntityType<? extends EntityC130> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            com.hbm_m.client.sound.ChopperSoundClient.tickPlane(this, "hbm:entity.bomberLoop");
        }

        if (!level().isClientSide && this.tickCount == this.getLifetime() / 2 && this.health > 0) {
            Vec3 m = getDeltaMovement();
            EntityParachuteCrate crate = new EntityParachuteCrate(ModEntities.PARACHUTE_CRATE.get(), level());
            crate.setPos(getX() - m.x * 7, getY() - 10, getZ() - m.z * 7);

            if (this.payload == C130PayloadType.SUPPLIES) {
                for (int i = 0; i < 5; i++) crate.items.add(getStack(POOL_SUPPLIES, this.random));
            }
            if (this.payload == C130PayloadType.WEAPONS) {
                int amount = 1 + random.nextInt(2);
                for (int i = 0; i < amount; i++) crate.items.add(getStack(POOL_WEAPONS, this.random));
                for (int i = 0; i < 6; i++) crate.items.add(getStack(POOL_AMMO, this.random));
            }

            crate.items.removeIf(ItemStack::isEmpty);
            level().addFreshEntity(crate);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        int i = nbt.getInt("payload");
        this.payload = C130PayloadType.values()[Math.max(0, Math.min(i, C130PayloadType.values().length - 1))];
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("payload", this.payload.ordinal());
    }

    public void fac(Level world, double x, double y, double z, C130PayloadType payload) {

        Vec3 vector = new Vec3(world.random.nextDouble() - 0.5, 0, world.random.nextDouble() - 0.5).normalize();
        vector = new Vec3(vector.x * 2, 0, vector.z * 2);

        this.payload = payload;

        this.moveTo(x - vector.x * 100, y + 100, z - vector.z * 100, 0.0F, 0.0F);
        this.loadNeighboringChunks((int) (x / 16), (int) (z / 16));

        this.setDeltaMovement(vector.x, 0.0D, vector.z);

        this.rotation();
    }

    public enum C130PayloadType {
        SUPPLIES,
        WEAPONS,
        A_FUCKING_FUEL_TRUCK
    }

    // ─── ItemPoolsC130 ────────────────────────────────────────────────────────

    public static final String POOL_SUPPLIES = "POOL_SUPPLIES";
    public static final String POOL_WEAPONS = "POOL_WEAPONS";
    public static final String POOL_AMMO = "POOL_AMMO";

    private record Entry(Supplier<ItemStack> stack, int min, int max, int weight) { }

    private static List<Entry> supplies;

    private static List<Entry> pool(String name) {
        if (POOL_SUPPLIES.equals(name)) {
            if (supplies == null) {
                supplies = new ArrayList<>();
                supplies.add(new Entry(() -> new ItemStack(ModItems.DEFINITELYFOOD.get()), 3, 10, 25));
                supplies.add(new Entry(() -> new ItemStack(ModItems.SYRINGE_METAL_STIMPAK.get()), 1, 3, 10));
                supplies.add(new Entry(() -> new ItemStack(ModItems.PILL_IODINE.get()), 1, 2, 2));
                supplies.add(new Entry(() -> ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.DIESEL.getSource(), 1), 1, 4, 5));
                supplies.add(new Entry(() -> new ItemStack(ModBlocks.DIESELGEN.get()), 1, 1, 1));
                supplies.add(new Entry(() -> new ItemStack(ModItems.GEIGER_COUNTER.get()), 1, 1, 2));
                supplies.add(new Entry(() -> new ItemStack(ModItems.MED_BAG.get()), 1, 1, 3));
                supplies.add(new Entry(() -> new ItemStack(ModItems.RADAWAY.get()), 1, 5, 10));
            }
            return supplies;
        }
        if (POOL_WEAPONS.equals(name)) {
            if (weapons == null) {
                weapons = new ArrayList<>();
                weapons.add(new Entry(() -> gun("gun_light_revolver"), 1, 1, 100));
                weapons.add(new Entry(() -> gun("gun_henry"), 1, 1, 100));
                weapons.add(new Entry(() -> gun("gun_maresleg"), 1, 1, 100));
                weapons.add(new Entry(() -> gun("gun_greasegun"), 1, 1, 100));
                weapons.add(new Entry(() -> gun("gun_carbine"), 1, 1, 50));
                weapons.add(new Entry(() -> gun("gun_heavy_revolver"), 1, 1, 50));
                weapons.add(new Entry(() -> gun("gun_panzerschreck"), 1, 1, 20));
                weapons.add(new Entry(() -> gun("gun_double_barrel"), 1, 1, 10));
                weapons.add(new Entry(() -> gun("gun_n_i_4_n_i"), 1, 1, 1));
            }
            return weapons;
        }
        if (POOL_AMMO.equals(name)) {
            if (ammo == null) {
                ammo = new ArrayList<>();
                ammo.add(new Entry(() -> ammo(EnumAmmo.M357_SP), 12, 12, 10));
                ammo.add(new Entry(() -> ammo(EnumAmmo.M357_FMJ), 6, 6, 10));
                ammo.add(new Entry(() -> ammo(EnumAmmo.M44_SP), 12, 12, 5));
                ammo.add(new Entry(() -> ammo(EnumAmmo.M44_FMJ), 6, 6, 5));
                ammo.add(new Entry(() -> ammo(EnumAmmo.P9_SP), 12, 12, 10));
                ammo.add(new Entry(() -> ammo(EnumAmmo.P9_FMJ), 6, 6, 10));
                ammo.add(new Entry(() -> ammo(EnumAmmo.R762_SP), 6, 6, 5));
                ammo.add(new Entry(() -> ammo(EnumAmmo.G12_BP), 6, 6, 10));
                ammo.add(new Entry(() -> ammo(EnumAmmo.ROCKET_HE), 1, 1, 3));
                ammo.add(new Entry(() -> new ItemStack(ModItems.AMMO_CONTAINER.get()), 1, 1, 1));
            }
            return ammo;
        }
        return List.of();
    }

    private static List<Entry> weapons;
    private static List<Entry> ammo;

    private static ItemStack gun(String name) { return new ItemStack(WeaponItems.gun(name)); }
    private static ItemStack ammo(EnumAmmo type) { return new ItemStack(WeaponItems.ammo(type)); }

    /** {@code ItemPool.getStack}: gewichtete Auswahl, Anzahl min..max. */
    public static ItemStack getStack(String name, RandomSource rand) {
        List<Entry> entries = pool(name);
        if (entries.isEmpty()) return ItemStack.EMPTY;

        int total = 0;
        for (Entry e : entries) total += e.weight;
        int roll = rand.nextInt(total);

        for (Entry e : entries) {
            roll -= e.weight;
            if (roll < 0) {
                ItemStack stack = e.stack.get().copy();
                stack.setCount(e.min + (e.max > e.min ? rand.nextInt(e.max - e.min + 1) : 0));
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
