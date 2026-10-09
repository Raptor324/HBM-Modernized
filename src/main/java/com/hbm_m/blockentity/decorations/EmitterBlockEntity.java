package com.hbm_m.blockentity.decorations;

import java.awt.Color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.decorations.EmitterBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code BlockEmitter.TileEntityEmitter}: misst jede Sekunde die Strahllaenge bis zum ersten festen Block
 * (max. 100), Effekt 4 schickt alle 5 Ticks eine Plasmawelle den Strahl entlang. Zustand geht jeden Tick an Spieler
 * im Umkreis 150 ({@code networkPackNT(150)}).
 */
public class EmitterBlockEntity extends BaseHbmBlockEntity {

    public static final int RANGE = 100;
    public static final int EFFECT_COUNT = 5;

    public int color;
    public int beam;
    public float girth = 0.5F;
    public int effect = 0;

    public EmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECO_EMITTER.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(EmitterBlock.FACING);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, EmitterBlockEntity te) {
        Direction dir = state.getValue(EmitterBlock.FACING);
        long time = world.getGameTime();

        if (time % 20 == 0) {
            for (int i = 1; i <= RANGE; i++) {
                te.beam = i;
                BlockPos p = pos.relative(dir, i);
                if (world.getBlockState(p).isSolid()) break;
            }
        }

        if (te.effect == 4 && te.beam > 0 && time % 5 == 0 && world instanceof ServerLevel server) {
            long step = time / 5L;
            double x = (int) (pos.getX() + dir.getStepX() * step % te.beam) + 0.5;
            double y = (int) (pos.getY() + dir.getStepY() * step % te.beam) + 0.5;
            double z = (int) (pos.getZ() + dir.getStepZ() * step % te.beam) + 0.5;

            int c = te.color == 0 ? Color.HSBtoRGB(time / 50.0F, 0.5F, 0.25F) & 16777215 : te.color;

            CompoundTag data = new CompoundTag();
            data.putString("type", "plasmablast");
            data.putFloat("r", ((float) ((c & 0xff0000) >> 16)) / 256F);
            data.putFloat("g", ((float) ((c & 0x00ff00) >> 8)) / 256F);
            data.putFloat("b", ((float) (c & 0x0000ff)) / 256F);
            data.putFloat("scale", te.girth * 5);
            // Original fragt die Metadaten 2-5 ab (Nord, Sued, West, Ost)
            switch (dir) {
                case NORTH -> data.putFloat("pitch", 90);
                case SOUTH -> data.putFloat("pitch", -90);
                case WEST -> { data.putFloat("pitch", 90); data.putFloat("yaw", 90); }
                case EAST -> { data.putFloat("pitch", -90); data.putFloat("yaw", 90); }
                default -> { }
            }
            com.hbm_m.particle.helper.IParticleCreator.sendPacket(server, x, y, z, 100, data);
        }

        if (world instanceof ServerLevel server) te.networkPackNT(server, 150);
    }

    private void networkPackNT(ServerLevel server, int range) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("beam", beam);
        writeNbtData(tag, null);
        ClientboundBlockEntityDataPacket packet = com.hbm_m.platform.BlockHooks.dataPacket(this, tag);
        for (var player : server.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= (double) range * range)
                player.connection.send(packet);
        }
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        if (tag.contains("beam")) this.beam = tag.getInt("beam");
        readNbtData(tag, null);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        this.color = nbt.getInt("color");
        this.girth = nbt.getFloat("girth");
        this.effect = nbt.getInt("effect");
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putInt("color", this.color);
        nbt.putFloat("girth", this.girth);
        nbt.putInt("effect", this.effect);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return AABB.ofSize(worldPosition.getCenter(), 2 * RANGE + 2, 2 * RANGE + 2, 2 * RANGE + 2);
    }
}
