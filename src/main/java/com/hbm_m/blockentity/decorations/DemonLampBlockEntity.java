package com.hbm_m.blockentity.decorations;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Порт {@code BlockEntityDemonLamp} — лампа из заряда-демона.
 *
 * <p>Каждый серверный тик облучает всех {@link LivingEntity} в радиусе {@link #RANGE}:
 * доза спадает квадратично с расстоянием и экранируется суммой взрывоустойчивости
 * блоков вдоль луча от центра лампы к глазу сущности (доза действует даже на
 * креативных игроков — {@link ContaminationType#CREATIVE}). Сущности ближе
 * {@link #BURN_RANGE} блоков дополнительно получают {@link #BURN_DAMAGE} урона огнём.
 */
public class DemonLampBlockEntity extends BaseHbmBlockEntity {

    public static final float RADS = 100000F;
    public static final double RANGE = 25D;
    public static final double BURN_RANGE = 2D;
    public static final float BURN_DAMAGE = 100F;

    /** Протяжённость световых лучей рендерера — используется и в render bbox. */
    public static final double RENDER_RANGE = 15D;

    public DemonLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEMON_LAMP_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DemonLampBlockEntity be) {
        if (!level.isClientSide) radiate((ServerLevel) level, pos);
    }

    public static void radiate(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;
        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
        for (LivingEntity e :
                level.getEntitiesOfClass(
                        LivingEntity.class, new AABB(cx, cy, cz, cx, cy, cz).inflate(RANGE))) {
            Vec3 vec = new Vec3(e.getX() - cx, e.getEyeY() - cy, e.getZ() - cz);
            double len = vec.length();
            vec = vec.normalize();

            float res = 0F;
            for (int i = 1; i < len; i++) {
                cell.set(
                        Mth.floor(cx + vec.x * i),
                        Mth.floor(cy + vec.y * i),
                        Mth.floor(cz + vec.z * i));
                res += level.getBlockState(cell).getBlock().getExplosionResistance();
            }
            if (res < 1F) res = 1F;

            float eRads = RADS;
            eRads /= res;
            eRads /= (float) (len * len);
            ContaminationUtil.contaminate(
                    e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);

            if (len < BURN_RANGE) e.hurt(level.damageSources().inFire(), BURN_DAMAGE);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(RENDER_RANGE);
    }
}
