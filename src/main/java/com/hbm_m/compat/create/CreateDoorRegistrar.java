//? if forge || neoforge {
package com.hbm_m.compat.create;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.UniversalMachinePartBlock;
import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * Регистрирует Create {@link MovementBehaviour} + {@link MovingInteractionBehaviour}
 * для каждого HBM {@link DoorBlock}. Запускается на FMLCommonSetup (после регистрации
 * блоков) только когда Create загружен.
 *
 * <p>Регистры Create — публичные статические {@code SimpleRegistry<Block, ...>},
 * вызов {@code register(Block, ...)} публичный, без event/IMP. Повторная регистрация
 * бросает IllegalArgumentException, поэтому регистрируем строго один раз.
 */
public final class CreateDoorRegistrar {

    private CreateDoorRegistrar() {}

    public static void register() {
        int count = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null || !RefStrings.MODID.equals(key.getNamespace())) continue;
            if (!(block instanceof DoorBlock door)) continue;

            try {
                MovementBehaviour.REGISTRY.register(door, new HbmDoorMovementBehaviour(door.getDoorDeclId()));
                MovingInteractionBehaviour.REGISTRY.register(door, new HbmDoorInteractionBehaviour());
                count++;
            } catch (IllegalArgumentException duplicate) {
                // Уже зарегистрировано (напр. двойной проход) — пропускаем.
            } catch (Throwable t) {
                MainRegistry.LOGGER.warn("[HBM/Create] Не удалось зарегистрировать door-behaviour для {}: {}", key, t.toString());
            }
        }
        if (count > 0) {
            MainRegistry.LOGGER.info("[HBM/Create] Зарегистрированы door-поведения для {} дверей.", count);
        }

        // Interaction для блоков-частей двери (UniversalMachinePartBlock), чтобы ПКМ по
        // части на поезде тогглил дверь-владельца (как UniversalMachinePartBlock.use на земле).
        // Не-дверные parts поведение no-op'ает (возвращает false).
        Block partBlock = ModBlocks.UNIVERSAL_MACHINE_PART.get();
        if (partBlock instanceof UniversalMachinePartBlock ump) {
            try {
                MovingInteractionBehaviour.REGISTRY.register(ump, new HbmDoorPartInteractionBehaviour());
            } catch (IllegalArgumentException duplicate) {
                // уже зарегистрировано
            } catch (Throwable t) {
                MainRegistry.LOGGER.warn("[HBM/Create] Не удалось зарегистрировать part-interaction: {}", t.toString());
            }
        }
    }
}
//?}
