package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity;
import com.hbm_m.blockentity.machines.MachinePUREXBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Серверный контракт постановки и GUI для PUREX и Precision Assembler:
 * - структура мультиблока раскладывается вокруг ядра (onPlace-каскад);
 * - BlockEntity живёт и тикает (serverTick с рецептурными запросами не падает);
 * - меню создаётся через createMenu (тот же путь, что у открытия GUI);
 * - 1.20.1: {@code BlockState.use} открывает GUI-путь (возвращает не FAIL);
 *   1.21.1: у PrecAss должен быть переопределён {@code useWithoutItem} — это
 *   чинит «GUI не открывается» при пустой руке (регрессия на useItemOn).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class PrecAssPurexGameTest {

    private PrecAssPurexGameTest() {}

    private static final String BATCH = "precass_purex";

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static Player makePlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }

    @GameTest(template = "empty15x7x15", batch = BATCH, timeoutTicks = 200)
    public static void precassPlacesStructureAndMenuOpens(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(7, 1, 7));
        level.setBlock(core, ModBlocks.MACHINE_PRECASS.get().defaultBlockState(), 3);

        helper.runAfterDelay(3, () -> {
            BlockState state = level.getBlockState(core);
            check(state.is(ModBlocks.MACHINE_PRECASS.get()), "precass core must survive placement");
            BlockEntity be = level.getBlockEntity(core);
            check(be instanceof MachinePrecAssBlockEntity, "precass BE must exist at the core");

            // Структура: 8 базовых клеток вокруг ядра (extra-порты) + башня на 2 вверх.
            check(level.getBlockState(core.offset(1, 0, 0)).is(ModBlocks.UNIVERSAL_MACHINE_PART.get()),
                    "precass base ring must be universal machine parts (+X)");
            check(level.getBlockState(core.offset(-1, 0, -1)).is(ModBlocks.UNIVERSAL_MACHINE_PART.get()),
                    "precass base ring must be universal machine parts (-X,-Z corner)");
            check(level.getBlockState(core.offset(0, 1, 0)).is(ModBlocks.UNIVERSAL_MACHINE_PART.get()),
                    "precass tower cell above the core must be a machine part");

            // Меню строится (тот же путь, что и открытие GUI клиентом).
            Player player = makePlayer(helper);
            var menu = ((MachinePrecAssBlockEntity) be).createMenu(0, player.getInventory(), player);
            check(menu != null, "precass createMenu must build the MachinePrecAssMenu");
            check(menu.slots.size() == 22 + 36, "precass menu slot count (22 machine + 36 player), got " + menu.slots.size());

            // 1.20.1: публичный Block#use обязан прокинуть открытие (не FAIL).
            //? if < 1.21.1 {
            var result = ((com.hbm_m.block.machines.MachinePrecAssBlock) state.getBlock()).use(state, level,
                    core, player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.ZERO,
                            net.minecraft.core.Direction.UP, core.above(), false));
            check(result != net.minecraft.world.InteractionResult.FAIL, "precass use must not FAIL");
            //?}

            helper.succeed();
        });
    }

    @GameTest(template = "empty15x7x15", batch = BATCH, timeoutTicks = 200)
    public static void purexPlacesStructureAndMenuOpens(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(7, 1, 7));
        level.setBlock(core, ModBlocks.PUREX.get().defaultBlockState(), 3);

        helper.runAfterDelay(3, () -> {
            BlockState state = level.getBlockState(core);
            check(state.is(ModBlocks.PUREX.get()), "purex core must survive placement");
            BlockEntity be = level.getBlockEntity(core);
            check(be instanceof MachinePUREXBlockEntity, "purex BE must exist at the core");

            // Структура 5x5 в основании (кольцо портов) + башня на 2 вверх.
            check(level.getBlockState(core.offset(2, 0, 0)).is(ModBlocks.UNIVERSAL_MACHINE_PART.get()),
                    "purex base ring must be universal machine parts (+2X)");
            check(level.getBlockState(core.offset(-2, 0, -2)).is(ModBlocks.UNIVERSAL_MACHINE_PART.get()),
                    "purex base ring must be universal machine parts (-2X,-2Z corner)");

            Player player = makePlayer(helper);
            var menu = ((MachinePUREXBlockEntity) be).createMenu(0, player.getInventory(), player);
            check(menu != null, "purex createMenu must build the MachinePUREXMenu");

            helper.succeed();
        });
    }
}
