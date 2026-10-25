package com.hbm_m.client.util;

/** Sprachcode des Clients (Original {@code ClientProxy.getLanguageCode}); nur clientseitig laden. */
public final class ClientLang {

    private ClientLang() { }

    public static String code() {
        return net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected();
    }
}
