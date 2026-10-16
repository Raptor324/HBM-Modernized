package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineSolderingStationMenu;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.SolderingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineSolderingStationBlockEntity extends BaseMachineBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

    // ─── Slot map (11 total) ──────────────────────────────────────────────────
    public static final int SLOTS      = 11;
    public static final int SLOT_TOP0  = 0, SLOT_TOP1 = 1, SLOT_TOP2 = 2; // toppings
    public static final int SLOT_PCB0  = 3, SLOT_PCB1 = 4;                // PCB (2 slots)
    public static final int SLOT_SOLDER = 5;                               // solder (1 slot)
    public static final int SLOT_OUT   = 6;  // output
    public static final int SLOT_BAT   = 7;  // battery
    public static final int SLOT_FLUID = 8;  // fluid-ID item
    public static final int SLOT_UPG1  = 9, SLOT_UPG2 = 10; // upgrades

    // ─── Energy constants ─────────────────────────────────────────────────────
    public static final long MAX_ENERGY  = 100_000L;
    public static final long CONSUMPTION =   2_000L;

    // ─── Processing state ─────────────────────────────────────────────────────
    public int  progress    = 0;
    public int  processTime = 200;
    public long consumption = CONSUMPTION;

    // ─── Fluid tank (solder flux / coolant) ──────────────────────────────────
    /** Welding fluid (8,000 mb as in the original). */
    public final FluidTank tank = new FluidTank(8_000);

    // ─── Feature flags ────────────────────────────────────────────────────────
    /**
     * When true, prevents processing no-fluid recipes while a fluid is present
     * in the tank (avoids accidental dry-welding when flux is loaded).
     */
    public boolean collisionPrevention = false;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public MachineSolderingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLDERING_STATION_BE.get(), pos, state, SLOTS, MAX_ENERGY, CONSUMPTION);
    }

    /** Exposes the machine inventory for SlotItemHandler in the menu. */
    public com.hbm_m.platform.ModItemStackHandler getItemHandler() { return inventory; }

    // ─── Collision Prevention toggle ──────────────────────────────────────────

    /** Called server-side by the GUI toggle packet. */
    public void toggleCollisionPrevention() {
        collisionPrevention = !collisionPrevention;
        setChanged();
        if (level != null)
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSolderingStationBlockEntity be) {
        if (level.isClientSide) return;

        // The tank had no way to be filled at all: ten of the seventeen soldering recipes ask for
        // a fluid, so they were unreachable. Subscribe to neighbouring pipes like the other
        // machines with a tank do.
        if (level.getGameTime() % 20 == 0) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                be.trySubscribe(be.tank, level, pos.relative(dir), dir);
            }
        }

        // Charge from battery slot
        com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(
                be.inventory.getStackInSlot(SLOT_BAT)).ifPresent(provider -> {
            long want = be.capacity - be.energy;
            long got  = provider.extractEnergy(want, false);
            if (got > 0) be.energy += got;
        });

        // Data-driven поиск рецепта: итерируем SolderingRecipe из RecipeManager (заменяет статику SolderingRecipes).
        // Кэшированный скан: подпись — фингерпринты 6 входных слотов; matchesFluid(tank)
        // переоценивается каждый тик против кэшированного рецепта (CachedRecipeResolver CONTRACT 1).
        SolderingRecipe recipe = be.findSolderingRecipe(level);
        if (recipe != null) {
            be.processTime  = recipe.getDuration();
            be.consumption  = recipe.getConsumption();
        }
        // matchesFluid на SolderingRecipe заменяет прежний recipe.fluid.satisfiedBy(tank) (FluidStack-based).
        boolean hasRecipe  = recipe != null && recipe.matchesFluid(be.tank);
        boolean canProcess = hasRecipe && be.canProcess();

        if (canProcess) {
            be.progress++;
            be.energy = Math.max(0, be.energy - be.consumption);
            if (be.progress >= be.processTime) {
                be.progress = 0;
                be.consumeItems(recipe);
                ItemStack out = be.inventory.getStackInSlot(SLOT_OUT);
                ItemStack recipeOut = recipe.getOutput();
                if (out.isEmpty()) {
                    be.inventory.setStackInSlot(SLOT_OUT, recipeOut.copy());
                } else {
                    out.grow(recipeOut.getCount());
                }
                recipe.consumeFluid(be.tank); // no-op, если требования по жидкости нет
                be.setChanged();
            }
        } else {
            be.progress = 0;
        }

        // Анти-спам синхронизации: пакет только при реальном изменении состояния,
        // которое уезжает в NBT/клиенту (энергия, прогресс, флаги, бак, инвентарь).
        long lo = com.hbm_m.recipe.index.CachedRecipeResolver.sign(11,
                be.energy, be.progress, be.processTime, be.consumption,
                be.collisionPrevention ? 1L : 0L,
                tankFingerprint(be.tank));
        long hi = com.hbm_m.recipe.index.CachedRecipeResolver.sign(12,
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_TOP0)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_TOP1)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_TOP2)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_PCB0)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_PCB1)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_SOLDER)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_OUT)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_BAT)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_FLUID)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_UPG1)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(be.inventory.getStackInSlot(SLOT_UPG2)));
        be.syncIfStateChanged(lo, hi);
    }

    /** Фингерпринт HBM-бака: тип жидкости + заполнение. */
    private static long tankFingerprint(FluidTank tank) {
        return (tank.getTankType() == null ? 0L : tank.getTankType().hashCode()) * 31L + tank.getFill();
    }

    private void consumeItems(SolderingRecipe recipe) {
        // Поглощаем по одной группе за раз: toppings (3 слота), pcb (2), solder (1).
        consumeGroup(recipe.getToppings(), recipe.getToppingCounts(), SLOT_TOP0, SLOT_TOP2 + 1);
        consumeGroup(recipe.getPcb(),      recipe.getPcbCounts(),      SLOT_PCB0, SLOT_PCB1 + 1);
        consumeGroup(recipe.getSolder(),   recipe.getSolderCounts(),   SLOT_SOLDER, SLOT_SOLDER + 1);
    }

    private void consumeGroup(Ingredient[] required, int[] counts, int from, int to) {
        for (int g = 0; g < required.length; g++) {
            Ingredient ing = required[g];
            int need = counts[g];
            // Перебираем слоты группы, ищем стак, проходящий ingredient.test() с нужным countом.
            for (int i = from; i < to; i++) {
                ItemStack s = inventory.getStackInSlot(i);
                if (ing.test(s) && s.getCount() >= need) { s.shrink(need); break; }
            }
        }
    }

    /**
     * Data-driven поиск SolderingRecipe по 6 слотам машины (заменяет статический SolderingRecipes.getRecipe).
     * Кэшируется через {@link CachedRecipeResolver}: подпись — фингерпринты входных стаков,
     * сканер — только матч по списку рецептов (CONTRACT 1).
     */
    private final com.hbm_m.recipe.index.CachedRecipeResolver<SolderingRecipe> recipeResolver =
            new com.hbm_m.recipe.index.CachedRecipeResolver<>();

    @org.jetbrains.annotations.Nullable
    private SolderingRecipe findSolderingRecipe(Level level) {
        if (level == null) return null;
        long signature = com.hbm_m.recipe.index.CachedRecipeResolver.sign(3,
                com.hbm_m.platform.PlatformHooks.stackFingerprint(inventory.getStackInSlot(SLOT_TOP0)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(inventory.getStackInSlot(SLOT_TOP1)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(inventory.getStackInSlot(SLOT_TOP2)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(inventory.getStackInSlot(SLOT_PCB0)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(inventory.getStackInSlot(SLOT_PCB1)),
                com.hbm_m.platform.PlatformHooks.stackFingerprint(inventory.getStackInSlot(SLOT_SOLDER)));
        return recipeResolver.resolve(level, SolderingRecipe.Type.INSTANCE, signature, (lvl, all) -> {
            for (SolderingRecipe recipe : all) {
                if (recipe.matches(inventory.getStackInSlot(SLOT_TOP0), inventory.getStackInSlot(SLOT_TOP1), inventory.getStackInSlot(SLOT_TOP2),
                                  inventory.getStackInSlot(SLOT_PCB0), inventory.getStackInSlot(SLOT_PCB1),
                                  inventory.getStackInSlot(SLOT_SOLDER))) {
                    return recipe;
                }
            }
            return null;
        });
    }

    public boolean canProcess() {
        if (energy < consumption) return false;
        // Collision prevention: don't process a fluid-less recipe if tank has fluid
        // (actual recipe fluid check will be added when SolderingRecipes is ported)
        if (collisionPrevention && tank.getFill() > 0) return false;
        // Output slot must be empty or stackable
        ItemStack out = inventory.getStackInSlot(SLOT_OUT);
        return out.isEmpty();
    }

    // ─── Progress helpers (for GUI) ───────────────────────────────────────────

    public int getProgressScaled(int scale) {
        return processTime > 0 ? progress * scale / processTime : 0;
    }

    public int getProgress()    { return progress;    }
    public int getMaxProgress() { return processTime; }

    // ─── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUT) return false;
        Level level = getLevel();
        if (level == null) return true; // пока нет level — позволяем всё (поведение при отсутствии рецептов).
        Ingredient[] group;
        if (slot < 3) {
            group = collectGroup(level, SolderingRecipe::getToppings);
        } else if (slot < SLOT_SOLDER) {
            group = collectGroup(level, SolderingRecipe::getPcb);
        } else if (slot == SLOT_SOLDER) {
            group = collectGroup(level, SolderingRecipe::getSolder);
        } else {
            return true;
        }
        if (group.length == 0) return true; // нет рецептов на эту группу — позволяем всё (как в оригинале).
        for (Ingredient ing : group) {
            if (ing.test(stack)) return true;
        }
        return false;
    }

    /** Собирает все ingredient-ы заданной группы по всем data-driven рецептам (заменяет SolderingRecipes.toppings/pcb/solder). */
    private static Ingredient[] collectGroup(Level level, java.util.function.Function<SolderingRecipe, Ingredient[]> pick) {
        java.util.List<Ingredient> out = new java.util.ArrayList<>();
        for (SolderingRecipe recipe : RecipeHooks.getAllRecipes(level, SolderingRecipe.Type.INSTANCE)) {
            for (Ingredient ing : pick.apply(recipe)) out.add(ing);
        }
        return out.toArray(new Ingredient[0]);
    }

    // ─── MenuProvider ─────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() { return Component.translatable("block.hbm_m.soldering_station"); }
    @Override
    public Component getDisplayName()    { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineSolderingStationMenu.create(id, inv, this);
    }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getAllTanks() {
        return new com.hbm_m.inventory.fluid.tank.FluidTank[] { tank };
    }

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getReceivingTanks() {
        return new com.hbm_m.inventory.fluid.tank.FluidTank[] { tank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress",    progress);
        tag.putInt("processTime", processTime);
        tag.putBoolean("collision", collisionPrevention);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress            = tag.getInt("progress");
        processTime         = tag.getInt("processTime");
        if (processTime <= 0) processTime = 200;
        collisionPrevention = tag.getBoolean("collision");
        tank.readFromNBT(tag, "tank");
    }
}
