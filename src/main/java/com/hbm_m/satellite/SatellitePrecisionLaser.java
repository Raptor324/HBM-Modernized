package com.hbm_m.satellite;

import java.util.Locale;

import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.entity.logic.EntityOrbitalLaser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/** 1:1 {@code SatellitePrecisionLaser} (Orbitaler Praezisionslaser): alle 5 Sekunden ein {@link EntityOrbitalLaser}. */
public class SatellitePrecisionLaser extends Satellite {

    public static final String CMD_FIRE = "fire";
    public static final String CMD_CANFIRE = "canfire";
    public static final String CMD_SETENTITYTARGET = "setentitytarget";

    public static final int MAX_TARGET_RANGE = 1_000;
    public static final int CHARGE_TIME = 5 * 20;
    public long lastShot;
    public int targetedEntity = -1;

    public SatellitePrecisionLaser() { }

    @Override public String getType() { return "ORBITAL_TATOO_REMOVER"; }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("lastShot", lastShot);
        nbt.putInt("targetedEntity", targetedEntity);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lastShot = nbt.getLong("lastShot");
        targetedEntity = nbt.getInt("targetedEntity");
    }

    @Override
    public void onCommandImpl(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_FIRE)) {

            if (this.targetedEntity != -1) {
                Entity e = world.getEntity(this.targetedEntity);
                this.targetedEntity = -1;
                if (e == null || e.isRemoved()) return;

                int x = (int) Math.floor(e.getX());
                int z = (int) Math.floor(e.getZ());
                double dX = x - targetX;
                double dZ = z - targetZ;
                if (dX * dX + dZ * dZ <= MAX_TARGET_RANGE * MAX_TARGET_RANGE) {
                    this.deathBlast(world, e.getX(), e.getY(), e.getZ());
                    return;
                }
            }

            deathBlast(world, targetX, targetZ);
            return;
        }

        if (cmd[0].equals(CMD_CANFIRE)) {
            this.tx = (lastShot + CHARGE_TIME < world.getGameTime()) + "";
            this.tx = this.tx.toUpperCase(Locale.US);
            return;
        }

        if (cmd[0].equals(CMD_SETENTITYTARGET)) {
            this.targetedEntity = IRORInteractive.parseInt(cmd[1]);
        }
    }

    @Override
    public void onCoordAction(ServerLevel world, Player player, int x, int y, int z) {
        this.setTarget(x, z);
        this.deathBlast(world, targetX, targetZ);
    }

    public void deathBlast(ServerLevel world, int x, int z) {
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        deathBlast(world, x + 0.5, y, z + 0.5);
    }

    public void deathBlast(ServerLevel world, double x, double y, double z) {
        if (lastShot + CHARGE_TIME < world.getGameTime()) {
            lastShot = world.getGameTime();
            SatelliteManager.get(world).setDirty();
            EntityOrbitalLaser blast = new EntityOrbitalLaser(world);
            blast.setPos(x, y, z);
            blast.explode();
            world.addFreshEntity(blast);
        }
    }
}
