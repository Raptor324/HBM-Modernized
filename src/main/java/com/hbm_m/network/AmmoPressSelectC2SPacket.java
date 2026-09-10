package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Die Rezeptauswahl der Munitionspresse. Im Original der Schluessel {@code selection} im
 * {@code NBTControlPacket} - dort ein Listenindex, hier die Rezept-Kennung.
 */
public class AmmoPressSelectC2SPacket implements C2SPacket {

    private final BlockPos blockPos;
    /** Leer bedeutet: Auswahl aufheben. */
    private final String recipeId;

    private AmmoPressSelectC2SPacket(BlockPos blockPos, String recipeId) {
        this.blockPos = blockPos;
        this.recipeId = recipeId;
    }

    public static AmmoPressSelectC2SPacket decode(FriendlyByteBuf buf) {
        return new AmmoPressSelectC2SPacket(buf.readBlockPos(), buf.readUtf(256));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUtf(recipeId, 256);
    }

    public static void handle(AmmoPressSelectC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(msg.blockPos.getX() + 0.5, msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 20 * 20) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (!(be instanceof MachineAmmoPressBlockEntity press)) return;

            press.selectRecipe(msg.recipeId.isEmpty() ? null : ResourceLocation.tryParse(msg.recipeId));
        });
    }

    public static void send(BlockPos pos, ResourceLocation recipeId) {
        ModPacketHandler.sendToServer(ModPacketHandler.AMMO_PRESS_SELECT,
                new AmmoPressSelectC2SPacket(pos, recipeId == null ? "" : recipeId.toString()));
    }
}
