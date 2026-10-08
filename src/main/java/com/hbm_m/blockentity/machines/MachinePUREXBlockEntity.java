package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachinePUREXMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.module.machine.MachineModulePurex;
import com.hbm_m.recipe.PurexRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachinePUREX}: 13 Slots (Batterie, Blueprint-Ordner, zwei Upgrades, drei Eingaenge, sechs
 * Ausgaenge), drei Eingangs- und ein Ausgangstank zu je 24.000 mB, Rezept per Rezeptwaehler. Der Energiespeicher
 * waechst mit dem Rezept ({@code power * 100}, mindestens 1.000.000). Upgrades: Tempo/Strom/Overdrive je bis 3.
 * Zwanzig Anschluesse rund um den Sockelrand. Clientseitig dreht sich der Luefter, pumpt der Kolben und das Geruest
 * erscheint, sobald ueber der Anlage ein Block sitzt.
 */
public class MachinePUREXBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2,
        com.hbm_m.interfaces.IUpgradeInfoProvider, IRORValueProvider {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_UPGRADE_1 = 2;
    public static final int SLOT_UPGRADE_2 = 3;
    public static final int ITEM_INPUT_START = 4;
    public static final int ITEM_OUTPUT_START = 7;
    public static final int SLOT_COUNT = 13;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    public final FluidTank[] inputTanks = new FluidTank[3];
    public final FluidTank[] outputTanks = new FluidTank[1];

    public boolean didProcess = false;

    public boolean frame = false;
    public int anim;
    public int prevAnim;

    public final MachineModulePurex purexModule;
    public final UpgradeManager upgradeManager = new UpgradeManager();

    public MachinePUREXBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUREX_BE.get(), pos, state, SLOT_COUNT, 1_000_000L, 1_000_000L);
        for (int i = 0; i < 3; i++) inputTanks[i] = new FluidTank(ModFluids.NONE.getSource(), 24_000);
        outputTanks[0] = new FluidTank(ModFluids.NONE.getSource(), 24_000);

        this.purexModule = new MachineModulePurex(this, inventory,
                new int[] { 4, 5, 6 }, new int[] { 7, 8, 9, 10, 11, 12 },
                inputTanks, outputTanks, null);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachinePUREXBlockEntity be) {
        be.purexModule.setLevel(level);
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel, pos);
        else be.clientTick(level, pos);
    }

    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        BlockPos p = worldPosition;
        DirPos[] out = new DirPos[20];
        int n = 0;
        for (int i = -2; i <= 2; i++) out[n++] = new DirPos(p.offset(3, 0, i), Direction.EAST);
        for (int i = -2; i <= 2; i++) out[n++] = new DirPos(p.offset(-3, 0, i), Direction.WEST);
        for (int i = -2; i <= 2; i++) out[n++] = new DirPos(p.offset(i, 0, 3), Direction.SOUTH);
        for (int i = -2; i <= 2; i++) out[n++] = new DirPos(p.offset(i, 0, -3), Direction.NORTH);
        return out;
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        PurexRecipe recipe = purexModule.peekRecipe();
        long maxPower = 1_000_000L;
        if (recipe != null) maxPower = (long) recipe.getPowerConsumption() * 100L;
        maxPower = Math.max(Math.max(energy, maxPower), 1_000_000L);
        if (maxPower != getMaxEnergyStored()) setEnergyCapacity(maxPower);

        chargeFromBatterySlot(SLOT_BATTERY);
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);

        for (DirPos con : getConPos()) {
            this.trySubscribe(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            for (FluidTank tank : inputTanks) if (tank.getTankType() != ModFluids.NONE.getSource()) this.trySubscribe(tank.getTankType(), world, con.pos, con.dir);
            for (FluidTank tank : outputTanks) if (tank.getFill() > 0) this.tryProvide(tank, world, con.pos, con.dir);
        }

        double speed = 1D;
        double pow = 1D;

        speed += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        pow -= Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        pow += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) * 1D;
        pow += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;

        boolean dirty = this.purexModule.updateAndGetDirty(speed, pow, true, inventory.getStackInSlot(SLOT_BLUEPRINT));
        this.didProcess = this.purexModule.getDidProcess();
        if (dirty) setChanged();

        sendUpdateToClient();
    }

    private void clientTick(Level world, BlockPos pos) {
        this.prevAnim = this.anim;
        if (this.didProcess) this.anim++;

        if (world.getGameTime() % 20 == 0) {
            frame = !world.getBlockState(pos.above(5)).isAir();
        }
    }

    // ==================== Rezeptwahl ====================

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return purexModule.getSelectedRecipeId();
    }

    @Nullable
    public PurexRecipe getSelectedRecipe() {
        return purexModule.peekRecipe();
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        purexModule.setSelectedRecipe(recipeId);
        if (level != null && !level.isClientSide) purexModule.syncTankConfigurationToRecipe(level);
        setChanged();
        if (level != null && !level.isClientSide) sendUpdateToClient();
    }

    public ItemStack getBlueprintFolder() {
        return inventory.getStackInSlot(SLOT_BLUEPRINT);
    }

    /** Original {@code GUIScreenRecipeSelector}: alle Rezepte, deren Pool frei oder im Ordner installiert ist. */
    public List<PurexRecipe> getAvailableRecipes() {
        if (level == null) return List.of();
        String installedPool = com.hbm_m.item.industrial.ItemBlueprints.getBlueprintPool(getBlueprintFolder());
        return com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager()).getAll(PurexRecipe.Type.INSTANCE).stream().filter(r -> {
            String pool = r.getBlueprintPool();
            if (pool == null || pool.isEmpty()) return true;
            return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
        }).toList();
    }

    /** Fortschritt 0..1 wie {@code purexModule.progress} im Original. */
    public double getProgressFraction() {
        return purexModule.getProgressPercent();
    }

    // ==================== Slots ====================

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT && stack.getItem() instanceof com.hbm_m.item.industrial.ItemBlueprints) return true;
        if (slot >= SLOT_UPGRADE_1 && slot <= SLOT_UPGRADE_2 && stack.getItem() instanceof ItemMachineUpgrade) return true;
        return this.purexModule != null && this.purexModule.isItemValidForSlot(slot, stack);
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTank[] getSendingTanks() { return outputTanks; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { inputTanks[0], inputTanks[1], inputTanks[2], outputTanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < 3; i++) inputTanks[i].writeToNBT(tag, "i" + i);
        outputTanks[0].writeToNBT(tag, "o" + 0);
        tag.putLong("power", energy);
        tag.putLong("maxPower", getMaxEnergyStored());
        tag.putBoolean("didProcess", didProcess);
        purexModule.writeNBT(tag);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < 3; i++) inputTanks[i].readFromNBT(tag, "i" + i);
        outputTanks[0].readFromNBT(tag, "o" + 0);
        if (tag.contains("maxPower")) setEnergyCapacity(Math.max(1, tag.getLong("maxPower")));
        if (tag.contains("power")) energy = tag.getLong("power");
        purexModule.readNBT(tag);
        didProcess = tag.getBoolean("didProcess");
        purexModule.didProcess = didProcess;
    }

    // ==================== Upgrades ====================

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_SPEED, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ==================== ROR ====================

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "progress",
                PREFIX_VALUE + "recipe",
                PREFIX_VALUE + "active",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "progress").equals(name)) return "" + (int) Math.round(getProgressFraction() * 100);
        if ((PREFIX_VALUE + "recipe").equals(name)) return com.hbm_m.api.redstoneoverradio.RORRecipeNames.name(getSelectedRecipeId());
        if ((PREFIX_VALUE + "active").equals(name)) return "" + (this.didProcess ? 1 : 0);
        return null;
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.purex");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachinePUREXMenu(containerId, playerInventory, this);
    }

    /** Original: 5x5x5 um den Kern. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 5, worldPosition.getZ() + 3);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {4-12}; Zutaten nach Rezept hinein, Ausgaben 7-12 und verstopfte Eingaenge heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(4, 12); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return (slot >= 7 && slot <= 12) || purexModule.isSlotClogged(slot); }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}
}
