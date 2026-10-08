package com.hbm_m.blockentity.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import com.hbm_m.block.generic.GlyphidSpawnerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.MobConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.glyphid.EntityGlyphid;
import com.hbm_m.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code BlockGlyphidSpawner.TileEntityGlpyhidSpawner}: beim ersten Tick und danach alle {@code swarmCooldown}
 * Ticks ein Schwarm, sofern weniger als {@code spawnMax} Glyphiden existieren und hoechstens drei im Nest sitzen
 * (radioaktive Nester immer). Schwarmgroesse und Arten haengen am Russ; bei genug Russ gelegentlich ein Spaeher.
 */
public class GlyphidSpawnerBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {

    private record SpawnEntry(Supplier<? extends EntityType<? extends EntityGlyphid>> type, Supplier<int[]> chance) { }

    private static final List<SpawnEntry> spawnMap = new ArrayList<>();

    static {
        // big thanks to martin for the suggestion of using functions
        spawnMap.add(new SpawnEntry(() -> ModEntities.GLYPHID.get(), MobConfig::glyphidChance));
        spawnMap.add(new SpawnEntry(() -> ModEntities.GLYPHID_BOMBARDIER.get(), MobConfig::bombardierChance));
        spawnMap.add(new SpawnEntry(() -> ModEntities.GLYPHID_BRAWLER.get(), MobConfig::brawlerChance));
        spawnMap.add(new SpawnEntry(() -> ModEntities.GLYPHID_DIGGER.get(), MobConfig::diggerChance));
        spawnMap.add(new SpawnEntry(() -> ModEntities.GLYPHID_BLASTER.get(), MobConfig::blasterChance));
        spawnMap.add(new SpawnEntry(() -> ModEntities.GLYPHID_BEHEMOTH.get(), MobConfig::behemothChance));
        spawnMap.add(new SpawnEntry(() -> ModEntities.GLYPHID_BRENDA.get(), MobConfig::brendaChance));
        spawnMap.add(new SpawnEntry(() -> ModEntities.GLYPHID_NUCLEAR.get(), MobConfig::johnsonChance));
    }

    boolean initialSpawn = true;

    public GlyphidSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GLYPHID_SPAWNER_BE.get(), pos, state);
    }

    private int getBlockMetadata() {
        BlockState state = getBlockState();
        return state.hasProperty(GlyphidSpawnerBlock.TYPE) ? state.getValue(GlyphidSpawnerBlock.TYPE) : 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GlyphidSpawnerBlockEntity be) {
        be.updateEntity((ServerLevel) level, pos);
    }

    private void updateEntity(ServerLevel world, BlockPos pos) {

        if (world.getDifficulty() != Difficulty.PEACEFUL) {

            if (initialSpawn || world.getGameTime() % MobConfig.swarmCooldown() == 0) {

                initialSpawn = false;
                setChanged();
                int count = 0;

                for (Entity e : world.getAllEntities()) {
                    if (e instanceof EntityGlyphid) {
                        count++;
                        if (count >= MobConfig.spawnMax()) return;
                    }
                }

                List<EntityGlyphid> list = world.getEntitiesOfClass(EntityGlyphid.class, new AABB(pos.getX() - 5, pos.getY() + 1, pos.getZ() - 5, pos.getX() + 6, pos.getY() + 7, pos.getZ() + 6));
                float soot = PollutionHandler.getPollution(world, pos.getX(), pos.getY(), pos.getZ(), PollutionType.SOOT);

                int subtype = this.getBlockMetadata();
                if (list.size() <= 3 || subtype == EntityGlyphid.TYPE_RADIOACTIVE) {

                    ArrayList<EntityGlyphid> currentSwarm = createSwarm(world, soot, subtype);

                    for (EntityGlyphid glyphid : currentSwarm) {
                        trySpawnEntity(world, pos, glyphid);
                    }

                    if (!initialSpawn && world.random.nextInt(MobConfig.scoutSwarmSpawnChance() + 1) == 0 && soot >= MobConfig.scoutThreshold() && subtype != EntityGlyphid.TYPE_RADIOACTIVE) {
                        EntityGlyphidScout scout = new EntityGlyphidScout(ModEntities.GLYPHID_SCOUT.get(), world);
                        if (subtype == 1) scout.setSubtype(EntityGlyphid.TYPE_INFECTED);
                        trySpawnEntity(world, pos, scout);
                    }
                }
            }
        }
    }

    public void trySpawnEntity(Level world, BlockPos pos, EntityGlyphid glyphid) {
        double offsetX = glyphid.getRandom().nextGaussian() * 3;
        double offsetZ = glyphid.getRandom().nextGaussian() * 3;

        for (int i = 0; i < 7; i++) {
            glyphid.moveTo(pos.getX() + 0.5 + offsetX, pos.getY() - 2 + i, pos.getZ() + 0.5 + offsetZ, world.random.nextFloat() * 360.0F, 0.0F);
            if (glyphid.getCanSpawnHere()) {
                world.addFreshEntity(glyphid);
                return;
            }
        }
    }

    public ArrayList<EntityGlyphid> createSwarm(Level world, float soot, int meta) {

        Random rand = new Random();
        ArrayList<EntityGlyphid> currentSpawns = new ArrayList<>();
        int swarmAmount = (int) Math.min(MobConfig.baseSwarmSize() * Math.max(MobConfig.swarmScalingMult() * (soot / MobConfig.sootStep()), 1), 10);
        int cap = 100;

        while (currentSpawns.size() <= swarmAmount && cap >= 0) {
            // (dys)functional programing
            for (SpawnEntry glyphid : spawnMap) {
                int[] chance = glyphid.chance().get();
                int adjustedChance = (int) (chance[0] + (chance[1] - chance[1] / Math.max(((soot + 1) / 3), 1)));
                if (soot >= chance[2] && rand.nextInt(100) <= adjustedChance) {
                    EntityGlyphid entity = glyphid.type().get().create(world);
                    if (entity == null) continue;
                    if (meta == 1) entity.setSubtype(EntityGlyphid.TYPE_INFECTED);
                    if (meta == 2) entity.setSubtype(EntityGlyphid.TYPE_RADIOACTIVE);
                    currentSpawns.add(entity);
                }
            }

            cap--;
        }
        return currentSpawns;
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("initialSpawn", initialSpawn);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.initialSpawn = nbt.getBoolean("initialSpawn");
    }
}
