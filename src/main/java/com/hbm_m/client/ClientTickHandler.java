package com.hbm_m.client;

// Клиентский обработчик тиков для проверки, на какой блок смотрит игрок каждые 20 тиков.
// Если игрок смотрит на MachineAdvancedAssemblerBlockEntity или его часть, выводим в лог статус сборки.
// Это полезно для отладки и проверки взаимодействия с мультиблочными структурами.
import com.hbm_m.block.UniversalMachinePartBlock;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.IMultiblockPart;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientTickHandler {

    private int tickCounter = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || tickCounter++ % 20 != 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        HitResult result = mc.hitResult;
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockResult = (BlockHitResult) result;
        BlockPos lookingAtPos = blockResult.getBlockPos();
        BlockState lookingAtState = mc.level.getBlockState(lookingAtPos);
        BlockEntity targetBE = null;
        
        // Scenario 1: We are looking directly at the main block
        if (mc.level.getBlockEntity(lookingAtPos) instanceof MachineAdvancedAssemblerBlockEntity aamBe) {
            targetBE = aamBe;
        
        // Scenario 2: We are looking at a part block
        } else if (lookingAtState.getBlock() instanceof UniversalMachinePartBlock) {

            // Ask the part where its controller is, don't calculate it.
            if (mc.level.getBlockEntity(lookingAtPos) instanceof IMultiblockPart part) {
                BlockPos masterPos = part.getControllerPos();
                if (masterPos != null && mc.level.getBlockEntity(masterPos) instanceof MachineAdvancedAssemblerBlockEntity aamBe) {
                    targetBE = aamBe;
                }
            }

        }

        if (targetBE != null) {
             MainRegistry.LOGGER.debug("[CLIENT TICK] Looking at AAMBE. Crafting Status: {}", 
                ((MachineAdvancedAssemblerBlockEntity) targetBE).isCrafting());
        }
    }
}