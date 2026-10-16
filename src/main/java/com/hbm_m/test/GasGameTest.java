package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.gas.BlockGasAsbestos;
import com.hbm_m.block.gas.BlockGasBase;
import com.hbm_m.block.gas.BlockGasChlorine;
import com.hbm_m.block.gas.BlockGasCoal;
import com.hbm_m.block.gas.BlockGasExplosive;
import com.hbm_m.block.gas.BlockGasFlammable;
import com.hbm_m.block.gas.BlockGasMeltdown;
import com.hbm_m.block.gas.BlockGasMonoxide;
import com.hbm_m.block.gas.BlockGasRadon;
import com.hbm_m.block.gas.BlockGasRadonDense;
import com.hbm_m.block.gas.BlockGasRadonTomb;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.item.gasmask.ItemGasMaskFilter;
import com.hbm_m.item.gasmask.GasMaskUtil;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Кроссплатформенный набор GameTest-ов для системы газов ({@code com.hbm_m.block.gas}).
 *
 * <p>Покрывает (порт {@code com.hbm.blocks.gas} 1.7.10):
 * <ul>
 *   <li>регистрацию всех 10 газов и их общие свойства (замена воздуха, пустая коллизия,
 *       невидимость, нерушимость, DESTROY при поршне, random ticks);</li>
 *   <li>распространение: детерминированное падение monoxide, подъём radon (1/5 UP),
 *       движение через проём, непроходимость сквозь solid, коррупция травы radon_dense;</li>
 *   <li>начисление «очков» болезни при контакте: asbestos (+1/тик), coal (+10/тик black lung),
 *       радиация radon/radon_dense/meltdown/tomb — на свинье и на игроке;</li>
 *   <li>смерть при предельной дозе: {@code maxAsbestos}/{@code maxBlackLung} → 1000 урона,
 *       счётчик сбрасывается; доведение до дозы через газ;</li>
 *   <li>защиту: противогаз + фильтр блокирует начисление и изнашивается (1/контакт),
 *       tomb игнорирует защиту (RAD_BYPASS), mono-фильтр защищает от monoxide;</li>
 *   <li>эффекты: chlorine (слепота/отравление/иссушение/замедление), monoxide (1 урон/тик);</li>
 *   <li>воспламенение: flammable от факела и горящей сущности, explosive — со взрывом;</li>
 *   <li>фиделити 1.7.10: radon/radon_dense/meltdown/tomb копят radEnv через contaminate,
 *       эффект радиации (15с / 60с amp2), tomb снимает radaway, meltdown расползается
 *       в radon_dense (1/7) и качает чанк-радиацию под небом, испарение radon_dense
 *       оставляет fallout, трава → waste_earth.</li>
 * </ul>
 *
 * <p>Механика тестов:
 * <ul>
 *   <li>прямые вызовы {@code block.entityInside(...)} — детерминированная проверка
 *       начисления/эффектов без мирового цикла (это тот же метод, который ваниль
 *       вызывает в {@code Entity.checkInsideBlocks});</li>
 *   <li>мировые тесты на герметичных каменных комнатах внутри {@code hbm_m:empty5x5x5} —
 *       газ физически не может покинуть клетку (все соседи solid), поэтому
 *       {@code entityInside} срабатывает каждый тик;</li>
 *   <li>для вероятностных событий (движение в случайном направлении, испарение, коррупция)
 *       используется {@code succeedWhen} — опрос условия каждый тик до успеха; вероятность
 *       сбоя любого теста ≤ 1e-4 (анализ в комментариях к каждому тесту).</li>
 * </ul>
 *
 * <p>Запуск: {@code ./gradlew :1.20.1-forge:runGameTestServer} /
 * {@code ./gradlew :1.21.1-neoforge:runGameTestServer}, batch {@code gas}.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class GasGameTest {

    private GasGameTest() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Константы и хелперы.
    // ════════════════════════════════════════════════════════════════════════

    /** Центр внутренней области 5×5×5 (герметичная клетка с газом). */
    private static final BlockPos CENTER = new BlockPos(2, 1, 2);

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void checkEq(float expected, float actual, String msg) {
        if (Math.abs(expected - actual) > 1e-3f) {
            throw new GameTestAssertException(msg + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private static void checkBlockIs(GameTestHelper helper, BlockPos local, Block block, String msg) {
        BlockState s = helper.getBlockState(local);
        check(s.is(block), msg + " (at " + local.toShortString() + " found " + s.getBlock() + ")");
    }

    /** Заполняет бокс блоком (локальные координаты шаблона). */
    private static void fillBox(GameTestHelper helper, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    helper.setBlock(new BlockPos(x, y, z), block);
    }

    /** Герметичная камера: весь шаблон — камень, в центре — газ. Газ не может никуда сместиться. */
    private static void buildSealedCell(GameTestHelper helper, Block gas) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        helper.setBlock(CENTER, gas);
    }

    /**
     * Прямой контакт с газом — тот же вызов, который ваниль делает из Entity.checkInsideBlocks.
     * Через BlockStateBase.entityInside: на 1.21.1 метод блока protected, а state-мост публичен
     * в обеих версиях.
     */
    private static void contacts(GameTestHelper helper, Block gas, LivingEntity living, int times) {
        BlockState state = gas.defaultBlockState();
        BlockPos pos = helper.absolutePos(CENTER);
        for (int i = 0; i < times; i++) {
            state.entityInside(helper.getLevel(), pos, living);
        }
    }

    private static Pig spawnPig(GameTestHelper helper, BlockPos local) {
        BlockPos abs = helper.absolutePos(local);
        Pig pig = EntityType.PIG.create(helper.getLevel());
        check(pig != null, "Pig must be creatable");
        pig.moveTo(abs.getX() + 0.5D, abs.getY(), abs.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(pig);
        return pig;
    }

    private static Cow makeCow(GameTestHelper helper) {
        Cow cow = EntityType.COW.create(helper.getLevel());
        check(cow != null, "Cow must be creatable");
        return cow;
    }

    /**
     * Survival-игрок для тестов газов. ВАЖНО: ванильный {@code makeMockPlayer()} на 1.20.1
     * возвращает КРЕАТИВНОГО игрока, а креатив/спектатор игнорируют газы
     * (см. {@link BlockGasBase#entityInside}) — поэтому своя anonymous-реализация.
     */
    private static Player makeSurvivalPlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return new Player(helper.getLevel(), BlockPos.ZERO, 0.0F,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "gas-test-player")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
         *///?}
    }

    /** Надевает противогаз с установленным фильтром. */
    private static void equipMask(LivingEntity living, Item filter) {
        ItemStack mask = new ItemStack(ModItems.GAS_MASK.get());
        IGasMask.installFilter(mask, filter);
        living.setItemSlot(EquipmentSlot.HEAD, mask);
    }

    /** Остаточный износ фильтра на надетой маске (с проверками, что маска и фильтр на месте). */
    private static int wornFilterDamage(LivingEntity living) {
        ItemStack mask = GasMaskUtil.resolveWornMask(living);
        check(!mask.isEmpty() && mask.getItem() instanceof IGasMask, "gas mask must be worn on head");
        check(IGasMask.hasFilter(mask), "filter must be installed in the mask");
        return IGasMask.getFilterDamage(mask);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 1: регистрация и общие свойства.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_blocksRegistered(GameTestHelper helper) {
        Block asbestos = ModBlocks.GAS_ASBESTOS.get();
        check(asbestos != null, "gas_asbestos present");
        check(ModBlocks.GAS_COAL.get() != null, "gas_coal present");
        check(ModBlocks.CHLORINE_GAS.get() != null, "chlorine_gas present");
        check(ModBlocks.GAS_EXPLOSIVE.get() != null, "gas_explosive present");
        check(ModBlocks.GAS_FLAMMABLE.get() != null, "gas_flammable present");
        check(ModBlocks.GAS_MELTDOWN.get() != null, "gas_meltdown present");
        check(ModBlocks.GAS_MONOXIDE.get() != null, "gas_monoxide present");
        check(ModBlocks.GAS_RADON.get() != null, "gas_radon present");
        check(ModBlocks.GAS_RADON_DENSE.get() != null, "gas_radon_dense present");
        check(ModBlocks.GAS_RADON_TOMB.get() != null, "gas_radon_tomb present");

        check(asbestos instanceof BlockGasAsbestos, "gas_asbestos → BlockGasAsbestos");
        check(ModBlocks.GAS_COAL.get() instanceof BlockGasCoal, "gas_coal → BlockGasCoal");
        check(ModBlocks.CHLORINE_GAS.get() instanceof BlockGasChlorine, "chlorine_gas → BlockGasChlorine");
        check(ModBlocks.GAS_EXPLOSIVE.get() instanceof BlockGasExplosive, "gas_explosive → BlockGasExplosive");
        check(ModBlocks.GAS_FLAMMABLE.get() instanceof BlockGasFlammable, "gas_flammable → BlockGasFlammable");
        check(ModBlocks.GAS_MELTDOWN.get() instanceof BlockGasMeltdown, "gas_meltdown → BlockGasMeltdown");
        check(ModBlocks.GAS_MONOXIDE.get() instanceof BlockGasMonoxide, "gas_monoxide → BlockGasMonoxide");
        check(ModBlocks.GAS_RADON.get() instanceof BlockGasRadon, "gas_radon → BlockGasRadon");
        check(ModBlocks.GAS_RADON_DENSE.get() instanceof BlockGasRadonDense, "gas_radon_dense → BlockGasRadonDense");
        check(ModBlocks.GAS_RADON_TOMB.get() instanceof BlockGasRadonTomb, "gas_radon_tomb → BlockGasRadonTomb");
        // Explosive наследует Flammable (как в оригинале) — остальные классы различны.
        check(BlockGasExplosive.class.getSuperclass() == BlockGasFlammable.class,
                "gas_explosive extends gas_flammable (1.7.10 parity)");

        // Идентификаторы 1:1 с портом (chlorine — историческое имя из оригинала).
        check(BuiltInRegistries.BLOCK.getKey(ModBlocks.CHLORINE_GAS.get()).getPath().equals("chlorine_gas"),
                "chlorine keeps its 'chlorine_gas' id");
        check(BuiltInRegistries.BLOCK.getKey(ModBlocks.GAS_RADON_DENSE.get()).getPath().equals("gas_radon_dense"),
                "dense radon id");
        check(BuiltInRegistries.BLOCK.getKey(ModBlocks.GAS_RADON_TOMB.get()).getPath().equals("gas_radon_tomb"),
                "tomb radon id");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_blockProperties(GameTestHelper helper) {
        Block[] gases = {
                ModBlocks.GAS_ASBESTOS.get(), ModBlocks.GAS_COAL.get(), ModBlocks.CHLORINE_GAS.get(),
                ModBlocks.GAS_EXPLOSIVE.get(), ModBlocks.GAS_FLAMMABLE.get(), ModBlocks.GAS_MELTDOWN.get(),
                ModBlocks.GAS_MONOXIDE.get(), ModBlocks.GAS_RADON.get(), ModBlocks.GAS_RADON_DENSE.get(),
                ModBlocks.GAS_RADON_TOMB.get()
        };
        Level level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        int i = 0;
        for (Block gas : gases) {
            BlockState state = gas.defaultBlockState();
            String name = BuiltInRegistries.BLOCK.getKey(gas).getPath();
            check(state.canBeReplaced(), name + " must be replaceable (fills air)");
            check(state.getCollisionShape(level, pos).isEmpty(), name + " must have no collision");
            check(state.getShape(level, pos).isEmpty(), name + " must have empty outline shape");
            check(state.getRenderShape() == RenderShape.INVISIBLE, name + " must render invisible");
            check(state.getDestroySpeed(level, pos) == -1.0F, name + " must be unbreakable in survival (-1.0F)");
            check(state.getPistonPushReaction() == PushReaction.DESTROY, name + " must be destroyed by pistons");
            check(state.isRandomlyTicking(), name + " must have random ticks (anti-stuck rescheduler)");
            check(state.getFluidState().isEmpty(), name + " is not a fluid");
            i++;
        }
        check(i == 10, "exactly 10 gas blocks");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_onPlaceSchedulesTick(GameTestHelper helper) {
        Block gas = ModBlocks.GAS_ASBESTOS.get();
        BlockPos local = new BlockPos(1, 1, 1);
        helper.setBlock(local, gas);
        boolean scheduled = helper.getLevel().getBlockTicks()
                .hasScheduledTick(helper.absolutePos(local), gas);
        check(scheduled, "onPlace must schedule a tick (movement driver)");
        // Убираем газ в тот же тик, чтобы он не начал мигрировать за пределы шаблона.
        helper.setBlock(local, Blocks.AIR);
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 2: направления движения и задержки (детерминированные, RandomSource инъекция).
    //  Допуски ±5σ и шире — тесты фактически точные.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_directionsSinkingGases(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        RandomSource rnd = RandomSource.create(42L);

        // Monoxide: ВСЕГДА вниз (как в оригинале — тяжёлый газ).
        BlockGasMonoxide mono = (BlockGasMonoxide) ModBlocks.GAS_MONOXIDE.get();
        for (int i = 0; i < 100; i++) {
            check(mono.getFirstDirection(level, pos, rnd) == Direction.DOWN,
                    "monoxide getFirstDirection must always be DOWN");
        }

        // Asbestos и Coal: DOWN идёт и из первой ветки (1/5), и из случайного 6-направления
        // (4/5 × 1/6): p = 1/5 + 4/5·1/6 = 11/30 ≈ 0.367. n=1000 → [285, 450] (±5.5σ).
        int down = 0;
        BlockGasAsbestos asbestos = (BlockGasAsbestos) ModBlocks.GAS_ASBESTOS.get();
        for (int i = 0; i < 1000; i++) {
            if (asbestos.getFirstDirection(level, pos, rnd) == Direction.DOWN) down++;
        }
        check(down >= 285 && down <= 450, "asbestos DOWN-bias 11/30 out of range: " + down);
        down = 0;
        BlockGasCoal coal = (BlockGasCoal) ModBlocks.GAS_COAL.get();
        for (int i = 0; i < 1000; i++) {
            if (coal.getFirstDirection(level, pos, rnd) == Direction.DOWN) down++;
        }
        check(down >= 285 && down <= 450, "coal DOWN-bias 11/30 out of range: " + down);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_directionsRisingGases(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        RandomSource rnd = RandomSource.create(42L);

        // Radon и radon_dense: 1/5 вверх (лёгкий радиоактивный газ).
        int up = countUp((BlockGasRadon) ModBlocks.GAS_RADON.get(), level, pos, rnd, 1000);
        check(up >= 130 && up <= 270, "radon UP-bias 1/5 out of range: " + up);
        up = countUp((BlockGasRadonDense) ModBlocks.GAS_RADON_DENSE.get(), level, pos, rnd, 1000);
        check(up >= 130 && up <= 270, "radon_dense UP-bias 1/5 out of range: " + up);
        // Tomb: 1/3 вверх. p=1/3, n=900 → [225, 375].
        up = countUp((BlockGasRadonTomb) ModBlocks.GAS_RADON_TOMB.get(), level, pos, rnd, 900);
        check(up >= 225 && up <= 375, "radon_tomb UP-bias 1/3 out of range: " + up);
        // Meltdown: 1/2 вверх. p=1/2, n=1000 → [415, 585].
        up = countUp((BlockGasMeltdown) ModBlocks.GAS_MELTDOWN.get(), level, pos, rnd, 1000);
        check(up >= 415 && up <= 585, "meltdown UP-bias 1/2 out of range: " + up);
        helper.succeed();
    }

    private static int countUp(BlockGasBase gas, Level level, BlockPos pos, RandomSource rnd, int n) {
        int up = 0;
        for (int i = 0; i < n; i++) {
            if (gas.getFirstDirection(level, pos, rnd) == Direction.UP) up++;
        }
        return up;
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_directionsFlammableAndDelays(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        RandomSource rnd = RandomSource.create(42L);

        // Flammable: 1/3 вертикальное (UP или DOWN), иначе горизонталь. p=1/3, n=1000 → [255, 415].
        BlockGasFlammable flammable = (BlockGasFlammable) ModBlocks.GAS_FLAMMABLE.get();
        int vertical = 0;
        for (int i = 0; i < 1000; i++) {
            Direction d = flammable.getFirstDirection(level, pos, rnd);
            if (d == Direction.UP || d == Direction.DOWN) vertical++;
        }
        check(vertical >= 255 && vertical <= 415, "flammable vertical-bias 1/3 out of range: " + vertical);

        // Задержки: базовая — 2 тика, у flammable — случайная 16..20 (как в оригинале).
        for (int i = 0; i < 50; i++) {
            check(flammable.getDelay(level, rnd) >= 16 && flammable.getDelay(level, rnd) <= 20,
                    "flammable delay must be in [16, 20]");
        }
        BlockGasAsbestos asbestos = (BlockGasAsbestos) ModBlocks.GAS_ASBESTOS.get();
        for (int i = 0; i < 50; i++) {
            check(asbestos.getDelay(level, rnd) == 2, "base gas delay must be 2 ticks");
        }
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 3: распространение (мировой цикл, герметичные комнаты).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Monoxide тонет детерминированно: первая попытка — всегда DOWN.
     * 3 независимые шахты (газ на y=2, воздух на y=1): падение на первом же
     * запланированном тике (~10). Сбояет только если все 3 клетки испарятся на первом
     * тике (1/100 каждая) → p ≤ 1e-6.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 80)
    public static void gas_monoxideSinksIntoAirPocket(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        Block mono = ModBlocks.GAS_MONOXIDE.get();
        int[][] shafts = {{1, 1}, {1, 3}, {3, 1}};
        for (int[] s : shafts) {
            helper.setBlock(new BlockPos(s[0], 1, s[1]), Blocks.AIR);
            helper.setBlock(new BlockPos(s[0], 2, s[1]), mono);
        }
        helper.succeedWhen(() -> {
            boolean any = false;
            for (int[] s : shafts) {
                if (helper.getBlockState(new BlockPos(s[0], 1, s[1])).is(mono)) {
                    any = true;
                    break;
                }
            }
            check(any, "monoxide must sink into the air pocket below (getFirstDirection == DOWN)");
        });
    }

    /**
     * Газ не проходит сквозь solid: monoxide в герметичной клетке с воздухом только сверху —
     * вверх он не умеет. Полная детерминированность: «наверху воздух» не нарушается ни
     * испарением, ни движением.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 140)
    public static void gas_cannotLeakThroughSolid(GameTestHelper helper) {
        buildSealedCell(helper, ModBlocks.GAS_MONOXIDE.get());
        Block mono = ModBlocks.GAS_MONOXIDE.get();
        helper.startSequence().thenExecuteAfter(100, () -> {
            check(!helper.getBlockState(new BlockPos(2, 2, 2)).is(mono),
                    "monoxide must never rise through solid rock");
            // По всему шаблону газ либо испарился, либо остался ровно один — в исходной клетке.
            int found = 0;
            boolean atCenter = false;
            Block foundBlock = null;
            for (int x = 0; x < 5; x++)
                for (int y = 0; y < 5; y++)
                    for (int z = 0; z < 5; z++) {
                        BlockState s = helper.getBlockState(new BlockPos(x, y, z));
                        if (s.is(mono)) {
                            found++;
                            atCenter |= (x == 2 && y == 1 && z == 2);
                        }
                    }
            check(found == 0 || (found == 1 && atCenter),
                    "sealed monoxide must not leak elsewhere (found " + found + ")");
        }).thenSucceed();
    }

    /**
     * Распространение через проём: газ в камере, соединённой с одной пустой клеткой.
     * ~37% движение за попытку (каждые 2 тика), но испарение 1/50 обгоняет движение
     * в ~5% случаев на проём → 3 независимых проёма: p(сбоя) ≈ 1.6e-4.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 150)
    public static void gas_asbestosSpreadsThroughDoorway(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        Block asbestos = ModBlocks.GAS_ASBESTOS.get();
        int[][] doors = {{2, 1, 2, 1, 1, 2}, {1, 1, 1, 1, 1, 3}, {3, 1, 3, 2, 1, 3}};
        for (int[] d : doors) {
            helper.setBlock(new BlockPos(d[0], d[1], d[2]), Blocks.AIR);      // клетка-приёмник
            helper.setBlock(new BlockPos(d[3], d[4], d[5]), asbestos);        // исходная клетка
        }
        helper.succeedWhen(() -> {
            boolean any = false;
            for (int[] d : doors) {
                any |= helper.getBlockState(new BlockPos(d[0], d[1], d[2])).is(asbestos);
            }
            check(any, "asbestos must spread through an opening into the air pocket");
        });
    }

    /**
     * Radon поднимается: 3 независимые шахты из 2 воздушных клеток над газом, все соседи solid.
     * На каждой попытке (раз в 2 тика): подъём p=1/5, испарение p=1/50 → «испарение обгонит
     * подъём» в 1/11 случаев на шахту; 3 шахты → p(сбоя) ≈ 7.5e-4.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 240)
    public static void gas_radonRises(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        Block radon = ModBlocks.GAS_RADON.get();
        int[][] columns = {{1, 1}, {1, 3}, {3, 1}};
        for (int[] c : columns) {
            helper.setBlock(new BlockPos(c[0], 2, c[1]), Blocks.AIR);
            helper.setBlock(new BlockPos(c[0], 3, c[1]), Blocks.AIR);
            helper.setBlock(new BlockPos(c[0], 1, c[1]), radon);
        }
        helper.succeedWhen(() -> {
            boolean inShaft = false;
            for (int[] c : columns) {
                inShaft |= helper.getBlockState(new BlockPos(c[0], 2, c[1])).is(radon)
                        || helper.getBlockState(new BlockPos(c[0], 3, c[1])).is(radon);
            }
            check(inShaft, "radon must rise into the air shaft (1/5 UP per attempt)");
        });
    }

    /**
     * Radon_dense корруптирует траву под собой в coarse dirt (1/20 за тик).
     * 3×3 = 9 газов над 9 травами; ожидаемое число коррупций до испарения ≈ 13 →
     * p(ни одной) ≈ 1e-6.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 240)
    public static void gas_radonDenseCorruptsGrass(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        Block dense = ModBlocks.GAS_RADON_DENSE.get();
        for (int x = 1; x <= 3; x++)
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
                helper.setBlock(new BlockPos(x, 1, z), dense);
            }
        helper.succeedWhen(() -> {
            boolean any = false;
            for (int x = 1; x <= 3 && !any; x++)
                for (int z = 1; z <= 3 && !any; z++)
                    any = helper.getBlockState(new BlockPos(x, 0, z)).is(ModBlocks.WASTE_EARTH.get());
            check(any, "radon_dense must corrupt grass below into waste_earth (1/20 per tick, 1.7.10 parity)");
        });
    }

    /**
     * Flammable вспыхивает от факела-соседа: факел ставится ПОСЛЕ газа → neighborChanged →
     * запланированный тик через 2 → combust. Детерминированно, до первого тика движения
     * (+10) — испарение не успевает.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 40)
    public static void gas_flammableIgnitesFromTorch(GameTestHelper helper) {
        buildSealedCell(helper, ModBlocks.GAS_FLAMMABLE.get());
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.TORCH);
        helper.succeedWhen(() ->
                checkBlockIs(helper, CENTER, Blocks.FIRE, "flammable gas must ignite from a torch neighbour"));
    }

    /**
     * Explosive детонирует от факела: combust + взрыв 3.0 (TNT interaction).
     * Оболочка из обсидиана сдерживает взрыв (resistance 1200 против intensity ~2.8).
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 40)
    public static void gas_explosiveDetonatesFromTorch(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.OBSIDIAN);
        helper.setBlock(CENTER, ModBlocks.GAS_EXPLOSIVE.get());
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.TORCH);
        helper.succeedWhen(() -> {
            BlockState s = helper.getBlockState(CENTER);
            check(!s.is(ModBlocks.GAS_EXPLOSIVE.get()),
                    "explosive gas must detonate from a torch neighbour (block gone)");
        });
    }

    /** Горящая сущность поджигает flammable-газ (как в оригинале). */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 40)
    public static void gas_flammableIgnitesFromBurningPig(GameTestHelper helper) {
        buildSealedCell(helper, ModBlocks.GAS_FLAMMABLE.get());
        Pig pig = spawnPig(helper, CENTER);
        pig.setRemainingFireTicks(600);
        check(pig.isOnFire(), "pig must be on fire");
        helper.succeedWhen(() ->
                checkBlockIs(helper, CENTER, Blocks.FIRE, "burning entity must ignite flammable gas"));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 4: начисление очков в мировом цикле (свинья в герметичной клетке).
    //  До первого запланированного тика газа (+10) успевает ≥ 9 тиков контакта →
    //  пороги с 2× запасом; испарение газа ничего не ломает.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 60)
    public static void gas_exposureAsbestosAccumulates(GameTestHelper helper) {
        buildSealedCell(helper, ModBlocks.GAS_ASBESTOS.get());
        Pig pig = spawnPig(helper, CENTER);
        helper.startSequence().thenExecuteAfter(20, () ->
                check(HbmLivingProps.getAsbestos(pig) >= 5,
                        "asbestos must accumulate while standing in gas (+1 per contact tick), got "
                                + HbmLivingProps.getAsbestos(pig))
        ).thenSucceed();
    }

    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 60)
    public static void gas_exposureCoalBlackLungAccumulates(GameTestHelper helper) {
        buildSealedCell(helper, ModBlocks.GAS_COAL.get());
        Pig pig = spawnPig(helper, CENTER);
        helper.startSequence().thenExecuteAfter(20, () ->
                check(HbmLivingProps.getBlackLung(pig) >= 50,
                        "black lung must accumulate while standing in coal gas (+10 per contact tick), got "
                                + HbmLivingProps.getBlackLung(pig))
        ).thenSucceed();
    }

    /**
     * Доведение до предельной дозы ЧЕРЕЗ ГАЗ: свинье остаётся 2 тика контакта до maxAsbestos —
     * на втором контакте газ убивает её (1000 урона) и сбрасывает счётчик.
     * Смерть на 2-й тик контакта (~t=3) — детерминированно.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 60)
    public static void gas_exposureReachesLethalDose(GameTestHelper helper) {
        buildSealedCell(helper, ModBlocks.GAS_ASBESTOS.get());
        Pig pig = spawnPig(helper, CENTER);
        HbmLivingProps.setAsbestos(pig, HbmLivingProps.maxAsbestos - 2);
        helper.startSequence().thenExecuteAfter(30, () -> {
            check(pig.isDeadOrDying() || pig.getHealth() <= 0f,
                    "reaching maxAsbestos through gas exposure must kill (1000 damage)");
            check(HbmLivingProps.getAsbestos(pig) < 100,
                    "lethal dose must consume the counter (reset), got " + HbmLivingProps.getAsbestos(pig));
        }).thenSucceed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 5: начисления при контакте (прямые вызовы entityInside — детерминированно).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactAsbestosExact(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        check(!ArmorRegistry.hasProtection(cow, 3, HazardClass.PARTICLE_FINE),
                "sanity: bare cow is unprotected");
        contacts(helper, ModBlocks.GAS_ASBESTOS.get(), cow, 5);
        check(HbmLivingProps.getAsbestos(cow) == 5,
                "asbestos contact must add +1 per tick, got " + HbmLivingProps.getAsbestos(cow));
        check(HbmLivingProps.getBlackLung(cow) == 0, "asbestos gas must not touch black lung");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactAsbestosProtected(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        equipMask(cow, ModItems.GAS_MASK_FILTER.get());
        check(ArmorRegistry.hasProtection(cow, 3, HazardClass.PARTICLE_FINE),
                "sanity: mask + filter must protect from fine particles");
        contacts(helper, ModBlocks.GAS_ASBESTOS.get(), cow, 5);
        check(HbmLivingProps.getAsbestos(cow) == 0,
                "protected entity must not accumulate asbestos");
        check(wornFilterDamage(cow) == 5,
                "filter must take 1 damage per protected contact, got " + wornFilterDamage(cow));
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactCoalExact(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        check(!ArmorRegistry.hasProtection(cow, 3, HazardClass.PARTICLE_COARSE),
                "sanity: bare cow is unprotected");
        contacts(helper, ModBlocks.GAS_COAL.get(), cow, 5);
        check(HbmLivingProps.getBlackLung(cow) == 50,
                "coal gas contact must add +10 black lung per tick, got " + HbmLivingProps.getBlackLung(cow));
        check(HbmLivingProps.getAsbestos(cow) == 0, "coal gas must not touch asbestos");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactRadon(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        contacts(helper, ModBlocks.GAS_RADON.get(), cow, 10);
        checkEq(0.5f, HbmLivingProps.getRadiation(cow),
                "radon contact must add 0.05 RAD per tick (10 ticks → 0.5)");
        // 1.7.10: contaminate копит radEnv (Geiger-HUD); у коровы radMod = 1 → значения равны
        checkEq(0.5f, HbmLivingProps.getRadEnv(cow), "radon contact must accumulate radEnv");
        check(HbmLivingProps.getAsbestos(cow) == 10, "radon contact must add +1 asbestos per tick");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactRadonDense(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        contacts(helper, ModBlocks.GAS_RADON_DENSE.get(), cow, 10);
        checkEq(5.0f, HbmLivingProps.getRadiation(cow),
                "radon_dense contact must add 0.5 RAD per tick (10 ticks → 5.0)");
        checkEq(5.0f, HbmLivingProps.getRadEnv(cow), "radon_dense contact must accumulate radEnv");
        check(HbmLivingProps.getAsbestos(cow) == 50, "radon_dense contact must add +5 asbestos per tick");
        // 1.7.10: PotionEffect(HbmPotion.radiation, 15 * 20, 0)
        check(PlatformHooks.hasEffect(cow, ModEffects.RADIATION), "radon_dense must apply the radiation effect");
        MobEffectInstance inst = PlatformHooks.getEffect(cow, ModEffects.RADIATION);
        check(inst != null && inst.getDuration() == 15 * 20 && inst.getAmplifier() == 0,
                "radiation effect must be 15s amp 0, got " + (inst == null ? "null" : inst.getDuration() + "/" + inst.getAmplifier()));
        helper.succeed();
    }

    /** radon_dense: маска полностью блокирует заражение — только износ фильтра (как в оригинале). */
    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactRadonDenseProtected(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        equipMask(cow, ModItems.GAS_MASK_FILTER.get());
        contacts(helper, ModBlocks.GAS_RADON_DENSE.get(), cow, 10);
        checkEq(0.0f, HbmLivingProps.getRadiation(cow), "protected: radon_dense must add no radiation");
        checkEq(0.0f, HbmLivingProps.getRadEnv(cow), "protected: radon_dense must add no radEnv");
        check(!PlatformHooks.hasEffect(cow, ModEffects.RADIATION), "protected: no radiation effect");
        check(HbmLivingProps.getAsbestos(cow) == 0, "protected: no asbestos");
        check(wornFilterDamage(cow) == 10, "filter must take 1 damage per contact, got " + wornFilterDamage(cow));
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactMeltdown(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        contacts(helper, ModBlocks.GAS_MELTDOWN.get(), cow, 10);
        checkEq(5.0f, HbmLivingProps.getRadiation(cow),
                "meltdown contact must add 0.5 RAD per tick unprotected (10 ticks → 5.0)");
        checkEq(5.0f, HbmLivingProps.getRadEnv(cow), "meltdown contact must accumulate radEnv");
        check(HbmLivingProps.getAsbestos(cow) == 50, "meltdown contact must add +5 asbestos per tick");
        // 1.7.10: PotionEffect(HbmPotion.radiation, 60 * 20, 2) — всегда, даже в маске
        check(PlatformHooks.hasEffect(cow, ModEffects.RADIATION), "meltdown must apply the radiation effect");
        MobEffectInstance inst = PlatformHooks.getEffect(cow, ModEffects.RADIATION);
        check(inst != null && inst.getDuration() == 60 * 20 && inst.getAmplifier() == 2,
                "radiation effect must be 60s amp 2, got " + (inst == null ? "null" : inst.getDuration() + "/" + inst.getAmplifier()));
        helper.succeed();
    }

    /**
     * Meltdown: радиация идёт ВСЕГДА (в обход маски — как contaminate CREATIVE в оригинале),
     * асбест заблокирован маской, фильтр изнашивается.
     */
    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactMeltdownProtected(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        equipMask(cow, ModItems.GAS_MASK_FILTER.get());
        contacts(helper, ModBlocks.GAS_MELTDOWN.get(), cow, 10);
        checkEq(5.0f, HbmLivingProps.getRadiation(cow),
                "meltdown radiation must bypass the mask (0.5 per tick)");
        checkEq(5.0f, HbmLivingProps.getRadEnv(cow), "meltdown radEnv must bypass the mask");
        check(PlatformHooks.hasEffect(cow, ModEffects.RADIATION), "meltdown radiation effect must bypass the mask");
        check(HbmLivingProps.getAsbestos(cow) == 0, "mask must block meltdown asbestos");
        check(wornFilterDamage(cow) == 10, "filter must take 1 damage per tick, got " + wornFilterDamage(cow));
        helper.succeed();
    }

    /**
     * Tomb игнорирует защиту полностью (RAD_BYPASS «get fucked»): маска с фильтром не
     * спасает ни от радиации, ни от асбеста, и фильтр не изнашивается.
     */
    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactTombBypassesMask(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        equipMask(cow, ModItems.GAS_MASK_FILTER.get());
        contacts(helper, ModBlocks.GAS_RADON_TOMB.get(), cow, 5);
        checkEq(2.5f, HbmLivingProps.getRadiation(cow),
                "tomb radiation must bypass the mask (0.5 per tick)");
        checkEq(2.5f, HbmLivingProps.getRadEnv(cow), "tomb contact must accumulate radEnv");
        check(HbmLivingProps.getAsbestos(cow) == 50, "tomb asbestos (+10/tick) must bypass the mask");
        check(wornFilterDamage(cow) == 0, "tomb must not damage the filter (no protection path)");
        helper.succeed();
    }

    /**
     * 1.7.10 «get fucked»: tomb снимает эффект противоядия (radaway; radx в порте
     * пока не существует — ни эффекта, ни предмета).
     */
    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactTombRemovesRadaway(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        PlatformHooks.addEffect(cow, ModEffects.RADAWAY, 200, 0);
        check(PlatformHooks.hasEffect(cow, ModEffects.RADAWAY), "sanity: radaway must be applied");
        contacts(helper, ModBlocks.GAS_RADON_TOMB.get(), cow, 1);
        check(!PlatformHooks.hasEffect(cow, ModEffects.RADAWAY), "tomb must strip the radaway effect");
        checkEq(0.5f, HbmLivingProps.getRadiation(cow), "tomb contact after strip adds 0.5 RAD");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactChlorineEffects(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        contacts(helper, ModBlocks.CHLORINE_GAS.get(), cow, 1);
        check(cow.hasEffect(MobEffects.BLINDNESS), "chlorine must apply blindness");
        check(cow.hasEffect(MobEffects.POISON), "chlorine must apply poison");
        check(cow.hasEffect(MobEffects.WITHER), "chlorine must apply wither");
        check(cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "chlorine must apply slowness");
        check(cow.hasEffect(MobEffects.DIG_SLOWDOWN), "chlorine must apply mining fatigue");

        MobEffectInstance blind = cow.getEffect(MobEffects.BLINDNESS);
        check(blind.getDuration() == 5 * 20 && blind.getAmplifier() == 0, "blindness 5s amp 0");
        MobEffectInstance poison = cow.getEffect(MobEffects.POISON);
        check(poison.getDuration() == 20 * 20 && poison.getAmplifier() == 2, "poison 20s amp 2");
        MobEffectInstance wither = cow.getEffect(MobEffects.WITHER);
        check(wither.getDuration() == 1 * 20 && wither.getAmplifier() == 1, "wither 1s amp 1");
        MobEffectInstance slow = cow.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(slow.getDuration() == 30 * 20 && slow.getAmplifier() == 1, "slowness 30s amp 1");
        MobEffectInstance dig = cow.getEffect(MobEffects.DIG_SLOWDOWN);
        check(dig.getDuration() == 30 * 20 && dig.getAmplifier() == 2, "mining fatigue 30s amp 2");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactChlorineProtected(GameTestHelper helper) {
        Cow cow = makeCow(helper);
        equipMask(cow, ModItems.GAS_MASK_FILTER.get());
        check(ArmorRegistry.hasProtection(cow, 3, HazardClass.GAS_LUNG),
                "sanity: mask + filter must protect from lung gas");
        contacts(helper, ModBlocks.CHLORINE_GAS.get(), cow, 3);
        check(!cow.hasEffect(MobEffects.BLINDNESS) && !cow.hasEffect(MobEffects.POISON)
                && !cow.hasEffect(MobEffects.WITHER), "protected entity must get no chlorine effects");
        check(wornFilterDamage(cow) == 3, "filter must take 1 damage per protected contact");
        helper.succeed();
    }

    /**
     * Monoxide наносит 1 урон за контакт — но ванильные i-frames (invulnerableTime > 10
     * при amount ≤ lastHurt) пропускают повторный урон той же силы только раз в 10 тиков:
     * удары приходятся на t=1, 11, 21, ... Итог: 3 удара к t=21.
     * 3 независимые ячейки со свиньями и опрос succeedWhen: сбой только если газ испарится
     * (1/100 за тик) до 3-го удара ВО ВСЕХ трёх ячейках → p ≈ 2e-4.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 100)
    public static void gas_contactMonoxideDamages(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        Block mono = ModBlocks.GAS_MONOXIDE.get();
        int[][] cells = {{1, 1, 1}, {3, 1, 1}, {2, 1, 3}};
        Pig[] pigs = new Pig[cells.length];
        for (int i = 0; i < cells.length; i++) {
            helper.setBlock(new BlockPos(cells[i][0], cells[i][1], cells[i][2]), mono);
            pigs[i] = spawnPig(helper, new BlockPos(cells[i][0], cells[i][1], cells[i][2]));
        }
        float max = pigs[0].getMaxHealth();
        helper.succeedWhen(() -> {
            boolean hurt = false;
            for (Pig pig : pigs) {
                hurt |= pig.getHealth() <= max - 3f;
            }
            check(hurt, "monoxide gas must deal 1 damage per contact tick (i-frame gated to 1/10 ticks)");
        });
    }

    /** Mono-фильтр защищает от monoxide (базовый фильтр — НЕТ, у него нет GAS_MONOXIDE). */
    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_contactMonoxideProtected(GameTestHelper helper) {
        Pig pig = spawnPig(helper, new BlockPos(1, 1, 1));
        equipMask(pig, ModItems.GAS_MASK_FILTER_MONO.get());
        check(ArmorRegistry.hasProtection(pig, 3, HazardClass.GAS_MONOXIDE),
                "sanity: mono filter must protect from CO");
        float max = pig.getMaxHealth();
        contacts(helper, ModBlocks.GAS_MONOXIDE.get(), pig, 3);
        check(Math.abs(pig.getHealth() - max) < 1e-3f, "protected entity takes no monoxide damage");
        check(wornFilterDamage(pig) == 3, "mono filter must take 1 damage per contact");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 6: смерть при предельной дозе.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_lethalAsbestosKillsPig(GameTestHelper helper) {
        Pig pig = spawnPig(helper, new BlockPos(1, 1, 1));
        check(pig.isAlive(), "pig alive before the dose");
        HbmLivingProps.incrementAsbestos(pig, HbmLivingProps.maxAsbestos);
        check(pig.isDeadOrDying() || pig.getHealth() <= 0f,
                "maxAsbestos dose must kill (1000 damage bypass)");
        check(HbmLivingProps.getAsbestos(pig) == 0, "asbestos counter resets after lethal dose");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_lethalCoalKillsPig(GameTestHelper helper) {
        Pig pig = spawnPig(helper, new BlockPos(1, 1, 1));
        check(pig.isAlive(), "pig alive before the dose");
        HbmLivingProps.incrementBlackLung(pig, HbmLivingProps.maxBlackLung);
        check(pig.isDeadOrDying() || pig.getHealth() <= 0f,
                "maxBlackLung dose must kill (1000 damage bypass)");
        check(HbmLivingProps.getBlackLung(pig) == 0, "black lung counter resets after lethal dose");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 7: игрок — начисления, защита, смерть.
    //  Креатив/спектатор игнорируют газы, поэтому makeSurvivalPlayer.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_playerAsbestosAccumulates(GameTestHelper helper) {
        Player player = makeSurvivalPlayer(helper);
        contacts(helper, ModBlocks.GAS_ASBESTOS.get(), player, 5);
        check(HbmLivingProps.getAsbestos(player) == 5,
                "survival player must accumulate asbestos from gas, got " + HbmLivingProps.getAsbestos(player));
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_playerCoalAccumulates(GameTestHelper helper) {
        Player player = makeSurvivalPlayer(helper);
        contacts(helper, ModBlocks.GAS_COAL.get(), player, 5);
        check(HbmLivingProps.getBlackLung(player) == 50,
                "survival player must accumulate black lung from coal gas, got "
                        + HbmLivingProps.getBlackLung(player));
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_playerProtectedByGasMask(GameTestHelper helper) {
        Player player = makeSurvivalPlayer(helper);
        equipMask(player, ModItems.GAS_MASK_FILTER.get());
        contacts(helper, ModBlocks.GAS_ASBESTOS.get(), player, 5);
        check(HbmLivingProps.getAsbestos(player) == 0, "masked player must not accumulate asbestos");
        check(wornFilterDamage(player) == 5, "player's filter must take 1 damage per contact");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_playerAsbestosLethal(GameTestHelper helper) {
        Player player = makeSurvivalPlayer(helper);
        check(player.isAlive(), "player alive before the dose");
        HbmLivingProps.incrementAsbestos(player, HbmLivingProps.maxAsbestos);
        check(player.isDeadOrDying() || player.getHealth() <= 0f,
                "maxAsbestos dose must kill the player");
        check(HbmLivingProps.getAsbestos(player) == 0, "player asbestos counter resets after lethal dose");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 8: фиделити 1.7.10 — fallout при испарении, расползание meltdown,
    //  чанковая радиация при видимом небе.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 1.7.10: испарение radon_dense (1/30 за тик) оставляет на месте fallout
     * (ModBlocks.fallout → NUCLEAR_FALLOUT), если тот может выжить.
     * 9 газов в герметичной камере → первое испарение ожидается ~t≈30;
     * p(ни одного за 240 тиков) ≈ 0.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 240)
    public static void gas_radonDenseLeavesFallout(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        Block dense = ModBlocks.GAS_RADON_DENSE.get();
        for (int x = 1; x <= 3; x++)
            for (int z = 1; z <= 3; z++)
                helper.setBlock(new BlockPos(x, 1, z), dense);
        helper.succeedWhen(() -> {
            boolean any = false;
            for (int x = 0; x <= 4 && !any; x++)
                for (int y = 0; y <= 4 && !any; y++)
                    for (int z = 0; z <= 4 && !any; z++)
                        any = helper.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.NUCLEAR_FALLOUT.get());
            check(any, "dissipating radon_dense must leave a fallout block (1/30 per tick)");
        });
    }

    /**
     * 1.7.10: meltdown-газ с вероятностью 1/7 за тик превращает случайного
     * соседа-воздух в gas_radon_dense — газ расползается по отсеку.
     * Камера 3×3×3 воздуха с одним meltdown-газом: конверсия ~0.1/тик →
     * за 240 тиков практически гарантирована; появившиеся dense-газы живут
     * ~30 тиков, но появляются быстрее, чем испаряются.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 480)
    public static void gas_meltdownConvertsAirToRadonDense(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        // Воздушная камера 3×3×3 внутри каменной оболочки; 3 независимых газа-источника
        for (int x = 1; x <= 3; x++)
            for (int y = 1; y <= 3; y++)
                for (int z = 1; z <= 3; z++)
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.GAS_MELTDOWN.get());
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.GAS_MELTDOWN.get());
        helper.setBlock(new BlockPos(3, 3, 3), ModBlocks.GAS_MELTDOWN.get());
        helper.succeedWhen(() -> {
            int dense = 0;
            for (int x = 1; x <= 3; x++)
                for (int y = 1; y <= 3; y++)
                    for (int z = 1; z <= 3; z++)
                        if (helper.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.GAS_RADON_DENSE.get())) dense++;
            check(dense > 0, "meltdown gas must convert neighbouring air into radon_dense (1/7 per tick)");
        });
    }

    /**
     * 1.7.10: meltdown-газ при видимом небе качает +5 RAD/тик в чанк
     * (ChunkRadiationManager). Колонка воздуха под стеклом: небо видно,
     * газ заперт в колонне (вверх — стекло, вниз — камень, стены — камень).
     * Базу читаем до установки газа (мир gameTestServer персистентен).
     *
     * <p>Оба gameTestServer-рана используют выделенный чистый gameDir
     * ({@code runGameTest/}) — свежий flat-мир каждый прогон, без следов чужих миров.
     * При этом ВАНИЛЬНЫЕ арены различаются высотой flat-мира: 1.21.1 — поверхность
     * на y≈-61 (тесты под открытым небом, проверяем накачку), 1.20.1 — поверхность
     * на y≈63 (тесты на y=-60 уходят под землю, skylight=0 — проверяем сам гейт
     * canSeeSky: радиация не качается). Тест адаптивен: ветка выбирается по
     * фактическому {@code canSeeSky} на арене, код один и тот же на обеих версиях.
     */
    @GameTest(template = "empty5x5x5", batch = "gas", timeoutTicks = 200)
    public static void gas_meltdownPumpsChunkRadiationUnderSky(GameTestHelper helper) {
        fillBox(helper, 0, 0, 0, 4, 4, 4, Blocks.STONE);
        // Колонка: воздух y=1..3, стекло y=4 (небо видно, свет проходит)
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 4, 2), Blocks.GLASS);
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        Level level = helper.getLevel();
        float baseline = ChunkRadiationManager.getRadiation(level, abs.getX(), abs.getY(), abs.getZ());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.GAS_MELTDOWN.get());
        // Система чанк-радиации имеет собственный распад (×0.99 − 0.05 за цикл):
        // при непрерывной накачке +5/тик значение выходит на равновесие ~30 RAD.
        helper.startSequence().thenExecuteAfter(160, () -> {
            float rad = ChunkRadiationManager.getRadiation(level, abs.getX(), abs.getY(), abs.getZ());
            if (level.canSeeSky(abs)) {
                // Арена под открытым небом — газ качает +5 RAD/тик в чанк.
                check(rad > baseline + 10f,
                        "meltdown under open sky must pump chunk radiation (+5/tick), got " + rad
                                + " (baseline " + baseline + ")");
            } else {
                // Подземная арена — гейт canSeeSky корректно держит радиацию на нуле.
                check(rad < baseline + 10f,
                        "underground arena: the canSeeSky gate must keep chunk radiation flat, got " + rad
                                + " (baseline " + baseline + ")");
            }
        }).thenSucceed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 9: износ фильтра.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "gas", timeoutTicks = 100)
    public static void gas_filterExhaustsAndDetaches(GameTestHelper helper) {
        ItemStack mask = new ItemStack(ModItems.GAS_MASK.get());
        IGasMask.installFilter(mask, ModItems.GAS_MASK_FILTER.get());
        check(IGasMask.hasFilter(mask), "filter must be installed");
        IGasMask.damageFilter(mask, ItemGasMaskFilter.DEFAULT_MAX_DAMAGE);
        check(IGasMask.hasFilter(mask) && IGasMask.getFilterDamage(mask) == ItemGasMaskFilter.DEFAULT_MAX_DAMAGE,
                "filter at exactly max damage must still be attached");
        IGasMask.damageFilter(mask, 1);
        check(!IGasMask.hasFilter(mask), "exhausted filter must detach from the mask");
        helper.succeed();
    }
}
