//? if fabric {
package com.hbm_m.main;

import com.hbm_m.client.ClientSetup;
import com.hbm_m.config.ModConfigKeybindHandler;
import com.hbm_m.event.ClientModEvents;
import net.fabricmc.api.ClientModInitializer;

public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModConfigKeybindHandler.init();
        ClientModEvents.init();
        ClientSetup.initClient();
    }
}
//?}
