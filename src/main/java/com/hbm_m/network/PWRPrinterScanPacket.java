package com.hbm_m.network;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 1:1 {@code ItemPWRPrinter.serialize}/{@code deserialize}: Grenzen des Reaktors, Blickrichtung und das gemerkte Bauteil
 * jedes Traegers (Block-ID, 0 = keins). Das Original haengt das an das Sync-Paket des Controllers, hier ist es ein eigenes
 * Paket. Der Client schreibt die Bauteile in seine Traeger und oeffnet den Schnittdrucker.
 *
 * <p>Der Handler ruft den Client-Hook nur ueber den voll qualifizierten Namen in der Lambda auf, damit der Server beim
 * Registrieren keine Client-Klassen laedt.</p>
 */
public class PWRPrinterScanPacket implements S2CPacket {

    private final int x1, y1, z1, x2, y2, z2, dir;
    private final int[] blocks;

    public PWRPrinterScanPacket(int x1, int y1, int z1, int x2, int y2, int z2, int dir, int[] blocks) {
        this.x1 = x1; this.y1 = y1; this.z1 = z1;
        this.x2 = x2; this.y2 = y2; this.z2 = z2;
        this.dir = dir;
        this.blocks = blocks;
    }

    public static PWRPrinterScanPacket decode(FriendlyByteBuf buf) {
        int x1 = buf.readInt(), y1 = buf.readInt(), z1 = buf.readInt();
        int x2 = buf.readInt(), y2 = buf.readInt(), z2 = buf.readInt();
        int dir = buf.readInt();
        int[] blocks = buf.readVarIntArray();
        return new PWRPrinterScanPacket(x1, y1, z1, x2, y2, z2, dir, blocks);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(x1); buf.writeInt(y1); buf.writeInt(z1);
        buf.writeInt(x2); buf.writeInt(y2); buf.writeInt(z2);
        buf.writeInt(dir);
        buf.writeVarIntArray(blocks);
    }

    public static void handle(PWRPrinterScanPacket p, PacketContext context) {
        context.queue(() -> com.hbm_m.client.PwrPrinterClientHooks.deserialize(p.x1, p.y1, p.z1, p.x2, p.y2, p.z2, p.dir, p.blocks));
    }
}
