package com.hbm_m.network;

import com.hbm_m.inventory.menu.PneumoStorageAccessMenu;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Der Anzeigezustand des Zugangsterminals: Blaetterstelle, Suchtext, Sortierweise und die
 * ausfuehrliche Suche.
 *
 * <p>Im Original teilen sich beide Seiten denselben Container, und der Bildschirm schiebt die
 * Blaetterstelle mit einem Klick auf die Sonderplatznummer {@code -666} hinueber. In 1.20 baut der
 * Server das Schaufenster allein, darum geht der ganze Zustand hier als eigenes Paket - sonst
 * blieben Suche und Sortierung reine Anzeige ohne Wirkung.</p>
 */
public class PneumoAccessStateC2SPacket implements C2SPacket {

    private static final int MAX_SEARCH_LENGTH = 50;

    private final int listingStart;
    private final String search;
    private final int sorting;
    private final boolean detailedSearch;

    private PneumoAccessStateC2SPacket(int listingStart, String search, int sorting, boolean detailedSearch) {
        this.listingStart = listingStart;
        this.search = search;
        this.sorting = sorting;
        this.detailedSearch = detailedSearch;
    }

    public static PneumoAccessStateC2SPacket decode(FriendlyByteBuf buf) {
        return new PneumoAccessStateC2SPacket(buf.readInt(), buf.readUtf(MAX_SEARCH_LENGTH),
                buf.readInt(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(listingStart);
        buf.writeUtf(search, MAX_SEARCH_LENGTH);
        buf.writeInt(sorting);
        buf.writeBoolean(detailedSearch);
    }

    public static void handle(PneumoAccessStateC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof PneumoStorageAccessMenu menu)) return;

            menu.setSorting(msg.sorting);
            menu.setDetailedSearch(msg.detailedSearch);
            menu.setSearchString(msg.search);
            // Zuletzt, weil die drei davor die Anzeige an den Anfang setzen.
            menu.setListingStart(msg.listingStart);
        });
    }

    public static void send(int listingStart, String search, int sorting, boolean detailedSearch) {
        String trimmed = search.length() > MAX_SEARCH_LENGTH ? search.substring(0, MAX_SEARCH_LENGTH) : search;
        ModPacketHandler.sendToServer(ModPacketHandler.PNEUMO_ACCESS_STATE,
                new PneumoAccessStateC2SPacket(listingStart, trimmed, sorting, detailedSearch));
    }
}
