package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineMicrowaveMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Microwave: Direktport der Kernlogik aus {@code TileEntityMicrowave} (1.7.10 Original).
 * <p>
 * Vereinfachung: das Original nutzt Vanilla {@code FurnaceRecipes.smelting()} gefiltert auf
 * {@code ItemFood}-Eingaben/-Ergebnisse - hier 1:1 uebernommen ueber die moderne
 * {@code RecipeType.SMELTING}-Suche mit einem zusaetzlichen {@code isEdible()}-Filter auf
 * Eingang UND Ergebnis (entspricht {@code instanceof ItemFood} Check im Original). Das
 * OpenComputers-Interface des Originals entfaellt (dieser Port hat keine OC-Integration).
 * <p>
 * WICHTIG - 1:1 aus dem Original uebernommenes Verhalten: bei {@code speed == maxSpeed} (5)
 * EXPLODIERT die Maschine sofort sobald Verarbeitung moeglich waere, statt zu verarbeiten -
 * dies ist im Original ein absichtlicher Bestrafungsmechanismus fuer die hoechste Geschwindigkeits-
 * stufe, keine Fehlportierung.
 */
public class MachineMicrowaveBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BATTERY = 2;
    private static final int SLOT_COUNT = 3;

    private static final long MAX_POWER = 50_000L;
    private static final long CONSUMPTION = 50L;
    private static final int MAX_TIME = 300;
    public static final int MAX_SPEED = 5;

    public int time = 0;
    public int speed = 0;

    private final SimpleContainer recipeInput = new SimpleContainer(1);

    public MachineMicrowaveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MICROWAVE_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMicrowaveBlockEntity be) {
        if (level.isClientSide()) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        if (be.canProcess(level)) {
            if (be.speed >= MAX_SPEED) {
                level.removeBlock(pos, false);
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.5F, Level.ExplosionInteraction.BLOCK);
                return;
            }

            if (be.time >= MAX_TIME) {
                be.process(level);
                be.time = 0;
            }

            if (be.canProcess(level)) {
                be.setEnergyStored(Math.max(0, be.energy - CONSUMPTION));
                be.time += be.speed * 2;
            }
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    public void adjustSpeed(int delta) {
        speed = Math.max(0, Math.min(MAX_SPEED, speed + delta));
        setChanged();
    }

    private void process(Level level) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return;

        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();
        inventory.extractItem(SLOT_INPUT, 1, false);

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(result.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, output);
        }
    }

    private boolean canProcess(Level level) {
        if (speed == 0) return false;
        if (energy < CONSUMPTION) return false;

        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return false;

        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        if (result.isEmpty()) return false;
        if (!com.hbm_m.platform.PlatformHooks.isEdible(input) && !com.hbm_m.platform.PlatformHooks.isEdible(result)) return false;

        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) return true;
        if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= result.getMaxStackSize();
    }

    private java.util.Optional<SmeltingRecipe> getRecipe(Level level, ItemStack input) {
        // 1.21.1: getRecipeFor требует RecipeInput (SingleRecipeInput) и возвращает RecipeHolder.
        return com.hbm_m.platform.recipe.RecipeHooks.getRecipeFor(level, RecipeType.SMELTING, input);
    }

    public long getPowerScaled(int scale) {
        return capacity > 0 ? (energy * scale) / capacity : 0;
    }

    public int getProgressScaled(int scale) {
        return (time * scale) / MAX_TIME;
    }

    public int getSpeedScaled(int scale) {
        return (speed * scale) / MAX_SPEED;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("time", time);
        tag.putInt("speed", speed);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        time = tag.getInt("time");
        speed = tag.getInt("speed");
    }

    // ==================== GUI ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.microwave");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> true;
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineMicrowaveMenu.create(id, inv, this);
    }
}
