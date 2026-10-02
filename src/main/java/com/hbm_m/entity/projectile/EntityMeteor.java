package com.hbm_m.entity.projectile;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.worldgen.Meteorite;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.entity.projectile.EntityMeteor}: faellt mit bis zu 2.5 Bloecken/Tick, raeumt unterhalb von
 * Y 260 eine Kugel (Radius 5) frei bzw. beschaedigt Bloecke und erzeugt beim Aufschlag eine Explosion
 * (5-6, bei {@code safe} ohne Blockschaden) und einen {@link Meteorite}. Ohne {@code enableMeteorStrikes}
 * verschwindet er sofort.
 */
public class EntityMeteor extends Entity {

    public boolean safe = false;

    /** Client: Flug-Schleifenklang (Original AudioWrapper). */
    public Object audioFly;

    public EntityMeteor(EntityType<? extends EntityMeteor> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityMeteor(Level world) {
        this(ModEntities.METEOR.get(), world);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public List<BlockPos> getBlocksInRadius(Level world, int x, int y, int z, int radius) {
        List<BlockPos> foundBlocks = new ArrayList<>();

        int rSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Check if point (dx, dy, dz) lies inside the sphere
                    if (dx * dx + dy * dy + dz * dz <= rSq) {
                        foundBlocks.add(new BlockPos(x + dx, y + dy, z + dz));
                    }
                }
            }
        }
        return foundBlocks;
    }

    public void damageOrDestroyBlock(Level world, BlockPos pos) {
        if (safe) return;

        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;

        float hardness = state.getDestroySpeed(world, pos);

        // Check if the block is weak and can be destroyed
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || (hardness >= 0 && hardness <= 0.3F)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            // Found solid block
            if (hardness < 0 || hardness > 5F) return;

            if (random.nextInt(6) == 1) {
                // Turn blocks into damaged variants
                if (state.is(Blocks.DIRT)) {
                    world.setBlock(pos, ModBlocks.DIRT_DEAD.get().defaultBlockState(), 3);
                } else if (state.is(Blocks.SAND)) {
                    if (random.nextInt(2) == 1) {
                        world.setBlock(pos, Blocks.SANDSTONE.defaultBlockState(), 3);
                    } else {
                        world.setBlock(pos, Blocks.GLASS.defaultBlockState(), 3);
                    }
                } else if (state.is(Blocks.STONE)) {
                    world.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                } else if (state.is(Blocks.GRASS_BLOCK)) {
                    world.setBlock(pos, ModBlocks.WASTE_EARTH.get().defaultBlockState(), 3);
                }
            }
        }
    }

    public void clearMeteorPath(Level world, int x, int y, int z) {
        for (BlockPos blockPos : getBlocksInRadius(world, x, y, z, 5)) {
            damageOrDestroyBlock(world, blockPos);
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !ModClothConfig.get().enableMeteorStrikes) {
            this.discard();
            return;
        }

        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        double motionY = this.getDeltaMovement().y - 0.03;
        if (motionY < -2.5) motionY = -2.5;
        this.setDeltaMovement(this.getDeltaMovement().x, motionY, this.getDeltaMovement().z);

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (!this.level().isClientSide && this.getY() < 260) {
            Level world = this.level();
            clearMeteorPath(world, (int) this.getX(), (int) this.getY(), (int) this.getZ());

            if (this.onGround()) {
                world.explode(this, this.getX(), this.getY(), this.getZ(), 5 + random.nextFloat(),
                        !safe ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);

                if (ModClothConfig.get().enableMeteorTails) {
                    ExplosionLarge.spawnRubble(world, this.getX(), this.getY(), this.getZ(), 15);
                    ExplosionLarge.spawnParticles(world, getX(), getY() + 5, getZ(), 75);
                    ExplosionLarge.spawnParticles(world, getX() + 5, getY(), getZ(), 75);
                    ExplosionLarge.spawnParticles(world, getX() - 5, getY(), getZ(), 75);
                    ExplosionLarge.spawnParticles(world, getX(), getY(), getZ() + 5, 75);
                    ExplosionLarge.spawnParticles(world, getX(), getY(), getZ() - 5, 75);
                }

                double motionZ = this.getDeltaMovement().z;
                // Bury the meteor into the ground (Original nimmt fuer X und Z motionZ)
                int spawnPosX = (int) (Math.round(this.getX() - 0.5D) + (safe ? 0 : (motionZ * 4)));
                int spawnPosY = (int) Math.round(this.getY() - (safe ? 0 : 4));
                int spawnPosZ = (int) (Math.round(this.getZ() - 0.5D) + (safe ? 0 : (motionZ * 4)));

                (new Meteorite()).generate(world, random, spawnPosX, spawnPosY, spawnPosZ, safe, true, true);

                clearMeteorPath(world, spawnPosX, spawnPosY, spawnPosZ);

                world.playSound(null, this.getX(), this.getY(), this.getZ(), HbmSoundsNT.get("hbm:entity.oldExplosion"), SoundSource.AMBIENT,
                        10000.0F, 0.5F + this.random.nextFloat() * 0.1F);
                this.discard();
            }
        }

        if (this.level().isClientSide) {
            com.hbm_m.client.sound.MeteorSoundClient.tick(this);

            if (ModClothConfig.get().enableMeteorTails) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "exhaust");
                data.putString("mode", "meteor");
                data.putInt("count", 10);
                data.putDouble("width", 1);
                data.putDouble("posX", getX() - getDeltaMovement().x);
                data.putDouble("posY", getY() - getDeltaMovement().y);
                data.putDouble("posZ", getZ() - getDeltaMovement().z);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.level().isClientSide) com.hbm_m.client.sound.MeteorSoundClient.stop(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override protected void defineSynchedData() { }
    @Override protected void readAdditionalSaveData(CompoundTag nbt) { this.safe = nbt.getBoolean("safe"); }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) { nbt.putBoolean("safe", safe); }
}
