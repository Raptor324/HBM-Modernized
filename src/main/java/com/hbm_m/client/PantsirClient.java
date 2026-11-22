package com.hbm_m.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.hbm_m.entity.logic.EntityPantsir;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Motorsound und Fahreranzeige des Pantsir-S1. */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class PantsirClient {

    private static final int GREEN = 0xFF4DFF7A;
    private static final int DIM = 0x9944DD66;
    private static final int RED = 0xFFFF4A3A;
    private static final Map<EntityPantsir, EngineSound> SOUNDS = new WeakHashMap<>();

    private PantsirClient() {}

    public static void onEntityTick(EntityPantsir vehicle) {
        boolean running = vehicle.isVehicle() || Math.abs(vehicle.getDriveSpeed()) > 0.02F;
        EngineSound sound = SOUNDS.get(vehicle);
        if (running && (sound == null || sound.isStopped())) {
            sound = new EngineSound(vehicle);
            SOUNDS.put(vehicle, sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
    }

    public static void renderHud(GuiGraphics g) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !(player.getVehicle() instanceof EntityPantsir vehicle)) {
            return;
        }
        Font font = minecraft.font;
        int x = 12;
        int y = g.guiHeight() - 62;
        int speed = Math.round(Math.abs(vehicle.getDriveSpeed()) * 72.0F);
        g.drawString(font, speed + " KM/H " + (vehicle.getDriveSpeed() < -0.01F ? "R" : "D"), x, y, GREEN, false);
        g.drawString(font, "57E6  " + vehicle.getAmmo() + "/" + EntityPantsir.MAX_AMMO, x, y + 11,
                vehicle.getAmmo() > 0 ? GREEN : RED, false);
        g.drawString(font, "30MM  " + vehicle.getShells(), x, y + 22, vehicle.getShells() > 0 ? GREEN : RED, false);
        g.drawString(font, "HP    " + Math.round(vehicle.getHealthFraction() * 100.0F) + "%", x, y + 33,
                vehicle.getHealthFraction() > 0.35F ? GREEN : RED, false);
        String radar = !vehicle.isActive() ? "RADAR OFF" : vehicle.isLocked() ? "RADAR LOCK" : "RADAR SCAN";
        g.drawString(font, radar, x, y + 44, !vehicle.isActive() ? DIM : vehicle.isLocked() ? RED : GREEN, false);
    }

    /** Dieselton, der dem Fahrzeug folgt und mit der Fahrt hoeher wird. */
    private static final class EngineSound extends AbstractTickableSoundInstance {

        private final EntityPantsir vehicle;

        EngineSound(EntityPantsir vehicle) {
            super(HbmSoundsNT.get("hbm:block.engine"), SoundSource.NEUTRAL, RandomSource.create());
            this.vehicle = vehicle;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.1F;
            this.attenuation = Attenuation.LINEAR;
            tick();
        }

        @Override
        public void tick() {
            boolean running = this.vehicle.isVehicle() || Math.abs(this.vehicle.getDriveSpeed()) > 0.02F;
            if (this.vehicle.isRemoved() || !running) {
                stop();
                return;
            }
            float load = Math.abs(this.vehicle.getDriveSpeed());
            this.x = this.vehicle.getX();
            this.y = this.vehicle.getY();
            this.z = this.vehicle.getZ();
            this.volume = 1.5F + load * 2.0F;
            this.pitch = 0.6F + load * 0.7F;
        }
    }
}
