package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.api.fluids.FluidNetProvider;
import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.fluids.ForgeFluidHandlerAdapter;
import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidProviderMK2;
import com.hbm_m.api.fluids.IFluidReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.api.fluids.IFluidUserMK2;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.blockentity.BaseHbmBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.block.machines.FluidDuctBlock;
//? if forge {
import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}


@SuppressWarnings("UnstableApiUsage")
public class UniversalMachinePartBlockEntity extends BaseHbmBlockEntity implements IMultiblockPart, IEnergyConnector, IFluidConnectorMK2 {

    // Виртуальные узлы жидкостной сети на позиции коннектора, по одному на тип жидкости контроллера.
    // Используется для "коннектор-к-коннектору" без труб: переносы делает FluidNet,
    // как если бы между ними стояла обычная труба.
    //
    // Раньше тут был ровно один узел с "первым непустым" типом. Это ломало мультижидкостные мультиблоки
    // (хим. установка с 6 баками): сеть видела только один тип за раз. Теперь по узлу на тип
    // (UniNodespace различает узлы по NodeKey = (BlockPos, INetworkProvider)).
    private final java.util.Map<Fluid, FluidNode> fluidNodes = new java.util.HashMap<>();

    private BlockPos controllerPos;
    private PartRole role = PartRole.DEFAULT;

    /**
     * Локальный оффсет этой части относительно контроллера в сетке структуры
     * (до поворота {@code facing}). Не зависит от того, как структура повёрнута в мире,
     * поэтому сохраняется в NBT как вращение-инвариантный «адрес» части.
     *
     * <p>Когда contraption (Create / Aeronautics / Sable) разбирается и часть оказывается
     * в мире на новых координатах, часть детерминированно вычисляет, где обязан стоять
     * её контроллер:
     * <pre>{@code
     *   controllerPos = partWorldPos - rotate(localOffsetFromController, partFacing)
     * }</pre>
     * где {@code partFacing} — это FACING фантомного блока (он же FACING структуры).
     *
     * <p>Это работает потому, что Create's {@code StructureTransform} вращает и позиции,
     * и blockstate (включая FACING) одним и тем же Y-осевым поворотом R, а Y-осевые
     * повороты коммутативны: {@code R(rotate(v, F)) = rotate(v, R(F))}.
     *
     * <p>Для контрапшенов с наклоном/креном (Aeronautics pitch/roll) формула может
     * дать неверный результат — тогда работает fallback: радиус-поиск в
     * {@link com.hbm_m.multiblock.MultiblockStructureHelper#relinkOrphanedPart}.
     *
     * <p>{@code null} = не задано (старый NBT до этой правки, или часть не из структуры).
     */
    private BlockPos localOffsetFromController;

    /**
     * Одноразовый флаг самолечения для осиротевших частей (Create / Sable disassembly recovery).
     * <p>Устанавливается в true после {@link #readNbtData}, чтобы на первом server-tick часть
     * перепроверила свою ссылку на контроллер. Если {@code controllerPos} невалиден (Create
     * восстановил BE из старого NBT, переписав только собственные x/y/z), часть сама ищет
     * ближайший подходящий контроллер и перепривязывается (см.
     * {@link com.hbm_m.multiblock.MultiblockStructureHelper#relinkOrphanedPart}).
     * Потребляется за один тик; для здоровых частей — сбрасывается сразу, сканирование
     * не повторяется каждый тик (нулевая стоимость в steady state).
     */
    private boolean pendingRelinkCheck = true;

    /**
     * Retry-механизм самолечения: одноразовой проверки недостаточно, потому что
     * сразу после разборки контрапшена (Create/Sable) контроллер может быть ещё
     * не установлен на своё место, когда часть тикает первый раз. Часть повторяет
     * детерминированную проверку каждые {@link #RELINK_RETRY_INTERVAL_TICKS}
     * тиков до {@link #RELINK_MAX_ATTEMPTS} попыток (~2 минуты), после чего
     * сдаётся (контроллера действительно нет — часть осиротела навсегда).
     * Оба поля транзиентные (не сохраняются в NBT).
     */
    private static final int RELINK_RETRY_INTERVAL_TICKS = 20;
    private static final int RELINK_MAX_ATTEMPTS = 60;
    private int relinkAttempts;
    private int relinkCooldownTicks;
    private java.util.Set<Direction> allowedClimbSides = java.util.EnumSet.noneOf(Direction.class);
    /** Мировые стороны энергоподключения; пусто = не задано (для коннектора - все стороны). */
    private java.util.Set<Direction> allowedEnergySides = java.util.EnumSet.noneOf(Direction.class);
    /** Мировые стороны жидкостного подключения; пусто = не задано (для коннектора - все стороны). */
    private java.util.Set<Direction> allowedFluidSides = java.util.EnumSet.noneOf(Direction.class);

    //? if forge {
    /**
     * Reiner Konnektivitäts-Marker für JEDEN Teil der Struktur (auch DEFAULT-Phantomblöcke), NICHT
     * an die Connector-Rollen delegiert. Nodespace#PowerNode lehnt Knoten ohne
     * ModCapabilities.hasEnergyComponent() sofort ab (PowerNode#isValid) - ohne diesen Marker
     * würden DEFAULT-Blöcke nie als Knoten registriert, und der Controller könnte nie eine
     * physische Kette zu weit entfernten Connectoren (z.B. Chungus' Energie-Connector 10 Blöcke
     * entfernt) bilden. canConnectEnergy() bleibt unabhängig rollenbasiert - Kabel docken weiterhin
     * nur visuell an echten Connector-Rollen an.
     */
    private final net.minecraftforge.common.util.LazyOptional<IEnergyConnector> selfEnergyConnector =
            net.minecraftforge.common.util.LazyOptional.of(() -> this);
    //?}

    public UniversalMachinePartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.UNIVERSAL_MACHINE_PART_BE.get(), pPos, pBlockState);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel sl) {
            destroyAllFluidNodes(sl);
        }
        // Энергия: снимаем все подписки части, чтобы пересобралась сеть.
        com.hbm_m.api.energy.EnergySubscriptions.unsubscribeAll(this);
        super.setRemoved();
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null) return false;
        if (!isFluidConnector(this.role)) return false;
        // Если grid-side ограничен у коннектора, проверяем разрешённые стороны.
        if (allowedFluidSides != null && !allowedFluidSides.isEmpty() && !allowedFluidSides.contains(fromDir)) {
            return false;
        }
        return true;
    }

    @Override
    public synchronized void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setPartRole(PartRole role) {
        if (this.role != role) {
            boolean wasEnergy = this.role.canReceiveEnergy() || this.role.canSendEnergy();
            boolean isEnergy = role.canReceiveEnergy() || role.canSendEnergy();
            boolean wasFluid = isFluidConnector(this.role);
            boolean isFluid  = isFluidConnector(role);
            this.role = role;
            this.setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                // Уведомляем соседей при смене роли, влияющей на соединения (провода, трубы, etc.)
                if (wasEnergy || isEnergy || wasFluid || isFluid) {
                    level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                }
            }
        }
    }

    private static boolean isFluidConnector(PartRole r) {
        return r == PartRole.FLUID_CONNECTOR || r == PartRole.UNIVERSAL_CONNECTOR;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UniversalMachinePartBlockEntity be) {
        if (level.isClientSide) {
            return;
        }

        // Самолечение осиротевших частей (Create/Sable disassembly): один раз после загрузки
        // проверяем, валиден ли сохранённый ControllerPos. Если нет — перепривязываемся.
        // Ограничено pendingRelinkCheck (not-per-tick) → нулевая стоимость в steady state.
        //
        // ПЕРВИЧНЫЙ путь (детерминированный): если у части сохранён localOffsetFromController,
        // вычисляем контроллера по формуле controllerPos = partPos - rotate(offset, facing)
        // без радиус-поиска. Это работает для любого размера мультиблока и не путается
        // между двумя соседними одинаковыми машинами.
        //
        // FALLBACK: если localOffsetFromController == null (старый NBT) или вычисленная
        // позиция не содержит контроллер (контрапшен с наклоном/креном, или структура
        // повёрнута нестандартно) — откатываемся к радиус-поиску relinkOrphanedPart.
        if (be.pendingRelinkCheck) {
            // Retry: сразу после разборки контрапшена контроллер может быть ещё
            // не на месте — повторяем детерминированную проверку, пока не найдём.
            if (be.relinkCooldownTicks > 0) {
                be.relinkCooldownTicks--;
            } else {
                boolean stale = (be.controllerPos == null);
                if (!stale) {
                    BlockState ctrlState = level.getBlockState(be.controllerPos);
                    stale = !(ctrlState.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController);
                }
                if (!stale) {
                    // Здоровая часть — самолечение завершено.
                    be.pendingRelinkCheck = false;
                    be.relinkAttempts = 0;
                } else if (be.relinkAttempts < RELINK_MAX_ATTEMPTS) {
                    be.relinkAttempts++;
                    be.relinkCooldownTicks = RELINK_RETRY_INTERVAL_TICKS;
                    // 1) Детерминированный путь: пробуем вычислить контроллера из localOffset.
                    com.hbm_m.multiblock.MultiblockStructureHelper
                            .relinkOrphanedPartDeterministic(level, pos, be);
                    // 2) relinkOrphanedPartDeterministic сам вызывает fallback relinkOrphanedPart
                    //    при необходимости — отдельный вызов здесь НЕ нужен (двойной поиск).
                } else {
                    // Попытки исчерпаны: контроллера действительно нет рядом.
                    be.pendingRelinkCheck = false;
                }
            }
        }

        // Энергия: роль-коннектор должна быть узлом энергосети,
        // чтобы коннектор-к-коннектору работал точно как через кабели.
        if (be.role.canReceiveEnergy() || be.role.canSendEnergy()) {
            com.hbm_m.api.energy.EnergySubscriptions.update(be);
        }

        if (!isFluidConnector(be.role) || be.controllerPos == null) {
            return;
        }

        // Коннекторы вплотную НЕ делают прямую перекачку.
        // Создаём виртуальный узел на позиции коннектора и подписываем контроллер в сеть.
        if (level instanceof ServerLevel serverLevel) {
            be.tickFluidConnector(serverLevel);
        }

    }

    /**
     * Тик жидкостного коннектора: 1.7.10-философия для 1.20.1.
     *
     * Делает две вещи:
     *  1) Создаёт по виртуальному {@link FluidNode} на каждый уникальный тип жидкости контроллера —
     *     это решает проблему мультижидкостных контроллеров (хим. установка 6 баков), где раньше
     *     виден был только "первый непустой" тип.
     *  2) Подписывает контроллер в сети — нативно, если контроллер реализует MK2-интерфейсы,
     *     иначе через {@link ForgeFluidHandlerAdapter} (совместимость со сторонними машинами).
     *
     * Аналог 1.7.10: контроллер сам обходит {@code getConPos()} и делает {@code trySubscribe}/
     * {@code tryProvide}. У нас контроллер скрыт за мультиблоком; роль "посредника" играет коннектор.
     */
    private void tickFluidConnector(ServerLevel serverLevel) {
        BlockEntity controller = serverLevel.getBlockEntity(controllerPos);
        if (controller == null || controller.isRemoved()) {
            destroyAllFluidNodes(serverLevel);
            return;
        }

        // 1) Собираем уникальные типы жидкостей, которые контроллер хочет видеть в сетях.
        //    Для MK2-контроллеров — все его баки; для обычных — обходим Forge IFluidHandler.
        java.util.Set<Fluid> activeTypes = collectControllerFluidTypes(controller);

        if (activeTypes.isEmpty()) {
            destroyAllFluidNodes(serverLevel);
            return;
        }

        // 2) Удалить узлы для типов, которые больше неактуальны.
        java.util.Iterator<java.util.Map.Entry<Fluid, FluidNode>> it = fluidNodes.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Fluid, FluidNode> entry = it.next();
            Fluid f = entry.getKey();
            FluidNode n = entry.getValue();
            if (!activeTypes.contains(f) || n == null || n.isExpired()) {
                if (n != null && !n.isExpired()) UniNodespace.destroyNode(serverLevel, n);
                it.remove();
            }
        }

        // 3) Создать недостающие узлы.
        NodeDirPos[] connections = buildFluidNodeConnections();
        for (Fluid type : activeTypes) {
            if (fluidNodes.containsKey(type)) continue;
            FluidNode node = new FluidNode(FluidNetProvider.forFluid(type), worldPosition)
                    .setConnections(connections);
            UniNodespace.createNode(serverLevel, node);
            fluidNodes.put(type, node);
        }

        // 4) Подписать контроллер в каждой сети (per fluid type).
        boolean ctrlMk2 = controller instanceof IFluidUserMK2;
        boolean ctrlSender = controller instanceof IFluidStandardSenderMK2;
        boolean ctrlReceiver = controller instanceof IFluidStandardReceiverMK2;

        for (Fluid type : activeTypes) {
            var nodeRef = UniNodespace.getNode(serverLevel, worldPosition, FluidNetProvider.forFluid(type));
            if (!(nodeRef instanceof FluidNode fn) || fn.net == null) continue;

            if (ctrlMk2) {
                // Нативный MK2: регистрируем контроллер прямо в сети.
                // FluidNet.update() сам разруливает (type, pressure) через get*Tanks().
                if (controller instanceof IFluidProviderMK2 prov) fn.net.addProvider(prov);
                if (controller instanceof IFluidReceiverMK2 rec)  fn.net.addReceiver(rec);
            } else {
                // Совместимость: оборачиваем в Forge-адаптер (pressure=0).
                ForgeFluidHandlerAdapter adapter = new ForgeFluidHandlerAdapter(serverLevel, worldPosition, null, type);
                fn.net.addProvider(adapter);
                fn.net.addReceiver(adapter);
            }
        }

        // 5) Если контроллер MK2 — попросим его trySubscribe/tryProvide ещё и в трубы соседей,
        //    чтобы сети, в которых нет наших виртуальных узлов (например, чужая труба соседнего
        //    мультиблока), увидели контроллер. Это аналог 1.7.10 getConPos()-обхода.
        if (ctrlMk2) {
            for (Direction dir : Direction.values()) {
                if (allowedFluidSides != null && !allowedFluidSides.isEmpty() && !allowedFluidSides.contains(dir)) {
                    continue;
                }
                BlockPos pipePos = worldPosition.relative(dir);
                BlockEntity pipeBe = serverLevel.getBlockEntity(pipePos);
                // Подписываемся только в IFluidConnectorMK2 (трубы/коннекторы).
                // Прочие соседи (обычные машины) — не цель: для них всё ещё работает legacy-путь
                // через FluidDuctBlockEntity → ForgeFluidHandlerAdapter, если они вообще через трубу.
                if (!(pipeBe instanceof IFluidConnectorMK2)) continue;

                if (ctrlReceiver) {
                    IFluidStandardReceiverMK2 rec = (IFluidStandardReceiverMK2) controller;
                    for (FluidTank t : rec.getReceivingTanks()) {
                        Fluid type = t.getTankType();
                        if (type == null || type == Fluids.EMPTY) continue;
                        if (type == com.hbm_m.inventory.fluid.ModFluids.NONE.getSource()) continue;
                        rec.trySubscribe(type, serverLevel, pipePos, dir);
                    }
                }
                if (ctrlSender) {
                    IFluidStandardSenderMK2 snd = (IFluidStandardSenderMK2) controller;
                    for (FluidTank t : snd.getSendingTanks()) {
                        if (t.getFill() <= 0) continue;
                        snd.tryProvide(t, serverLevel, pipePos, dir);
                    }
                }
            }
        }
    }

    /**
     * Возвращает множество уникальных типов жидкостей, которые контроллер представлен в сети.
     * Для MK2 — все баки {@code getAllTanks()} с непустым типом; для остальных — пробуем читать
     * Forge IFluidHandler / Fabric Transfer API (а для цистерны — её настроенный тип, даже если бак пуст).
     */
    private java.util.Set<Fluid> collectControllerFluidTypes(BlockEntity controller) {
        java.util.Set<Fluid> result = new java.util.LinkedHashSet<>();
        if (controller instanceof IFluidUserMK2 mk2) {
            for (FluidTank tank : mk2.getAllTanks()) {
                Fluid type = tank.getTankType();
                if (type != null && type != Fluids.EMPTY
                        && type != com.hbm_m.inventory.fluid.ModFluids.NONE.getSource()) {
                    result.add(type);
                }
            }
            return result;
        }
        if (controller instanceof MachineFluidTankBlockEntity tank) {
            Fluid type = tank.getFluidTank().getTankType();
            if (type != null && type != Fluids.EMPTY
                    && type != com.hbm_m.inventory.fluid.ModFluids.NONE.getSource()) {
                result.add(type);
            }
            return result;
        }
        //? if forge {
        IFluidHandler handler = controller.getCapability(ForgeCapabilities.FLUID_HANDLER, null).resolve().orElse(null);
        if (handler != null) {
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fs = handler.getFluidInTank(i);
                if (fs != null && !fs.isEmpty()) result.add(fs.getFluid());
            }
        }
        //?}
        //? if fabric {
        /*if (controller.getLevel() instanceof ServerLevel sl) {
            BlockPos bp = controller.getBlockPos();
            BlockState st = sl.getBlockState(bp);
            Storage<FluidVariant> storage = FluidStorage.SIDED.find(sl, bp, st, controller, null);
            if (storage != null) {
                for (StorageView<FluidVariant> view : storage) {
                    if (!view.isResourceBlank() && view.getAmount() > 0) {
                        Fluid f = view.getResource().getFluid();
                        if (f != null && f != Fluids.EMPTY
                                && f != com.hbm_m.inventory.fluid.ModFluids.NONE.getSource()) {
                            result.add(f);
                        }
                    }
                }
            }
        }
        *///?}
        return result;
    }

    private NodeDirPos[] buildFluidNodeConnections() {
        if (allowedFluidSides == null || allowedFluidSides.isEmpty()) {
            return new NodeDirPos[] {
                    new NodeDirPos(worldPosition.relative(Direction.EAST),  Direction.EAST),
                    new NodeDirPos(worldPosition.relative(Direction.WEST),  Direction.WEST),
                    new NodeDirPos(worldPosition.relative(Direction.UP),    Direction.UP),
                    new NodeDirPos(worldPosition.relative(Direction.DOWN),  Direction.DOWN),
                    new NodeDirPos(worldPosition.relative(Direction.SOUTH), Direction.SOUTH),
                    new NodeDirPos(worldPosition.relative(Direction.NORTH), Direction.NORTH),
            };
        }
        java.util.ArrayList<NodeDirPos> cons = new java.util.ArrayList<>();
        for (Direction d : Direction.values()) {
            if (allowedFluidSides.contains(d)) {
                cons.add(new NodeDirPos(worldPosition.relative(d), d));
            }
        }
        return cons.toArray(new NodeDirPos[0]);
    }

    private void destroyAllFluidNodes(ServerLevel serverLevel) {
        for (FluidNode n : fluidNodes.values()) {
            if (n != null && !n.isExpired()) UniNodespace.destroyNode(serverLevel, n);
        }
        fluidNodes.clear();
    }

    @Override
    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    @Override
    public BlockPos getLocalOffsetFromController() {
        return this.localOffsetFromController;
    }

    @Override
    public void setLocalOffsetFromController(BlockPos offset) {
        this.localOffsetFromController = offset == null ? null : offset.immutable();
        this.setChanged();
    }

    @Override
    public PartRole getPartRole() {
        return this.role;
    }

    @Override
    public void setAllowedClimbSides(java.util.Set<Direction> sides) {
        // EnumSet.copyOf(Collection) бросает IllegalArgumentException на пустой коллекции
        // (не может вывести enum-класс). Используем defensive-копию с явным EnumSet.noneOf.
        this.allowedClimbSides = copyDirectionSet(sides);
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public java.util.Set<Direction> getAllowedClimbSides() {
        return this.allowedClimbSides;
    }

    @Override
    public void setAllowedEnergySides(java.util.Set<Direction> sides) {
        this.allowedEnergySides = copyDirectionSet(sides);
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public java.util.Set<Direction> getAllowedEnergySides() {
        return this.allowedEnergySides;
    }

    @Override
    public void setAllowedFluidSides(java.util.Set<Direction> sides) {
        this.allowedFluidSides = copyDirectionSet(sides);
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    /**
     * Безопасная defensive-копия {@link java.util.Set<Direction>} в {@link EnumSet}.
     * <p>{@code EnumSet.copyOf(Collection)} бросает {@link IllegalArgumentException} на пустой
     * коллекции (не может вывести enum-класс). Этот helper всегда возвращает валидный
     * {@code EnumSet} — пустой ли, нет — и принимает {@code null} как пустое множество.
     * Используется всеми {@code setAllowed*Sides}-сеттерами в мультиблок-частях и контроллерах.
     */
    private static java.util.EnumSet<Direction> copyDirectionSet(java.util.Set<Direction> sides) {
        java.util.EnumSet<Direction> out = java.util.EnumSet.noneOf(Direction.class);
        if (sides != null && !sides.isEmpty()) {
            out.addAll(sides);
        }
        return out;
    }

    @Override
    public java.util.Set<Direction> getAllowedFluidSides() {
        return this.allowedFluidSides;
    }

    /**
     * Как Forge {@code getCapability(HBM_ENERGY_*)}: часть с ролью коннектора участвует в визуале проводов и
     * {@link com.hbm_m.capability.ModCapabilities#hasEnergyComponent} на Fabric ({@code instanceof}).
     */
    @Override
    public boolean canConnectEnergy(@Nullable Direction side) {
        if (!this.role.canReceiveEnergy() && !this.role.canSendEnergy()) {
            return false;
        }
        if (side == null) {
            return true;
        }
        if (allowedEnergySides.isEmpty()) {
            return true;
        }
        return allowedEnergySides.contains(side);
    }

    //? if forge {
    @Override
    public void onLoad() {
        super.onLoad();
        // При загрузке мира роль восстанавливается из NBT, минуя setPartRole.
        // Уведомляем соседей, чтобы трубы/провода обновили визуальные соединения.
        if (level != null && !level.isClientSide() &&
                (isFluidConnector(role) || role.canReceiveEnergy() || role.canSendEnergy())) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        // Клиент: после перезахода трубы могут визуально "не увидеть" коннектор до первого апдейта.
        // Пересчитаем соединения вокруг части, чтобы рукава не отлипали/не липли к контроллеру случайно.
        if (level != null && level.isClientSide && (isFluidConnector(role) || role.canReceiveEnergy() || role.canSendEnergy())) {
            FluidDuctBlock.refreshAdjacentDucts(level, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.level instanceof ServerLevel sl) {
            destroyAllFluidNodes(sl);
        }
        com.hbm_m.api.energy.EnergySubscriptions.unsubscribeAll(this);
    }


    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        var level = this.level;
        if (this.controllerPos == null || level == null) {
            return super.getCapability(cap, side);
        }

        BlockEntity controllerBE = level.getBlockEntity(this.controllerPos);
        if (controllerBE == null) {
            return super.getCapability(cap, side);
        }

        // === ДЕЛЕГИРОВАНИЕ ЭНЕРГИИ ===
        // ENERGY_CONNECTOR и UNIVERSAL_CONNECTOR оба принимают/отдают энергию (PartRole.canReceiveEnergy/canSendEnergy)
        if (this.role.canReceiveEnergy() || this.role.canSendEnergy()) {
            boolean energySideOk = side == null
                    || allowedEnergySides.isEmpty()
                    || allowedEnergySides.contains(side);
            if (!energySideOk) {
                return super.getCapability(cap, side);
            }

            // HBM API (Provider, Receiver, Connector)
            if (cap == ModCapabilities.HBM_ENERGY_PROVIDER ||
                    cap == ModCapabilities.HBM_ENERGY_RECEIVER ||
                    cap == ModCapabilities.HBM_ENERGY_CONNECTOR)
            {
                return controllerBE.getCapability(cap, side);
            }

            // Forge Energy API (как и было)
            if (cap == ForgeCapabilities.ENERGY) {
                return controllerBE.getCapability(cap, side);
            }
        }

        // Reiner Konnektivitäts-Marker für ALLE Rollen (auch DEFAULT) - siehe Javadoc bei
        // selfEnergyConnector. Greift nur, wenn der Block oben NICHT schon als echte
        // Connector-Rolle an den Controller delegiert hat.
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return selfEnergyConnector.cast();
        }

        // === ДЕЛЕГИРОВАНИЕ ПРЕДМЕТОВ ===
        if (cap == ForgeCapabilities.ITEM_HANDLER &&
                (this.role == PartRole.ITEM_INPUT || this.role == PartRole.ITEM_OUTPUT
                        || this.role == PartRole.UNIVERSAL_CONNECTOR)) {
            // MachineAssemblerBlockEntity вернет специальный proxy-handler
            if (controllerBE instanceof MachineAssemblerBlockEntity assembler && this.role != PartRole.UNIVERSAL_CONNECTOR) {
                return assembler.getItemHandlerForPart(this.role).cast();
            }
            if (controllerBE instanceof MachineBlastFurnaceBlockEntity furnace) {
                return controllerBE.getCapability(cap, side);
            }

            // Для других машин (если появятся) можно делегировать напрямую
            return controllerBE.getCapability(cap, side);
        }

        // === ДЕЛЕГИРОВАНИЕ ЖИДКОСТЕЙ ===
        if (cap == ForgeCapabilities.FLUID_HANDLER && isFluidConnector(this.role)) {
            boolean fluidSideOk = side == null
                    || allowedFluidSides.isEmpty()
                    || allowedFluidSides.contains(side);
            if (!fluidSideOk) {
                return super.getCapability(cap, side);
            }
            // Всегда делегируем в контроллер как "внутренний" доступ (side == null),
            // чтобы настройки сторон контроллера не блокировали подключение через коннектор.
            return controllerBE.getCapability(cap, null);
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        selfEnergyConnector.invalidate();
    }
    //?}

    @Override
    protected void writeNbtData(@NotNull CompoundTag pTag, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(pTag, registries);
        if (this.controllerPos != null) {
            com.hbm_m.platform.PlatformHooks.writeBlockPos(pTag, "ControllerPos", this.controllerPos);
        }
        pTag.putString("PartRole", this.role.name());

        // Локальный оффсет от контроллера — вращение-инвариантный «адрес» части.
        // Сохраняем всегда (даже если controllerPos null), чтобы после contraption
        // disassembly часть могла детерминированно вычислить новый controllerPos.
        // См. javadoc у localOffsetFromController.
        if (this.localOffsetFromController != null) {
            pTag.putLong("LocalOffset", this.localOffsetFromController.asLong());
        }

        if (!allowedClimbSides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedClimbSides) mask |= (1 << dir.get3DDataValue());
            pTag.putInt("ClimbSides", mask);
        }
        if (!allowedEnergySides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedEnergySides) mask |= (1 << dir.get3DDataValue());
            pTag.putInt("EnergySides", mask);
        }
        if (!allowedFluidSides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedFluidSides) mask |= (1 << dir.get3DDataValue());
            pTag.putInt("FluidSides", mask);
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag pTag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(pTag, registries);
        if (pTag.contains("ControllerPos")) {
            this.controllerPos = com.hbm_m.platform.PlatformHooks.readBlockPos(pTag, "ControllerPos");
        }
        // Восстанавливаем локальный оффсет от контроллера (вращение-инвариантный).
        // Create/Sable disassembly сохраняет этот тег как есть (он не зависит от worldPos),
        // поэтому после разборки часть «помнит», на каком месте сетки она стояла.
        if (pTag.contains("LocalOffset")) {
            this.localOffsetFromController = BlockPos.of(pTag.getLong("LocalOffset"));
        } else {
            this.localOffsetFromController = null;
        }
        // Create/Sable disassembly восстанавливает BE из старого NBT, переписывая только x/y/z,
        // но НЕ ControllerPos. Активируем самолечение на следующем server-tick.
        this.pendingRelinkCheck = true;
        if (pTag.contains("PartRole")) {
            try {
                this.role = PartRole.valueOf(pTag.getString("PartRole"));
            } catch (IllegalArgumentException e) {
                this.role = PartRole.DEFAULT;
            }
        }
        if (pTag.contains("ClimbSides")) {
            int mask = pTag.getInt("ClimbSides");
            allowedClimbSides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) allowedClimbSides.add(dir);
            }
        }
        if (pTag.contains("EnergySides")) {
            int mask = pTag.getInt("EnergySides");
            allowedEnergySides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) allowedEnergySides.add(dir);
            }
        }
        if (pTag.contains("FluidSides")) {
            int mask = pTag.getInt("FluidSides");
            allowedFluidSides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) allowedFluidSides.add(dir);
            }
        }
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        super.applyClientUpdate(tag);
        
        // Принудительно обновляем состояние блока на клиенте, чтобы обновилась визуализация/логика
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════════════════════
    //  Polymorphic Capability Delegation (для NeoForge автоматической регистрации)
    // ═══════════════════════════════════════════════════════════════════════════════════════════════

    @Override
    public @Nullable Object getItemHandler(@Nullable net.minecraft.core.Direction side) {
        if (level == null || controllerPos == null) return null;
        if (this.role != PartRole.ITEM_INPUT && this.role != PartRole.ITEM_OUTPUT
                && this.role != PartRole.UNIVERSAL_CONNECTOR) return null;

        BlockEntity ctrl = level.getBlockEntity(controllerPos);
        if (ctrl instanceof MachineAssemblerBlockEntity) {
            //? if forge {
            return ((MachineAssemblerBlockEntity) ctrl).getItemHandlerForPart(this.role).resolve().orElse(null);
            //?} elif neoforge {
            /*return ((MachineAssemblerBlockEntity) ctrl).getItemHandler(side);
             *///?}
        }
        if (ctrl instanceof BaseHbmBlockEntity hbm) {
            return hbm.getItemHandler(side);
        }
        return null;
    }

    @Override
    public @Nullable Object getFluidHandler(@Nullable net.minecraft.core.Direction side) {
        if (level == null || controllerPos == null) return null;
        if (this.role != PartRole.FLUID_CONNECTOR && this.role != PartRole.UNIVERSAL_CONNECTOR) return null;
        if (allowedFluidSides != null && !allowedFluidSides.isEmpty() && !allowedFluidSides.contains(side)) return null;

        BlockEntity ctrl = level.getBlockEntity(controllerPos);
        if (ctrl instanceof BaseHbmBlockEntity hbm) {
            return hbm.getFluidHandler(null);
        }
        return null;
    }

    @Override
    public @Nullable Object getEnergyStorage(@Nullable net.minecraft.core.Direction side) {
        if (level == null || controllerPos == null) return null;
        if (!this.role.canReceiveEnergy() && !this.role.canSendEnergy()) return null;
        if (allowedEnergySides != null && !allowedEnergySides.isEmpty() && !allowedEnergySides.contains(side)) return null;

        BlockEntity ctrl = level.getBlockEntity(controllerPos);
        if (ctrl instanceof BaseHbmBlockEntity hbm) {
            return hbm.getEnergyStorage(side);
        }
        return null;
    }
}
