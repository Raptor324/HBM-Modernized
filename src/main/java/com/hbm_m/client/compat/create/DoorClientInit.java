//? if forge {
package com.hbm_m.client.compat.create;

import com.hbm_m.compat.ContraptionDoorState;
import com.hbm_m.compat.create.CreateLevelAccess;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Клиентская инициализация render-состояния двери на контрапшене.
 */
@OnlyIn(Dist.CLIENT)
public final class DoorClientInit {

    private DoorClientInit() {}

    public static void initRenderState(Contraption contraption, BlockPos controllerPos, boolean open) {
        if (contraption == null || controllerPos == null) return;
        Object clientContraption;
        try {
            clientContraption = contraption.getClass().getMethod("getOrCreateClientContraptionLazy").invoke(contraption);
        } catch (Throwable t) {
            // Create API переименовал/убрал метод — рендер-состояние двери на контрапшене
            // не проинициализируется (до первого S2C-пакета). Не фатально, но видимо в логе.
            com.hbm_m.main.MainRegistry.LOGGER.debug(
                    "[HBM/Create] DoorClientInit: getOrCreateClientContraptionLazy failed: {}", t.toString());
            return;
        }
        Level vw = CreateLevelAccess.clientRenderLevel(clientContraption);
        if (vw != null) {
            ContraptionDoorState.markContraptionWorld(vw);
            ContraptionDoorState.setOpen(vw, controllerPos, open);
        }
    }
}
//?}