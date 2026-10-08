package com.hbm_m.explosion;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityMist;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.server.level.ServerLevel;

/**
 * 1:1 {@code BombMulti.igniteTestBomb}: Grundwert 8, je Modul (Slot 2 und 5) Schiesspulver +1, TNT +4,
 * Streuladung 50 Splitter, Brandsatz Radius 10, Gift (Verstrahlung ohne Schrabidium) Radius 15, Chlorgaswolke 50.
 */
public final class MultiBombExplosion {

    public static final float BASE_STRENGTH = 8.0F;

    private MultiBombExplosion() {}

    public static void detonate(ServerLevel world, int x, int y, int z, int type2, int type5) {
        float explosionValue = BASE_STRENGTH;
        int clusterCount = 0;
        int fireRadius = 0;
        int poisonRadius = 0;
        int gasCloud = 0;

        for (int type : new int[] { type2, type5 }) {
            switch (type) {
                case 1 -> explosionValue += 1.0F;
                case 2 -> explosionValue += 4.0F;
                case 3 -> clusterCount += 50;
                case 4 -> fireRadius += 10;
                case 5 -> poisonRadius += 15;
                case 6 -> gasCloud += 50;
                default -> { }
            }
        }

        ExplosionLarge.explode(world, x, y, z, explosionValue, true, true, true);

        if (clusterCount > 0) {
            ExplosionChaos.cluster(world, x + 0.5, y + 0.5, z + 0.5, clusterCount, 0, (float) Math.PI * 0.5F, (float) Math.PI * 2F, (float) Math.PI * 0.125F, 0.375F);
        }

        if (fireRadius > 0) {
            ExplosionChaos.igniteAllBlocks(world, x, y, z, fireRadius);
        }

        if (poisonRadius > 0) {
            ExplosionNukeGeneric.wasteNoSchrab(world, x, y, z, poisonRadius);
        }

        if (gasCloud > 0) {
            EntityMist mist = new EntityMist(ModEntities.ENTITY_MIST.get(), world);
            mist.setFluidType(FluidType.forFluid(ModFluids.CHLORINE.getSource()));
            mist.setPos(x + 0.5, y + 0.5, z + 0.5);
            mist.setArea(gasCloud * 15F / 50F, gasCloud * 7.5F / 50F);
            world.addFreshEntity(mist);
        }
    }
}
