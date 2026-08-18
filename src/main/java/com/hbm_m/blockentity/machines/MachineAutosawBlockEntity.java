package com.hbm_m.blockentity.machines;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Autosaw: Direktport der Kernidee aus {@code TileEntityMachineAutosaw} (1.7.10 Original) - ein
 * rotierender Saegearm, der Baeume im Umkreis erkennt, faellt und Setzlinge nachpflanzt.
 * <p>
 * Grosse Vereinfachung: das Original nutzt einen aufwaendigen 601-Zeilen-Algorithmus (18-Wege-
 * BFS mit Mehr-Baum-Trunk-Zuordnung ueber ein grosses Arbeitsgebiet, praezise Arm-Rotations-
 * mathematik fuer die Trefferzone). Dieser Port behaelt die Kernmechanik (rotierender Arm sucht im
 * Ring 2-9 Bloecke Radius nach Holz/Blaettern, faellt bei Treffer den zusammenhaengenden Baum per
 * Flutfuellung, pflanzt am Stammfuss neu), vereinfacht aber die Flutfuellung auf eine einzelne
 * Baum-Komponente statt gleichzeitiger Mehrbaum-Verarbeitung im gesamten Arbeitsbereich, und nutzt
 * {@code Level.destroyBlock(pos, true)} fuer Abbau+Drop in einem Schritt statt manuellem
 * Drop-Handling. Kein Audio-Loop-System (dieser Port hat keine {@code AudioWrapper}-Entsprechung).
 */
public class MachineAutosawBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IFluidStandardReceiverMK2 {

    private static final int MIN_DIST = 2;
    private static final int MAX_DIST = 9;
    private static final int MAX_FELL_BLOCKS = 256;

    private final FluidTank tank = new FluidTank(100);

    public boolean isOn = false;
    public float rotationYaw = 0f;
    public float rotationPitch = 0f;
    private int state = 0; // 0=searching, 1=extending, 2=retracting
    private int forceSkip = 0;

    public MachineAutosawBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOSAW_BE.get(), pos, state);
    }

    public FluidTank getTank() { return tank; }

    public static void tick(Level level, BlockPos pos, BlockState blockState, MachineAutosawBlockEntity be) {
        if (level.isClientSide()) return;

        if (level.getGameTime() % 20 == 0) {
            if (be.tank.getFill() > 0) {
                be.tank.drainMb(1);
                be.isOn = true;
            } else {
                be.isOn = false;
            }
            for (Direction dir : Direction.values()) {
                if (dir != Direction.UP) be.trySubscribe(be.tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        if (!be.isOn) return;

        // Entity damage AoE roughly under the arm tip (simplified radius around the block, not the
        // original's precise rotating-arm-tip vector math).
        AABB aabb = new AABB(pos).inflate(1.5, 0.5, 1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (e.isAlive() && e.hurt(level.damageSources().generic(), 100)) {
                level.playSound(null, e.getX(), e.getY(), e.getZ(), net.minecraft.sounds.SoundEvents.WOOD_BREAK,
                        net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.95F + level.getRandom().nextFloat() * 0.2F);
            }
        }

        if (be.state == 0) {
            be.rotationYaw = (be.rotationYaw + 3f) % 360f;

            if (be.forceSkip > 0) {
                be.forceSkip--;
            } else {
                double rotRad = Math.toRadians((be.rotationYaw + 270) % 360);
                final double CUT_ANGLE = Math.toRadians(8);

                outer:
                for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
                    for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                        int sqrDst = dx * dx + dz * dz;
                        if (sqrDst <= MIN_DIST * MIN_DIST || sqrDst > MAX_DIST * MAX_DIST) continue;

                        double angle = Math.atan2(dz, dx);
                        double relAngle = Math.abs(angle - rotRad);
                        relAngle = Math.abs((relAngle + Math.PI) % (2 * Math.PI) - Math.PI);
                        if (relAngle > CUT_ANGLE) continue;

                        BlockPos target = pos.offset(dx, 1, dz);
                        BlockState targetState = level.getBlockState(target);
                        if (targetState.is(BlockTags.LOGS) || targetState.is(BlockTags.LEAVES) || targetState.is(BlockTags.SAPLINGS)) {
                            if (level instanceof ServerLevel serverLevel) {
                                be.fell(serverLevel, target);
                            }
                            be.state = 1;
                            break outer;
                        }
                    }
                }
            }
        }

        if (be.state == 1) {
            be.rotationPitch += 4;
            if (be.rotationPitch > 80) {
                be.rotationPitch = 80;
                be.state = 2;
            }
        } else if (be.state == 2) {
            be.rotationPitch -= 4;
            if (be.rotationPitch <= 0) {
                be.rotationPitch = 0;
                be.state = 0;
            }
        }

        be.setChanged();
        level.sendBlockUpdated(pos, blockState, blockState, 3);
    }

    /** Flood-fills the connected log/leaf cluster from the hit position and harvests it, replanting
     *  a sapling at the trunk base. Simplified single-tree version of the original's area-wide BFS. */
    private void fell(ServerLevel level, BlockPos hit) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(hit);
        visited.add(hit);

        BlockPos.MutableBlockPos lowestLog = hit.mutable();
        boolean foundLog = level.getBlockState(hit).is(BlockTags.LOGS);
        net.minecraft.world.item.Item saplingItem = null;

        while (!queue.isEmpty() && visited.size() < MAX_FELL_BLOCKS) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);

            if (state.is(BlockTags.LOGS) && current.getY() <= lowestLog.getY()) {
                lowestLog = current.immutable().mutable();
                foundLog = true;
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (visited.contains(next)) continue;
                if (next.distSqr(hit) > (MAX_DIST + 6) * (MAX_DIST + 6)) continue;

                BlockState nextState = level.getBlockState(next);
                if (nextState.is(BlockTags.LOGS) || nextState.is(BlockTags.LEAVES)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        for (BlockPos p : visited) {
            level.destroyBlock(p, true);
        }

        if (foundLog) {
            BlockPos base = lowestLog.below();
            BlockState baseState = level.getBlockState(base);
            if (baseState.isAir() && level.getBlockState(base.below()).isSolid()) {
                level.setBlockAndUpdate(base, net.minecraft.world.level.block.Blocks.OAK_SAPLING.defaultBlockState());
            }
        }
    }

    // ==================== IFluidUserMK2 ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != Direction.UP && (
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.WOODOIL.getSource()) ||
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.ETHANOL.getSource()) ||
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.FISHOIL.getSource()) ||
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.HEAVYOIL.getSource()) ||
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.COALCREOSOTE.getSource()));
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putBoolean("isOn", isOn);
        tag.putFloat("yaw", rotationYaw);
        tag.putFloat("pitch", rotationPitch);
        tag.putInt("state", state);
        tag.putInt("skip", forceSkip);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        isOn = tag.getBoolean("isOn");
        rotationYaw = tag.getFloat("yaw");
        rotationPitch = tag.getFloat("pitch");
        state = tag.getInt("state");
        forceSkip = tag.getInt("skip");
        tank.readFromNBT(tag, "tank");
    }
}
