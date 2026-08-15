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
 * Кроссплатформенные GameTest-ы для системы крафта станков: сборочная (Assembler)
 * и химическая установка (Chemical Plant).
 *
 * <p>Покрывает:
 * <ul>
 *   <li>{@link RecipeHooks} — кросс-версионный доступ к {@link RecipeManager} (getAllRecipes, byKey, recipeId).</li>
 *   <li>{@link ModRecipeIndex} — кэшированный индекс рецептов (byId, byPool, ordered).</li>
 *   <li>{@link AssemblerRecipe} — структура, {@code blueprintPool}, {@code requiresBlueprint}.</li>
 *   <li>{@link ChemicalPlantRecipe} — fluid inputs/outputs, {@code blueprintPool}.</li>
 *   <li>{@link ItemBlueprintFolder} — запись/чтение pool из NBT, {@code isBlueprintPoolAllowed}.</li>
 *   <li>{@link MachineModuleBase} — energy gate ({@code hasEnoughEnergyToStartCraft}/{@code hasEnoughEnergyForTick}).</li>
 *   <li>{@link MachineModuleAdvancedAssembler} — {@code update}, progress, {@code processCraft}.</li>
 *   <li>{@link MachineModuleChemplant} — выбор рецепта по ID, blueprint-gating.</li>
 * </ul>
 *
 * <p><b>Диагностика проблемы «нет рецептов на 1.21.1»:</b> тесты группы 1 проверяют,
 * что {@link RecipeManager} загрузил рецепты {@link AssemblerRecipe}/{@link ChemicalPlantRecipe}
 * из JSON. Если на 1.21.1 JSON-сериализатор ({@code PlatformRecipeSerializer.MapCodec})
 * отбрасывает рецепты из-за несовместимости формата ({@code "item":} vs {@code "id":}),
 * тесты группы 1 это выявят.
 *
 * <p>Шаблоны: {@code hbm_m:empty3x3x3} и {@code hbm_m:empty5x5x5}.
 * Аннотации: {@code @GameTestHolder("hbm_m")} + {@code @PrefixGameTestTemplate(false)}.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class MachineCraftingGameTest {

    private MachineCraftingGameTest() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Вспомогательные assert-методы и mock-объекты.
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
     * Mock {@link IEnergyReceiver} с заданной ёмкостью и текущим зарядом.
     * Используется модулем станка как energyStorage — позволяет тестировать
     * energy-gate логику без реального BlockEntity.
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
     * Создаёт анонимный {@link ModItemStackHandler} заданного размера (как в BE).
     */
    private static ModItemStackHandler makeInventory(int size) {
        return new ModItemStackHandler(size) {
            @Override protected void onContentsChanged(int slot) {}
        };
    }

    /**
     * Создаёт {@link MachineModuleAdvancedAssembler} с заданным инвентарём и энергией.
     * Передаваемый {@code inv} остаётся доступным тесту для прямого чтения/записи слотов.
     * Blueprint слот = 1, входные слоты 4..15, выходной слот = 16 (как в оригинале).
     */
    private static MachineModuleAdvancedAssembler makeAssemblerModule(
            Level level, ModItemStackHandler inv, long capacity, long energy) {
        MockEnergyReceiver er = new MockEnergyReceiver(capacity, energy);
        MachineModuleAdvancedAssembler mod = new MachineModuleAdvancedAssembler(0, er, inv, level);
        mod.setInputSlots(4, 12);
        mod.setOutputSlot(16);
        return mod;
    }

    /** Контейнер: модуль + инвентарь (для тестов, где нужен доступ к слотам). */
    private record AssemblerSetup(MachineModuleAdvancedAssembler module, ModItemStackHandler inv) {}

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 1: RecipeManager / RecipeHooks — наличие рецептов (диагностика 1.21.1).
    //  Эти тесты — индикатор: если на 1.21.1 JSON-сериализатор не парсит рецепты,
    //  getAllRecipes вернёт пустой список, и тест упадёт с понятным сообщением.
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
    //  Группа 2: ModRecipeIndex — кэшированный индекс.
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
        // Дважды запрашиваем индекс —WeakHashMap-кэш должен вернуть тот же список.
        Level level = helper.getLevel();
        List<AssemblerRecipe> a1 = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE);
        List<AssemblerRecipe> a2 = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE);
        check(a1 == a2, "ModRecipeIndex cache must return same list instance for unchanged recipe count");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 3: AssemblerRecipe — структура, blueprintPool, requiresBlueprint.
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
            // pool либо null/пустой (не требует blueprint), либо непустая строка.
            if (pool != null) {
                check(!pool.isEmpty(),
                        "AssemblerRecipe blueprintPool if non-null must be non-empty: " + pool);
            }
            // requiresBlueprint должен соответствовать pool.
            check(r.requiresBlueprint() == (pool != null && !pool.isEmpty()),
                    "requiresBlueprint must match (pool != null && !pool.isEmpty())");
        }
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 4: ChemicalPlantRecipe — структура, fluids.
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
        // Каждый chemplant-рецепт должен иметь хотя бы один fluid или item input/output.
        // (хотя бы что-то перерабатывать во что-то).
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
    //  Группа 5: Blueprint pool — ItemBlueprintFolder, isBlueprintPoolAllowed.
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
        // Пустой стак (ItemStack.EMPTY) не должен крашить getBlueprintPool.
        String pool = ItemBlueprintFolder.getBlueprintPool(ItemStack.EMPTY);
        check(pool != null && pool.isEmpty(),
                "EMPTY stack pool must be empty string, got: " + pool);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeIndex_byPool_assembler(GameTestHelper helper) {
        // Если среди assembler-рецептов есть рецепты с blueprintPool, индекс должен их группировать.
        Level level = helper.getLevel();
        List<AssemblerRecipe> all = ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE);
        check(!all.isEmpty(), "Need assembler recipes");
        // Находим хотя бы один рецепт с pool (если есть).
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
    //  Группа 6: Energy gate — hasEnoughEnergyToStartCraft / hasEnoughEnergyForTick.
    //  Статические методы MachineModuleBase — чистая логика без зависимостей.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_zeroCost_alwaysEnough(GameTestHelper helper) {
        // energyPerTick == 0 → всегда достаточно (бесплатный крафт).
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
        // maxProgress == 0 → всегда достаточно (защита от деления на 0).
        check(MachineModuleBase.hasEnoughEnergyToStartCraft(0.0, 100, 50, 0),
                "hasEnoughEnergyToStartCraft with maxProgress=0 must be true");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_progressStarted_skipBufferCheck(GameTestHelper helper) {
        // progress > 0 → крафт уже идёт, не требуем полный буфер энергии.
        check(MachineModuleBase.hasEnoughEnergyToStartCraft(1.0, 10, 50, 100),
                "hasEnoughEnergyToStartCraft with progress>0 must be true (already crafting)");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_notEnoughForFullCycle(GameTestHelper helper) {
        // energyPerTick=50, maxProgress=100 → total=5000; stored=1000 → мало (progress=0).
        check(!MachineModuleBase.hasEnoughEnergyToStartCraft(0.0, 1000, 50, 100),
                "hasEnoughEnergyToStartCraft: 1000 < 5000 must be false");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_enoughForFullCycle(GameTestHelper helper) {
        // energyPerTick=50, maxProgress=100 → total=5000; stored=5000 → достаточно.
        check(MachineModuleBase.hasEnoughEnergyToStartCraft(0.0, 5000, 50, 100),
                "hasEnoughEnergyToStartCraft: 5000 >= 5000 must be true");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void energyGate_forTick_exact(GameTestHelper helper) {
        // stored == energyPerTick → достаточно ровно на тик.
        check(MachineModuleBase.hasEnoughEnergyForTick(100, 100),
                "hasEnoughEnergyForTick: 100 >= 100 must be true");
        check(!MachineModuleBase.hasEnoughEnergyForTick(99, 100),
                "hasEnoughEnergyForTick: 99 < 100 must be false");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 7: MachineModuleAdvancedAssembler — update, progress, processCraft.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerModule_noRecipe_noProgress(GameTestHelper helper) {
        Level level = helper.getLevel();
        ModItemStackHandler inv = makeInventory(20);
        MachineModuleAdvancedAssembler mod = makeAssemblerModule(level, inv, 100_000L, 100_000L);
        // Инвентарь пуст → нет рецепта → update не должен прогрессировать.
        mod.update(1.0, 1.0, true, null);
        checkEq(0.0, mod.getProgress(), "progress must stay 0 with no recipe");
        check(!mod.isProcessing(), "isProcessing must be false with no recipe");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void assemblerModule_lowEnergy_noStart(GameTestHelper helper) {
        Level level = helper.getLevel();
        ModItemStackHandler inv = makeInventory(20);
        // Энергии 0 → крафт не стартует (assembler требует полный буфер).
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
        // Тихий succeed при пустых рецептах маскировал полностью сломанный крафт.
        check(!recipes.isEmpty(), "No recipes found to execute test — see assemblerRecipes_loaded for diagnostics");
        // Берём первый рецепт с минимальными требованиями.
        AssemblerRecipe recipe = recipes.get(0);
        int duration = recipe.getDuration();
        long power = recipe.getPowerConsumption();
        long totalEnergy = power * duration;

        ModItemStackHandler inv = makeInventory(20);
        MachineModuleAdvancedAssembler mod = makeAssemblerModule(level, inv, totalEnergy * 2, totalEnergy * 2);

        // Заполняем входные слоты предметами, удовлетворяющими ингредиентам.
        // Ингредиенты — NonNullList<Ingredient> (расширенный по count).
        net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> ings =
                recipe.getIngredients();
        int inputSlotIdx = 0;
        for (net.minecraft.world.item.crafting.Ingredient ing : ings) {
            if (ing.isEmpty()) continue;
            // Берём первый подходящий стак из Ingredient.
            ItemStack[] matchingStacks = ing.getItems();
            if (matchingStacks.length == 0 || matchingStacks[0].isEmpty()) continue;
            ItemStack sample = matchingStacks[0].copy();
            sample.setCount(sample.getMaxStackSize()); // с запасом
            inv.setStackInSlot(4 + inputSlotIdx, sample);
            inputSlotIdx++;
            if (inputSlotIdx >= 12) break;
        }

        // Выбираем рецепт.
        mod.setPreferredRecipe(recipe);

        // Симулируем duration тиков (с overdrive для ускорения).
        double speed = Math.max(1.0, duration); // 1 тик = весь цикл
        mod.update(speed, 1.0, true, null);

        // После одного update с speed=duration прогресс должен достичь maxProgress и craft выполниться.
        ItemStack output = inv.getStackInSlot(16);
        check(!output.isEmpty(),
                "Assembler module must produce output after full cycle. Output=" + output);

        // Доп. проверка: output соответствует рецепту.
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
        // extraCondition=false → даже с энергией крафт не идёт.
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
    //  Группа 8: MachineModuleChemplant — выбор рецепта по ID, blueprint-gating.
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
        // Нет выбранного рецепта → peekRecipe возвращает null.
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
        // Если у рецепта есть blueprintPool, а на машине лежит папка с дргуим pool —
        // updateAndGetDirty должен сбросить selectedRecipeId.
        Level level = helper.getLevel();
        List<ChemicalPlantRecipe> recipes = RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE);
        // Ищем рецепт с blueprintPool.
        ChemicalPlantRecipe pooledRecipe = null;
        for (ChemicalPlantRecipe r : recipes) {
            if (r.getBlueprintPool() != null && !r.getBlueprintPool().isEmpty()) {
                pooledRecipe = r;
                break;
            }
        }
        if (pooledRecipe == null) {
            // Нет рецептов с pool — нечего тестировать.
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

        // Папка с дргуим pool.
        ItemStack wrongFolder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        ItemBlueprintFolder.writeBlueprintPool(wrongFolder, pool + "_wrong");

        mod.updateAndGetDirty(1.0, 1.0, true, wrongFolder);
        check(mod.peekRecipe(level) == null,
                "peekRecipe must be null after blueprint pool mismatch");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void chemplantModule_blueprintMatch_keepsSelection(GameTestHelper helper) {
        // Если у рецепта есть blueprintPool, и папка имеет тот же pool — выбор сохраняется.
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
    //  Группа 9: RecipeType registration — типы зарегистрированы и совпадают.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "machines", timeoutTicks = 100)
    public static void recipeType_assembler_singleton(GameTestHelper helper) {
        check(AssemblerRecipe.Type.INSTANCE != null, "AssemblerRecipe.Type.INSTANCE must not be null");
        // Используем RecipeHooks.getAllRecipes — кросс-версионная обёртка,
        // разворачивающая RecipeHolder на 1.21.1.
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
