package com.hbm_m.entity.cart;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * 1:1 {@code EntityMinecartTest} ({@code entity_minecart_test}): Test-Lore des Originals - verhaelt sich wie die
 * TNT-Lore (Zuender 80 Ticks, Explosion bei Aufprall/Sturz/Feuer), zeigt aber eine Kiste bzw. das Little-Boy-Modell.
 * Ohne Gegenstand, nur per /summon.
 */
public class EntityMinecartTest extends AbstractMinecart {

    private int minecartTNTFuse = -1;

    public EntityMinecartTest(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMinecartTest(EntityType<?> type, Level world, double x, double y, double z) {
        super(type, world, x, y, z);
    }

    @Override
    public @NotNull Type getMinecartType() {
        // Original getMinecartType() == 9 (kein Vanilla-Typ)
        return Type.TNT;
    }

    @Override
    protected @NotNull Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public @NotNull BlockState getDefaultDisplayBlockState() {
        return ModBlocks.CRATE.get().defaultBlockState();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.minecartTNTFuse > 0) {
            --this.minecartTNTFuse;
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
        } else if (this.minecartTNTFuse == 0) {
            this.explodeCart(this.getDeltaMovement().horizontalDistanceSqr());
        }

        if (this.horizontalCollision) {
            double d0 = this.getDeltaMovement().horizontalDistanceSqr();

            if (d0 >= 0.009999999776482582D) {
                this.explodeCart(d0);
            }
        }
    }

    /** Original {@code killMinecart}. */
    @Override
    public void destroy(@NotNull DamageSource source) {
        super.destroy(source);
        double d0 = this.getDeltaMovement().horizontalDistanceSqr();

        if (!source.is(DamageTypeTags.IS_EXPLOSION)) {
            this.spawnAtLocation(new ItemStack(Blocks.TNT, 1), 0.0F);
        }

        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION) || d0 >= 0.009999999776482582D) {
            this.explodeCart(d0);
        }
    }

    /** Laesst die Lore explodieren. */
    protected void explodeCart(double speedSq) {
        if (!this.level().isClientSide) {
            double d1 = Math.sqrt(speedSq);

            if (d1 > 5.0D) {
                d1 = 5.0D;
            }

            this.level().explode(this, this.getX(), this.getY(), this.getZ(), (float) (4.0D + this.random.nextDouble() * 1.5D * d1), Level.ExplosionInteraction.TNT);
            this.discard();
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        if (fallDistance >= 3.0F) {
            float f1 = fallDistance / 10.0F;
            this.explodeCart(f1 * f1);
        }

        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    /** Jeden Tick auf einer Aktivierungsschiene. */
    @Override
    public void activateMinecart(int x, int y, int z, boolean powered) {
        if (powered && this.minecartTNTFuse < 0) {
            this.ignite();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 10) {
            this.ignite();
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Zuendet die Lore. */
    public void ignite() {
        this.minecartTNTFuse = 80;

        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 10);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public int getFuse() {
        return this.minecartTNTFuse;
    }

    public boolean isIgnited() {
        return this.minecartTNTFuse > -1;
    }

    @Override
    public float getBlockExplosionResistance(@NotNull Explosion explosion, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull FluidState fluid, float resistance) {
        return this.isIgnited() && (state.is(net.minecraft.tags.BlockTags.RAILS) || world.getBlockState(pos.above()).is(net.minecraft.tags.BlockTags.RAILS)) ? 0.0F : super.getBlockExplosionResistance(explosion, world, pos, state, fluid, resistance);
    }

    @Override
    public boolean shouldBlockExplode(@NotNull Explosion explosion, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull BlockState state, float power) {
        return this.isIgnited() && (BaseRailBlock.isRail(state) || world.getBlockState(pos.above()).is(net.minecraft.tags.BlockTags.RAILS)) ? false : super.shouldBlockExplode(explosion, world, pos, state, power);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if (nbt.contains("TNTFuse", 99)) {
            this.minecartTNTFuse = nbt.getInt("TNTFuse");
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("TNTFuse", this.minecartTNTFuse);
    }
}
