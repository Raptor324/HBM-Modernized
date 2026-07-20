//? if forge {
package com.hbm_m.compat.create;

import com.hbm_m.compat.ContraptionDoorState;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Right-click по блоку-ЧАСТИ двери (UniversalMachinePartBlock) на собранном поезде.
 *
 * <p>На земле {@code UniversalMachinePartBlock.use} роутит клик на контроллер через
 * {@code getControllerPos()}. На контрапшене NBT-координаты контроллера — мировые, а
 * blocks-map ключируется локальными позициями, поэтому прямой роутинг не работает.
 * Контроллер достаётся из кэша {@link ContraptionDoorState#getControllerForPart},
 * который наполняет {@code DoorShapeComputer.populate} на каждом тике behaviour; если
 * записи ещё нет (дверь не успела тикнуть после сборки) — клик игнорируем, на следующем
 * тике повторим.
 *
 * <p>Не-дверные parts (radar/launchpad/…) → в кэше отсутствуют → возвращаем false
 * (клик не поглощаем).
 */
public class HbmDoorPartInteractionBehaviour extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos partLocalPos,
                                           AbstractContraptionEntity contraptionEntity) {
        Contraption contraption = contraptionEntity.getContraption();
        if (contraption == null) return false;

        // ContraptionWorld — тот же Level, по которому DoorShapeComputer.populate
        // разложил part→controller. Берём контроллер оттуда (O(1)), а не сканируем
        // blocks-map × structure-offsets на каждый клик.
        Level cw = CreateLevelAccess.contraptionCollisionLevel(contraption);
        if (cw == null) return false;

        BlockPos controllerLocal = ContraptionDoorState.getControllerForPart(cw, partLocalPos);
        if (controllerLocal == null) return false; // часть не двери (или кэш ещё пуст) — не поглощаем клик

        for (Pair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
            MovementContext ctx = pair.getRight();
            if (ctx == null || ctx.localPos == null || !ctx.localPos.equals(controllerLocal)) continue;
            if (ctx.world != null && !ctx.world.isClientSide) {
                ctx.data.putBoolean("wantToggle", true);
            }
            return true;
        }
        return true;
    }
}
//?}
