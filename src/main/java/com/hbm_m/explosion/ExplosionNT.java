package com.hbm_m.explosion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code ExplosionNT} (1.7.10): die Vanilla-Explosion mit Zusatzattributen
 * ({@link ExAttrib}) - Feuer/Balefire/Lava auf den Kraterboden, Digamma-Asche, Erosion von Beton,
 * wahlweise ohne Drops, ohne Schaden, ohne Partikel oder ohne Klang, und mit einstellbarer
 * Strahlaufloesung. Im Original als {@code @Deprecated} markiert, aber von Mini-Nukes,
 * Vulkanschrapnell und etlichen Bomben weiter benutzt.
 */
public class ExplosionNT {

    public final Set<ExAttrib> atttributes = EnumSet.noneOf(ExAttrib.class);
    private final RandomSource explosionRNG = RandomSource.create();
    private final Level worldObj;
    private final Entity exploder;
    private final double explosionX, explosionY, explosionZ;
    private float explosionSize;
    protected int resolution = 16;
    private final boolean isSmoking = true;
    private final List<BlockPos> affectedBlockPositions = new ArrayList<>();
    protected final Map<Player, Vec3> affectedEntities = new HashMap<>();
    private final Explosion vanilla;

    @Deprecated
    public static final List<ExAttrib> nukeAttribs = Arrays.asList(ExAttrib.FIRE, ExAttrib.NOPARTICLE, ExAttrib.NOSOUND, ExAttrib.NODROP, ExAttrib.NOHURT);

    public ExplosionNT(Level world, Entity exploder, double x, double y, double z, float strength) {
        this.worldObj = world;
        this.exploder = exploder;
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;
        this.explosionSize = strength;
        this.vanilla = new Explosion(world, exploder, x, y, z, strength, false, Explosion.BlockInteraction.DESTROY);
    }

    public ExplosionNT addAttrib(ExAttrib attrib) {
        atttributes.add(attrib);
        return this;
    }

    public ExplosionNT addAllAttrib(List<ExAttrib> attrib) {
        atttributes.addAll(attrib);
        return this;
    }

    public ExplosionNT addAllAttrib(ExAttrib... attrib) {
        atttributes.addAll(Arrays.asList(attrib));
        return this;
    }

    public ExplosionNT overrideResolution(int res) {
        resolution = res;
        return this;
    }

    public void explode() {
        doExplosionA();
        doExplosionB(false);
    }

    private float resistance(BlockPos pos, BlockState state) {
        return state.getExplosionResistance(worldObj, pos, vanilla);
    }

    public void doExplosionA() {
        float f = this.explosionSize;
        Set<BlockPos> hashset = new HashSet<>();

        for (int i = 0; i < this.resolution; ++i) {
            for (int j = 0; j < this.resolution; ++j) {
                for (int k = 0; k < this.resolution; ++k) {
                    if (i == 0 || i == this.resolution - 1 || j == 0 || j == this.resolution - 1 || k == 0 || k == this.resolution - 1) {
                        double d0 = (float) i / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                        double d1 = (float) j / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                        double d2 = (float) k / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                        double dist = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= dist;
                        d1 /= dist;
                        d2 /= dist;
                        float remainingPower = this.explosionSize * (0.7F + this.worldObj.random.nextFloat() * 0.6F);
                        double currentX = this.explosionX;
                        double currentY = this.explosionY;
                        double currentZ = this.explosionZ;

                        for (float step = 0.3F; remainingPower > 0.0F; remainingPower -= step * 0.75F) {
                            BlockPos pos = BlockPos.containing(currentX, currentY, currentZ);
                            if (!worldObj.isInWorldBounds(pos)) break;
                            BlockState block = this.worldObj.getBlockState(pos);

                            if (!block.isAir()) {
                                float res = resistance(pos, block);
                                remainingPower -= (res + 0.3F) * step;
                            }

                            if (!block.isAir() && remainingPower > 0.0F) {
                                hashset.add(pos);
                            } else if (this.has(ExAttrib.ERRODE) && erodesInto(block.getBlock()) != null) {
                                hashset.add(pos);
                            }

                            currentX += d0 * step;
                            currentY += d1 * step;
                            currentZ += d2 * step;
                        }
                    }
                }
            }
        }

        this.affectedBlockPositions.addAll(hashset);

        if (!has(ExAttrib.NOHURT)) {
            this.explosionSize *= 2.0F;
            int i = Mth.floor(this.explosionX - this.explosionSize - 1.0D);
            int j = Mth.floor(this.explosionX + this.explosionSize + 1.0D);
            int k = Mth.floor(this.explosionY - this.explosionSize - 1.0D);
            int i2 = Mth.floor(this.explosionY + this.explosionSize + 1.0D);
            int l = Mth.floor(this.explosionZ - this.explosionSize - 1.0D);
            int j2 = Mth.floor(this.explosionZ + this.explosionSize + 1.0D);
            List<Entity> list = this.worldObj.getEntities(this.exploder, new AABB(i, k, l, j, i2, j2));
            //? if forge {
            net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.worldObj, this.vanilla, list, this.explosionSize);
            //?} elif neoforge {
            /*net.neoforged.neoforge.event.EventHooks.onExplosionDetonate(this.worldObj, this.vanilla, list, this.explosionSize);
            *///?}
            Vec3 vec3 = new Vec3(this.explosionX, this.explosionY, this.explosionZ);

            for (Entity entity : list) {
                double d4 = Math.sqrt(entity.distanceToSqr(this.explosionX, this.explosionY, this.explosionZ)) / this.explosionSize;
                if (d4 <= 1.0D) {
                    double cx = entity.getX() - this.explosionX;
                    double cy = entity.getY() + entity.getEyeHeight() - this.explosionY;
                    double cz = entity.getZ() - this.explosionZ;
                    double d9 = Math.sqrt(cx * cx + cy * cy + cz * cz);
                    if (d9 != 0.0D) {
                        cx /= d9;
                        cy /= d9;
                        cz /= d9;
                        double d10 = Explosion.getSeenPercent(vec3, entity);
                        double d11 = (1.0D - d4) * d10;
                        entity.hurt(worldObj.damageSources().explosion(vanilla), (float) ((int) ((d11 * d11 + d11) / 2.0D * 8.0D * this.explosionSize + 1.0D)));
                        double d8 = entity instanceof LivingEntity le ? com.hbm_m.platform.PlatformHooks.getExplosionKnockbackAfterDampener(le, d11) : d11;
                        entity.setDeltaMovement(entity.getDeltaMovement().add(cx * d8, cy * d8, cz * d8));
                        if (entity instanceof Player p) {
                            this.affectedEntities.put(p, new Vec3(cx * d11, cy * d11, cz * d11));
                        }
                    }
                }
            }
            this.explosionSize = f;
        }
    }

    public void doExplosionB(boolean unused) {
        if (!has(ExAttrib.NOSOUND)) {
            this.worldObj.playSound(null, this.explosionX, this.explosionY, this.explosionZ, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                    (1.0F + (this.worldObj.random.nextFloat() - this.worldObj.random.nextFloat()) * 0.2F) * 0.7F);
        }

        if (!has(ExAttrib.NOPARTICLE) && worldObj instanceof ServerLevel sl) {
            if (this.explosionSize >= 2.0F && this.isSmoking) {
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.explosionX, this.explosionY, this.explosionZ, 1, 0, 0, 0, 0);
            } else {
                sl.sendParticles(ParticleTypes.EXPLOSION, this.explosionX, this.explosionY, this.explosionZ, 1, 0, 0, 0, 0);
            }
        }

        if (this.isSmoking) {
            for (BlockPos pos : this.affectedBlockPositions) {
                BlockState state = this.worldObj.getBlockState(pos);
                Block block = state.getBlock();

                if (!has(ExAttrib.NOPARTICLE) && worldObj instanceof ServerLevel sl) {
                    double d0 = pos.getX() + this.worldObj.random.nextFloat();
                    double d1 = pos.getY() + this.worldObj.random.nextFloat();
                    double d2 = pos.getZ() + this.worldObj.random.nextFloat();
                    double d3 = d0 - this.explosionX;
                    double d4 = d1 - this.explosionY;
                    double d5 = d2 - this.explosionZ;
                    double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                    d3 /= d6;
                    d4 /= d6;
                    d5 /= d6;
                    double d7 = 0.5D / (d6 / this.explosionSize + 0.1D);
                    d7 *= this.worldObj.random.nextFloat() * this.worldObj.random.nextFloat() + 0.3F;
                    d3 *= d7;
                    d4 *= d7;
                    d5 *= d7;
                    sl.sendParticles(ParticleTypes.POOF, (d0 + this.explosionX) / 2.0D, (d1 + this.explosionY) / 2.0D, (d2 + this.explosionZ) / 2.0D, 0, d3, d4, d5, 1);
                    sl.sendParticles(ParticleTypes.SMOKE, d0, d1, d2, 0, d3, d4, d5, 1);
                }

                if (!state.isAir()) {
                    boolean doesErrode = false;
                    Block errodesInto = Blocks.AIR;

                    if (this.has(ExAttrib.ERRODE) && this.explosionRNG.nextFloat() < 0.6F) {
                        Block into = erodesInto(block);
                        if (into != null) {
                            doesErrode = true;
                            errodesInto = into;
                        }
                    }

                    if (state.canDropFromExplosion(worldObj, pos, vanilla) && !has(ExAttrib.NODROP) && !doesErrode && worldObj instanceof ServerLevel sl) {
                        LootParams.Builder params = new LootParams.Builder(sl)
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, worldObj.getBlockEntity(pos))
                                .withOptionalParameter(LootContextParams.THIS_ENTITY, this.exploder);
                        // Original: chance = 1 / explosionSize, ausser ALLDROP.
                        if (!has(ExAttrib.ALLDROP)) params.withParameter(LootContextParams.EXPLOSION_RADIUS, this.explosionSize);
                        state.getDrops(params).forEach(stack -> Block.popResource(worldObj, pos, stack));
                    }

                    state.onBlockExploded(this.worldObj, pos, this.vanilla);

                    if (state.isRedstoneConductor(worldObj, pos) || state.isCollisionShapeFullBlock(worldObj, pos)) {
                        if (doesErrode) {
                            this.worldObj.setBlockAndUpdate(pos, errodesInto.defaultBlockState());
                        }
                        if (has(ExAttrib.DIGAMMA)) {
                            this.worldObj.setBlockAndUpdate(pos, ModBlocks.ASH_DIGAMMA.get().defaultBlockState());
                            if (this.explosionRNG.nextInt(5) == 0 && this.worldObj.getBlockState(pos.above()).isAir())
                                this.worldObj.setBlockAndUpdate(pos.above(), ModBlocks.FIRE_DIGAMMA.get().defaultBlockState());
                        } else if (has(ExAttrib.DIGAMMA_CIRCUIT)) {
                            int i = pos.getX(), k = pos.getZ();
                            if (i % 3 == 0 && k % 3 == 0) {
                                this.worldObj.setBlockAndUpdate(pos, ModBlocks.RBMK_DEBRIS_DIGAMMA.get().defaultBlockState());
                            } else if ((i % 3 == 0 || k % 3 == 0) && this.explosionRNG.nextBoolean()) {
                                this.worldObj.setBlockAndUpdate(pos, ModBlocks.RBMK_DEBRIS_DIGAMMA.get().defaultBlockState());
                            } else {
                                this.worldObj.setBlockAndUpdate(pos, ModBlocks.ASH_DIGAMMA.get().defaultBlockState());
                                if (this.explosionRNG.nextInt(5) == 0 && this.worldObj.getBlockState(pos.above()).isAir())
                                    this.worldObj.setBlockAndUpdate(pos.above(), ModBlocks.FIRE_DIGAMMA.get().defaultBlockState());
                            }
                        } else if (has(ExAttrib.LAVA_V)) {
                            this.worldObj.setBlockAndUpdate(pos, ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState());
                        } else if (has(ExAttrib.LAVA_R)) {
                            this.worldObj.setBlockAndUpdate(pos, ModBlocks.RAD_LAVA_BLOCK.get().defaultBlockState());
                        }
                    }
                }
            }
        }

        if (has(ExAttrib.FIRE) || has(ExAttrib.BALEFIRE) || has(ExAttrib.LAVA)) {
            for (BlockPos pos : this.affectedBlockPositions) {
                BlockState block = this.worldObj.getBlockState(pos);
                BlockState block1 = this.worldObj.getBlockState(pos.below());
                boolean shouldReplace = true;
                if (!has(ExAttrib.ALLMOD) && !has(ExAttrib.DIGAMMA)) shouldReplace = this.explosionRNG.nextInt(3) == 0;

                if (block.isAir() && block1.isSolidRender(worldObj, pos.below()) && shouldReplace) {
                    if (has(ExAttrib.FIRE)) this.worldObj.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    else if (has(ExAttrib.BALEFIRE)) this.worldObj.setBlockAndUpdate(pos, ModBlocks.BALEFIRE.get().defaultBlockState());
                    else if (has(ExAttrib.LAVA)) this.worldObj.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                }
            }
        }
    }

    public Map<Player, Vec3> getHitPlayers() {
        return this.affectedEntities;
    }

    public LivingEntity getExplosivePlacedBy() {
        if (this.exploder == null) return null;
        if (this.exploder instanceof PrimedTnt tnt) return tnt.getOwner();
        return this.exploder instanceof LivingEntity le ? le : null;
    }

    public boolean has(ExAttrib attrib) {
        return this.atttributes.contains(attrib);
    }

    public enum ExAttrib {
        FIRE,            // classic vanilla fire explosion
        BALEFIRE,        // same with but with balefire
        DIGAMMA,
        DIGAMMA_CIRCUIT,
        LAVA,            // again the same thing but lava
        LAVA_V,          // again the same thing but volcanic lava
        LAVA_R,          // again the same thing but radioactive lava
        ERRODE,          // will turn select blocks into gravel or sand
        ALLMOD,          // block placer attributes like fire are applied for all destroyed blocks
        ALLDROP,         // miner TNT!
        NODROP,          // the opposite
        NOPARTICLE,
        NOSOUND,
        NOHURT
    }

    /** Original {@code errosion}: Beton zu Kies, Betonziegel zu zerbrochenen Betonziegeln. */
    private static final Map<Supplier<Block>, Supplier<Block>> EROSION = new HashMap<>();
    static {
        EROSION.put(() -> ModBlocks.CONCRETE.get(), () -> Blocks.GRAVEL);
        EROSION.put(() -> ModBlocks.BRICK_CONCRETE.get(), () -> ModBlocks.BRICK_CONCRETE_BROKEN.get());
        EROSION.put(() -> ModBlocks.BRICK_CONCRETE_BROKEN.get(), () -> Blocks.GRAVEL);
    }

    public static Block erodesInto(Block block) {
        for (Map.Entry<Supplier<Block>, Supplier<Block>> e : EROSION.entrySet()) {
            if (e.getKey().get() == block) return e.getValue().get();
        }
        return null;
    }

    /** Fuer die Deko-Runde: {@code concrete_smooth} kommt hinzu, sobald der Block existiert. */
    public static void addErosion(Supplier<Block> from, Supplier<Block> into) {
        EROSION.put(from, into);
    }
}
