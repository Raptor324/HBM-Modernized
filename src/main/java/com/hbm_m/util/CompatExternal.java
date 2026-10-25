package com.hbm_m.util;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Teil von {@code com.hbm.util.CompatExternal}: Blockentity des Multiblock-Kerns zu einer Position, Zielregeln der Geschuetze. */
public final class CompatExternal {

    private CompatExternal() {}

    @Nullable
    public static BlockEntity getCoreFromPos(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        // Original: BlockDummyable.findCore; im Port fuehren Teilbloecke die Kernposition selbst.
        if (te instanceof UniversalMachinePartBlockEntity part) {
            BlockPos core = part.getControllerPos();
            if (core != null) return world.getBlockEntity(core);
        }
        return te;
    }

    public static java.util.Set<Class<?>> turretTargetPlayer = new java.util.HashSet<>();
    public static java.util.Set<Class<?>> turretTargetFriendly = new java.util.HashSet<>();
    public static java.util.Set<Class<?>> turretTargetHostile = new java.util.HashSet<>();
    public static java.util.Set<Class<?>> turretTargetMachine = new java.util.HashSet<>();

    /**
     * Registers a class for turret targeting
     * @param clazz is the class that should be targeted.
     * @param type determines what setting the turret needs to have enabled to target this class. 0 is player, 1 is friendly, 2 is hostile and 3 is machine.
     */
    public static void registerTurretTargetSimple(Class<?> clazz, int type) {
        switch (type) {
            case 0 -> turretTargetPlayer.add(clazz);
            case 1 -> turretTargetFriendly.add(clazz);
            case 2 -> turretTargetHostile.add(clazz);
            case 3 -> turretTargetMachine.add(clazz);
            default -> { }
        }
    }

    public static java.util.Set<Class<?>> turretTargetBlacklist = new java.util.HashSet<>();

    /** Registers a class to be fully ignored by turrets */
    public static void registerTurretTargetBlacklist(Class<?> clazz) {
        turretTargetBlacklist.add(clazz);
    }

    public static java.util.HashMap<Class<?>, java.util.function.BiFunction<net.minecraft.world.entity.Entity, Object, Integer>> turretTargetCondition = new java.util.HashMap<>();

    /**
     * Registers a BiFunction lambda for more complex targeting compatibility. The function should return 0 to continue
     * with other targeting checks, -1 to ignore this entity or 1 to target it. Params: the entity and the turret.
     */
    public static void registerTurretTargetingCondition(Class<?> clazz, java.util.function.BiFunction<net.minecraft.world.entity.Entity, Object, Integer> bi) {
        turretTargetCondition.put(clazz, bi);
    }
}
