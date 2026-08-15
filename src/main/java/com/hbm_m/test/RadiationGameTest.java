package com.hbm_m.test;

import com.hbm_m.capability.ChunkRadiation;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.EntityEffectHandler;
import com.hbm_m.handler.HazmatRegistry;
import com.hbm_m.hazard.HazardData;
import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.interfaces.IChunkRadiation;
import com.hbm_m.item.ModItems;
import com.hbm_m.radiation.ChunkRadiationHandlerSimple;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Обширный кроссплатформенный набор GameTest-ов для системы радиации мода.
 *
 * <p>Покрывает все аспекты радиационной механики:
 * <ul>
 *   <li>{@link ChunkRadiation} — capability/attachment хранения ambient-радиации чанка (clamping, copyFrom).</li>
 *   <li>{@link ChunkRadiationManager} — статические обёртки (get/set/incrementRad), config-gating.</li>
 *   <li>{@link ChunkRadiationHandlerSimple} — get/set/increment/decrement, receiveChunkLoad, clearSystem.</li>
 *   <li>{@link PlayerHandler} — getPlayerRads/setPlayerRads, округление, clamp, null-safety.</li>
 *   <li>{@link HbmLivingProps} — get/set/increment radiation, radEnv/radBuf, cap 2500, Player-delegation.</li>
 *   <li>{@link ContaminationUtil} — contaminate, calculateRadiationMod, isRadImmune, enums.</li>
 *   <li>{@link HazmatRegistry} — getResistance(ItemStack/Player), initDefault.</li>
 *   <li>{@link HazardSystem} — getHazardLevelFromStack/State, getHazardsFromStack, cache, sellafite.</li>
 *   <li>{@link HazardRegistry} — RADIATION singleton identity.</li>
 *   <li>{@link HazardData}/{@link HazardEntry} — override, mutex, baseLevel, addMod.</li>
 *   <li>{@link EntityEffectHandler} — radBuf snap, kill-порог 1000 RAD.</li>
 *   <li>{@link com.hbm_m.hazard.type.HazardTypeRadiation} — rad = level / 20F.</li>
 * </ul>
 *
 * <p>Тесты используют собственные пустые шаблоны мода {@code hbm_m:empty3x3x3} и
 * {@code hbm_m:empty5x5x5} (генерируются {@code scripts/gen_gametest_structures.py}
 * в обе папки — {@code data/hbm_m/structures/} и {@code data/hbm_m/structure/}).
 * Аннотации FQN-gated (Forge/NeoForge различаются):
 * {@code @GameTestHolder("hbm_m")} — авто-регистрация + {@code templateNamespace = "hbm_m"};
 * {@code @PrefixGameTestTemplate(false)} — отключает авто-префикс имени класса.
 *
 * <p>Запуск: {@code ./gradlew :1.21.1-neoforge:runGameTestServer} (и аналогично для forge).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class RadiationGameTest {

    private RadiationGameTest() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Вспомогательные assert-методы.
    // ════════════════════════════════════════════════════════════════════════

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void checkEq(float expected, float actual, String msg) {
        if (Math.abs(expected - actual) > 1e-4f) {
            throw new GameTestAssertException(msg + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    /**
     * Версионно-совместимое создание mock-игрока.
     * 1.20.1 (forge): {@code GameTestHelper.makeMockPlayer()} → Player.
     * 1.21.1 (neoforge): {@code GameTestHelper.makeMockPlayer(GameType)} → Player.
     */
    private static Player makePlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }

    /**
     * Создаёт Cow и размещает её в мировых координатах, заданных helper.absolutePos.
     * ENTITY-тесты должны размещать сущность ВНУТРИ структуры (через absolutePos),
     * а не в фиксированных мировых (1,1,1) — иначе чанковая радиация от предыдущих
     * тестов (manager_setGetRadiation) может утечь через handleRadiationFromChunk.
     */
    private static Cow makeCowAt(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        Cow cow = EntityType.COW.create(helper.getLevel());
        check(cow != null, "Cow entity must be creatable");
        cow.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return cow;
    }

    private static Pig makePigAt(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        Pig pig = EntityType.PIG.create(helper.getLevel());
        check(pig != null, "Pig entity must be creatable");
        pig.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return pig;
    }

    /** Создаёт LivingEntity (Cow) без добавления в мир — для persistentData/contaminate тестов. */
    private static Cow makeCow(Level level) {
        Cow cow = EntityType.COW.create(level);
        check(cow != null, "Cow entity must be creatable");
        cow.moveTo(1.0, 1.0, 1.0);
        return cow;
    }

    /** Создаёт Pig для тестов EntityEffectHandler (kill-порог). */
    private static Pig makePig(Level level) {
        Pig pig = EntityType.PIG.create(level);
        check(pig != null, "Pig entity must be creatable");
        pig.moveTo(1.0, 1.0, 1.0);
        return pig;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 1: ChunkRadiation (IChunkRadiation impl) — clamping, copyFrom, default.
    //  MAX_RAD = ModClothConfig.get().maxRad = 100_000F (по умолчанию).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void chunkRadiation_defaultZero(GameTestHelper helper) {
        IChunkRadiation cap = new ChunkRadiation();
        checkEq(0.0f, cap.getAmbientRadiation(), "ambient radiация по умолчанию должна быть 0");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void chunkRadiation_setGet(GameTestHelper helper) {
        IChunkRadiation cap = new ChunkRadiation();
        cap.setAmbientRadiation(42.5f);
        checkEq(42.5f, cap.getAmbientRadiation(), "set→get round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void chunkRadiation_clampHigh(GameTestHelper helper) {
        IChunkRadiation cap = new ChunkRadiation();
        float maxRad = ModClothConfig.get().maxRad;
        cap.setAmbientRadiation(maxRad + 50_000f);
        checkEq(maxRad, cap.getAmbientRadiation(), "ambient clamp к maxRad");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void chunkRadiation_clampNegative(GameTestHelper helper) {
        IChunkRadiation cap = new ChunkRadiation();
        cap.setAmbientRadiation(-100f);
        checkEq(0.0f, cap.getAmbientRadiation(), "отрицательная радиация clamp к 0");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void chunkRadiation_copyFrom(GameTestHelper helper) {
        IChunkRadiation source = new ChunkRadiation();
        source.setAmbientRadiation(77.7f);
        IChunkRadiation dest = new ChunkRadiation();
        dest.copyFrom(source);
        checkEq(77.7f, dest.getAmbientRadiation(), "copyFrom копирует ambient");
        checkEq(source.getAmbientRadiation(), dest.getAmbientRadiation(),
                "copyFrom: source == dest");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void chunkRadiation_zeroIsZero(GameTestHelper helper) {
        IChunkRadiation cap = new ChunkRadiation();
        cap.setAmbientRadiation(0f);
        checkEq(0.0f, cap.getAmbientRadiation(), "явный 0 остаётся 0");
        cap.setAmbientRadiation(10f);
        cap.setAmbientRadiation(0f);
        checkEq(0.0f, cap.getAmbientRadiation(), "сброс в 0 после значения");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 2: ChunkRadiationManager — статические обёртки, config-gating.
    //  Делегируют к ChunkRadiationHandlerSimple через getProxy().
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void manager_setGetRadiation(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        Level level = helper.getLevel();
        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 50f);
        float rad = ChunkRadiationManager.getRadiation(level, abs.getX(), abs.getY(), abs.getZ());
        checkEq(50f, rad, "setRadiation→getRadiation round-trip через Manager");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void manager_incrementRad(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        Level level = helper.getLevel();
        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 100f);
        ChunkRadiationManager.incrementRad(level, abs.getX(), abs.getY(), abs.getZ(), 50f);
        float rad = ChunkRadiationManager.getRadiation(level, abs.getX(), abs.getY(), abs.getZ());
        checkEq(150f, rad, "incrementRad добавляет к существующей радиации");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void manager_defaultZero(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        Level level = helper.getLevel();
        float rad = ChunkRadiationManager.getRadiation(level, abs.getX(), abs.getY(), abs.getZ());
        checkEq(0f, rad, "нетронутый чанк = 0 радиации");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void manager_getProxySingleton(GameTestHelper helper) {
        var proxy1 = ChunkRadiationManager.getProxy();
        var proxy2 = ChunkRadiationManager.getProxy();
        check(proxy1 == proxy2, "getProxy должен возвращать singleton");
        check(proxy1 instanceof ChunkRadiationHandlerSimple,
                "getProxy по умолчанию = ChunkRadiationHandlerSimple");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 3: ChunkRadiationHandlerSimple — get/set/increment/decrement, clearSystem.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void handlerSimple_setGetRadiation(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        Level level = helper.getLevel();
        ChunkRadiationHandlerSimple proxy = (ChunkRadiationHandlerSimple) ChunkRadiationManager.getProxy();
        proxy.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 75f);
        float rad = proxy.getRadiation(level, abs.getX(), abs.getY(), abs.getZ());
        checkEq(75f, rad, "handler set→get");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void handlerSimple_incrementDecrement(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        Level level = helper.getLevel();
        ChunkRadiationHandlerSimple proxy = (ChunkRadiationHandlerSimple) ChunkRadiationManager.getProxy();
        proxy.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 100f);
        proxy.incrementRad(level, abs.getX(), abs.getY(), abs.getZ(), 50f);
        checkEq(150f, proxy.getRadiation(level, abs.getX(), abs.getY(), abs.getZ()),
                "increment добавляет");
        proxy.decrementRad(level, abs.getX(), abs.getY(), abs.getZ(), 30f);
        checkEq(120f, proxy.getRadiation(level, abs.getX(), abs.getY(), abs.getZ()),
                "decrement вычитает");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void handlerSimple_decrementBelowZero(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        Level level = helper.getLevel();
        ChunkRadiationHandlerSimple proxy = (ChunkRadiationHandlerSimple) ChunkRadiationManager.getProxy();
        proxy.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 10f);
        proxy.decrementRad(level, abs.getX(), abs.getY(), abs.getZ(), 100f);
        checkEq(0f, proxy.getRadiation(level, abs.getX(), abs.getY(), abs.getZ()),
                "decrement ниже 0 clamp к 0 (Math.max(0, ...))");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void handlerSimple_clearSystem(GameTestHelper helper) {
        Level level = helper.getLevel();
        ChunkRadiationHandlerSimple proxy = (ChunkRadiationHandlerSimple) ChunkRadiationManager.getProxy();
        proxy.clearSystem(level);
        // После clearSystem getRadiation должен вернуть 0 (активные чанки очищены).
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        proxy.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 50f);
        checkEq(50f, proxy.getRadiation(level, abs.getX(), abs.getY(), abs.getZ()),
                "после clearSystem set→get работает");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void handlerSimple_getRadiationNullLevel(GameTestHelper helper) {
        ChunkRadiationHandlerSimple proxy = (ChunkRadiationHandlerSimple) ChunkRadiationManager.getProxy();
        checkEq(0f, proxy.getRadiation(null, 0, 0, 0),
                "getRadiation(null, ...) → 0 без краша");
        proxy.setRadiation(null, 0, 0, 0, 50f);
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 4: PlayerHandler — get/set/increment/decrement, rounding, null-safety.
    //  setPlayerRads: Math.round(Math.max(0, rads) * 10.0f) / 10.0f (округление до 1 знака).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_setGetRads(GameTestHelper helper) {
        Player player = makePlayer(helper);
        PlayerHandler.setPlayerRads(player, 100f);
        checkEq(100f, PlayerHandler.getPlayerRads(player), "set→get round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_rounding(GameTestHelper helper) {
        Player player = makePlayer(helper);
        PlayerHandler.setPlayerRads(player, 123.456f);
        // Math.round(123.456 * 10) / 10 = Math.round(1234.56) / 10 = 1235 / 10 = 123.5
        checkEq(123.5f, PlayerHandler.getPlayerRads(player),
                "setPlayerRads округляет до 1 знака (×10/10)");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_negativeClamps(GameTestHelper helper) {
        Player player = makePlayer(helper);
        PlayerHandler.setPlayerRads(player, -50f);
        checkEq(0f, PlayerHandler.getPlayerRads(player),
                "отрицательная радиация clamp к 0 (Math.max(0, ...))");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_incrementDecrement(GameTestHelper helper) {
        Player player = makePlayer(helper);
        PlayerHandler.setPlayerRads(player, 100f);
        PlayerHandler.incrementPlayerRads(player, 50f);
        checkEq(150f, PlayerHandler.getPlayerRads(player), "increment");
        PlayerHandler.decrementPlayerRads(player, 30f);
        checkEq(120f, PlayerHandler.getPlayerRads(player), "decrement");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_decrementBelowZero(GameTestHelper helper) {
        Player player = makePlayer(helper);
        PlayerHandler.setPlayerRads(player, 10f);
        PlayerHandler.decrementPlayerRads(player, 100f);
        checkEq(0f, PlayerHandler.getPlayerRads(player),
                "decrement ниже 0 clamp к 0");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_incrementZeroNoOp(GameTestHelper helper) {
        Player player = makePlayer(helper);
        PlayerHandler.setPlayerRads(player, 50f);
        PlayerHandler.incrementPlayerRads(player, 0f);
        checkEq(50f, PlayerHandler.getPlayerRads(player),
                "increment на 0 — no-op (rads <= 0 return)");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_nullSafety(GameTestHelper helper) {
        checkEq(0f, PlayerHandler.getPlayerRads(null), "getPlayerRads(null) → 0");
        PlayerHandler.setPlayerRads(null, 100f);     // не должно крашить
        PlayerHandler.incrementPlayerRads(null, 10f);
        PlayerHandler.decrementPlayerRads(null, 10f);
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_defaultZero(GameTestHelper helper) {
        Player player = makePlayer(helper);
        checkEq(0f, PlayerHandler.getPlayerRads(player),
                "новый игрок: 0 радиации по умолчанию");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 5: HbmLivingProps (non-Player) — persistentData на сущностях.
    //  incrementRadiation cap = 2500F. setRadiation = Math.max(0, rad).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_setGetRadiation(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        HbmLivingProps.setRadiation(cow, 50f);
        checkEq(50f, HbmLivingProps.getRadiation(cow), "set→get radiation на Cow");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_incrementRadiation(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        HbmLivingProps.setRadiation(cow, 100f);
        HbmLivingProps.incrementRadiation(cow, 50f);
        checkEq(150f, HbmLivingProps.getRadiation(cow), "increment на Cow");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_cap2500(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        HbmLivingProps.setRadiation(cow, 2400f);
        HbmLivingProps.incrementRadiation(cow, 200f);
        // 2400 + 200 = 2600 → cap 2500
        checkEq(2500f, HbmLivingProps.getRadiation(cow),
                "incrementRadiation cap 2500F");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_negativeClamps(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        HbmLivingProps.setRadiation(cow, -50f);
        checkEq(0f, HbmLivingProps.getRadiation(cow),
                "setRadiation negative → Math.max(0, rad)");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_radEnvRadBuf(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        HbmLivingProps.setRadEnv(cow, 5.5f);
        HbmLivingProps.setRadBuf(cow, 7.7f);
        checkEq(5.5f, HbmLivingProps.getRadEnv(cow), "radEnv set→get");
        checkEq(7.7f, HbmLivingProps.getRadBuf(cow), "radBuf set→get");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_incrementZeroNoOp(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        HbmLivingProps.setRadiation(cow, 50f);
        HbmLivingProps.incrementRadiation(cow, 0f);
        checkEq(50f, HbmLivingProps.getRadiation(cow),
                "increment на 0 — no-op (rad == 0F return)");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_asbestosBlackLungDigamma(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        check(0 == HbmLivingProps.getAsbestos(cow), "asbestos по умолчанию 0");
        HbmLivingProps.incrementAsbestos(cow, 5);
        check(5 == HbmLivingProps.getAsbestos(cow), "asbestos increment");
        check(0 == HbmLivingProps.getBlackLung(cow), "blackLung по умолчанию 0");
        HbmLivingProps.incrementBlackLung(cow, 3);
        check(3 == HbmLivingProps.getBlackLung(cow), "blackLung increment");
        check(0f == HbmLivingProps.getDigamma(cow), "digamma по умолчанию 0");
        HbmLivingProps.incrementDigamma(cow, 2.5f);
        checkEq(2.5f, HbmLivingProps.getDigamma(cow), "digamma increment");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 6: HbmLivingProps (Player delegation) — делегирует к PlayerHandler.
    //  Для Player getRadiation/setRadiation вызывает PlayerHandler, а не persistentData.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_playerDelegation(GameTestHelper helper) {
        Player player = makePlayer(helper);
        HbmLivingProps.setRadiation(player, 80f);
        // Для Player getRadiation делегирует к PlayerHandler.getPlayerRads.
        checkEq(80f, HbmLivingProps.getRadiation(player),
                "HbmLivingProps делегирует к PlayerHandler для Player");
        checkEq(PlayerHandler.getPlayerRads(player), HbmLivingProps.getRadiation(player),
                "HbmLivingProps == PlayerHandler для Player");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_playerIncrement(GameTestHelper helper) {
        Player player = makePlayer(helper);
        HbmLivingProps.setRadiation(player, 100f);
        HbmLivingProps.incrementRadiation(player, 50f);
        checkEq(150f, HbmLivingProps.getRadiation(player),
                "incrementRadiation делегирует к Player для Player");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void livingProps_playerKey(GameTestHelper helper) {
        check("NTM_EXT_LIVING".equals(HbmLivingProps.KEY),
                "NBT ключ сущностей = NTM_EXT_LIVING (1.7.10 parity)");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 7: ContaminationUtil — contaminate, calculateRadiationMod, isRadImmune.
    //  contaminate: radEnv += amount, затем incrementRadiation(amount × radMod).
    //  Для non-Player radMod = 1.0. Для Player radMod = 10^(-hazmatResistance).
    //  Player с tickCount < 200 → contaminate возвращает false (иммунитет при спавне).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void contaminate_cowRadiation(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        boolean result = ContaminationUtil.contaminate(cow,
                HazardType.RADIATION, ContaminationType.CREATIVE, 10f);
        check(result, "contaminate Cow RADIATION CREATIVE → true (не иммун)");
        checkEq(10f, HbmLivingProps.getRadiation(cow),
                "contaminate increment radiation на Cow (radMod=1.0)");
        checkEq(10f, HbmLivingProps.getRadEnv(cow),
                "contaminate накапливает radEnv");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void contaminate_radEnvAccumulates(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        ContaminationUtil.contaminate(cow, HazardType.RADIATION, ContaminationType.CREATIVE, 5f);
        ContaminationUtil.contaminate(cow, HazardType.RADIATION, ContaminationType.CREATIVE, 3f);
        checkEq(8f, HbmLivingProps.getRadEnv(cow),
                "radEnv накапливается (5 + 3 = 8)");
        checkEq(8f, HbmLivingProps.getRadiation(cow),
                "radiation накапливается (5 + 3 = 8, radMod=1.0)");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void contaminate_playerTickCountGate(GameTestHelper helper) {
        Player player = makePlayer(helper);
        // Свежесозданный игрок: tickCount < 200 → contaminate возвращает false.
        check(player.tickCount < 200, "mock player tickCount < 200");
        boolean result = ContaminationUtil.contaminate(player,
                HazardType.RADIATION, ContaminationType.CREATIVE, 10f);
        check(!result, "contaminate Player с tickCount < 200 → false (spawn immunity)");
        checkEq(0f, PlayerHandler.getPlayerRads(player),
                "радиация не изменена при tickCount < 200");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void contaminate_radImmuneCow(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        // Cow НЕ иммунна (только MushroomCow иммунна).
        check(!ContaminationUtil.isRadImmune(cow), "Cow не иммунна к радиации");
        boolean result = ContaminationUtil.contaminate(cow,
                HazardType.RADIATION, ContaminationType.CREATIVE, 10f);
        check(result, "contaminate Cow → true");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void contaminate_radImmuneZombie(GameTestHelper helper) {
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        check(zombie != null, "Zombie must be creatable");
        zombie.moveTo(1.0, 1.0, 1.0);
        check(ContaminationUtil.isRadImmune(zombie), "Zombie иммуннен к радиации");
        boolean result = ContaminationUtil.contaminate(zombie,
                HazardType.RADIATION, ContaminationType.CREATIVE, 10f);
        check(!result, "contaminate Zombie RADIATION → false (иммун)");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void calcRadMod_noArmor(GameTestHelper helper) {
        Player player = makePlayer(helper);
        // Без брони: HazmatRegistry.getResistance = 0 → 10^(-0) = 10^0 = 1.0.
        float mod = ContaminationUtil.calculateRadiationMod(player);
        checkEq(1.0f, mod, "calculateRadiationMod без брони = 1.0 (нет защиты)");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void calcRadMod_nonPlayer(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        // Non-Player → всегда 1.0.
        float mod = ContaminationUtil.calculateRadiationMod(cow);
        checkEq(1.0f, mod, "calculateRadiationMod non-Player = 1.0");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void isRadImmune_nonLiving(GameTestHelper helper) {
        // Non-LivingEntity → false.
        check(!ContaminationUtil.isRadImmune(null),
                "isRadImmune(null) → false (не LivingEntity)");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazardType_enumValues(GameTestHelper helper) {
        check(HazardType.RADIATION != HazardType.DIGAMMA,
                "HazardType.RADIATION ≠ DIGAMMA");
        check(ContaminationType.CREATIVE != ContaminationType.HAZMAT,
                "ContaminationType.CREATIVE ≠ HAZMAT");
        check(ContaminationType.NONE != ContaminationType.RAD_BYPASS,
                "ContaminationType.NONE ≠ RAD_BYPASS");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 8: HazmatRegistry — getResistance(ItemStack), getResistance(Player).
    //  Коэффициенты: HELMET=0.2, CHEST=0.4, LEGS=0.3, BOOTS=0.1.
    //  HAZMAT set: hazYellow=0.6 → helmet=0.12, chest=0.24, legs=0.18, boots=0.06.
    //  Iron: 0.0225 → helmet=0.0045. Diamond helmet=0.05.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazmat_emptyStack(GameTestHelper helper) {
        check(0.0 == HazmatRegistry.getResistance(ItemStack.EMPTY),
                "getResistance(EMPTY) → 0");
        check(0.0 == HazmatRegistry.getResistance((ItemStack) null),
                "getResistance(null) → 0");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazmat_nonArmorItem(GameTestHelper helper) {
        // Предмет без зарегистрированного сопротивления → только cladding (0).
        double res = HazmatRegistry.getResistance(new ItemStack(Items.STICK));
        check(0.0 == res, "getResistance(не броня) → 0 (cladding=0)");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazmat_ironHelmet(GameTestHelper helper) {
        // Iron: 0.0225 × HELMET(0.2) = 0.0045.
        double res = HazmatRegistry.getResistance(new ItemStack(Items.IRON_HELMET));
        check(Math.abs(res - 0.0045) < 1e-6,
                "iron helmet resistance = 0.0225 × 0.2 = 0.0045 (got " + res + ")");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazmat_diamondChestplate(GameTestHelper helper) {
        // Diamond chestplate: 0.25 (hardcoded).
        double res = HazmatRegistry.getResistance(new ItemStack(Items.DIAMOND_CHESTPLATE));
        check(Math.abs(res - 0.25) < 1e-6,
                "diamond chestplate resistance = 0.25 (got " + res + ")");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazmat_hazmatHelmet(GameTestHelper helper) {
        // HAZMAT helmet: hazYellow(0.6) × HELMET(0.2) = 0.12.
        double res = HazmatRegistry.getResistance(
                new ItemStack(ModItems.HAZMAT_HELMET.get()));
        check(Math.abs(res - 0.12) < 1e-6,
                "hazmat helmet resistance = 0.6 × 0.2 = 0.12 (got " + res + ")");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void hazmat_playerNoArmor(GameTestHelper helper) {
        Player player = makePlayer(helper);
        // Без брони → getResistance(Player) = 0.
        float res = HazmatRegistry.getResistance(player);
        check(Math.abs(res) < 1e-6, "player без брони → resistance 0");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazmat_claddingZero(GameTestHelper helper) {
        // getCladding всегда возвращает 0 в текущей реализации.
        check(0f == HazmatRegistry.getCladding(new ItemStack(Items.IRON_HELMET)),
                "getCladding → 0 (нет реализации cladding)");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 9: HazardSystem — getHazardLevelFromStack, getHazardsFromStack,
    //  getHazardLevelFromState, cache, sellafiteRadiationForLevel.
    //  Регистрация: HazardRegistry.registerItems() вызывается в MainRegistry.setup.
    //  CRYSTAL_URANIUM=3.5f, CRYSTAL_THORIUM=1.0f, POLONIUM210_BLOCK=750f.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_crystalUranium(GameTestHelper helper) {
        float level = HazardSystem.getHazardLevelFromStack(
                new ItemStack(ModItems.CRYSTAL_URANIUM.get()), HazardRegistry.RADIATION);
        checkEq(3.5f, level, "CRYSTAL_URANIUM radiation hazard = 3.5f");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_crystalThorium(GameTestHelper helper) {
        float level = HazardSystem.getHazardLevelFromStack(
                new ItemStack(ModItems.CRYSTAL_THORIUM.get()), HazardRegistry.RADIATION);
        checkEq(1.0f, level, "CRYSTAL_THORIUM radiation hazard = 1.0f");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_nonRadioactiveItem(GameTestHelper helper) {
        // Iron ingot не зарегистрирован с RADIATION hazard.
        float level = HazardSystem.getHazardLevelFromStack(
                new ItemStack(Items.IRON_INGOT), HazardRegistry.RADIATION);
        checkEq(0.0f, level, "IRON_INGOT radiation = 0 (не радиоактивен)");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_emptyStack(GameTestHelper helper) {
        check(HazardSystem.getHazardsFromStack(ItemStack.EMPTY).isEmpty(),
                "getHazardsFromStack(EMPTY) → пустой список");
        check(HazardSystem.getHazardsFromStack(null).isEmpty(),
                "getHazardsFromStack(null) → пустой список");
        checkEq(0.0f, HazardSystem.getHazardLevelFromStack(null, HazardRegistry.RADIATION),
                "getHazardLevelFromStack(null) → 0");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_cacheConsistency(GameTestHelper helper) {
        // Кэш должен возвращать один и тот же список для того же Item.
        var list1 = HazardSystem.getHazardsFromStack(
                new ItemStack(ModItems.CRYSTAL_URANIUM.get()));
        var list2 = HazardSystem.getHazardsFromStack(
                new ItemStack(ModItems.CRYSTAL_URANIUM.get()));
        check(list1 == list2, "HAZARD_CACHE возвращает тот же список (identity)");
        check(!list1.isEmpty(), "CRYSTAL_URANIUM имеет hazards");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_getHazardLevelFromState(GameTestHelper helper) {
        // Iron block → asItem → iron_ingot → не радиоактивен → 0.
        BlockState state = Blocks.IRON_BLOCK.defaultBlockState();
        float level = HazardSystem.getHazardLevelFromState(state, HazardRegistry.RADIATION);
        checkEq(0.0f, level, "IRON_BLOCK state radiation = 0");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_getHazardLevelFromStateAir(GameTestHelper helper) {
        BlockState air = Blocks.AIR.defaultBlockState();
        checkEq(0.0f, HazardSystem.getHazardLevelFromState(air, HazardRegistry.RADIATION),
                "AIR state radiation = 0");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_sellafiteRadiationForLevel(GameTestHelper helper) {
        // sellafiteRadiationForLevel: 0→0.5, 1→1.0, 2→2.5, 3→4.0, 4→5.0, 5→10.0,
        // 6→15.0, 7→20.0, 8→25.0, 9→30.0, 10→35.0.
        checkEq(0.5f, HazardSystem.sellafiteRadiationForLevel(0), "sellafite level 0 = 0.5");
        checkEq(1.0f, HazardSystem.sellafiteRadiationForLevel(1), "sellafite level 1 = 1.0");
        checkEq(2.5f, HazardSystem.sellafiteRadiationForLevel(2), "sellafite level 2 = 2.5");
        checkEq(4.0f, HazardSystem.sellafiteRadiationForLevel(3), "sellafite level 3 = 4.0");
        checkEq(5.0f, HazardSystem.sellafiteRadiationForLevel(4), "sellafite level 4 = 5.0");
        checkEq(10.0f, HazardSystem.sellafiteRadiationForLevel(5), "sellafite level 5 = 10.0");
        checkEq(15.0f, HazardSystem.sellafiteRadiationForLevel(6), "sellafite level 6 = 15.0");
        checkEq(35.0f, HazardSystem.sellafiteRadiationForLevel(10), "sellafite level 10 = 35.0");
        // Clamp: -5 → 0 → 0.5, 100 → 10 → 35.0.
        checkEq(0.5f, HazardSystem.sellafiteRadiationForLevel(-5), "sellafite -5 clamps to 0 = 0.5");
        checkEq(35.0f, HazardSystem.sellafiteRadiationForLevel(100), "sellafite 100 clamps to 10 = 35.0");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_getArmorProtection(GameTestHelper helper) {
        // getArmorProtection делегирует к HazmatRegistry.getResistance.
        float prot = HazardSystem.getArmorProtection(new ItemStack(Items.IRON_HELMET));
        check(Math.abs(prot - 0.0045f) < 1e-5f,
                "getArmorProtection(iron helmet) = 0.0045 (got " + prot + ")");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazard_applyHazardsNoCrash(GameTestHelper helper) {
        // applyHazards на не-радиоактивном предмете — не должно крашить.
        Cow cow = makeCow(helper.getLevel());
        HazardSystem.applyHazards(new ItemStack(Items.STICK), cow);
        HazardSystem.applyHazards(ItemStack.EMPTY, cow);
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 10: HazardRegistry — RADIATION singleton identity.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazardRegistry_radiationSingleton(GameTestHelper helper) {
        check(HazardRegistry.RADIATION != null, "RADIATION singleton не null");
        check(HazardRegistry.RADIATION instanceof com.hbm_m.hazard.type.HazardTypeRadiation,
                "RADIATION — instanceof HazardTypeRadiation");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazardRegistry_allTypesNonNull(GameTestHelper helper) {
        check(HazardRegistry.RADIATION != null, "RADIATION");
        check(HazardRegistry.HOT != null, "HOT");
        check(HazardRegistry.DIGAMMA != null, "DIGAMMA");
        check(HazardRegistry.BLINDING != null, "BLINDING");
        check(HazardRegistry.ASBESTOS != null, "ASBESTOS");
        check(HazardRegistry.COAL != null, "COAL");
        check(HazardRegistry.HYDROACTIVE != null, "HYDROACTIVE");
        check(HazardRegistry.EXPLOSIVE != null, "EXPLOSIVE");
        // Все разные синглтоны.
        check(HazardRegistry.RADIATION != HazardRegistry.HOT, "RADIATION ≠ HOT");
        check(HazardRegistry.RADIATION != HazardRegistry.DIGAMMA, "RADIATION ≠ DIGAMMA");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 11: HazardData / HazardEntry — override, mutex, baseLevel, addMod.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazardEntry_baseLevel(GameTestHelper helper) {
        HazardEntry entry = new HazardEntry(HazardRegistry.RADIATION, 5.0f);
        checkEq(5.0f, entry.baseLevel, "baseLevel = 5.0");
        check(entry.type == HazardRegistry.RADIATION, "type = RADIATION");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazardEntry_defaultLevel(GameTestHelper helper) {
        HazardEntry entry = new HazardEntry(HazardRegistry.RADIATION);
        checkEq(1.0f, entry.baseLevel, "default baseLevel = 1.0F");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazardEntry_addMod(GameTestHelper helper) {
        HazardEntry entry = new HazardEntry(HazardRegistry.RADIATION, 5.0f);
        int before = entry.mods.size();
        HazardEntry returned = entry.addMod(null); // addMod принимает HazardModifier
        check(returned == entry, "addMod возвращает this (builder pattern)");
        check(entry.mods.size() == before + 1, "addMod добавляет modifier в список");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazardData_overrideAndMutex(GameTestHelper helper) {
        HazardData data = new HazardData(
                new HazardEntry(HazardRegistry.RADIATION, 10f));
        check(!data.doesOverride, "doesOverride по умолчанию false");
        check(0 == data.getMutex(), "mutex по умолчанию 0");
        HazardData returned = data.setAsOverride();
        check(returned == data, "setAsOverride возвращает this");
        check(data.doesOverride, "doesOverride true после setAsOverride");
        check(!data.entries.isEmpty(), "entries не пустой");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void hazardEntry_applyHazardNoCrash(GameTestHelper helper) {
        // applyHazard вызывает type.onUpdate — для RADIATION это contaminate.
        // На Cow (не Player, tickCount не проверяется) должно работать.
        Cow cow = makeCow(helper.getLevel());
        HazardEntry entry = new HazardEntry(HazardRegistry.RADIATION, 10f);
        entry.applyHazard(new ItemStack(ModItems.CRYSTAL_URANIUM.get()), cow);
        // onUpdate: level × count / 20F → 3.5 × 1 / 20 = 0.175 → contaminate CREATIVE.
        check(HbmLivingProps.getRadiation(cow) > 0f,
                "applyHazard RADIATION накапливает radiation на Cow");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 12: EntityEffectHandler — radBuf snap (tickCount % 20 == 0),
    //  kill-порог 1000 RAD (Pig, не Cow/Villager/Creeper).
    //  onUpdate: каждые 20 тиков setRadBuf(getRadEnv) + setRadEnv(0).
    //  handleRadiationEffect: eRad >= 1000 → hurt(1000) + setRadiation(0) + die.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void entityEffect_radBufSnap(GameTestHelper helper) {
        Level level = helper.getLevel();
        // Размещаем сущность ВНУТРИ структуры (через absolutePos) и очищаем чанковую
        // радиацию в этой позиции — изоляция от предыдущих manager_setGetRadiation.
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 0f);
        Cow cow = makeCowAt(helper);
        // Свежесозданная сущность: tickCount = 0 → 0 % 20 == 0 → snap происходит.
        check(cow.tickCount == 0, "Cow tickCount == 0 при создании");
        HbmLivingProps.setRadEnv(cow, 5.0f);
        checkEq(5.0f, HbmLivingProps.getRadEnv(cow), "radEnv установлен");
        // onUpdate: setRadBuf(getRadEnv) + setRadEnv(0).
        EntityEffectHandler.onUpdate(cow);
        checkEq(5.0f, HbmLivingProps.getRadBuf(cow),
                "radBuf = старый radEnv после snap");
        checkEq(0.0f, HbmLivingProps.getRadEnv(cow),
                "radEnv сброшен в 0 после snap");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void entityEffect_lowRadNoEffect(GameTestHelper helper) {
        Level level = helper.getLevel();
        // Очищаем чанковую радиацию в позиции структуры (изоляция тестов).
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 0f);
        Cow cow = makeCowAt(helper);
        HbmLivingProps.setRadiation(cow, 50f);
        // eRad = 50 < 200 → handleRadiationEffect early return.
        // Но Cow at eRad >= 50 → Cow→Mooshroom transformation! Используем Pig.
        Pig pig = makePigAt(helper);
        HbmLivingProps.setRadiation(pig, 50f);
        float healthBefore = pig.getHealth();
        EntityEffectHandler.onUpdate(pig);
        check(pig.isAlive(), "Pig при 50 RAD жив (eRad < 200)");
        // handleRadiationEffect при eRad < 200 не меняет радиацию; handleRadiationFromChunk
        // добавляет rad/20F из чанка (очищен → 0). Проверяем точное равенство.
        checkEq(50f, HbmLivingProps.getRadiation(pig),
                "Pig при 50 RAD: радиация не изменена");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void entityEffect_killAt1000(GameTestHelper helper) {
        Level level = helper.getLevel();
        // Очищаем чанковую радиацию в позиции структуры (изоляция тестов).
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        ChunkRadiationManager.setRadiation(level, abs.getX(), abs.getY(), abs.getZ(), 0f);
        Pig pig = makePigAt(helper);
        HbmLivingProps.setRadiation(pig, 1000f);
        checkEq(1000f, HbmLivingProps.getRadiation(pig), "Pig rad = 1000 до onUpdate");
        float healthBefore = pig.getHealth();
        check(healthBefore > 0f, "Pig жив до onUpdate");
        EntityEffectHandler.onUpdate(pig);
        // eRad >= 1000 → hurt(1000) + setRadiation(0) + if health > 0 setHealth(0) + die.
        check(pig.isDeadOrDying() || pig.getHealth() <= 0f,
                "Pig при 1000 RAD мёртв/умирает после onUpdate");
        // После kill setRadiation(0); handleRadiationFromChunk добавляет rad/20F из чанка
        // (очищен → 0). Проверяем точное равенство 0.
        checkEq(0f, HbmLivingProps.getRadiation(pig),
                "Pig радиация сброшена в 0 после kill (1000 RAD)");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void entityEffect_radImmuneNoKill(GameTestHelper helper) {
        // Zombie иммунен — handleRadiationEffect early return (isRadImmune).
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        check(zombie != null, "Zombie must be creatable");
        zombie.moveTo(1.0, 1.0, 1.0);
        HbmLivingProps.setRadiation(zombie, 1000f);
        float healthBefore = zombie.getHealth();
        EntityEffectHandler.onUpdate(zombie);
        check(zombie.isAlive(), "Zombie при 1000 RAD жив (иммун)");
        check(zombie.getHealth() > 0f, "Zombie health > 0 (иммун)");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 13: HazardTypeRadiation — onUpdate: rad = level / 20F.
    //  level *= stack.getCount(). Без reacher: rad = level / 20F.
    //  CRYSTAL_URANIUM: hazard=3.5, 1 штак → onUpdate → 3.5×1/20 = 0.175 RAD.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void hazardTypeRadiation_onUpdateCow(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        // CRYSTAL_URANIUM × 1: hazard=3.5, onUpdate → rad = 3.5×1/20 = 0.175.
        ItemStack stack = new ItemStack(ModItems.CRYSTAL_URANIUM.get(), 1);
        HazardSystem.applyHazards(stack, cow);
        // applyHazards → HazardTypeRadiation.onUpdate → contaminate CREATIVE →
        // radEnv += 0.175, incrementRadiation += 0.175 (radMod=1.0 для Cow).
        check(HbmLivingProps.getRadiation(cow) > 0f,
                "CRYSTAL_URANIUM накачивает radiation на Cow");
        check(HbmLivingProps.getRadEnv(cow) > 0f,
                "CRYSTAL_URANIUM накапливает radEnv");
        // Точное значение: 3.5 / 20 = 0.175.
        checkEq(0.175f, HbmLivingProps.getRadiation(cow),
                "CRYSTAL_URANIUM ×1: rad = 3.5 / 20 = 0.175");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void hazardTypeRadiation_stackCount(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        // CRYSTAL_URANIUM × 4: hazard=3.5, onUpdate → rad = 3.5×4/20 = 0.7.
        ItemStack stack = new ItemStack(ModItems.CRYSTAL_URANIUM.get(), 4);
        HazardSystem.applyHazards(stack, cow);
        checkEq(0.7f, HbmLivingProps.getRadiation(cow),
                "CRYSTAL_URANIUM ×4: rad = 3.5 × 4 / 20 = 0.7");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void hazardTypeRadiation_nonRadioactiveNoEffect(GameTestHelper helper) {
        Cow cow = makeCow(helper.getLevel());
        // STICK не имеет RADIATION hazard → applyHazards не вызывает onUpdate.
        HazardSystem.applyHazards(new ItemStack(Items.STICK), cow);
        checkEq(0f, HbmLivingProps.getRadiation(cow),
                "STICK не накачивает radiation");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 14: PlayerHandler.getInventoryRadiation — сумма радиоактивных
    //  предметов в инвентаре. getRadiationFromItemStack = hazard × count.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_inventoryRadiationEmpty(GameTestHelper helper) {
        Player player = makePlayer(helper);
        checkEq(0f, PlayerHandler.getInventoryRadiation(player),
                "пустой инвентарь → 0 inventory radiation");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_inventoryRadiationItem(GameTestHelper helper) {
        Player player = makePlayer(helper);
        // CRYSTAL_URANIUM × 2: hazard=3.5 × count=2 = 7.0.
        player.getInventory().add(new ItemStack(ModItems.CRYSTAL_URANIUM.get(), 2));
        float invRad = PlayerHandler.getInventoryRadiation(player);
        checkEq(7.0f, invRad,
                "CRYSTAL_URANIUM ×2: inventory radiation = 3.5 × 2 = 7.0");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_inventoryRadiationNonRadioactive(GameTestHelper helper) {
        Player player = makePlayer(helper);
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 64));
        checkEq(0f, PlayerHandler.getInventoryRadiation(player),
                "IRON_INGOT ×64: inventory radiation = 0 (не радиоактивен)");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 15: PlayerHandler.getIncomingEnvironmentRad — radBuf на гейгере.
    //  getIncomingEnvironmentRad = HbmLivingProps.getRadBuf(player).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_incomingEnvRadDefault(GameTestHelper helper) {
        Player player = makePlayer(helper);
        checkEq(0f, PlayerHandler.getIncomingEnvironmentRad(player),
                "incomingEnvRad по умолчанию = 0 (radBuf = 0)");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_incomingEnvRadAfterSet(GameTestHelper helper) {
        Player player = makePlayer(helper);
        HbmLivingProps.setRadBuf(player, 15.0f);
        checkEq(15.0f, PlayerHandler.getIncomingEnvironmentRad(player),
                "incomingEnvRad = radBuf после set");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "radiation", timeoutTicks = 100)
    public static void playerHandler_incomingEnvRadNull(GameTestHelper helper) {
        checkEq(0f, PlayerHandler.getIncomingEnvironmentRad(null),
                "incomingEnvRad(null) → 0");
        helper.succeed();
    }
}
