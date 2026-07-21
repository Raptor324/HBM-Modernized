//? if forge {
package com.hbm_m.compat.create;

import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.compat.ContraptionDoorState;
import com.hbm_m.network.DoorContraptionStatePacket;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * Door-actor для Create-контрапшенов. НОЛЬ blockstate-изменений → ноль reset'ов.
 * Состояние синкается S2C-пакетом через vanilla-only кэш {@link ContraptionDoorState}.
 */
public class HbmDoorMovementBehaviour implements MovementBehaviour {

    private final String doorDeclId;

    public HbmDoorMovementBehaviour(String doorDeclId) {
        this.doorDeclId = doorDeclId;
    }

    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return false;
    }

    @Override
    public void tick(MovementContext context) {
        if (context == null || context.world == null || context.contraption == null || context.localPos == null) {
            return;
        }
        Contraption contraption = context.contraption;
        BlockPos controllerPos = context.localPos;
        Level cw = CreateLevelAccess.contraptionCollisionLevel(contraption);
        if (cw == null) return;

        Direction facing = facingOf(contraption, controllerPos);

        if (!ContraptionDoorState.hasOpenEntry(cw, controllerPos)) {
            boolean initialOpen;
            
            // Загружаем состояние двери из сохраненных данных контекста
            if (context.data.contains("isOpen")) {
                initialOpen = context.data.getBoolean("isOpen");
            } else {
                initialOpen = assemblyOpen(contraption, controllerPos);
                context.data.putBoolean("isOpen", initialOpen);
            }
            
            DoorShapeComputer.populate(cw, controllerPos, doorDeclId, facing, initialOpen);
            // Важно инвалидировать при инициализации, чтобы Create сразу увидел правильную коллизию
            contraption.invalidateColliders();
            
            if (context.world.isClientSide) {
                initClientRenderState(contraption, controllerPos, initialOpen);
            }
        }

        if (!context.world.isClientSide) {
            handleServerToggle(context, contraption, cw, controllerPos, facing);
        }
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (context == null || context.world == null || !context.world.isClientSide) return;
        invokeStopAll(context);
    }

    private void handleServerToggle(MovementContext context, Contraption contraption, Level cw,
                                    BlockPos controllerPos, Direction facing) {
        CompoundTag data = context.data;
        if (!data.getBoolean("wantToggle")) return;
        data.putBoolean("wantToggle", false);

        long now = System.currentTimeMillis();
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        int openTime = decl != null ? decl.getOpenTime() : 60;
        long totalMs = Math.max(1, openTime) * 50L;
        if (data.contains("lastToggle") && (now - data.getLong("lastToggle")) < totalMs) return;
        data.putLong("lastToggle", now);

        boolean currentOpen = ContraptionDoorState.getOpen(cw, controllerPos);
        boolean newOpen = !currentOpen;
        
        // Сохраняем состояние открытости для персистентности (load/unload чанка)
        data.putBoolean("isOpen", newOpen);
        
        DoorShapeComputer.populate(cw, controllerPos, doorDeclId, facing, newOpen);
        contraption.invalidateColliders();

        // ======= UPDATE CONTRAPTION BLOCKS FOR DISASSEMBLY =======
        // Обновляем блоки внутри поезда, чтобы при его разборке двери размещались с правильным состоянием
        StructureBlockInfo info = contraption.getBlocks().get(controllerPos);
        if (info != null && info.state().hasProperty(DoorBlock.OPEN)) {
            BlockState newState = info.state().setValue(DoorBlock.OPEN, newOpen).setValue(DoorBlock.DOOR_MOVING, false);
            CompoundTag nbt = info.nbt();
            if (nbt != null) {
                nbt = nbt.copy();
            } else {
                nbt = new CompoundTag();
            }
            nbt.putByte("state", (byte) (newOpen ? 1 : 0));
            nbt.putInt("openTicks", newOpen ? openTime : 0);
            contraption.getBlocks().put(controllerPos, new StructureBlockInfo(info.pos(), newState, nbt));
            
            if (decl != null && decl.getStructureDefinition() != null) {
                for (BlockPos offset : decl.getStructureDefinition().getClosedShapes().keySet()) {
                    if (offset.equals(BlockPos.ZERO)) continue;
                    BlockPos rotated = com.hbm_m.multiblock.MultiblockStructureHelper.rotate(offset, facing);
                    BlockPos partPos = controllerPos.offset(rotated);
                    StructureBlockInfo partInfo = contraption.getBlocks().get(partPos);
                    if (partInfo != null && partInfo.state().hasProperty(com.hbm_m.block.UniversalMachinePartBlock.PASSABLE)) {
                        BlockState newPartState = partInfo.state().setValue(com.hbm_m.block.UniversalMachinePartBlock.PASSABLE, newOpen);
                        contraption.getBlocks().put(partPos, new StructureBlockInfo(partInfo.pos(), newPartState, partInfo.nbt()));
                    }
                }
            }
        }

        if (context.position != null && context.world instanceof ServerLevel serverLevel
                && contraption.entity != null) {
            DoorContraptionStatePacket.sendToNear(serverLevel, context.position,
                    contraption.entity.getId(), controllerPos, doorDeclId, facing, newOpen);
        }
    }

    private static Direction facingOf(Contraption contraption, BlockPos pos) {
        StructureBlockInfo info = contraption.getBlocks().get(pos);
        if (info != null && info.state().hasProperty(DoorBlock.FACING)) {
            return info.state().getValue(DoorBlock.FACING);
        }
        return Direction.NORTH;
    }

    private static boolean assemblyOpen(Contraption contraption, BlockPos pos) {
        StructureBlockInfo info = contraption.getBlocks().get(pos);
        return info != null && info.state().hasProperty(DoorBlock.OPEN) && info.state().getValue(DoorBlock.OPEN);
    }

    private static final String CLIENT_INIT = "com.hbm_m.client.compat.create.DoorClientInit";
    private static final String SOUND_HELPER = "com.hbm_m.client.compat.create.ContraptionDoorSoundHelper";

    private static void initClientRenderState(Contraption contraption, BlockPos controllerPos, boolean open) {
        try {
            Class<?> cls = Class.forName(CLIENT_INIT);
            cls.getMethod("initRenderState", Contraption.class, BlockPos.class, boolean.class)
                    .invoke(null, contraption, controllerPos, open);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.debug("[HBM/Create] DoorClientInit unavailable: {}", t.toString());
        }
    }

    private static void invokeStopAll(MovementContext ctx) {
        try {
            Class<?> cls = Class.forName(SOUND_HELPER);
            cls.getMethod("stopAll", MovementContext.class, DoorDecl.class, boolean.class)
                    .invoke(null, ctx, null, false);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.debug("[HBM/Create] door sound helper unavailable: {}", t.toString());
        }
    }
}
//?}