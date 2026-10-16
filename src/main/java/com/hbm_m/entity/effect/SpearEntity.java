package com.hbm_m.entity.effect;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1:1 port of {@code com.hbm.entity.effect.EntitySpear} - the digamma lance that descends on a
 * reactor that melted down while carrying digamma-contaminated fuel.
 *
 * <p>It falls straight down at a fixed 0.2 blocks per tick. On the way it repeatedly detonates a
 * silent, dropless digamma blast at a random point up to ~25 blocks away, converting terrain to
 * digamma ash (or, close in, to the debris lattice the original calls the "circuit" pattern) and
 * dosing every player in the world. Once it hits ground it stands there for a hundred ticks and
 * then discharges: everything alive takes a lethal digamma dose, and the flash is audible across
 * the entire world.</p>
 */
public class SpearEntity extends Entity {

    /** Synced so the renderer can fade the lance in as it settles. */
    private static final EntityDataAccessor<Integer> TICKS_IN_GROUND =
            SynchedEntityData.defineId(SpearEntity.class, EntityDataSerializers.INT);

    /** The original's blast: 7.5 radius, NOHURT + NOPARTICLE + NODROP + NOSOUND + DIGAMMA. */
    private static final float BLAST_RADIUS = 7.5F;
    /** Inside this distance the blast lays the "circuit" lattice instead of plain ash. */
    private static final double CIRCUIT_RANGE = 20.0D;
    private static final int DISCHARGE_TICKS = 100;

    public SpearEntity(EntityType<? extends SpearEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TICKS_IN_GROUND, 0);
    }

    public int getTicksInGround() {
        return this.entityData.get(TICKS_IN_GROUND);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.setDeltaMovement(0.0D, -0.2D, 0.0D);

        BlockPos below = BlockPos.containing(this.getX(), this.getY() - 1, this.getZ());

        if (this.level().getBlockState(below).isAir()) {
            this.setPos(this.getX(), this.getY() - 0.2D, this.getZ());

            if (this.level() instanceof ServerLevel server) {
                descentBlast(server);
                for (net.minecraft.world.entity.player.Player player : server.players()) {
                    ContaminationUtil.contaminate(player, HazardType.DIGAMMA, ContaminationType.DIGAMMA, 0.05F);
                    com.hbm_m.advancement.ModAdvancements.grant(player,
                            com.hbm_m.advancement.ModAdvancements.DIGAMMA_KAUAI_MOHO);
                }
                spawnSmoke(server, groundHeight(server, this.getX(), this.getZ()) + 2, 5);
            }

            // Three blocks of clear air underneath means it is still airborne, not resting on a
            // one-block overhang - the original resets the counter in exactly that case.
            if (this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() - 3, this.getZ())).isAir()) {
                this.entityData.set(TICKS_IN_GROUND, 0);
            }
        } else {
            int ticks = getTicksInGround() + 1;
            this.entityData.set(TICKS_IN_GROUND, ticks);

            if (this.level() instanceof ServerLevel server && ticks > DISCHARGE_TICKS) {
                discharge(server);
            }
        }
    }

    /**
     * One digamma blast per tick at a gaussian offset from the lance. Nothing is hurt, dropped or
     * heard - the whole point is the terrain conversion, so this walks the sphere by hand rather
     * than going through a vanilla explosion, which would blow blocks apart instead of replacing
     * them.
     */
    private void descentBlast(ServerLevel server) {
        double ix = this.getX() + this.random.nextGaussian() * 25;
        double iz = this.getZ() + this.random.nextGaussian() * 25;
        double iy = groundHeight(server, ix, iz) + 2;

        double dx = ix - this.getX();
        double dz = iz - this.getZ();
        boolean circuit = Math.sqrt(dx * dx + dz * dz) < CIRCUIT_RANGE;

        BlockPos centre = BlockPos.containing(ix, iy, iz);
        int r = (int) Math.ceil(BLAST_RADIUS);
        BlockState ash = ModBlocks.ASH_DIGAMMA.get().defaultBlockState();
        BlockState debris = ModBlocks.RBMK_DEBRIS_DIGAMMA.get().defaultBlockState();
        BlockState fire = ModBlocks.FIRE_DIGAMMA.get().defaultBlockState();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > BLAST_RADIUS * BLAST_RADIUS) continue;

                    BlockPos pos = centre.offset(x, y, z);
                    BlockState state = server.getBlockState(pos);
                    // isNormalCube: only full solid blocks convert; air and foliage are left be.
                    if (state.isAir() || !state.isCollisionShapeFullBlock(server, pos)) continue;

                    int wx = pos.getX(), wz = pos.getZ();
                    boolean lattice = circuit
                            && ((wx % 3 == 0 && wz % 3 == 0)
                                || ((wx % 3 == 0 || wz % 3 == 0) && this.random.nextBoolean()));

                    if (lattice) {
                        server.setBlock(pos, debris, 3);
                    } else {
                        server.setBlock(pos, ash, 3);
                        if (this.random.nextInt(5) == 0 && server.getBlockState(pos.above()).isAir()) {
                            server.setBlock(pos.above(), fire, 3);
                        }
                    }
                }
            }
        }
    }

    /** The lance has stood long enough: everything alive takes a lethal dose and it winks out. */
    private void discharge(ServerLevel server) {
        for (Entity entity : server.getAllEntities()) {
            if (entity instanceof LivingEntity living) {
                ContaminationUtil.contaminate(living, HazardType.DIGAMMA, ContaminationType.DIGAMMA2, 10F);
            }
        }

        // Volume 25000 in the original: this is meant to be heard from anywhere in the world.
        server.playSound(null, this.getX(), this.getY(), this.getZ(),
                com.hbm_m.sound.ModSounds.D_FLASH.get(), SoundSource.BLOCKS, 25000.0F, 1.0F);
        spawnSmoke(server, this.getY() + 7, 100);
        this.discard();
    }

    /** {@code mode: "radialDigamma"}: {@code count} puffs thrown outward on an even ring. */
    private void spawnSmoke(ServerLevel server, double y, int count) {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        for (int i = 0; i < count; i++) {
            server.sendParticles(com.hbm_m.particle.ModParticleTypes.DIGAMMA_SMOKE.get(),
                    this.getX(), y, this.getZ(), 0,
                    Math.cos(angle) * 2.0D, 0.0D, Math.sin(angle) * 2.0D, 1);
            angle += Math.PI * 2.0D / count;
        }
    }

    private static double groundHeight(ServerLevel server, double x, double z) {
        return server.getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(x), (int) Math.floor(z));
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(TICKS_IN_GROUND, tag.getInt("ticksInGround"));
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ticksInGround", getTicksInGround());
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 25000 * 25000; }
    @Override public float getLightLevelDependentMagicValue() { return 1.0F; }
}
