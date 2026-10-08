package com.hbm_m.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.hbm_m.effect.ModEffects;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Original {@code PermaSyncHandler}, Abschnitt "SHITTY MEMES": Entity-IDs aller Spieler der Welt mit dem
 * Death-Effekt ({@code HbmPotion.death}). Clientseitig landen sie in {@link #boykissers}; diese Spieler zeichnet
 * {@code ModelManLayer} als {@code ModelMan}.
 */
public class PermaSyncMemePacket implements S2CPacket {

    /** Original {@code PermaSyncHandler.boykissers} (nur clientseitig befuellt). */
    public static final HashSet<Integer> boykissers = new HashSet<>();

    private final List<Integer> ids;

    public PermaSyncMemePacket(List<Integer> ids) {
        this.ids = ids;
    }

    public static PermaSyncMemePacket decode(FriendlyByteBuf buf) {
        int count = buf.readShort();
        List<Integer> ids = new ArrayList<>(Math.max(count, 0));
        for (int i = 0; i < count; i++) ids.add(buf.readInt());
        return new PermaSyncMemePacket(ids);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeShort((short) ids.size());
        for (Integer i : ids) buf.writeInt(i);
    }

    public static void handle(PermaSyncMemePacket msg, PacketContext context) {
        context.queue(() -> {
            boykissers.clear();
            boykissers.addAll(msg.ids);
        });
    }

    /** Original {@code writePacket}: alle Spieler der Welt des Empfaengers mit aktivem Death-Effekt. */
    public static void sendTo(ServerPlayer player) {
        List<Integer> ids = new ArrayList<>();
        for (Player p : player.serverLevel().players()) {
            if (p.hasEffect(ModEffects.DEATH.get())) {
                ids.add(p.getId());
            }
        }
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.PERMA_SYNC_MEME, new PermaSyncMemePacket(ids));
    }
}
