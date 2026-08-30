package com.hbm_m.worldgen;

import java.util.List;
import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.KeyholeBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.DecoLootBlockEntity;
import com.hbm_m.blockentity.decorations.PedestalBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/**
 * Порт скрытых комнат из 1.7.10 —
 * {@code BlockKeyhole.generateRoom} (красная комната) и
 * {@code BlockRedBrickKeyhole.generateRoom} (чёрная комната).
 *
 * <p><b>Красная комната</b> (каменная скважина, замурованная в камень):
 * замкнутая коробка 9x9x5 из красного кирпича — строятся ВСЕ четыре стены,
 * ближняя стена проходит ровно через плоскость скважины, дверь встраивается
 * в неё. Декор: настенные факелы, паутина/колонны/огонь/круг/лава (по 1/4),
 * в центре либо постамент с {@code POOL_RED_PEDESTAL}, либо куча лута
 * {@code deco_loot} с комплектом брони. На случайной стене — вторая скважина
 * {@code stone_keyhole_meta}, ведущая в чёрную комнату.</p>
 *
 * <p><b>Чёрная комната</b> (кирпичная скважина в стене красной комнаты):
 * ближняя стена НЕ перестраивается — она уже существует как часть стены
 * красной комнаты. В центре постамент с глиняной табличкой
 * ({@code POOL_BLACK_SLAB}), по бокам — секреты/синепринты
 * ({@code POOL_BLACK_PART}, шанс 50%).</p>
 */
public final class RedRoomGenerator {

    private RedRoomGenerator() {}

    private static final int SIZE = 9;
    private static final int HEIGHT = 5;
    private static final int W = SIZE / 2;

    /** Взвешенная запись лута (аналог weighted() из HbmChestContents). */
    private record Entry(Supplier<Item> item, int min, int max, int weight) {}

    /**
     * {@code POOL_RED_PEDESTAL} из ItemPoolsRedRoom (1.7.10).
     * Не портировано (предметов ещё нет в Modernized): flask_infusion,
     * gun_hangman, gun_mas36, weapon_mod_special (NICKEL, DOUBLOONS).
     */
    private static final List<Entry> RED_PEDESTAL = List.of(
            // вес 10
            new Entry(ModItems.BALLISTIC_GAUNTLET::get, 1, 1, 10),
            new Entry(ModItems.ARMOR_POLISH::get, 1, 1, 10),
            new Entry(ModItems.BANDAID::get, 1, 1, 10),
            new Entry(ModItems.SERUM::get, 1, 1, 10),
            new Entry(ModItems.QUARTZ_PLUTONIUM::get, 1, 1, 10),
            new Entry(ModItems.MORNING_GLORY::get, 1, 1, 10),
            new Entry(ModItems.SPIDER_MILK::get, 1, 1, 10),
            new Entry(ModItems.INK::get, 1, 1, 10),
            new Entry(ModItems.HEART_CONTAINER::get, 1, 1, 10),
            new Entry(ModItems.BLACK_DIAMOND::get, 1, 1, 10),
            new Entry(ModItems.SCRUMPY::get, 1, 1, 10),
            // вес 5
            new Entry(ModItems.WILD_P::get, 1, 1, 5),
            new Entry(ModItems.CARD_AOS::get, 1, 1, 5),
            new Entry(ModItems.CARD_QOS::get, 1, 1, 5),
            new Entry(ModItems.STARMETAL_SWORD::get, 1, 1, 5),
            new Entry(ModItems.GEM_ALEXANDRITE::get, 1, 1, 5),
            new Entry(ModItems.CRACKPIPE::get, 1, 1, 5),
            new Entry(() -> ModBlocks.BOXCAR.get().asItem(), 1, 1, 5),
            new Entry(ModItems.BOOK_OF_::get, 1, 1, 5),
            // вес 1
            new Entry(ModItems.ITEM_SECRET_FOLLY::get, 1, 1, 1));

    /** {@code POOL_BLACK_SLAB} — глиняная табличка на центральном постаменте. */
    private static final List<Entry> BLACK_SLAB = List.of(
            new Entry(ModItems.CLAY_TABLET::get, 1, 1, 10));

    /** {@code POOL_BLACK_PART} — секреты и синепринты на боковых постаментах. */
    private static final List<Entry> BLACK_PART = List.of(
            new Entry(ModItems.ITEM_SECRET_SELENIUM_STEEL::get, 4, 4, 10),
            new Entry(ModItems.ITEM_SECRET_CONTROLLER::get, 1, 1, 10),
            new Entry(ModItems.ITEM_SECRET_CANISTER::get, 1, 1, 10),
            new Entry(ModItems.BLUEPRINT_FOLDER::get, 1, 1, 1));

    /**
     * Красная комната — порт {@code BlockKeyhole.generateRoom}.
     *
     * @param keyholePos позиция каменной скважины
     * @param outward    кликнутая грань (наружу от стены, к игроку) — комната строится за ней
     */
    public static void generateRedRoom(ServerLevel level, BlockPos keyholePos, Direction outward) {
        Direction inward = outward.getOpposite();
        BlockPos center = keyholePos.relative(inward, W).below(2);
        RandomSource rand = level.random;
        BlockState brick = ModBlocks.BRICK_RED.get().defaultBlockState();

        // Полная замкнутая оболочка — ближняя стена проходит через скважину
        buildShell(level, center, brick, outward, false);

        // Внутренняя скважина stone_keyhole_meta на случайной стене (в оригинале
        // nextInt(1)==0 — всегда), лицевой гранью внутрь комнаты
        switch (rand.nextInt(4)) {
            case 0 -> placeInnerKeyhole(level, center.offset(W, 2, 0), Direction.WEST);
            case 1 -> placeInnerKeyhole(level, center.offset(-W, 2, 0), Direction.EAST);
            case 2 -> placeInnerKeyhole(level, center.offset(0, 2, W), Direction.NORTH);
            default -> placeInnerKeyhole(level, center.offset(0, 2, -W), Direction.SOUTH);
        }

        // Очистка внутренности — воздух
        for (int i = -W + 1; i <= W - 1; i++) {
            for (int j = -W + 1; j <= W - 1; j++) {
                set(level, center.offset(i, 0, j), brick);
                set(level, center.offset(i, HEIGHT - 1, j), brick);
                for (int k = 1; k <= HEIGHT - 2; k++) {
                    level.setBlock(center.offset(i, k, j), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Факелы. В 1.7.10 это напольные факелы без опоры (стояли в воздухе у
        // стен до первого апдейта); в modern используем настенные, иначе сразу
        // отвалятся. Позиции и высота — как в оригинале.
        int torchDist = W - 1;
        int torchOff = torchDist - 1;
        wallTorch(level, center.offset(torchDist, 2, torchOff), Direction.WEST);
        wallTorch(level, center.offset(torchDist, 2, -torchOff), Direction.WEST);
        wallTorch(level, center.offset(-torchDist, 2, torchOff), Direction.EAST);
        wallTorch(level, center.offset(-torchDist, 2, -torchOff), Direction.EAST);
        wallTorch(level, center.offset(torchOff, 2, torchDist), Direction.NORTH);
        wallTorch(level, center.offset(-torchOff, 2, torchDist), Direction.NORTH);
        wallTorch(level, center.offset(torchOff, 2, -torchDist), Direction.SOUTH);
        wallTorch(level, center.offset(-torchOff, 2, -torchDist), Direction.SOUTH);

        // Паутина под потолком
        if (rand.nextInt(4) == 0) {
            for (int i = -W + 1; i <= W - 1; i++) {
                for (int j = -W + 1; j <= W - 1; j++) {
                    if (rand.nextBoolean()) {
                        set(level, center.offset(i, HEIGHT - 2, j),
                                net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState());
                    }
                }
            }
        }

        // Колонны в углах внутреннего зала
        if (rand.nextInt(4) == 0) {
            BlockState concrete = ModBlocks.CONCRETE_RED.get().defaultBlockState();
            for (int k = 1; k <= HEIGHT - 2; k++) {
                set(level, center.offset(W - 2, k, W - 2), concrete);
                set(level, center.offset(W - 2, k, -(W - 2)), concrete);
                set(level, center.offset(-(W - 2), k, W - 2), concrete);
                set(level, center.offset(-(W - 2), k, -(W - 2)), concrete);
            }
        }

        // Костры на горелках (netherrack + fire по углам)
        if (rand.nextInt(4) == 0) {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    set(level, center.offset(sx * (W - 1), 0, sz * (W - 1)),
                            net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState());
                    set(level, center.offset(sx * (W - 1), 1, sz * (W - 1)),
                            net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                }
            }
        }

        // Круг на полу вокруг центра
        if (rand.nextInt(4) == 0) {
            BlockState concrete = ModBlocks.CONCRETE_RED.get().defaultBlockState();
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0) set(level, center.offset(i, 0, j), concrete);
                }
            }
        }

        // Лавовые желоба вдоль стен
        if (rand.nextInt(4) == 0) {
            BlockState lava = net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState();
            for (int i = 2; i <= 3; i++) {
                set(level, center.offset(i, 0, W - 1), lava);
                set(level, center.offset(-i, 0, W - 1), lava);
                set(level, center.offset(i, 0, -(W - 1)), lava);
                set(level, center.offset(-i, 0, -(W - 1)), lava);
                set(level, center.offset(W - 1, 0, i), lava);
                set(level, center.offset(W - 1, 0, -i), lava);
                set(level, center.offset(-(W - 1), 0, i), lava);
                set(level, center.offset(-(W - 1), 0, -i), lava);
            }
        }

        // Лут: 1/20 — куча брони (deco_loot), иначе постамент с POOL_RED_PEDESTAL
        if (rand.nextInt(20) == 0) {
            spawnArmorLoot(level, center.offset(0, 1, 0), rand);
        } else {
            placePedestal(level, center.offset(0, 1, 0), rollPool(RED_PEDESTAL, rand));
        }

        placeDoor(level, keyholePos, outward);
        clearDroppedItems(level, center);
    }

    /**
     * Чёрная комната — порт {@code BlockRedBrickKeyhole.generateRoom}.
     * Ближняя стена не строится: скважина уже встроена в существующую
     * кирпичную стену красной комнаты.
     */
    public static void generateBlackRoom(ServerLevel level, BlockPos keyholePos, Direction outward) {
        Direction inward = outward.getOpposite();
        BlockPos center = keyholePos.relative(inward, W).below(2);
        RandomSource rand = level.random;
        BlockState brick = ModBlocks.BRICK_RED.get().defaultBlockState();

        buildShell(level, center, brick, outward, true);

        for (int i = -W + 1; i <= W - 1; i++) {
            for (int j = -W + 1; j <= W - 1; j++) {
                set(level, center.offset(i, 0, j), brick);
                set(level, center.offset(i, HEIGHT - 1, j), brick);
                for (int k = 1; k <= HEIGHT - 2; k++) {
                    level.setBlock(center.offset(i, k, j), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Постаменты: в центре табличка (POOL_BLACK_SLAB), по 4 сторонам с
        // шансом 50% — секреты (POOL_BLACK_PART)
        placePedestal(level, center.offset(0, 1, 0), rollPool(BLACK_SLAB, rand));
        if (rand.nextBoolean()) placePedestal(level, center.offset(2, 1, 0), rollPool(BLACK_PART, rand));
        if (rand.nextBoolean()) placePedestal(level, center.offset(-2, 1, 0), rollPool(BLACK_PART, rand));
        if (rand.nextBoolean()) placePedestal(level, center.offset(0, 1, 2), rollPool(BLACK_PART, rand));
        if (rand.nextBoolean()) placePedestal(level, center.offset(0, 1, -2), rollPool(BLACK_PART, rand));

        placeDoor(level, keyholePos, outward);
        clearDroppedItems(level, center);
    }

    /**
     * Строит оболочку комнаты: рёбра, углы, стены, пол и потолок.
     *
     * @param outward      направление от центра комнаты к скважине (ближняя стена)
     * @param skipNearWall не строить ближнюю стену (чёрная комната — там уже
     *                     стоит стена красной комнаты со встроенной скважиной)
     */
    private static void buildShell(ServerLevel level, BlockPos center, BlockState brick,
                                   Direction outward, boolean skipNearWall) {
        // Рёбра: пол и потолок по периметру
        for (int i = -W; i <= W; i++) {
            set(level, center.offset(i, 0, W), brick);
            set(level, center.offset(i, 0, -W), brick);
            set(level, center.offset(W, 0, i), brick);
            set(level, center.offset(-W, 0, i), brick);
            set(level, center.offset(i, HEIGHT - 1, W), brick);
            set(level, center.offset(i, HEIGHT - 1, -W), brick);
            set(level, center.offset(W, HEIGHT - 1, i), brick);
            set(level, center.offset(-W, HEIGHT - 1, i), brick);
        }
        for (int i = 1; i <= HEIGHT - 2; i++) {
            // Углы
            set(level, center.offset(W, i, W), brick);
            set(level, center.offset(W, i, -W), brick);
            set(level, center.offset(-W, i, W), brick);
            set(level, center.offset(-W, i, -W), brick);
            // Стены
            for (int j = -W + 1; j <= W - 1; j++) {
                if (!(skipNearWall && outward == Direction.WEST)) set(level, center.offset(-W, i, j), brick);
                if (!(skipNearWall && outward == Direction.EAST)) set(level, center.offset(W, i, j), brick);
                if (!(skipNearWall && outward == Direction.NORTH)) set(level, center.offset(j, i, -W), brick);
                if (!(skipNearWall && outward == Direction.SOUTH)) set(level, center.offset(j, i, W), brick);
            }
        }
    }

    /** Дверь красной/чёрной комнаты на месте скважины (порт ItemModDoor.placeDoorBlock). */
    private static void placeDoor(ServerLevel level, BlockPos keyholePos, Direction outward) {
        BlockState door = ModBlocks.DOOR_RED_BLOCK.get().defaultBlockState()
                .setValue(DoorBlock.FACING, outward)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(keyholePos, door, 3);
        level.setBlock(keyholePos.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
    }

    private static void placeInnerKeyhole(ServerLevel level, BlockPos pos, Direction facing) {
        set(level, pos, ModBlocks.STONE_KEYHOLE_META.get().defaultBlockState()
                .setValue(KeyholeBlock.FACING, facing));
    }

    private static void wallTorch(ServerLevel level, BlockPos pos, Direction facing) {
        set(level, pos, net.minecraft.world.level.block.Blocks.WALL_TORCH.defaultBlockState()
                .setValue(WallTorchBlock.FACING, facing));
    }

    /**
     * Куча брони в центре красной комнаты (1/20).
     *
     * ЗАГЛУШКА: в 1.7.10 сюда кладётся комплект NCRPA (шанс 1/5) или
     * Trenchmaster — брони в Modernized ещё нет, временно лежит кожаный
     * комплект. Заменить на предметы брони NCRPA / Trenchmaster при их
     * портировании.
     */
    private static void spawnArmorLoot(ServerLevel level, BlockPos pos, RandomSource rand) {
        level.setBlock(pos, ModBlocks.DECO_LOOT.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof DecoLootBlockEntity loot) {
            // ЗАГЛУШКА брони (см. выше) — 4 предмета, как addItem(..., 0, 0, 0) в оригинале
            loot.addItem(new ItemStack(Items.LEATHER_HELMET), 0, 0, 0);
            loot.addItem(new ItemStack(Items.LEATHER_CHESTPLATE), 0, 0, 0);
            loot.addItem(new ItemStack(Items.LEATHER_LEGGINGS), 0, 0, 0);
            loot.addItem(new ItemStack(Items.LEATHER_BOOTS), 0, 0, 0);
        }
    }

    private static ItemStack rollPool(List<Entry> pool, RandomSource rand) {
        int total = 0;
        for (Entry e : pool) total += e.weight();
        int roll = rand.nextInt(total);
        for (Entry e : pool) {
            roll -= e.weight();
            if (roll < 0) {
                int count = e.min() == e.max() ? e.min()
                        : e.min() + rand.nextInt(e.max() - e.min() + 1);
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

    /** Убирает выпавшие предметы внутри комнаты (как в оригинале). */
    private static void clearDroppedItems(ServerLevel level, BlockPos center) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                new AABB(center).inflate(W + 1, 2, W + 1));
        for (ItemEntity e : items) e.discard();
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }
}
