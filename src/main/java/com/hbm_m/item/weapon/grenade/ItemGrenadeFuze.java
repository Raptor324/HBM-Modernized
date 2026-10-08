package com.hbm_m.item.weapon.grenade;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hbm_m.entity.grenade.EntityGrenadeUniversal;
import com.hbm_m.util.MovingObjectPosition;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code ItemGrenadeFuze}: jede Original-Meta ist ein eigener Gegenstand {@code grenade_fuze_<typ>}. */
public class ItemGrenadeFuze extends Item {

    public final EnumGrenadeFuze type;

    public ItemGrenadeFuze(EnumGrenadeFuze type, Properties props) {
        super(props);
        this.type = type;
    }

    public static enum EnumGrenadeFuze {
        S3(FuzeLambdas.FUZE_3S,              0x000000), // 3s timed
        S7(FuzeLambdas.FUZE_7S,              0x404040), // 7s times
        S15(FuzeLambdas.FUZE_15S,            0x808080), // 15s timed
        IMPACT(FuzeLambdas.FUZE_IMPACT,      0xE36C17), // on block/entity impact, 0.5s safety
        AIRBURST(FuzeLambdas.FUZE_AIRBURST,  0x56A137); // 2s safety, explodes 10 blocks above ground

        public Consumer<EntityGrenadeUniversal> updateTick;
        public BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> onImpact;
        public int bandColor;

        private EnumGrenadeFuze(Consumer<EntityGrenadeUniversal> updateTick, int color) { this(updateTick, null, color); }
        private EnumGrenadeFuze(BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> onImpact, int color) { this(null, onImpact, color); }
        private EnumGrenadeFuze(Consumer<EntityGrenadeUniversal> updateTick, BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> onImpact, int color) {
            this.updateTick = updateTick;
            this.onImpact = onImpact;
            this.bandColor = color;
        }
    }

    public static Consumer<EntityGrenadeUniversal> FUZE_3S = FuzeLambdas.FUZE_3S;
    public static Consumer<EntityGrenadeUniversal> FUZE_7S = FuzeLambdas.FUZE_7S;
    public static Consumer<EntityGrenadeUniversal> FUZE_15S = FuzeLambdas.FUZE_15S;
    public static BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> FUZE_IMPACT = FuzeLambdas.FUZE_IMPACT;
    public static Consumer<EntityGrenadeUniversal> FUZE_AIRBURST = FuzeLambdas.FUZE_AIRBURST;

    /** Die Lambdas muessen vor den Enum-Konstanten initialisiert sein (Java-Initialisierungsreihenfolge). */
    static final class FuzeLambdas {
        static final Consumer<EntityGrenadeUniversal> FUZE_3S = (grenade) -> { if (grenade.getTimer() >= 60) grenade.explode(); };
        static final Consumer<EntityGrenadeUniversal> FUZE_7S = (grenade) -> { if (grenade.getTimer() >= 140) grenade.explode(); };
        static final Consumer<EntityGrenadeUniversal> FUZE_15S = (grenade) -> { if (grenade.getTimer() >= 300) grenade.explode(); };
        static final BiConsumer<EntityGrenadeUniversal, MovingObjectPosition> FUZE_IMPACT = (grenade, mop) -> {
            if (grenade.getTimer() >= 10) {
                grenade.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                grenade.explode();
            }
        };
        static final Consumer<EntityGrenadeUniversal> FUZE_AIRBURST = (grenade) -> {
            if (grenade.getTimer() >= 30) {
                Vec3 start = grenade.position();
                Vec3 end = start.add(0, -10, 0);
                BlockHitResult mop = grenade.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, grenade));
                if (mop.getType() == HitResult.Type.BLOCK) grenade.explode();
            }
        };
    }
}
