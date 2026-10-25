package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.EntityCyberCrab;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityCyberCrab}: alle 200 Ticks, wenn darueber Luft und ein Spieler binnen 25 Bloecken ist und
 * weniger als 5 Krabben in der Naehe sind, eine Krabbe (1/5 Tesla-Krabbe). Der Zaehler wird nicht gespeichert.
 */
public class CyberCrabSpawnerBlockEntity extends BlockEntity {

    int age = 0;

    public CyberCrabSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CYBERCRAB_SPAWNER_BE.get(), pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, CyberCrabSpawnerBlockEntity be) {

        be.age++;
        int xCoord = pos.getX(), yCoord = pos.getY(), zCoord = pos.getZ();
        if (be.age > 200 && world.getBlockState(pos.above()).isAir() && world.getNearestPlayer(xCoord + 0.5, yCoord + 1, zCoord + 0.5, 25, false) != null) {
            List<EntityCyberCrab> entities = world.getEntitiesOfClass(EntityCyberCrab.class,
                    new AABB(xCoord - 5, yCoord - 2, zCoord - 5, xCoord + 6, yCoord + 4, zCoord + 6));

            if (entities.size() < 5) {

                EntityCyberCrab crab;

                if (world.random.nextInt(5) == 0)
                    crab = ModEntities.TESLA_CRAB.get().create(world);
                else
                    crab = ModEntities.CYBER_CRAB.get().create(world);

                if (crab != null) {
                    crab.setPos(xCoord + 0.5, yCoord + 1, zCoord + 0.5);
                    world.addFreshEntity(crab);
                }
            }

            be.age = 0;
        }
    }
}
