package com.hbm_m.blockentity.decorations;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.FloodlightBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code Floodlight.TileEntityFloodlight}: verbraucht 100 HE/Tick (Puffer 5000, Anschluss nur an der
 * Befestigungsseite), wirft 15 Lichtstrahlen (Faecher 5 x 3 um den Neigungswinkel, bis 64 Bloecke) und setzt an deren
 * Ende {@code floodlight_beam}-Lichtbloecke; im Betrieb wird alle 5 Ticks ein Strahl neu berechnet. Ohne Strom 60 Ticks
 * Sperre, Lichter weg.
 */
public class FloodlightBlockEntity extends BlockEntity implements IEnergyReceiver {

    public float rotation;
    protected BlockPos[] lightPos = new BlockPos[15];
    public static final long maxPower = 5_000;
    public long power;
    public int delay;
    public boolean isOn;

    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOODLIGHT.get(), pos, state);
    }

    private int meta() {
        return getBlockState().getValue(FloodlightBlock.META);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FloodlightBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel world) {
        Direction dir = Direction.from3DDataValue(meta() % 6).getOpposite();
        BlockPos p = worldPosition.relative(dir);
        this.trySubscribe(world, p.getX(), p.getY(), p.getZ(), dir);

        if (delay > 0) {
            delay--;
            return;
        }

        if (power >= 100) {
            power -= 100;

            if (!isOn) {
                this.isOn = true;
                this.castLights();
                this.setChanged();
                world.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            } else {
                long timer = world.getGameTime();
                if (timer % 5 == 0) {
                    timer = timer / 5;
                    this.castLight((int) Math.abs(timer % this.lightPos.length));
                }
            }
        } else {
            if (isOn) {
                this.isOn = false;
                this.delay = 60;
                this.destroyLights();
                this.setChanged();
                world.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public BlockPos getLightPos(int index) {
        return lightPos[index];
    }

    private void castLight(int index) {
        BlockPos newPos = this.getRayEndpoint(index);
        BlockPos oldPos = this.lightPos[index];
        this.lightPos[index] = null;

        if (newPos == null || !newPos.equals(oldPos)) { //if the new end point is null or not equal to the previous, delete the previous spot
            if (oldPos != null) {
                BlockEntity tile = level.getBlockEntity(oldPos);
                if (tile instanceof FloodlightBeamBlockEntity beam) {
                    if (beam.cache == this) {
                        level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        if (newPos == null) return;

        if (level.getBlockState(newPos).is(Blocks.AIR)) {
            level.setBlock(newPos, ModBlocks.FLOODLIGHT_BEAM.get().defaultBlockState(), 2);
            BlockEntity tile = level.getBlockEntity(newPos);
            if (tile instanceof FloodlightBeamBlockEntity beam) beam.setSource(this, newPos, index); // Original: Strahlkoordinaten als "Quelle"
            this.lightPos[index] = newPos;
        }

        if (level.getBlockState(newPos).is(ModBlocks.FLOODLIGHT_BEAM.get())) {
            this.lightPos[index] = newPos;
        }
    }

    // 1.7-Vec3-Drehungen
    private static double[] rotZ(double[] v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new double[] { v[0] * c + v[1] * s, v[1] * c - v[0] * s, v[2] };
    }

    private static double[] rotY(double[] v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new double[] { v[0] * c + v[2] * s, v[1], v[2] * c - v[0] * s };
    }

    public BlockPos getRayEndpoint(int index) {
        if (index < 0 || index >= lightPos.length) return null;

        int meta = meta();
        double[] dir = { 1, 0, 0 };
        float[] angles = getVariation(index);

        float rotation = this.rotation;
        if (meta == 1 || meta == 7) rotation = 180 - rotation;
        if (meta == 6) rotation = 180 - rotation;

        dir = rotZ(dir, (float) (rotation / 180D * Math.PI) + angles[0]);
        if (meta == 6) dir = rotY(dir, (float) (Math.PI / 2D));
        if (meta == 7) dir = rotY(dir, (float) (Math.PI / 2D));
        if (meta == 2) dir = rotY(dir, (float) (Math.PI / 2D));
        if (meta == 3) dir = rotY(dir, (float) -(Math.PI / 2D));
        if (meta == 4) dir = rotY(dir, (float) (Math.PI));
        dir = rotY(dir, angles[1]);

        int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();
        for (int i = 1; i < 64; i++) {
            int iX = (int) Math.floor(xCoord + 0.5 + dir[0] * i);
            int iY = (int) Math.floor(yCoord + 0.5 + dir[1] * i);
            int iZ = (int) Math.floor(zCoord + 0.5 + dir[2] * i);

            if (iX == xCoord && iY == yCoord && iZ == zCoord) continue;

            BlockPos ip = new BlockPos(iX, iY, iZ);
            BlockState block = level.getBlockState(ip);
            // getLightOpacity < 127: alles, was nicht voll lichtundurchlaessig ist
            if (!block.canOcclude() || block.getLightBlock(level, ip) < 15) continue;

            int fX = (int) Math.floor(xCoord + 0.5 + dir[0] * (i - 1));
            int fY = (int) Math.floor(yCoord + 0.5 + dir[1] * (i - 1));
            int fZ = (int) Math.floor(zCoord + 0.5 + dir[2] * (i - 1));

            if (i > 1) return new BlockPos(fX, fY, fZ);
        }

        return null;
    }

    private void castLights() {
        for (int i = 0; i < this.lightPos.length; i++) this.castLight(i);
    }

    private void destroyLight(int index) {
        BlockPos pos = lightPos[index];
        if (pos != null) {
            if (level.getBlockState(pos).is(ModBlocks.FLOODLIGHT_BEAM.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    public void destroyLights() {
        for (int i = 0; i < this.lightPos.length; i++) destroyLight(i);
    }

    private float[] getVariation(int index) {
        return new float[] {
                (((index / 3) - 2) * 7.5F) / 180F * (float) Math.PI,
                (((index % 3) - 1) * 15F) / 180F * (float) Math.PI
        };
    }

    // --- NBT / Sync ---

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.rotation = nbt.getFloat("rotation");
        this.power = nbt.getLong("power");
        this.isOn = nbt.getBoolean("isOn");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putFloat("rotation", rotation);
        nbt.putLong("power", power);
        nbt.putBoolean("isOn", isOn);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, -1, -1), worldPosition.offset(2, 2, 2));
    }

    // --- Energie (IEnergyReceiverMK2) ---

    @Override public long getEnergyStored() { return power; }
    @Override public long getMaxEnergyStored() { return maxPower; }
    @Override public void setEnergyStored(long energy) { this.power = Math.max(0, Math.min(maxPower, energy)); setChanged(); }
    @Override public long getReceiveSpeed() { return maxPower; }
    @Override public IEnergyReceiver.Priority getPriority() { return IEnergyReceiver.Priority.NORMAL; }
    @Override public boolean canReceive() { return power < maxPower; }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        long received = Math.min(maxPower - power, maxReceive);
        if (!simulate && received > 0) setEnergyStored(power + received);
        return received;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side == null || side == Direction.from3DDataValue(meta() % 6).getOpposite();
    }

    //? if forge {
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER && canConnectEnergy(side))
            return net.minecraftforge.common.util.LazyOptional.of(() -> (IEnergyReceiver) this).cast();
        if (cap == com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR && canConnectEnergy(side))
            return net.minecraftforge.common.util.LazyOptional.of(() -> (com.hbm_m.interfaces.IEnergyConnector) this).cast();
        return super.getCapability(cap, side);
    }
    //?}
}
