package com.hbm_m.world.gen.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockChargeBase;
import com.hbm_m.blockentity.bomb.ChargeBlockEntity;
import com.hbm_m.blockentity.decorations.PedestalBlockEntity;
import com.hbm_m.blockentity.generic.LogicBlockEntity;
import com.hbm_m.entity.mob.EntityUndeadSoldier;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code com.hbm.world.gen.util.LogicBlockConditions}: Bedingungen der Logikbloecke; true = naechste Phase.
 */
public class LogicBlockConditions {

    public static LinkedHashMap<String, Function<LogicBlockEntity, Boolean>> conditions;

    /**
     * Nachbau von {@code AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ).expand(ex, ey, ez)} der 1.7.10:
     * dort wurden min/max nicht sortiert, die Erweiterung wirkt also auf die Rohwerte.
     */
    public static AABB box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double ex, double ey, double ez) {
        return new AABB(minX - ex, minY - ey, minZ - ez, maxX + ex, maxY + ey, maxZ + ez);
    }

    /** {@code getEntitiesWithinAABB(EntityPlayer.class, ...)}: leer, wenn der Kasten "umgedreht" ist (wie im Original). */
    public static List<Player> players(Level world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double ex, double ey, double ez) {
        return entities(world, Player.class, minX, minY, minZ, maxX, maxY, maxZ, ex, ey, ez);
    }

    public static <T extends net.minecraft.world.entity.Entity> List<T> entities(Level world, Class<T> clazz, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double ex, double ey, double ez) {
        if (minX - ex > maxX + ex || minY - ey > maxY + ey || minZ - ez > maxZ + ez) return new ArrayList<>();
        return world.getEntitiesOfClass(clazz, box(minX, minY, minZ, maxX, maxY, maxZ, ex, ey, ez));
    }

    /** Fuer Interaktionen, die alle Bedingungen selbst behandeln. */
    public static Function<LogicBlockEntity, Boolean> EMPTY = (tile) -> false;

    public static Function<LogicBlockEntity, Boolean> ABERRATOR = (tile) -> {
        Level world = tile.getWorldObj();
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();

        boolean aoeCheck = !players(world, x, y, z, x + 1, y - 2, z + 1, 10, 10, 10).isEmpty();
        if (tile.phase == 0) {
            if (world.getGameTime() % 20 != 0) return false;
            return aoeCheck;
        }
        if (tile.phase < 3) {
            if (world.getGameTime() % 20 != 0 || tile.timer < 60) return false;
            return world.getEntitiesOfClass(EntityUndeadSoldier.class, box(x, y, z, x - 2, y + 1, z + 1, 50, 20, 50)).isEmpty() && aoeCheck;
        }
        return false;
    };

    public static Function<LogicBlockEntity, Boolean> PLAYER_CUBE_3 = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();
        return !players(world, x, y, z, x + 1, y - 2, z + 1, 3, 3, 3).isEmpty();
    };

    public static Function<LogicBlockEntity, Boolean> PLAYER_CUBE_5 = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();
        return !players(world, x, y, z, x + 1, y - 2, z + 1, 5, 5, 5).isEmpty();
    };

    public static Function<LogicBlockEntity, Boolean> PLAYER_CUBE_25 = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();
        return !players(world, x, y, z, x + 1, y - 2, z + 1, 25, 25, 25).isEmpty();
    };

    public static Function<LogicBlockEntity, Boolean> REDSTONE = (tile) -> {
        Level world = tile.getWorldObj();
        return world.hasNeighborSignal(tile.getBlockPos());
    };

    public static Function<LogicBlockEntity, Boolean> PUZZLE_TEST = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();

        if (tile.phase == 0 && world.hasNeighborSignal(tile.getBlockPos())) {
            Player closest = world.getNearestPlayer(x, y, z, 25, false);
            if (closest != null)
                closest.sendSystemMessage(Component.literal("Find a ").append(Component.literal("great").withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" ancient weapon, of questionable use in the modern age")));
            world.setBlock(new BlockPos(x, y + 1, z), ModBlocks.PEDESTAL.get().defaultBlockState(), 3);
            return true;
        }

        BlockEntity pedestal = world.getBlockEntity(new BlockPos(x, y + 1, z));

        return tile.phase == 1
                && pedestal instanceof PedestalBlockEntity ped
                && !ped.getItem().isEmpty()
                && ped.getItem().getItem() == ModItems.BIG_SWORD.get();
    };

    public static Function<LogicBlockEntity, Boolean> BOMB_CRANE = (tile) -> {
        Level world = tile.getWorldObj();
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();

        if (tile.phase == 0) {
            BlockPos up = new BlockPos(x, y + 1, z);
            world.setBlock(up, ModBlocks.CHARGE_C4.get().defaultBlockState().setValue(BlockChargeBase.FACING, Direction.UP), 3);

            if (world.getBlockEntity(up) instanceof ChargeBlockEntity bomb) {
                bomb.timer = 200;
            }
        }

        return !players(world, x, y, z, x + 1, y - 2, z + 1, 10, 10, 10).isEmpty();
    };

    public static List<String> getConditionNames() {
        return new ArrayList<>(conditions.keySet());
    }

    // neue Bedingungen hier registrieren
    static {
        initialize();
    }

    public static void initialize() {
        conditions = new LinkedHashMap<>();

        conditions.put("EMPTY", EMPTY);
        conditions.put("PLAYER_CUBE_3", PLAYER_CUBE_3);
        conditions.put("PLAYER_CUBE_5", PLAYER_CUBE_5);
        conditions.put("PLAYER_CUBE_25", PLAYER_CUBE_25);

        conditions.put("BOMB_CRANE", BOMB_CRANE);

        // Beispielbedingungen
        conditions.put("ABERRATOR", ABERRATOR);
        conditions.put("REDSTONE", REDSTONE);
        conditions.put("PUZZLE_TEST", PUZZLE_TEST);
    }
}
