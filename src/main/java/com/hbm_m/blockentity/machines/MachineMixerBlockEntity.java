package com.hbm_m.blockentity.machines;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IUpgradeInfoProvider;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineMixerMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.recipe.MixerRecipes;
import com.hbm_m.recipe.MixerRecipes.MixerRecipe;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineMixer}: Ausgabefluid per Fluid-ID (Platz 2) bestimmt die Rezeptliste
 * ({@link MixerRecipes}), ein Klick in der GUI schaltet zwischen mehreren Rezepten desselben Ausgangs um. Bis zu zwei
 * Eingangsfluide und ein fester Stoff (Platz 1), Verbrauch 50 HE/t mit Tempo-/Strom-/Overdrive-Upgrades (Plaetze 3-4).
 * Anschluesse unten und an den vier Seiten des Kerns.
 */
public class MachineMixerBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IUpgradeInfoProvider, com.hbm_m.api.tile.IControlReceiver {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_SOLID = 1;
    public static final int SLOT_FLUID_ID = 2;
    public static final int SLOT_UPGRADE_1 = 3;
    public static final int SLOT_UPGRADE_2 = 4;
    public static final int SLOT_COUNT = 5;

    public static final long maxPower = 10_000;

    public int progress;
    public int processTime;
    public int recipeIndex;

    public float rotation;
    public float prevRotation;
    public boolean wasOn = false;

    private int consumption = 50;

    public final FluidTank[] tanks = new FluidTank[3];

    private final UpgradeManager upgradeManager = new UpgradeManager();

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3,
            UpgradeType.OVERDRIVE, 6);

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> processTime;
                case 2 -> (int) getEnergyStored();
                case 3 -> recipeIndex;
                case 4 -> wasOn ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 5; }
    };

    public MachineMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXER_BE.get(), pos, state, SLOT_COUNT, maxPower, maxPower);
        this.tanks[0] = new FluidTank(ModFluids.NONE.getSource(), 16_000);
        this.tanks[1] = new FluidTank(ModFluids.NONE.getSource(), 16_000);
        this.tanks[2] = new FluidTank(ModFluids.NONE.getSource(), 24_000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMixerBlockEntity be) {
        if (!level.isClientSide) be.serverTick(level, pos);
        else be.clientTick();
    }

    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();

        ItemStack battery = inventory.getStackInSlot(SLOT_BATTERY);
        if (!battery.isEmpty() && battery.getItem() instanceof ItemCreativeBattery) setEnergyStored(getMaxEnergyStored());
        else chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = inventory.getStackInSlot(i);
        if (tanks[2].setType(SLOT_FLUID_ID, slots)) inventory.setStackInSlot(SLOT_FLUID_ID, slots[SLOT_FLUID_ID] == null ? ItemStack.EMPTY : slots[SLOT_FLUID_ID]);

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        this.consumption = 50;

        this.consumption += speedLevel * 150;
        this.consumption -= this.consumption * powerLevel * 0.25;
        this.consumption *= (overLevel * 3 + 1);

        for (Direction d : CON_DIRS) {
            BlockPos con = pos.relative(d);
            if (FluidTank.isFluidTypeExplicitlySet(tanks[0].getTankType())) this.trySubscribe(tanks[0].getTankType(), level, con, d);
            if (FluidTank.isFluidTypeExplicitlySet(tanks[1].getTankType())) this.trySubscribe(tanks[1].getTankType(), level, con, d);
        }

        this.wasOn = this.canProcess();

        if (this.wasOn) {
            this.progress++;
            this.energy -= this.getConsumption();

            this.processTime -= this.processTime * speedLevel / 4;
            this.processTime /= (overLevel + 1);

            if (processTime <= 0) this.processTime = 1;

            if (this.progress >= this.processTime) {
                this.process();
                this.progress = 0;
            }

        } else {
            this.progress = 0;
        }

        for (Direction d : CON_DIRS) {
            if (tanks[2].getFill() > 0) this.tryProvide(tanks[2], level, pos.relative(d), d);
        }

        setChanged();
        sendUpdateToClient();
    }

    /** Original getConPos: unten und die vier Seiten. */
    private static final Direction[] CON_DIRS = { Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH };

    private void clientTick() {
        this.prevRotation = this.rotation;

        if (this.wasOn) {
            this.rotation += 20F;
        }

        if (this.rotation >= 360) {
            this.rotation -= 360;
            this.prevRotation -= 360;
        }
    }

    public boolean canProcess() {

        MixerRecipe[] recipes = MixerRecipes.getOutput(tanks[2].getTankType());
        if (recipes == null || recipes.length <= 0) {
            this.recipeIndex = 0;
            return false;
        }

        this.recipeIndex = this.recipeIndex % recipes.length;
        MixerRecipe recipe = recipes[this.recipeIndex];
        if (recipe == null) {
            this.recipeIndex = 0;
            return false;
        }

        tanks[0].setTankType(recipe.input1 != null ? recipe.input1.type() : ModFluids.NONE.getSource());
        tanks[1].setTankType(recipe.input2 != null ? recipe.input2.type() : ModFluids.NONE.getSource());

        if (recipe.input1 != null && tanks[0].getFill() < recipe.input1.fill()) return false;
        if (recipe.input2 != null && tanks[1].getFill() < recipe.input2.fill()) return false;

        /* simplest check would usually go first, but fluid checks also do the setup and we want that to happen even without power */
        if (this.energy < getConsumption()) return false;

        if (recipe.output + tanks[2].getFill() > tanks[2].getMaxFill()) return false;

        if (recipe.solidInput != null) {
            ItemStack solid = inventory.getStackInSlot(SLOT_SOLID);
            if (solid.isEmpty()) return false;

            if (!recipe.solidInput.matchesRecipe(solid) || recipe.solidInput.stacksize() > solid.getCount()) return false;
        }

        this.processTime = recipe.processTime;
        return true;
    }

    protected void process() {

        MixerRecipe[] recipes = MixerRecipes.getOutput(tanks[2].getTankType());
        MixerRecipe recipe = recipes[this.recipeIndex % recipes.length];

        if (recipe.input1 != null) tanks[0].drainMb(recipe.input1.fill());
        if (recipe.input2 != null) tanks[1].drainMb(recipe.input2.fill());
        if (recipe.solidInput != null) inventory.extractItem(SLOT_SOLID, recipe.solidInput.stacksize(), false);
        tanks[2].fillMb(tanks[2].getTankType(), recipe.output);
    }

    public int getConsumption() {
        return consumption;
    }

    public ContainerData getContainerData() { return data; }
    public FluidTank[] getTanks() { return tanks; }

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[2] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0], tanks[1] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Original {@code isItemValidForSlot}: nur der feste Stoff des gewaehlten Rezepts (fuer Trichter). */
    @Override
    protected boolean isItemValidForSlot(int i, ItemStack itemStack) {
        if (i == SLOT_BATTERY || i == SLOT_FLUID_ID || i == SLOT_UPGRADE_1 || i == SLOT_UPGRADE_2) return true; // GUI-Plaetze
        MixerRecipe[] recipes = MixerRecipes.getOutput(tanks[2].getTankType());
        if (recipes == null || recipes.length <= 0) return false;

        MixerRecipe recipe = recipes[this.recipeIndex % recipes.length];
        if (recipe == null || recipe.solidInput == null) return false;

        return recipe.solidInput.matchesRecipe(itemStack);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 16 * 16;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) this.recipeIndex++;
        setChanged();
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(com.hbm_m.block.ModBlocks.MIXER.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 300) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 3, worldPosition.getZ() + 1);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putInt("progress", progress);
        nbt.putInt("processTime", processTime);
        nbt.putInt("recipe", recipeIndex);
        nbt.putBoolean("wasOn", wasOn);
        for (int i = 0; i < 3; i++) this.tanks[i].writeToNBT(nbt, i + "");
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.progress = nbt.getInt("progress");
        this.processTime = nbt.getInt("processTime");
        this.recipeIndex = nbt.getInt("recipe");
        this.wasOn = nbt.getBoolean("wasOn");
        for (int i = 0; i < 3; i++) this.tanks[i].readFromNBT(nbt, i + "");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.mixer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMixerMenu.create(id, inventory, this);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slot {1}; nur der Feststoff des Rezepts hinein, nichts heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 1 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 1 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
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
