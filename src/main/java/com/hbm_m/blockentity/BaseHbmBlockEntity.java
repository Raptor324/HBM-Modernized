package com.hbm_m.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;

/**
 * Верхняя прослойка над {@link BlockEntity} с минимальным набором stonecutter-ветвлений
 * для версионно-зависимых override'ов ({@code saveAdditional}/{@code load}/{@code getUpdateTag}).
 *
 * <p><b>Цель:</b> убрать дублирование каменной-ножничной
 * логики в каждом BlockEntity. Логика персистенции вынесена в два НЕ-override метода —
 * {@link #writeNbtData(CompoundTag, Provider)} и {@link #readNbtData(CompoundTag, Provider)} —
 * которые дочерние классы реализуют <b>один раз</b> без ветвлений. Прослойка берет на себя
 * маппинг:
 *
 * <ul>
 *   <li>1.20.1 (forge/fabric): {@code saveAdditional(CompoundTag)}/{@code load(CompoundTag)} —
 *       вызывают логику с {@code null} провайдером (1.20.1 не требует Provider в NBT-записи).</li>
 *   <li>1.21.1 (neoforge): {@code saveAdditional(CompoundTag, Provider)}/
 *       {@code loadAdditional(CompoundTag, Provider)} — передают реальный Provider.</li>
 * </ul>
 *
 * <p><b>ВАЖНО:</b> дочерние классы <b>НЕ</b> должны переопределять {@code saveAdditional} или
 * {@code load}/{@code loadAdditional}. Вместо этого реализуют
 * {@link #writeNbtData}/{@link #readNbtData}. Прослойка автоматически вызывает их для обоих
 * версий MC через один и тот же код.
 *
 * <p><b>Синхронизация клиента:</b> переопределение {@code getUpdateTag} также инкапсулировано
 * — он просто берет свежий {@link CompoundTag} у {@link BlockEntity} и вызывает
 * {@link #writeNbtData}. Никаких ветвей в подклассах.
 *
 * <p>Для тонкого client-packet sync (Forge {@code handleUpdateTag}/{@code onDataPacket}) —
 * используйте {@link #applyClientUpdate(CompoundTag)} (метод для переопределения, вызывается
 * прослойкой при получении пакета) — он должен делегировать в {@link #readNbtData}.
 */
public abstract class BaseHbmBlockEntity extends BlockEntity
        implements com.hbm_m.api.render.RenderBoundsProvider, com.hbm_m.api.render.RenderDirtyTracker {

    public BaseHbmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    // ═════════════════════════════════════════════════════════════════════════════════════
    //  Render dirty tracking (GPU-driven сбор машин: см. com.hbm_m.api.render.RenderDirtyTracker)
    //  Чистая машина пропускает ежекадровую сборку (матрицы/свет/сравнение записей) и
    //  подтверждает присутствие roster-assert'ом. Свет/fade не имеют событийной модели —
    //  обновляются TTL-пересбором (штамп gameTick, фаза размазана по позиции).
    // ═════════════════════════════════════════════════════════════════════════════════════

    /** TTL пересбора света/fade в тиках — консистентен с LightSampleCache.LIGHT_TTL_TICKS. */
    private static final long NUCLEUS_RENDER_REFRESH_TICKS = 15;

    private boolean nucleusRenderDirty = true;
    private long nucleusLastRenderTick = Long.MIN_VALUE;
    private long nucleusLastRenderWorldGen = -1L;

    @Override
    public boolean isRenderDirty() {
        return nucleusRenderDirty;
    }

    @Override
    public void markRenderDirty() {
        nucleusRenderDirty = true;
    }

    @Override
    public boolean isRenderStale(long gameTick, long worldGen) {
        if (nucleusRenderDirty || nucleusLastRenderWorldGen != worldGen) {
            return true;
        }
        long since = gameTick - nucleusLastRenderTick;
        if (since < 0) {
            return true; // смена измерения/откат времени
        }
        // Фаза по позиции: машины фермы не пересобираются все в один тик.
        long phase = (worldPosition.asLong() >>> 4) & 7;
        return since >= NUCLEUS_RENDER_REFRESH_TICKS + phase;
    }

    @Override
    public void onRenderCollected(long gameTick, long worldGen) {
        nucleusRenderDirty = false;
        nucleusLastRenderTick = gameTick;
        nucleusLastRenderWorldGen = worldGen;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Загрузка чанка / пересоздание BE при смене blockstate — запись надо собрать заново.
        nucleusRenderDirty = true;
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        // LevelChunk переиспользует BE-инстанс при смене состояния того же блока.
        nucleusRenderDirty = true;
    }

    /**
     * Render bounding box по умолчанию — 1 блок (чуть расширенный).
     * На 1.21.1 NeoForge BER-пасс зовёт его через {@link com.hbm_m.api.render.RenderBoundsProvider}
     * (ванильного BlockEntity#getRenderBoundingBox там нет); на 1.20.1 Forge это @Override.
     * Мультиблоки переопределяют на AABB всей структуры.
     */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(0.5D);
    }

    // ═════════════════════════════════════════════════════════════════════════════════════
    //  Централизованный дроп инвентаря при разрушении блока. Платформенно-независимо:
    //  один источник правды — {@link #getItemHandler(net.minecraft.core.Direction)},
    //  который переопределяют машины с инвентарём.
    //  ВАЖНО: НЕ переопределять setRemoved() для дропа, так как на 1.21.1 / NeoForge
    //  при выгрузке чанков и сохранении мира ваниль вызывает setRemoved(), что приводило
    //  к дюпу предметов на пол при перезаходе в мир.
    // ═════════════════════════════════════════════════════════════════════════════════════

    /** Переопределить и вернуть {@code false}, если машина дропает инвентарь сама или хранит в NBT предмета. */
    protected boolean dropInventoryOnRemove() { return true; }

    /**
     * Выбрасывает содержимое инвентаря в мир при фактическом разрушении блока / мультиблока.
     * Очищает слоты после выброса для предотвращения повторного дропа.
     */
    public void dropInventory() {
        if (level == null || level.isClientSide || !dropInventoryOnRemove()) return;
        if (com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) return;
        Object handler = getItemHandler(null);
        if (handler instanceof com.hbm_m.platform.ModItemStackHandler h) {
            for (int i = 0; i < h.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = h.getStackInSlot(i);
                if (stack != null && !stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level,
                            worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                    h.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════
    //  Единая точка персистенции — переопределяйте ЭТИ методы в дочерних классах.
    //  Никаких stonecutter-ветвей, никаких версионных сигнатур.
    // ═════════════════════════════════════════════════════════════════════════════════════

    /**
     * Сохраняет данные BlockEntity в {@code tag}. Реализация НЕ должна вызывать
     * {@code super.saveAdditional(...)} — прослойка делает это сама.
     *
     * <p><b>Provider</b>: на 1.21.1 это реальный {@link HolderLookup.Provider} (берётся из
     * {@code saveAdditional}/{@code loadAdditional}); на 1.20.1 — {@code null} (1.20.1 не
     * использует Provider в NBT). Если методу нужны реестры на 1.21.1 — используйте
     * {@code if (registries != null) ...}. {@link PlatformHooks#saveItemStack} и
     * {@link PlatformHooks#itemStackOf} принимают Provider как {@code null}-safe аргумент.
     *
     * @param registries 1.21.1 — реальный Provider, 1.20.1 — null
     */
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        // default: no-op. Переопределяется дочерними классами для записи своих данных.
    }

    /**
     * Читает данные BlockEntity из {@code tag}. Реализация НЕ должна вызывать
     * {@code super.load(...)} — прослойка делает это сама.
     *
     * @param registries 1.21.1 — реальный Provider, 1.20.1 — null
     */
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        // default: no-op. Переопределяется дочерними классами для чтения своих данных.
    }

    /**
     * Хук, вызываемый при получении клиентского update-packet (аналог Forge
     * {@code handleUpdateTag} / {@code onDataPacket}). По умолчанию — {@link #readNbtData}
     * с {@code null} провайдером (как в 1.20.1 handleUpdateTag → load(tag)).
     *
     * <p>Переопределяйте, если нужно другое поведение (например, invalidate cached
     * render-state, как в {@code MachineFluidTankBlockEntity}).
     */
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        readNbtData(tag, null);
    }

    /** Как выше, но с реестрами (neoforge 1.21.1 передаёт реальный Provider из пакета). */
    protected void applyClientUpdate(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        readNbtData(tag, registries);
    }

    // ═════════════════════════════════════════════════════════════════════════════════════
    //  Stonecutter-гатиннг. В дочерних классах НЕТ переопределения saveAdditional/load.
    //  Вся версионная магия собрана здесь, один раз на весь проект.
    // ═════════════════════════════════════════════════════════════════════════════════════

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        writeNbtData(tag, null);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        readNbtData(tag, null);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeNbtData(tag, registries);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readNbtData(tag, registries);
    }
    *///?}

    // ═════════════════════════════════════════════════════════════════════════════════════
    //  Синхронизация клиента. Ветвление — единственное место на проекте.
    // ═════════════════════════════════════════════════════════════════════════════════════

    //? if < 1.21.1 {
    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeNbtData(tag, null);
        return tag;
    }
    //?} elif neoforge {
    /*@Override
    public @NotNull CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeNbtData(tag, registries);
        return tag;
    }
    *///?} else {
    /*@Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeNbtData(tag, registries);
        return tag;
    }
    *///?}

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if forge {
    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag) {
        nucleusRenderDirty = true;
        applyClientUpdate(tag);
    }

    @Override
    public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt) {
        nucleusRenderDirty = true;
        CompoundTag tag = PlatformHooks.getItemTag(pkt);
        if (tag != null) applyClientUpdate(tag);
    }
    //?} else {
    /*// NeoForge 1.21.1: дефолт IBlockEntityExtension.onDataPacket ПРОПУСКАЕТ ПУСТОЙ тег
    // (if (!tag.isEmpty())), из-за чего сброс опциональных данных (например, camo=null
    // у paintable-кабеля) не доезжал до клиента — тег после сброса пустой.
    // Переопределяем с безусловным применением.
    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        nucleusRenderDirty = true;
        applyClientUpdate(tag, registries);
    }

    @Override
    public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        nucleusRenderDirty = true;
        applyClientUpdate(pkt.getTag(), registries);
    }
    *///?}

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    //  Capability Providers (Автоматизация для NeoForge и платформенных адаптеров)
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Возвращает IItemHandler (или ModItemStackHandler) для указанной стороны
     */
    public @Nullable Object getItemHandler(@Nullable net.minecraft.core.Direction side) {
        return null;
    }

    /**
     * Возвращает IFluidHandler (NeoForge / Forge) для указанной стороны
     */
    public @Nullable Object getFluidHandler(@Nullable net.minecraft.core.Direction side) {
        if (!isFluidSideAllowed(side)) return null;
        if (this instanceof com.hbm_m.api.fluids.IFluidUserMK2 mk2) {
            //? if forge {
            return null; // На Forge разруливается через getCapability
            //?} elif neoforge {
            /*return new com.hbm_m.api.fluids.NeoForgeFluidHandlerMK2(mk2);
            *///?}
        }
        return null;
    }

    /**
     * Пускает ли машина жидкость через эту грань. {@code side == null} — несторонний запрос.
     * На Forge то же самое делал сторонний фильтр в {@code getCapability}; на NeoForge капабилити
     * вешается через ModCapabilities и сторону не проверяет, поэтому фильтр живёт здесь.
     */
    protected boolean isFluidSideAllowed(@Nullable net.minecraft.core.Direction side) {
        return true;
    }

    /**
     * Возвращает IEnergyStorage для указанной стороны
     */
    public @Nullable Object getEnergyStorage(@Nullable net.minecraft.core.Direction side) {
        return null;
    }

    /**
     * Участвует ли BE в энергетической сети вообще (провода, зарядка, выдача).
     * Машины без энергии (пресс) переопределяют в {@code false}: на Forge их getCapability
     * возвращает empty, на NeoForge ModCapabilities не регистрирует на них energy-капы.
     */
    public boolean joinsEnergyNetwork() {
        return true;
    }
}
