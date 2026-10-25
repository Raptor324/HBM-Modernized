package com.hbm_m.satellite;

import java.util.Locale;

import com.hbm_m.entity.logic.EntityDeathBlast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/** 1:1 {@code SatelliteDeathRay} (Orbitaler Todesstrahl): alle 5 Minuten ein {@link EntityDeathBlast}. */
public class SatelliteDeathRay extends Satellite {

    public static final String CMD_FIRE = "fire";
    public static final String CMD_CANFIRE = "canfire";

    public static final int CHARGE_TIME = 5 * 60 * 20;
    public long lastShot;

    public SatelliteDeathRay() { }

    @Override public String getType() { return "ORBITAL_FUN_PLATFORM_:)"; }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("lastShot", lastShot);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lastShot = nbt.getLong("lastShot");
    }

    @Override
    public void onCommandImpl(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_FIRE)) {
            deathBlast(world, targetX, targetZ);
            return;
        }

        if (cmd[0].equals(CMD_CANFIRE)) {
            this.tx = (lastShot + CHARGE_TIME < world.getGameTime()) + "";
            this.tx = this.tx.toUpperCase(Locale.US);
        }
    }

    @Override
    public void onCoordAction(ServerLevel world, Player player, int x, int y, int z) {
        this.setTarget(x, z);
        this.deathBlast(world, targetX, targetZ);
    }

    public void deathBlast(ServerLevel world, int x, int z) {
        if (lastShot + CHARGE_TIME < world.getGameTime()) {
            lastShot = world.getGameTime();
            SatelliteManager.get(world).setDirty();
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            EntityDeathBlast blast = new EntityDeathBlast(world);
            blast.setPos(x, y, z);
            world.addFreshEntity(blast);
        }
    }
}
