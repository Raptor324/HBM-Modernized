//? if forge || neoforge {
package com.hbm_m.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Правый клик по двери на собранном поезде. Не мутирует состояние сам — только
 * ставит {@code wantToggle} флаг в {@link MovementContext#data} соответствующего
 * actor'а; {@link HbmDoorMovementBehaviour#tick} на сервере его обрабатывает
 * (единая точка правды для toggle/коллизии/звуков).
 */
public class HbmDoorInteractionBehaviour extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        Contraption contraption = contraptionEntity.getContraption();
        if (contraption == null) return true;

        for (Pair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
            StructureBlockInfo info = pair.getLeft();
            MovementContext ctx = pair.getRight();
            if (info == null || ctx == null || ctx.localPos == null) continue;
            if (!ctx.localPos.equals(localPos)) continue;

            // Только серверная сторона мутирует (поведение читает wantToggle на сервере).
            if (ctx.world != null && !ctx.world.isClientSide) {
                ctx.data.putBoolean("wantToggle", true);
            }
            break;
        }
        return true; // поглотить клик
    }
}
//?}
