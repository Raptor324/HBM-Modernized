package com.hbm_m.client;

import com.hbm_m.block.AdvancedAssemblyMachinePartBlock;
import com.hbm_m.block.entity.AdvancedAssemblyMachineBlockEntity;
import com.hbm_m.main.MainRegistry;
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
        
        // Сценарий 1: Мы смотрим прямо на главный блок
        if (mc.level.getBlockEntity(lookingAtPos) instanceof AdvancedAssemblyMachineBlockEntity aamBe) {
            targetBE = aamBe;
        
        // Сценарий 2: Мы смотрим на блок-часть
        } else if (lookingAtState.getBlock() instanceof AdvancedAssemblyMachinePartBlock) {
            // Читаем оффсеты из BlockState части
            int offsetX = lookingAtState.getValue(AdvancedAssemblyMachinePartBlock.OFFSET_X) - 1;
            int offsetY = lookingAtState.getValue(AdvancedAssemblyMachinePartBlock.OFFSET_Y);
            int offsetZ = lookingAtState.getValue(AdvancedAssemblyMachinePartBlock.OFFSET_Z) - 1;

            // Вычисляем позицию главного блока
            BlockPos masterPos = lookingAtPos.offset(-offsetX, -offsetY, -offsetZ);
            
            // Пытаемся получить BlockEntity из вычисленной позиции
            if (mc.level.getBlockEntity(masterPos) instanceof AdvancedAssemblyMachineBlockEntity aamBe) {
                targetBE = aamBe;
            }
        }

        // Если мы нашли наш BlockEntity одним из двух способов
        if (targetBE != null) {
             MainRegistry.LOGGER.debug("[ASSEMBLER >>>] Успешно смотрю на AAMBE. Статус isCrafting: {}", 
                ((AdvancedAssemblyMachineBlockEntity) targetBE).isCrafting());
        }
    }
}