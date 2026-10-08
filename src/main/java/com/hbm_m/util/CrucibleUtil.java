package com.hbm_m.util;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.util.CrucibleUtil}: Giessen per Strahl senkrecht nach unten auf den ersten Block (auch Fluessig-
 * keiten halten ihn auf). Ist er ein {@link ICrucibleAcceptor}, wird dorthin gegossen, sonst - wenn nicht sicher -
 * verschuettet. {@code impactPosHolder} (drei Werte) bekommt die Auftreffstelle des Strahls.
 */
public class CrucibleUtil {

    /**
     * Standard pouring, casting a hitscan straight down at the given coordinates with the given range. Returns the leftover material, just like ICrucibleAcceptor's pour.
     * The method directly modifies the original stack, so be careful and make a copy beforehand if you don't want that.
     */
    @Nullable
    public static MaterialStack pourSingleStack(Level world, double x, double y, double z, double range, boolean safe, MaterialStack stack, int quanta, @Nullable double[] impactPosHolder) {

        Vec3 start = new Vec3(x, y, z);
        Vec3 end = new Vec3(x, y - range, z);

        BlockHitResult[] mopHolder = new BlockHitResult[1];
        ICrucibleAcceptor acc = getPouringTarget(world, start, end, mopHolder);
        BlockHitResult mop = mopHolder[0];

        if (acc == null) {
            spill(mop, safe, stack, quanta, impactPosHolder);
            return stack;
        }

        MaterialStack ret = tryPourStack(world, acc, mop, stack, impactPosHolder);

        if (ret != null) {
            return ret;
        }

        spill(mop, safe, stack, quanta, impactPosHolder);
        return stack;
    }

    /**
     * Standard pouring, casting a hitscan straight down at the given coordinates with the given range. Returns the materialStack that has been removed.
     * The method doesn't make copies of the MaterialStacks in the list, so the materials being subtracted or outright removed will apply to the original list.
     */
    @Nullable
    public static MaterialStack pourFullStack(Level world, double x, double y, double z, double range, boolean safe, List<MaterialStack> stacks, int quanta, @Nullable double[] impactPosHolder) {

        if (stacks.isEmpty()) return null;

        Vec3 start = new Vec3(x, y, z);
        Vec3 end = new Vec3(x, y - range, z);

        BlockHitResult[] mopHolder = new BlockHitResult[1];
        ICrucibleAcceptor acc = getPouringTarget(world, start, end, mopHolder);
        BlockHitResult mop = mopHolder[0];

        if (acc == null) {
            return spill(mop, safe, stacks, quanta, impactPosHolder);
        }

        for (MaterialStack stack : stacks) {
            if (stack.material == null) continue;

            int amountToPour = Math.min(stack.amount, quanta);
            MaterialStack toPour = new MaterialStack(stack.material, amountToPour);
            MaterialStack left = tryPourStack(world, acc, mop, toPour, impactPosHolder);

            if (left != null) {
                stack.amount -= (amountToPour - left.amount);
                return new MaterialStack(stack.material, stack.amount - left.amount);
            }
        }

        return spill(mop, safe, stacks, quanta, impactPosHolder);
    }

    /**
     * Tries to pour the stack onto the supplied crucible acceptor instance.
     * Returns whatever is left of the stack when successful or null when unsuccessful (potential spillage).
     */
    @Nullable
    public static MaterialStack tryPourStack(Level world, ICrucibleAcceptor acc, BlockHitResult mop, MaterialStack stack, @Nullable double[] impactPosHolder) {
        Vec3 hit = mop.getLocation();

        if (stack.material.smeltable != SmeltingBehavior.SMELTABLE) {
            return null;
        }

        if (acc.canAcceptPartialPour(world, mop.getBlockPos(), hit.x, hit.y, hit.z, mop.getDirection(), stack)) {
            MaterialStack left = acc.pour(world, mop.getBlockPos(), hit.x, hit.y, hit.z, mop.getDirection(), stack);
            if (left == null) {
                left = new MaterialStack(stack.material, 0);
            }

            if (impactPosHolder != null) {
                impactPosHolder[0] = hit.x;
                impactPosHolder[1] = hit.y;
                impactPosHolder[2] = hit.z;
            }

            return left;
        }

        return null;
    }

    /** Uses hitscan to find the target of the pour, from start (the top) to end (the bottom). */
    @Nullable
    public static ICrucibleAcceptor getPouringTarget(Level world, Vec3 start, Vec3 end, @Nullable BlockHitResult[] mopHolder) {

        BlockHitResult mop = world.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, null));

        if (mopHolder != null) {
            mopHolder[0] = mop;
        }

        if (mop == null || mop.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        return acceptorAt(world, mop.getBlockPos());
    }

    /** Der Abnehmer an einer Stelle - Block-Entity selbst oder der Kern eines Multiblocks. */
    @Nullable
    public static ICrucibleAcceptor acceptorAt(Level world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ICrucibleAcceptor acc) return acc;
        if (be instanceof com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity part) {
            BlockPos core = part.getControllerPos();
            if (core != null && world.getBlockEntity(core) instanceof ICrucibleAcceptor acc) return acc;
        }
        return null;
    }

    /** Regular spillage routine but accepts a stack list instead of a stack. simply uses the first available stack from the list. */
    @Nullable
    public static MaterialStack spill(@Nullable BlockHitResult mop, boolean safe, List<MaterialStack> stacks, int quanta, @Nullable double[] impactPos) {
        MaterialStack top = stacks.get(0);
        MaterialStack ret = spill(mop, safe, top, quanta, impactPos);
        stacks.removeIf(o -> o.amount <= 0);
        return ret;
    }

    /** The routine used for then there is no valid crucible acceptor found. Will NOP with safe mode on. Returns the MaterialStack that was lost. */
    @Nullable
    public static MaterialStack spill(@Nullable BlockHitResult mop, boolean safe, MaterialStack stack, int quanta, @Nullable double[] impactPos) {

        if (safe) {
            return null;
        }

        MaterialStack toWaste = new MaterialStack(stack.material, Math.min(stack.amount, quanta));
        stack.amount -= toWaste.amount;

        if (impactPos != null && mop != null) {
            impactPos[0] = mop.getLocation().x;
            impactPos[1] = mop.getLocation().y;
            impactPos[2] = mop.getLocation().z;
        }

        return toWaste;
    }
}
