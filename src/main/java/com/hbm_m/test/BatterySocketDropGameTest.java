package com.hbm_m.test;

import java.util.List;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Диагностика дропа battery socket при ломании в выживании.
 *
 * <p>Три отдельных проверки:
 * <ol>
 *   <li>лут-таблица блока отдаёт предмет (dropResources без инструмента);</li>
 *   <li>каскад при ломании ЧАСТИ структуры дропает предмет контроллера
 *       (уровень.destroyBlock(part, false) -> destroyBlock(controller, true));</li>
 *   <li>vanilla canHarvestBlock/hasCorrectToolForDrops с разными тирами кирки
 *       (needs_iron_tool).</li>
 * </ol>
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class BatterySocketDropGameTest {

    private BatterySocketDropGameTest() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static BlockPos placeSocket(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, ModBlocks.MACHINE_BATTERY_SOCKET.get().defaultBlockState(), 3);
        return pos;
    }

    private static long countDrops(GameTestHelper helper, BlockPos pos, int radius) {
        List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(radius));
        return items.stream().filter(e -> e.getItem().is(ModBlocks.MACHINE_BATTERY_SOCKET.get().asItem())).count();
    }

    private static void clearDrops(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8)).forEach(e -> e.discard());
    }

    /** 1) Лут-таблица: dropResources без игрока/инструмента обязан дропнуть предмет. */
    @GameTest(template = "empty3x3x3", batch = "battery_socket_drop", timeoutTicks = 100)
    public static void lootTableDropsItem(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            BlockPos pos = placeSocket(helper);
            BlockState state = helper.getLevel().getBlockState(pos);
            check(state.getBlock() == ModBlocks.MACHINE_BATTERY_SOCKET.get(), "socket must be placed");
            clearDrops(helper, pos);

            net.minecraft.world.level.block.Block.dropResources(state, helper.getLevel(), pos,
                    helper.getLevel().getBlockEntity(pos));

            check(countDrops(helper, pos, 3) > 0, "loot table must drop machine_battery_socket item");
            helper.succeed();
        });
    }

    /** 2) Ломаем ЧАСТЬ структуры - каскад должен дропнуть предмет контроллера. */
    @GameTest(template = "empty3x3x3", batch = "battery_socket_drop", timeoutTicks = 100)
    public static void partBreakCascadeDropsItem(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            BlockPos controller = placeSocket(helper);
            // ищем какую-нибудь часть 2x2x2 (контроллер в нижнем слое, часть выше)
            BlockPos part = null;
            for (int dx = 0; dx < 2 && part == null; dx++) {
                for (int dy = 0; dy < 2 && part == null; dy++) {
                    for (int dz = 0; dz < 2 && part == null; dz++) {
                        BlockPos p = controller.offset(dx, dy, dz);
                        if (!p.equals(controller)
                                && helper.getLevel().getBlockState(p).getBlock() instanceof com.hbm_m.block.UniversalMachinePartBlock) {
                            part = p;
                        }
                    }
                }
            }
            check(part != null, "structure must have universal part cells");
            clearDrops(helper, controller);

            helper.getLevel().destroyBlock(part, false);

            helper.runAfterDelay(2, () -> {
                check(helper.getLevel().getBlockState(controller).isAir(),
                        "controller must be air after part-break teardown");
                check(countDrops(helper, controller, 6) > 0,
                        "part-break cascade must drop the controller item (Level.destroyBlock(controller, true)); items nearby: "
                                + helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(controller).inflate(8)).size());
                helper.succeed();
            });
        });
    }

    /** 3) canHarvestBlock: какой тир инструмента нужен (needs_iron_tool?). */
    @GameTest(template = "empty3x3x3", batch = "battery_socket_drop", timeoutTicks = 100)
    public static void toolTierHarvest(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            BlockPos pos = placeSocket(helper);
            BlockState state = helper.getLevel().getBlockState(pos);
            //? if < 1.21.1 {
            Player player = helper.makeMockPlayer();
            //?} else {
            /*Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            *///?}
            boolean hand = state.canHarvestBlock(helper.getLevel(), pos, player);
            player.getInventory().items.set(0, new ItemStack(Items.IRON_PICKAXE));
            player.getInventory().selected = 0;
            boolean iron = state.canHarvestBlock(helper.getLevel(), pos, player);
            if (!iron) throw new GameTestAssertException("iron pickaxe cannot harvest (hand=" + hand + ")");
            if (hand) throw new GameTestAssertException("hand CAN harvest - tool tier is not required");
            helper.succeed();
        });
    }
}
