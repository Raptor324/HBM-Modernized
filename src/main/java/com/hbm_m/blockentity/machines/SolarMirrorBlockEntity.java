package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntitySolarMirror}: Ziel (tX/tY/tZ) setzt das Spiegelwerkzeug. Liegt das Ziel nicht unter dem
 * Spiegel und sieht er den Himmel, heizt er den Solarkessel darunter ({@code tY - 1}) mit
 * {@code Himmelslicht - Abdunklung - 11} pro Tick. Auf dem Client meldet er sich beim Kessel fuer den Lichtstrahl an.
 */
public class SolarMirrorBlockEntity extends BaseHbmBlockEntity {

    public int tX;
    public int tY;
    public int tZ;
    public boolean isOn;

    public SolarMirrorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_MIRROR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarMirrorBlockEntity be) {
        be.updateEntity(level);
    }

    private void updateEntity(Level world) {
        int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();

        if (!world.isClientSide) {

            // Original networkPackNT(200) alle 20 Ticks
            if (world.getGameTime() % 20 == 0)
                this.sendUpdate();

            if (tY < yCoord) {
                isOn = false;
                return;
            }

            int sun = world.getBrightness(LightLayer.SKY, worldPosition) - world.getSkyDarken() - 11;

            if (sun <= 0 || !world.canSeeSky(worldPosition.above())) {
                isOn = false;
                return;
            }

            isOn = true;

            BlockEntity te = world.getBlockEntity(new BlockPos(tX, tY - 1, tZ));

            if (te instanceof MachineSolarBoilerBlockEntity boiler) {
                boiler.heat += sun;
            }
        } else {

            BlockEntity te = world.getBlockEntity(new BlockPos(tX, tY - 1, tZ));

            if (isOn && te instanceof MachineSolarBoilerBlockEntity boiler) {
                boiler.primary.add(new BlockPos(xCoord, yCoord, zCoord));
            }
            // markBlockForUpdate entfaellt: der Spiegel wird vom BER gezeichnet, nicht in den Chunk gebacken
        }
    }

    private void sendUpdate() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setTarget(int x, int y, int z) {
        tX = x;
        tY = y;
        tZ = z;
        this.setChanged();
        this.sendUpdate();
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        nbt.putInt("targetX", tX);
        nbt.putInt("targetY", tY);
        nbt.putInt("targetZ", tZ);
        // Original nur im Paket (serialize); hier laeuft das Paket ueber das NBT
        nbt.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        tX = nbt.getInt("targetX");
        tY = nbt.getInt("targetY");
        tZ = nbt.getInt("targetZ");
        isOn = nbt.getBoolean("isOn");
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) {
            int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();
            bb = new AABB(xCoord - 25, yCoord - 25, zCoord - 25, xCoord + 25, yCoord + 25, zCoord + 25);
        }
        return bb;
    }
}
