package com.hbm_m.block.entity.explosives;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.entity.BaseMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Базовый BlockEntity для всех пусковых площадок.
 *
 * ВНИМАНИЕ: Вся старая ракетная логика из 1.7.10 специально вырезана.
 * Здесь остаётся только:
 * - инвентарь (7 слотов, как в оригинале)
 * - энергия (через BaseMachineBlockEntity)
 * - простое состояние площадки (STATE_MISSING / STATE_LOADING / STATE_READY)
 *
 * Логику ракет, топлива, OpenComputers и прочее нужно будет
 * портировать отдельно. Сейчас оставлены только безопасные заглушки.
 */
public abstract class LaunchPadBaseBlockEntity extends BaseMachineBlockEntity {

    // Слоты как в старом контейнере:
    // 0 – ракета
    // 1 – дизайнатор
    // 2 – батарея
    // 3 – топливо (вход)
    // 4 – топливо (выход)
    // 5 – окислитель (вход)
    // 6 – окислитель (выход)
    public static final int SLOT_MISSILE = 0;
    public static final int SLOT_DESIGNATOR = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FUEL_IN = 3;
    public static final int SLOT_FUEL_OUT = 4;
    public static final int SLOT_OXIDIZER_IN = 5;
    public static final int SLOT_OXIDIZER_OUT = 6;
    public static final int SLOT_COUNT = 7;

    public static final int STATE_MISSING = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_READY = 2;

    protected static final long MAX_POWER = 100_000L;
    protected static final long MAX_RECEIVE = 1_000L;

    /**
     * Текущее логическое состояние площадки (для GUI).
     * Пока вычисляется очень грубо: площадка всегда "не готова".
     * Реальная логика будет добавлена вместе с ракетами.
     */
    protected int state = STATE_MISSING;

    /**
     * Отслеживание редстоуна от нескольких блоков-пускателей.
     * Порт старой логики: сумма активных позиций.
     */
    protected int redstonePower = 0;
    protected int prevRedstonePower = 0;
    protected final Set<BlockPos> activatedBlocks = new HashSet<>();

    protected LaunchPadBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    // -----------------------
    // Tick‑логика (заглушка)
    // -----------------------

    /**
     * Общий server‑tick для всех пусковых площадок.
     * Сейчас только инициализирует энергетическую сеть.
     */
    protected static void commonServerTick(Level level, BlockPos pos, BlockState state,
                                           LaunchPadBaseBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        be.ensureNetworkInitialized();
    }

    // -----------------------
    // API для GUI / меню
    // -----------------------

    public int getState() {
        return state;
    }

    /**
     * Старый GUI показывал 3 индикатора:
     * - топливо
     * - окислитель
     * - готовность ракеты
     *
     * Пока всегда возвращаем 0 (не определено), чтобы GUI мог
     * отличить «реализовано» от «не реализовано».
     */
    public int getFuelState() {
        return 0;
    }

    public int getOxidizerState() {
        return 0;
    }

    /**
     * Заглушка проверки ракеты. Сейчас всегда false.
     * Когда логика ракет будет портирована, сюда вернётся настоящая проверка.
     */
    public boolean isMissileValid() {
        return false;
    }

    public boolean isMissileValid(@NotNull net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    /**
     * Полная готовность к пуску. Сейчас всегда false.
     */
    public boolean canLaunch() {
        return false;
    }

    // -----------------------
    // Редстоун‑логика (упрощённый порт)
    // -----------------------

    public void updateRedstonePower(BlockPos pos, boolean powered) {
        boolean contained = activatedBlocks.contains(pos);
        if (!contained && powered) {
            activatedBlocks.add(pos);
            if (redstonePower == -1) {
                redstonePower = 0;
            }
            redstonePower++;
        } else if (contained && !powered) {
            activatedBlocks.remove(pos);
            redstonePower--;
            if (redstonePower == 0) {
                redstonePower = -1;
            }
        }
    }

    // -----------------------
    // NBT
    // -----------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("launchpad_state", state);
        tag.putInt("launchpad_redstone", redstonePower);
        tag.putInt("launchpad_prev_redstone", prevRedstonePower);
        // activatedBlocks можно добавить позже, когда появится реальная логика пуска
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        state = tag.getInt("launchpad_state");
        redstonePower = tag.getInt("launchpad_redstone");
        prevRedstonePower = tag.getInt("launchpad_prev_redstone");
    }

    // -----------------------
    // BaseMachineBlockEntity overrides
    // -----------------------

    @Override
    protected boolean isItemValidForSlot(int slot, net.minecraft.world.item.ItemStack stack) {
        // Пока никаких особых ограничений, кроме базового количества слотов.
        return slot >= 0 && slot < SLOT_COUNT;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        // Конкретное меню зависит от типа площадки (обычная / большая / ржавая),
        // поэтому реализацию оставляем в подклассах.
        throw new UnsupportedOperationException("LaunchPadBaseBlockEntity is abstract; createMenu must be implemented in subclasses.");
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        // Площадки питаются со всех сторон, кроме верха/низа можно будет
        // ограничить позже, если потребуется.
        return true;
    }
}
