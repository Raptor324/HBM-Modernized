package com.hbm_m.blockentity.machines;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
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
 * SCOPE-Entscheidung: Der Upgrade-Slot (SPEED-Upgrades Stufe 1-3) des Originals entfaellt
 * (konsistent mit der durchgaengigen Upgrade-System-Streichung in diesem Port, siehe
 * {@code MachineMiningDrillBlockEntity}) - {@link #EXTEND_SPEED}/{@link #RETRACT_SPEED} sind die
 * Original-Werte bei Upgrade-Stufe 0 (bereits mit dem Original-Multiplikator {@code 1+level/4}
 * vorverrechnet). Ebenso nicht uebernommen: die animierte Kopf-3D-Bewegung (Original-Renderer) -
 * das statische, kombinierte {@code epress.json}-Modell wird ohne Animation dargestellt.
 */
public class MachineEPressBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_COUNT = 4;
    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_STAMP = 1;
    private static final int SLOT_MATERIAL = 2;
    private static final int SLOT_OUTPUT = 3;

    private static final long MAX_POWER = 50_000L;
    private static final long POWER_PER_TICK = 100L;
    private static final int MAX_PRESS = 200;
    private static final int EXTEND_SPEED = 56;
    private static final int RETRACT_SPEED = 25;

    private int press = 0;
    private boolean isRetracting = false;
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
        if (level.isClientSide()) return;
        be.serverTick();
    }

    private void serverTick() {
        if (level == null) return;

        boolean needsSync = false;
        chargeFromBatterySlot(SLOT_BATTERY);

        boolean canProcess = canProcess();

        if (delay <= 0) {
            if ((canProcess || isRetracting) && getEnergyStored() >= POWER_PER_TICK) {
                setEnergyStored(getEnergyStored() - POWER_PER_TICK);

                if (isRetracting) {
                    press -= RETRACT_SPEED;
                    if (press <= 0) {
                        press = 0;
                        isRetracting = false;
                        delay = 5;
                    }
                } else {
                    press += EXTEND_SPEED;
                    if (press >= MAX_PRESS) {
                        press = MAX_PRESS;
                        craftItem();
                        isRetracting = true;
                        delay = 5;
                    }
                }
                needsSync = true;
            } else if (!canProcess && press > 0 && !isRetracting) {
                isRetracting = true;
            }
        } else {
            delay--;
        }

        pressPosition = Math.min(20, (press * 20) / MAX_PRESS);

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

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putInt("press", press);
        tag.putBoolean("isRetracting", isRetracting);
        tag.putInt("delay", delay);
        tag.putInt("pressPosition", pressPosition);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
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
}
