package com.hbm_m.network.packets;

import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * S2C пакет: синхронизация dash-движения силовой брони.
 * Сервер отправляет всем клиентам вокруг, чтобы те обновили deltaMovement игрока.
 */
public class PowerArmorDashPacket implements S2CPacket {

    private final int playerId;
    private final Vec3 velocity;

    public PowerArmorDashPacket(int playerId, Vec3 velocity) {
        this.playerId = playerId;
        this.velocity = velocity;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static PowerArmorDashPacket decode(FriendlyByteBuf buf) {
        int   id  = buf.readInt();
        Vec3  vel = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new PowerArmorDashPacket(id, vel);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeDouble(velocity.x);
        buf.writeDouble(velocity.y);
        buf.writeDouble(velocity.z);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(PowerArmorDashPacket msg, PacketContext context) {
        context.queue(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            net.minecraft.world.entity.Entity entity = mc.level.getEntity(msg.playerId);
            if (entity instanceof Player player && player != mc.player) {
                // Применяем импульс только к другим игрокам (не к локальному)
                player.setDeltaMovement(msg.velocity);
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    /**
     * Отправить импульс всем игрокам в радиусе от источника.
     * Вызывается с серверной стороны.
     */
    public static void sendToAll(net.minecraft.server.MinecraftServer server,
                                 int playerId, Vec3 velocity) {
        PowerArmorDashPacket packet = new PowerArmorDashPacket(playerId, velocity);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModPacketHandler.sendToPlayer(player, ModPacketHandler.POWER_ARMOR_DASH, packet);
        }
    }
}