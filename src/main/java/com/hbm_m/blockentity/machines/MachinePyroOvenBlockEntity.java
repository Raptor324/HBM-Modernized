package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.menu.MachinePyroOvenMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.PyroOvenRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachinePyroOven}: Pyrolyseofen. Strom und Fluid kommen ueber fuenf Anschluesse an der
 * Laengsseite ({@code rot * 3}, {@code rot = dir.getRotation(DOWN)}), das Produktfluid geht dort hinaus, der Rauch
 * oben aus dem Schornstein ({@code -rot}, drei Bloecke hoch). Upgrades Geschwindigkeit/Energiesparen/Overdrive.
 * Auf dem Client laufen Schieber und Luefter ({@code anim}), der Betriebsloop und Dampfwolken ueber den Oeffnungen;
 * laeuft der Rauchpuffer ueber, blaest der Schornstein ab.
 */
public class MachinePyroOvenBlockEntity extends com.hbm_m.blockentity.MachinePollutingBlockEntity
        implements IFluidStandardTransceiverMK2, com.hbm_m.interfaces.IUpgradeInfoProvider {

    public static final int SLOT_BATTERY  = 0;
    public static final int SLOT_ITEM_IN  = 1;
    public static final int SLOT_ITEM_OUT = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_UPGRADE_1 = 4;
    public static final int SLOT_UPGRADE_2 = 5;
    public static final int INVENTORY_SIZE = 6;

    public static final long maxPower = 10_000_000L;
    public static int consumption = 10_000;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    public boolean isVenting;
    public boolean isProgressing;
    public float progress;

    public int prevAnim;
    public int anim = 0;

    private final FluidTank tank0 = new FluidTank(24_000);
    private final FluidTank tank1 = new FluidTank(24_000);

    public final UpgradeManager upgradeManager = new UpgradeManager();

    public MachinePyroOvenBlockEntity(BlockPos pos, BlockState state) {
        // Original: super(6, 50) - sechs Slots, 50 mB Rauchpuffer je Sorte.
        super(ModBlockEntities.PYROOVEN_BE.get(), pos, state, INVENTORY_SIZE, maxPower, maxPower, 0L, 50);
        // Original pollute(): laeuft ein Rauchtank ueber, blaest der Ofen ab.
        smokeTanks.onOverflow(() -> this.isVenting = true);
    }

    /** Original {@code setInventorySlotContents}: Upgrade einstecken macht das Steckgeraeusch. */
    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                ItemStack stack = getStackInSlot(slot);
                if (level != null && !level.isClientSide && slot >= SLOT_UPGRADE_1 && slot <= SLOT_UPGRADE_2
                        && stack.getItem() instanceof ItemMachineUpgrade) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
        };
    }

    /** Original: {@code ForgeDirection.getOrientation(meta - 10)} - im Port die FACING-Richtung. */
    private Direction dir() {
        BlockState state = getBlockState();
        return state.hasProperty(com.hbm_m.block.machines.MachinePyroOvenBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.MachinePyroOvenBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachinePyroOvenBlockEntity be) {
        if (!level.isClientSide) be.serverTick((ServerLevel) level, pos);
        else be.clientTick(level, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        // Original: Library.chargeTEFromItems(slots, 0, power, maxPower)
        ItemStack battery = inventory.getStackInSlot(SLOT_BATTERY);
        if (!battery.isEmpty() && battery.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
        } else {
            chargeFromBatterySlot(SLOT_BATTERY);
        }

        ItemStack[] slots = inventorySlotArray();
        if (tank0.setType(SLOT_FLUID_ID, slots)) applySlotsArray(slots);

        for (DirPos con : getConPos()) {
            this.trySubscribe(level, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            if (tank0.getTankType() != com.hbm_m.inventory.fluid.ModFluids.NONE.getSource())
                this.trySubscribe(tank0.getTankType(), level, con.pos, con.dir);
            if (tank1.getFill() > 0) this.tryProvide(tank1, level, con.pos, con.dir);
        }

        Direction rot = dir().getCounterClockWise(); // Original: dir.getRotation(DOWN)
        if (smoke.getFill() > 0) this.tryProvide(smoke, level, pos.offset(-rot.getStepX(), 3, -rot.getStepZ()), Direction.UP);

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerSaving = upgradeManager.getLevel(UpgradeType.POWER);
        int overdrive = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        this.isProgressing = false;
        this.isVenting = false;

        if (this.canProcess()) {
            PyroOvenRecipe recipe = getMatchingRecipe();
            this.progress += 1F / Math.max((recipe.getDuration() - speed * (recipe.getDuration() / 4)) / (overdrive * 2 + 1), 1);
            this.isProgressing = true;
            setEnergyStored(getEnergyStored() - getConsumption(speed + overdrive * 2, powerSaving));

            if (progress >= 1F) {
                this.progress = 0F;
                this.finishRecipe(recipe);
                this.setChanged();
            }

            this.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);

        } else {
            this.progress = 0F;
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level level, BlockPos pos) {

        this.prevAnim = this.anim;
        Direction dir = dir();
        Direction rot = dir.getCounterClockWise();

        if (isProgressing) {
            this.anim++;

            if (com.hbm_m.particle.helper.ParticleEffectClient.localPlayerDistanceSq(pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5) < 50 * 50) {
                double bx = pos.getX() + 0.5 - rot.getStepX(), bz = pos.getZ() + 0.5 - rot.getStepZ();
                if (level.random.nextInt(20) == 0) level.addParticle(ParticleTypes.CLOUD, bx - dir.getStepX() * 0.875, pos.getY() + 3, bz - dir.getStepZ() * 0.875, 0.0, 0.05, 0.0);
                if (level.random.nextInt(20) == 0) level.addParticle(ParticleTypes.CLOUD, bx - dir.getStepX() * 2.375, pos.getY() + 3, bz - dir.getStepZ() * 2.375, 0.0, 0.05, 0.0);
                if (level.random.nextInt(20) == 0) level.addParticle(ParticleTypes.CLOUD, bx + dir.getStepX() * 0.875, pos.getY() + 3, bz + dir.getStepZ() * 0.875, 0.0, 0.05, 0.0);
                if (level.random.nextInt(20) == 0) level.addParticle(ParticleTypes.CLOUD, bx + dir.getStepX() * 2.375, pos.getY() + 3, bz + dir.getStepZ() * 2.375, 0.0, 0.05, 0.0);
            }
        }

        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, isProgressing, this::createAudioLoop);

        if (this.isVenting) {

            if (level.getGameTime() % 2 == 0) {
                CompoundTag fx = new CompoundTag();
                fx.putString("type", "tower");
                fx.putFloat("lift", 10F);
                fx.putFloat("base", 0.25F);
                fx.putFloat("max", 2.5F);
                fx.putInt("life", 100 + level.random.nextInt(20));
                fx.putInt("color", 0x202020);
                fx.putDouble("posX", pos.getX() + 0.5 - rot.getStepX());
                fx.putDouble("posY", pos.getY() + 3);
                fx.putDouble("posZ", pos.getZ() + 0.5 - rot.getStepZ());
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(fx);
            }
        }
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.PyroOvenLoopSoundFactory").getMethod("create", MachinePyroOvenBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getConsumption(int speed, int powerSaving) {
        return (int) (consumption * Math.pow(speed + 1, 2)) / (powerSaving + 1);
    }

    protected PyroOvenRecipe lastValidRecipe;

    public PyroOvenRecipe getMatchingRecipe() {
        if (lastValidRecipe != null && lastValidRecipe.matchesInputs(tank0, inventory.getStackInSlot(SLOT_ITEM_IN))) return lastValidRecipe;
        for (PyroOvenRecipe rec : RecipeHooks.getAllRecipes(getLevel(), PyroOvenRecipe.Type.INSTANCE)) {
            if (rec.matchesInputs(tank0, inventory.getStackInSlot(SLOT_ITEM_IN))) {
                lastValidRecipe = rec;
                return rec;
            }
        }
        return null;
    }

    public boolean canProcess() {
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerSaving = upgradeManager.getLevel(UpgradeType.POWER);
        if (getEnergyStored() < getConsumption(speed, powerSaving)) return false; // zu wenig Strom

        PyroOvenRecipe recipe = this.getMatchingRecipe();
        if (recipe == null) return false; // kein passendes Rezept
        if (recipe.getInputFluid() != null && tank0.getFill() < recipe.getInputFluidMb()) return false; // zu wenig Eingangsfluid
        ItemStack itemIn = inventory.getStackInSlot(SLOT_ITEM_IN);
        if (recipe.getInputItem() != null && itemIn.getCount() < recipe.getInputItemCount()) return false; // zu wenig Eingangsitem
        if (recipe.getOutputFluid() != null && recipe.getOutputFluidMb() + tank1.getFill() > tank1.getMaxFill()
                && recipe.getOutputFluid() == tank1.getTankType()) return false; // zu viel Ausgangsfluid
        ItemStack out = inventory.getStackInSlot(SLOT_ITEM_OUT);
        ItemStack result = recipe.getOutputItem();
        if (result != null && !result.isEmpty() && !out.isEmpty()) {
            if (result.getCount() + out.getCount() > out.getMaxStackSize()) return false; // zu viel Ausgangsitem
            if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(out, result)) return false; // Ausgangsitem passt nicht
        }

        return true;
    }

    public void finishRecipe(PyroOvenRecipe recipe) {
        ItemStack result = recipe.getOutputItem();
        if (result != null && !result.isEmpty()) {
            ItemStack current = inventory.getStackInSlot(SLOT_ITEM_OUT);
            if (current.isEmpty()) {
                inventory.setStackInSlot(SLOT_ITEM_OUT, result.copy());
            } else {
                current.grow(result.getCount());
                inventory.setStackInSlot(SLOT_ITEM_OUT, current);
            }
        }
        if (recipe.getOutputFluid() != null) {
            tank1.setTankType(recipe.getOutputFluid());
            tank1.setFill(tank1.getFill() + recipe.getOutputFluidMb());
        }
        if (recipe.getInputItem() != null) {
            inventory.extractItem(SLOT_ITEM_IN, recipe.getInputItemCount(), false);
        }
        if (recipe.getInputFluid() != null) {
            tank0.setFill(tank0.getFill() - recipe.getInputFluidMb());
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    protected DirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = dir.getCounterClockWise(); // Original: dir.getRotation(DOWN)
        BlockPos p = worldPosition;

        return new DirPos[] {
                new DirPos(p.relative(dir, 2).relative(rot, 3), rot),
                new DirPos(p.relative(dir, 1).relative(rot, 3), rot),
                new DirPos(p.relative(rot, 3), rot),
                new DirPos(p.relative(dir, -1).relative(rot, 3), rot),
                new DirPos(p.relative(dir, -2).relative(rot, 3), rot),
        };
    }

    // ── Inventar-Hilfen ──────────────────────────────────────────────────────

    private ItemStack[] inventorySlotArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
        }
        setChanged();
    }

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank0, tank1, smoke }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tank1, smoke }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank0 }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── Zugriff ──────────────────────────────────────────────────────────────

    public FluidTank getTank0() { return tank0; }
    public FluidTank getTank1() { return tank1; }
    public float getProgress()  { return progress; }
    public boolean isProgressing() { return isProgressing; }
    public boolean isVenting() { return isVenting; }

    // ── Upgrades ─────────────────────────────────────────────────────────────

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (Math.pow(level + 1, 2) * 100 - 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle((System.currentTimeMillis() / 500) % 2 == 0 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank0.writeToNBT(tag, "t0");
        tank1.writeToNBT(tag, "t1");
        tag.putFloat("prog", progress);
        tag.putBoolean("isVenting", isVenting);
        tag.putBoolean("isProgressing", isProgressing);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank0.readFromNBT(tag, "t0");
        tank1.readFromNBT(tag, "t1");
        progress = tag.getFloat("prog");
        isVenting = tag.getBoolean("isVenting");
        isProgressing = tag.getBoolean("isProgressing");
    }

    // ── Slots (Original: isItemValidForSlot i == 1) ───────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_ITEM_IN;
    }

    // ── Menue ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machinePyroOven");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachinePyroOvenMenu.create(id, inventory, this);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 3.5, worldPosition.getZ() + 4);
        return bb;
    }
}
