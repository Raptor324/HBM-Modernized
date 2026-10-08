package com.hbm_m.world.gen.legacy;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Port-Hilfe: die 1.7.10-{@code World}-Aufrufe der alten Weltgen-Klassen ({@code setBlock(x, y, z, block, meta, flag)},
 * {@code getBlock}, {@code getTileEntity}, Hoehenabfragen ...) auf 1.20 abgebildet. Die Klassen wurden aus dem Original
 * zeilengetreu umgesetzt; aus {@code world.setBlock(...)} wurde {@code L.setBlock(world, ...)}.
 */
public final class L {

    private L() { }

    /** Aktiver Bau-Puffer dieses Threads (siehe {@link VirtualWorld}); null = echte Welt. */
    private static final ThreadLocal<VirtualWorld> VIRTUAL = new ThreadLocal<>();

    public static VirtualWorld virtual() {
        return VIRTUAL.get();
    }

    /** Fuehrt {@code job} mit allen L-Zugriffen auf {@code vw} aus. */
    public static void runVirtual(VirtualWorld vw, Runnable job) {
        VirtualWorld old = VIRTUAL.get();
        VIRTUAL.set(vw);
        try {
            job.run();
        } finally {
            VIRTUAL.set(old);
        }
    }

    /** Weltgen-Bereiche ausserhalb des schreibbaren 3x3-Chunkfensters werden still uebersprungen. */
    private static boolean writable(LevelAccessor world, BlockPos pos) {
        if (world instanceof WorldGenRegion region) {
            // Merkmale duerfen nur in den Chunks um den Mittelchunk schreiben (Radius 1)
            net.minecraft.world.level.ChunkPos c = region.getCenter();
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            return Math.abs(cx - c.x) <= 1 && Math.abs(cz - c.z) <= 1;
        }
        return true;
    }

    public static boolean setBlock(LevelAccessor world, int x, int y, int z, LB block, int meta, int flag) {
        return setState(world, x, y, z, block.state(meta), flag);
    }

    public static boolean setBlock(LevelAccessor world, int x, int y, int z, LB block) {
        return setBlock(world, x, y, z, block, 0, 3);
    }

    public static boolean setState(LevelAccessor world, int x, int y, int z, BlockState state, int flag) {
        VirtualWorld vw = VIRTUAL.get();
        if (vw != null) return vw.setState(x, y, z, state);
        BlockPos pos = new BlockPos(x, y, z);
        if (y < world.getMinBuildHeight() || y >= world.getMaxBuildHeight()) return false;
        if (!writable(world, pos)) return false;
        return world.setBlock(pos, state, (world instanceof WorldGenLevel && !(world instanceof net.minecraft.world.level.Level)) ? 2 : (flag == 3 ? 3 : 2));
    }

    public static boolean setBlockToAir(LevelAccessor world, int x, int y, int z) {
        return setState(world, x, y, z, Blocks.AIR.defaultBlockState(), 3);
    }

    /** 1.7.10 {@code setBlockMetadataWithNotify}: gleicher Block, anderes Metadatum. */
    public static boolean setBlockMetadataWithNotify(LevelAccessor world, int x, int y, int z, int meta, int flag) {
        LB lb = getBlock(world, x, y, z);
        return setState(world, x, y, z, lb.state(meta), flag);
    }

    public static BlockState getState(LevelAccessor world, int x, int y, int z) {
        VirtualWorld vw = VIRTUAL.get();
        if (vw != null) return vw.getState(x, y, z);
        if (world instanceof WorldGenRegion region && !region.hasChunk(x >> 4, z >> 4)) return Blocks.AIR.defaultBlockState();
        return world.getBlockState(new BlockPos(x, y, z));
    }

    /** {@code getBlock(...).isReplaceableOreGen(world, x, y, z, target)}. */
    public static boolean isReplaceableOreGen(LevelAccessor world, int x, int y, int z, LB target) {
        BlockState s = getState(world, x, y, z);
        // 1.7.10 kannte nur "stone"; Granit/Diorit/Andesit/Tiefenschiefer der 1.20-Welt gehoeren dazu
        if (target == VB.stone) return s.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES) || s.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        return s.getBlock() == target.block();
    }

    /** Liegt die Spalte im schreibbaren Bereich (Weltgen-Fenster)? */
    public static boolean canWrite(LevelAccessor world, int x, int z) {
        if (VIRTUAL.get() != null) return true;
        return writable(world, new BlockPos(x, 64, z));
    }

    public static LB getBlock(LevelAccessor world, int x, int y, int z) {
        return LB.forState(getState(world, x, y, z));
    }

    public static int getBlockMetadata(LevelAccessor world, int x, int y, int z) {
        BlockState s = getState(world, x, y, z);
        return LB.forState(s).metaOf(s);
    }

    public static BlockEntity getTileEntity(LevelAccessor world, int x, int y, int z) {
        VirtualWorld vw = VIRTUAL.get();
        if (vw != null) return vw.getBlockEntity(x, y, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!writable(world, pos)) return null;
        return world.getBlockEntity(pos);
    }

    public static boolean isAirBlock(LevelAccessor world, int x, int y, int z) {
        return getState(world, x, y, z).isAir();
    }

    /** 1.7.10 {@code getHeightValue}: erste Luft ueber dem obersten lichtundurchlaessigen Block (inkl. Laub/Wasser). */
    public static int getHeightValue(LevelAccessor world, int x, int z) {
        VirtualWorld vw = VIRTUAL.get();
        if (vw != null) return vw.getHeightValue(x, z);
        if (world instanceof WorldGenRegion region && !region.hasChunk(x >> 4, z >> 4)) return 64;
        return world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** 1.7.10 {@code getTopSolidOrLiquidBlock}: oberster bewegungsblockierender Block ohne Laub, + 1. */
    public static int getTopSolidOrLiquidBlock(LevelAccessor world, int x, int z) {
        VirtualWorld vw = VIRTUAL.get();
        if (vw != null) return vw.getHeightValue(x, z);
        if (world instanceof WorldGenRegion region && !region.hasChunk(x >> 4, z >> 4)) return 64;
        return world.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    public static boolean doesBlockHaveSolidTopSurface(LevelAccessor world, int x, int y, int z) {
        VirtualWorld vw = VIRTUAL.get();
        if (vw != null) return vw.getState(x, y, z).isFaceSturdy(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO, net.minecraft.core.Direction.UP, net.minecraft.world.level.block.SupportType.CENTER);
        return net.minecraft.world.level.block.Block.canSupportCenter(world, new BlockPos(x, y, z), net.minecraft.core.Direction.UP);
    }

    /** {@code world.rand} - ein java.util.Random aus dem Weltzufall. */
    public static Random rand(LevelAccessor world) {
        VirtualWorld vw = VIRTUAL.get();
        if (vw != null) return vw.rand;
        return new Random(world.getRandom().nextLong());
    }

    /** 1.7.10 {@code BiomeGenBase.getFloatTemperature(x, y, z)}: Biomtemperatur, ueber y 64 abnehmend. */
    public static float getFloatTemperature(LevelAccessor world, int x, int y, int z) {
        VirtualWorld vw = VIRTUAL.get();
        float t = vw != null ? vw.biome(x, y, z).temperature : com.hbm_m.world.gen.LegacyBiome.of(world.getBiome(new BlockPos(x, y, z))).temperature;
        if (y > 64) t -= ((float) y - 64.0F) * 0.05F / 30.0F;
        return t;
    }

    /**
     * 1:1 {@code ItemDoor.placeDoorBlock}: unten Ausrichtung {@code rot}, oben Scharnierseite nach den Nachbarbloecken.
     */
    public static void placeDoorBlock(LevelAccessor world, int x, int y, int z, int rot, LB door) {
        byte b0 = 0;
        byte b1 = 0;

        if (rot == 0) b1 = 1;
        if (rot == 1) b0 = -1;
        if (rot == 2) b1 = -1;
        if (rot == 3) b0 = 1;

        int i1 = (getBlock(world, x - b0, y, z - b1).isNormalCube() ? 1 : 0) + (getBlock(world, x - b0, y + 1, z - b1).isNormalCube() ? 1 : 0);
        int j1 = (getBlock(world, x + b0, y, z + b1).isNormalCube() ? 1 : 0) + (getBlock(world, x + b0, y + 1, z + b1).isNormalCube() ? 1 : 0);
        boolean flag = getBlock(world, x - b0, y, z - b1) == door || getBlock(world, x - b0, y + 1, z - b1) == door;
        boolean flag1 = getBlock(world, x + b0, y, z + b1) == door || getBlock(world, x + b0, y + 1, z + b1) == door;
        boolean flag2 = false;

        if (flag && !flag1) {
            flag2 = true;
        } else if (j1 > i1) {
            flag2 = true;
        }

        BlockState lower = door.state(rot & 3);
        setState(world, x, y, z, lower, 2);
        if (lower.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockState upper = lower.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
            if (upper.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOOR_HINGE)) {
                upper = upper.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOOR_HINGE, flag2 ? net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT : net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
                lower = lower.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOOR_HINGE, flag2 ? net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT : net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
                setState(world, x, y, z, lower, 2);
            }
            setState(world, x, y + 1, z, upper, 2);
        }
    }

    /** Forge-1.7.10 {@code ChestGenHooks} (Vanilla-Truhenbeute) -> 1.20-Beutetabelle. */
    public static void setLootTable(Object te, String table, Random rand) {
        if (te instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) {
            c.setLootTable(new net.minecraft.resources.ResourceLocation(table), rand.nextLong());
        }
    }

    /** Forge-1.7.10 {@code DungeonHooks.getRandomDungeonMob}: Skelett 100, Zombie 200, Spinne 100. */
    public static String getRandomDungeonMob(Random rand) {
        int r = rand.nextInt(400);
        if (r < 100) return "Skeleton";
        if (r < 300) return "Zombie";
        return "Spider";
    }

    private static final Random libraryRand = new Random();

    /** 1:1 {@code Library.getRandomConcrete}. */
    public static LB getRandomConcrete() {
        VirtualWorld vw = VIRTUAL.get();
        int i = (vw != null ? vw.libraryRand : libraryRand).nextInt(20);
        if (i <= 1) return MB.brick_concrete_broken;
        if (i <= 4) return MB.brick_concrete_cracked;
        if (i <= 10) return MB.brick_concrete_mossy;
        return MB.brick_concrete;
    }

    /** 1.7.10 {@code TileEntityMobSpawner.func_145881_a().setEntityName(name)}: Spawner-Mob ueber den alten Namen. */
    public static void setSpawnerEntity(BlockEntity te, String legacyName) {
        if (!(te instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner)) return;
        net.minecraft.world.entity.EntityType<?> type = switch (legacyName) {
            case "Skeleton" -> net.minecraft.world.entity.EntityType.SKELETON;
            case "Zombie" -> net.minecraft.world.entity.EntityType.ZOMBIE;
            case "Spider" -> net.minecraft.world.entity.EntityType.SPIDER;
            case "CaveSpider" -> net.minecraft.world.entity.EntityType.CAVE_SPIDER;
            case "Creeper" -> net.minecraft.world.entity.EntityType.CREEPER;
            case "Blaze" -> net.minecraft.world.entity.EntityType.BLAZE;
            case "Silverfish" -> net.minecraft.world.entity.EntityType.SILVERFISH;
            case "PigZombie" -> net.minecraft.world.entity.EntityType.ZOMBIFIED_PIGLIN;
            case "Enderman" -> net.minecraft.world.entity.EntityType.ENDERMAN;
            case "Witch" -> net.minecraft.world.entity.EntityType.WITCH;
            default -> net.minecraft.world.entity.EntityType.byString(legacyName.toLowerCase()).orElse(net.minecraft.world.entity.EntityType.ZOMBIE);
        };
        spawner.setEntityId(type, net.minecraft.util.RandomSource.create());
        spawner.setChanged();
    }

    public static long getSeed(LevelAccessor world) {
        VirtualWorld vw = VIRTUAL.get();
        if (vw != null) return vw.seed;
        if (world instanceof WorldGenLevel wg) return wg.getSeed();
        if (world instanceof net.minecraft.server.level.ServerLevel sl) return sl.getSeed();
        return 0L;
    }
}
