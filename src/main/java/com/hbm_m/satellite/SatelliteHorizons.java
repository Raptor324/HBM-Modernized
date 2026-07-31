package com.hbm_m.satellite;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.TomEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * "Gerald the Construction Android" ({@code sat_gerald}). Despite the name, the legacy
 * behavior (class {@code SatelliteHorizons}) has nothing to do with construction: it's a
 * one-time orbital strike - the matching-frequency designator drops a meteor from Y=600 that
 * detonates into a 600-block-radius explosion. Port of legacy
 * {@code com.hbm.saveddata.satellites.SatelliteHorizons}.
 */
public class SatelliteHorizons extends Satellite {

    private boolean used = false;

    public SatelliteHorizons() {
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
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        if (used) {
            return;
        }
        used = true;
        SatelliteManager.get(level).setDirty();

        TomEntity tom = ModEntities.TOM_METEOR.get().create(level);
        if (tom == null) {
            return;
        }
        tom.setPos(x + 0.5, 600, z + 0.5);
        level.getChunkSource().addRegionTicket(
                net.minecraft.server.level.TicketType.FORCED,
                new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4), 2,
                net.minecraft.world.level.ChunkPos.ZERO);
        level.addFreshEntity(tom);

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Horizons has been activated.").withStyle(ChatFormatting.RED), false);
    }
}
