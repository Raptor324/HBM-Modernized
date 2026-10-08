package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.MachinePollutingBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityHeaterOilburner}: Oelbrenner. Drei Plaetze (Behaelter rein 0, leer raus 1, Fluid-ID 2), Ein/Aus
 * per GUI-Schalter ({@code NBTControlPacket "toggle"}), Brennrate 1..10 mB/t per Schraubenzieher. Hitze je mB =
 * {@code FT_Flammable.getHeatEnergy() / 1000}; Abgas geht in die Rauchtanks und an die vier Anschluesse ({@code getConPos}).
 *
 * <p>Der Port fuehrt zusaetzlich {@code oilburner_hp}: im Original gab es dazu nur Grafiken, keinen Code. Er nutzt diese
 * Klasse mit doppeltem Tank und doppelter Brennrate.</p>
 */
public class MachineOilburnerBlockEntity extends MachinePollutingBlockEntity
        implements IFluidStandardReceiverMK2, IHeatSource, com.hbm_m.api.tile.IControlReceiver,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider, com.hbm_m.api.redstoneoverradio.IRORInteractive {

    public static final int MAX_SETTING = 10;
    public static final int maxHeatEnergy = 100_000;

    private final FluidTank tank;
    private final int burnMultiplier;

    public boolean isOn = false;
    public int setting = 1;
    public int heatEnergy;

    public MachineOilburnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OILBURNER_BE.get(), pos, state, 3, 0L, 0L, 0L, 100);

        boolean isHp = state.is(ModBlocks.OILBURNER_HP.get());
        this.tank = new FluidTank(ModFluids.HEATINGOIL.getSource(), isHp ? 32_000 : 16_000);
        this.burnMultiplier = isHp ? 2 : 1;
    }

    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getSendingTanks() { return getSmokeTanks(); }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    public FluidTank getOilTank() { return tank; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineOilburnerBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        ItemStack[] slots = new ItemStack[3];
        for (int i = 0; i < 3; i++) slots[i] = inventory.getStackInSlot(i);
        boolean changed = tank.loadTank(0, 1, slots);
        changed |= tank.setType(2, slots);
        if (changed) for (int i = 0; i < 3; i++) inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos con = pos.relative(dir, 2);
            this.trySubscribe(tank.getTankType(), level, con, dir);
            this.sendSmoke(level, con, dir);
        }

        boolean shouldCool = true;

        if (this.isOn && this.heatEnergy < maxHeatEnergy) {

            FT_Flammable type = FluidType.getTrait(tank.getTankType(), FT_Flammable.class);
            if (type != null) {

                int burnRate = setting * burnMultiplier;
                int toBurn = Math.min(burnRate, tank.getFluidAmountMb());

                tank.drainMb(toBurn);

                int heat = (int) (type.getHeatEnergy() / 1000);

                this.heatEnergy += heat * toBurn;

                if (level.getGameTime() % 5 == 0 && toBurn > 0) {
                    super.pollute(tank.getTankType(), FluidReleaseType.BURN, toBurn * 5);
                }

                shouldCool = false;
            }
        }

        if (this.heatEnergy >= maxHeatEnergy)
            shouldCool = false;

        if (shouldCool)
            this.heatEnergy = Math.max(this.heatEnergy - Math.max(this.heatEnergy / 1000, 1), 0);

        setChanged();
        sendUpdateToClient();
    }

    /** Original {@code toggleSetting}. */
    public void cycleSetting() {
        setting++;
        if (setting > MAX_SETTING) setting = 1;
        setChanged();
    }

    public int getSetting() { return setting; }

    /** GUI: Original zeigt Flamme/Schalter nach {@code isOn}. */
    public boolean isBurning() { return isOn; }

    @Override public boolean hasPermission(Player player) { return player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) <= 256; }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            this.isOn = !this.isOn;
        }
        setChanged();
    }

    @Override public int getHeatStored() { return heatEnergy; }
    @Override public int getMaxHeatStored() { return maxHeatEnergy; }

    @Override
    public void useUpHeat(int amount) {
        heatEnergy = Math.max(0, heatEnergy - amount);
        setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.oilburner");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineOilburnerMenu.create(id, inventory, this);
    }

    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putBoolean("isOn", isOn);
        tag.putInt("heatEnergy", heatEnergy);
        tag.putByte("setting", (byte) setting);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "tank");
        isOn = tag.getBoolean("isOn");
        heatEnergy = tag.getInt("heatEnergy");
        setting = tag.contains("setting") ? tag.getByte("setting") : 1;
        if (setting < 1) setting = 1;
    }

    // ── Redstone-over-Radio (1:1 TileEntityHeaterOilburner) ──

    /** 1:1 inklusive Original-Eigenheit: gelistet als "burnRate", abgefragt wird (kleingeschrieben) "burnrate". */
    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "heat",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "burnRate",
                PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "active",
                PREFIX_FUNCTION + "setburnrate" + NAME_SEPARATOR + "rate"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name))     return "" + heatEnergy;
        if ((PREFIX_VALUE + "fuel").equals(name))     return "" + tank.getFill();
        if ((PREFIX_VALUE + "burnrate").equals(name)) return "" + setting;
        if ((PREFIX_VALUE + "state").equals(name))    return isOn ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // Original ohne Laengenpruefung (ein leerer Befehl haette den Server-Tick abgebrochen)
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            this.isOn = params[0].equals("1");
            this.setChanged();
            this.sendUpdateToClient();
            return null;
        }
        if ((PREFIX_FUNCTION + "setburnrate").equals(name) && params.length > 0) {
            int rate = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 1, 10);
            this.setting = rate;
            this.setChanged();
            this.sendUpdateToClient();
            return null;
        }
        return null;
    }
}
