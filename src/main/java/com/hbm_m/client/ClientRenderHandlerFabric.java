//? if fabric {
/*package com.hbm_m.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

/^*
 * Fabric world-render hook for {@link ClientRenderHandler} highlight boxes.
 ^/
public final class ClientRenderHandlerFabric {
    private ClientRenderHandlerFabric() {}

    public static void register() {
        WorldRenderEvents.AFTER_BLOCK_ENTITIES.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            ClientRenderHandler.onRenderWorldLate(
                    mc.renderBuffers().bufferSource(), ctx.matrixStack(), ctx.camera().getPosition());
        });
    }
}
*///?}
