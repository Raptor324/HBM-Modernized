package com.hbm_m.client.missile.track;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTickEvent;

//? if fabric {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?}

public final class MissileTrackClientEvents {

    private MissileTrackClientEvents() {}

    public static void register() {
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> MissileTrackClient.clear());
        ClientTickEvent.CLIENT_POST.register(client -> MissileTrackClient.tick());

        //? if fabric {
        /*WorldRenderEvents.AFTER_ENTITIES.register(context ->
                MissileTrackWorldRender.render(context.tickDelta(), context.matrixStack()));
        *///?}
        // Forge: {@link com.hbm_m.client.ClientModEvents#onRenderLevelStage} AFTER_ENTITIES
    }
}
