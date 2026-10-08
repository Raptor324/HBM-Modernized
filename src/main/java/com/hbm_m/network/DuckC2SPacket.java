package com.hbm_m.network;

import com.hbm_m.config.MobConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.EntityDuck;
import com.hbm_m.platform.PlayerPersistentData;
import com.hbm_m.sound.HbmSoundsNT;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Taste O: der Spieler wirft einmalig eine Ente. Im Original der {@code AuxButtonPacket(0, 0, 0, 999, 0)}
 * ("why make new packets when you can just abuse and uglify the existing ones?") ohne TileEntity.
 */
public class DuckC2SPacket implements C2SPacket {

    public DuckC2SPacket() { }

    public static DuckC2SPacket decode(FriendlyByteBuf buf) {
        return new DuckC2SPacket();
    }

    @Override
    public void write(FriendlyByteBuf buf) { }

    public static void handle(DuckC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer p)) return;

            CompoundTag data = PlayerPersistentData.get(p);
            CompoundTag perDat = data.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);

            if (MobConfig.enableDucks && !perDat.getBoolean("hasDucked")) {
                EntityDuck ducc = ModEntities.DUCK.get().create(p.level());
                if (ducc == null) return;
                ducc.setPos(p.getX(), p.getY() + p.getEyeHeight(), p.getZ());

                Vec3 vec = p.getLookAngle();
                ducc.setDeltaMovement(vec.x, vec.y, vec.z);

                p.level().addFreshEntity(ducc);
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), HbmSoundsNT.get("hbm:entity.ducc"), SoundSource.PLAYERS, 1.0F, 1.0F);

                perDat.putBoolean("hasDucked", true);

                data.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, perDat);
            }
        });
    }

    public static void send() {
        ModPacketHandler.sendToServer(ModPacketHandler.DUCK, new DuckC2SPacket());
    }
}
