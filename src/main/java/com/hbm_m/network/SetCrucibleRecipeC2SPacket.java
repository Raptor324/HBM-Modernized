package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Выбор рецепта тигля из GUI — порт {@code NBTControlPacket("index", "selection")}
 * → {@code TileEntityCrucible.receiveControl}: сервер ставит {@code recipe}.
 */
public class SetCrucibleRecipeC2SPacket implements C2SPacket {

    private final BlockPos pos;
    private final String recipeId;

    public SetCrucibleRecipeC2SPacket(BlockPos pos, String recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static SetCrucibleRecipeC2SPacket decode(FriendlyByteBuf buf) {
        return new SetCrucibleRecipeC2SPacket(buf.readBlockPos(), buf.readUtf(256));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(recipeId, 256);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(SetCrucibleRecipeC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);

            if (be instanceof MachineCrucibleBlockEntity crucible) {
                crucible.setSelectedRecipe(msg.recipeId);
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos pos, String recipeId) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_CRUCIBLE_RECIPE,
                new SetCrucibleRecipeC2SPacket(pos, recipeId));
    }
}
