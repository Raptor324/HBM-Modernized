package com.hbm_m.network;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 1:1-Gegenstueck zu {@code ExtPropPacket} (1.7.10): schickt dem Spieler jeden Tick seine
 * {@link HbmLivingProps} und {@link HbmPlayerProps}, damit Schild-/Dash-Anzeige, Kontamination,
 * Digamma usw. clientseitig stimmen. Uebertragen wird der NBT-Zweig statt des ByteBufs.
 * Dazu kommen die fluechtigen Dash-Werte, die im Original ueber {@code PermaSyncPacket} laufen.
 */
public class ExtPropPacket implements S2CPacket {

    private final CompoundTag living;
    private final CompoundTag player;
    private final int stamina;
    private final int dashCount;

    public ExtPropPacket(CompoundTag living, CompoundTag player, int stamina, int dashCount) {
        this.living = living;
        this.player = player;
        this.stamina = stamina;
        this.dashCount = dashCount;
    }

    public static ExtPropPacket decode(FriendlyByteBuf buf) {
        return new ExtPropPacket(buf.readNbt(), buf.readNbt(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(living);
        buf.writeNbt(player);
        buf.writeVarInt(stamina);
        buf.writeVarInt(dashCount);
    }

    public static void handle(ExtPropPacket msg, PacketContext context) {
        context.queue(() -> {
            Player p = com.hbm_m.client.ClientAccess.player();
            if (p == null) return;
            if (msg.living != null) p.getPersistentData().put(HbmLivingProps.KEY, msg.living);
            HbmPlayerProps props = HbmPlayerProps.get(p);
            if (msg.player != null) props.clientRead(msg.player);
            props.setStamina(msg.stamina);
            props.setDashCount(msg.dashCount);
        });
    }

    public static void sendTo(ServerPlayer player) {
        HbmPlayerProps props = HbmPlayerProps.get(player);
        CompoundTag living = player.getPersistentData().getCompound(HbmLivingProps.KEY);
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.EXT_PROP,
                new ExtPropPacket(living.copy(), props.write(), props.getStamina(), props.getDashCount()));
    }
}
