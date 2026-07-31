//? if forge {
package com.hbm_m.client.compat.create;

import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.compat.ContraptionDoorState;
import com.hbm_m.compat.create.CreateLevelAccess;
import com.hbm_m.compat.create.DoorShapeComputer;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Клиентский приёмник {@code DoorContraptionStatePacket}.
 */
@OnlyIn(Dist.CLIENT)
public final class DoorContraptionClientApplier {

    private DoorContraptionClientApplier() {}

    public static void apply(int entityId, long controllerLocalPos, String doorDeclId,
                             byte facingIndex, boolean open) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof AbstractContraptionEntity ace)) return;

        Contraption contraption = ace.getContraption();
        if (contraption == null) return;

        Direction facing = Direction.from3DDataValue(facingIndex);
        BlockPos controllerPos = BlockPos.of(controllerLocalPos);

        Level collisionLevel = CreateLevelAccess.contraptionCollisionLevel(contraption);
        DoorShapeComputer.populate(collisionLevel, controllerPos, doorDeclId, facing, open);

        Object clientContraption = clientContraptionOf(contraption);
        Level renderLevel = CreateLevelAccess.clientRenderLevel(clientContraption);
        if (renderLevel != null) {
            ContraptionDoorState.markContraptionWorld(renderLevel);
            ContraptionDoorState.setOpen(renderLevel, controllerPos, open);
        }

        contraption.invalidateColliders();

        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        MovementContext ctx = findActorContext(contraption, controllerPos);
        if (decl != null && ctx != null) {
            ContraptionDoorSoundHelper.onStart(ctx, decl, open);
        }
    }

    private static Object clientContraptionOf(Contraption contraption) {
        try {
            return contraption.getClass().getMethod("getOrCreateClientContraptionLazy").invoke(contraption);
        } catch (Throwable t) {
            // Create API изменился — рендер-уровень (VirtualRenderWorld) недоступен,
            // анимация двери на этом контрапшене пойдёт через fallback. Не фатально.
            com.hbm_m.main.MainRegistry.LOGGER.debug(
                    "[HBM/Create] clientContraptionOf: getOrCreateClientContraptionLazy failed: {}", t.toString());
            return null;
        }
    }

    private static MovementContext findActorContext(Contraption contraption, BlockPos controllerPos) {
        for (Pair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
            MovementContext ctx = pair.getRight();
            if (ctx != null && ctx.localPos != null && ctx.localPos.equals(controllerPos)) return ctx;
        }
        return null;
    }
}
//?}