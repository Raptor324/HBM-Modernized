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
public abstract class BaseHbmBlockEntity extends BlockEntity implements com.hbm_m.api.render.RenderBoundsProvider {

    public BaseHbmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
        applyClientUpdate(tag);
    }

    @Override
    public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = PlatformHooks.getItemTag(pkt);
        if (tag != null) applyClientUpdate(tag);
    }
    //?}

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
     * Возвращает IEnergyStorage для указанной стороны
     */
    public @Nullable Object getEnergyStorage(@Nullable net.minecraft.core.Direction side) {
        return null;
    }
}
