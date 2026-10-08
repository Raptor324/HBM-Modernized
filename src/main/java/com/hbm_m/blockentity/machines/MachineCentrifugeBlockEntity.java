package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineCentrifugeMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityMachineCentrifuge}: 100000 HE Speicher, 200 Ticks pro Durchlauf, Grundverbrauch 200 HE/t.
 * Aufwertungen (Slots 6-7, je max. 3): Tempo +1 Fortschritt/t und +100 % Verbrauch je Stufe, Overdrive
 * x(1 + 5*Stufe) Tempo und +5000 % Verbrauch je Stufe, Sparsamkeit teilt den Verbrauch durch (1 + Stufe).
 * Port-Slotlage (GUI): 0 Batterie, 1 Eingang, 2-5 Ausgaenge, 6-7 Aufwertungen (Original: 0 Eingang, 1 Batterie).
 */
public class MachineCentrifugeBlockEntity extends BaseMachineBlockEntity {

    private static final int BATTERY_SLOT = 0;
    private static final int INPUT_SLOT = 1;
    private static final int OUTPUT_SLOT_START = 2;
    private static final int OUTPUT_SLOTS = 4;
    private static final int UPGRADE_SLOT_START = 6;
    private static final int UPGRADE_SLOT_END = 7;
    // TODO(port): GUI zeigt die Aufwertungsplaetze 6-7 noch nicht an (Menue gehoert dem GUI-Agenten).
    private static final int TOTAL_SLOTS = 8;

    /** Original (konfigurierbar): {@code maxPower = 100000}, {@code processingSpeed = 200}, {@code baseConsumption = 200}. */
    public static final long MAX_POWER = 100_000L;
    public static final int PROCESSING_SPEED = 200;
    public static final int BASE_CONSUMPTION = 200;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3,
            UpgradeType.OVERDRIVE, 3);

    private final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();

    private int progress = 0;
    public boolean isProgressing;
    /** Original: clientseitige Hochlaufzeit des Betriebsgeraeuschs (0-60). */
    private int audioDuration = 0;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PROCESSING_SPEED;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // read-only
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MachineCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE_BE.get(), pos, state, TOTAL_SLOTS, MAX_POWER, MAX_POWER);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.centrifuge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= OUTPUT_SLOT_START && slot < OUTPUT_SLOT_START + OUTPUT_SLOTS) {
            return false;
        }
        if (slot == BATTERY_SLOT) {
            return isEnergyProviderItem(stack);
        }
        if (slot >= UPGRADE_SLOT_START) {
            return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
        }
        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCentrifugeMenu(containerId, playerInventory, this, containerData);
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, container);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCentrifugeBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.clientTick();
            return;
        }

        blockEntity.ensureNetworkInitialized();
        blockEntity.chargeFromBattery();

        if (level.getGameTime() % 10L == 0L) {
            blockEntity.updateEnergyDelta(blockEntity.getEnergyStored());
        }

        // 1:1 Original updateEntity
        int consumption = BASE_CONSUMPTION;
        int speed = 1;

        blockEntity.upgradeManager.checkSlots(blockEntity.inventory, UPGRADE_SLOT_START, UPGRADE_SLOT_END, VALID_UPGRADES);
        speed += blockEntity.upgradeManager.getLevel(UpgradeType.SPEED);
        consumption += blockEntity.upgradeManager.getLevel(UpgradeType.SPEED) * BASE_CONSUMPTION;

        speed *= (1 + blockEntity.upgradeManager.getLevel(UpgradeType.OVERDRIVE) * 5);
        consumption += blockEntity.upgradeManager.getLevel(UpgradeType.OVERDRIVE) * BASE_CONSUMPTION * 50;

        consumption /= (1 + blockEntity.upgradeManager.getLevel(UpgradeType.POWER));

        boolean hasPower = blockEntity.getEnergyStored() > 0;

        if (hasPower && blockEntity.progress > 0) {
            blockEntity.setEnergyStored(Math.max(0L, blockEntity.getEnergyStored() - consumption));
        }

        blockEntity.isProgressing = blockEntity.getEnergyStored() > 0 && blockEntity.canProcess();

        if (blockEntity.isProgressing) {
            blockEntity.progress += speed;

            if (blockEntity.progress >= PROCESSING_SPEED) {
                blockEntity.progress = 0;
                blockEntity.finishCycle();
            }
        } else {
            blockEntity.progress = 0;
        }

        blockEntity.setChanged();
        blockEntity.sendUpdateToClient();
    }

    /** Original: {@code getLoopedSound(CENTRIFUGE_LOOP, 1F, 10F, 1F, 20)}, Tonhoehe {@code (audioDuration - 10) / 100 + 0.5}. */
    private void clientTick() {
        if (isProgressing) audioDuration += 2;
        else audioDuration -= 3;
        audioDuration = Math.max(0, Math.min(audioDuration, 60));

        com.hbm_m.client.sound.MachineLoopSoundClient.tick(this, "hbm:block.centrifugeOperate", audioDuration > 10, 1.0F,
                (audioDuration - 10) / 100F + 0.5F, 25);
    }

    private boolean canProcess() {
        var recipe = findMatchingRecipe();
        if (recipe == null) {
            return false;
        }

        ItemStack[] outputs = recipe.getOutputs();

        for (int i = 0; i < OUTPUT_SLOTS && i < outputs.length; i++) {
            ItemStack result = outputs[i];
            if (result.isEmpty()) {
                continue;
            }

            ItemStack outputSlot = inventory.getStackInSlot(OUTPUT_SLOT_START + i);
            if (outputSlot.isEmpty()) {
                continue;
            }

            if (!PlatformHooks.isSameItemSameTags(outputSlot, result)) {
                return false;
            }

            if (outputSlot.getCount() + result.getCount() > result.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    private void finishCycle() {
        var recipe = findMatchingRecipe();
        if (recipe == null) {
            return;
        }

        ItemStack[] outputs = recipe.getOutputs();

        for (int i = 0; i < OUTPUT_SLOTS && i < outputs.length; i++) {
            ItemStack result = outputs[i];
            if (result.isEmpty()) continue;

            int slot = OUTPUT_SLOT_START + i;
            ItemStack outputSlot = inventory.getStackInSlot(slot);
            if (outputSlot.isEmpty()) {
                inventory.setStackInSlot(slot, result.copy());
            } else if (PlatformHooks.isSameItemSameTags(outputSlot, result)) {
                outputSlot.grow(result.getCount());
            }
        }

        inventory.getStackInSlot(INPUT_SLOT).shrink(1);
    }

    @Nullable
    private com.hbm_m.recipe.CentrifugeRecipe findMatchingRecipe() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || level == null) {
            return null;
        }

        // 1.21.1: getRecipeFor требует RecipeInput, а SimpleContainer им не является —
        // используем устоявшийся в проекте паттерн: getAllRecipes + matchesRecipe(RecipeInputWrapper).
        com.hbm_m.platform.recipe.RecipeInputWrapper wrapper =
                new com.hbm_m.platform.recipe.RecipeInputWrapper(new net.minecraft.world.SimpleContainer(input));
        return com.hbm_m.platform.recipe.RecipeHooks
                .getAllRecipes(level, com.hbm_m.recipe.ModRecipes.CENTRIFUGE_TYPE.get()).stream()
                .filter(r -> r.matchesRecipe(wrapper, level))
                .findFirst()
                .orElse(null);
    }

    private void chargeFromBattery() {
        chargeFromBatterySlot(BATTERY_SLOT);
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putShort("progress", (short) progress);
        tag.putBoolean("isProgressing", isProgressing);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getShort("progress");
        isProgressing = tag.getBoolean("isProgressing");
        // alte Welten hatten 50000 HE gespeichert
        setEnergyCapacity(MAX_POWER);
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /** Original {@code getAccessibleSlotsFromSide {Eingang, 2-5}}: nur der Eingang ist befuellbar, nur die Ausgaenge entnehmbar. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return TOTAL_SLOTS; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (slot != INPUT_SLOT) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot < OUTPUT_SLOT_START || slot >= OUTPUT_SLOT_START + OUTPUT_SLOTS) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return slot == INPUT_SLOT; }
            })).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sided.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        sided.clear();
    }
    //?} elif neoforge {
    /*private final Map<Direction, com.hbm_m.platform.LazyCap<net.neoforged.neoforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /^* Original {@code getAccessibleSlotsFromSide {Eingang, 2-5}}: nur der Eingang ist befuellbar, nur die Ausgaenge entnehmbar. ^/
    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> com.hbm_m.platform.LazyCap.of(() -> new net.neoforged.neoforge.items.IItemHandler() {
                @Override public int getSlots() { return TOTAL_SLOTS; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (slot != INPUT_SLOT) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot < OUTPUT_SLOT_START || slot >= OUTPUT_SLOT_START + OUTPUT_SLOTS) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return slot == INPUT_SLOT; }
            })).cast();
        }
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sided.values().forEach(com.hbm_m.platform.LazyCap::invalidate);
        sided.clear();
    }
    *///?}
}
