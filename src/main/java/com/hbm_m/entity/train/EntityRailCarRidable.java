package com.hbm_m.entity.train;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.util.Vec3NT;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code EntityRailCarRidable}: Waggon mit Fahrersitz und dynamischen Passagiersitzen ({@link SeatDummyEntity}). */
public abstract class EntityRailCarRidable extends EntityRailCarCargo {

    public double engineSpeed;
    public SeatDummyEntity[] passengerSeats;

    public EntityRailCarRidable(EntityType<?> type, Level world) {
        super(type, world);
        this.passengerSeats = new SeatDummyEntity[this.getPassengerSeats().length];
    }

    /** Lineare Beschleunigung pro Tick bei Motorbetrieb */
    public abstract double getPoweredAcceleration();
    /** Faktor auf die Geschwindigkeit ohne Fahrer oder bei angezogener Feststellbremse */
    public abstract double getPassivBrake();
    /** Feststellbremse; ohne Spieler implizit AN */
    public abstract boolean shouldUseEngineBrake(Player player);
    /** Hoechstgeschwindigkeit des Motors in beide Richtungen */
    public abstract double getMaxPoweredSpeed();
    /** Ob der Motor laeuft */
    public abstract boolean canAccelerate();
    /** Jeden Tick bei erfolgreicher Beschleunigung */
    public void consumeFuel() { }

    /** Zusatz zur Motorgeschwindigkeit durch Steigungen */
    public double getGravitySpeed() {
        return 0D;
    }

    @Override
    public double getCurrentSpeed() { // in dieser Form nur einmal pro Tick aufrufen

        if (this.getFirstPassenger() instanceof Player player) {

            if (this.canAccelerate()) {
                if (player.zza > 0) {
                    engineSpeed += this.getPoweredAcceleration();
                    this.consumeFuel();
                } else if (player.zza < 0) {
                    engineSpeed -= this.getPoweredAcceleration();
                    this.consumeFuel();
                } else {
                    if (this.shouldUseEngineBrake(player)) {
                        engineSpeed *= this.getPassivBrake();
                    } else {
                        this.consumeFuel();
                    }
                }
            } else {
                engineSpeed *= this.getPassivBrake();
            }

        } else {
            engineSpeed *= this.getPassivBrake();
        }

        double maxSpeed = this.getMaxPoweredSpeed();
        engineSpeed = Mth.clamp(engineSpeed, -maxSpeed, maxSpeed);

        return engineSpeed + this.getGravitySpeed();
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        return interactFirst(player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public boolean interactFirst(Player player, InteractionHand hand) {

        if (super.interactFirst(player, hand)) return true;
        if (level().isClientSide) return true;

        int nearestSeat = this.getNearestSeat(player);

        if (nearestSeat == -1) {
            player.startRiding(this);
        } else if (nearestSeat >= 0) {
            SeatDummyEntity dummySeat = new SeatDummyEntity(level(), this, nearestSeat);
            Vec3NT passengerSeat = this.getPassengerSeats()[nearestSeat];
            passengerSeat.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
            double x = renderX + passengerSeat.xCoord;
            double y = renderY + passengerSeat.yCoord;
            double z = renderZ + passengerSeat.zCoord;
            dummySeat.setPos(x, y - 1, z);
            passengerSeats[nearestSeat] = dummySeat;
            level().addFreshEntity(dummySeat);
            player.startRiding(dummySeat);
        }

        return true;
    }

    public int getNearestSeat(Player player) {

        double nearestDist = Double.POSITIVE_INFINITY;
        int nearestSeat = -3;

        Vec3NT[] seats = getPassengerSeats();
        Vec3 view = player.getViewVector(2F);
        Vec3NT look = Vec3NT.createVectorHelper(view.x, view.y, view.z); // getLook(2): Einheitsvektor
        look.xCoord += player.getX();
        look.yCoord += player.getEyeY();
        look.zCoord += player.getZ();

        for (int i = 0; i < seats.length; i++) {

            Vec3NT seat = seats[i];
            if (seat == null) continue;
            if (passengerSeats[i] != null) continue;

            seat.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
            double x = renderX + seat.xCoord;
            double y = renderY + seat.yCoord;
            double z = renderZ + seat.zCoord;

            Vec3NT delta = Vec3NT.createVectorHelper(look.xCoord - x, look.yCoord - y, look.zCoord - z);
            double dist = delta.lengthVector();

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = i;
            }
        }

        if (this.getFirstPassenger() == null) {
            Vec3NT seat = getRiderSeatPosition();
            seat.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
            double x = renderX + seat.xCoord;
            double y = renderY + seat.yCoord;
            double z = renderZ + seat.zCoord;

            Vec3NT delta = Vec3NT.createVectorHelper(look.xCoord - x, look.yCoord - y, look.zCoord - z);
            double dist = delta.lengthVector();

            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = -1;
            }
        }

        if (nearestDist > 180) return -2;

        return nearestSeat;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {

            Vec3NT[] seats = this.getPassengerSeats();
            for (int i = 0; i < passengerSeats.length; i++) {
                SeatDummyEntity seat = passengerSeats[i];

                if (seat != null) {
                    if (seat.getFirstPassenger() == null) {
                        passengerSeats[i] = null;
                        seat.discard();
                    } else {
                        Vec3NT rot = seats[i];
                        rot.rotateAroundX((float) (this.getXRot() * Math.PI / 180));
                        rot.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
                        double x = renderX + rot.xCoord;
                        double y = renderY + rot.yCoord;
                        double z = renderZ + rot.zCoord;
                        seat.setPos(x, y - 1, z);
                    }
                }
            }
        }
    }

    /** Original {@code updateRiderPosition}. */
    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move) {

        Vec3NT offset = getRiderSeatPosition();
        offset.rotateAroundX((float) (this.getXRot() * Math.PI / 180));
        offset.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));

        move.accept(passenger, this.renderX + offset.xCoord, this.renderY + offset.yCoord - SeatDummyEntity.riderEyeOffset(passenger), this.renderZ + offset.zCoord);
    }

    /** Relative Position des Fahrers zum Kern */
    public abstract Vec3NT getRiderSeatPosition();

    public abstract Vec3NT[] getPassengerSeats();
}
