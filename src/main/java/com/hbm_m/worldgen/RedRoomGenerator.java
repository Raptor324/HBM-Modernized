package com.hbm_m.worldgen;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.PedestalBlockEntity;
import com.hbm_m.item.ModItems;

import dev.architectury.registry.registries.RegistrySupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/**
 * Порт скрытой комнаты "Red Room" из 1.7.10
 * ({@code BlockKeyhole.generateRoom} / {@code BlockRedBrickKeyhole.generateRoom}).
 *
 * <p>Комната 9x9x5 из красного кирпича строится за ключевой скважиной,
 * в центре — постамент с глиняной табличкой (POOL_BLACK_SLAB), по бокам —
 * постаменты с секретами/синепринтами (POOL_BLACK_PART, шанс 50%).</p>
 */
public final class RedRoomGenerator {

    private RedRoomGenerator() {}

    /** Взвешенная запись лута (аналог weighted() из HbmChestContents). */
    private record Entry(RegistrySupplier<net.minecraft.world.item.Item> item,
                         int min, int max, int weight) {}

    private static final List<Entry> BLACK_PART = List.of(
            new Entry(ModItems.ITEM_SECRET_SELENIUM_STEEL, 4, 4, 10),
            new Entry(ModItems.ITEM_SECRET_CONTROLLER, 1, 1, 10),
            new Entry(ModItems.ITEM_SECRET_CANISTER, 1, 1, 10),
            new Entry(ModItems.BLUEPRINT_FOLDER, 2, 3, 1),
            // Книга Вагонов — из ItemPoolsRedRoom оригинала (weighted(book_of_, 0, 1, 1, 5))
            new Entry(ModItems.BOOK_OF_, 1, 1, 5));

    /**
     * @param keyholePos позиция скважины (блока keyhole)
     * @param facing     направление, куда смотрит лицевая сторона скважины
     */
    public static void generate(ServerLevel level, BlockPos keyholePos, Direction facing) {
        Direction dir = facing.getOpposite(); // в оригинале комната строится ЗА скважиной
        BlockPos center = keyholePos.relative(dir, 4).below(2);

        int size = 9;
        int height = 5;
        int w = size / 2;
        Block brick = ModBlocks.BRICK_RED.get();
        BlockState brickState = brick.defaultBlockState();

        // Внешние рёбра (пол и потолок по периметру)
        for (int i = -w; i <= w; i++) {
            set(level, center.offset(i, 0, w), brickState);
            set(level, center.offset(i, 0, -w), brickState);
            set(level, center.offset(w, 0, i), brickState);
            set(level, center.offset(-w, 0, i), brickState);
            set(level, center.offset(i, height - 1, w), brickState);
            set(level, center.offset(i, height - 1, -w), brickState);
            set(level, center.offset(w, height - 1, i), brickState);
            set(level, center.offset(-w, height - 1, i), brickState);
        }
        for (int i = 1; i <= height - 2; i++) {
            // Углы
            set(level, center.offset(w, i, w), brickState);
            set(level, center.offset(w, i, -w), brickState);
            set(level, center.offset(-w, i, w), brickState);
            set(level, center.offset(-w, i, -w), brickState);
            // Стены; сторона, обращённая к скважине, оставляется под дверь
            for (int j = -w + 1; j <= w - 1; j++) {
                if (dir != Direction.EAST) set(level, center.offset(w, i, j), brickState);
                if (dir != Direction.WEST) set(level, center.offset(-w, i, j), brickState);
                if (dir != Direction.SOUTH) set(level, center.offset(j, i, w), brickState);
                if (dir != Direction.NORTH) set(level, center.offset(j, i, -w), brickState);
            }
        }
        for (int i = -w + 1; i <= w - 1; i++) {
            for (int j = -w + 1; j <= w - 1; j++) {
                // Пол и потолок
                set(level, center.offset(i, 0, j), brickState);
                set(level, center.offset(i, height - 1, j), brickState);
                // Внутренность — воздух
                for (int k = 1; k <= height - 2; k++) {
                    level.setBlock(center.offset(i, k, j), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Дверь на месте скважины (замещает сам блок keyhole)
        BlockState door = ModBlocks.DOOR_RED_BLOCK.get().defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(keyholePos, door, 3);
        level.setBlock(keyholePos.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);

        // Постаменты с лутом
        placePedestal(level, center.offset(0, 1, 0), new ItemStack(ModItems.CLAY_TABLET.get()));
        Direction side1 = dir.getClockWise();
        Direction side2 = side1.getClockWise().getClockWise();
        trySidePedestal(level, center.offset(side1.getStepX() * 2, 1, side1.getStepZ() * 2));
        trySidePedestal(level, center.offset(-side1.getStepX() * 2, 1, -side1.getStepZ() * 2));
        trySidePedestal(level, center.offset(side2.getStepX() * 2, 1, side2.getStepZ() * 2));
        trySidePedestal(level, center.offset(-side2.getStepX() * 2, 1, -side2.getStepZ() * 2));

        // Убираем выпавшие предметы внутри комнаты (как в оригинале)
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                new AABB(center).inflate(w + 1, 2, w + 1));
        for (ItemEntity e : items) e.discard();
    }

    private static void trySidePedestal(ServerLevel level, BlockPos pos) {
        if (level.random.nextBoolean()) {
            placePedestal(level, pos, rollBlackPart(level));
        }
    }

    private static ItemStack rollBlackPart(ServerLevel level) {
        int total = 0;
        for (Entry e : BLACK_PART) total += e.weight();
        int roll = level.random.nextInt(total);
        for (Entry e : BLACK_PART) {
            roll -= e.weight();
            if (roll < 0) {
                int count = e.min() == e.max() ? e.min()
                        : e.min() + level.random.nextInt(e.max() - e.min() + 1);
                return new ItemStack(e.item().get(), count);
            }
        }
        return ItemStack.EMPTY;
    }

    private static void placePedestal(ServerLevel level, BlockPos pos, ItemStack stack) {
        level.setBlock(pos, ModBlocks.PEDESTAL.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PedestalBlockEntity pedestal) {
            pedestal.setItem(stack);
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }
}
