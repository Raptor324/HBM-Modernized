package com.hbm_m.blockentity.bomb;

import java.util.function.BiConsumer;

import com.hbm_m.block.bomb.CrashedBombBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code TileEntityCrashedBomb}: alle 2 Ticks Strahlung auf Lebewesen, linear abfallend bis zum Radius. */
public class CrashedBombBlockEntity extends BlockEntity implements com.hbm_m.api.render.RenderBoundsProvider {

    public CrashedBombBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRASHED_BOMB.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, CrashedBombBlockEntity te) {
        if (world.getGameTime() % 2 != 0 || !(state.getBlock() instanceof CrashedBombBlock block)) return;
        switch (block.type) {
            case BALEFIRE -> te.affectEntities((e, i) -> rad(e, 1F * i), 15D);
            case NUKE -> te.affectEntities((e, i) -> rad(e, 0.25F * i), 10D);
            case SALTED -> te.affectEntities((e, i) -> rad(e, 0.5F * i), 10D);
            default -> { }
        }
    }

    private static void rad(LivingEntity e, float amount) {
        ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, amount);
    }

    public void affectEntities(BiConsumer<LivingEntity, Float> effect, double range) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(cx, cy, cz, cx, cy, cz).inflate(range))) {
            double dx = entity.getX() - cx, dy = entity.getY() + entity.getBbHeight() / 2 - cy, dz = entity.getZ() - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > range) continue;
            effect.accept(entity, (float) (1D - dist / range));
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(4);
    }
}
