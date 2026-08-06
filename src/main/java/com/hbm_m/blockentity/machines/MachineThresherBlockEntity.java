package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * Thresher: Direktport der Kernidee aus {@code TileEntityMachineThresher} (1.7.10 Original) - ein
 * schwenkender Erntearm, der reife Feldfruechte, Zuckerrohr/Kakteen im Bereich vor der Maschine
 * automatisch erntet und neu pflanzt.
 * <p>
 * Vereinfachung: das Original behandelt zusaetzlich Sonnenblumen/hohes Gras (Zufalls-Drops),
 * NTM-eigene {@code BlockTallPlant}s (Hanf o.ae., in diesem Port nicht vorhanden) und einen
 * Sonderfall-Item-Drop beim Toeten von Monstern ({@code nitra_small}, existiert hier nicht) - alle
 * drei entfallen. Kernmechanik (Standard-{@code CropBlock}-Reife-Erkennung + Erneut-Pflanzen,
 * Zuckerrohr/Kaktus-Saeulen-Schnitt, Entitaeten-Schaden-AoE, Fluid-Tank-Betrieb) ist 1:1 erhalten.
 */
public class MachineThresherBlockEntity extends BlockEntity implements IFluidStandardReceiverMK2 {

    private final FluidTank tank = new FluidTank(100);

    public boolean isOn = false;
    public float angle = 0f;
    private int state = 0; // 0=waiting, 1=extending, 2=retracting
    private int delay = 0;

    public MachineThresherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THRESHER_BE.get(), pos, state);
    }

    public FluidTank getTank() { return tank; }

    public static void tick(Level level, BlockPos pos, BlockState blockState, MachineThresherBlockEntity be) {
        if (level.isClientSide()) return;

        Direction facing = blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        Direction rot = facing.getClockWise();

        if (level.getGameTime() % 20 == 0) {
            if (be.tank.getFill() > 0) {
                be.tank.drainMb(1);
                be.isOn = true;
            } else {
                be.isOn = false;
            }
            be.trySubscribe(be.tank.getTankType(), level, pos.relative(rot), rot);
            be.trySubscribe(be.tank.getTankType(), level, pos.relative(rot.getOpposite()), rot.getOpposite());
            be.trySubscribe(be.tank.getTankType(), level, pos.below(), Direction.DOWN);
        }

        if (!be.isOn) return;

        if (be.state == 0) {
            be.delay--;
            if (be.delay <= 0) be.state = 1;
        }

        if (be.state == 1) {
            be.angle += 82.5F / 60F;
            if (be.angle >= 82.5F) {
                be.angle = 82.5F;
                be.state = 2;
            }
        } else if (be.state == 2) {
            be.angle -= 82.5F / 60F;
            if (be.angle <= 0F) {
                be.angle = 0F;
                be.state = 0;
                be.delay = 200 + level.getRandom().nextInt(100);
            }
        }

        if (be.angle != 0 && level instanceof ServerLevel serverLevel) {
            BlockPos armTip = pos.relative(facing.getOpposite(), 2);

            for (int i = -3; i <= 3; i++) {
                BlockPos target = armTip.relative(rot, i);
                BlockState targetState = level.getBlockState(target);
                Block block = targetState.getBlock();

                if (targetState.isCollisionShapeFullBlock(level, target)) {
                    be.state = 2;
                    break;
                }

                if (block instanceof CropBlock crop) {
                    be.harvestCrop(serverLevel, crop, target, targetState, pos, facing);
                } else if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
                    be.cutColumn(serverLevel, target, block, pos, facing);
                }
            }

            AABB aabb = new AABB(armTip).inflate(3.5, 0.5, 3.5);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
                if (e.isAlive() && e.hurt(level.damageSources().generic(), 100)) {
                    level.playSound(null, e.getX(), e.getY(), e.getZ(), net.minecraft.sounds.SoundEvents.WOOD_BREAK,
                            net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.95F + level.getRandom().nextFloat() * 0.2F);
                }
            }
        }

        be.setChanged();
        level.sendBlockUpdated(pos, blockState, blockState, 3);
    }

    private void harvestCrop(ServerLevel level, CropBlock crop, BlockPos target, BlockState state, BlockPos machinePos, Direction facing) {
        if (!crop.isMaxAge(state)) return;

        for (ItemStack drop : net.minecraft.world.level.block.Block.getDrops(state, level, target, null)) {
            dropItem(level, drop, machinePos, facing);
        }
        // CropBlock.getAgeProperty() is protected, so replant via the block's default (age 0) state.
        level.setBlockAndUpdate(target, crop.defaultBlockState());
    }

    private void cutColumn(ServerLevel level, BlockPos target, Block columnBlock, BlockPos machinePos, Direction facing) {
        // Cut everything above the base of the 2-3 tall column, leaving the bottom block to keep growing.
        BlockPos current = target.above();
        while (level.getBlockState(current).is(columnBlock)) {
            BlockState state = level.getBlockState(current);
            for (ItemStack drop : net.minecraft.world.level.block.Block.getDrops(state, level, current, null)) {
                dropItem(level, drop, machinePos, facing);
            }
            level.removeBlock(current, false);
            current = current.above();
        }
    }

    private void dropItem(ServerLevel level, ItemStack stack, BlockPos machinePos, Direction facing) {
        double spawnX = machinePos.getX() + 0.5 - facing.getStepX() * 0.75;
        double spawnZ = machinePos.getZ() + 0.5 - facing.getStepZ() * 0.75;
        ItemEntity item = new ItemEntity(level, spawnX, machinePos.getY(), spawnZ, stack);
        item.setPickUpDelay(10);
        item.setDeltaMovement(facing.getStepX() * -0.2 + 0.2, item.getDeltaMovement().y, facing.getStepZ() * -0.2);
        level.addFreshEntity(item);
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
        return com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.WOODOIL.getSource()) ||
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.ETHANOL.getSource()) ||
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.FISHOIL.getSource()) ||
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.HEAVYOIL.getSource()) ||
                com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.COALCREOSOTE.getSource());
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("isOn", isOn);
        tag.putFloat("angle", angle);
        tag.putInt("state", state);
        tag.putInt("delay", delay);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isOn = tag.getBoolean("isOn");
        angle = tag.getFloat("angle");
        state = tag.getInt("state");
        delay = tag.getInt("delay");
        tank.readFromNBT(tag, "tank");
    }
}
