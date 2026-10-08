package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityFoundryTank}: Speicher fuer 4 Bloecke fluessiges Material. Laeuft zuerst in einen Tank darunter,
 * sonst in einen seitlichen Abnehmer (keine Rinne), sonst gleicht er sich mit Nachbartanks aus.
 */
public class MachineFoundryTankBlockEntity extends MachineFoundryBaseBlockEntity {

    public int nextUpdate;

    public MachineFoundryTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_TANK_BE.get(), pos, state);
    }

    @Override
    public void updateEntity() {

        if (level != null && !level.isClientSide) {

            if (this.type == null && this.amount != 0) {
                this.amount = 0;
            }

            nextUpdate--;

            if (nextUpdate <= 0 && this.amount > 0 && this.type != null) {

                boolean hasOp = false;
                nextUpdate = level.random.nextInt(6) + 5;

                BlockEntity te = level.getBlockEntity(worldPosition.below());

                if (te instanceof MachineFoundryTankBlockEntity tank) {
                    if ((tank.type == null || tank.type == this.type) && tank.amount < tank.getCapacity()) {
                        tank.type = this.type;
                        int toFill = Math.min(this.amount, tank.getCapacity() - tank.amount);
                        this.amount -= toFill;
                        tank.amount += toFill;
                        hasOp = true;
                    }
                }

                List<Integer> ints = new ArrayList<>(List.of(2, 3, 4, 5));
                Collections.shuffle(ints);

                if (!hasOp) {
                    for (Integer i : ints) {
                        Direction dir = Direction.from3DDataValue(i);
                        BlockPos np = worldPosition.relative(dir);
                        if (level.getBlockEntity(np) instanceof MachineFoundryChannelBlockEntity) continue;
                        ICrucibleAcceptor acc = CrucibleUtil.acceptorAt(level, np);

                        if (acc != null) {
                            if (acc.canAcceptPartialFlow(level, np, dir.getOpposite(), new MaterialStack(this.type, this.amount))) {
                                MaterialStack left = acc.flow(level, np, dir.getOpposite(), new MaterialStack(this.type, this.amount));
                                if (left == null) {
                                    this.type = null;
                                    this.amount = 0;
                                } else {
                                    this.amount = left.amount;
                                }
                                hasOp = true;
                                break;
                            }
                        }
                    }
                }

                if (!hasOp) {
                    for (Integer i : ints) {
                        Direction dir = Direction.from3DDataValue(i);
                        BlockEntity b = level.getBlockEntity(worldPosition.relative(dir));

                        if (b instanceof MachineFoundryTankBlockEntity acc) {

                            if (acc.type == null || acc.type == this.type || acc.amount == 0) {

                                acc.type = this.type;

                                if (level.random.nextInt(5) == 0) {
                                    //1:4 chance that the fill states are simply swapped
                                    int buf = this.amount;
                                    this.amount = acc.amount;
                                    acc.amount = buf;

                                } else {
                                    int diff = this.amount - acc.amount;

                                    if (diff > 0) {
                                        diff /= 2;
                                        this.amount -= diff;
                                        acc.amount += diff;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        super.updateEntity();
    }

    /* Original FoundryTank: nur von oben befuellbar */
    @Override public boolean canAcceptPartialFlow(net.minecraft.world.level.Level world, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override @org.jetbrains.annotations.Nullable public MaterialStack flow(net.minecraft.world.level.Level world, BlockPos pos, Direction side, MaterialStack stack) { return stack; }

    @Override
    public int getCapacity() {
        return MaterialShapes.BLOCK.q(4);
    }
}
