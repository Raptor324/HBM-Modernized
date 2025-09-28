package com.hbm_m.network.sounds;

// Пакет для отправки звуковых эффектов от сервера к клиенту.
// Используется для воспроизведения звуковых эффектов на клиентской стороне, инициируемых сервером (например, звуки приборов, действия игрока и т.д.).

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.config.ModClothConfig;

import java.util.function.Supplier;

public class GeigerSoundPacket {
    private final ResourceLocation soundLocation;
    private final float volume;
    private final float pitch;

    public GeigerSoundPacket(ResourceLocation soundLocation, float volume, float pitch) {
        this.soundLocation = soundLocation;
        this.volume = volume;
        this.pitch = pitch;
    }

    public static void encode(GeigerSoundPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.soundLocation);
        buf.writeFloat(msg.volume);
        buf.writeFloat(msg.pitch);
    }

    public static GeigerSoundPacket decode(FriendlyByteBuf buf) {
        return new GeigerSoundPacket(buf.readResourceLocation(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(GeigerSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Получаем SoundEvent из ResourceLocation
                SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(msg.soundLocation);
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (sound != null && mc != null && mc.level != null && mc.player != null) {
                    //MainRegistry.LOGGER.debug("GeigerSoundPacket: Attempting to play sound {} at volume {} and pitch {}", msg.soundLocation, msg.volume, msg.pitch);
                    // Воспроизводим звук на клиентской стороне
                    mc.level.playLocalSound(
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        sound,
                        SoundSource.PLAYERS,
                        msg.volume,
                        msg.pitch,
                        false // distanceDelay - false for immediate playback
                    );
                    if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.debug("GeigerSoundPacket: Sound played successfully.");
                    }
                } else {
                    if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.warn("GeigerSoundPacket: Failed to play sound. Sound: {}, MC: {}, Level: {}, Player: {}", sound != null, mc != null, mc != null ? mc.level != null : false, mc != null ? mc.player != null : false);
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}