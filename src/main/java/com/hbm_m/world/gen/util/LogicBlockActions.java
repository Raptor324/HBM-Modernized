package com.hbm_m.world.gen.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import com.hbm_m.api.tile.ILockableTile;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockChargeBase;
import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.blockentity.bomb.ChargeBlockEntity;
import com.hbm_m.blockentity.crates.BaseCrateBlockEntity;
import com.hbm_m.blockentity.decorations.SkeletonHolderBlockEntity;
import com.hbm_m.blockentity.generic.LogicBlockEntity;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.missile.MissileTier2;
import com.hbm_m.entity.mob.EntityUndeadSoldier;
import com.hbm_m.entity.mob.ai.EntityAIFireGun;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.MobUtil;
import com.hbm_m.util.Vec3NT;
import com.hbm_m.util.WorldUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.world.gen.util.LogicBlockActions}: Aktionen der Logikbloecke, laufen jeden Tick vor der Bedingung.
 */
public class LogicBlockActions {

    public static LinkedHashMap<String, Consumer<LogicBlockEntity>> actions;

    /** {@code World.getHeightValue}. */
    private static int height(Level world, int x, int z) {
        return world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** {@code Block.getExplosionResistance(null)}. */
    private static float resistance(Level world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock().getExplosionResistance();
    }

    /** {@code getCanSpawnHere} + {@code onSpawnWithEgg(null)} fuer die Untoten-Soldaten. */
    private static boolean canSpawnHere(Level world, EntityUndeadSoldier mob) {
        return mob.checkSpawnRules(world, MobSpawnType.EVENT) && mob.checkSpawnObstruction(world);
    }

    private static void onSpawnWithEgg(Level world, EntityUndeadSoldier mob) {
        if (world instanceof ServerLevel server) {
            //? if < 1.21.1 {
            mob.finalizeSpawn(server, server.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
            //?} else {
            /*mob.finalizeSpawn(server, server.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
            *///?}
        }
    }

    private static void chatClosest(Level world, int x, int y, int z, double range, Component msg) {
        Player closest = world.getNearestPlayer(x, y, z, range, false);
        if (closest != null) closest.sendSystemMessage(msg);
    }

    private static Component prefixed(String prefix, String text) {
        return Component.literal(prefix).withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal(text).withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */);
    }

    private static ItemStack byId(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void setSkeletonItem(Level world, BlockPos pos, ItemStack stack) {
        if (world.getBlockEntity(pos) instanceof SkeletonHolderBlockEntity skeleton) {
            skeleton.setItem(stack);
        }
    }

    public static Consumer<LogicBlockEntity> PHASE_ABERRATOR = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();
        if (tile.phase == 1 || tile.phase == 2) {
            tile.player = world.getNearestPlayer(x, y, z, 25, false);
            if (tile.timer == 0) {
                Vec3NT vec = new Vec3NT(20, 0, 0);
                for (int i = 0; i < 10; i++) {

                    if (vec.xCoord > 8) vec.xCoord += world.random.nextInt(10) - 5;

                    EntityUndeadSoldier mob = new EntityUndeadSoldier(world);
                    for (int j = 0; j < 7; j++) {
                        mob.moveTo(x + 0.5 + vec.xCoord, height(world, (int) (x + 0.5 + vec.xCoord), (int) (z + 0.5 + vec.zCoord)), z + 0.5 + vec.zCoord, i * 36F, 0);
                        if (canSpawnHere(world, mob)) {
                            onSpawnWithEgg(world, mob);
                            if (tile.player != null) {
                                mob.setTarget(tile.player);
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
            if (world.getBlockEntity(skull) instanceof SkeletonHolderBlockEntity) {
                if (world.random.nextInt(5) == 0) {
                    setSkeletonItem(world, skull, new ItemStack(ModItems.ITEM_SECRET_ABERRATOR.get()));
                } else {
                    // clay_tablet Meta 1
                    setSkeletonItem(world, skull, new ItemStack(ModItems.CLAY_TABLET_1.get()));
                }
            }
            world.setBlock(tile.getBlockPos(), Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
    };

    private static void collapseRoof(LogicBlockEntity tile, int r) {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();

        if (tile.phase == 0) return;

        // aus explosionChaos uebernommen
        int r2 = r * r;
        int r22 = r2 / 2;

        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {

                        if (resistance(world, X, Y, Z) <= 70) {
                            BlockPos pos = new BlockPos(X, Y, Z);
                            BlockState state = world.getBlockState(pos);
                            // Original EntityFallingBlockNT; Luft faellt nicht
                            if (!state.isAir() && pos.equals(tile.getBlockPos()) == false) {
                                FallingBlockEntity.fall(world, pos, state);
                            }
                        }
                    }
                }
            }
        }
        world.setBlock(tile.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
    }

    public static Consumer<LogicBlockEntity> COLLAPSE_ROOF_RAD_5 = (tile) -> collapseRoof(tile, 4);

    public static Consumer<LogicBlockEntity> COLLAPSE_ROOF_RAD_10 = (tile) -> collapseRoof(tile, 8);

    public static Consumer<LogicBlockEntity> FODDER_WAVE = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int z = tile.getBlockPos().getZ();
        if (tile.phase == 1) {
            Vec3NT vec = new Vec3NT(5, 0, 0);
            for (int i = 0; i < 10; i++) {
                Zombie mob = new Zombie(world);
                mob.moveTo(x + 0.5 + vec.xCoord, height(world, x, z), z + 0.5 + vec.zCoord, i * 36F, 0);
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolAdv, new Random());
                world.addFreshEntity(mob);

                vec.rotateAroundYDeg(36D);
            }
            world.setBlock(tile.getBlockPos(), ModBlocks.getIngotBlock(ModMaterials.STEEL).get().defaultBlockState(), 3);
        }
    };

    private static Skeleton skeleton(Level world, LogicBlockEntity tile) {
        Skeleton mob = new Skeleton(EntityType.SKELETON, world);
        mob.moveTo(tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ(), 0, 0);
        return mob;
    }

    public static Consumer<LogicBlockEntity> SKELETONS_GUN_TIER_1 = (tile) -> {
        Level world = tile.getWorldObj();
        if (tile.phase == 1) {
            for (int i = 0; i < 3; i++) {
                Skeleton mob = skeleton(world, tile);
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolGunsTier1, new Random());
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolMasks, new Random());
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolRanged, new Random());
                world.addFreshEntity(mob);
                world.setBlock(tile.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    };

    public static Consumer<LogicBlockEntity> SKELETONS_GUN_TIER_2 = (tile) -> {
        Level world = tile.getWorldObj();
        if (tile.phase == 1) {
            for (int i = 0; i < 3; i++) {
                Skeleton mob = skeleton(world, tile);
                EntityAIFireGun gunTask = new EntityAIFireGun(mob);
                gunTask.minWait = 4;
                gunTask.maxWait = 5;
                gunTask.maxRange = 50;
                gunTask.burstTime = 6;
                gunTask.inaccuracy = 5F;
                gunTask.randomBurst = false;
                MobUtil.addFireTask(mob, gunTask);
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolGunsTier2, new Random());
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolRanged, new Random());
                world.addFreshEntity(mob);
                world.setBlock(tile.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    };

    public static Consumer<LogicBlockEntity> SKELETONS_GUN_TIER_3 = (tile) -> {
        Level world = tile.getWorldObj();
        if (tile.phase == 1) {
            for (int i = 0; i < 3; i++) {
                Skeleton mob = skeleton(world, tile);
                EntityAIFireGun gunTask = new EntityAIFireGun(mob);
                gunTask.minWait = 4;
                gunTask.maxWait = 5;
                gunTask.maxRange = 100;
                gunTask.burstTime = 6;
                gunTask.inaccuracy = 1F;
                gunTask.randomBurst = false;
                MobUtil.addFireTask(mob, gunTask);
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolGunsTier3, new Random());
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolAdvRanged, new Random());
                world.addFreshEntity(mob);
                world.setBlock(tile.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    };

    public static Consumer<LogicBlockEntity> ZOMBIES_TIER_1 = (tile) -> {
        Level world = tile.getWorldObj();
        if (tile.phase == 1) {
            for (int i = 0; i < 3; i++) {
                Zombie mob = new Zombie(world);
                mob.moveTo(tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ(), 0, 0);
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolCommon, new Random());
                world.addFreshEntity(mob);
                world.setBlock(tile.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    };

    public static Consumer<LogicBlockEntity> ZOMBIES_TIER_2 = (tile) -> {
        Level world = tile.getWorldObj();
        if (tile.phase == 1) {
            for (int i = 0; i < 3; i++) {
                Zombie mob = new Zombie(world);
                mob.moveTo(tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ(), 0, 0);
                MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolAdv, new Random());
                world.addFreshEntity(mob);
                world.setBlock(tile.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    };

    public static Consumer<LogicBlockEntity> PUZZLE_TEST = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int z = tile.getBlockPos().getZ();

        if (tile.phase == 2) {
            BlockPos pos = tile.getBlockPos();
            world.setBlock(pos, ModBlocks.CRATE_STEEL.get().defaultBlockState(), 3);

            LightningBolt blitz = EntityType.LIGHTNING_BOLT.create(world);
            if (blitz != null) {
                blitz.moveTo(x, height(world, x, z) + 2, z);
                world.addFreshEntity(blitz);
            }

            if (world.getBlockEntity(pos) instanceof BaseCrateBlockEntity crate) {
                crate.getItemHandler().setStackInSlot(15, new ItemStack(WeaponItems.gun("gun_bolter")));
            }
        }
    };

    public static Consumer<LogicBlockEntity> MISSILE_STRIKE = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();

        if (tile.phase != 1) return;

        chatClosest(world, x, y, z, 25, prefixed("[COMMAND UNIT]", " Missile Fired"));

        ForgeDirection parallel = tile.direction.getRotation(ForgeDirection.UP);

        MissileTier2.MissileStrong missile = ModEntities.MISSILE_STRONG.get().create(world);
        if (missile != null) {
            missile.initLaunch(
                    x + tile.direction.offsetX * 300,
                    200,
                    z + tile.direction.offsetZ * 300,
                    x + parallel.offsetX * 30 + tile.direction.offsetX * 30,
                    z + parallel.offsetZ * 30 + tile.direction.offsetZ * 30);
            WorldUtil.loadAndSpawnEntityInWorld(missile);
        }

        world.setBlock(tile.getBlockPos(), ModBlocks.BLOCK_ELECTRICAL_SCRAP.get().defaultBlockState(), 3);
    };

    public static Consumer<LogicBlockEntity> RAD_CONTAINMENT_SYSTEM = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();

        ForgeDirection direction = tile.direction.getOpposite();
        ForgeDirection rot = direction.getRotation(ForgeDirection.UP);

        List<LivingEntity> entities = LogicBlockConditions.entities(world, LivingEntity.class,
                x - rot.offsetX, y - 1, z - rot.offsetZ,
                x + rot.offsetX + direction.offsetX * 15, y + 1, z + rot.offsetZ + direction.offsetZ * 15,
                2, 2, 2);

        for (LivingEntity e : entities) {

            Vec3 vec = new Vec3(e.getX() - (x + 0.5), (e.getY() + e.getEyeHeight()) - (y + 0.5), e.getZ() - (z + 0.5));
            double len = vec.length();
            vec = vec.normalize();

            len = Math.max(len, 1D);

            float res = 0;

            for (int i = 1; i < len; i++) {

                int ix = (int) Math.floor(x + 0.5 + vec.x * i);
                int iy = (int) Math.floor(y + 0.5 + vec.y * i);
                int iz = (int) Math.floor(z + 0.5 + vec.z * i);

                res += resistance(world, ix, iy, iz);
            }

            if (res < 1)
                res = 1;

            float eRads = 100F;
            eRads /= (float) res;
            eRads /= (float) (len * len);

            ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.HAZMAT2, eRads);
        }

        if (tile.phase == 2 && tile.timer > 40) {
            chatClosest(world, x, y, z, 25, prefixed("[RAD CONTAINMENT SYSTEM]", " Diagnostics found containment failure, commencing lockdown"));

            for (int i = 1; i < 20; i++) {
                BlockPos check = new BlockPos(x + direction.offsetX * i, y + 1, z + direction.offsetZ * i);
                BlockEntity te = world.getBlockEntity(check);
                // Original BlockDummyable.findCore: der Port fuehrt jeden Teil der Tuer zum Steuerblock
                if (te instanceof DoorBlockEntity part) {
                    DoorBlockEntity door = part.getController();
                    if (door == null) door = part;
                    door.setPins(456);
                    door.close();
                    door.lock();
                    break;
                }
            }

            tile.phase = 3;
        }
    };

    public static Consumer<LogicBlockEntity> POWER_LOCK = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();

        if (tile.phase == 0 && !LogicBlockConditions.players(world, x, y, z, x + 1, y - 2, z + 1, 3, 3, 3).isEmpty()) {
            chatClosest(world, x, y, z, 300, prefixed("[POWER LOCK]", " Low Power Warning! Locking Safe"));
            tile.phase++;

            ILockableTile safe = null;

            for (Direction dir : Direction.values()) {
                if (world.getBlockEntity(tile.getBlockPos().relative(dir)) instanceof ILockableTile lockable) {
                    safe = lockable;
                    break;
                }
            }
            if (safe != null) {
                safe.setPins(world.random.nextInt(999));
            }
        }
    };

    public static Consumer<LogicBlockEntity> BOMB_TRAP = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();

        ForgeDirection direction = tile.direction.getOpposite();

        if (tile.phase == 1) {
            BlockPos bombPos = new BlockPos(x, y, z + direction.offsetZ);
            // Meta 2 = Norden
            world.setBlock(bombPos, ModBlocks.CHARGE_C4.get().defaultBlockState().setValue(BlockChargeBase.FACING, Direction.NORTH), 3);

            if (world.getBlockEntity(bombPos) instanceof ChargeBlockEntity bomb) {
                bomb.timer = 2400;
                bomb.started = true;
            }

            world.setBlock(tile.getBlockPos(), tile.disguise != null ? tile.disguise.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
    };

    public static Consumer<LogicBlockEntity> BOMB_CRANE = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();
        BlockPos up = new BlockPos(x, y + 1, z);

        if (tile.phase == 0) {
            world.setBlock(up, ModBlocks.CHARGE_C4.get().defaultBlockState().setValue(BlockChargeBase.FACING, Direction.UP), 3);

            if (world.getBlockEntity(up) instanceof ChargeBlockEntity bomb) {
                bomb.timer = 1200;
            }
        }

        if (tile.phase >= 1) {
            if (world.getBlockEntity(up) instanceof ChargeBlockEntity bomb) {
                bomb.started = true;
            }
            world.setBlock(tile.getBlockPos(), ModBlocks.getIngotBlock(ModMaterials.STEEL).get().defaultBlockState(), 3);
        }
    };

    public static Consumer<LogicBlockEntity> DEAD_GUY_CRANE = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();
        if (tile.phase == 1) {
            BlockPos pos = tile.getBlockPos();
            List<Player> players = LogicBlockConditions.players(world, x, y, z, x + 1, y - 2, z + 1, 25, 25, 25);
            Player player = players.isEmpty() ? null : players.get(0);
            world.setBlock(pos, ModBlocks.SKELETON_HOLDER.get().defaultBlockState(), 3);

            if (world.getBlockEntity(pos) instanceof SkeletonHolderBlockEntity) {
                Item hangman = WeaponItems.gun("gun_hangman");
                if (player != null && player.getInventory().contains(new ItemStack(hangman))) {
                    setSkeletonItem(world, pos, new ItemStack(ModItems.CLAY_TABLET.get()));
                } else {
                    setSkeletonItem(world, pos, new ItemStack(hangman));
                }
            }
        }
    };

    public static Consumer<LogicBlockEntity> DEAD_GUY_BASE_TOWER = (tile) -> {
        Level world = tile.getWorldObj();
        if (tile.phase == 1) {
            BlockPos pos = tile.getBlockPos();
            world.setBlock(pos, ModBlocks.SKELETON_HOLDER.get().defaultBlockState(), 3);

            if (world.getBlockEntity(pos) instanceof SkeletonHolderBlockEntity) {
                int roll = world.random.nextInt(100);

                // Original ammo_standard Meta 91 / 92 / 73 = CT_MORTAR / CT_MORTAR_CHARGE / NUKE_STANDARD
                if (roll < 44) {
                    setSkeletonItem(world, pos, byId("ammo_standard_ct_mortar", 3));
                } else if (roll < 71) {
                    setSkeletonItem(world, pos, byId("ammo_standard_ct_mortar_charge", 1));
                } else if (roll < 92) {
                    setSkeletonItem(world, pos, byId("ammo_standard_nuke_standard", 1));
                } else if (roll < 96) {
                    setSkeletonItem(world, pos, new ItemStack(ModItems.ITEM_SECRET_ABERRATOR.get()));
                } else {
                    setSkeletonItem(world, pos, new ItemStack(ModItems.ITEM_SECRET_FOLLY.get()));
                }
            }
        }
    };

    public static List<String> getActionNames() {
        return new ArrayList<>(actions.keySet());
    }

    // neue Aktionen hier registrieren
    static {
        initialize();
    }

    public static void initialize() {
        actions = new LinkedHashMap<>();
        // Logikaktionen
        actions.put("FODDER_WAVE", FODDER_WAVE);
        actions.put("POWER_LOCK", POWER_LOCK);
        actions.put("COLLAPSE_ROOF_RAD_5", COLLAPSE_ROOF_RAD_5);
        actions.put("COLLAPSE_ROOF_RAD_10", COLLAPSE_ROOF_RAD_10);
        actions.put("BOMB_TRAP", BOMB_TRAP);
        actions.put("BOMB_CRANE", BOMB_CRANE);
        actions.put("DEAD_GUY_CRANE", DEAD_GUY_CRANE);
        //actions.put("DEAD_GUY_TOWER_BASE", DEAD_GUY_TOWER_BASE);

        // Mob-Aktionen
        actions.put("SKELETON_GUN_TIER_1", SKELETONS_GUN_TIER_1);
        actions.put("SKELETON_GUN_TIER_2", SKELETONS_GUN_TIER_2);
        actions.put("SKELETON_GUN_TIER_3", SKELETONS_GUN_TIER_3);

        actions.put("ZOMBIE_TIER_1", ZOMBIES_TIER_1);
        actions.put("ZOMBIE_TIER_2", ZOMBIES_TIER_2);

        // Beispielaktionen
        actions.put("ABERRATOR", PHASE_ABERRATOR);
        actions.put("PUZZLE_TEST", PUZZLE_TEST);
        actions.put("MISSILE_STRIKE", MISSILE_STRIKE);
        actions.put("IRRADIATE_ENTITIES_AOE", RAD_CONTAINMENT_SYSTEM);
    }
}
