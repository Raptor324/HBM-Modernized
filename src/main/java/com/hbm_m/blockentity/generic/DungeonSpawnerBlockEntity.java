package com.hbm_m.blockentity.generic;

import java.util.function.Consumer;
import java.util.function.Function;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.SkeletonHolderBlockEntity;
import com.hbm_m.entity.mob.EntityUndeadSoldier;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.Vec3NT;
import com.hbm_m.world.gen.util.LogicBlockConditions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code DungeonSpawner.TileEntityDungeonSpawner} (tileentity_ntm_dungeon_spawner): Phasengesteuerter Spawner
 * (Typ ABERRATOR) - Spieler naehert sich, zwei Wellen Untoter, danach Belohnung im Skeletthalter 18 Bloecke darueber
 * und der Spawner wird zu Obsidian.
 */
public class DungeonSpawnerBlockEntity extends BaseHbmBlockEntity {

    public int phase = 0;
    public int timer = 0;
    public EnumSpawnerType type = EnumSpawnerType.ABERRATOR;

    public DungeonSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DUNGEON_SPAWNER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DungeonSpawnerBlockEntity tile) {
        if (!level.isClientSide) {
            tile.type.phase.accept(tile);
            if (tile.isRemoved()) return;
            if (tile.type.phaseCondition.apply(tile)) {
                tile.phase++;
                tile.timer = 0;
            } else {
                tile.timer++;
            }
        }
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putInt("phase", phase);
        nbt.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void readNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        this.phase = nbt.getInt("phase");
        int t = nbt.getByte("type");
        EnumSpawnerType[] values = EnumSpawnerType.values();
        this.type = values[Math.abs(t) % values.length];
    }

    public enum EnumSpawnerType {

        ABERRATOR(DungeonSpawnerBlockEntity::conAberrator, DungeonSpawnerBlockEntity::phaseAberrator);

        public final Function<DungeonSpawnerBlockEntity, Boolean> phaseCondition;
        public final Consumer<DungeonSpawnerBlockEntity> phase;

        EnumSpawnerType(Function<DungeonSpawnerBlockEntity, Boolean> con, Consumer<DungeonSpawnerBlockEntity> ph) {
            this.phaseCondition = con;
            this.phase = ph;
        }
    }

    public static boolean conAberrator(DungeonSpawnerBlockEntity tile) {
        Level world = tile.level;
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        int x = tile.worldPosition.getX();
        int y = tile.worldPosition.getY();
        int z = tile.worldPosition.getZ();
        if (tile.phase == 0) {
            if (world.getGameTime() % 20 != 0) return false;
            return !LogicBlockConditions.players(world, x, y, z, x + 1, y - 2, z + 1, 20, 10, 20).isEmpty();
        }
        if (tile.phase < 3) {
            if (world.getGameTime() % 20 != 0 || tile.timer < 60) return false;
            return world.getEntitiesOfClass(EntityUndeadSoldier.class, LogicBlockConditions.box(x, y, z, x - 2, y + 1, z + 1, 50, 20, 50)).isEmpty();
        }
        return false;
    }

    public static void phaseAberrator(DungeonSpawnerBlockEntity tile) {
        Level world = tile.level;
        int x = tile.worldPosition.getX();
        int y = tile.worldPosition.getY();
        int z = tile.worldPosition.getZ();
        if (tile.phase == 1 || tile.phase == 2) {
            if (tile.timer == 0) {
                Vec3NT vec = new Vec3NT(10, 0, 0);
                for (int i = 0; i < 10; i++) {
                    EntityUndeadSoldier mob = new EntityUndeadSoldier(world);
                    for (int j = 0; j < 7; j++) {
                        mob.moveTo(x + 0.5 + vec.xCoord, y - 5, z + 0.5 + vec.zCoord, i * 36F, 0);
                        if (mob.checkSpawnRules(world, MobSpawnType.EVENT) && mob.checkSpawnObstruction(world)) {
                            if (world instanceof ServerLevel server) {
                                //? if < 1.21.1 {
                                mob.finalizeSpawn(server, server.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
                                //?} else {
                                /*mob.finalizeSpawn(server, server.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
                                *///?}
                            }
                            world.addFreshEntity(mob);
                            break;
                        }
                    }

                    vec.rotateAroundYDeg(36D);
                }
            }
        }
        if (tile.phase > 2) {
            BlockPos skull = new BlockPos(x, y + 18, z);
            if (world.getBlockEntity(skull) instanceof SkeletonHolderBlockEntity skeleton) {
                if (world.random.nextInt(5) == 0) {
                    skeleton.setItem(new ItemStack(ModItems.ITEM_SECRET_ABERRATOR.get()));
                } else {
                    // clay_tablet Meta 1
                    skeleton.setItem(new ItemStack(ModItems.CLAY_TABLET_1.get()));
                }
            }
            world.setBlock(tile.worldPosition, Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
    }
}
