package com.hbm_m.test;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.module.machine.MachineModuleAdvancedAssembler;
import com.hbm_m.module.machine.MachineModuleBase;
import com.hbm_m.module.machine.MachineModuleChemplant;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.recipe.index.ModRecipeIndex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

import java.util.List;
import java.util.Map;

/**
 * Cross-platform GameTests for the machine crafting system: the Assembler
 * and the Chemical Plant.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@link RecipeHooks} — cross-version access to {@link RecipeManager} (getAllRecipes, byKey, recipeId).</li>
 *   <li>{@link ModRecipeIndex} — cached recipe index (byId, byPool, ordered).</li>
 *   <li>{@link AssemblerRecipe} — structure, {@code blueprintPool}, {@code requiresBlueprint}.</li>
 *   <li>{@link ChemicalPlantRecipe} — fluid inputs/outputs, {@code blueprintPool}.</li>
 *   <li>{@link ItemBlueprintFolder} — writing/reading pool from NBT, {@code isBlueprintPoolAllowed}.</li>
 *   <li>{@link MachineModuleBase} — energy gate ({@code hasEnoughEnergyToStartCraft}/{@code hasEnoughEnergyForTick}).</li>
 *   <li>{@link MachineModuleAdvancedAssembler} — {@code update}, progress, {@code processCraft}.</li>
 *   <li>{@link MachineModuleChemplant} — recipe selection by ID, blueprint-gating.</li>
 * </ul>
 *
 * <p><b>Diagnosing the "no recipes on 1.21.1" problem:</b> group 1 tests verify that
 * {@link RecipeManager} loaded the {@link AssemblerRecipe}/{@link ChemicalPlantRecipe}
 * recipes from JSON. If on 1.21.1 the JSON serializer ({@code PlatformRecipeSerializer.MapCodec})
 * drops recipes due to format incompatibility ({@code "item":} vs {@code "id":}),
 * the group 1 tests will expose it.
 *
 * <p>Templates: {@code hbm_m:empty3x3x3} and {@code hbm_m:empty5x5x5}.
 * Annotations: {@code @GameTestHolder("hbm_m")} + {@code @PrefixGameTestTemplate(false)}.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class MachineCraftingGameTest {

    private MachineCraftingGameTest() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Helper assert methods and mock objects.
    // ════════════════════════════════════════════════════════════════════════

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void checkEq(long expected, long actual, String msg) {
        if (expected != actual) {
            throw new GameTestAssertException(msg + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private static void checkEq(double expected, double actual, String msg) {
        if (Math.abs(expected - actual) > 1e-9) {
            throw new GameTestAssertException(msg + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    /**
     * Mock {@link IEnergyReceiver} with a given capacity and current charge.
     * Used by the machine module as its energyStorage — allows testing
     * energy-gate logic without a real BlockEntity.
     */
    private static final class MockEnergyReceiver implements IEnergyReceiver {
        private long energy;
        private final long capacity;

        MockEnergyReceiver(long capacity, long initialEnergy) {
            this.capacity = capacity;
            this.energy = Math.min(initialEnergy, capacity);
        }

        @Override public long getEnergyStored() { return energy; }
        @Override public long getMaxEnergyStored() { return capacity; }
        @Override public void setEnergyStored(long e) { this.energy = Math.min(e, capacity); }
        @Override public long getReceiveSpeed() { return capacity; }
        @Override public Priority getPriority() { return Priority.NORMAL; }
        @Override public long receiveEnergy(long maxReceive, boolean simulate) {
            long accepted = Math.min(maxReceive, capacity - energy);
            if (!simulate) energy += accepted;
            return accepted;
        }
        @Override public boolean canReceive() { return true; }
        @Override public boolean canConnectEnergy(Direction side) { return true; }
    }

    /**
     * Creates an anonymous {@link ModItemStackHandler} of the given size (as in the BE).
     */
    private static ModItemStackHandler makeInventory(int size) {
        return new ModItemStackHandler(size) {
            @Override protected void onContentsChanged(int slot) {}
        };
    }

    /**
     * Creates a {@link MachineModuleAdvancedAssembler} with the given inventory and energy.
     * The passed {@code inv} stays accessible to the test for direct slot read/write.
     * Blueprint slot = 1, input slots 4..15, output slot = 16 (as in the original).
     */
    private static MachineModuleAdvancedAssembler makeAssemblerModule(
            Level level, ModItemStackHandler inv, long capacity, long energy) {
        MockEnergyReceiver er = new MockEnergyReceiver(capacity, energy);
        MachineModuleAdvancedAssembler mod = new MachineModuleAdvancedAssembler(0, er, inv, level);
        mod.setInputSlots(4, 12);
        mod.setOutputSlot(16);
        return mod;
    }

    /** Container: module + inventory (for tests that need slot access). */
    private record AssemblerSetup(MachineModuleAdvancedAssembler module, ModItemStackHandler inv) {}

    // ════════════════════════════════════════════════════════════════════════
    //  Group 1: RecipeManager / RecipeHooks — recipe presence (1.21.1 diagnostics).
    //  These tests are an indicator: if on 1.21.1 the JSON serializer fails to parse
    //  recipes, getAllRecipes will return an empty list and the test will fail
    //  with a clear message.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeManager_nonNull(GameTestHelper helper) {
        Level level = helper.getLevel();
        check(level.getRecipeManager() != null, "RecipeManager must not be null");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerRecipes_loaded(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<AssemblerRecipe> recipes =
                RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(),
                "AssemblerRecipe list must NOT be empty — JSON recipes failed to load on this version! "
                + "Check PlatformRecipeSerializer.MapCodec / ItemStack format (item: vs id:). "
                + "Count=" + recipes.size());
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemicalPlantRecipes_loaded(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes =
                RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(),
                "ChemicalPlantRecipe list must NOT be empty — JSON recipes failed to load on this version! "
                + "Check PlatformRecipeSerializer.MapCodec / ItemStack format (item: vs id:). "
                + "Count=" + recipes.size());
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerRecipes_byId_nonEmpty(GameTestHelper helper) {
        Level level = helper.getLevel();
        Map<net.minecraft.resources.ResourceLocation, AssemblerRecipe> byId =
                RecipeHooks.getAllRecipesById(level, AssemblerRecipe.Type.INSTANCE);
        check(!byId.isEmpty(),
                "AssemblerRecipe byId map must NOT be empty. Size=" + byId.size());
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemicalPlantRecipes_byId_nonEmpty(GameTestHelper helper) {
        Level level = helper.getLevel();
        Map<net.minecraft.resources.ResourceLocation, ChemicalPlantRecipe> byId =
                RecipeHooks.getAllRecipesById(level, ChemicalPlantRecipe.Type.INSTANCE);
        check(!byId.isEmpty(),
                "ChemicalPlantRecipe byId map must NOT be empty. Size=" + byId.size());
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Group 2: ModRecipeIndex — cached index.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeIndex_assembler_ordered_nonEmpty(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<AssemblerRecipe> all = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE);
        check(!all.isEmpty(), "ModRecipeIndex.getAll(assembler) must not be empty");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeIndex_chemplant_ordered_nonEmpty(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> all = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(ChemicalPlantRecipe.Type.INSTANCE);
        check(!all.isEmpty(), "ModRecipeIndex.getAll(chemical_plant) must not be empty");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeIndex_assembler_byId_resolves(GameTestHelper helper) {
        Level level = helper.getLevel();
        Map<net.minecraft.resources.ResourceLocation, AssemblerRecipe> byId =
                RecipeHooks.getAllRecipesById(level, AssemblerRecipe.Type.INSTANCE);
        check(!byId.isEmpty(), "Need at least 1 assembler recipe to test byId lookup");
        net.minecraft.resources.ResourceLocation firstId = byId.keySet().iterator().next();
        AssemblerRecipe resolved = ModRecipeIndex.of(level.getRecipeManager())
                .getById(AssemblerRecipe.Type.INSTANCE, firstId)
                .orElse(null);
        check(resolved != null, "getById must resolve existing assembler recipe id: " + firstId);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeIndex_cacheStable(GameTestHelper helper) {
        // Request the index twice — the WeakHashMap cache must return the same list.
        Level level = helper.getLevel();
        List<AssemblerRecipe> a1 = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE);
        List<AssemblerRecipe> a2 = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE);
        check(a1 == a2, "ModRecipeIndex cache must return same list instance for unchanged recipe count");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Group 3: AssemblerRecipe — structure, blueprintPool, requiresBlueprint.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerRecipe_hasDuration(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<AssemblerRecipe> recipes = RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need assembler recipes");
        for (AssemblerRecipe r : recipes) {
            check(r.getDuration() > 0,
                    "AssemblerRecipe duration must be > 0, got " + r.getDuration());
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerRecipe_hasPowerConsumption(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<AssemblerRecipe> recipes = RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need assembler recipes");
        for (AssemblerRecipe r : recipes) {
            check(r.getPowerConsumption() > 0,
                    "AssemblerRecipe power must be > 0, got " + r.getPowerConsumption());
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerRecipe_hasOutput(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<AssemblerRecipe> recipes = RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need assembler recipes");
        for (AssemblerRecipe r : recipes) {
            ItemStack out = r.getResultItem(level.registryAccess());
            check(out != null && !out.isEmpty(),
                    "AssemblerRecipe output must be non-empty");
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerRecipe_hasIngredients(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<AssemblerRecipe> recipes = RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need assembler recipes");
        for (AssemblerRecipe r : recipes) {
            check(!r.getIngredients().isEmpty(),
                    "AssemblerRecipe ingredients must not be empty");
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerRecipe_blueprintPool_nullOrNonEmpty(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<AssemblerRecipe> recipes = RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need assembler recipes");
        for (AssemblerRecipe r : recipes) {
            String pool = r.getBlueprintPool();
            // pool is either null/empty (no blueprint required) or a non-empty string.
            if (pool != null) {
                check(!pool.isEmpty(),
                        "AssemblerRecipe blueprintPool if non-null must be non-empty: " + pool);
            }
            // requiresBlueprint must match the pool.
            check(r.requiresBlueprint() == (pool != null && !pool.isEmpty()),
                    "requiresBlueprint must match (pool != null && !pool.isEmpty())");
        }
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Group 4: ChemicalPlantRecipe — structure, fluids.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantRecipe_hasDuration(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need chemplant recipes");
        for (ChemicalPlantRecipe r : recipes) {
            check(r.getDuration() > 0,
                    "ChemicalPlantRecipe duration must be > 0, got " + r.getDuration());
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantRecipe_hasPowerConsumption(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need chemplant recipes");
        for (ChemicalPlantRecipe r : recipes) {
            check(r.getPowerConsumption() > 0,
                    "ChemicalPlantRecipe power must be > 0, got " + r.getPowerConsumption());
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantRecipe_hasFluidOrItemIO(GameTestHelper helper) {
        // Each chemplant recipe must have at least one fluid or item input/output.
        // (something to process into something).
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need chemplant recipes");
        for (ChemicalPlantRecipe r : recipes) {
            boolean hasInput = !r.getFluidInputs().isEmpty() || !r.getItemInputs().isEmpty();
            boolean hasOutput = !r.getFluidOutputs().isEmpty() || !r.getItemOutputs().isEmpty();
            check(hasInput,
                    "ChemicalPlantRecipe must have at least one fluid or item input");
            check(hasOutput,
                    "ChemicalPlantRecipe must have at least one fluid or item output");
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantRecipe_blueprintPool_nullOrNonEmpty(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "Need chemplant recipes");
        for (ChemicalPlantRecipe r : recipes) {
            String pool = r.getBlueprintPool();
            if (pool != null) {
                check(!pool.isEmpty(),
                        "ChemicalPlantRecipe blueprintPool if non-null must be non-empty: " + pool);
            }
            check(r.requiresBlueprint() == (pool != null && !pool.isEmpty()),
                    "requiresBlueprint must match (pool != null && !pool.isEmpty())");
        }
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Group 5: Blueprint pool — ItemBlueprintFolder, isBlueprintPoolAllowed.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void blueprintFolder_emptyByDefault(GameTestHelper helper) {
        ItemStack folder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        String pool = ItemBlueprintFolder.getBlueprintPool(folder);
        check(pool != null && pool.isEmpty(),
                "Fresh blueprint folder pool must be empty string, got: " + pool);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void blueprintFolder_writeReadPool(GameTestHelper helper) {
        ItemStack folder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        ItemBlueprintFolder.writeBlueprintPool(folder, "machines");
        String pool = ItemBlueprintFolder.getBlueprintPool(folder);
        check("machines".equals(pool),
                "Blueprint folder must read back written pool 'machines', got: " + pool);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void blueprintFolder_emptyStackPool(GameTestHelper helper) {
        // An empty stack (ItemStack.EMPTY) must not crash getBlueprintPool.
        String pool = ItemBlueprintFolder.getBlueprintPool(ItemStack.EMPTY);
        check(pool != null && pool.isEmpty(),
                "EMPTY stack pool must be empty string, got: " + pool);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeIndex_byPool_assembler(GameTestHelper helper) {
        // If any assembler recipes have a blueprintPool, the index must group them.
        Level level = helper.getLevel();
        List<AssemblerRecipe> all = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE);
        check(!all.isEmpty(), "Need assembler recipes");
        // Find at least one recipe with a pool (if any).
        String anyPool = null;
        for (AssemblerRecipe r : all) {
            if (r.getBlueprintPool() != null && !r.getBlueprintPool().isEmpty()) {
                anyPool = r.getBlueprintPool();
                break;
            }
        }
        if (anyPool != null) {
            List<AssemblerRecipe> poolRecipes = ModRecipeIndex.of(level.getRecipeManager())
                    .getByPool(AssemblerRecipe.Type.INSTANCE, anyPool);
            check(!poolRecipes.isEmpty(),
                    "getByPool must return recipes for existing pool: " + anyPool);
            for (AssemblerRecipe r : poolRecipes) {
                check(anyPool.equals(r.getBlueprintPool()),
                        "getByPool recipe must have matching pool");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeIndex_byPool_chemplant(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> all = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(ChemicalPlantRecipe.Type.INSTANCE);
        check(!all.isEmpty(), "Need chemplant recipes");
        String anyPool = null;
        for (ChemicalPlantRecipe r : all) {
            if (r.getBlueprintPool() != null && !r.getBlueprintPool().isEmpty()) {
                anyPool = r.getBlueprintPool();
                break;
            }
        }
        if (anyPool != null) {
            List<ChemicalPlantRecipe> poolRecipes = ModRecipeIndex.of(level.getRecipeManager())
                    .getByPool(ChemicalPlantRecipe.Type.INSTANCE, anyPool);
            check(!poolRecipes.isEmpty(),
                    "getByPool must return chemplant recipes for existing pool: " + anyPool);
        }
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Group 6: Energy gate — hasEnoughEnergyToStartCraft / hasEnoughEnergyForTick.
    //  Static MachineModuleBase methods — pure logic without dependencies.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_zeroCost_alwaysEnough(GameTestHelper helper) {
        // energyPerTick == 0 → always enough (free craft).
        check(MachineModuleBase.hasEnoughEnergyForTick(0, 0),
                "hasEnoughEnergyForTick(0, 0) must be true (free recipe)");
        check(MachineModuleBase.hasEnoughEnergyForTick(1000, 0),
                "hasEnoughEnergyForTick(1000, 0) must be true (free recipe)");
        check(MachineModuleBase.hasEnoughEnergyToStartCraft(0.0, 0, 0, 100),
                "hasEnoughEnergyToStartCraft with 0 cost must be true");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_zeroMaxProgress_alwaysEnough(GameTestHelper helper) {
        // maxProgress == 0 → always enough (division-by-zero guard).
        check(MachineModuleBase.hasEnoughEnergyToStartCraft(0.0, 100, 50, 0),
                "hasEnoughEnergyToStartCraft with maxProgress=0 must be true");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_progressStarted_skipBufferCheck(GameTestHelper helper) {
        // progress > 0 → the craft is already running, no full energy buffer required.
        check(MachineModuleBase.hasEnoughEnergyToStartCraft(1.0, 10, 50, 100),
                "hasEnoughEnergyToStartCraft with progress>0 must be true (already crafting)");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_notEnoughForFullCycle(GameTestHelper helper) {
        // energyPerTick=50, maxProgress=100 → total=5000; stored=1000 → not enough (progress=0).
        check(!MachineModuleBase.hasEnoughEnergyToStartCraft(0.0, 1000, 50, 100),
                "hasEnoughEnergyToStartCraft: 1000 < 5000 must be false");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_enoughForFullCycle(GameTestHelper helper) {
        // energyPerTick=50, maxProgress=100 → total=5000; stored=5000 → enough.
        check(MachineModuleBase.hasEnoughEnergyToStartCraft(0.0, 5000, 50, 100),
                "hasEnoughEnergyToStartCraft: 5000 >= 5000 must be true");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_forTick_exact(GameTestHelper helper) {
        // stored == energyPerTick → exactly one tick's worth.
        check(MachineModuleBase.hasEnoughEnergyForTick(100, 100),
                "hasEnoughEnergyForTick: 100 >= 100 must be true");
        check(!MachineModuleBase.hasEnoughEnergyForTick(99, 100),
                "hasEnoughEnergyForTick: 99 < 100 must be false");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Group 7: MachineModuleAdvancedAssembler — update, progress, processCraft.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerModule_noRecipe_noProgress(GameTestHelper helper) {
        Level level = helper.getLevel();
        ModItemStackHandler inv = makeInventory(20);
        MachineModuleAdvancedAssembler mod = makeAssemblerModule(level, inv, 100_000L, 100_000L);
        // Inventory is empty → no recipe → update must not progress.
        mod.update(1.0, 1.0, true, null);
        checkEq(0.0, mod.getProgress(), "progress must stay 0 with no recipe");
        check(!mod.isProcessing(), "isProcessing must be false with no recipe");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerModule_lowEnergy_noStart(GameTestHelper helper) {
        Level level = helper.getLevel();
        ModItemStackHandler inv = makeInventory(20);
        // Energy 0 → the craft must not start (the assembler requires a full buffer).
        MachineModuleAdvancedAssembler mod = makeAssemblerModule(level, inv, 100_000L, 0L);
        mod.update(1.0, 1.0, true, null);
        checkEq(0.0, mod.getProgress(), "progress must be 0 with no energy");
        check(!mod.isProcessing(), "isProcessing must be false with no energy");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "machines", timeoutTicks = 200)
    public static void assemblerModule_fullCycle_producesOutput(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<AssemblerRecipe> recipes = RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE);
        // A silent succeed with no recipes would mask a completely broken craft.
        check(!recipes.isEmpty(), "No recipes found to execute test — see assemblerRecipes_loaded for diagnostics");
        // Take the first recipe with minimal requirements.
        AssemblerRecipe recipe = recipes.get(0);
        int duration = recipe.getDuration();
        long power = recipe.getPowerConsumption();
        long totalEnergy = power * duration;

        ModItemStackHandler inv = makeInventory(20);
        MachineModuleAdvancedAssembler mod = makeAssemblerModule(level, inv, totalEnergy * 2, totalEnergy * 2);

        // Fill the input slots with items matching the ingredients.
        // Ingredients are a NonNullList<Ingredient> (expanded by count).
        net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> ings =
                recipe.getIngredients();
        int inputSlotIdx = 0;
        for (net.minecraft.world.item.crafting.Ingredient ing : ings) {
            if (ing.isEmpty()) continue;
            // Take the first matching stack from the Ingredient.
            ItemStack[] matchingStacks = ing.getItems();
            if (matchingStacks.length == 0 || matchingStacks[0].isEmpty()) continue;
            ItemStack sample = matchingStacks[0].copy();
            sample.setCount(sample.getMaxStackSize()); // with headroom
            inv.setStackInSlot(4 + inputSlotIdx, sample);
            inputSlotIdx++;
            if (inputSlotIdx >= 12) break;
        }

        // Select the recipe.
        mod.setPreferredRecipe(recipe);

        // Simulate duration ticks (with overdrive to speed things up).
        double speed = Math.max(1.0, duration); // 1 tick = whole cycle
        mod.update(speed, 1.0, true, null);

        // After one update with speed=duration the progress must reach maxProgress and the craft must complete.
        ItemStack output = inv.getStackInSlot(16);
        check(!output.isEmpty(),
                "Assembler module must produce output after full cycle. Output=" + output);

        // Extra check: the output must match the recipe result.
        ItemStack expected = recipe.getResultItem(level.registryAccess());
        check(net.minecraft.world.item.ItemStack.isSameItem(expected, output),
                "Output item must match recipe result. Expected=" + expected + ", got=" + output);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerModule_extraConditionFalse_noProgress(GameTestHelper helper) {
        Level level = helper.getLevel();
        ModItemStackHandler inv = makeInventory(20);
        MachineModuleAdvancedAssembler mod = makeAssemblerModule(level, inv, 100_000L, 100_000L);
        // extraCondition=false → even with energy, no crafting happens.
        mod.update(1.0, 1.0, false, null);
        checkEq(0.0, mod.getProgress(), "progress must be 0 when extraCondition=false");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerModule_resetProgress_zeroes(GameTestHelper helper) {
        Level level = helper.getLevel();
        ModItemStackHandler inv = makeInventory(20);
        MachineModuleAdvancedAssembler mod = makeAssemblerModule(level, inv, 100_000L, 100_000L);
        mod.resetProgress();
        checkEq(0.0, mod.getProgress(), "resetProgress must zero progress");
        check(!mod.isProcessing(), "resetProgress must clear isProcessing");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Group 8: MachineModuleChemplant — recipe selection by ID, blueprint-gating.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantModule_noSelection_noRecipe(GameTestHelper helper) {
        Level level = helper.getLevel();
        com.hbm_m.inventory.fluid.tank.FluidTank[] inTanks = new com.hbm_m.inventory.fluid.tank.FluidTank[]{
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000)
        };
        com.hbm_m.inventory.fluid.tank.FluidTank[] outTanks = new com.hbm_m.inventory.fluid.tank.FluidTank[]{
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000)
        };
        ModItemStackHandler inv = makeInventory(20);
        MockEnergyReceiver er = new MockEnergyReceiver(1_000_000L, 1_000_000L);
        MachineModuleChemplant mod = new MachineModuleChemplant(er, inv,
                new int[]{0, 1, 2, 3}, new int[]{4, 5, 6, 7},
                inTanks, outTanks, level);
        // No recipe selected → peekRecipe must return null.
        check(mod.peekRecipe(level) == null, "peekRecipe must be null without selection");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantModule_selectRecipe_resolves(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        check(!recipes.isEmpty(), "No chemplant recipes found to execute test — see chemicalPlantRecipes_loaded for diagnostics");
        ChemicalPlantRecipe recipe = recipes.get(0);
        net.minecraft.resources.ResourceLocation id = RecipeHooks.recipeId(
                level.getRecipeManager(), ChemicalPlantRecipe.Type.INSTANCE, recipe);
        check(id != null, "recipeId must be non-null for existing chemplant recipe");

        com.hbm_m.inventory.fluid.tank.FluidTank[] inTanks = new com.hbm_m.inventory.fluid.tank.FluidTank[]{
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000)
        };
        com.hbm_m.inventory.fluid.tank.FluidTank[] outTanks = new com.hbm_m.inventory.fluid.tank.FluidTank[]{
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000)
        };
        ModItemStackHandler inv = makeInventory(20);
        MockEnergyReceiver er = new MockEnergyReceiver(1_000_000L, 1_000_000L);
        MachineModuleChemplant mod = new MachineModuleChemplant(er, inv,
                new int[]{0, 1, 2, 3}, new int[]{4, 5, 6, 7},
                inTanks, outTanks, level);
        mod.setSelectedRecipe(id);
        ChemicalPlantRecipe peeked = mod.peekRecipe(level);
        check(peeked != null, "peekRecipe must resolve after setSelectedRecipe");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantModule_blueprintMismatch_clearsSelection(GameTestHelper helper) {
        // If the recipe has a blueprintPool but the machine holds a folder with a
        // different pool, updateAndGetDirty must clear selectedRecipeId.
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        // Look for a recipe with a blueprintPool.
        ChemicalPlantRecipe pooledRecipe = null;
        for (ChemicalPlantRecipe r : recipes) {
            if (r.getBlueprintPool() != null && !r.getBlueprintPool().isEmpty()) {
                pooledRecipe = r;
                break;
            }
        }
        if (pooledRecipe == null) {
            // No pooled recipes — nothing to test.
            helper.succeed();
            return;
        }
        net.minecraft.resources.ResourceLocation id = RecipeHooks.recipeId(
                level.getRecipeManager(), ChemicalPlantRecipe.Type.INSTANCE, pooledRecipe);
        String pool = pooledRecipe.getBlueprintPool();

        com.hbm_m.inventory.fluid.tank.FluidTank[] inTanks = new com.hbm_m.inventory.fluid.tank.FluidTank[]{
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000)
        };
        com.hbm_m.inventory.fluid.tank.FluidTank[] outTanks = new com.hbm_m.inventory.fluid.tank.FluidTank[]{
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000)
        };
        ModItemStackHandler inv = makeInventory(20);
        MockEnergyReceiver er = new MockEnergyReceiver(1_000_000L, 1_000_000L);
        MachineModuleChemplant mod = new MachineModuleChemplant(er, inv,
                new int[]{0, 1, 2, 3}, new int[]{4, 5, 6, 7},
                inTanks, outTanks, level);
        mod.setSelectedRecipe(id);
        check(mod.peekRecipe(level) != null, "peekRecipe must resolve before blueprint mismatch");

        // Folder with a different pool.
        ItemStack wrongFolder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        ItemBlueprintFolder.writeBlueprintPool(wrongFolder, pool + "_wrong");

        mod.updateAndGetDirty(1.0, 1.0, true, wrongFolder);
        check(mod.peekRecipe(level) == null,
                "peekRecipe must be null after blueprint pool mismatch");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantModule_blueprintMatch_keepsSelection(GameTestHelper helper) {
        // If the recipe has a blueprintPool and the folder has the same pool, the selection is kept.
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        ChemicalPlantRecipe pooledRecipe = null;
        for (ChemicalPlantRecipe r : recipes) {
            if (r.getBlueprintPool() != null && !r.getBlueprintPool().isEmpty()) {
                pooledRecipe = r;
                break;
            }
        }
        if (pooledRecipe == null) {
            helper.succeed();
            return;
        }
        net.minecraft.resources.ResourceLocation id = RecipeHooks.recipeId(
                level.getRecipeManager(), ChemicalPlantRecipe.Type.INSTANCE, pooledRecipe);
        String pool = pooledRecipe.getBlueprintPool();

        com.hbm_m.inventory.fluid.tank.FluidTank[] inTanks = new com.hbm_m.inventory.fluid.tank.FluidTank[]{
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000)
        };
        com.hbm_m.inventory.fluid.tank.FluidTank[] outTanks = new com.hbm_m.inventory.fluid.tank.FluidTank[]{
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000),
                new com.hbm_m.inventory.fluid.tank.FluidTank(16000)
        };
        ModItemStackHandler inv = makeInventory(20);
        MockEnergyReceiver er = new MockEnergyReceiver(1_000_000L, 1_000_000L);
        MachineModuleChemplant mod = new MachineModuleChemplant(er, inv,
                new int[]{0, 1, 2, 3}, new int[]{4, 5, 6, 7},
                inTanks, outTanks, level);
        mod.setSelectedRecipe(id);

        ItemStack correctFolder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        ItemBlueprintFolder.writeBlueprintPool(correctFolder, pool);

        mod.updateAndGetDirty(1.0, 1.0, true, correctFolder);
        ChemicalPlantRecipe peeked = mod.peekRecipe(level);
        check(peeked != null,
                "peekRecipe must still resolve when blueprint pool matches");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Group 9: RecipeType registration — types are registered and match.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeType_assembler_singleton(GameTestHelper helper) {
        check(AssemblerRecipe.Type.INSTANCE != null, "AssemblerRecipe.Type.INSTANCE must not be null");
        // Use RecipeHooks.getAllRecipes — a cross-version wrapper that
        // unwraps the RecipeHolder on 1.21.1.
        Level level = helper.getLevel();
        List<AssemblerRecipe> list = RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE);
        check(list != null, "getAllRecipes(assembler) must not return null");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeType_chemplant_singleton(GameTestHelper helper) {
        check(ChemicalPlantRecipe.Type.INSTANCE != null, "ChemicalPlantRecipe.Type.INSTANCE must not be null");
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> list = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        check(list != null, "getAllRecipes(chemical_plant) must not return null");
        helper.succeed();
    }
}
