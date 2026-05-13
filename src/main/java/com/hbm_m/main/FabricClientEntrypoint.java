//? if fabric {
package com.hbm_m.main;

import com.hbm_m.client.ClientSetup;
import com.hbm_m.client.model.loading.ForgeLikeModelLoadingFabric;
import com.hbm_m.config.ModConfigKeybindHandler;
import com.hbm_m.event.ClientModEvents;
import net.fabricmc.api.ClientModInitializer;

@SuppressWarnings("UnstableApiUsage")
public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ForgeLikeModelLoadingFabric.init();
        ModConfigKeybindHandler.init();
        ClientModEvents.init();
        ClientSetup.initClient();
    }
}
//?}
