package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineExposureChamberMenu;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.ExposureChamberRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exposure Chamber: Direktport der Kernlogik aus {@code TileEntityMachineExposureChamber}
 * (1.7.10 Original).
 * <p>
 * Vereinfachungen (gleiche Konvention wie andere Maschinen diese Session):
 * <ul>
 *   <li>Kein Item-Upgrade-System (SPEED/POWER/OVERDRIVE-Upgrade-Slots des Originals entfallen);
 *       {@code processTime}/{@code consumption} sind fest auf die Basiswerte ohne Upgrades gesetzt.</li>
 *   <li>Kein "Behaelter-Item"-Nebenprodukt-Slot: die Partikel-Items in diesem Port sind einfache
 *       {@code Item}s ohne {@code getContainerItem()}-Aequivalent (im Original waere das Feld
 *       ohnehin immer {@code null} fuer diese drei Partikeltypen - keine Funktionalitaet verloren).</li>
 * </ul>
 * Die Kernmechanik ist 1:1 erhalten: ein Partikel-Item wird verbraucht und liefert einen internen
 * Vorrat von {@link #MAX_PARTICLES} Nutzungen (kein sichtbarer Slot dafuer, siehe {@code cachedParticle}),
 * die dann nacheinander mit dem Ingredient-Slot zu Output verarbeitet werden.
 */
public class MachineExposureChamberBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_PARTICLE = 0;
    public static final int SLOT_INGREDIENT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_BATTERY = 3;
    private static final int SLOT_COUNT = 4;

    private static final long MAX_POWER = 1_000_000L;
    private static final int PROCESS_TIME = 200;
    private static final int CONSUMPTION = 10_000;
    public static final int MAX_PARTICLES = 8;

    public int progress = 0;
    public int savedParticles = 0;
    public boolean isOn = false;
    @Nullable private Item cachedParticle = null;

    public MachineExposureChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPOSURE_CHAMBER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineExposureChamberBlockEntity be) {
        if (level.isClientSide()) return;

        be.isOn = false;
        be.chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack particleStack = be.inventory.getStackInSlot(SLOT_PARTICLE);
        ItemStack ingredientStack = be.inventory.getStackInSlot(SLOT_INGREDIENT);

        // Load a fresh particle capsule into the internal cache once it's empty.
        if (be.cachedParticle == null && !particleStack.isEmpty() && !ingredientStack.isEmpty() && be.savedParticles <= 0) {
            ExposureChamberRecipe recipe = findRecipe(be, particleStack, ingredientStack);
            if (recipe != null) {
                be.cachedParticle = particleStack.getItem();
                be.inventory.extractItem(SLOT_PARTICLE, 1, false);
                be.savedParticles = MAX_PARTICLES;
            }
        }

        // Consume the cached particle against the ingredient slot.
        if (be.cachedParticle != null && be.savedParticles > 0 && be.energy >= CONSUMPTION) {
            ExposureChamberRecipe recipe = findRecipe(be, new ItemStack(be.cachedParticle), ingredientStack);

            if (recipe != null && be.canAcceptOutput(recipe.getOutput())) {
                be.progress++;
                be.setEnergyStored(be.energy - CONSUMPTION);
                be.isOn = true;

                if (be.progress >= PROCESS_TIME) {
                    be.progress = 0;
                    be.savedParticles--;
                    be.inventory.extractItem(SLOT_INGREDIENT, 1, false);
                    be.produceOutput(recipe.getOutput());
                }
            } else {
                be.progress = 0;
            }
        } else {
            be.progress = 0;
        }

        if (be.savedParticles <= 0) {
            be.cachedParticle = null;
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) return true;
        if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    /** Data-driven поиск ExposureChamberRecipe по паре (particle, ingredient) — заменяет статический ExposureChamberRecipes.getRecipe. */
    @Nullable
    private static ExposureChamberRecipe findRecipe(MachineExposureChamberBlockEntity be,
                                                    ItemStack particle, ItemStack ingredient) {
        Level level = be.getLevel();
        if (level == null) return null;
        for (ExposureChamberRecipe recipe : RecipeHooks.getAllRecipes(level, ExposureChamberRecipe.Type.INSTANCE)) {
            if (recipe.matches(particle, ingredient)) return recipe;
        }
        return null;
    }

    private void produceOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    public int getProgressScaled(int scale) {
        return progress * scale / PROCESS_TIME;
    }

    public int getParticlesScaled(int scale) {
        return savedParticles * scale / MAX_PARTICLES;
    }

    public int getEnergyScaled(int scale) {
        long max = getMaxEnergyStored();
        if (max <= 0) return 0;
        return (int) (getEnergyStored() * scale / max);
    }

    // ==================== NBT ====================

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putInt("progress", progress);
        tag.putInt("savedParticles", savedParticles);
        if (cachedParticle != null) {
            tag.putString("cachedParticle", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cachedParticle).toString());
        }
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.putInt("progress", progress);
    tag.putInt("savedParticles", savedParticles);
    if (cachedParticle != null) {
    tag.putString("cachedParticle", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cachedParticle).toString());
    }
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        progress = tag.getInt("progress");
        savedParticles = tag.getInt("savedParticles");
        cachedParticle = tag.contains("cachedParticle")
                ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(tag.getString("cachedParticle")))
                : null;
        if (cachedParticle == net.minecraft.world.item.Items.AIR) cachedParticle = null;
    }

    // ==================== GUI ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.exposure_chamber");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_PARTICLE, SLOT_INGREDIENT -> true;
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineExposureChamberMenu.create(id, inv, this);
    }
}
