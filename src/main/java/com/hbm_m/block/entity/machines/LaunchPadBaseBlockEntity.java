package com.hbm_m.block.entity.machines;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.entity.missile.MissileTestEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.missile.MissileItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    /** Регистрация ракет: Item -> тип сущности. Пока только прототип. */
    protected static final Map<Item, EntityTypeRef> MISSILES = new HashMap<>();

    static {
        // Прототип: missile_test -> MissileTestEntity
        MISSILES.put(ModItems.MISSILE_TEST.get(), new EntityTypeRef());
    }

    /**
     * Текущее логическое состояние площадки (для GUI).
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

    public int getFuelState() {
        ItemStack missileStack = inventory.getStackInSlot(SLOT_MISSILE);
        if (!isMissileValid(missileStack)) {
            return 0;
        }
        MissileItem missile = (MissileItem) missileStack.getItem();
        if (missile.fuel == MissileItem.MissileFuel.SOLID) {
            return 0;
        }
        // Пока считаем, что бак либо пуст, либо полон
        return this.energy >= 75_000L ? 1 : -1;
    }

    public int getOxidizerState() {
        return getFuelState();
    }

    /**
     * Заглушка проверки ракеты. Сейчас всегда false.
     * Когда логика ракет будет портирована, сюда вернётся настоящая проверка.
     */
    public boolean isMissileValid() {
        ItemStack stack = inventory.getStackInSlot(SLOT_MISSILE);
        return isMissileValid(stack);
    }

    public boolean isMissileValid(@NotNull net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof MissileItem missile)) {
            return false;
        }
        return missile.launchable && MISSILES.containsKey(stack.getItem());
    }

    public boolean canLaunch() {
        return isMissileValid() && getFuelState() == 1 && isReadyForLaunch();
    }

    /** Дополнительные условия в конкретной площадке (задержка, анимации и т.п.). */
    protected abstract boolean isReadyForLaunch();

    /**
     * Запуск по координатам (X/Z). Возвращает true, если ракета реально запущена.
     */
    public boolean launchToCoordinate(int targetX, int targetZ) {
        if (!canLaunch() || level == null || level.isClientSide) {
            return false;
        }

        Entity missile = instantiateMissile(targetX, targetZ);
        if (missile != null) {
            finalizeLaunch(missile);
            return true;
        }
        return false;
    }

    /**
     * Запуск по дизайнатору (слот 1).
     */
    public boolean launchFromDesignator() {
        if (!canLaunch() || level == null || level.isClientSide) {
            return false;
        }

        ItemStack designatorStack = inventory.getStackInSlot(SLOT_DESIGNATOR);
        int targetX = worldPosition.getX();
        int targetZ = worldPosition.getZ();

        if (!designatorStack.isEmpty() && designatorStack.getItem() instanceof IDesignatorItem designator) {
            if (!designator.isReady(level, designatorStack, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) {
                return false;
            }
            net.minecraft.world.phys.Vec3 coords = designator.getCoords(level, designatorStack,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
            targetX = (int) Math.floor(coords.x);
            targetZ = (int) Math.floor(coords.z);
        }

        return launchToCoordinate(targetX, targetZ);
    }

    /**
     * Создание сущности ракеты из текущего слота.
     */
    protected Entity instantiateMissile(int targetX, int targetZ) {
        if (level == null) {
            return null;
        }
        ItemStack stack = inventory.getStackInSlot(SLOT_MISSILE);
        if (!isMissileValid(stack)) {
            return null;
        }

        // Сейчас у нас только один тип ракеты – MissileTestEntity
        MissileTestEntity missile = new MissileTestEntity(level);
        missile.initLaunch(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + getLaunchOffset(),
                worldPosition.getZ() + 0.5D,
                targetX, targetZ
        );
        return missile;
    }

    /**
     * Финализация пуска: спавн entity, звук, расход ресурсов.
     */
    protected void finalizeLaunch(Entity missile) {
        if (level == null || level.isClientSide) {
            return;
        }

        level.addFreshEntity(missile);
        level.playSound(null,
                worldPosition.getX() + 0.5D,
                worldPosition.getY(),
                worldPosition.getZ() + 0.5D,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                2.0F, 1.0F);

        this.energy = Math.max(0, this.energy - 75_000L);

        ItemStack stack = inventory.getStackInSlot(SLOT_MISSILE);
        stack.shrink(1);
        setChanged();
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

    /** Смещение точки старта ракеты относительно верха блока. */
    protected double getLaunchOffset() {
        return 1.0D;
    }

    /**
     * Внутренняя заглушка для потенциального реестра типов сущностей ракет.
     * Сейчас не содержит ничего, но оставлена для будущего расширения.
     */
    protected static class EntityTypeRef { }

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
