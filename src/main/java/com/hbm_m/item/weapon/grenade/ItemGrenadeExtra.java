package com.hbm_m.item.weapon.grenade;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hbm_m.entity.grenade.EntityGrenadeUniversal;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code ItemGrenadeExtra}: jede Original-Meta ist ein eigener Gegenstand {@code grenade_extra_<typ>}. */
public class ItemGrenadeExtra extends Item {

    public final EnumGrenadeExtra type;

    public ItemGrenadeExtra(EnumGrenadeExtra type, Properties props) {
        super(props);
        this.type = type;
    }

    public static enum EnumGrenadeExtra {
        GLUE(null, Lambdas.EXTRA_GLUE, null),          // sticky bombs!
        PROXY_FUZE(Lambdas.EXTRA_PROXY, null, null),   // additional 10m EntityLivingBase triggered fuze
        FRAG_SLEEVE(null, null, Lambdas.EXTRA_FRAG),   // 25 extra frags
        TRIPLEX(null, null, Lambdas.EXTRA_TRIPLEX);    // [THE BIG ONE]

        public Consumer<EntityGrenadeUniversal> updateTick;
        public BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> onImpact;
        public Consumer<EntityGrenadeUniversal> onExplode;

        private EnumGrenadeExtra(Consumer<EntityGrenadeUniversal> updateTick,
                BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> onImpact,
                Consumer<EntityGrenadeUniversal> onExplode) {
            this.updateTick = updateTick;
            this.onImpact = onImpact;
            this.onExplode = onExplode;
        }
    }

    public static BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> EXTRA_GLUE = Lambdas.EXTRA_GLUE;
    public static Consumer<EntityGrenadeUniversal> EXTRA_PROXY = Lambdas.EXTRA_PROXY;
    public static Consumer<EntityGrenadeUniversal> EXTRA_FRAG = Lambdas.EXTRA_FRAG;
    public static Consumer<EntityGrenadeUniversal> EXTRA_TRIPLEX = Lambdas.EXTRA_TRIPLEX;

    static final class Lambdas {

        static final BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> EXTRA_GLUE = (grenade, mop) -> {
            if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                grenade.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                grenade.getStuck(new BlockPos(mop.blockX, mop.blockY, mop.blockZ), mop.sideHit);
            }
        };

        static final Consumer<EntityGrenadeUniversal> EXTRA_PROXY = (grenade) -> {
            if (grenade.getTimer() >= 10 && grenade.getTimer() % 3 == 0) {
                List<LivingEntity> living = grenade.level().getEntitiesOfClass(LivingEntity.class, new AABB(grenade.getX(), grenade.getY(), grenade.getZ(), grenade.getX(), grenade.getY(), grenade.getZ()).inflate(10, 10, 10));
                for (LivingEntity e : living) {
                    if (e == grenade.getThrower()) continue;
                    if (e.distanceTo(grenade) <= 10) {
                        grenade.explode();
                        return;
                    }
                }
            }
        };

        static final Consumer<EntityGrenadeUniversal> EXTRA_FRAG = (grenade) -> {
            ItemGrenadeFilling.standardFragmentation(grenade, 25);
        };

        static final Consumer<EntityGrenadeUniversal> EXTRA_TRIPLEX = (grenade) -> {
            ItemStack frag = ItemGrenadeUniversal.make(grenade.getShell(), grenade.getFilling(), EnumGrenadeFuze.S3);

            Vec3NT vec = new Vec3NT(0.25, 0, 0).rotateAroundYDeg(grenade.level().random.nextDouble() * 360);

            for (int i = 0; i < 3; i++) {
                EntityGrenadeUniversal triplet = new EntityGrenadeUniversal(grenade.level(), frag).setTrail(EntityGrenadeUniversal.TRAIL_TRIPLET);
                triplet.setPosition(grenade.getX(), grenade.getY(), grenade.getZ());
                triplet.setThrower(grenade.getThrower());
                triplet.setDeltaMovement(vec.xCoord, 0.75D, vec.zCoord);
                grenade.level().addFreshEntity(triplet);
                vec.rotateAroundYDeg(120);
            }
        };
    }
}
