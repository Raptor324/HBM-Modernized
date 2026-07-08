package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MachineFoundryChannelBlockEntity extends BlockEntity {

    public static final int CAPACITY = MaterialStack.MB_PER_INGOT * 2;

    @Nullable public MaterialType type   = null;
    public int   amount   = 0;
    private int  lastFlow = 0;
    private int  nextUpdate = 5;

    private static final Direction[] H_DIRS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public MachineFoundryChannelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_CHANNEL_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFoundryChannelBlockEntity be) {
        if (level.isClientSide) return;

        if (be.type != null && be.amount == 0) be.type = null;
        if (be.type == null && be.amount != 0) be.amount = 0;

        be.nextUpdate--;
        if (be.nextUpdate > 0 || be.amount <= 0 || be.type == null) return;

        be.nextUpdate = 5;
        boolean acted = false;

        List<Direction> dirs = new ArrayList<>(List.of(H_DIRS));
        Collections.shuffle(dirs);
        if (be.lastFlow != 0) {
            Direction preferred = Direction.from3DDataValue(be.lastFlow);
            dirs.remove(preferred);
            dirs.add(preferred);
        }

        for (Direction dir : dirs) {
            BlockPos neighbor = pos.relative(dir);
            BlockEntity te = level.getBlockEntity(neighbor);
            int poured = 0;

            if (te instanceof MachineFoundryBasinBlockEntity basin) {
                poured = basin.receiveMaterial(be.type, be.amount);
            } else if (te instanceof MachineFoundryOutletBlockEntity outlet) {
                poured = outlet.receiveMaterial(level, neighbor, level.getBlockState(neighbor), dir.getOpposite(), be.type, be.amount);
            }

            if (poured > 0) {
                be.amount -= poured;
                if (be.amount <= 0) { be.amount = 0; be.type = null; }
                be.lastFlow = dir.get3DDataValue();
                acted = true;
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                break;
            }
        }

        if (!acted) {
            for (Direction dir : dirs) {
                BlockPos neighbor = pos.relative(dir);
                BlockEntity te = level.getBlockEntity(neighbor);

                if (te instanceof MachineFoundryChannelBlockEntity other) {
                    if (other.type == null || other.type == be.type) {
                        other.type = be.type;

                        if (be.amount > 1 && level.getRandom().nextInt(5) == 0) {
                            int buf = be.amount;
                            be.amount = other.amount;
                            other.amount = buf;
                        } else {
                            int diff = be.amount - other.amount;
                            if (diff > 1) {
                                diff /= 2;
                                be.amount -= diff;
                                other.amount += diff;
                            }
                        }

                        if (be.amount <= 0) { be.amount = 0; be.type = null; }
                        if (other.amount <= 0) { other.amount = 0; other.type = null; }

                        be.setChanged();
                        other.setChanged();
                        break;
                    }
                }
            }
        }

        if (be.amount == 0) { be.lastFlow = 0; be.nextUpdate = 5; }
    }

    public int receiveMaterial(MaterialType inType, int inAmount) {
        if (inAmount <= 0) return 0;
        if (type != null && type != inType) return 0;
        int space = CAPACITY - amount;
        if (space <= 0) return 0;
        int filled = Math.min(inAmount, space);
        type   = inType;
        amount += filled;
        setChanged();
        return filled;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
        tag.putInt("lastFlow", lastFlow);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("mat_type")) type = MaterialType.byName(tag.getString("mat_type"));
        amount   = tag.getInt("mat_amount");
        lastFlow = tag.getInt("lastFlow");
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
