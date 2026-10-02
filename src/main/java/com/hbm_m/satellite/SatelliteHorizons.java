package com.hbm_m.satellite;

import java.util.Locale;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.TomEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 1:1 {@code SatelliteHorizons} ({@code sat_gerald}): einmaliger Orbitalschlag - am Ziel faellt aus Y 600 ein
 * {@link TomEntity}. Wie im Original speichert er nur "used" (kein super.writeToNBT).
 */
public class SatelliteHorizons extends Satellite {

    public static final String CMD_FIRE = "fire";
    public static final String CMD_CANFIRE = "settarget";

    boolean used = false;

    public SatelliteHorizons() { }

    @Override public String getType() { return "PAYLOAD_UNKNOWN"; }

    @Override
    public void onOrbit(ServerLevel world, double x, double y, double z) {
        super.onOrbit(world, x, y, z);
        com.hbm_m.advancement.ModAdvancements.grantAll(world, com.hbm_m.advancement.ModAdvancements.HORIZONS_START);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("used", used);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        used = nbt.getBoolean("used");
    }

    @Override
    public void onCommandImpl(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_FIRE)) {
            theHorizons(world, targetX, targetZ);
            return;
        }

        if (cmd[0].equals(CMD_CANFIRE)) {
            this.tx = (!used) + "";
            this.tx = this.tx.toUpperCase(Locale.US);
        }
    }

    @Override
    public void onCoordAction(ServerLevel world, Player player, int x, int y, int z) {
        this.setTarget(x, z);
        this.theHorizons(world, x, z);
    }

    public void theHorizons(ServerLevel world, int x, int z) {
        if (used) return;
        used = true;
        SatelliteManager.get(world).setDirty();

        TomEntity tom = ModEntities.TOM_METEOR.get().create(world);
        if (tom == null) return;
        tom.setPos(x + 0.5, 600, z + 0.5);

        world.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,
                new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4), 2, net.minecraft.world.level.ChunkPos.ZERO);

        world.addFreshEntity(tom);
        com.hbm_m.advancement.ModAdvancements.grantAll(world, com.hbm_m.advancement.ModAdvancements.HORIZONS_END);

        world.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Horizons has been activated.").withStyle(ChatFormatting.RED), false);
    }
}
