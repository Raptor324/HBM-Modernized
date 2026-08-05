package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Порт {@code TileEntityMachineRadarScreen} из 1.7.10.
 *
 * Пустая «доска», на которую радар (через radar linker в слоте SLOT_SCREEN_LINKER)
 * каждую серверную-тировку копирует свой список целей. Клиент рендерит их на экране
 * через {@code MachineRadarScreenRenderer}.
 *
 * Поля linked/refX/refY/refZ/range/entries синкаются через getUpdateTag.
 */
public class MachineRadarScreenBlockEntity extends BlockEntity {

    private static final int MAP_SYNC_SLICES = 5;
    private boolean fullMapSyncPending = true;

    public final List<int[]> entries = new ArrayList<>();
    public int refX;
    public int refY;
    public int refZ;
    public int range;
    public boolean linked;
    /** Режим HeightMap, переданный от связанного радара. */
    public boolean showMap;
    /** Карта высот экрана; пустые ячейки остаются прозрачными на клиенте. */
    public byte[] heightMap = new byte[MachineRadarBlockEntity.MAP_LENGTH];

    public MachineRadarScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_SCREEN_BE.get(), pos, state);
    }

    /**
     * Вызывается радаром серверно: принимает список целей и координаты радара-источника
     * (порт {@code TileEntityMachineRadarNT.updateEntity} → screen.entries = ...).
     *
     * <p>setChanged() вызывается ТОЛЬКО при реальной смене персистентного состояния
     * (линк/координаты радара/режим карты). Координаты целей и содержимое карты —
     * транзитные данные: они синкаются на клиент по сети (sendBlockUpdated), но
     * не должны помечать чанк грязным и заставлять сервер каждые 5 тиков
     * сериализовать 40 КБ height_map на диск.
     *
     * @return true, если набор целей изменился с прошлого вызова.
     */
    public boolean receiveFromRadar(List<int[]> sourceEntries, int refX, int refY, int refZ, int range,
                                    boolean showMap, byte[] sourceMap) {
        if (level == null || level.isClientSide) {
            return false;
        }

        // Канонизируем список, чтобы сравнивать набор целей по entityID,
        // а не по порядку сканирования (он недетерминирован для HashSet/mixed списков).
        List<int[]> canon = new ArrayList<>(sourceEntries.size());
        for (int[] e : sourceEntries) {
            if (e != null && e.length >= 5) {
                canon.add(e);
            }
        }
        canon.sort((a, b) -> Integer.compare(a.length >= 7 ? a[6] : -1, b.length >= 7 ? b[6] : -1));

        boolean targetsChanged = canon.size() != this.entries.size();
        if (!targetsChanged) {
            // Грубый детект изменений: суммы координат/типов/eid. Для радарного
            // экрана ложноположительный детект безвреден (лишний сетевой пакет),
            // ложноотрицательный сведётся к пропуску одного периода обновления.
            long oldSig = 0, newSig = 0;
            for (int i = 0; i < canon.size(); i++) {
                int[] ne = canon.get(i);
                int[] oe = this.entries.get(i);
                newSig += ne[0] * 31L + ne[1] * 37L + ne[2] * 41L + ne[3] * 43L + ne[4]
                        + (ne.length >= 7 ? ne[6] * 47L : 0);
                oldSig += oe[0] * 31L + oe[1] * 37L + oe[2] * 41L + oe[3] * 43L + oe[4]
                        + (oe.length >= 7 ? oe[6] * 47L : 0);
            }
            targetsChanged = oldSig != newSig;
        }

        if (targetsChanged) {
            this.entries.clear();
            for (int[] e : canon) {
                this.entries.add(e.clone());
            }
        }

        boolean persistedChanged = this.refX != refX || this.refY != refY || this.refZ != refZ
                || this.range != range;
        this.refX = refX;
        this.refY = refY;
        this.refZ = refZ;
        this.range = range;
        this.linked = true;

        boolean mapChanged = this.showMap != showMap;
        this.showMap = showMap;
        if (mapChanged && showMap) {
            // Переключение режима должно показать уже построенную карту целиком,
            // а не заставлять экран ждать последовательной отправки всех срезов.
            fullMapSyncPending = true;
        }
        if (showMap && sourceMap != null && sourceMap.length >= MachineRadarBlockEntity.MAP_LENGTH
                && (mapChanged || level.getGameTime() % 5L == 0L)) {
            if (heightMap == null || heightMap.length != MachineRadarBlockEntity.MAP_LENGTH) {
                heightMap = new byte[MachineRadarBlockEntity.MAP_LENGTH];
            }
            System.arraycopy(sourceMap, 0, heightMap, 0, MachineRadarBlockEntity.MAP_LENGTH);
            persistedChanged = true; // height_map сериализуется на диск
        } else if (!showMap && mapChanged) {
            Arrays.fill(heightMap, (byte) 0);
            persistedChanged = true;
        }

        // Диск: только при изменении персистентного состояния.
        if (persistedChanged) {
            setChanged();
        }

        // Сеть: при любом релевантном изменении, но не чаще раза в 5 тиков
        // (движущиеся цели не требуют пакета каждый тик — 4 Гц достаточно).
        if (!isRemoved()
                && (targetsChanged || mapChanged || level.getGameTime() % 5L == 0L)) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return targetsChanged;
    }

    /** Периодически проверяет источник, чтобы сломанный радар не оставлял экран linked=true. */
    public static void tick(Level level, BlockPos pos, BlockState state, MachineRadarScreenBlockEntity screen) {
        if (level.isClientSide || level.getGameTime() % 5L != 0L) {
            return;
        }
        // Экран никогда не был прилинкован (координаты радара неизвестны) —
        // нечего проверять, и чанк (0,0,0) дёргать не нужно.
        if (!screen.linked && screen.refX == 0 && screen.refY == 0 && screen.refZ == 0) {
            return;
        }
        BlockPos radarPos = new BlockPos(screen.refX, screen.refY, screen.refZ);
        // hasChunkAt обязателен: level.getBlockEntity() в 1.20.1 синхронно загружает
        // (и при необходимости генерирует) чанк. Экран не должен тянуть чанк радара,
        // иначе на выходе из мира сохранение конкурирует с загрузкой чанков.
        if (!level.hasChunkAt(radarPos)) {
            return;
        }
        BlockEntity source = level.getBlockEntity(radarPos);
        if (source instanceof MachineRadarBlockEntity radar
                && pos.equals(radar.getScreenLinkerPos())) {
            // Дублируем источник синхронизации на стороне экрана. Это устраняет
            // зависимость от порядка тиков двух BlockEntity и гарантирует, что BER
            // получает linked/entries/map даже сразу после загрузки чанка.
            screen.receiveFromRadar(radar.nearbyMissiles,
                    radar.getBlockPos().getX(), radar.getBlockPos().getY(), radar.getBlockPos().getZ(),
                    radar.getRange(), radar.showMap, radar.map);
        } else {
            screen.unlink();
        }
    }

    /**
     * Разрыв связи с радаром-источником (когда из радара убран radar linker).
     * Сбрасывает linked/entries/range и синкает клиентам — экран переключается
     * на статичный «шум» (порт отсутствия slots[8]==radar_linker в 1.7.10).
     */
    public void unlink() {
        if (level == null || level.isClientSide) {
            return;
        }
        // Идемпотентность: если связи уже нет и целей нет — ничего не изменилось,
        // незачем помечать чанк грязным и слать пакет каждые 5 тиков.
        if (!this.linked && this.entries.isEmpty()) {
            return;
        }
        this.linked = false;
        this.entries.clear();
        this.range = 0;
        this.refX = 0;
        this.refY = 0;
        this.refZ = 0;
        this.showMap = false;
        if (heightMap != null) {
            Arrays.fill(heightMap, (byte) 0);
        }
        setChanged();
        if (!isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("linked", linked);
        tag.putInt("refX", refX);
        tag.putInt("refY", refY);
        tag.putInt("refZ", refZ);
        tag.putInt("range", range);
        tag.putBoolean("show_map", showMap);
        if (showMap && level != null && !level.isClientSide
                && heightMap != null && heightMap.length == MachineRadarBlockEntity.MAP_LENGTH) {
            if (fullMapSyncPending) {
                tag.putByteArray("mapFull", Arrays.copyOf(heightMap, MachineRadarBlockEntity.MAP_LENGTH));
                fullMapSyncPending = false;
            } else {
                int start = (int) (level.getGameTime() % MachineRadarBlockEntity.MAP_SLICES);
                byte[] slices = new byte[MAP_SYNC_SLICES * MachineRadarBlockEntity.MAP_SLICE_SIZE];
                for (int i = 0; i < MAP_SYNC_SLICES; i++) {
                    int slice = (start + i) % MachineRadarBlockEntity.MAP_SLICES;
                    System.arraycopy(heightMap, slice * MachineRadarBlockEntity.MAP_SLICE_SIZE,
                            slices, i * MachineRadarBlockEntity.MAP_SLICE_SIZE,
                            MachineRadarBlockEntity.MAP_SLICE_SIZE);
                }
                tag.putInt("mapSliceStart", start);
                tag.putInt("mapSliceCount", MAP_SYNC_SLICES);
                tag.putByteArray("mapSlices", slices);
            }
        }
        tag.putInt("count", entries.size());
        for (int i = 0; i < entries.size(); i++) {
            int[] e = entries.get(i);
            tag.putIntArray("e" + i, e);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        linked = tag.getBoolean("linked");
        refX = tag.getInt("refX");
        refY = tag.getInt("refY");
        refZ = tag.getInt("refZ");
        range = tag.getInt("range");
        showMap = tag.getBoolean("show_map");
        if (tag.contains("mapFull")) {
            byte[] fullMap = tag.getByteArray("mapFull");
            if (fullMap.length == MachineRadarBlockEntity.MAP_LENGTH) {
                if (heightMap == null || heightMap.length != MachineRadarBlockEntity.MAP_LENGTH) {
                    heightMap = new byte[MachineRadarBlockEntity.MAP_LENGTH];
                }
                System.arraycopy(fullMap, 0, heightMap, 0, MachineRadarBlockEntity.MAP_LENGTH);
            }
        } else if (tag.contains("mapSlices")) {
            byte[] slices = tag.getByteArray("mapSlices");
            int start = tag.getInt("mapSliceStart");
            int count = Math.min(tag.getInt("mapSliceCount"),
                    slices.length / MachineRadarBlockEntity.MAP_SLICE_SIZE);
            if (start >= 0 && start < MachineRadarBlockEntity.MAP_SLICES && count > 0) {
                if (heightMap == null || heightMap.length != MachineRadarBlockEntity.MAP_LENGTH) {
                    heightMap = new byte[MachineRadarBlockEntity.MAP_LENGTH];
                }
                for (int i = 0; i < count; i++) {
                    int slice = (start + i) % MachineRadarBlockEntity.MAP_SLICES;
                    System.arraycopy(slices, i * MachineRadarBlockEntity.MAP_SLICE_SIZE,
                            heightMap, slice * MachineRadarBlockEntity.MAP_SLICE_SIZE,
                            MachineRadarBlockEntity.MAP_SLICE_SIZE);
                }
            }
        } else if (!showMap && heightMap != null) {
            Arrays.fill(heightMap, (byte) 0);
        }
        entries.clear();
        int count = tag.getInt("count");
        for (int i = 0; i < count; i++) {
            entries.add(tag.getIntArray("e" + i));
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        if (PlatformHooks.getItemTag(pkt) != null) {
            handleUpdateTag(PlatformHooks.getItemTag(pkt));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("linked", linked);
        tag.putInt("refX", refX);
        tag.putInt("refY", refY);
        tag.putInt("refZ", refZ);
        tag.putInt("range", range);
        tag.putBoolean("show_map", showMap);
        if (heightMap != null && heightMap.length == MachineRadarBlockEntity.MAP_LENGTH) {
            // IOWorker пишет chunk NBT на отдельном потоке, а экран обновляет карту
            // из радара каждые 5 тиков. Передаём только снимок массива.
            tag.putByteArray("height_map", Arrays.copyOf(heightMap, MachineRadarBlockEntity.MAP_LENGTH));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linked = tag.getBoolean("linked");
        refX = tag.getInt("refX");
        refY = tag.getInt("refY");
        refZ = tag.getInt("refZ");
        range = tag.getInt("range");
        showMap = tag.getBoolean("show_map");
        if (tag.contains("height_map")) {
            byte[] savedMap = tag.getByteArray("height_map");
            if (savedMap.length == MachineRadarBlockEntity.MAP_LENGTH) {
                // clone() обязателен: массив принадлежит ByteArrayTag загруженного
                // chunk NBT. Без копии receiveFromRadar правил бы тот же массив,
                // который IOWorker сериализует в другом потоке.
                heightMap = savedMap.clone();
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 1,
                worldPosition.getY(),
                worldPosition.getZ() - 1,
                worldPosition.getX() + 2,
                worldPosition.getY() + 2,
                worldPosition.getZ() + 2);
    }
}
