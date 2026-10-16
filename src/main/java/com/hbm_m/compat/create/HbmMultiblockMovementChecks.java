package com.hbm_m.compat.create;

//? if forge || neoforge {
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;

import com.simibubi.create.api.contraption.BlockMovementChecks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Встроенная "взаимная приклеенность" блоков мультиблоков HBM для систем сборки
 * Create / simulated (Aeronautics).
 *
 * <p><b>Идея:</b> блоки, висящие на других (факелы, рычаги), попадают в контрапшен
 * БЕЗ клея — через {@code BlockMovementChecks.isBlockAttachedTowards}. Мы
 * регистрируем свою Attached-проверку: блок мультиблока (часть или контроллер)
 * считается привязанным к любому соседнему блоку ТОГО ЖЕ мультиблока. В результате
 * BFS сборки, задев ХОТЯ БЫ ОДИН блок мультиблока (клеем игрока любого типа,
 * поршнем, структурной связью), автоматически захватывает ВСЮ конструкцию как
 * единое целое — никакого AABB-клея и сотен glue-entity не требуется.
 *
 * <p>Свойство действует в обе стороны: части привязаны к контроллеру и друг к другу,
 * контроллер — к своим частям. Чужие структуры к нашим блокам НЕ притягиваются
 * (проверка отвечает PASS вне контекста мультиблока).
 *
 * <p>Регистрация — один раз на процесс, через {@link #register()} из common setup
 * (см. {@code CreateCompat}), только когда Create загружен.
 */
public final class HbmMultiblockMovementChecks {

    private static boolean registered;

    private HbmMultiblockMovementChecks() {}

    public static void register() {
        if (registered) return;
        registered = true;
        BlockMovementChecks.registerAttachedCheck(HbmMultiblockMovementChecks::isAttachedToSameMultiblock);
        // Наши блоки всегда перемещаемы: явный SUCCESS исключает отказ по тегам/настройкам,
        // а лог показывает, что BFS вообще рассматривал контроллер как кандидата.
        BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) -> {
            if (isHbmMultiblockBlock(state)) {
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM][AllowedCheck] кандидат {} ({}) разрешён к перемещению",
                        pos.toShortString(), state.getBlock());
                return BlockMovementChecks.CheckResult.SUCCESS;
            }
            return BlockMovementChecks.CheckResult.PASS;
        });
    }

    private static boolean isHbmMultiblockBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof IMultiblockController
                || state.getBlock() instanceof com.hbm_m.block.UniversalMachinePartBlock;
    }

    /**
     * @param state     состояние блока-кандидата (соседа, который может присоединиться к BFS)
     * @param level     уровень
     * @param pos       позиция кандидата
     * @param direction направление ОТ кандидата К уже захваченному блоку ("опоре")
     */
    private static BlockMovementChecks.CheckResult isAttachedToSameMultiblock(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity be = level.getBlockEntity(pos);

        // Кандидат — часть: привязана к своему контроллеру и к частям своего же станка.
        if (be instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos == null) {
                return BlockMovementChecks.CheckResult.PASS;
            }
            BlockPos support = pos.relative(direction);
            if (support.equals(controllerPos)) {
                return debug(pos, direction, "part->controller", BlockMovementChecks.CheckResult.SUCCESS);
            }
            BlockEntity supportBe = level.getBlockEntity(support);
            if (supportBe instanceof IMultiblockPart supportPart
                    && controllerPos.equals(supportPart.getControllerPos())) {
                return debug(pos, direction, "part->part", BlockMovementChecks.CheckResult.SUCCESS);
            }
            return BlockMovementChecks.CheckResult.PASS;
        }

        // Кандидат — контроллер: привязан к любому своему блоку-части.
        // ВАЖНО: IMultiblockController реализован на КЛАССЕ БЛОКА, а не на BlockEntity,
        // поэтому проверяем state, а не getBlockEntity(pos)!
        if (state.getBlock() instanceof IMultiblockController) {
            BlockEntity supportBe = level.getBlockEntity(pos.relative(direction));
            boolean ok = supportBe instanceof IMultiblockPart supportPart
                    && pos.equals(supportPart.getControllerPos());
            return debug(pos, direction,
                    "controller, support=" + (supportBe == null ? "null" : supportBe.getClass().getSimpleName()),
                    ok ? BlockMovementChecks.CheckResult.SUCCESS : BlockMovementChecks.CheckResult.PASS);
        }

        return BlockMovementChecks.CheckResult.PASS;
    }

    private static BlockMovementChecks.CheckResult debug(BlockPos pos, Direction dir, String what, BlockMovementChecks.CheckResult result) {
        com.hbm_m.main.MainRegistry.LOGGER.info(
                "[HBM][AttachCheck] {} {} dir={} -> {}", what, pos.toShortString(), dir, result);
        return result;
    }
}
//?}
