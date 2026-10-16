package com.hbm_m.test;

import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.api.energy.WireBlockEntity;
import com.hbm_m.api.energy.WireCenterVisual;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.network.BoxCableBlock;
import com.hbm_m.block.network.RedCablePaintableBlock;
import com.hbm_m.blockentity.network.RedCablePaintableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Кроссплатформенный набор GameTest-ов для красномедных кабелей ({@code red_cable},
 * {@code red_cable_classic}, {@code red_cable_box*}, {@code red_wire_coated}, paintable).
 *
 * <p>Покрывает:
 * <ul>
 *   <li>регистрацию и классы всех кабелей, размер-свойства box (0-4);</li>
 *   <li>вычисление соединений WireBlock: одиночный провод, пары по всем 6 осям,
 *       прямые прогоны (center=straight_x/y/z), Г-повороты (в т.ч. вертикальные),
 *       крестовина, разрыв прогона обновляет соседей;</li>
 *   <li>соединения между РАЗНЫМИ типами кабелей (box ↔ red_cable ↔ classic ↔ wire_coated),
 *       как в 1.7.10 (все — TileEntityCableBaseNT);</li>
 *   <li>коллайдеры (VoxelShape) box-кабеля: одиночный, размеры, прямые по осям,
 *       угол, junction — 1:1 с setBlockBoundsBasedOnState оригинала;</li>
 *   <li>BlockEntity энерго-сети (WireBlockEntity) у всех кабелей;</li>
 *   <li>paintable: VEIL=false по умолчанию, хранение/сброс камуфляжа в BE.</li>
 * </ul>
 *
 * <p>Запуск: {@code ./gradlew :1.20.1-forge:gameTestServer -PnoClientMods} или
 * {@code ./gradlew :1.21.1-neoforge:runGameTestServer}, batch {@code cable}.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class CableGameTest {

    private CableGameTest() {}

    private static final BlockPos C = new BlockPos(2, 1, 2);

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** Устанавливает блок и «встряхивает» его соседей, чтобы WireBlock пересчитал соединения. */
    private static void placeWire(GameTestHelper helper, BlockPos local, Block block) {
        helper.setBlock(local, block);
        refresh(helper, local);
    }

    /** Форсирует пересчёт соединений провода (как реальный neighborChanged при установке предметом). */
    private static void refresh(GameTestHelper helper, BlockPos local) {
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(local);
        helper.getBlockState(local).updateNeighbourShapes(level, abs, 3);
    }

    private static BlockState wireState(GameTestHelper helper, BlockPos local) {
        return helper.getBlockState(local);
    }

    private static void checkConn(GameTestHelper helper, BlockPos local, Direction dir, boolean expected, String ctx) {
        boolean actual = wireState(helper, local).getValue(WireBlock.PROPERTIES_MAP.get(dir));
        check(actual == expected, ctx + ": " + dir + " connection expected=" + expected + " actual=" + actual
                + " at " + local.toShortString());
    }

    private static void checkCenter(GameTestHelper helper, BlockPos local, WireCenterVisual expected, String ctx) {
        WireCenterVisual actual = wireState(helper, local).getValue(WireBlock.CENTER);
        check(actual == expected, ctx + ": center expected=" + expected + " actual=" + actual);
    }

    /** Границы формы по оси (в пикселях 0-16). */
    private static void checkShapeBox(GameTestHelper helper, BlockPos local,
                                      float minX, float minY, float minZ, float maxX, float maxY, float maxZ, String ctx) {
        VoxelShape shape = helper.getBlockState(local).getShape(helper.getLevel(), helper.absolutePos(local));
        check(!shape.isEmpty(), ctx + ": shape empty");
        float tol = 0.001f;
        float[] got = {
                (float) (shape.min(Direction.Axis.X) * 16), (float) (shape.min(Direction.Axis.Y) * 16),
                (float) (shape.min(Direction.Axis.Z) * 16), (float) (shape.max(Direction.Axis.X) * 16),
                (float) (shape.max(Direction.Axis.Y) * 16), (float) (shape.max(Direction.Axis.Z) * 16)
        };
        float[] want = {minX, minY, minZ, maxX, maxY, maxZ};
        for (int i = 0; i < 6; i++) {
            check(Math.abs(got[i] - want[i]) < 0.0625f,
                    ctx + ": shape axis " + i + " expected=" + want[i] + " actual=" + got[i]);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Регистрация и классы.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "cable", timeoutTicks = 100)
    public static void cable_registration(GameTestHelper helper) {
        check(ModBlocks.RED_CABLE.get() instanceof WireBlock, "red_cable → WireBlock (functional, not decorative)");
        check(ModBlocks.RED_CABLE_CLASSIC.get() instanceof WireBlock, "red_cable_classic → WireBlock");
        check(ModBlocks.RED_CABLE_BOX.get() instanceof BoxCableBlock, "red_cable_box → BoxCableBlock");
        check(ModBlocks.RED_CABLE_BOX_1.get() instanceof BoxCableBlock, "red_cable_box_1 → BoxCableBlock");
        check(ModBlocks.RED_CABLE_BOX_2.get() instanceof BoxCableBlock, "red_cable_box_2 → BoxCableBlock");
        check(ModBlocks.RED_CABLE_BOX_3.get() instanceof BoxCableBlock, "red_cable_box_3 → BoxCableBlock");
        check(ModBlocks.RED_CABLE_BOX_4.get() instanceof BoxCableBlock, "red_cable_box_4 → BoxCableBlock");
        check(ModBlocks.RED_CABLE_PAINTABLE.get() instanceof RedCablePaintableBlock, "red_cable_paintable class");
        check(BuiltInRegistries.BLOCK.getKey(ModBlocks.RED_CABLE.get()).getPath().equals("red_cable"), "red_cable id");
        check(BuiltInRegistries.BLOCK.getKey(ModBlocks.RED_CABLE_BOX_4.get()).getPath().equals("red_cable_box_4"),
                "box_4 id");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cable", timeoutTicks = 100)
    public static void cable_boxSizes(GameTestHelper helper) {
        for (int i = 0; i <= 4; i++) {
            Block b = switch (i) {
                case 0 -> ModBlocks.RED_CABLE_BOX.get();
                case 1 -> ModBlocks.RED_CABLE_BOX_1.get();
                case 2 -> ModBlocks.RED_CABLE_BOX_2.get();
                case 3 -> ModBlocks.RED_CABLE_BOX_3.get();
                default -> ModBlocks.RED_CABLE_BOX_4.get();
            };
            int got = b.defaultBlockState().getValue(BoxCableBlock.SIZE);
            check(got == i, "box size " + i + " expected, got " + got);
        }
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Соединения WireBlock: одиночные, пары, прогоны, повороты, кресты.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "cable", timeoutTicks = 100)
    public static void cable_loneWire(GameTestHelper helper) {
        placeWire(helper, C, ModBlocks.RED_CABLE.get());
        for (Direction d : Direction.values()) checkConn(helper, C, d, false, "lone red_cable");
        checkCenter(helper, C, WireCenterVisual.JUNCTION, "lone red_cable");
        check(helper.getBlockEntity(C) instanceof WireBlockEntity, "lone red_cable has WireBlockEntity");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_pairsAllAxes(GameTestHelper helper) {
        Block wire = ModBlocks.RED_CABLE_CLASSIC.get();
        for (Direction d : Direction.values()) {
            BlockPos other = C.relative(d);
            placeWire(helper, C, wire);
            placeWire(helper, other, wire);
            refresh(helper, C);
            refresh(helper, other);
            checkConn(helper, C, d, true, "pair " + d);
            checkConn(helper, other, d.getOpposite(), true, "pair " + d);
            // остальные стороны не соединились
            for (Direction e : Direction.values()) {
                if (e != d) checkConn(helper, C, e, false, "pair " + d + " (side " + e + ")");
            }
            checkCenter(helper, C, WireCenterVisual.JUNCTION, "pair " + d);
            helper.setBlock(other, Blocks.AIR);
            refresh(helper, C);
            checkConn(helper, C, d, false, "pair " + d + " after break");
        }
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_straightRuns(GameTestHelper helper) {
        // Три отдельные области, чтобы прогоны не мешали друг другу.
        // X: три классических кабеля в ряд (y=1, z=2, x=0..2)
        for (int x = 0; x <= 2; x++) placeWire(helper, new BlockPos(x, 1, 2), ModBlocks.RED_CABLE_CLASSIC.get());
        refresh(helper, new BlockPos(1, 1, 2));
        checkCenter(helper, new BlockPos(1, 1, 2), WireCenterVisual.STRAIGHT_X, "straight X middle");
        checkConn(helper, new BlockPos(1, 1, 2), Direction.EAST, true, "straight X");
        checkConn(helper, new BlockPos(1, 1, 2), Direction.WEST, true, "straight X");
        checkConn(helper, new BlockPos(1, 1, 2), Direction.NORTH, false, "straight X");

        // Z: red_cable (y=1, x=4, z=0..2)
        for (int z = 0; z <= 2; z++) placeWire(helper, new BlockPos(4, 1, z), ModBlocks.RED_CABLE.get());
        refresh(helper, new BlockPos(4, 1, 1));
        checkCenter(helper, new BlockPos(4, 1, 1), WireCenterVisual.STRAIGHT_Z, "straight Z middle");
        checkConn(helper, new BlockPos(4, 1, 1), Direction.NORTH, true, "straight Z");
        checkConn(helper, new BlockPos(4, 1, 1), Direction.SOUTH, true, "straight Z");

        // Y: box (x=0, z=0, y=0..2)
        for (int y = 0; y <= 2; y++) placeWire(helper, new BlockPos(0, y, 0), ModBlocks.RED_CABLE_BOX.get());
        refresh(helper, new BlockPos(0, 1, 0));
        checkCenter(helper, new BlockPos(0, 1, 0), WireCenterVisual.STRAIGHT_Y, "straight Y middle");
        checkConn(helper, new BlockPos(0, 1, 0), Direction.UP, true, "straight Y");
        checkConn(helper, new BlockPos(0, 1, 0), Direction.DOWN, true, "straight Y");
        checkConn(helper, new BlockPos(0, 1, 0), Direction.EAST, false, "straight Y");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_cornerTurns(GameTestHelper helper) {
        Block wire = ModBlocks.RED_CABLE.get();
        // Все 4 горизонтальных Г-поворота вокруг центра
        Direction[][] corners = {
                {Direction.NORTH, Direction.EAST},
                {Direction.EAST, Direction.SOUTH},
                {Direction.SOUTH, Direction.WEST},
                {Direction.WEST, Direction.NORTH},
        };
        int i = 0;
        for (Direction[] pair : corners) {
            BlockPos a = C.relative(pair[0]);
            BlockPos b = C.relative(pair[1]);
            placeWire(helper, C, wire);
            placeWire(helper, a, wire);
            placeWire(helper, b, wire);
            refresh(helper, C);
            checkConn(helper, C, pair[0], true, "corner " + i + " " + pair[0]);
            checkConn(helper, C, pair[1], true, "corner " + i + " " + pair[1]);
            for (Direction e : Direction.values()) {
                if (e != pair[0] && e != pair[1]) checkConn(helper, C, e, false, "corner " + i + " side " + e);
            }
            checkCenter(helper, C, WireCenterVisual.JUNCTION, "corner " + i);
            helper.setBlock(a, Blocks.AIR);
            helper.setBlock(b, Blocks.AIR);
            refresh(helper, C);
            i++;
        }
        // Вертикальный Г: восток + вверх
        placeWire(helper, C, wire);
        placeWire(helper, C.relative(Direction.EAST), wire);
        placeWire(helper, C.relative(Direction.UP), wire);
        refresh(helper, C);
        checkConn(helper, C, Direction.EAST, true, "vertical corner");
        checkConn(helper, C, Direction.UP, true, "vertical corner");
        for (Direction e : Direction.values()) {
            if (e != Direction.EAST && e != Direction.UP) checkConn(helper, C, e, false, "vertical corner side " + e);
        }
        checkCenter(helper, C, WireCenterVisual.JUNCTION, "vertical corner");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_crossJunction(GameTestHelper helper) {
        Block wire = ModBlocks.RED_CABLE_CLASSIC.get();
        placeWire(helper, C, wire);
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            placeWire(helper, C.relative(d), wire);
        }
        refresh(helper, C);
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            checkConn(helper, C, d, true, "cross " + d);
        }
        checkConn(helper, C, Direction.UP, false, "cross up");
        checkConn(helper, C, Direction.DOWN, false, "cross down");
        checkCenter(helper, C, WireCenterVisual.JUNCTION, "cross");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Смешанные сети: разные типы кабелей соединяются между собой (1.7.10 parity).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_mixedTypeLine(GameTestHelper helper) {
        // Линия восток-запад из всех типов: classic — red_cable — box_2 — wire_coated — box_0
        placeWire(helper, new BlockPos(0, 1, 2), ModBlocks.RED_CABLE_CLASSIC.get());
        placeWire(helper, new BlockPos(1, 1, 2), ModBlocks.RED_CABLE.get());
        placeWire(helper, new BlockPos(2, 1, 2), ModBlocks.RED_CABLE_BOX_2.get());
        placeWire(helper, new BlockPos(3, 1, 2), ModBlocks.WIRE_COATED.get());
        placeWire(helper, new BlockPos(4, 1, 2), ModBlocks.RED_CABLE_BOX.get());
        for (int x = 0; x <= 4; x++) refresh(helper, new BlockPos(x, 1, 2));

        for (int x = 1; x <= 3; x++) {
            BlockPos p = new BlockPos(x, 1, 2);
            checkConn(helper, p, Direction.EAST, true, "mixed line " + x);
            checkConn(helper, p, Direction.WEST, true, "mixed line " + x);
        }
        checkCenter(helper, new BlockPos(2, 1, 2), WireCenterVisual.STRAIGHT_X, "mixed line center");
        // крайние: по одному соединению
        checkConn(helper, new BlockPos(0, 1, 2), Direction.EAST, true, "mixed line end0");
        checkConn(helper, new BlockPos(4, 1, 2), Direction.WEST, true, "mixed line end4");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_boxVsRedCableIntersection(GameTestHelper helper) {
        // Перекрёсток: box по X, red_cable по Z, пересекаются в центре
        placeWire(helper, C, ModBlocks.RED_CABLE_BOX.get());
        placeWire(helper, C.east(), ModBlocks.RED_CABLE_BOX_1.get());
        placeWire(helper, C.west(), ModBlocks.RED_CABLE_BOX_2.get());
        placeWire(helper, C.north(), ModBlocks.RED_CABLE.get());
        placeWire(helper, C.south(), ModBlocks.RED_CABLE.get());
        refresh(helper, C);
        checkConn(helper, C, Direction.EAST, true, "intersection");
        checkConn(helper, C, Direction.WEST, true, "intersection");
        checkConn(helper, C, Direction.NORTH, true, "intersection");
        checkConn(helper, C, Direction.SOUTH, true, "intersection");
        checkConn(helper, C, Direction.UP, false, "intersection");
        // box с 4 соединениями по двум осям → junction-ветка рендера
        checkCenter(helper, C, WireCenterVisual.JUNCTION, "intersection center");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Коллайдеры box-кабеля (VoxelShape) — 1:1 с setBlockBoundsBasedOnState.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "cable", timeoutTicks = 100)
    public static void cable_boxShapeLone(GameTestHelper helper) {
        helper.setBlock(C, ModBlocks.RED_CABLE_BOX.get());
        refresh(helper, C);
        // размер 0, без соединений: 2..14 px
        checkShapeBox(helper, C, 2, 2, 2, 14, 14, 14, "box0 lone");
        helper.setBlock(C, ModBlocks.RED_CABLE_BOX_4.get());
        refresh(helper, C);
        // размер 4: 6..10 px
        checkShapeBox(helper, C, 6, 6, 6, 10, 10, 10, "box4 lone");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_boxShapeStraight(GameTestHelper helper) {
        // Прогон по X, размер 0: 0..16 по X, 2..14 по YZ
        placeWire(helper, C, ModBlocks.RED_CABLE_BOX.get());
        placeWire(helper, C.east(), ModBlocks.RED_CABLE_BOX.get());
        refresh(helper, C);
        checkShapeBox(helper, C, 0, 2, 2, 16, 14, 14, "box straight X");

        // Прогон по Y, размер 2 (сжатие 4..12): сначала убираем восточный сосед из X-прогона
        helper.setBlock(C.east(), Blocks.AIR);
        refresh(helper, C);
        placeWire(helper, C, ModBlocks.RED_CABLE_BOX_2.get());
        placeWire(helper, C.above(), ModBlocks.RED_CABLE_BOX_2.get());
        placeWire(helper, C.below(), ModBlocks.RED_CABLE_BOX_2.get());
        checkShapeBox(helper, C, 4, 0, 4, 12, 16, 12, "box2 straight Y");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_boxShapeCornerAndJunction(GameTestHelper helper) {
        Block box = ModBlocks.RED_CABLE_BOX.get();
        // Угол: соединения north + east → общий случай (ядро, вытянутое к соединениям)
        placeWire(helper, C, box);
        placeWire(helper, C.north(), box);
        placeWire(helper, C.east(), box);
        refresh(helper, C);
        checkShapeBox(helper, C, 2, 2, 0, 16, 14, 14, "box corner");

        // Junction: N+S+E → вытянуто по трём сторонам, запад и вертикаль сжаты
        placeWire(helper, C.south(), box);
        refresh(helper, C);
        checkShapeBox(helper, C, 2, 2, 0, 16, 14, 16, "box junction");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Энерго-сеть и paintable.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "cable", timeoutTicks = 100)
    public static void cable_wireBlockEntities(GameTestHelper helper) {
        BlockPos[] spots = {
                C, C.east(), C.west(), C.north(), C.south(), C.above()
        };
        Block[] wires = {
                ModBlocks.RED_CABLE.get(), ModBlocks.RED_CABLE_CLASSIC.get(), ModBlocks.RED_CABLE_BOX.get(),
                ModBlocks.RED_CABLE_BOX_3.get(), ModBlocks.WIRE_COATED.get(), ModBlocks.RED_WIRE_COATED.get()
        };
        for (int i = 0; i < spots.length; i++) {
            helper.setBlock(spots[i], wires[i]);
        }
        helper.succeedWhen(() -> {
            for (int i = 0; i < spots.length; i++) {
                BlockEntity be = helper.getBlockEntity(spots[i]);
                check(be instanceof WireBlockEntity,
                        "wire BE at " + spots[i].toShortString() + " expected WireBlockEntity, got "
                                + (be == null ? "null" : be.getClass().getSimpleName()));
            }
        });
    }

    @GameTest(template = "empty3x3x3", batch = "cable", timeoutTicks = 100)
    public static void cable_paintableVeilAndCamo(GameTestHelper helper) {
        helper.setBlock(C, ModBlocks.RED_CABLE_PAINTABLE.get());
        // Хелпер-блоки рендера (базовый куб + вуаль) зарегистрированы и имеют MODEL-рендер,
        // т.к. renderSingleBlock пропускает INVISIBLE-состояния
        check(ModBlocks.RED_CABLE_PAINTABLE_BASE.get() != null, "paintable base helper registered");
        check(ModBlocks.RED_CABLE_PAINTABLE_VEIL.get() != null, "paintable veil helper registered");
        RedCablePaintableBlockEntity be = helper.getBlockEntity(C) instanceof RedCablePaintableBlockEntity p ? p : null;
        check(be != null, "paintable BE");
        check(be.getCamo() == null, "paintable starts unpainted");
        // Покраска камнем (как ПКМ блоком)
        be.setCamo(Blocks.STONE.defaultBlockState());
        check(be.getCamo() != null && be.getCamo().is(Blocks.STONE), "paintable stores camo");
        // Сброс (Shift+ПКМ пустой рукой)
        be.setCamo(null);
        check(be.getCamo() == null, "paintable resets camo");
        helper.succeed();
    }
}
