package com.hbm_m.blockentity.bomb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.bomb.BlockChargeBase;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityCharge}: Zuender in Ticks; scharf zaehlt er herunter, piept jede Sekunde und zuendet bei 0.
 * Zeit und Zustand gehen jeden Tick an Spieler im Umkreis 100 (fuer die Anzeige).
 */
public class ChargeBlockEntity extends BaseHbmBlockEntity {

    public boolean started;
    public int timer;

    public ChargeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGE.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, ChargeBlockEntity te) {
        if (te.started) {
            te.timer--;
            if (te.timer % 20 == 0 && te.timer > 0)
                world.playSound(null, pos, HbmSoundsNT.get("weapon.fstbmbPing"), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (te.timer <= 0 && state.getBlock() instanceof BlockChargeBase charge) {
                charge.explode(world, pos);
                return;
            }
        }
        if (world instanceof ServerLevel server) te.networkPackNT(server, 100);
    }

    private void networkPackNT(ServerLevel server, int range) {
        CompoundTag tag = new CompoundTag();
        writeNbtData(tag, null);
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this, be -> tag);
        for (var player : server.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= (double) range * range)
                player.connection.send(packet);
        }
    }

    public String getMinutes() {
        String mins = "" + (timer / 1200);
        return mins.length() == 1 ? "0" + mins : mins;
    }

    public String getSeconds() {
        String secs = "" + ((timer / 20) % 60);
        return secs.length() == 1 ? "0" + secs : secs;
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putInt("timer", timer);
        nbt.putBoolean("started", started);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        timer = nbt.getInt("timer");
        started = nbt.getBoolean("started");
    }
}
