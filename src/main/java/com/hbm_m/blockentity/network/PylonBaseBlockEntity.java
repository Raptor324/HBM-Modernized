package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.energy.PowerConductor;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.ColorUtil;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Базовое блок-энтити пилона/коннектора длинной ЛЭП — порт TileEntityPylonBaseNT (1.7.10).
 * Хранит список подключённых пилонов (соединяется предметом-проводкой), красит кабель красителем
 * и создаёт PowerNode со связями по всем подключённым пилонам.
 */
public abstract class PylonBaseBlockEntity extends BaseHbmBlockEntity implements PowerConductor {

    public enum ConnectionType { SINGLE, TRIPLE, QUAD }

    protected final List<BlockPos> connected = new ArrayList<>();
    public int color;

    public PylonBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean canConnectEnergy(@Nullable Direction side) {
        return true;
    }

    /** Кабели тянутся до 100 м — рендер-габарит бесконечный (как в оригинале). */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(-1.0E7, -1.0E7, -1.0E7, 1.0E7, 1.0E7, 1.0E7);
    }

    public static void tick(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, PylonBaseBlockEntity entity) {
        if (level.isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level;
        Nodespace.PowerNode node = Nodespace.getNode(serverLevel, pos);
        if (node == null || node.expired) {
            Nodespace.createNode(serverLevel, entity.createNode(pos));
        }
    }

    /** Статическая проверка возможности соединения двух пилонов. Возврат: 0 = ок, 1 = разные типы, 2 = сам с собой, 3 = слишком далеко. */
    public static int canConnect(PylonBaseBlockEntity first, PylonBaseBlockEntity second) {
        if (first.getConnectionType() != second.getConnectionType()) return 1;
        if (first == second) return 2;
        double len = Math.min(first.getMaxWireLength(), second.getMaxWireLength());
        Vec3 a = first.getConnectionPoint();
        Vec3 b = second.getConnectionPoint();
        return len >= a.distanceTo(b) ? 0 : 3;
    }

    public void addConnection(BlockPos other) {
        connected.add(other.immutable());
        if (level instanceof ServerLevel serverLevel) {
            Nodespace.PowerNode node = Nodespace.getNode(serverLevel, getBlockPos());
            if (node != null) {
                node.recentlyChanged = true;
                node.addConnection(new NodeDirPos(other, null));
            }
        }
        setChanged();
        syncToClient();
    }

    /** Порт disconnectAll из 1.7.10: разрывает все соединения и уничтожает узлы на обоих концах. */
    public void disconnectAll() {
        if (!(level instanceof ServerLevel serverLevel)) {
            connected.clear();
            return;
        }
        for (BlockPos pos : connected) {
            BlockEntity be = serverLevel.getBlockEntity(pos);
            if (be == this) continue;
            if (be instanceof PylonBaseBlockEntity pylon) {
                Nodespace.destroyNode(serverLevel, pos);
                pylon.connected.removeIf(p -> p.equals(getBlockPos()));
                pylon.setChanged();
                pylon.syncToClient();
            }
        }
        connected.clear();
        Nodespace.destroyNode(serverLevel, getBlockPos());
    }

    /** Съедает один краситель и красит кабель. Возврат true, если цвет изменился. */
    public boolean setColor(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        int color = ColorUtil.getColorFromDye(stack);
        if (color == 0 || color == this.color) return false;
        stack.shrink(1);
        this.color = color;
        setChanged();
        syncToClient();
        return true;
    }

    @Override
    public Nodespace.PowerNode createNode(BlockPos pos) {
        Nodespace.PowerNode node = new Nodespace.PowerNode(Nodespace.THE_POWER_PROVIDER, pos)
                .setConnections(new NodeDirPos(pos, null));
        for (BlockPos p : connected) node.addConnection(new NodeDirPos(p, null));
        addExtraConnections(node, pos);
        return node;
    }

    /** Дополнительные соединения узла для конкретных пилонов (соседи пилона, коннектор к машине). */
    protected void addExtraConnections(Nodespace.PowerNode node, BlockPos pos) {}

    public ConnectionType getConnectionType() { return ConnectionType.SINGLE; }
    /** Точки крепления кабеля относительно позиции блока. */
    public abstract Vec3[] getMountPos();
    public abstract double getMaxWireLength();

    public Vec3 getConnectionPoint() {
        Vec3[] mounts = getMountPos();
        if (mounts == null || mounts.length == 0) return Vec3.atCenterOf(getBlockPos());
        return mounts[0].add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
    }

    public List<BlockPos> getConnected() { return connected; }

    /** Текст для сообщений игрока при соединении (дублирует строки ItemWiring). */
    public static Component messageFor(int code) {
        return switch (code) {
            case 0 -> Component.translatable("chat.hbm_m.wire_end");
            case 1 -> Component.translatable("chat.hbm_m.wire_error_type");
            case 2 -> Component.translatable("chat.hbm_m.wire_error_same");
            default -> Component.translatable("chat.hbm_m.wire_error_far");
        };
    }

    public static void tryConnect(Player player, PylonBaseBlockEntity first, PylonBaseBlockEntity second) {
        int code = canConnect(first, second);
        if (code == 0) {
            first.addConnection(second.getBlockPos());
            second.addConnection(first.getBlockPos());
        }
        player.displayClientMessage(messageFor(code), false);
    }

    protected void syncToClient() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("color", color);
        ListTag list = new ListTag();
        for (BlockPos p : connected) {
            list.add(net.minecraft.nbt.LongTag.valueOf(p.asLong()));
        }
        tag.put("connected", list);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        color = tag.getInt("color");
        connected.clear();
        ListTag list = tag.getList("connected", Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            connected.add(BlockPos.of(((net.minecraft.nbt.LongTag) list.get(i)).getAsLong()));
        }
    }
}
