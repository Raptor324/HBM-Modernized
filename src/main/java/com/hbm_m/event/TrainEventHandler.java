package com.hbm_m.event;

import com.hbm_m.entity.train.EntityRailCarBase;

import dev.architectury.event.events.common.TickEvent;

/** Original {@code ModEventHandler.worldTick} (Server, Phase END): {@code EntityRailCarBase.updateMotion(world)}. */
public final class TrainEventHandler {

    private TrainEventHandler() { }

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(EntityRailCarBase::updateMotion);
    }
}
