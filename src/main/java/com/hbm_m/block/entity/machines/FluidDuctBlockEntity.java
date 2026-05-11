package com.hbm_m.block.entity.machines;

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
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;
//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if forge {
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;
 *///?}

/**
 * BlockEntity трубы. Хранит тип жидкости и управляет MK2 узлом в UniNodespace.
 * <p>
 * Визуал оверлея (neo / colored / silver): в Forge OBJ MTL задаётся {@code forge_TintIndex 1} для слоя
 * «skin»; на Fabric {@link com.hbm_m.client.model.loading.MtlData} читает это поле, JSON {@code mtl_override}
 * подставляет MTL для bake (base / overlay), а блоки труб зарегистрированы в {@code BlockRenderLayerMap}
 * как {@code cutout} в {@link com.hbm_m.client.ClientSetup}, чтобы оверлей с альфой и tint из
 * {@code ColorProviderRegistry.BLOCK} отображался как на Forge (multipart solid + cutout).
 *
 * Логика передачи жидкостей:
 *  - Узел (FluidNode) создаётся при загрузке блока/смене типа и разрушается при выгрузке.
 *  - UniNodespace обновляется раз в тик (через MainRegistry.onServerTick) и строит сети.
 *  - Каждый тик duct сканирует соседей: для Forge-машин создаёт ForgeFluidHandlerAdapter
 *    и регистрирует их как providers/receivers в сети трубы.
 *  - Фактический перенос жидкостей выполняется FluidNet.update() (в UniNodespace.updateNodespace()).
 */
public class FluidDuctBlockEntity extends BlockEntity implements IFluidPipeMK2 {

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
     * Ключ — направление от duct к машине.
     */
    private final Map<Direction, ForgeFluidHandlerAdapter> adapterCache = new EnumMap<>(Direction.class);

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
        adapterCache.clear();
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

    //? if forge {
    /*@Override
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
    *///?}

    //? if fabric {
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            ensureNode(serverLevel);
        }
    }
    //?}

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel && node != null && !node.isExpired()) {
            UniNodespace.destroyNode(serverLevel, node);
        }
        node = null;
        adapterCache.clear();
        super.setRemoved();
    }

    //? if forge {
    /*@Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel serverLevel && node != null && !node.isExpired()) {
            UniNodespace.destroyNode(serverLevel, node);
        }
        node = null;
        adapterCache.clear();
        super.onChunkUnloaded();
    }
    *///?}

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

        // === DEBUG: статус трубы раз в 2 секунды (удалить после отладки) ===
        if (level.getGameTime() % 40 == 0) {
            String typeStr = entity.fluidType == net.minecraft.world.level.material.Fluids.EMPTY
                ? "EMPTY (труба не покрашена идентификатором!)"
                : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(entity.fluidType).toString();
            int connectedSides = 0;
            for (Direction d : Direction.values()) {
                if (state.getValue(FluidDuctBlock.PROPERTY_BY_DIRECTION.get(d))) connectedSides++;
            }
            org.slf4j.LoggerFactory.getLogger("FluidDuctDBG")
                .info("[tick] pos={} fluidType={} connectedSides={} hasNode={}",
                    pos, typeStr, connectedSides, entity.node != null && !entity.node.isExpired());
        }
        // === END DEBUG ===

        if (entity.fluidType == Fluids.EMPTY) return;

        // Восстановить узел, если потерялся
        entity.ensureNode(serverLevel);
        if (entity.node == null || entity.node.isExpired()) return;

        // Для каждого подключённого соседа, не являющегося трубой, создать адаптер и подписать его
        for (Direction dir : Direction.values()) {
            if (!state.getValue(FluidDuctBlock.PROPERTY_BY_DIRECTION.get(dir))) continue;

            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            // === DEBUG (удалить после отладки) ===
            boolean shouldLogThis = level.getGameTime() % 40 == 0;
            // === END DEBUG ===

            // Пропускаем другие трубы (они сами обслуживают свои узлы)
            if (neighbor == null || neighbor instanceof FluidDuctBlockEntity) continue;
            // Пропускаем машины, реализующие MK2 напрямую (они вызывают trySubscribe/tryProvide сами)
            if (neighbor instanceof IFluidConnectorMK2) continue;

            // Проверяем, что у соседа есть IFluidHandler на нашей стороне
            Direction sideOfNeighborFacingDuct = dir.getOpposite();
            boolean hasFluidHandler = checkNeighborFluidHandler(level, neighbor, sideOfNeighborFacingDuct);

            // === DEBUG ===
            if (shouldLogThis) {
                org.slf4j.LoggerFactory.getLogger("FluidDuctDBG")
                    .info("  neighbor at {} dir={} class={} hasHandler={}",
                        neighborPos, dir, neighbor.getClass().getSimpleName(), hasFluidHandler);
            }
            // === END DEBUG ===

            if (!hasFluidHandler) {
                entity.adapterCache.remove(dir);
                continue;
            }

            ForgeFluidHandlerAdapter adapter = entity.adapterCache.get(dir);
            if (adapter == null) {
                adapter = new ForgeFluidHandlerAdapter(level, neighborPos, sideOfNeighborFacingDuct, entity.fluidType);
                entity.adapterCache.put(dir, adapter);
            }

            adapter.trySubscribe(entity.fluidType, serverLevel, pos, dir.getOpposite());
            adapter.tryProvide(entity.fluidType, serverLevel, pos, dir.getOpposite());
        }
    }


    /**
     * Проверяет наличие fluid handler у соседнего BlockEntity.
     * На Forge — через ForgeCapabilities.FLUID_HANDLER,
     * на Fabric — через Fabric Transfer API FluidStorage.SIDED.
     */
    private static boolean checkNeighborFluidHandler(Level level, BlockEntity neighbor, Direction side) {
        //? if forge {
        /*return neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent();
         *///?}
        //? if fabric {
        return FluidStorage.SIDED.find(level, neighbor.getBlockPos(), neighbor.getBlockState(), neighbor, side) != null;
        //?}
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
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluidType);
        if (loc != null) {
            tag.putString(NBT_FLUID_TYPE, loc.toString());
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        Fluid before = this.fluidType;
        super.load(tag);
        if (tag.contains(NBT_FLUID_TYPE)) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString(NBT_FLUID_TYPE)));
            this.fluidType = f != null ? f : Fluids.EMPTY;
        }
        adapterCache.clear();
        if (level != null && level.isClientSide && !Objects.equals(before, this.fluidType)) {
            refreshClientTintMesh();
        }
    }

    // =====================================================================================
    // Client sync
    // =====================================================================================

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Триггер перестройки tint-меша после смены типа жидкости на клиенте.
     * Embeddium/Sodium кэшируют chunk quads — нужно явное расписание.
     */
    private void refreshClientTintMesh() {
        if (level == null || !level.isClientSide) return;
        //? if forge {
        /*requestModelDataUpdate();
         *///?}
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        DoorChunkInvalidationHelper.scheduleChunkInvalidation(worldPosition);
    }

    // =====================================================================================
    // Capabilities — duct не экспонирует IFluidHandler напрямую.
    // Взаимодействие с Forge-машинами идёт через ForgeFluidHandlerAdapter в tick().
    // =====================================================================================
}