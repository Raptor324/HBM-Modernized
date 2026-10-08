package com.hbm_m.blockentity.machines;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineEPressMenu;
import com.hbm_m.recipe.PressRecipe;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * E-Press (Electric Press) - Port von {@code TileEntityMachineEPress} (1.7.10 Original). Fast
 * identisch zum bereits portierten {@link MachinePressBlockEntity} (gleiche {@link PressRecipe}-
 * Tabelle, gleiche Stempel/Material/Output-Slot-Rollen, gleiche Retract/Delay-Zustandsmaschine),
 * aber Batterie-Strom statt Brennstoff-Slot: kein {@code burnTime}/{@code speed}-Hitze-Hochlauf,
 * stattdessen fester Press-/Retract-Vorschub, der pro Tick {@link #POWER_PER_TICK} Energie zieht
 * (1:1 aus dem Original: {@code power >= 100} pro Schritt statt Brennstoff-Pauschale pro fertiger
 * Operation).
 * <p>
 * <p><b>Aufwertungen</b> wie im Original, Platz 4, nur Tempo bis Stufe 3: der Stempel faehrt je
 * Stufe schneller ({@code Grundwert * (1 + speed/4)} mit {@code speed = 1 + Stufe}) und die Pause
 * zwischen zwei Huben wird kuerzer ({@code 5 - speed + 1}). Drei Stufen halbieren die Zeit je Hub
 * ungefaehr.</p>
 *
 * <p>Die Kopfanimation des Original-Renderers zeichnet {@code EPressRenderer} aus dem synchronisierten
 * {@code press} ({@code renderPress/lastPress} wie im Original ueber zwei Ticks nachgezogen).
 */
public class MachineEPressBlockEntity extends BaseMachineBlockEntity {

    /** ContainerMachineEPress carries a SlotUpgrade (index 4); this port had none, so the press
     *  could never take speed or power upgrades even though the upgrade system exists. */
    private static final int SLOT_COUNT = 5;
    private static final int SLOT_UPGRADE = 4;
    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_STAMP = 1;
    private static final int SLOT_MATERIAL = 2;
    private static final int SLOT_OUTPUT = 3;

    /** Original: {@code getValidUpgrades} kennt hier ausschliesslich Tempo bis Stufe 3. */
    private static final java.util.Map<UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(
            UpgradeType.SPEED, 3);

    private final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();

    private static final long MAX_POWER = 50_000L;
    private static final long POWER_PER_TICK = 100L;
    private static final int MAX_PRESS = 200;
    /** Original: {@code stampSpeed = isRetracting ? 20 : 45}, danach mal {@code 1 + speed/4}. */
    private static final int EXTEND_SPEED = 45;
    private static final int RETRACT_SPEED = 20;

    private int press = 0;
    private boolean isRetracting = false;
    /** Original Client-Seite: {@code renderPress/lastPress/turnProgress} fuer RenderEPress. */
    public double renderPress;
    public double lastPress;
    private int turnProgress;
    private int delay = 0;
    private int pressPosition = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> press;
                case 1 -> MAX_PRESS;
                case 2 -> (int) Math.min(Integer.MAX_VALUE, getEnergyStored());
                case 3 -> (int) Math.min(Integer.MAX_VALUE, getMaxEnergyStored());
                case 4 -> pressPosition;
                case 5 -> isRetracting ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> press = value;
                case 4 -> pressPosition = value;
                case 5 -> isRetracting = value == 1;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public MachineEPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EPRESS_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineEPressBlockEntity be) {
        if (level.isClientSide()) {
            be.lastPress = be.renderPress;
            if (be.turnProgress > 0) {
                be.renderPress = be.renderPress + ((be.press - be.renderPress) / (double) be.turnProgress);
                --be.turnProgress;
            } else {
                be.renderPress = be.press;
            }
            return;
        }
        be.serverTick();
    }

    private void serverTick() {
        if (level == null) return;

        boolean needsSync = false;
        chargeFromBatterySlot(SLOT_BATTERY);
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE, SLOT_UPGRADE, VALID_UPGRADES);

        boolean canProcess = canProcess();

        // 1:1 Original: 100 HE je Tick, solange gearbeitet, eingefahren oder pausiert wird - unabhaengig vom Tempo
        if ((canProcess || isRetracting || delay > 0) && getEnergyStored() >= POWER_PER_TICK) {
            setEnergyStored(getEnergyStored() - POWER_PER_TICK);

            if (delay <= 0) {
                upgradeManager.checkSlots(inventory, SLOT_UPGRADE, SLOT_UPGRADE, VALID_UPGRADES);
                int speed = 1 + upgradeManager.getLevel(UpgradeType.SPEED);

                int stampSpeed = isRetracting ? RETRACT_SPEED : EXTEND_SPEED;
                stampSpeed = (int) (stampSpeed * (1D + (double) speed / 4D));

                if (isRetracting) {
                    press -= stampSpeed;
                    if (press <= 0) {
                        isRetracting = false;
                        delay = 5 - speed + 1;
                    }
                } else if (canProcess) {
                    press += stampSpeed;
                    if (press >= MAX_PRESS) {
                        craftItem();
                        isRetracting = true;
                        delay = 5 - speed + 1;
                    }
                } else if (press > 0) {
                    isRetracting = true;
                }
            } else {
                delay--;
            }
            needsSync = true;
        }

        pressPosition = Math.max(0, Math.min(20, (press * 20) / MAX_PRESS));

        if (needsSync) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private void craftItem() {
        Optional<PressRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty() || level == null) return;

        ItemStack output = recipe.get().getResultItem(level.registryAccess());

        inventory.extractItem(SLOT_MATERIAL, 1, false);

        ItemStack outputSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (outputSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, output.copy());
        } else {
            outputSlot.grow(output.getCount());
        }

        ItemStack stamp = inventory.getStackInSlot(SLOT_STAMP);
        if (stamp.isDamageableItem()) {
            stamp.setDamageValue(stamp.getDamageValue() + 1);
            if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
                inventory.setStackInSlot(SLOT_STAMP, ItemStack.EMPTY);
                level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.5f, 0.8f);
            }
        }

        level.playSound(null, worldPosition, ModSounds.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.5f, 1.0f);
    }

    private boolean canProcess() {
        if (getEnergyStored() < POWER_PER_TICK) return false;
        if (inventory.getStackInSlot(SLOT_STAMP).isEmpty() || inventory.getStackInSlot(SLOT_MATERIAL).isEmpty()) return false;

        Optional<PressRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty() || level == null) return false;

        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        ItemStack outputSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (outputSlot.isEmpty()) return true;

        return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize()
                && outputSlot.is(result.getItem())
                && outputSlot.getDamageValue() == result.getDamageValue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<PressRecipe> getCurrentRecipe() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }

        // 1.21.1: Recipe.matches требует RecipeInput, а рецепты завёрнуты в RecipeHolder —
        // используем RecipeHooks.getAllRecipes + matchesRecipe(RecipeInputWrapper).
        for (PressRecipe recipe : com.hbm_m.platform.recipe.RecipeHooks.getAllRecipes(level, (RecipeType<PressRecipe>) (RecipeType<?>) PressRecipe.Type.INSTANCE)) {
            if (recipe.matchesRecipe(new com.hbm_m.platform.recipe.RecipeInputWrapper(container), level)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, container);
        }
    }

    public ContainerData getBlockEntityData() {
        return this.data;
    }

    public ItemStack getMaterialStack() {
        return inventory.getStackInSlot(SLOT_MATERIAL);
    }

    public ItemStack getStampStack() {
        return inventory.getStackInSlot(SLOT_STAMP);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 3, worldPosition.getZ() + 1);
    }

    /** Original deserialize: {@code turnProgress = 2} bei jedem Paket. */
    @Override
    protected void applyClientUpdate(CompoundTag tag) {
        super.applyClientUpdate(tag);
        this.turnProgress = 2;
    }

    /** Original {@code syncStack = slots[2]} - das Material unter dem Stempel. */
    public ItemStack getSyncStack() {
        return inventory.getStackInSlot(SLOT_MATERIAL);
    }

    /** Original {@code maxPress}. */
    public static int getMaxPress() {
        return MAX_PRESS;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("press", press);
        tag.putBoolean("isRetracting", isRetracting);
        tag.putInt("delay", delay);
        tag.putInt("pressPosition", pressPosition);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        press = tag.getInt("press");
        isRetracting = tag.getBoolean("isRetracting");
        delay = tag.getInt("delay");
        pressPosition = tag.getInt("pressPosition");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.epress");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            case SLOT_STAMP -> true;
            case SLOT_MATERIAL -> true;
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineEPressMenu(containerId, playerInventory, this, this.data);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {1, 2, 3}; Stempel nur in 1, alles andere in 2, nur das Ergebnis heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 1, 2, 3 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return stack.getItem() instanceof com.hbm_m.item.industrial.ItemStamp ? slot == 1 : slot == 2; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 3; }
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
