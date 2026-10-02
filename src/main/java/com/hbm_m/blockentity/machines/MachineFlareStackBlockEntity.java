package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm_m.inventory.menu.MachineFlareStackMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineGasFlare}: Gasfackel mit Durchflussventil ({@code isOn}) und Zuendung ({@code doesBurn}).
 * Offenes Ventil ohne Zuendung (oder mit nicht brennbarem Fluid) blaest Gase ab (50 mB/t, Zischen, Rauchsaeule in
 * Fluidfarbe, Verschmutzung SPILL); mit Zuendung werden brennbare Fluide verbrannt (10 mB/t, Energie aus dem
 * Brennwert, Gase /5 sonst /10, Flamme, Brandschaden ueber der Spitze, Verschmutzung BURN). Upgrades: Geschwindigkeit
 * (+100 % Durchsatz je Stufe), Effizienz (+33 % Energie je Stufe). Anschluesse zwei Bloecke von der Mitte entfernt.
 */
public class MachineFlareStackBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2,
        com.hbm_m.api.tile.IControlReceiver, com.hbm_m.interfaces.IUpgradeInfoProvider {

    public static final long maxPower = 100000;

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_IN = 1;
    public static final int SLOT_FLUID_OUT = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_UPGRADE_1 = 4;
    public static final int SLOT_UPGRADE_2 = 5;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.EFFECT, 3);
    }

    public final FluidTank tank = new FluidTank(ModFluids.GAS.getSource(), 64000);
    public boolean isOn = false;
    public boolean doesBurn = false;
    protected int fluidUsed = 0;
    protected int output = 0;

    public final UpgradeManager upgradeManager = new UpgradeManager();

    public MachineFlareStackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLARE_STACK_BE.get(), pos, state, 6, maxPower, 0L, maxPower);
    }

    public FluidTank getTank() { return tank; }

    public long getPowerScaled(long i) {
        return (energy * i) / maxPower;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) <= 256;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("valve")) this.isOn = !this.isOn;
        if (data.contains("dial")) this.doesBurn = !this.doesBurn;
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFlareStackBlockEntity be) {
        if (!level.isClientSide) be.serverTick((ServerLevel) level, pos);
        else be.clientTick(level, pos);
    }

    private static boolean isGas(FluidType type) {
        return type.hasTrait(FT_Gaseous.class) || type.hasTrait(FT_Gaseous_ART.class);
    }

    private void serverTick(ServerLevel world, BlockPos pos) {
        this.checkTilt(TiltType.CONFIG, false);

        this.fluidUsed = 0;
        this.output = 0;

        // Original getConPos: zwei Bloecke von der Mitte in alle vier Richtungen
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos con = pos.relative(dir, 2);
            this.tryProvide(world, con.getX(), con.getY(), con.getZ(), dir);
            this.trySubscribe(tank.getTankType(), world, con, dir);
        }

        ItemStack[] slots = slotsArray();
        boolean changed = tank.setType(SLOT_FLUID_ID, slots);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, slots);
        if (changed) applySlots(slots);

        int maxVent = 50;
        int maxBurn = 10;

        FluidType type = FluidType.forFluid(tank.getTankType());

        if (isOn && tank.getFill() > 0 && !this.tilted) {

            upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
            int burn = upgradeManager.getLevel(UpgradeType.SPEED);
            int yield = upgradeManager.getLevel(UpgradeType.EFFECT);

            maxVent += maxVent * burn;
            maxBurn += maxBurn * burn;

            if (!doesBurn || !type.hasTrait(FT_Flammable.class)) {

                if (isGas(type)) {
                    int eject = Math.min(maxVent, tank.getFill());
                    this.fluidUsed = eject;
                    tank.setFill(tank.getFill() - eject);
                    type.onFluidRelease(this, tank, eject);

                    if (world.getGameTime() % 7 == 0)
                        world.playSound(null, pos.getX(), pos.getY() + 11, pos.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.5F, 0.5F);

                    if (world.getGameTime() % 5 == 0 && eject > 0) {
                        FT_Polluting.pollute(world, pos, tank.getTankType(), FluidReleaseType.SPILL, eject * 5);
                    }
                }
            } else {

                if (type.hasTrait(FT_Flammable.class)) {
                    int eject = Math.min(maxBurn, tank.getFill());
                    this.fluidUsed = eject;
                    tank.setFill(tank.getFill() - eject);

                    int penalty = 5;
                    if (!isGas(type))
                        penalty = 10;

                    long powerProd = type.getTrait(FT_Flammable.class).getHeatEnergy() * eject / 1_000; // pro mB durch 1000
                    powerProd /= penalty;
                    powerProd += powerProd * yield / 3;

                    this.output = (int) powerProd;
                    energy += powerProd;

                    if (energy > maxPower)
                        energy = maxPower;

                    com.hbm_m.util.ParticleUtil.spawnGasFlame(world, pos.getX() + 0.5F, pos.getY() + 11.75F, pos.getZ() + 0.5F,
                            world.random.nextGaussian() * 0.15, 0.2, world.random.nextGaussian() * 0.15);

                    List<Entity> list = world.getEntitiesOfClass(Entity.class, new AABB(pos.getX() - 1, pos.getY() + 12, pos.getZ() - 2, pos.getX() + 2, pos.getY() + 17, pos.getZ() + 2));
                    for (Entity e : list) {
                        e.setSecondsOnFire(5);
                        e.hurt(world.damageSources().onFire(), 5F);
                    }

                    if (world.getGameTime() % 3 == 0)
                        world.playSound(null, pos.getX(), pos.getY() + 11, pos.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.flamethrowerShoot"), SoundSource.BLOCKS, 1.5F, 0.75F);

                    if (world.getGameTime() % 5 == 0 && eject > 0) {
                        FT_Polluting.pollute(world, pos, tank.getTankType(), FluidReleaseType.BURN, eject * 5);
                    }
                }
            }
        }

        // Original: Library.chargeItemsFromTE(slots, 0, power, maxPower)
        chargeItemInSlot(SLOT_BATTERY);

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level world, BlockPos pos) {

        if (isOn && tank.getFill() > 0) {
            FluidType type = FluidType.forFluid(tank.getTankType());

            if ((!doesBurn || !type.hasTrait(FT_Flammable.class)) && isGas(type)) {

                CompoundTag data = new CompoundTag();
                data.putString("type", "tower");
                data.putFloat("lift", 1F);
                data.putFloat("base", 0.25F);
                data.putFloat("max", 3F);
                data.putInt("life", 150 + world.random.nextInt(20));
                data.putInt("color", type.getColor());

                data.putDouble("posX", pos.getX() + 0.5);
                data.putDouble("posZ", pos.getZ() + 0.5);
                data.putDouble("posY", pos.getY() + 11);

                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
            }

            if (doesBurn && type.hasTrait(FT_Flammable.class)
                    && com.hbm_m.particle.helper.ParticleEffectClient.localPlayerDistanceSq(pos.getX(), pos.getY() + 10, pos.getZ()) <= 1024) {

                CompoundTag data = new CompoundTag();
                data.putString("type", "vanillaExt");
                data.putString("mode", "smoke");
                data.putBoolean("noclip", true);
                data.putInt("overrideAge", 50);

                if (world.getGameTime() % 2 == 0) {
                    data.putDouble("posX", pos.getX() + 1.5);
                    data.putDouble("posZ", pos.getZ() + 1.5);
                    data.putDouble("posY", pos.getY() + 10.75);
                } else {
                    data.putDouble("posX", pos.getX() + 1.125);
                    data.putDouble("posZ", pos.getZ() - 0.5);
                    data.putDouble("posY", pos.getY() + 11.75);
                }

                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
            }
        }
    }

    private ItemStack[] slotsArray() {
        ItemStack[] arr = new ItemStack[inventory.getSlots()];
        for (int i = 0; i < arr.length; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlots(ItemStack[] arr) {
        for (int i = 0; i < arr.length; i++) inventory.setStackInSlot(i, arr[i]);
    }

    @Override public int getFloorCount() { return 2 * 2; }
    @Override public BlockPos getFloorPosFromIndex(int index) { return this.standardFloor3x3(index); }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== Upgrades ====================

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.EFFECT;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(KEY_EFFICIENCY, "+" + (100 * level / 3) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "gas");
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("doesBurn", doesBurn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "gas");
        isOn = tag.getBoolean("isOn");
        doesBurn = tag.getBoolean("doesBurn");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gasFlare");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFlareStackMenu.create(id, inventory, this);
    }

    /** Original: {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
