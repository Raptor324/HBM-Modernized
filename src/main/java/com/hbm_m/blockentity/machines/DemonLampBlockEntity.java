package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityDemonLamp}: jeden Tick 100000 RAD auf alles im Umkreis 25, geteilt durch die Summe der
 * Sprengfestigkeit dazwischen und das Abstandsquadrat; naeher als 2 Bloecke verbrennt man (100 Schaden).
 */
public class DemonLampBlockEntity extends BlockEntity {

    public DemonLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEMON_LAMP.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, DemonLampBlockEntity te) {
        float rads = 100000F;
        double range = 25D;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;

        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(cx, cy, cz, cx, cy, cz).inflate(range))) {
            Vec3 vec = new Vec3(e.getX() - cx, (e.getY() + e.getEyeHeight()) - cy, e.getZ() - cz);
            double len = vec.length();
            vec = vec.normalize();
            float res = 0;
            for (int i = 1; i < len; i++) {
                BlockPos p = BlockPos.containing(cx + vec.x * i, cy + vec.y * i, cz + vec.z * i);
                res += world.getBlockState(p).getBlock().getExplosionResistance();
            }
            if (res < 1) res = 1;
            float eRads = rads / res / (float) (len * len);
            ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, eRads);
            if (len < 2) e.hurt(world.damageSources().inFire(), 100);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return AABB.ofSize(worldPosition.getCenter(), 40, 40, 40);
    }
}
