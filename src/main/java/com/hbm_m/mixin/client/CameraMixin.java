package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.hbm_m.entity.logic.EntityKa52;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;

/**
 * Zieht die Aussenkamera zurueck, solange der Spieler im Ka-52 sitzt. Mit den ueblichen vier Bloecken Abstand
 * steckt sie mitten im Rumpf.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    private static final float HELICOPTER_DISTANCE = 16.0F;

    @Shadow
    private Entity entity;

    //? if < 1.21.1 {
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"), require = 0)
    private double hbm_m$helicopterDistance(double distance) {
        return this.entity != null && this.entity.getVehicle() instanceof EntityKa52 ? HELICOPTER_DISTANCE : distance;
    }
    //?} else {
    /*@ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), require = 0)
    private float hbm_m$helicopterDistance(float distance) {
        return this.entity != null && this.entity.getVehicle() instanceof EntityKa52 ? HELICOPTER_DISTANCE : distance;
    }
    *///?}
}
