package com.hbm_m.block.entity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.ConnectionPriority;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.block.machines.MachineFluidTankBlock;
import com.hbm_m.interfaces.IMultiblockSidedIO;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.menu.MachineFluidTankMenu;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

@SuppressWarnings("UnstableApiUsage")
public class MachineFluidTankBlockEntity extends BlockEntity implements MenuProvider, IMultiblockSidedIO, IFluidStandardTransceiverMK2
    //? if fabric {
    /*, net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity
    *///?}
{

    //? if fabric {
    /*/^*
     * Единый экземпляр для Fabric Transfer API (регистрация {@link com.hbm_m.main.FabricEntrypoint}).
     * Логика fill/drain как у Forge {@code NetworkFluidHandlerWrapper}.
     ^/
    @SuppressWarnings("UnstableApiUsage")
    private final TankFabricNetworkStorage tankFabricNetworkStorage = new TankFabricNetworkStorage(this);
    *///?}

    public static final int SLOT_ID_IN = 0;
    public static final int SLOT_ID_OUT = 1;
    public static final int SLOT_LOAD_IN = 2;
    public static final int SLOT_LOAD_OUT = 3;
    public static final int SLOT_UNLOAD_IN = 4;
    public static final int SLOT_UNLOAD_OUT = 5;

    public static final int MODES = 4;
    private static final int TANK_CAPACITY = 256000;

    /**
     * Режим ввода/вывода.
     * Важно: значение 0 запрещает drain через capability (см. {@link NetworkFluidHandlerWrapper}),
     * что ломает переток "бак → сеть" при дефолтном состоянии.
     */
    private short mode = 1;
    public boolean hasExploded = false;
    public boolean onFire = false;
    private byte lastRedstone = 0;
    private int age = 0;

    @Nullable
    private Fluid filterFluid = null;

    private final FluidTank fluidTank;
    private final ModItemStackHandler itemHandler;
    protected final ContainerData data;

    //? if forge {
    private final LazyOptional<IItemHandler> lazyItemHandler;
    private final LazyOptional<IFluidHandler> lazyFluidHandler;
    //?}

    /** Разрешённые стороны прямого подключения к контроллеру. Если {@link #fluidSidesFromMultiblockStructure} — пусто допустимо (= ни одной стороны); иначе пусто = все стороны. */
    private java.util.Set<Direction> allowedFluidSides = java.util.EnumSet.noneOf(Direction.class);
    /** Набор из tuple fluidSideMap символа контроллера при постройке мультиблока. */
    private boolean fluidSidesFromMultiblockStructure = false;

    //? if forge {
    public static final ModelProperty<ResourceLocation> FLUID_TEXTURE_PROPERTY = new ModelProperty<>();
    //?}

    public MachineFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK_BE.get(), pos, state);

        this.fluidTank = new FluidTank(ModFluids.NONE.getSource(), TANK_CAPACITY);

        this.itemHandler = new ModItemStackHandler(6) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot != SLOT_ID_OUT && slot != SLOT_LOAD_OUT && slot != SLOT_UNLOAD_OUT;
            }
        };

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                switch (index) {
                    case 0: return fluidTank.getFill();
                    case 1: return BuiltInRegistries.FLUID.getId(fluidTank.getTankType());
                    case 2: return mode;
                    case 3: return hasExploded ? 1 : 0;
                    case 4: return onFire ? 1 : 0;
                    case 5: return fluidTank.getPressure();
                    case 6:
                        if (filterFluid == null || filterFluid == Fluids.EMPTY) return -1;
                        return BuiltInRegistries.FLUID.getId(filterFluid);
                    default: return 0;
                }
            }
            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 2: mode = (short) value; break;
                    case 3: hasExploded = value == 1; break;
                    case 4: onFire = value == 1; break;
                }
            }
            @Override
            public int getCount() { return 7; }
        };

        //? if forge {
        this.lazyItemHandler = LazyOptional.of(() -> itemHandler);
        this.lazyFluidHandler = LazyOptional.of(() -> new NetworkFluidHandlerWrapper(this));
        //?}
    }

    //? if forge {
    @Override
    public void onLoad() {
        super.onLoad();
        // На Forge ModelData кешируется отдельно от NBT; при загрузке чанка гарантируем первичную инициализацию.
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
        // Важно: при загрузке чанка load(NBT) может отработать до установки level у BE,
        // поэтому refreshAdjacentFluidDuctConnections() там иногда пропускается.
        // Дёргаем повторно здесь, чтобы визуальные соединения труб всегда пересчитались после перезахода.
        refreshAdjacentFluidDuctConnections();
    }
    //?}

    // =====================================================================================
    // IFluidStandardTransceiverMK2 — нативное участие в MK2-сети.
    // Mode: 0=drain only (отдача в сеть), 1=fill+drain буфер, 2=fill only (приём из сети), 3=lock.
    // Совпадает с gui.hbm_m.fluid_tank.mode.*: 0 Output only, 2 Input only (подписи раньше были перепутаны).
    // Приоритет: режим 1 (буфер) → LOW, чтобы обычные приёмники забирали жидкость первыми
    // (как 1.7.10 TileEntityMachineFluidTank#getFluidPriority).
    // =====================================================================================

    private static final FluidTank[] EMPTY_TANKS = new FluidTank[0];
    private static final long BASE_NETWORK_SPEED_MB_PER_TICK = 1_000_000_000L;

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[]{ fluidTank }; }

    /** В сеть сливаем, если режим разрешает drain (0/1) и взорванный — нет. */
    @Override
    public FluidTank[] getSendingTanks() {
        if (hasExploded || mode == 2 || mode == 3) return EMPTY_TANKS;
        return new FluidTank[]{ fluidTank };
    }

    /** Из сети принимаем, если режим разрешает fill (1/2) и взорванный — нет. */
    @Override
    public FluidTank[] getReceivingTanks() {
        if (hasExploded || mode == 0 || mode == 3) return EMPTY_TANKS;
        return new FluidTank[]{ fluidTank };
    }

    @Override
    public long getProviderSpeed(Fluid fluid, int pressure) {
        if (hasExploded || mode == 2 || mode == 3) return 0L;
        // Скорость зависит от заполненности: пустой бак почти не отдаёт → меньше "пинг-понга".
        // Дополнительно ограничиваем "не более половины текущего fill за тик", чтобы два одинаковых буфера
        // не могли полностью поменяться местами за один тик (классический full↔empty пингпонг).
        long dyn = fluidTank.getDynamicNetworkSpeedMb(BASE_NETWORK_SPEED_MB_PER_TICK, true);
        long halfFill = Math.max(1L, (long) fluidTank.getFill() / 2L);
        return Math.min(dyn, halfFill);
    }

    @Override
    public long getReceiverSpeed(Fluid fluid, int pressure) {
        if (hasExploded || mode == 0 || mode == 3) return 0L;
        // Скорость зависит от свободного места: полный бак почти не принимает.
        long dyn = fluidTank.getDynamicNetworkSpeedMb(BASE_NETWORK_SPEED_MB_PER_TICK, false);
        long halfSpace = Math.max(1L, (long) Math.max(0, fluidTank.getMaxFill() - fluidTank.getFill()) / 2L);
        return Math.min(dyn, halfSpace);
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        // mode == 1 → буфер: заливать в последнюю очередь (LOW), как 1.7.10.
        if (!hasExploded && mode == 1) return ConnectionPriority.LOW;
        return ConnectionPriority.NORMAL;
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null || hasExploded) return false;
        // Если стороны заданы (через мультиблок-структуру или вручную) — фильтруем.
        if (fluidSidesFromMultiblockStructure) {
            if (!allowedFluidSides.contains(fromDir)) return false;
        } else if (!allowedFluidSides.isEmpty() && !allowedFluidSides.contains(fromDir)) {
            return false;
        }
        // Принимаем подключение либо по совпадению типа, либо если бак ещё пуст и тип не зафиксирован.
        Fluid current = fluidTank.getTankType();
        if (current == null || current == Fluids.EMPTY || current == ModFluids.NONE.getSource()) {
            return true;
        }
        return com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(current, fluid);
    }

    /**
     * Бесконечный источник, если в инвентаре лежит {@link InfiniteFluidItem} c instant-режимом
     * (порт fluid_barrel_infinite из 1.7.10).
     */
    @Override
    public boolean isInfiniteNetworkSource(Fluid fluid) {
        if (hasExploded || mode == 2 || mode == 3) return false;
        if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, fluidTank.getTankType())) return false;
        return hasInstantInfiniteBarrel();
    }

    /** Бесконечный сток-утилизатор по тем же критериям. */
    @Override
    public boolean isInfiniteNetworkSink(Fluid fluid) {
        if (hasExploded || mode == 0 || mode == 3) return false;
        if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, fluidTank.getTankType())) return false;
        return hasInstantInfiniteBarrel();
    }

    private boolean hasInstantInfiniteBarrel() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof InfiniteFluidItem inf && inf.isInstantNetwork()) {
                return true;
            }
        }
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFluidTankBlockEntity entity) {
        if (level.isClientSide) return;

        // 1.7.10-стиль: подписываемся на трубы у разрешённых сторон.
        // FluidDuctBlockEntity#tick сам по себе нас не подключит, потому что мы IFluidConnectorMK2
        // (это сделано умышленно — нативный MK2-путь даёт давление/приоритет/per-(fluid,pressure)).
        Fluid mk2Type = entity.fluidTank.getTankType();
        if (mk2Type != null && mk2Type != Fluids.EMPTY && mk2Type != ModFluids.NONE.getSource()) {
            for (Direction dir : Direction.values()) {
                if (entity.fluidSidesFromMultiblockStructure) {
                    if (!entity.allowedFluidSides.contains(dir)) continue;
                } else if (!entity.allowedFluidSides.isEmpty() && !entity.allowedFluidSides.contains(dir)) {
                    continue;
                }
                BlockPos pipePos = pos.relative(dir);
                BlockEntity pipeBe = level.getBlockEntity(pipePos);
                // Подписываемся только в HBM-трубы/коннекторы; на чужие машины (или Forge IFluidHandler-машины)
                // продолжит работать классический путь через Forge capability.
                if (!(pipeBe instanceof com.hbm_m.api.fluids.IFluidConnectorMK2)) continue;

                // mode != 0/3 → принимаем
                if (!entity.hasExploded && entity.mode != 0 && entity.mode != 3) {
                    entity.trySubscribe(mk2Type, level, pipePos, dir);
                }
                // mode != 2/3 и есть содержимое → отдаём
                if (!entity.hasExploded && entity.mode != 2 && entity.mode != 3 && entity.fluidTank.getFill() > 0) {
                    entity.tryProvide(entity.fluidTank, level, pipePos, dir);
                }
            }
        }

        if (!entity.hasExploded) {
            entity.age++;
            if (entity.age >= 20) {
                entity.age = 0;
                entity.setChanged();
            }

            if (entity.fluidTank.getFill() > 0) {
                Fluid type = entity.fluidTank.getTankType();
                FluidType ftype = FluidType.forFluid(type);
                if (ftype.isAntimatter()) {
                    entity.explode();
                    entity.fluidTank.fill(0);
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 5F, Level.ExplosionInteraction.BLOCK);
                }
                if (ftype.isCorrosive()) {
                    FT_Corrosive corrosive = ftype.getTrait(FT_Corrosive.class);
                    if (corrosive != null && corrosive.isHighlyCorrosive()) {
                        entity.explode();
                    }
                }
            }

            if (entity.hasExploded) {
                entity.updateLeak(entity.calculateLeakAmount());
                return;
            }

            // --- ОБРАБОТКА ИНВЕНТАРЯ ---
            // Сначала идентификатор: тип бака должен быть известен до loadTank (ведра, InfiniteFluidItem в том же тике).
            boolean changed = false;
            ItemStack[] slotsArray = entity.getSlotsArray();

            if (entity.fluidTank.setType(SLOT_ID_IN, SLOT_ID_OUT, slotsArray)) {
                changed = true;
            }

            if (entity.fluidTank.loadTank(SLOT_LOAD_IN, SLOT_LOAD_OUT, slotsArray)) {
                changed = true;
            }

            // TANK -> ITEM
            if (entity.fluidTank.unloadTank(SLOT_UNLOAD_IN, SLOT_UNLOAD_OUT, slotsArray)) {
                changed = true;
            }

            if (changed) {
                // Применяем изменения слотов обратно в Handler, так как FluidTank работает с массивом
                entity.applySlotsArray(slotsArray);
                entity.setChanged();
                	level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
                entity.refreshAdjacentFluidDuctConnections();
            }

        } else {
            if (entity.fluidTank.getFill() > 0) {
                entity.updateLeak(entity.calculateLeakAmount());
            }
        }

        byte comp = entity.getComparatorPower();
        if (comp != entity.lastRedstone) {
            entity.lastRedstone = comp;
            level.updateNeighborsAt(pos, state.getBlock());
            entity.setChanged();
        }
    }

    //? if forge {
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        // Важно: super.onDataPacket вызывает load(tag). Нам нужно сравнить старое/новое и
        // при смене текстуры попросить Forge обновить ModelData и пересобрать меш чанка.
        final boolean clientForge = level != null && level.isClientSide;
        final ResourceLocation prevTankTextureForge = clientForge ? getTankTextureLocation() : null;

        super.onDataPacket(net, pkt);

        if (clientForge && prevTankTextureForge != null && !prevTankTextureForge.equals(getTankTextureLocation())) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
    }
    //?}

    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        if (level != null && level.isClientSide) {
            //? if forge {
            requestModelDataUpdate();
            //?}
            // На Forge для корректной перерисовки нужен UPDATE_CLIENTS (иначе baked model может не обновиться).
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
            //? if fabric {
            /*scheduleChunkRebuild();
            *///?}
        }
    }

    /**
     * Уведомляет соседей (в т.ч. multipart-трубы) о смене доступности/типа жидкости: все блоки мультиблока,
     * чтобы обновились соединения у лиц коннекторов.
     */
    public void refreshAdjacentFluidDuctConnections() {
        Level level = this.level;
        if (level == null) return;
        if (level.isClientSide) {
            refreshFluidDuctVisualsAroundMultiblock();
            return;
        }
        BlockState st = getBlockState();
        Block blk = st.getBlock();
        if (blk instanceof MachineFluidTankBlock tankBlock) {
            Direction facing = st.getValue(MachineFluidTankBlock.FACING);
            MultiblockStructureHelper helper = tankBlock.getStructureHelper();
            for (BlockPos p : helper.getAllPartPositions(worldPosition, facing)) {
                Block at = level.getBlockState(p).getBlock();
                level.updateNeighborsAt(p, at);
            }
        } else {
            level.updateNeighborsAt(worldPosition, blk);
        }
    }

    /**
     * На клиенте {@link Level#updateNeighborsAt} не дергает пересчёт multipart у труб — после синка сторон жидкости
     * рукава остаются как до постройки. Пересчитываем состояние каждой соседней трубы у всех блоков мультиблока.
     */
    private void refreshFluidDuctVisualsAroundMultiblock() {
        Level level = this.level;
        if (level == null || !level.isClientSide) return;
        BlockState st = getBlockState();
        if (!(st.getBlock() instanceof MachineFluidTankBlock tankBlock)) {
            for (Direction d : Direction.values()) {
                BlockPos n = worldPosition.relative(d);
                BlockState bs = level.getBlockState(n);
                if (bs.getBlock() instanceof FluidDuctBlock duct) {
                    BlockState next = duct.getConnectionState(level, n);
                    if (!next.equals(bs)) {
                        level.setBlock(n, next, Block.UPDATE_CLIENTS);
                    }
                }
            }
            return;
        }
        Direction facing = st.getValue(MachineFluidTankBlock.FACING);
        MultiblockStructureHelper helper = tankBlock.getStructureHelper();
        for (BlockPos p : helper.getAllPartPositions(worldPosition, facing)) {
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                BlockState bs = level.getBlockState(n);
                if (bs.getBlock() instanceof FluidDuctBlock duct) {
                    BlockState next = duct.getConnectionState(level, n);
                    if (!next.equals(bs)) {
                        level.setBlock(n, next, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    //? if fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    private void scheduleChunkRebuild() {
        if (level != null && level.isClientSide) {
            com.hbm_m.client.render.DoorChunkInvalidationHelper.scheduleChunkInvalidation(worldPosition);
        }
    }

    /^*
     * Сторона {@code null}: делегирование с коннекторов мультиблока / внутренний доступ без фильтра по грани.
     * Для {@code side != null}: если задан tuple fluidSideMap контроллера — только перечисленные грани; иначе пустой разрешённый набор = все грани (совместимость).
     ^/
    @SuppressWarnings("UnstableApiUsage")
    @org.jetbrains.annotations.Nullable
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> getFluidStorage(
            @org.jetbrains.annotations.Nullable Direction side) {
        if (side != null) {
            if (fluidSidesFromMultiblockStructure) {
                if (!allowedFluidSides.contains(side)) {
                    return null;
                }
            } else if (!allowedFluidSides.isEmpty() && !allowedFluidSides.contains(side)) {
                return null;
            }
        }
        return tankFabricNetworkStorage;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static final class TankFabricNetworkStorage
            extends net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<TankFabricNetworkStorage.Snapshot>
            implements net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> {

        private static final long DROPLETS_PER_MB = 81L;
        private final MachineFluidTankBlockEntity entity;
        /^* Остаток дроплетов (&lt; 81), накапливается между insert до полной мБ — уменьшает потери округления на Fabric. ^/
        private long insertRemainderDroplets;

        private TankFabricNetworkStorage(MachineFluidTankBlockEntity entity) {
            this.entity = entity;
        }

        private FluidTank tank() {
            return entity.fluidTank;
        }

        @Override
        public long insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant resource, long maxAmount,
                net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            if (entity.hasExploded || entity.mode == 0 || entity.mode == 3) return 0;
            if (resource.isBlank() || maxAmount <= 0) return 0;
            if (tank().getPressure() != 0) return 0;
            if (tank().getFill() <= 0 && !FluidTank.isFluidTypeExplicitlySet(tank().getTankType())) {
                return 0;
            }

            long spaceMb = tank().getMaxFill() - tank().getFill();
            if (spaceMb <= 0) return 0;

            if (tank().getFill() > 0 && !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank().getTankType(), resource.getFluid())) {
                return 0;
            }

            long spaceDroplets = spaceMb * DROPLETS_PER_MB;
            long rBefore = insertRemainderDroplets;
            long incoming = maxAmount + rBefore;
            long takeDroplets = Math.min(incoming, spaceDroplets);
            long mbAdd = takeDroplets / DROPLETS_PER_MB;
            long consumedFromOffer = Math.min(maxAmount, Math.max(0L, takeDroplets - rBefore));
            if (mbAdd <= 0) {
                updateSnapshots(transaction);
                insertRemainderDroplets = (incoming - takeDroplets) + (takeDroplets % DROPLETS_PER_MB);
                entity.setChanged();
                return consumedFromOffer;
            }

            updateSnapshots(transaction);
            insertRemainderDroplets = (incoming - takeDroplets) + (takeDroplets % DROPLETS_PER_MB);
            if (tank().getTankType() == Fluids.EMPTY || tank().getFill() == 0) {
                tank().setTankType(resource.getFluid());
            }
            tank().fill(tank().getFill() + (int) mbAdd);
            entity.setChanged();
            entity.refreshAdjacentFluidDuctConnections();
            return consumedFromOffer;
        }

        @Override
        public long extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant resource, long maxAmount,
                net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            if (entity.hasExploded || entity.mode == 2 || entity.mode == 3) return 0;
            if (resource.isBlank() || maxAmount <= 0) return 0;
            if (tank().getFill() <= 0) return 0;
            if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank().getTankType(), resource.getFluid())) {
                return 0;
            }

            long toDrainMb = Math.min(tank().getFill(), maxAmount / DROPLETS_PER_MB);
            if (toDrainMb <= 0) return 0;

            updateSnapshots(transaction);
            tank().fill(tank().getFill() - (int) toDrainMb);
            entity.setChanged();
            entity.refreshAdjacentFluidDuctConnections();
            return toDrainMb * DROPLETS_PER_MB;
        }

        @Override
        public boolean isResourceBlank() {
            return tank().getFill() <= 0 || tank().getTankType() == Fluids.EMPTY;
        }

        @Override
        public net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant getResource() {
            return isResourceBlank()
                    ? net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank()
                    : net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(tank().getTankType());
        }

        @Override
        public long getAmount() {
            return (long) tank().getFill() * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            return (long) tank().getMaxFill() * DROPLETS_PER_MB;
        }

        @Override
        protected Snapshot createSnapshot() {
            return new Snapshot(tank().getTankType(), tank().getFill(), insertRemainderDroplets);
        }

        @Override
        protected void readSnapshot(Snapshot snapshot) {
            tank().setTankType(snapshot.type);
            tank().fill(snapshot.amountMb);
            insertRemainderDroplets = snapshot.insertRemainderDroplets;
        }

        @Override
        protected void onFinalCommit() {}

        private record Snapshot(Fluid type, int amountMb, long insertRemainderDroplets) {}
    }
    *///?}

    public ResourceLocation getTankTextureLocation() {
        Fluid fluid = fluidTank.getTankType();
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            fluid = getFilterFluid();
        }
    
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            //? if fabric && < 1.21.1 {
            /*return new ResourceLocation(MainRegistry.MOD_ID, "block/tank/tank_none");
            *///?} else {
                        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/tank/tank_none");
            //?}

        }
    
        ResourceLocation typeId = BuiltInRegistries.FLUID.getKey(fluid);
        String fluidName = typeId != null ? typeId.getPath() : "none";
        
        //? if fabric && < 1.21.1 {
        /*return new ResourceLocation(MainRegistry.MOD_ID, "block/tank/tank_" + fluidName);
        *///?} else {
                return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/tank/tank_" + fluidName);
        //?}

    }

    //? if forge {
    @Override
    public @NotNull ModelData getModelData() {
        return ModelData.builder()
                .with(FLUID_TEXTURE_PROPERTY, getTankTextureLocation())
                .build();
    }
    //?}

    //? if fabric {
    /*@Override
    public @org.jetbrains.annotations.Nullable Object getRenderAttachmentData() {
        return getTankTextureLocation();
    }
    *///?}

    public void explode() {
        if (this.hasExploded) return;
        this.hasExploded = true;
        this.onFire = FluidType.forFluid(fluidTank.getTankType()).hasTrait(FT_Flammable.class);
        this.setChanged();
    }

    public void repair() {
        this.hasExploded = false;
        this.onFire = false;
        this.setChanged();
    }

    private int calculateLeakAmount() {
        Fluid type = fluidTank.getTankType();
        FluidType ftype = FluidType.forFluid(type);
        int max = fluidTank.getMaxFill();
        int current = fluidTank.getFill();
        
        if (ftype.isAntimatter()) {
            return current; 
        } else if (ftype.hasTrait(FT_Gaseous.class)) {
            return Math.min(current, max / 100); 
        } else {
            return Math.min(current, max / 10000); 
        }
    }

    private void updateLeak(int amount) {
        if (!hasExploded || amount <= 0) return;

        fluidTank.fill(Math.max(0, fluidTank.getFill() - amount));
        Fluid type = fluidTank.getTankType();
        FluidType ftype = FluidType.forFluid(type);

        if (ftype.isAntimatter()) {
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 5F, ExplosionInteraction.BLOCK);
        } else if (ftype.hasTrait(FT_Flammable.class) && onFire) {
            AABB fireBox = new AABB(worldPosition).inflate(2.5, 5.0, 2.5);
            List<LivingEntity> affected = level.getEntitiesOfClass(LivingEntity.class, fireBox);
            for (LivingEntity e : affected) {
                e.setSecondsOnFire(5);
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + level.random.nextDouble(), worldPosition.getY() + 0.5 + level.random.nextDouble(), worldPosition.getZ() + level.random.nextDouble(), 3, 0.1, 0.1, 0.1, 0.05);
            }
        } else if (ftype.hasTrait(FT_Gaseous.class)) {
            if (level.getGameTime() % 5 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 5, 0.2, 0.5, 0.2, 0.05);
            }
        }
    }

    public byte getComparatorPower() {
        if (fluidTank.getFill() == 0) return 0;
        double frac = (double) fluidTank.getFill() / (double) fluidTank.getMaxFill() * 15D;
        return (byte) (Mth.clamp((int) frac + 1, 0, 15));
    }

    public void handleModeButton() {
        mode = (short) ((mode + 1) % MODES);
        setChanged();
    }

    public short getMode() { return mode; }
    public FluidTank getFluidTank() { return fluidTank; }
    public com.hbm_m.platform.ModItemStackHandler getItemHandler() { return itemHandler; }
    
    private ItemStack[] getSlotsArray() {
        ItemStack[] arr = new ItemStack[6];
        for (int i = 0; i < 6; i++) arr[i] = itemHandler.getStackInSlot(i);
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        for (int i = 0; i < 6; i++) itemHandler.setStackInSlot(i, arr[i]);
    }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3.0D);
    }

    @Override
    public void load(CompoundTag tag) {
        //? if fabric {
        /*// На Fabric клиентский пакет синхронизации часто приходит через load(), а не через
        // onDataPacket (Forge). Без инвалидации чанка Sodium держит старые квады baked-модели.
        final boolean clientFabric = level != null && level.isClientSide;
        final ResourceLocation prevTankTexture = clientFabric ? getTankTextureLocation() : null;
        *///?}
        //? if forge {
        final boolean clientForge = level != null && level.isClientSide;
        final ResourceLocation prevTankTextureForge = clientForge ? getTankTextureLocation() : null;
        //?}

        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        fluidTank.readFromNBT(tag, "tank");
        // Старые миры могли не иметь этого поля — по умолчанию нужен режим, который умеет и fill и drain.
        mode = tag.contains("mode") ? tag.getShort("mode") : 1;
        hasExploded = tag.getBoolean("exploded");
        onFire = tag.getBoolean("onFire");

        if (tag.contains("filterFluid")) {
            filterFluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("filterFluid")));
        } else {
            filterFluid = null;
        }

        if (tag.contains("AllowedFluidSides")) {
            int mask = tag.getInt("AllowedFluidSides");
            allowedFluidSides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) {
                    allowedFluidSides.add(dir);
                }
            }
        }
        if (tag.contains("FluidSidesFromMbStructure")) {
            fluidSidesFromMultiblockStructure = tag.getBoolean("FluidSidesFromMbStructure");
        }

        //? if fabric {
        /*if (clientFabric && !getTankTextureLocation().equals(prevTankTexture)) {
            scheduleChunkRebuild();
        }
        *///?}

        //? if forge {
        if (clientForge && prevTankTextureForge != null && !prevTankTextureForge.equals(getTankTextureLocation())) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
        //?}
        if (level != null) {
            refreshAdjacentFluidDuctConnections();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        fluidTank.writeToNBT(tag, "tank");
        tag.putShort("mode", mode);
        tag.putBoolean("exploded", hasExploded);
        tag.putBoolean("onFire", onFire);

        if (filterFluid != null && filterFluid != Fluids.EMPTY) {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(filterFluid);
            if (key != null) {
                tag.putString("filterFluid", key.toString());
            }
        }

        if (fluidSidesFromMultiblockStructure) {
            tag.putBoolean("FluidSidesFromMbStructure", true);
            int mask = 0;
            for (Direction dir : allowedFluidSides) {
                mask |= (1 << dir.get3DDataValue());
            }
            tag.putInt("AllowedFluidSides", mask);
        } else if (!allowedFluidSides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedFluidSides) mask |= (1 << dir.get3DDataValue());
            tag.putInt("AllowedFluidSides", mask);
        }
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
            if (cap == ForgeCapabilities.FLUID_HANDLER) {
                if (side != null) {
                    if (fluidSidesFromMultiblockStructure) {
                        if (!allowedFluidSides.contains(side)) {
                            return LazyOptional.empty();
                        }
                    } else if (!allowedFluidSides.isEmpty() && !allowedFluidSides.contains(side)) {
                        return LazyOptional.empty();
                    }
                }
                return lazyFluidHandler.cast();
            }
        return super.getCapability(cap, side);
    }
    //?}

    @Override
    public void setAllowedFluidSidesFromMultiblockStructure(java.util.Set<Direction> sides) {
        this.allowedFluidSides = java.util.EnumSet.copyOf(sides);
        this.fluidSidesFromMultiblockStructure = true;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setAllowedFluidSides(java.util.Set<Direction> sides) {
        this.allowedFluidSides = java.util.EnumSet.copyOf(sides);
        this.fluidSidesFromMultiblockStructure = false;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public java.util.Set<Direction> getAllowedFluidSides() {
        return this.allowedFluidSides;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        //? if forge {
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
        //?}
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.FLUID_TANK.get().getDescriptionId().toString());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineFluidTankMenu(id, inventory, this, this.data);
    }

    /**
     * Shift+ПКМ идентификатором по цистерне: задаёт тип бака (включая NONE), сливает содержимое, обновляет фильтр для GUI.
     */
    public void setFilterFromIdentifier(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        Fluid resolved;
        if (stack.getItem() instanceof FluidIdentifierItem) {
            resolved = FluidIdentifierItem.resolvePrimaryForTank(stack);
            if (resolved == null) {
                return;
            }
        } else if (stack.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier idItem) {
            Fluid t = idItem.getType(level, worldPosition, stack);
            if (t == null || t == Fluids.EMPTY) {
                return;
            }
            resolved = t;
        } else {
            return;
        }

        // Если тип уже совпадает — ничего не делаем (не сливаем бак).
        if (com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(resolved, fluidTank.getTankType())) {
            return;
        }

        fluidTank.assignTypeAndZeroFluid(resolved);
        filterFluid = resolved == ModFluids.NONE.getSource() ? null : resolved;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
            refreshAdjacentFluidDuctConnections();
        }
    }

    @Nullable
    public Fluid getFilterFluid() {
        return filterFluid;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    //? if forge {
    private class NetworkFluidHandlerWrapper implements IFluidHandler {
        private final MachineFluidTankBlockEntity entity;
        private IFluidHandler internal;

        public NetworkFluidHandlerWrapper(MachineFluidTankBlockEntity entity) {
            this.entity = entity;
            this.internal = entity.fluidTank.getCapability().orElse(null);
        }

        @Override
        public int getTanks() { return internal.getTanks(); }

        @Override
        public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) { return internal.getFluidInTank(tank); }

        @Override
        public int getTankCapacity(int tank) { return internal.getTankCapacity(tank); }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            return internal.isFluidValid(tank, stack);
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (entity.hasExploded || entity.mode == 0 || entity.mode == 3) return 0;
            return internal.fill(resource, action);
        }

        @NotNull
        @Override
        public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (entity.hasExploded || entity.mode == 2 || entity.mode == 3) return net.minecraftforge.fluids.FluidStack.EMPTY;
            return internal.drain(resource, action);
        }

        @NotNull
        @Override
        public net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (entity.hasExploded || entity.mode == 2 || entity.mode == 3) return net.minecraftforge.fluids.FluidStack.EMPTY;
            return internal.drain(maxDrain, action);
        }
    }
    //?}
}