package com.hbm_m.test;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityFalloutRain;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.explosion.NuclearExplosionAPI;
import com.hbm_m.explosion.NuclearExplosionConfig;
import com.hbm_m.explosion.command.ExplosionCommandOptions;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Автоматизированный headless-профильный тест ядерного взрыва MK5 на РЕАЛЬНОМ
 * ванильном террейне (неймспейс {@code tf_integration} — тяжёлые интеграционные
 * тесты, только они запускаются конфигом runGameTestServer).
 *
 * <p>Арена гейт-фреймворка ставится ваниллой на фиксированном y=-59 (в обычном
 * мире — под землёй), поэтому детонация и маркеры кратера выносятся на реальную
 * поверхность по heightmap: кратер режет настоящий рельеф. Мир пересоздаётся при
 * каждом запуске (build-скрипт чистит runGameTest/world перед launch).
 *
 * <p>Сценарий: детонация (эквивалент /hbm_m explosion через
 * {@link NuclearExplosionAPI}) → spark profiler start от консоли сервера (spark
 * уже в runtime-classpath гейм-тестов) → монитор на СЕРВЕРНОМ тике (не в тиковой
 * карте гейт-фреймворка: GameTestServer гонит тики без пауз, ~2000+ tps,
 * абсолютные runAtTickTime-метки в прошлом не срабатывают), виталки
 * [HBM NUKE-PROFILE] раз в секунду → после полного завершения (движок +
 * фоллаут-дождь ушли) spark profiler stop (ссылка на профиль падает в лог),
 * проверка, что кратер реально образовался (регрессия на но-оп publish), и
 * завершение теста. Реальный тайм-бюджет 30 минут.
 *
 * <p>Сила по умолчанию 5000 (абсурдная: ~76k чанков кратера — больше инцидента
 * OOM 0927 при 3764); оверрайд {@code -Dhbm.nukeProfileStrength=<N>}.
 *
 * <p>Внимание: все позиции — ЛОКАЛЬНЫЕ координаты арены,
 * {@link GameTestHelper#absolutePos} ровно один раз на границе (см.
 * TurbofanIntegrationGameTest). Время теста ограничено: falloutRangePercent
 * прижат до 20%, иначе фоллаут обрабатывает тысячи чанков часами.
 */
@GameTestHolder("tf_integration")
@PrefixGameTestTemplate(false)
public class NukeProfileGameTest {

    private static final int FALLOUT_RANGE_PERCENT = 20;
    /** Реальный бюджет всего цикла; оверрайд -Dhbm.nukeProfileBudgetMinutes (дефолт 60). */
    private static final long REAL_TIME_BUDGET_MS =
            60_000L * Integer.getInteger("hbm.nukeProfileBudgetMinutes", 60);
    /** Окно между spark profiler stop и заключением теста: загрузка профиля на viewer асинхронная. */
    private static final long SPARK_UPLOAD_GRACE_MS = 5_000;
    /**
     * Бюджет фазы settle: после ухода взрыва и фоллаута чанки кратера обязаны
     * выгрузиться (тикеты сняты -> сейв -> unload). Застревание в unload queue —
     * утечка генерационных ссылок (ванильный баг 1.21.x), чанки навсегда остаются
     * в куче и душат GC вплоть до OOM. Порог: baseline + max(256, peak/25),
     * где baseline снят ДО детонации.
     */
    private static final long UNLOAD_SETTLE_BUDGET_MS = 600_000;

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    // ── Активный монитор (монитор живёт на серверном тике, не в гейт-фреймворке) ──

    private static volatile Monitor active;

    private static final class Monitor {
        final GameTestHelper helper;
        final ServerLevel level;
        final EntityNukeExplosionMK5 nuke;
        final BlockPos[] markers;
        /** Счётчик чанков ДО детонации — передаётся снаружи: конструктор монитора уже после start(). */
        final int baselineChunks;
        final long startMs = System.currentTimeMillis();
        int peakChunks;
        long settleStartMs;
        boolean settled;
        int finalChunks;
        long lastLogMs = System.currentTimeMillis();
        double msptSum;
        double msptMax;
        int samples;
        boolean profilerStopped;
        long graceUntil;

        Monitor(GameTestHelper helper, ServerLevel level, EntityNukeExplosionMK5 nuke,
                BlockPos[] markers, int baselineChunks) {
            this.helper = helper;
            this.level = level;
            this.nuke = nuke;
            this.markers = markers;
            this.baselineChunks = baselineChunks;
            this.peakChunks = baselineChunks;
        }
    }

    /** Тик из MainRegistry SERVER_POST: виталки и завершение теста. */
    public static void serverTick(MinecraftServer server) {
        Monitor m = active;
        if (m == null) return;
        long now = System.currentTimeMillis();
        boolean nukeAlive = m.nuke != null && !m.nuke.isRemoved();
        int fallout = countFallout(m.level);
        int chunks = m.level.getChunkSource().getLoadedChunksCount();
        if (chunks > m.peakChunks) m.peakChunks = chunks;

        if (now - m.lastLogMs >= 1000) {
            m.msptSum += PlatformHooks.averageTickMs(m.level.getServer());
            m.msptMax = Math.max(m.msptMax, PlatformHooks.averageTickMs(m.level.getServer()));
            m.samples++;
            MainRegistry.LOGGER.info(
                    "[HBM NUKE-PROFILE] t={}ms mspt={} chunks={} peak={} entities={} mem={}/{}MB nukeAlive={} fallout={}",
                    now - m.startMs,
                    (long) PlatformHooks.averageTickMs(server),
                    chunks,
                    m.peakChunks,
                    countEntities(m.level),
                    (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20,
                    Runtime.getRuntime().maxMemory() >> 20,
                    nukeAlive,
                    fallout);
            m.lastLogMs = now;
        }

        boolean settled = false;
        boolean settleBudgetOut = false;
        if (!nukeAlive && fallout == 0) {
            if (m.settleStartMs == 0) m.settleStartMs = now;
            int threshold = m.baselineChunks + Math.max(256, m.peakChunks / 25);
            m.finalChunks = chunks;
            settled = chunks <= threshold;
            settleBudgetOut = now - m.settleStartMs > UNLOAD_SETTLE_BUDGET_MS;
            m.settled = settled;
        }

        boolean cycleDone = !nukeAlive && fallout == 0 && (settled || settleBudgetOut);
        boolean budgetExceeded = now - m.startMs > REAL_TIME_BUDGET_MS;
        if (!cycleDone && !budgetExceeded) return;

        if (!m.profilerStopped) {
            // Фаза 1: остановить профайлер и дать ему время выгрузить профиль на viewer —
            // сервер умирает сразу после заключения теста, ссылка не успевает уйти в лог.
            stopSpark(m.level.getServer());
            m.profilerStopped = true;
            m.graceUntil = now + SPARK_UPLOAD_GRACE_MS;
            return;
        }
        if (now < m.graceUntil) return;

        active = null;
        if (cycleDone) {
            finish(m);
        } else {
            try {
                m.helper.fail("explosion cycle did not complete within budget (nukeAlive="
                        + nukeAlive + ", fallout=" + fallout + ") — engine wedged or too slow");
            } catch (RuntimeException ignored) {
                // fail() бросает GameTestAssertException как control flow — глотаем,
                // чтобы не рвать цепочку SERVER_POST-листенеров
            }
        }
    }

    /** Сброс (сервер остановился). */
    public static void reset() {
        active = null;
    }

    @GameTest(template = "empty15x7x15", batch = "nuke_profile", timeoutTicks = 20_000_000)
    public static void craterFormsAndPerfLogged(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(7, 0, 7));
        int strength = Integer.getInteger("hbm.nukeProfileStrength", 5000);

        // Детонация на РЕАЛЬНОЙ поверхности: арена ваниллой уходит под землю (y=-59),
        // кратер должен резать настоящий рельеф, а не подземный камень.
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, anchor.getX(), anchor.getZ());
        BlockPos center = new BlockPos(anchor.getX(), surfaceY, anchor.getZ());

        // Регрессионные маркеры кратера: камень на поверхности вокруг эпицентра —
        // после взрыва обязаны стать воздухом.
        BlockPos[] markers = {
                center,
                center.offset(0, 0, -3),
                center.offset(-3, 0, 0),
                center.offset(3, 0, 0)
        };
        for (BlockPos marker : markers) {
            level.setBlock(marker, Blocks.STONE.defaultBlockState(), 3);
        }

        // Ограничение длительности: масштаб фоллаута = length*2.5*percent/100.
        ModClothConfig.get().falloutRangePercent = FALLOUT_RANGE_PERCENT;

        NuclearExplosionConfig cfg = NuclearExplosionConfig.builder(25)
                .fallout(true)
                .radiation(true)
                .mushroomType(0)
                .build();
        // start(): финальная сила = round(25 * amplifier) * 2 → amplifier = strength/50
        ExplosionCommandOptions opt = new ExplosionCommandOptions(
                true, true, true, true, strength / 50.0f, true, true);

        MainRegistry.LOGGER.info(
                "[HBM NUKE-PROFILE] detonating strength={} at ({},{},{}) surfaceY={} falloutRange={}% realBudget=configurable",
                strength, center.getX(), center.getY(), center.getZ(), FALLOUT_RANGE_PERCENT);
        startSpark(level.getServer());
        // ДО детонации: конструктор монитора выполняется уже после start(), когда
        // движок мог успеть начать загрузку чанков кратера
        int baselineChunks = level.getChunkSource().getLoadedChunksCount();
        EntityNukeExplosionMK5 nuke = NuclearExplosionAPI.start(
                level, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5, cfg, opt);
        check(nuke != null, "explosion entity must spawn");

        active = new Monitor(helper, level, nuke, markers, baselineChunks);
        // Тест завершит монитор через helper.succeed()/fail() на серверном тике.
    }

    /** Spark уже в runtime-classpath гейм-тестов — дёргаем его консольной командой. */
    private static void startSpark(MinecraftServer server) {
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withPermission(4), "spark profiler start");
        MainRegistry.LOGGER.info("[HBM NUKE-PROFILE] spark profiler start dispatched (viewer link appears on stop)");
    }

    private static void stopSpark(MinecraftServer server) {
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withPermission(4), "spark profiler stop");
        MainRegistry.LOGGER.info("[HBM NUKE-PROFILE] spark profiler stop dispatched — grab the viewer link above");
    }

    private static void finish(Monitor m) {
        long total = System.currentTimeMillis() - m.startMs;

        for (BlockPos marker : m.markers) {
            if (!m.level.getBlockState(marker).isAir()) {
                m.helper.fail("crater did not form: marker at " + marker.toShortString() + " is not air"
                        + " — carve/publish pipeline is broken (SectionSnapshot.changed regression?)");
                return;
            }
        }

        if (!m.settled) {
            m.helper.fail("crater chunks did not unload after explosion: peak=" + m.peakChunks
                    + " final=" + m.finalChunks + " baseline=" + m.baselineChunks
                    + " — leaked generation refs keep chunks in the unload queue forever"
                    + " (memory leak / OOM regression)");
            return;
        }

        MainRegistry.LOGGER.info(
                "[HBM NUKE-PROFILE] FINISHED in {} ms: mspt avg={} max={} over {} samples — crater verified (all markers air), chunks unloaded peak={} final={} baseline={}",
                total,
                m.samples == 0 ? -1 : String.format("%.1f", m.msptSum / m.samples),
                String.format("%.1f", m.msptMax),
                m.samples,
                m.peakChunks,
                m.finalChunks,
                m.baselineChunks);
        m.helper.succeed();
    }

    private static int countFallout(ServerLevel level) {
        List<? extends EntityFalloutRain> list =
                level.getEntities(ModEntities.NUKE_FALLOUT_RAIN.get(), e -> true);
        return list.size();
    }

    private static int countEntities(ServerLevel level) {
        int n = 0;
        for (Entity ignored : level.getAllEntities()) n++;
        return n;
    }
}
