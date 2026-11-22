package com.hbm_m.test;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Серверный контракт демонтажа мультиблока: разрушение ЛЮБОЙ части обязано
 * снести весь мультблок — контроллер и все части уходят в воздух, все BE
 * сняты. Часть фикса «призрачной» модели (1.21.1): клиентская эвикция визуала
 * (каналы A/B/B2 + инвариант присутствия в AbstractPartBasedRenderer)
 * опирается на этот серверный инвариант — без него блок-апдейты о сносе
 * до клиента вообще не доходят.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class MultiblockTeardownGameTest {

    private MultiblockTeardownGameTest() {}

    private static final String BATCH = "multiblock_teardown";

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    @GameTest(template = "empty15x7x15", batch = BATCH, timeoutTicks = 200)
    public static void breakingOnePart_tearsDownWholeStructure(GameTestHelper helper) {
        var level = helper.getLevel();
        // Геометрия турбовентилятора как в TurbofanIntegrationGameTest:
        // контроллер в (4,1,6), части x1..7 / z5..7 / y1..3 (FACING по умолчанию).
        BlockPos controller = helper.absolutePos(new BlockPos(4, 1, 6));
        level.setBlock(controller, ModBlocks.TURBOFAN.get().defaultBlockState(), 3);

        List<BlockPos> partPositions = new ArrayList<>();
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 5; z <= 7; z++) {
                    BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
                    if (pos.equals(controller)) continue;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof IMultiblockPart) {
                        partPositions.add(pos);
                    }
                }
            }
        }
        check(partPositions.size() > 0, "turbofan must have placed multiblock part BEs after setBlock (onPlace cascade)");
        check(level.getBlockEntity(controller) != null, "controller BE must exist");

        // Разрушаем одну ЛЮБУЮ часть (не контроллер) — тот же путь, что и у пикапа игрока.
        BlockPos brokenPart = partPositions.get(0);
        level.destroyBlock(brokenPart, false);

        // Каскад синхронный; даём пару тиков на оседание и проверяем инвариант.
        helper.runAfterDelay(2, () -> {
            for (BlockPos pos : partPositions) {
                check(level.getBlockState(pos).isAir(),
                        "part " + pos.toShortString() + " must be air after part-break teardown");
                check(level.getBlockEntity(pos) == null,
                        "part BE " + pos.toShortString() + " must be removed");
            }
            check(level.getBlockState(controller).isAir(),
                    "controller must be air after part-break teardown");
            check(level.getBlockEntity(controller) == null, "controller BE must be removed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty15x7x15", batch = BATCH, timeoutTicks = 200)
    public static void breakingController_tearsDownAllParts(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(4, 1, 6));
        level.setBlock(controller, ModBlocks.TURBOFAN.get().defaultBlockState(), 3);

        List<BlockPos> partPositions = new ArrayList<>();
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 5; z <= 7; z++) {
                    BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
                    if (!pos.equals(controller) && level.getBlockEntity(pos) instanceof IMultiblockPart) {
                        partPositions.add(pos);
                    }
                }
            }
        }
        check(partPositions.size() > 0, "turbofan must have placed multiblock part BEs after setBlock");

        level.destroyBlock(controller, false);

        helper.runAfterDelay(2, () -> {
            for (BlockPos pos : partPositions) {
                check(level.getBlockState(pos).isAir(),
                        "part " + pos.toShortString() + " must be air after controller teardown");
                check(level.getBlockEntity(pos) == null,
                        "part BE " + pos.toShortString() + " must be removed");
            }
            helper.succeed();
        });
    }
}
