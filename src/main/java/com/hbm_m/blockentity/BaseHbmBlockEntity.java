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
    //  1:1 TileEntityLoadedBase.checkTilt - Maschinen ohne tragfaehiges Fundament kippen.
    // ═════════════════════════════════════════════════════════════════════════════════════

    public enum TiltType { UNAVOIDABLE, CONFIG }

    public boolean tilted = false;
    public int tiltBlocksChecked = 0;
    public int tiltBlocksValid = 0;

    /**
     * Prueft pro Sekunde einen Bodenblock; nach einem vollen Durchgang ist die Maschine gekippt, wenn weniger als 95 %
     * tragen. Schwere Maschinen brauchen volle, undurchsichtige Bloecke ohne Sand/Wolle/Erde und mindestens die
     * Explosionsfestigkeit von Stein, normale nur eine feste Oberseite ohne Sand, tote/oelige Erde und rissigen Stein.
     */
    public void checkTilt(TiltType cfg, boolean extraHeavy) {
        if (level == null || level.isClientSide) return;
        boolean doesTilt = false;
        if (cfg == TiltType.UNAVOIDABLE) doesTilt = true;
        if (cfg == TiltType.CONFIG && com.hbm_m.config.ModClothConfig.get().enableMachineGravity) doesTilt = true;
        if (cfg == TiltType.CONFIG && com.hbm_m.config.GeneralConfig.enable528 && com.hbm_m.config.ModClothConfig.get().enable528MachineGravity) doesTilt = true;

        if (!doesTilt) { setTilted(false); return; }
        if (this.getFloorCount() <= 0) { setTilted(false); return; }
        // Original: BlockPos.getIdentity
        int identity = (worldPosition.getY() + worldPosition.getZ() * 27644437) * 27644437 + worldPosition.getX();
        if ((level.getGameTime() + identity) % 20 != 0) return;

        if (this.tiltBlocksChecked >= this.getFloorCount()) {

            if (this.tiltBlocksValid >= this.tiltBlocksChecked * 0.95) {
                setTilted(false);
            } else {
                if (!this.tilted) level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        com.hbm_m.sound.HbmSoundsNT.get("hbm:block.metalImpact"), net.minecraft.sounds.SoundSource.BLOCKS, 3F, 1F);
                setTilted(true);
            }

            this.setChanged();
            this.tiltBlocksChecked = 0;
            this.tiltBlocksValid = 0;
        }

        BlockPos pos = getFloorPosFromIndex(this.tiltBlocksChecked);
        if (pos == null) return;

        BlockState ground = level.getBlockState(pos);
        this.tiltBlocksChecked++;

        if (extraHeavy) {
            if (!ground.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP)) return;
            if (!ground.isRedstoneConductor(level, pos)) return;
            if (isSandMaterial(ground) || ground.is(net.minecraft.tags.BlockTags.WOOL) || isGroundMaterial(ground)) return;
            if (ground.getBlock().getExplosionResistance() < net.minecraft.world.level.block.Blocks.STONE.getExplosionResistance()) return;
            this.tiltBlocksValid++;
        } else {
            if (!ground.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP)) return;
            if (isSandMaterial(ground)) return;
            if (ground.is(com.hbm_m.block.ModBlocks.DIRT_DEAD.get()) || ground.is(com.hbm_m.block.ModBlocks.DIRT_OILY.get())
                    || ground.is(com.hbm_m.block.ModBlocks.STONE_CRACKED.get())) return;
            this.tiltBlocksValid++;
        }
    }

    /** Original {@code Material.sand}: Sand, Kies, Seelensand. */
    private static boolean isSandMaterial(BlockState s) {
        return s.is(net.minecraft.tags.BlockTags.SAND) || s.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                || s.is(net.minecraft.world.level.block.Blocks.SOUL_SAND) || s.is(net.minecraft.world.level.block.Blocks.SUSPICIOUS_GRAVEL);
    }

    /** Original {@code Material.ground}: Erde samt Varianten, Ackerboden (Gras und Myzel waren Material.grass). */
    private static boolean isGroundMaterial(BlockState s) {
        return s.is(net.minecraft.world.level.block.Blocks.DIRT) || s.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                || s.is(net.minecraft.world.level.block.Blocks.PODZOL) || s.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)
                || s.is(net.minecraft.world.level.block.Blocks.FARMLAND) || s.is(net.minecraft.world.level.block.Blocks.DIRT_PATH)
                || s.is(net.minecraft.world.level.block.Blocks.MUD);
    }

    private void setTilted(boolean t) {
        if (this.tilted != t) {
            this.tilted = t;
            this.setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getFloorCount() { return 0; }
    public @Nullable BlockPos getFloorPosFromIndex(int index) { return null; }

    public BlockPos standardFloor3x3(int index) {
        return new BlockPos(worldPosition.getX() - 1 + (index / 2) * 2, worldPosition.getY() - 1, worldPosition.getZ() - 1 + (index % 2) * 2);
    }
    public BlockPos standardFloor5x5(int index) {
        return new BlockPos(worldPosition.getX() - 2 + (index / 3) * 2, worldPosition.getY() - 1, worldPosition.getZ() - 2 + (index % 3) * 2);
    }
    public BlockPos standardFloor7x7(int index) {
        return new BlockPos(worldPosition.getX() - 3 + (index / 4) * 2, worldPosition.getY() - 1, worldPosition.getZ() - 3 + (index % 4) * 2);
    }

    /** Liest "tilted" aus einem Client-Update; bei Aenderung wird das (gekippte) Blockmodell neu gebaut. */
    private void readTiltClient(@NotNull CompoundTag tag) {
        boolean t = tag.getBoolean("tilted");
        if (t != this.tilted) {
            this.tilted = t;
            if (level != null && level.isClientSide) {
                //? if forge {
                this.requestModelDataUpdate();
                //?}
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
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

    // ═════════════════════════════════════════════════════════════════════════════════════
    //  Stonecutter-гатиннг. В дочерних классах НЕТ переопределения saveAdditional/load.
    //  Вся версионная магия собрана здесь, один раз на весь проект.
    // ═════════════════════════════════════════════════════════════════════════════════════

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("tilted", tilted);
        writeNbtData(tag, null);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.tilted = tag.getBoolean("tilted");
        readNbtData(tag, null);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("tilted", tilted);
        writeNbtData(tag, registries);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tilted = tag.getBoolean("tilted");
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
        tag.putBoolean("tilted", tilted);
        writeNbtData(tag, null);
        return tag;
    }
    //?} elif neoforge {
    /*@Override
    public @NotNull CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("tilted", tilted);
        writeNbtData(tag, registries);
        return tag;
    }
    *///?} else {
    /*@Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("tilted", tilted);
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
        readTiltClient(tag);
        applyClientUpdate(tag);
    }

    @Override
    public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = PlatformHooks.getItemTag(pkt);
        if (tag != null) { readTiltClient(tag); applyClientUpdate(tag); }
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
