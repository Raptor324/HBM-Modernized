package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.api.fluids.FluidNetProvider;
import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.fluids.ForgeFluidHandlerAdapter;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.api.energy.EnergyNetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

//? if forge {
/*import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.block.machines.FluidDuctBlock;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
//?}

public class UniversalMachinePartBlockEntity extends BlockEntity implements IMultiblockPart, IEnergyConnector {

    // Виртуальный узел жидкостной сети на позиции коннектора.
    // Используется для "коннектор-к-коннектору" без труб: всё управление переносом делает FluidNet,
    // как если бы между ними стояла обычная труба.
    @Nullable
    private FluidNode fluidNode;
    @Nullable
    private Fluid fluidNodeType;

    private BlockPos controllerPos;
    private PartRole role = PartRole.DEFAULT;
    private java.util.Set<Direction> allowedClimbSides = java.util.EnumSet.noneOf(Direction.class);
    /** Мировые стороны энергоподключения; пусто = не задано (для коннектора - все стороны). */
    private java.util.Set<Direction> allowedEnergySides = java.util.EnumSet.noneOf(Direction.class);
    /** Мировые стороны жидкостного подключения; пусто = не задано (для коннектора - все стороны). */
    private java.util.Set<Direction> allowedFluidSides = java.util.EnumSet.noneOf(Direction.class);

    public UniversalMachinePartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.UNIVERSAL_MACHINE_PART_BE.get(), pPos, pBlockState);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel sl) {
            destroyFluidNode(sl);
            // Энергия: убираем узел части, чтобы пересобралась сеть.
            EnergyNetworkManager.get(sl).removeNode(worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public synchronized void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
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

        // Энергия: роль-коннектор должна быть узлом EnergyNetworkManager,
        // чтобы коннектор-к-коннектору работал точно как через кабели.
        if (level instanceof ServerLevel serverLevel
                && (be.role.canReceiveEnergy() || be.role.canSendEnergy())) {
            EnergyNetworkManager manager = EnergyNetworkManager.get(serverLevel);
            if (!manager.hasNode(pos)) {
                manager.addNode(pos);
            }
        }

        if (!isFluidConnector(be.role) || be.controllerPos == null) {
            return;
        }

        // Коннекторы вплотную НЕ делают прямую перекачку.
        // Создаём виртуальный узел на позиции коннектора и подписываем контроллер в сеть.
        if (level instanceof ServerLevel serverLevel) {
            be.tickFluidConnector(serverLevel);
        }
        return;

        //? if fabric {
        long selfKey = pos.asLong();
        for (Direction dir : Direction.values()) {
            BlockPos otherPos = pos.relative(dir);
            if (selfKey >= otherPos.asLong()) continue;
            BlockEntity otherBe = level.getBlockEntity(otherPos);
            if (!(otherBe instanceof UniversalMachinePartBlockEntity otherPart)) continue;
            if (!isFluidConnector(otherPart.role) || otherPart.controllerPos == null) continue;
            if (be.controllerPos.equals(otherPart.controllerPos)) continue;
            tryDirectFluidTransferFabric(level, be, otherPart);
        }
        return;
        //?}

    }

    //? if forge {
    /*private void tickFluidConnector(ServerLevel serverLevel) {
        BlockEntity controller = serverLevel.getBlockEntity(controllerPos);
        if (controller == null || controller.isRemoved()) {
            destroyFluidNode(serverLevel);
            return;
        }

        // Determine fluid channel for this node.
        // Для цистерны учитываем "заданный тип" при пустом баке.
        Fluid type = null;
        if (controller instanceof MachineFluidTankBlockEntity tank) {
            type = tank.getFluidTank().getTankType();
        } else {
            IFluidHandler handler = controller.getCapability(ForgeCapabilities.FLUID_HANDLER, null).resolve().orElse(null);
            if (handler != null) {
                for (int i = 0; i < handler.getTanks(); i++) {
                    FluidStack fs = handler.getFluidInTank(i);
                    if (fs != null && !fs.isEmpty()) {
                        type = fs.getFluid();
                        break;
                    }
                }
            }
        }

        if (type == null || type == Fluids.EMPTY) {
            destroyFluidNode(serverLevel);
            return;
        }

        if (fluidNode == null || fluidNode.isExpired() || fluidNodeType != type) {
            destroyFluidNode(serverLevel);
            fluidNodeType = type;
            fluidNode = new FluidNode(FluidNetProvider.forFluid(type), worldPosition)
                    .setConnections(buildFluidNodeConnections());
            UniNodespace.createNode(serverLevel, fluidNode);
        }

        var node = UniNodespace.getNode(serverLevel, worldPosition, FluidNetProvider.forFluid(type));
        if (node instanceof FluidNode fn && fn.net != null) {
            // Wrap controller behind this part; adapter resolves controllerPos + side=null for IMultiblockPart.
            ForgeFluidHandlerAdapter adapter = new ForgeFluidHandlerAdapter(serverLevel, worldPosition, null, type);
            fn.net.addProvider(adapter);
            fn.net.addReceiver(adapter);
        }
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

    private void destroyFluidNode(ServerLevel serverLevel) {
        if (fluidNode != null && !fluidNode.isExpired()) {
            UniNodespace.destroyNode(serverLevel, fluidNode);
        }
        fluidNode = null;
        fluidNodeType = null;
    }
    *///?}

    //? if fabric {
    @SuppressWarnings("UnstableApiUsage")
    private static void tryDirectFluidTransferFabric(Level level, UniversalMachinePartBlockEntity a, UniversalMachinePartBlockEntity b) {
        var aStorage = FluidStorage.SIDED.find(level, a.controllerPos, null);
        var bStorage = FluidStorage.SIDED.find(level, b.controllerPos, null);
        if (aStorage == null || bStorage == null) return;

        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            for (var view : aStorage) {
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                long maxDrain = Math.min(view.getAmount(), DIRECT_FLUID_TRANSFER_PER_TICK * 81L);
                long inserted = bStorage.insert(view.getResource(), maxDrain, tx);
                if (inserted > 0) {
                    long extracted = view.extract(view.getResource(), inserted, tx);
                    if (extracted > 0) {
                        tx.commit();
                        return;
                    }
                }
                break;
            }
        }

        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            for (var view : bStorage) {
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                long maxDrain = Math.min(view.getAmount(), DIRECT_FLUID_TRANSFER_PER_TICK * 81L);
                long inserted = aStorage.insert(view.getResource(), maxDrain, tx);
                if (inserted > 0) {
                    long extracted = view.extract(view.getResource(), inserted, tx);
                    if (extracted > 0) {
                        tx.commit();
                        return;
                    }
                }
                break;
            }
        }
    }
    //?}

    @Override
    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    @Override
    public PartRole getPartRole() {
        return this.role;
    }

    @Override
    public void setAllowedClimbSides(java.util.Set<Direction> sides) {
        this.allowedClimbSides = java.util.EnumSet.copyOf(sides);
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
        this.allowedEnergySides = java.util.EnumSet.copyOf(sides);
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
        this.allowedFluidSides = java.util.EnumSet.copyOf(sides);
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
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

    /**
     * Fabric Transfer API: делегирование энергии в контроллер (аналог Forge {@code getCapability(ENERGY, side)}).
     * Зарегистрирован через {@code EnergyStorage.SIDED.registerForBlockEntity} в FabricEntrypoint.
     */
    //? if fabric {
    @Nullable
    public team.reborn.energy.api.EnergyStorage getEnergyStorageSided(@Nullable Direction side) {
        if (this.controllerPos == null || this.level == null) return null;
        if (!this.role.canReceiveEnergy() && !this.role.canSendEnergy()) return null;
        boolean energySideOk = side == null
                || allowedEnergySides.isEmpty()
                || allowedEnergySides.contains(side);
        if (!energySideOk) return null;
        BlockEntity ctrl = this.level.getBlockEntity(this.controllerPos);
        if (ctrl == null) return null;
        return team.reborn.energy.api.EnergyStorage.SIDED.find(this.level, this.controllerPos, ctrl.getBlockState(), ctrl, null);
    }
    //?}

    /**
     * Fabric Transfer API: делегирование жидкости в контроллер (аналог Forge {@code getCapability(FLUID_HANDLER, null)}).
     */
    //? if fabric {
    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (this.controllerPos == null || this.level == null) {
            return null;
        }
        if (!isFluidConnector(this.role)) {
            return null;
        }
        boolean fluidSideOk = side == null
                || allowedFluidSides.isEmpty()
                || allowedFluidSides.contains(side);
        if (!fluidSideOk) {
            return null;
        }
        BlockEntity ctrl = this.level.getBlockEntity(this.controllerPos);
        if (ctrl == null) {
            return null;
        }
        return FluidStorage.SIDED.find(this.level, this.controllerPos, ctrl.getBlockState(), ctrl, null);
    }
    //?}

    //? if forge {
    /*@Override
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
            destroyFluidNode(sl);
            EnergyNetworkManager.get(sl).removeNode(worldPosition);
        }
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

        // === ДЕЛЕГИРОВАНИЕ ПРЕДМЕТОВ ===
        if (cap == ForgeCapabilities.ITEM_HANDLER &&
                (this.role == PartRole.ITEM_INPUT || this.role == PartRole.ITEM_OUTPUT))
        {
            // MachineAssemblerBlockEntity вернет специальный proxy-handler
            if (controllerBE instanceof MachineAssemblerBlockEntity assembler) {
                return assembler.getItemHandlerForPart(this.role).cast();
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
    *///?}

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        if (this.controllerPos != null) {
            pTag.put("ControllerPos", NbtUtils.writeBlockPos(this.controllerPos));
        }
        pTag.putString("PartRole", this.role.name());

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
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("ControllerPos")) {
            this.controllerPos = NbtUtils.readBlockPos(pTag.getCompound("ControllerPos"));
        }
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
        //? if fabric {
        if (level != null && !level.isClientSide()
                && (isFluidConnector(this.role) || this.role.canReceiveEnergy() || this.role.canSendEnergy())) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        //?}
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    //? if forge {
    /*@Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
            
            // Принудительно обновляем состояние блока на клиенте, чтобы обновилась визуализация/логика
            if (level != null && level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }
    *///?}
}
