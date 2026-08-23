package com.hbm_m.network;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C packet carrying a scanned PWR structure "blueprint" (see {@code PWRFuelPrinterItem}) for
 * the client to display. 1:1 in purpose to the original's piggybacked {@code TileEntityPWRController}
 * sync packet ({@code ItemPWRPrinter.serialize}/{@code deserialize}), implemented as its own
 * dedicated packet instead of overloading the reactor's block-entity sync packet.
 *
 * <p>ВАЖНО: обработчик НЕ ссылается напрямую на клиентские классы (GUI/Minecraft) —
 * открытие экрана делегировано в {@code PwrPrinterClientHooks}. Прямая ссылка линковала
 * иерархию GUIPWRPrinter → Screen уже при регистрации пакета и роняла загрузку мода
 * на выделенном сервере.
 */
public class PWRPrinterScanPacket implements S2CPacket {

    private final int sizeX, sizeY, sizeZ;
    private final byte[] grid;

    public PWRPrinterScanPacket(int sizeX, int sizeY, int sizeZ, byte[] grid) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.grid = grid;
    }

    public static PWRPrinterScanPacket decode(FriendlyByteBuf buf) {
        int sx = buf.readVarInt();
        int sy = buf.readVarInt();
        int sz = buf.readVarInt();
        byte[] grid = new byte[sx * sy * sz];
        buf.readBytes(grid);
        return new PWRPrinterScanPacket(sx, sy, sz, grid);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(sizeX);
        buf.writeVarInt(sizeY);
        buf.writeVarInt(sizeZ);
        buf.writeBytes(grid);
    }

    public static void handle(PWRPrinterScanPacket packet, PacketContext context) {
        // Делегирование в клиентский хук через FQN: ссылка резолвится лениво,
        // только при исполнении лямбды на клиенте (см. javadoc класса).
        context.queue(() ->
                com.hbm_m.client.PwrPrinterClientHooks.openScanScreen(
                        packet.sizeX, packet.sizeY, packet.sizeZ, packet.grid));
    }
}
