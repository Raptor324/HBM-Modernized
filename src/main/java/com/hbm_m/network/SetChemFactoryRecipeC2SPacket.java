package com.hbm_m.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Выбор рецепта для одной из 4 линий Chemical Factory (аналог
 * {@link SetChemPlantRecipeC2SPacket} с индексом линии; соответствует
 * receiveControl("index"/"selection") оригинала).
 */
public class SetChemFactoryRecipeC2SPacket implements C2SPacket {

    private final BlockPos blockPos;
    private final int lane;
    @Nullable
    private final ResourceLocation recipeId;

    public SetChemFactoryRecipeC2SPacket(BlockPos blockPos, int lane, @Nullable ResourceLocation recipeId) {
        this.blockPos = blockPos;
        this.lane = lane;
        this.recipeId = recipeId;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static SetChemFactoryRecipeC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos         blockPos  = buf.readBlockPos();
        int              lane      = buf.readVarInt();
        boolean          hasRecipe = buf.readBoolean();
        ResourceLocation recipeId  = hasRecipe ? buf.readResourceLocation() : null;
        return new SetChemFactoryRecipeC2SPacket(blockPos, lane, recipeId);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeVarInt(lane);
        buf.writeBoolean(recipeId != null);
        if (recipeId != null) buf.writeResourceLocation(recipeId);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(SetChemFactoryRecipeC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.distanceToSqr(
                    msg.blockPos.getX() + 0.5,
                    msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5) > 64.0) return;

            BlockEntity be = player.level().getBlockEntity(msg.blockPos);
            if (be instanceof MachineChemicalFactoryBlockEntity factory) {
                factory.setSelectedRecipe(msg.lane, msg.recipeId);
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos blockPos, int lane, @Nullable ResourceLocation recipeId) {
        ModPacketHandler.sendToServer(ModPacketHandler.SET_CHEM_FACTORY_RECIPE,
                new SetChemFactoryRecipeC2SPacket(blockPos, lane, recipeId));
    }
}
