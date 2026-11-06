package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidNet;
import com.hbm_m.api.fluids.FluidNetProvider;
import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.fluids.ForgeFluidHandlerAdapter;
import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidPipeMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if forge {
 //?}

 /**
 * BlockEntity трубы. Хранит тип жидкости и управляет MK2 узлом в UniNodespace.
 * <p>
 * Визуал оверлея (neo / colored / silver): в OBJ MTL слой «skin» помечен {@code forge_TintIndex 1},
 * JSON {@code mtl_override} подставляет MTL для bake (base / overlay), а блоки труб зарегистрированы
 * как {@code cutout} в {@link com.hbm_m.client.ClientSetup}, чтобы оверлей с альфой отображался
 * поверх основы.
 *
 * Логика передачи жидкостей:
 *  - Узел (FluidNode) создаётся при загрузке блока/смене типа и разрушается при выгрузке.
 *  - UniNodespace обновляется раз в тик (через MainRegistry.onServerTick) и строит сети.
 *  - Каждый тик duct сканирует соседей: для Forge-машин создаёт ForgeFluidHandlerAdapter
 *    и регистрирует их как providers/receivers в сети трубы.
 *  - Фактический перенос жидкостей выполняется FluidNet.update() (в UniNodespace.updateNodespace()).
 */

@SuppressWarnings("UnstableApiUsage")
public class FluidDuctBlockEntity extends BaseHbmBlockEntity implements IFluidPipeMK2, com.hbm_m.interfaces.ICopiable {

    private static final String NBT_FLUID_TYPE = "FluidType";

    private Fluid fluidType = Fluids.EMPTY;

    /** Текущий узел в UniNodespace. null до первого onLoad или если тип не задан. */
    @Nullable
    private FluidNode node;

    /**
     * Клиентский отложенный refresh визуальных соединений после загрузки чанка.
     * На первом onLoad соседний UniversalMachinePartBlockEntity/контроллер может ещё не иметь
     * актуального NBT на клиенте, поэтому пересчитываем трубу несколько тиков подряд.
     */
    private int clientVisualRefreshTicks = 0;

    /**
     * Кэш адаптеров для соседних Forge-машин.
     * Ключ — направление от duct к машине; валидность — по (позиция, blockstate, identity BE).
     */
    private final Map<Direction, ForgeFluidHandlerAdapter> adapterCache = new EnumMap<>(Direction.class);
    private final Map<Direction, AdapterKey> adapterKeys = new EnumMap<>(Direction.class);

    /** Ключ валидности кэшированного адаптера: сосед, его состояние и позиция. */
    private static final class AdapterKey {
        final BlockPos pos;
        final BlockState state;
        final BlockEntity be;
        AdapterKey(BlockPos pos, BlockState state, BlockEntity be) { this.pos = pos; this.state = state; this.be = be; }
        boolean matches(BlockPos pos, BlockState state, BlockEntity be) {
            return this.pos.equals(pos) && this.state == state && this.be == be;
        }
    }

    private void invalidateAdapterCache() {
        adapterCache.clear();
        adapterKeys.clear();
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        // Смена blockstate (подключения/поворот трубы) — кэш соседей мог устареть.
        invalidateAdapterCache();
    }

    public FluidDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DUCT_BE.get(), pos, state);
    }

    // =====================================================================================
    // IFluidPipeMK2
    // =====================================================================================

    @Override
    public Fluid getFluidType() {
        return fluidType;
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && VanillaFluidEquivalence.sameSubstance(fluid, this.fluidType);
    }

    // =====================================================================================
    // Fluid type management
    // =====================================================================================

    public void setFluidType(Fluid fluid) {
        setFluidTypeSilent(fluid);
        syncFluidToClients();
    }

    /** Устанавливает тип без немедленной синхронизации (для batch-покраски). */
    public void setFluidTypeSilent(Fluid fluid) {
        Fluid prev = this.fluidType;
        this.fluidType = fluid != null ? fluid : Fluids.EMPTY;
        invalidateAdapterCache();
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            rebuildNode(serverLevel, prev);
        }
    }

    public void syncFluidToClients() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // =====================================================================================
    // Node lifecycle
    // =====================================================================================

    /** Создать или пересоздать узел в UniNodespace. Уничтожает старый узел, если тип сменился. */
    private void rebuildNode(ServerLevel serverLevel, Fluid previousFluid) {
        // Уничтожить старый узел
        if (node != null && !node.isExpired()) {
            UniNodespace.destroyNode(serverLevel, node);
        }
        node = null;

        // Если старый тип был другим — явно убрать его узел из UniNodespace
        if (previousFluid != null && previousFluid != Fluids.EMPTY && previousFluid != fluidType) {
            UniNodespace.destroyNode(serverLevel, worldPosition, FluidNetProvider.forFluid(previousFluid));
        }

        // Создать новый узел для текущего типа
        if (fluidType != Fluids.EMPTY) {
            node = createNode(fluidType, worldPosition);
            UniNodespace.createNode(serverLevel, node);
        }
    }

    /** Убедиться, что узел существует (lazy создание, например после чтения NBT). */
    private void ensureNode(ServerLevel serverLevel) {
        if (fluidType == Fluids.EMPTY) return;
        if (node == null || node.isExpired()) {
            // Попытаться получить существующий узел на этой позиции
            var existing = UniNodespace.getNode(serverLevel, worldPosition, FluidNetProvider.forFluid(fluidType));
            if (existing instanceof FluidNode fn && !fn.isExpired()) {
                node = fn;
            } else {
                node = createNode(fluidType, worldPosition);
                UniNodespace.createNode(serverLevel, node);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            ensureNode(serverLevel);

            if (getBlockState().getBlock() instanceof FluidDuctBlock duct) {
                BlockState current = getBlockState();
                BlockState updated = duct.getConnectionState(serverLevel, worldPosition);
                if (updated != current) {
                    serverLevel.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                } else {
                    serverLevel.sendBlockUpdated(worldPosition, current, current, Block.UPDATE_CLIENTS);
                }
                // Ещё один пересчёт через пару тиков: соседние BE мультиблоков могут загрузиться позже трубы.
                serverLevel.scheduleTick(worldPosition, current.getBlock(), 2);
            }
        }

        if (level != null && level.isClientSide && getBlockState().getBlock() instanceof FluidDuctBlock) {
            clientVisualRefreshTicks = 40;
            FluidDuctBlock.refreshAdjacentDucts(level, worldPosition);
        }
    }


    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel && node != null && !node.isExpired()) {
            UniNodespace.destroyNode(serverLevel, node);
        }
        node = null;
        invalidateAdapterCache();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel serverLevel && node != null && !node.isExpired()) {
            UniNodespace.destroyNode(serverLevel, node);
        }
        node = null;
        invalidateAdapterCache();
        super.onChunkUnloaded();
    }

    // =====================================================================================
    // Tick — регистрация Forge-машин в сети
    // =====================================================================================
    
    
    private void clientTick(Level level, BlockPos pos) {
        if (clientVisualRefreshTicks <= 0) {
            return;
        }

        clientVisualRefreshTicks--;
        // Не каждый кадр, чтобы не спамить пересборкой чанка, но достаточно долго после входа в мир.
        if ((level.getGameTime() & 1L) == 0L) {
            FluidDuctBlock.refreshAdjacentDucts(level, pos);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidDuctBlockEntity entity) {
        if (level.isClientSide) {
            entity.clientTick(level, pos);
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (entity.fluidType == Fluids.EMPTY) return;

        // Восстановить узел, если потерялся
        entity.ensureNode(serverLevel);
        if (entity.node == null || entity.node.isExpired()) return;

        // Для каждого подключённого соседа, не являющегося трубой, создать адаптер и подписать его.
        // Троттлинг подписок до 2 Гц (%10): FluidNet отбрасывает записи по таймауту 3 с —
        // 2 Гц с запасом покрывает 0.33 Гц; сразу после инвалидации кэша подписка идёт без гейта.
        boolean throttled = serverLevel.getGameTime() % 10 != 0;
        for (Direction dir : Direction.values()) {
            if (!state.getValue(FluidDuctBlock.PROPERTY_BY_DIRECTION.get(dir))) continue;

            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            // Пропускаем другие трубы (они сами обслуживают свои узлы)
            if (neighbor == null || neighbor instanceof FluidDuctBlockEntity) continue;
            // Пропускаем машины, реализующие MK2 напрямую (они вызывают trySubscribe/tryProvide сами)
            if (neighbor instanceof IFluidConnectorMK2) continue;

            Direction sideOfNeighborFacingDuct = dir.getOpposite();
            boolean subscribeNow = !throttled;
            AdapterKey key = entity.adapterKeys.get(dir);
            boolean cacheValid = key != null && key.matches(neighborPos, state, neighbor);
            if (!cacheValid) {
                entity.adapterCache.remove(dir);
                entity.adapterKeys.remove(dir);
                // Проверяем, что у соседа есть IFluidHandler на нашей стороне (дорого — только при инвалидации)
                if (!checkNeighborFluidHandler(level, neighbor, sideOfNeighborFacingDuct)) {
                    continue;
                }
                ForgeFluidHandlerAdapter fresh = new ForgeFluidHandlerAdapter(level, neighborPos, sideOfNeighborFacingDuct, entity.fluidType);
                entity.adapterCache.put(dir, fresh);
                entity.adapterKeys.put(dir, new AdapterKey(neighborPos, state, neighbor));
                subscribeNow = true; // сразу после (пере)создания — подписываем немедленно
            }

            ForgeFluidHandlerAdapter adapter = entity.adapterCache.get(dir);
            if (adapter == null) continue;

            // Прямой перенос к соседу — каждый тик: троттлинг делит пропускную способность,
            // а не только стоимость скана. На троттлинге остаётся только подписка в сеть.
            adapter.tryProvide(entity.fluidType, serverLevel, pos, dir.getOpposite());
            if (subscribeNow) {
                adapter.trySubscribe(entity.fluidType, serverLevel, pos, dir.getOpposite());
            }
        }
    }


    /**
     * Проверяет наличие fluid handler у соседнего BlockEntity.
     * На Forge — через ForgeCapabilities.FLUID_HANDLER,
     * на Fabric — через Fabric Transfer API FluidStorage.SIDED.
     */
    private static boolean checkNeighborFluidHandler(Level level, BlockEntity neighbor, Direction side) {
        return com.hbm_m.api.fluids.FluidCapabilityAccess.hasFluidHandler(level, neighbor.getBlockPos(), side);
    }

    // =====================================================================================
    // Debug
    // =====================================================================================

    /** Возвращает текущий fluidTracker для оверлея. */
    public long getFluidTracker() {
        if (node == null || node.net == null) return 0L;
        return ((FluidNet) node.net).fluidTracker;
    }

    /** Размер активной сети (количество узлов). */
    public int getNetworkSize() {
        if (node == null || node.net == null) return 0;
        return node.net.links.size();
    }

    // =====================================================================================
    // NBT Save/Load
    // =====================================================================================

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluidType);
        if (loc != null) {
            tag.putString(NBT_FLUID_TYPE, loc.toString());
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        Fluid before = this.fluidType;
        if (tag.contains(NBT_FLUID_TYPE)) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString(NBT_FLUID_TYPE)));
            this.fluidType = f != null ? f : Fluids.EMPTY;
        }
        invalidateAdapterCache();
        if (level != null && level.isClientSide && !Objects.equals(before, this.fluidType)) {
            refreshClientTintMesh();
        }
    }

   /**
     * Триггер перестройки tint-меша после смены типа жидкости на клиенте.
     * Embeddium/Sodium кэшируют chunk quads — нужно явное расписание.
     */
    private void refreshClientTintMesh() {
        if (level == null || !level.isClientSide) return;
        requestModelDataUpdate();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        DoorChunkInvalidationHelper.scheduleChunkInvalidation(worldPosition);
    }

    // =====================================================================================
    // Устройство настройки: тип жидкости + Ctrl-массовая вставка по сети труб (оригинал
    // TileEntityPipeBaseNT: TOOL_CTRL → changeTypeRecursively, индекс >= длины → очистка).
    // =====================================================================================

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        if (fluidType != Fluids.EMPTY) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            list.add(net.minecraft.nbt.StringTag.valueOf(BuiltInRegistries.FLUID.getKey(fluidType).toString()));
            nbt.put("fluids", list);
        }
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, net.minecraft.world.entity.player.Player player, BlockPos pos) {
        net.minecraft.nbt.ListTag list = nbt.getList("fluids", net.minecraft.nbt.Tag.TAG_STRING);
        if (list.isEmpty()) return;

        // Оригинал: индекс за пределами списка = тип NONE (очистка трубы).
        Fluid fluid = Fluids.EMPTY;
        if (index < list.size()) {
            fluid = com.hbm_m.interfaces.IFluidCopiable.fluidFromString(list.getString(index));
        }

        if (player != null && com.hbm_m.network.CopyToolKeyState.get(player.getUUID()).ctrl) {
            com.hbm_m.block.machines.FluidDuctBlock.paintConnectedDuctNetwork(level, pos, fluid);
        } else {
            setFluidType(fluid);
        }
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        if (fluidType == Fluids.EMPTY) return new String[0];
        return new String[] {
                com.hbm_m.inventory.fluid.FluidType.forFluid(fluidType).getUnlocalizedName()
        };
    }
}
