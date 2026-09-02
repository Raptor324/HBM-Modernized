//? if forge || neoforge {
package com.hbm_m.compat.create;

import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.compat.ContraptionDoorState;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Вычисляет и кладёт в {@link ContraptionDoorState} коллизию всех блоков двери
 * (контроллер + части) для текущего open/closed состояния. Общий для серверного
 * behaviour и клиентского packet-applier.
 *
 * <p>Для поездов (ContraptionWorld) форма теперь собирается в единый VoxelShape,
 * который назначается контроллеру, а фантомные блоки становятся прозрачными.
 */
public final class DoorShapeComputer {

    private DoorShapeComputer() {}

    public static void populate(Level level, BlockPos controllerLocalPos, String doorDeclId,
                                Direction facing, boolean open) {
        if (level == null || controllerLocalPos == null) return;
        
        ContraptionDoorState.markContraptionWorld(level);
        
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        if (decl == null || decl.getStructureDefinition() == null) {
            // Нет схемы — тривиально: контроллер solid когда закрыт, empty когда открыт.
            ContraptionDoorState.setShape(level, controllerLocalPos, open ? Shapes.empty() : Shapes.block());
            ContraptionDoorState.setOpen(level, controllerLocalPos, open);
            ContraptionDoorState.setPartController(level, controllerLocalPos, controllerLocalPos);
            return;
        }
        var def = decl.getStructureDefinition();
        var closed = def.getClosedShapes();
        var opened = def.getOpenShapes();

        VoxelShape combined = Shapes.empty();

        for (BlockPos offset : closed.keySet()) {
            VoxelShape base = open ? opened.get(offset) : closed.get(offset);
            if (base == null) base = open ? Shapes.empty() : Shapes.block();
            
            // Сохраняем связи частей и контроллера для взаимодействия по фантомным блокам
            BlockPos rotatedOffset = MultiblockStructureHelper.rotate(offset, facing);
            BlockPos partLocalPos = controllerLocalPos.offset(rotatedOffset);
            ContraptionDoorState.setPartController(level, partLocalPos, controllerLocalPos);

            if (base.isEmpty()) continue;
            
            VoxelShape rotated = MultiblockStructureHelper.rotateShape(base, facing);
            
            // Собираем все части двери в единую форму относительно контроллера
            combined = Shapes.or(combined, rotated.move(rotatedOffset.getX(), rotatedOffset.getY(), rotatedOffset.getZ()));
        }
        
        // Оптимизируем и сохраняем 1 большую форму для контроллера
        ContraptionDoorState.setShape(level, controllerLocalPos, combined.optimize());
        ContraptionDoorState.setOpen(level, controllerLocalPos, open);
    }
}
//?}