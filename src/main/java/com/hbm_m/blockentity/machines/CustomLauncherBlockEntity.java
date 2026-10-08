package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb.BombReturnCode;
import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.IRadarCommandReceiver;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.missile.EntityMissileCustom;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.CustomLauncherMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.missile.ItemCustomMissile;
import com.hbm_m.item.missile.ItemCustomMissilePart;
import com.hbm_m.item.missile.ItemCustomMissilePart.FuelType;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartSize;
import com.hbm_m.item.missile.MissileStruct;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Gemeinsamer 1:1-Kern von {@code TileEntityCompactLauncher} und {@code TileEntityLaunchTable} (die beiden Originale
 * unterscheiden sich nur in Tankgroesse, Startfeld, Anschluessen und der waehlbaren Rampengroesse). Slots: Rakete (0),
 * Zielgeber (1), Treibstoff-/Oxidatorkanister (2/3 mit Ausgabe 6/7), Feststoff (4, je {@code rocket_fuel} 250 l),
 * Batterie (5). Der Treibstoff ergibt sich aus dem Rumpf der eingelegten Rakete, ein Redstonesignal auf dem Startfeld
 * startet sie zum Ziel des Zielgebers, die Streuung ergibt sich aus Chip und Leitwerk.
 */
public abstract class CustomLauncherBlockEntity extends BaseMachineBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2, IRadarCommandReceiver, com.hbm_m.api.tile.IControlReceiver {

    public static final int SLOT_MISSILE = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_FUEL_IN = 2;
    public static final int SLOT_OXIDIZER_IN = 3;
    public static final int SLOT_SOLID = 4;
    public static final int SLOT_BATTERY = 5;
    public static final int SLOT_FUEL_OUT = 6;
    public static final int SLOT_OXIDIZER_OUT = 7;
    public static final int SLOT_COUNT = 8;

    public static final long maxPower = 100000;

    public int solid;
    public final int maxSolid;
    public final FluidTank[] tanks;

    private String customName;

    protected CustomLauncherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, SLOT_COUNT, maxPower, maxPower);
        this.maxSolid = capacity;
        this.tanks = new FluidTank[] {
                new FluidTank(ModFluids.NONE.getSource(), capacity),
                new FluidTank(ModFluids.NONE.getSource(), capacity)
        };
    }

    /** Groesse des oberen Rumpfanschlusses, die dieser Werfer annimmt. */
    public abstract PartSize getPadSize();

    /** Halbe Kantenlaenge des Redstone-Startfelds (Kompaktwerfer 1, Starttisch 4). */
    protected abstract int getTriggerRadius();

    /** Ob ein bereiter Zielgeber Startbedingung ist (nur beim Kompaktwerfer). */
    protected abstract boolean needsDesignator();

    /** Abonniert Strom und beide Treibstoffe an den Anschlussfeldern ({@code updateConnections}). */
    protected abstract void updateConnections(ServerLevel level);

    protected abstract String defaultName();

    public static void serverTick(Level level, BlockPos pos, BlockState state, CustomLauncherBlockEntity be) {
        if (level.isClientSide) {
            be.clientTick(level, pos);
            return;
        }

        be.updateTypes();

        ItemStack[] slots = be.slotArray();
        be.tanks[0].loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, slots);
        be.tanks[1].loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, slots);
        be.writeBack(slots);

        ItemStack battery = be.inventory.getStackInSlot(SLOT_BATTERY);
        if (!battery.isEmpty() && battery.getItem() instanceof ItemCreativeBattery) be.setEnergyStored(be.getMaxEnergyStored());
        else be.chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack solidStack = be.inventory.getStackInSlot(SLOT_SOLID);
        if (!solidStack.isEmpty() && solidStack.is(ModItems.ROCKET_FUEL.get()) && be.solid + 250 <= be.maxSolid) {
            be.inventory.extractItem(SLOT_SOLID, 1, false);
            be.solid += 250;
        }

        if (level.getGameTime() % 20 == 0 && level instanceof ServerLevel server) be.updateConnections(server);

        be.sendUpdateToClient();

        int r = be.getTriggerRadius();
        outer:
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (level.hasNeighborSignal(pos.offset(x, 0, z)) && be.canLaunch()) {
                    be.launchFromDesignator();
                    break outer;
                }
            }
        }
    }

    /** Clientseite: Startrauch, solange eine Baukastenrakete ueber dem Werfer steht. */
    protected void clientTick(Level level, BlockPos pos) {
        List<EntityMissileCustom> entities = level.getEntitiesOfClass(EntityMissileCustom.class,
                new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 10, pos.getZ() + 1.5));
        if (entities.isEmpty()) return;

        for (int i = 0; i < 15; i++) {
            boolean dir = level.random.nextBoolean();
            float spread = smokeSpread();
            float moX = (float) (dir ? 0 : level.random.nextGaussian() * spread);
            float moZ = (float) (!dir ? 0 : level.random.nextGaussian() * spread);

            CompoundTag data = new CompoundTag();
            data.putDouble("posX", pos.getX() + 0.5);
            data.putDouble("posY", pos.getY() + 0.25);
            data.putDouble("posZ", pos.getZ() + 0.5);
            data.putString("type", "launchSmoke");
            data.putDouble("moX", moX);
            data.putDouble("moY", 0);
            data.putDouble("moZ", moZ);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    }

    protected float smokeSpread() {
        return 0.5F;
    }

    private ItemStack[] slotArray() {
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = inventory.getStackInSlot(i);
        return slots;
    }

    private void writeBack(ItemStack[] slots) {
        for (int i : new int[] { SLOT_FUEL_IN, SLOT_FUEL_OUT, SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT }) {
            if (inventory.getStackInSlot(i) != slots[i]) inventory.setStackInSlot(i, slots[i]);
        }
    }

    public long getPowerScaled(long i) {
        return (getEnergyStored() * i) / maxPower;
    }

    public int getSolidScaled(int i) {
        return (solid * i) / maxSolid;
    }

    public boolean canLaunch() {
        return getEnergyStored() >= maxPower * 0.75 && isMissileValid() && (!needsDesignator() || hasDesignator()) && hasFuel();
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        // Original: (int) Math.floor(target.posX) fuer x UND z
        return sendCommandPosition(new BlockPos((int) Math.floor(target.getX()), worldPosition.getY(), (int) Math.floor(target.getX())));
    }

    @Override
    public boolean sendCommandPosition(BlockPos pos) {
        if (!canLaunch()) return false;
        this.launchTo(pos.getX(), pos.getZ());
        return true;
    }

    /** {@code IBomb.explode}: Start ueber Zuender bzw. Fernzuender. */
    public BombReturnCode triggerLaunch() {
        if (canLaunch()) {
            launchFromDesignator();
            return BombReturnCode.LAUNCHED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    public void launchFromDesignator() {
        ItemStack stack = inventory.getStackInSlot(SLOT_DESIGNATOR);
        if (level != null && stack.getItem() instanceof IDesignatorItem designator) {
            if (designator.isReady(level, stack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) {
                Vec3 coords = designator.getCoords(level, stack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
                int tX = (int) Math.floor(coords.x);
                int tZ = (int) Math.floor(coords.z);
                this.launchTo(tX, tZ);
            }
        }
    }

    public void launchTo(int tX, int tZ) {
        if (level == null) return;
        level.playSound(null, worldPosition, ModSounds.MISSILE_TAKEOFF.get(), SoundSource.BLOCKS, 10.0F, 1.0F);

        ItemStack missileStack = inventory.getStackInSlot(SLOT_MISSILE);
        ItemCustomMissilePart chip = ItemCustomMissile.read(missileStack, "chip");
        float c = chip != null ? (Float) chip.attributes[0] : 1.0F;
        float f = 1.0F;

        MissileStruct struct = getStruct(missileStack);
        if (struct.fins != null) {
            ItemCustomMissilePart fins = ItemCustomMissile.read(missileStack, "stability");
            if (fins != null) f = (Float) fins.attributes[0];
        }

        double tx = (worldPosition.getX() - tX) * c * f;
        double tz = (worldPosition.getZ() - tZ) * c * f;
        // Vec3.rotateAroundY(rad) mit einem Gradwert 0..360 wie im Original
        float angle = level.random.nextFloat() * 360;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double rx = tx * cos + tz * sin;
        double rz = tz * cos - tx * sin;

        EntityMissileCustom missile = ModEntities.MISSILE_CUSTOM.get().create(level);
        if (missile != null) {
            missile.setup(worldPosition.getX() + 0.5F, worldPosition.getY() + 2.5F, worldPosition.getZ() + 0.5F, tX + (int) rx, tZ + (int) rz, struct);
            level.addFreshEntity(missile);
        }

        subtractFuel();

        inventory.setStackInSlot(SLOT_MISSILE, ItemStack.EMPTY);
        setChanged();
    }

    private boolean hasFuel() {
        return solidState() != 0 && liquidState() != 0 && oxidizerState() != 0;
    }

    private void subtractFuel() {
        MissileStruct multipart = getStruct(inventory.getStackInSlot(SLOT_MISSILE));
        if (multipart == null || multipart.fuselage == null) return;

        ItemCustomMissilePart fuselage = multipart.fuselage;
        float f = (Float) fuselage.attributes[1];
        int fuel = (int) f;

        switch ((FuelType) fuselage.attributes[0]) {
            case KEROSENE, HYDROGEN, BALEFIRE -> {
                tanks[0].setFill(tanks[0].getFill() - fuel);
                tanks[1].setFill(tanks[1].getFill() - fuel);
            }
            case XENON -> tanks[0].setFill(tanks[0].getFill() - fuel);
            case SOLID -> this.solid -= fuel;
            default -> { }
        }

        setEnergyStored(getEnergyStored() - (long) (maxPower * 0.75));
    }

    public static MissileStruct getStruct(ItemStack stack) {
        return ItemCustomMissile.getStruct(stack);
    }

    /** Die eingelegte Rakete (Renderer, GUI); {@code null} ohne Baukastenrakete. */
    @Nullable
    public MissileStruct getLoad() {
        return getStruct(inventory.getStackInSlot(SLOT_MISSILE));
    }

    public boolean isMissileValid() {
        MissileStruct multipart = getStruct(inventory.getStackInSlot(SLOT_MISSILE));
        if (multipart == null || multipart.fuselage == null) return false;
        return multipart.fuselage.top == getPadSize();
    }

    public boolean hasDesignator() {
        ItemStack stack = inventory.getStackInSlot(SLOT_DESIGNATOR);
        return level != null && stack.getItem() instanceof IDesignatorItem designator
                && designator.isReady(level, stack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    @Nullable
    private ItemCustomMissilePart fuselage() {
        MissileStruct multipart = getStruct(inventory.getStackInSlot(SLOT_MISSILE));
        return multipart == null ? null : multipart.fuselage;
    }

    public int solidState() {
        ItemCustomMissilePart fuselage = fuselage();
        if (fuselage == null) return -1;
        if ((FuelType) fuselage.attributes[0] == FuelType.SOLID) {
            return solid >= (Float) fuselage.attributes[1] ? 1 : 0;
        }
        return -1;
    }

    public int liquidState() {
        ItemCustomMissilePart fuselage = fuselage();
        if (fuselage == null) return -1;
        return switch ((FuelType) fuselage.attributes[0]) {
            case KEROSENE, HYDROGEN, XENON, BALEFIRE -> tanks[0].getFill() >= (Float) fuselage.attributes[1] ? 1 : 0;
            default -> -1;
        };
    }

    public int oxidizerState() {
        ItemCustomMissilePart fuselage = fuselage();
        if (fuselage == null) return -1;
        return switch ((FuelType) fuselage.attributes[0]) {
            case KEROSENE, HYDROGEN, BALEFIRE -> tanks[1].getFill() >= (Float) fuselage.attributes[1] ? 1 : 0;
            default -> -1;
        };
    }

    public void updateTypes() {
        ItemCustomMissilePart fuselage = fuselage();
        if (fuselage == null) return;

        switch ((FuelType) fuselage.attributes[0]) {
            case KEROSENE -> { setType(0, ModFluids.KEROSENE); setType(1, ModFluids.PEROXIDE); }
            case HYDROGEN -> { setType(0, ModFluids.HYDROGEN); setType(1, ModFluids.OXYGEN); }
            case XENON -> setType(0, ModFluids.XENON);
            case BALEFIRE -> { setType(0, ModFluids.BALEFIRE); setType(1, ModFluids.PEROXIDE); }
            default -> { }
        }
    }

    private void setType(int tank, ModFluids.FluidEntry fluid) {
        if (tanks[tank].getTankType() != fluid.getSource()) tanks[tank].setTankType(fluid.getSource());
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public void setCustomName(String name) {
        this.customName = name;
        setChanged();
    }

    // ─── Steuerung (Rampengroesse beim Starttisch) ──────────────────────────────

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void receiveControl(CompoundTag data) { }

    // ─── BaseMachineBlockEntity ─────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return customName != null && !customName.isEmpty() ? Component.literal(customName) : Component.translatable(defaultName());
    }

    public Component getDisplayName() {
        return getDefaultName();
    }

    /** Im Original {@code isItemValidForSlot = false} (keine Automatisierung) - die GUI-Slots nehmen aber alles an. */
    @Override
    protected boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    //? if forge {
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        // Original: canInsertItem/canExtractItem false - Trichter und Rohre kommen nicht an das Inventar
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) return net.minecraftforge.common.util.LazyOptional.empty();
        return super.getCapability(cap, side);
    }
    //?}

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new CustomLauncherMenu(containerId, inv, this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "fuel");
        tanks[1].writeToNBT(tag, "oxidizer");
        tag.putInt("solidfuel", solid);
        if (customName != null) tag.putString("name", customName);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "fuel");
        tanks[1].readFromNBT(tag, "oxidizer");
        solid = tag.getInt("solidfuel");
        customName = tag.contains("name") ? tag.getString("name") : null;
    }

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
}
