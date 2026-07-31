package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import api.hbm.entity.IRadarDetectable;
import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.IRadarCommandReceiver;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.inventory.menu.MachineRadarMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.entity.missile.MissileBaseEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

public class MachineRadarBlockEntity extends BaseMachineBlockEntity {

    /**
     * Раскладка слотов как в {@code TileEntityMachineRadarNT} / {@code ContainerMachineRadarNT}:
     *   слоты 0..7 — линк-слоты команд (radar_linker на пусковую / designator / sat_relay);
     *   слот 0 также заряжает батарею (chargeTEFromItems(slots, 0, ...));
     *   слот 8   — radar_linker, связывающий радар с блоком Radar Screen;
     *   слот 9   — батарея (chargeTEFromItems(slots, 9, ...)).
     */
    private static final int SLOT_COUNT = 10;
    public static final int SLOT_BATTERY_PRIMARY = 0;
    public static final int SLOT_BATTERY_SECONDARY = 9;
    /** Первый линк-слот команды (всего 8: 0..7). */
    public static final int SLOT_LINK_FIRST = 0;
    public static final int SLOT_LINK_LAST = 7;
    /** Слот radar linker'а для связи с Radar Screen (порт slots[8]). */
    public static final int SLOT_SCREEN_LINKER = 8;

    // Порт значений 1.7.10 TileEntityMachineRadarNT: radarRange=1000, radarBuffer=30, radarAltitude=55.
    public static final int RADAR_RANGE = 1_000;
    public static final int RADAR_LARGE_RANGE = 3_000;
    public static final int RADAR_BUFFER = 30;
    public static final int RADAR_ALTITUDE = 55;
    /** Потребление за тик (порт {@code TileEntityMachineRadarNT.consumption} = 500). */
    public static final long ENERGY_CONSUMPTION = 500L;
    private static final long ENERGY_DRAIN_PER_TICK = ENERGY_CONSUMPTION;
    private static final int MAX_CONTACTS = 64;
    private static final int DEFAULT_MAX_PROGRESS = 200;
    private static final int SONAR_PING_INTERVAL = 80;
    /** Сканирование сущностей не должно выполняться 20 раз в секунду для огромного AABB. */
    private static final int RADAR_SCAN_INTERVAL = 5;

    /** Размер карты высот радара (200x200), как TileEntityMachineRadarNT.map. */
    public static final int MAP_DIM = 200;
    public static final int MAP_LENGTH = MAP_DIM * MAP_DIM; // 40_000
    /** Сколько пикселей карты генерируется/синхронизируется за тик (порт 1.7.10: 100 за тик). */
    static final int MAP_SLICE_SIZE = 100;
    static final int MAP_SLICES = MAP_LENGTH / MAP_SLICE_SIZE; // 400
    /** Интервал построения слайса HeightMap. Один слайс всё равно содержит 100 точек. */
    private static final int MAP_UPDATE_INTERVAL = 5;
    /** Максимум последовательных слайсов в одном сетевом пакете. */
    private static final int MAP_SYNC_MAX_SLICES = 20;
    /** Жёсткий предохранитель от генерации тысяч чанков при включённом generateChunks. */
    private static final int MAX_GENERATED_CHUNKS_PER_MAP = 256;

    /** Макс. подгрузок чанков за тик (порт {@code TileEntityMachineRadarNT.chunkLoadCap} = 10). */
    private static final int CHUNK_LOAD_CAP = 10;

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private int pingTimer = 0;
    private boolean active = false;

    /** Клиентская анимация тарелки (порт {@code TileEntityMachineRadarNT}). */
    public float prevRotation;
    public float rotation;

    public boolean scanMissiles = true;
    public boolean scanShells = true;
    public boolean scanPlayers = false;
    public boolean smartMode = true;
    public boolean redMode = true;
    /** Показывать карту высот (порт TileEntityMachineRadarNT.showMap). */
    public boolean showMap = false;
    public boolean jammed = false;

    /** Карта высот 200x200 (порт TileEntityMachineRadarNT.map). */
    public byte[] map = new byte[MAP_LENGTH];
    /** Серверный флаг очистки карты (порт TileEntityMachineRadarNT.clearFlag). */
    public boolean clearFlag = false;
    /** Индекс текущего слайса карты для инкрементальной генерации/синхронизации. */
    private int mapSliceIndex = 0;
    /** Первый слайс очереди, ожидающей отправки клиенту. */
    private int mapSyncStartIndex = 0;
    /** Количество последовательных слайсов в очереди синхронизации. */
    private int mapSyncSliceCount = 0;
    /** Слайс изменился и должен попасть в ближайший update packet. */
    private boolean mapSliceReady = false;
    /** Сколько новых чанков было принудительно загружено за текущий проход карты. */
    private int generatedChunksThisMap = 0;
    /** Транзитный импульс очистки карты для клиентского синка (сбрасывается после отправки). */
    public boolean clearPulse = false;

    private int lastRedPower = 0;

    private final List<Entity> trackedEntities = new ArrayList<>();
    public final List<int[]> nearbyMissiles = new ArrayList<>();

    public MachineRadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_BE.get(), pos, state, SLOT_COUNT, 250_000L, 2_500L, 0L);
    }

    public boolean isLargeRadar() {
        return getBlockState().is(ModBlocks.LARGE_RADAR.get());
    }

    public int getRange() {
        return isLargeRadar() ? RADAR_LARGE_RANGE : RADAR_RANGE;
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (isLargeRadar()) {
            // Порт TileEntityMachineRadarLarge.getRenderBoundingBox()
            return new AABB(
                    worldPosition.getX() - 5,
                    worldPosition.getY(),
                    worldPosition.getZ() - 5,
                    worldPosition.getX() + 6,
                    worldPosition.getY() + 10,
                    worldPosition.getZ() + 6
            );
        }
        return super.getRenderBoundingBox();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRadarBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.clientTick();
            return;
        }
        blockEntity.serverTick();
    }

    private void clientTick() {
        prevRotation = rotation;
        if (active || getEnergyStored() > 0) {
            rotation += 5.0F;
            if (rotation >= 360.0F) {
                rotation -= 360.0F;
                prevRotation -= 360.0F;
            }
        }
    }

    private void serverTick() {
        ensureNetworkInitialized();
        chargeFromBatterySlot(SLOT_BATTERY_SECONDARY);
        chargeFromBatterySlot(SLOT_BATTERY_PRIMARY);
        chargeFromAdjacentBlocks();

        boolean wasActive = active;
        active = getEnergyStored() > 0;

        if (worldPosition.getY() < RADAR_ALTITUDE) {
            clearScanData();
            if (lastRedPower != 0) {
                lastRedPower = 0;
                notifyRedstoneNeighbors();
            }
        } else if (active) {
            if (level.getGameTime() % RADAR_SCAN_INTERVAL == 0) {
                performRadarScan();
            }
            setEnergyStored(Math.max(0L, getEnergyStored() - ENERGY_DRAIN_PER_TICK));
            active = getEnergyStored() > 0;
        } else {
            clearScanData();
        }

        progress = (progress + 1) % Math.max(1, maxProgress);

        pingTimer++;
        if (getEnergyStored() > 0 && pingTimer >= SONAR_PING_INTERVAL) {
            level.playSound(null, worldPosition, ModSounds.SONAR_PING.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            pingTimer = 0;
        }

        int redPower = getRedPower();
        if (redPower != lastRedPower) {
            lastRedPower = redPower;
            notifyRedstoneNeighbors();
        }

        // Генерация карты высот (порт TileEntityMachineRadarNT.showMap-блока).
        // В оригинале 100 точек считались КАЖДЫЙ тик. Ограничиваем только частоту
        // сетевого пакета, а не сам scan, иначе на клиенте появляются редкие полосы.
        if (showMap && active) {
            generateHeightmapSlice();
        }

        // Очистка карты (clearFlag поднимается кнопкой «clear»). Импульс уходит клиентам
        // в ближайшем getUpdateTag, затем сбрасывается.
        if (clearFlag) {
            if (map == null || map.length != MAP_LENGTH) {
                map = new byte[MAP_LENGTH];
            } else {
                Arrays.fill(map, (byte) 0);
            }
            mapSliceIndex = 0;
            mapSyncStartIndex = 0;
            mapSyncSliceCount = 0;
            mapSliceReady = false;
            generatedChunksThisMap = 0;
            clearFlag = false;
            clearPulse = true;
            setChanged();
            sendUpdateToClient();
        }

        // Линковка к Radar Screen (порт slots[8]==radar_linker → TileEntityMachineRadarScreen).
        pushDataToRadarScreen();

        // Скан идёт каждый тик, но несколько последовательных слайсов объединяются
        // в один пакет. Поэтому клиент не получает редкие полосы и при этом не тонет
        // в потоке BlockEntityDataPacket/NBT.
        boolean periodicSync = level.getGameTime() % MAP_UPDATE_INTERVAL == 0;
        if (mapSliceReady || wasActive != active || periodicSync || progress == 0) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /**
     * Инкрементальная генерация карты высот: 100 пикселей за тик
     * (порт {@code TileEntityMachineRadarNT.updateEntity} showMap-цикла).
     *
     * <p>Для незагруженных чанков используется базовая высота текущего генератора,
     * а явная загрузка чанков ограничена {@link #CHUNK_LOAD_CAP} за тик.
     */
    private void generateHeightmapSlice() {
        if (level == null || level.isClientSide || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        int range = getRange();
        int radarX = worldPosition.getX();
        int radarZ = worldPosition.getZ();
        int slice = mapSliceIndex;
        int baseIndex = slice * MAP_SLICE_SIZE;
        boolean generateChunks = com.hbm_m.config.ModClothConfig.get().machineRadar.generateChunks;
        int chunkLoads = 0;

        for (int i = 0; i < MAP_SLICE_SIZE; i++) {
            int index = baseIndex + i;
            int iX = (index % MAP_DIM) * range * 2 / MAP_DIM;
            int iZ = (index / MAP_DIM) * range * 2 / MAP_DIM;
            int x = radarX - range + iX;
            int z = radarZ - range + iZ;
            int cx = x >> 4;
            int cz = z >> 4;

            if (serverLevel.hasChunk(cx, cz)) {
                net.minecraft.world.level.chunk.LevelChunk chunk = serverLevel.getChunk(cx, cz);
                int h = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                map[index] = (byte) net.minecraft.util.Mth.clamp(h, 50, 128);
            } else if (generateChunks && chunkLoads < CHUNK_LOAD_CAP
                    && generatedChunksThisMap < MAX_GENERATED_CHUNKS_PER_MAP) {
                // Явно включённая генерация чанков сохраняет поведение оригинала,
                // но ограничена, чтобы радар не удерживал тысячи новых чанков.
                net.minecraft.world.level.chunk.LevelChunk chunk = serverLevel.getChunk(cx, cz);
                int h = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                map[index] = (byte) net.minecraft.util.Mth.clamp(h, 50, 128);
                chunkLoads++;
                generatedChunksThisMap++;
            }
        }

        if (mapSyncSliceCount == 0) {
            mapSyncStartIndex = slice;
        }
        mapSyncSliceCount = Math.min(MAP_SYNC_MAX_SLICES, mapSyncSliceCount + 1);
        mapSliceReady = true;
        mapSliceIndex = (slice + 1) % MAP_SLICES;
        if (mapSliceIndex == 0) {
            generatedChunksThisMap = 0;
        }
    }

    /**
     * Запоминает позицию экрана, связанного через radar linker в слоте SLOT_SCREEN_LINKER.
     * Нужен для корректного разрыва связи: если линкер убран, радар должен послать
     * экрану unlink(), иначе screen.linked останется true (баг: экран продолжает
     * «смотреть живым» после удаления linker'а).
     */
    private BlockPos lastScreenLinkerPos = null;

    /**
     * Передаёт список целей на связанный Radar Screen
     * (порт {@code TileEntityMachineRadarNT.updateEntity}: slots[8]==radar_linker → screen).
     * Если radar linker убран из слота — разрывает связь с предыдущим экраном.
     */
    private void pushDataToRadarScreen() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos screenPos = getScreenLinkerPos();
        if (screenPos == null) {
            // Линкер убран (или никогда не было) — разорвать связь с предыдущим экраном.
            if (lastScreenLinkerPos != null) {
                // hasChunkAt обязателен: level.getBlockEntity() в 1.20.1 синхронно
                // ЗАГРУЖАЕТ чанк, в отличие от worldObj.getTileEntity() в 1.7.10.
                if (level.hasChunkAt(lastScreenLinkerPos)
                        && level.getBlockEntity(lastScreenLinkerPos)
                                instanceof MachineRadarScreenBlockEntity screen) {
                    screen.unlink();
                }
                lastScreenLinkerPos = null;
            }
            return;
        }
        lastScreenLinkerPos = screenPos;
        // Экран в выгруженном чанке просто не получает данные (порт поведения
        // 1.7.10, где getTileEntity() для незагруженного чанка возвращал null).
        if (!level.hasChunkAt(screenPos)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(screenPos);
        if (be instanceof MachineRadarScreenBlockEntity screen) {
            screen.receiveFromRadar(nearbyMissiles,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getRange(),
                    showMap, map);
        }
    }

    /**
     * Точки подключения кабеля (порт {@code TileEntityMachineRadarNT.getConPos()} /
     * {@code TileEntityMachineRadarLarge.getConPos()}).
     */
    private BlockPos[] getConnectionPositions() {
        int offset = isLargeRadar() ? 2 : 1;
        return new BlockPos[] {
                worldPosition.offset(offset, 0, 0),
                worldPosition.offset(-offset, 0, 0),
                worldPosition.offset(0, 0, offset),
                worldPosition.offset(0, 0, -offset),
        };
    }

    private void chargeFromAdjacentBlocks() {
        if (level == null || !canReceive()) {
            return;
        }

        for (Direction dir : Direction.values()) {
            tryPullEnergyFromPos(worldPosition.relative(dir), dir.getOpposite());
            if (!canReceive()) {
                return;
            }
        }

        if (level.getGameTime() % 20 != 0) {
            return;
        }

        for (BlockPos connPos : getConnectionPositions()) {
            for (Direction dir : Direction.values()) {
                tryPullEnergyFromPos(connPos, dir);
                if (!canReceive()) {
                    return;
                }
            }
        }
    }

    private void tryPullEnergyFromPos(BlockPos sourcePos, Direction side) {
        if (level == null || !canReceive()) {
            return;
        }

        BlockEntity source = level.getBlockEntity(sourcePos);
        if (source == null) {
            return;
        }

        long needed = Math.min(getMaxEnergyStored() - getEnergyStored(), getReceiveSpeed());
        if (needed <= 0) {
            return;
        }
        final long pullLimit = needed;

        //? if forge {
        source.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER, side).ifPresent(provider -> {
            if (!provider.canExtract()) {
                return;
            }
            long toPull = Math.min(pullLimit, provider.getProvideSpeed());
            long extracted = provider.extractEnergy(toPull, false);
            if (extracted > 0) {
                receiveEnergy(extracted, false);
            }
        });

        if (!canReceive()) {
            return;
        }
        long feNeeded = Math.min(getMaxEnergyStored() - getEnergyStored(), getReceiveSpeed());
        if (feNeeded <= 0) {
            return;
        }
        final int fePullLimit = (int) Math.min(Integer.MAX_VALUE, feNeeded);

        source.getCapability(ForgeCapabilities.ENERGY, side).ifPresent(fe -> {
            if (!fe.canExtract()) {
                return;
            }
            if (fePullLimit <= 0) {
                return;
            }
            int extracted = fe.extractEnergy(fePullLimit, false);
            if (extracted > 0) {
                receiveEnergy(extracted, false);
            }
        });
        //?}
    }

    private void performRadarScan() {
        if (level == null || level.isClientSide) {
            return;
        }

        nearbyMissiles.clear();
        trackedEntities.clear();
        jammed = false;

        // Порт allocateTargets: оригинал не имеет верхнего лимита высоты
        // (глобальный реестр ракет). AABB-скан с maxY=getMaxBuildHeight() (320)
        // пропускал ракеты выше build height → поднимаем потолок скана.
        AABB area = new AABB(
                worldPosition.getX() + 0.5D - getRange(),
                level.getMinBuildHeight(),
                worldPosition.getZ() + 0.5D - getRange(),
                worldPosition.getX() + 0.5D + getRange(),
                level.getMaxBuildHeight() + 4096.0,
                worldPosition.getZ() + 0.5D + getRange()
        );

        List<Entity> entities = level.getEntities((Entity) null, area, Entity::isAlive);

        for (Entity entity : entities) {
            if (nearbyMissiles.size() >= MAX_CONTACTS) {
                break;
            }

            // Jamming-сущности проверяем на любой высоте (как и в 1.7.10 — фильтр высоты
            // применялся только к missile/shell-целям, не к игрокам и не к jammer'ам).
            if (isJammingEntity(entity)) {
                jammed = true;
                nearbyMissiles.clear();
                trackedEntities.clear();
                return;
            }

            // Игроки детектятся на ЛЮБОЙ высоте (порт 1.7.10: players не имели buffer).
            // Раньше RADAR_BUFFER=30 фильтровал игроков ниже radarY+30 → радар «не видел»
            // игроков на земле рядом с радаром.
            if (entity instanceof Player && scanPlayers) {
                nearbyMissiles.add(createContact(entity, getTargetTypeIndex(entity)));
                trackedEntities.add(entity);
                continue;
            }

            // Для missile/shell-целей — фильтр по высоте (RADAR_BUFFER),
            // чтобы не детектить ракеты на пусковой (ниже радара).
            if (entity.getY() < worldPosition.getY() + RADAR_BUFFER) {
                continue;
            }

            if (scanMissiles && isMissileLike(entity)) {
                if (entity instanceof com.hbm_m.entity.missile.MissileBaseEntity missile
                        && !missile.canBeDetectedByRadar()) {
                    continue;
                }
                int type = getTargetTypeIndex(entity);
                nearbyMissiles.add(createContact(entity, type));

                // ABM-перехватчик (как и suppliesRedstone()==false в 1.7.10) не даёт редстоун
                // и не считается «угрожающей» целью — в trackedEntities (для redstone) не попадает.
                if (type != IRadarDetectable.RadarTargetType.MISSILE_AB.ordinal()) {
                    if (smartMode) {
                        Vec3 motion = entity.getDeltaMovement();
                        if (motion.y <= 0.0D && isEntityApproaching(entity)) {
                            trackedEntities.add(entity);
                        }
                    } else {
                        trackedEntities.add(entity);
                    }
                }
            } else if (scanShells && isArtilleryShell(entity)) {
                // Порт IRadarDetectableNT.paramsApplicable(scanShells) — артиллерийские снаряды.
                nearbyMissiles.add(createContact(entity, IRadarDetectable.RadarTargetType.MISSILE_TIER0.ordinal()));
                trackedEntities.add(entity);
            }
        }

        // Глобальный трекер: ракеты в НЕзагруженных чанках
        // (порт IRadarDetectableNT — оригинал видел ракеты через глобальный реестр).
        scanGlobalTrackedMissiles();
    }

    /**
     * Сканирует глобальный реестр активных ракет (MissileTrackBroadcaster),
     * не привязанный к загрузке чанков. Добавляет ракеты в радиусе, которые
     * AABB-скан пропустил (находятся в выгруженных чанках).
     */
    private void scanGlobalTrackedMissiles() {
        if (level == null || level.isClientSide || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        if (!scanMissiles) {
            return;
        }
        // Уже детектированные ID (индекс 6 в массиве контакта) — чтобы не дублировать.
        java.util.Set<Integer> detected = new java.util.HashSet<>();
        for (int[] c : nearbyMissiles) {
            if (c != null && c.length >= 7) {
                detected.add(c[6]);
            }
        }
        double cx = worldPosition.getX() + 0.5D;
        double cz = worldPosition.getZ() + 0.5D;
        int scan = getRange();
        for (com.hbm_m.entity.missile.MissileBaseEntity missile
                : com.hbm_m.server.missile.MissileTrackBroadcaster.getActiveMissiles(serverLevel)) {
            if (nearbyMissiles.size() >= MAX_CONTACTS) {
                break;
            }
            if (missile.isRemoved()) {
                continue;
            }
            int eid = missile.getId();
            if (detected.contains(eid)) {
                continue;
            }
            if (Math.abs(missile.getX() - cx) > scan || Math.abs(missile.getZ() - cz) > scan) {
                continue;
            }
            if (missile.getY() < worldPosition.getY() + RADAR_BUFFER) {
                continue;
            }
            if (!missile.canBeDetectedByRadar()) {
                continue;
            }
            int type = getTargetTypeIndex(missile);
            nearbyMissiles.add(createContact(missile, type));
            detected.add(eid);
            if (type != IRadarDetectable.RadarTargetType.MISSILE_AB.ordinal()) {
                if (smartMode) {
                    Vec3 motion = missile.getDeltaMovement();
                    if (motion.y <= 0.0D && isEntityApproaching(missile)) {
                        trackedEntities.add(missile);
                    }
                } else {
                    trackedEntities.add(missile);
                }
            }
        }
    }

    private boolean isMissileLike(Entity entity) {
        if (entity instanceof MissileBaseEntity) {
            return true;
        }
        if (entity instanceof IRadarDetectable detectable) {
            return detectable.getTargetType() != IRadarDetectable.RadarTargetType.PLAYER;
        }
        String name = entity.getType().toString().toLowerCase();
        return name.contains("missile")
                || name.contains("rocket")
                || name.contains("airstrike")
                || name.contains("bomb")
                || name.contains("nuke");
    }

    private boolean isJammingEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        String typeName = living.getType().toString().toLowerCase();
        String displayName = living.getName().getString().toLowerCase();
        return typeName.contains("digamma")
                || typeName.contains("jam")
                || displayName.contains("digamma")
                || displayName.contains("jam");
    }

    /**
     * Порт IRadarDetectableNT.ARTY: артиллерийские/снарядные снаряды.
     * В 1.7.10 это AbstractEntityArtillery/Shell; здесь ловим по имени типа,
     * пока нет выделенного интерфейса.
     */
    private boolean isArtilleryShell(Entity entity) {
        if (entity instanceof MissileBaseEntity) {
            return false;
        }
        String name = entity.getType().toString().toLowerCase();
        return name.contains("shell")
                || name.contains("artillery")
                || name.contains("mortar")
                || name.contains("howitzer");
    }

    private int[] createContact(Entity entity, int type) {
        return createContact(entity, type, false);
    }

    private int[] createContact(Entity entity, int type, boolean mobContact) {
        // Порт RadarEntry: добавлен entityID (индекс 6) — без него невозможен клик-перехват
        // конкретной ракеты через sendCommandEntity (1.7.10 RadarEntry.entityID).
        return new int[] {
                (int) entity.getX(),
                (int) entity.getY(),
                (int) entity.getZ(),
                getVelocity(entity),
                type,
                mobContact ? 1 : 0,
                entity.getId()
        };
    }

    private int getVelocity(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        return (int) (Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z) * 20.0D);
    }

    private boolean isEntityApproaching(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        boolean xAxisApproaching = (worldPosition.getX() < entity.getX() && motion.x < 0.0D)
                || (worldPosition.getX() > entity.getX() && motion.x > 0.0D);
        boolean zAxisApproaching = (worldPosition.getZ() < entity.getZ() && motion.z < 0.0D)
                || (worldPosition.getZ() > entity.getZ() && motion.z > 0.0D);
        return xAxisApproaching && zAxisApproaching;
    }

    private int getTargetTypeIndex(Entity entity) {
        if (entity instanceof IRadarDetectable detectable) {
            return detectable.getTargetType().ordinal();
        }
        if (entity instanceof Player) {
            return IRadarDetectable.RadarTargetType.PLAYER.ordinal();
        }
        return 0;
    }

    public String getTargetTypeName(int index) {
        IRadarDetectable.RadarTargetType[] values = IRadarDetectable.RadarTargetType.values();
        if (index < 0 || index >= values.length) {
            return IRadarDetectable.RadarTargetType.MISSILE_TIER0.name;
        }
        return values[index].name;
    }

    public long getPowerScaled(int scale) {
        if (getMaxEnergyStored() <= 0) {
            return 0;
        }
        return getEnergyStored() * scale / getMaxEnergyStored();
    }

    // Действия для GUI радара (порт GUIMachineRadarNT.receiveControl).
    // Используются UpdateRadarC2SPacket: buttonId < ACTION_OFFSET — toggles,
    // buttonId == ACTION_LAUNCH_* — команды пуска через radar linker.
    public static final int ACTION_TOGGLE_MISSILES = 0;
    public static final int ACTION_TOGGLE_SHELLS = 1;
    public static final int ACTION_TOGGLE_PLAYERS = 2;
    public static final int ACTION_TOGGLE_SMART = 3;
    public static final int ACTION_TOGGLE_RED = 4;
    /** Показать/скрыть карту высот (порт receiveControl["map"]). */
    public static final int ACTION_TOGGLE_MAP = 6;
    /** Открыть GUI слотов (порт receiveControl["gui1"] ↔ GUIMachineRadarNTSlots). */
    public static final int ACTION_OPEN_SLOTS = 5;
    /** Очистить карту высот (порт receiveControl["clear"]). */
    public static final int ACTION_CLEAR_MAP = 7;
    /** Вернуться из GUI слотов в главный экран радара. */
    public static final int ACTION_OPEN_MAIN = 8;
    public static final int ACTION_OFFSET = 10;
    public static final int ACTION_LAUNCH_AT_ENTITY = 10;
    public static final int ACTION_LAUNCH_AT_COORDS = 11;

    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            case ACTION_TOGGLE_MISSILES -> scanMissiles = !scanMissiles;
            case ACTION_TOGGLE_SHELLS -> scanShells = !scanShells;
            case ACTION_TOGGLE_PLAYERS -> scanPlayers = !scanPlayers;
            case ACTION_TOGGLE_SMART -> smartMode = !smartMode;
            case ACTION_TOGGLE_RED -> redMode = !redMode;
            case ACTION_TOGGLE_MAP -> showMap = !showMap;
            case ACTION_CLEAR_MAP -> clearFlag = true;
            default -> {
                return;
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    /**
     * Команда пуска на связанную radar linker'ом пусковую установку
     * (порт TileEntityMachineRadarNT.receiveControl: слот[8]=radar_linker → IRadarCommandReceiver).
     *
     * @param action   ACTION_LAUNCH_AT_ENTITY или ACTION_LAUNCH_AT_COORDS
     * @param targetId entityID цели (для LAUNCH_AT_ENTITY), иначе -1
     * @param x        целевые X (для LAUNCH_AT_COORDS)
     * @param z        целевые Z (для LAUNCH_AT_COORDS)
     * @return true если команда принята установкой
     */
    /**
     * Команда пуска через radar linker в одном из линк-слотов 0..7
     * (порт {@code TileEntityMachineRadarNT.receiveControl}: {@code link=id} → slots[id]).
     *
     * @param linkSlot индекс слота 0..7 (клавиши 1-8 → слоты 0-7)
     * @param action   ACTION_LAUNCH_AT_ENTITY или ACTION_LAUNCH_AT_COORDS
     * @param targetId entityID цели (для LAUNCH_AT_ENTITY), иначе -1
     * @param x        целевые X (для LAUNCH_AT_COORDS)
     * @param z        целевые Z (для LAUNCH_AT_COORDS)
     * @return true если команда принята установкой
     */
    public boolean handleLaunchCommand(int linkSlot, int action, int targetId, int x, int z) {
        if (level == null || level.isClientSide) {
            return false;
        }
        if (linkSlot < SLOT_LINK_FIRST || linkSlot > SLOT_LINK_LAST) {
            return false;
        }

        BlockPos receiverPos = getLinkerPos(linkSlot);
        if (receiverPos == null) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(receiverPos);
        if (!(be instanceof IRadarCommandReceiver receiver)) {
            return false;
        }

        boolean ok;
        if (action == ACTION_LAUNCH_AT_ENTITY && targetId >= 0) {
            Entity target = level.getEntity(targetId);
            if (target == null) {
                return false;
            }
            ok = receiver.sendCommandEntity(target);
        } else if (action == ACTION_LAUNCH_AT_COORDS) {
            ok = receiver.sendCommandPosition(new BlockPos(x, worldPosition.getY(), z));
        } else {
            return false;
        }

        if (ok) {
            level.playSound(null, worldPosition, com.hbm_m.sound.ModSounds.TOOL_TECH_BLEEP.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ok;
    }

    /**
     * Читает BlockPos из NBT radar linker'а в линк-слоте 0..7
     * (порт slots[id] == radar_linker + ItemCoordinateBase.getPosition).
     */
    @org.jetbrains.annotations.Nullable
    public BlockPos getLinkerPos(int linkSlot) {
        if (linkSlot < SLOT_LINK_FIRST || linkSlot > SLOT_LINK_LAST) {
            return null;
        }
        return readLinkerPos(inventory.getStackInSlot(linkSlot));
    }

    /**
     * Читает BlockPos Radar Screen из NBT radar linker'а в слоте SLOT_SCREEN_LINKER
     * (порт slots[8] == radar_linker → TileEntityMachineRadarScreen).
     */
    @org.jetbrains.annotations.Nullable
    public BlockPos getScreenLinkerPos() {
        return readLinkerPos(inventory.getStackInSlot(SLOT_SCREEN_LINKER));
    }

    @org.jetbrains.annotations.Nullable
    private BlockPos readLinkerPos(ItemStack linker) {
        if (linker.isEmpty()) {
            return null;
        }
        net.minecraft.nbt.CompoundTag tag = linker.getTag();
        if (tag == null || !tag.contains("xCoord")) {
            return null;
        }
        return new BlockPos(
                tag.getInt("xCoord"),
                tag.getInt("yCoord"),
                tag.getInt("zCoord"));
    }

    public int getRedPower() {
        if (trackedEntities.isEmpty()) {
            return 0;
        }

        if (redMode) {
            double maxRange = getRange() * Math.sqrt(2.0D);
            int powerOut = 0;

            for (Entity entity : trackedEntities) {
                double dx = entity.getX() - worldPosition.getX();
                double dz = entity.getZ() - worldPosition.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                int p = 15 - (int) Math.floor(dist / maxRange * 15.0D);
                if (p > powerOut) {
                    powerOut = p;
                }
            }

            return Math.max(0, Math.min(15, powerOut));
        }

        int powerOut = 0;
        for (int[] contact : nearbyMissiles) {
            if (contact == null || contact.length < 5) {
                continue;
            }
            powerOut = Math.max(powerOut, contact[4] + 1);
        }
        return Math.max(0, Math.min(15, powerOut));
    }

    private void clearScanData() {
        jammed = false;
        nearbyMissiles.clear();
        trackedEntities.clear();
    }

    private void notifyRedstoneNeighbors() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        level.updateNeighborsAt(worldPosition, state.getBlock());
        level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());

        if (!isLargeRadar() || !(state.getBlock() instanceof IMultiblockController controller)) {
            return;
        }

        MultiblockStructureHelper helper = controller.getStructureHelper();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        for (BlockPos localPos : helper.getStructureMap().keySet()) {
            if (helper.resolvePartRole(localPos, controller) != PartRole.ENERGY_CONNECTOR) {
                continue;
            }
            BlockPos worldPos = helper.getRotatedPos(worldPosition, localPos, facing);
            level.updateNeighborsAt(worldPos, ModBlocks.UNIVERSAL_MACHINE_PART.get());
            level.updateNeighbourForOutputSignal(worldPos, ModBlocks.UNIVERSAL_MACHINE_PART.get());
        }
    }

    public int getProgressScaled(int scale) {
        if (maxProgress <= 0) {
            return 0;
        }
        return progress * scale / maxProgress;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        // Сначала собираем полностью независимый снимок. ChunkSerializer передаёт
        // результат в IOWorker, который пишет NBT асинхронно; нельзя оставлять в нём
        // ссылки на теги инвентаря/способностей, принадлежащие живым ItemStack.
        CompoundTag snapshot = new CompoundTag();
        super.saveAdditional(snapshot);
        snapshot.putInt("progress", progress);
        snapshot.putInt("max_progress", maxProgress);
        snapshot.putBoolean("active", active);
        snapshot.putBoolean("scan_missiles", scanMissiles);
        snapshot.putBoolean("scan_shells", scanShells);
        snapshot.putBoolean("scan_players", scanPlayers);
        // Позиция экрана, связанного через radar linker (для разрыва связи при удалении линкера).
        if (lastScreenLinkerPos != null) {
            snapshot.putInt("lastScreenX", lastScreenLinkerPos.getX());
            snapshot.putInt("lastScreenY", lastScreenLinkerPos.getY());
            snapshot.putInt("lastScreenZ", lastScreenLinkerPos.getZ());
        }
        snapshot.putBoolean("smart_mode", smartMode);
        snapshot.putBoolean("red_mode", redMode);
        snapshot.putBoolean("show_map", showMap);
        snapshot.putBoolean("jammed", jammed);
        // Карта высот сохраняется на диск целиком (порт nbt.setByteArray("map", map)).
        // В сетевой пакет (getUpdateTag) попадает только инкрементальный слайс.
        if (map == null || map.length != MAP_LENGTH) {
            map = new byte[MAP_LENGTH];
        }
        // IOWorker сериализует chunk NBT на отдельном потоке. Нельзя передавать ему
        // живой массив карты: следующий тик радара продолжит менять его во время
        // записи чанка. Также отсоединяем inventory NBT от ItemStack capability tags,
        // которые могут измениться при зарядке батареи.
        snapshot.putByteArray("map", Arrays.copyOf(map, MAP_LENGTH));
        snapshot.put("inventory", snapshot.getCompound("inventory").copy());
        // rotation/prevRotation НЕ сохраняем: это чисто клиентское состояние анимации.
        // На сервере rotation всегда 0, и попадание его в getUpdateTag()/handleUpdateTag()
        // каждую синхронизацию сбрасывало клиентский rotation → тарелка «дёргалась в припадке».

        ListTag contacts = new ListTag();
        for (int[] entry : nearbyMissiles) {
            if (entry == null || entry.length < 5) {
                continue;
            }
            CompoundTag contactTag = new CompoundTag();
            contactTag.putInt("x", entry[0]);
            contactTag.putInt("y", entry[1]);
            contactTag.putInt("z", entry[2]);
            contactTag.putInt("v", entry[3]);
            contactTag.putInt("t", entry[4]);
            contactTag.putInt("m", entry.length >= 6 ? entry[5] : 0);
            contactTag.putInt("eid", entry.length >= 7 ? entry[6] : -1);
            contacts.add(contactTag);
        }
        snapshot.put("contacts", contacts);

        // merge(copy) отсоединяет все вложенные Tag-объекты от временного снимка
        // и от любых объектов, которыми владеет BlockEntity.
        tag.merge(snapshot.copy());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("max_progress");
        if (maxProgress <= 0) {
            maxProgress = DEFAULT_MAX_PROGRESS;
        }
        active = tag.getBoolean("active");
        scanMissiles = !tag.contains("scan_missiles") || tag.getBoolean("scan_missiles");
        scanShells = !tag.contains("scan_shells") || tag.getBoolean("scan_shells");
        scanPlayers = tag.getBoolean("scan_players");
        if (tag.contains("lastScreenX")) {
            lastScreenLinkerPos = new BlockPos(
                    tag.getInt("lastScreenX"),
                    tag.getInt("lastScreenY"),
                    tag.getInt("lastScreenZ"));
        }
        smartMode = tag.getBoolean("smart_mode");
        redMode = tag.getBoolean("red_mode");
        showMap = tag.getBoolean("show_map");
        jammed = tag.getBoolean("jammed");

        // Карта: с диска — целиком, из сети — инкрементальный слайс (см. getUpdateTag).
        if (tag.contains("map")) {
            byte[] saved = tag.getByteArray("map");
            if (saved != null && saved.length == MAP_LENGTH) {
                // clone() обязателен: getByteArray() возвращает массив, которым владеет
                // ByteArrayTag загруженного chunk NBT. Без копии тик радара продолжал бы
                // менять массив, который IOWorker пишет на диск в другом потоке.
                map = saved.clone();
            }
        }
        if (tag.contains("mapSliceIdx")) {
            int idx = tag.getInt("mapSliceIdx");
            byte[] slice = tag.getByteArray("mapSlice");
            if (slice != null && idx >= 0 && idx * MAP_SLICE_SIZE + slice.length <= MAP_LENGTH) {
                if (map == null || map.length != MAP_LENGTH) {
                    map = new byte[MAP_LENGTH];
                }
                System.arraycopy(slice, 0, map, idx * MAP_SLICE_SIZE, slice.length);
            }
        }
        if (tag.getBoolean("mapClear")) {
            if (map == null || map.length != MAP_LENGTH) {
                map = new byte[MAP_LENGTH];
            } else {
                Arrays.fill(map, (byte) 0);
            }
        }

        nearbyMissiles.clear();
        ListTag contacts = tag.getList("contacts", Tag.TAG_COMPOUND);
        for (int i = 0; i < contacts.size(); i++) {
            CompoundTag contactTag = contacts.getCompound(i);
            nearbyMissiles.add(new int[] {
                    contactTag.getInt("x"),
                    contactTag.getInt("y"),
                    contactTag.getInt("z"),
                    contactTag.getInt("v"),
                    contactTag.getInt("t"),
                    contactTag.getInt("m"),
                    contactTag.getInt("eid")
            });
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.radar");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    /**
     * Лёгкий сетевой синк: без инвентаря и полной карты (40 КБ).
     * Карта — несколько последовательных инкрементальных слайсов по 100 байт.
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("energy", getEnergyStored());
        tag.putBoolean("active", active);
        tag.putBoolean("scan_missiles", scanMissiles);
        tag.putBoolean("scan_shells", scanShells);
        tag.putBoolean("scan_players", scanPlayers);
        tag.putBoolean("smart_mode", smartMode);
        tag.putBoolean("red_mode", redMode);
        tag.putBoolean("show_map", showMap);
        tag.putBoolean("jammed", jammed);

        ListTag contacts = new ListTag();
        for (int[] entry : nearbyMissiles) {
            if (entry == null || entry.length < 5) {
                continue;
            }
            CompoundTag contactTag = new CompoundTag();
            contactTag.putInt("x", entry[0]);
            contactTag.putInt("y", entry[1]);
            contactTag.putInt("z", entry[2]);
            contactTag.putInt("v", entry[3]);
            contactTag.putInt("t", entry[4]);
            contactTag.putInt("m", entry.length >= 6 ? entry[5] : 0);
            contactTag.putInt("eid", entry.length >= 7 ? entry[6] : -1);
            contacts.add(contactTag);
        }
        tag.put("contacts", contacts);

        if (showMap && level != null && !level.isClientSide && mapSliceReady && map != null) {
            int count = Math.min(mapSyncSliceCount, MAP_SYNC_MAX_SLICES);
            byte[] slices = new byte[count * MAP_SLICE_SIZE];
            for (int i = 0; i < count; i++) {
                int slice = (mapSyncStartIndex + i) % MAP_SLICES;
                System.arraycopy(map, slice * MAP_SLICE_SIZE, slices,
                        i * MAP_SLICE_SIZE, MAP_SLICE_SIZE);
            }
            tag.putInt("mapSliceStart", mapSyncStartIndex);
            tag.putInt("mapSliceCount", count);
            tag.putByteArray("mapSlices", slices);
            mapSyncSliceCount -= count;
            if (mapSyncSliceCount > 0) {
                mapSyncStartIndex = (mapSyncStartIndex + count) % MAP_SLICES;
            }
            if (mapSyncSliceCount <= 0) {
                mapSyncSliceCount = 0;
                mapSliceReady = false;
            }
        }
        if (clearPulse) {
            tag.putBoolean("mapClear", true);
            clearPulse = false;
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains("energy")) {
            setEnergyStored(tag.getLong("energy"));
        }
        if (tag.contains("active")) active = tag.getBoolean("active");
        if (tag.contains("scan_missiles")) scanMissiles = tag.getBoolean("scan_missiles");
        if (tag.contains("scan_shells")) scanShells = tag.getBoolean("scan_shells");
        if (tag.contains("scan_players")) scanPlayers = tag.getBoolean("scan_players");
        if (tag.contains("smart_mode")) smartMode = tag.getBoolean("smart_mode");
        if (tag.contains("red_mode")) redMode = tag.getBoolean("red_mode");
        if (tag.contains("show_map")) showMap = tag.getBoolean("show_map");
        if (tag.contains("jammed")) jammed = tag.getBoolean("jammed");

        if (tag.contains("contacts")) {
            nearbyMissiles.clear();
            ListTag contacts = tag.getList("contacts", Tag.TAG_COMPOUND);
            for (int i = 0; i < contacts.size(); i++) {
                CompoundTag ct = contacts.getCompound(i);
                nearbyMissiles.add(new int[] {
                        ct.getInt("x"), ct.getInt("y"), ct.getInt("z"),
                        ct.getInt("v"), ct.getInt("t"), ct.getInt("m"), ct.getInt("eid")
                });
            }
        }

        if (tag.contains("mapSlices")) {
            int start = tag.getInt("mapSliceStart");
            byte[] slices = tag.getByteArray("mapSlices");
            int count = Math.min(tag.getInt("mapSliceCount"), slices.length / MAP_SLICE_SIZE);
            if (start >= 0 && start < MAP_SLICES && count > 0) {
                if (map == null || map.length != MAP_LENGTH) {
                    map = new byte[MAP_LENGTH];
                }
                for (int i = 0; i < count; i++) {
                    int slice = (start + i) % MAP_SLICES;
                    System.arraycopy(slices, i * MAP_SLICE_SIZE, map,
                            slice * MAP_SLICE_SIZE, MAP_SLICE_SIZE);
                }
            }
        } else if (tag.contains("mapSliceIdx")) {
            int idx = tag.getInt("mapSliceIdx");
            byte[] slice = tag.getByteArray("mapSlice");
            if (slice != null && idx >= 0) {
                int from = idx * MAP_SLICE_SIZE;
                if (from + slice.length <= MAP_LENGTH) {
                    if (map == null || map.length != MAP_LENGTH) {
                        map = new byte[MAP_LENGTH];
                    }
                    System.arraycopy(slice, 0, map, from, slice.length);
                }
            }
        }
        if (tag.getBoolean("mapClear")) {
            if (map == null || map.length != MAP_LENGTH) {
                map = new byte[MAP_LENGTH];
            } else {
                Arrays.fill(map, (byte) 0);
            }
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) {
            handleUpdateTag(pkt.getTag());
        }
    }

    /** Публичная обёртка над {@link #isItemValidForSlot} для слотов меню. */
    public boolean canPlaceItemInSlot(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // Слот 8 — radar linker для связи с Radar Screen (порт slots[8] == radar_linker).
        if (slot == SLOT_SCREEN_LINKER) {
            return stack.getItem() instanceof com.hbm_m.item.tool.ItemRadarLinker;
        }
        // Слот 9 — только батарея.
        if (slot == SLOT_BATTERY_SECONDARY) {
            return isBattery(stack);
        }
        // Слоты 0..7 — линк-слоты: radar_linker, designator/sat_relay ИЛИ батарея
        // (слот 0 двойной: батарея + линк, как chargeTEFromItems(slots, 0, ...) в 1.7.10).
        if (slot >= SLOT_LINK_FIRST && slot <= SLOT_LINK_LAST) {
            return stack.getItem() instanceof com.hbm_m.item.tool.ItemRadarLinker
                    || stack.getItem() instanceof com.hbm_m.api.item.IDesignatorItem
                    || isBattery(stack);
        }
        return false;
    }

    private boolean isBattery(ItemStack stack) {
        if (stack.getItem() instanceof ItemCreativeBattery) {
            return true;
        }
        if (ItemEnergyAccess.getHbmProvider(stack).isPresent()) {
            return true;
        }
        //? if forge {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        //?}
        //? if fabric {
        /*return teamreborn.energy.api.EnergyStorage.ITEM.find(stack, null) != null;
        *///?}
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineRadarMenu.create(id, inventory, this);
    }
}
