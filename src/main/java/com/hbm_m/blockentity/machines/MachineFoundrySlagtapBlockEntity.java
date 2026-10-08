package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.Mats.MaterialStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityFoundrySlagtap}: wie der Ausguss, aber ohne Abnehmer - das Material faellt bis 15 Bloecke tief
 * auf den Boden und wird dort zu Schlackepfuetzen ({@code slag}), die zusammenlaufen, zerfliessen und als Schrott
 * wieder abgebaut werden.
 */
public class MachineFoundrySlagtapBlockEntity extends MachineFoundryOutletBlockEntity {

    public MachineFoundrySlagtapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_SLAGTAP_BE.get(), pos, state);
    }

    @Nullable
    private static BlockHitResult trace(Level world, BlockPos pos) {
        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() - 0.125, pos.getZ() + 0.5);
        Vec3 end = new Vec3(pos.getX() + 0.5, pos.getY() + 0.125 - 15, pos.getZ() + 0.5);
        BlockHitResult mop = world.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, null));
        return mop == null || mop.getType() != HitResult.Type.BLOCK ? null : mop;
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, MaterialStack stack) {

        if (filter != null && (filter != stack.material ^ invertFilter)) return false;
        if (isClosed()) return false;
        if (side != facing().getOpposite()) return false;

        return trace(world, pos) != null;
    }

    @Override
    @Nullable
    public MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack) {

        if (stack == null || stack.material == null || stack.amount <= 0) {
            return null;
        }

        BlockHitResult mop = trace(world, pos);

        if (mop == null) {
            return null;
        }

        BlockPos hitPos = mop.getBlockPos();
        BlockPos abovePos = hitPos.above();
        BlockState hit = world.getBlockState(hitPos);
        BlockState above = world.getBlockState(abovePos);

        boolean didFlow = false;

        if (hit.getBlock() == ModBlocks.SLAG_DYNAMIC.get()) {
            if (world.getBlockEntity(hitPos) instanceof SlagBlockEntity tile && tile.mat == stack.material) {
                int transfer = Math.min(SlagBlockEntity.maxAmount - tile.amount, stack.amount);
                tile.amount += transfer;
                stack.amount -= transfer;
                didFlow = didFlow || transfer > 0;
                tile.markForUpdate();
                world.scheduleTick(hitPos, ModBlocks.SLAG_DYNAMIC.get(), 1);
            }
        } else if (hit.canBeReplaced()) {
            didFlow = placeSlag(world, hitPos, stack) || didFlow;
        }

        if (stack.amount > 0 && above.canBeReplaced()) {
            didFlow = placeSlag(world, abovePos, stack) || didFlow;
        }

        if (didFlow && world instanceof ServerLevel server) {
            Direction dir = side.getOpposite();
            double hitY = hitPos.getY();
            CompoundTag data = new CompoundTag();
            data.putString("type", "foundry");
            data.putInt("color", stack.material.moltenColor);
            data.putByte("dir", (byte) dir.get3DDataValue());
            data.putFloat("off", 0.375F);
            data.putFloat("base", 0F);
            data.putFloat("len", Math.max(1F, worldPosition.getY() - (float) (Math.ceil(hitY))));
            com.hbm_m.particle.helper.IParticleCreator.sendPacket(server, worldPosition.getX() + 0.5D - dir.getStepX() * 0.125, worldPosition.getY() + 0.125, worldPosition.getZ() + 0.5D - dir.getStepZ() * 0.125, 50, data);
        }

        if (stack.amount <= 0) {
            stack = null;
        }

        return stack;
    }

    private static boolean placeSlag(Level world, BlockPos pos, MaterialStack stack) {
        world.setBlock(pos, ModBlocks.SLAG_DYNAMIC.get().defaultBlockState(), 3);
        if (!(world.getBlockEntity(pos) instanceof SlagBlockEntity tile)) return false;
        tile.mat = stack.material;
        int transfer = Math.min(SlagBlockEntity.maxAmount, stack.amount);
        tile.amount += transfer;
        stack.amount -= transfer;
        tile.markForUpdate();
        world.scheduleTick(pos, ModBlocks.SLAG_DYNAMIC.get(), 1);
        return transfer > 0;
    }

    @Override public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) { return false; }
    @Override @Nullable public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) { return stack; }
}
