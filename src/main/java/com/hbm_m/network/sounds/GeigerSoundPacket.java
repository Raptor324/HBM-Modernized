package com.hbm_m.network.sounds;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class GeigerSoundPacket implements S2CPacket {

    private final ResourceLocation soundLocation;
    private final float volume;
    private final float pitch;

    public GeigerSoundPacket(ResourceLocation soundLocation, float volume, float pitch) {
        this.soundLocation = soundLocation;
        this.volume        = volume;
        this.pitch         = pitch;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static GeigerSoundPacket decode(FriendlyByteBuf buf) {
        return new GeigerSoundPacket(
                buf.readResourceLocation(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(soundLocation);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(GeigerSoundPacket msg, PacketContext context) {
        // context.queue() выполняет на главном потоке клиента
        context.queue(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.warn(
                            "GeigerSoundPacket: cannot play — mc={}, level={}, player={}",
                            mc != null, mc != null && mc.level != null,
                            mc != null && mc.player != null);
                }
                return;
            }

            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(msg.soundLocation);
            if (sound == null) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.warn("GeigerSoundPacket: unknown sound {}", msg.soundLocation);
                }
                return;
            }

            mc.level.playLocalSound(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    sound, SoundSource.PLAYERS,
                    msg.volume, msg.pitch,
                    false
            );

            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("GeigerSoundPacket: played {}", msg.soundLocation);
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendTo(ServerPlayer player,
                              ResourceLocation sound, float volume, float pitch) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.GEIGER_SOUND,
                new GeigerSoundPacket(sound, volume, pitch));
    }
}