package com.hbm_m.block.entity.doors;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Порт NEXT {@code BlockEntityBlastDoor}/{@code BlastDoor} (идентичен 1.7.10
 * {@code TileEntityBlastDoor}): вертикальная сдвижная дверь-шторка 1x7.
 *
 * <p>Колонна створки (ячейки +1..+5) при открытии уезжает ВВЕРХ в корпус (+6):
 * каждый сегмент исчезает, поднявшись на свою высоту. Тайминги 1:1 с NEXT
 * {@code travel()} (BlockEntityBlastDoor.java:87-97): при открытии ячейка k+1
 * освобождается при timer &gt;= k*20, при закрытии твердеет при
 * timer &gt;= (6-cell)*20 — с обеих сторон это порог j/5 от прогресса
 * (см. {@link #getRetractionThreshold}). Звуки (REACTOR_START/STOP) играются
 * серверно в {@link BlastDoorBlockEntity} — все звуковые хуки декларации null.
 */
public final class BlastDoorDecl extends DoorDecl {

    public static final BlastDoorDecl INSTANCE = new BlastDoorDecl();

    /** Полное время хода створки (NEXT TRAVEL_TICKS = 100). */
    public static final int TRAVEL_TICKS = 100;

    /** Шаг между сегментами створки в тиках (NEXT STEP_TICKS = 20). */
    public static final int STEP_TICKS = 20;

    /** Верхняя ячейка подвижной створки (NEXT BlastDoor.LEAF_TOP = 5). */
    public static final int LEAF_TOP = 5;

    /** Подъём створки в блоках: 5 за полный ход. */
    private static final float EXTEND = 5.0F;

    private BlastDoorDecl() {
        DoorStructureDefinition.Builder builder = DoorStructureDefinition.create();
        builder.addSymbol('#', Shapes.block(), PartRole.DEFAULT);
        builder.addSymbol('C', Shapes.block(), PartRole.CONTROLLER);

        // Вид сбоку, 7 рядов сверху вниз = y6..y0. Ячейка +6 (корпус) всегда сплошная,
        // створка +1..+5 в открытом виде уезжает (пустые символы)
        String[] closed = { "#", "#", "#", "#", "#", "#", "C" };
        String[] open = { "#", " ", " ", " ", " ", " ", "C" };

        defineStructure(builder.parseVertical(closed, open, 'C'));
    }

    @Override
    public ResourceLocation getBlockId() {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blast_door");
    }

    @Override
    public String[] getPartNames() {
        return new String[] { "base", "block", "tooth", "slider1", "slider2", "slider3", "slider4" };
    }

    @Override
    public int getOpenTime() {
        return TRAVEL_TICKS;
    }

    // Колонна створки: локальные y1..y5, колонка j = y-1 (0..4), порог j/5.
    // Строгий порог в isPartRetracted даёт: открытие — ячейка k+1 освобождается
    // при openTicks >= 20k (после ремапа +1 тик ниже), закрытие — твердеет при
    // openTicks <= 20k. Ячейка +6 (j=5) вне диапазона — никогда не ретрактится.
    @Override
    public int[][] getDoorOpenRanges() {
        return new int[][] { { 0, 1, 0, 5, 6, 1 } };
    }

    @Override
    public float getRetractionThreshold(int j, int steps, int rangeIdx) {
        return (float) j / LEAF_TOP;
    }

    /**
     * Ремап прогресса открытия на +1 тик: NEXT переключает ячейку k+1 при
     * {@code timer >= 20k}, а строгий порог даёт {@code openTicks > 20k}.
     * Прибавка 1/100 (один тик) выравнивает флип на ровно 20k. Защита
     * {@code progress <= 0} держит ячейку +1 сплошной в тик клика — как в NEXT,
     * где timer=0 и travel() стартует со следующего тика.
     */
    @Override
    public float getCollisionProgress(byte state, float progress) {
        if (state != 3) return progress;
        if (progress <= 0.0f) return 0.0f;
        return Math.min(1.0f, progress + 1.0f / TRAVEL_TICKS);
    }

    // ==================== Рендер (порт RenderBlastDoor.java:91-123) ====================

    /**
     * Подъём створки в блоках: 5 * openTicks / 100. У NEXT ramp = 5 - rise в ОБОИХ
     * направлениях движения (RenderBlastDoor.ramp: закрыто=5, открыто=0,
     * MOVING = 5*(isOpening ? 1-t : t)), а геометрия рисуется на высоте
     * authored + (5 - ramp) = authored + rise.
     */
    private static float rise(float openTicks) {
        return EXTEND * openTicks / TRAVEL_TICKS;
    }

    @Override
    public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
        float r = rise(openTicks);
        switch (partName) {
            // Корпус: OBJ авторован на y3..4, NEXT рисует его со сдвигом +3 → ячейка +6
            case "block" -> set(trans, 0, 3.0F, 0);
            case "tooth" -> set(trans, 0, r, 0);
            default -> {
                if (partName.startsWith("slider")) {
                    int n = sliderNumber(partName);
                    set(trans, 0, r + (n - 1), 0);
                } else {
                    super.getTranslation(partName, openTicks, child, trans);
                }
            }
        }
    }

    /**
     * Куллинг сегментов (NEXT RenderBlastDoor.java:114-115 {@code if (ramp <= i) break;}):
     * sliderN виден, пока 5 - rise &gt; N, т.е. rise &lt; 5 - N. Верхний сегмент
     * исчезает первым, войдя в корпус; в терминальных состояниях — все видны
     * (закрыто, rise=0) либо все скрыты (открыто, rise=5). Зубец и корпус — всегда.
     */
    @Override
    public boolean doesRender(DoorBlockEntity be, String partName, float openTicks, boolean child) {
        if (!partName.startsWith("slider")) return true;
        return rise(openTicks) < EXTEND - sliderNumber(partName);
    }

    private static int sliderNumber(String partName) {
        try {
            return Integer.parseInt(partName.substring("slider".length()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
