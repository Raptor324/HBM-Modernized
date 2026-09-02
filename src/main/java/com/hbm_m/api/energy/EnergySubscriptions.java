package com.hbm_m.api.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.MapMaker;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Движок подписки машин на энергосеть — аналог autoPort/EnumTransferAction из 1.7.10.
 * Машины сами периодически (с бэкоффом) подписываются к сетям соседних проводников.
 *
 * Задержки повторных попыток соответствуют оригинальному EnumTransferAction:
 *   NOTHING = 20 тиков (ничего не найдено),
 *   CONNECT_NET = 10 тиков (подписка успешна),
 *   PROVIDE_DIRECT = 1 тик (прямая передача соседу).
 */
public final class EnergySubscriptions {

    private EnergySubscriptions() {}

    private static final int DELAY_NOTHING = 20;
    private static final int DELAY_CONNECT_NET = 10;
    private static final int DELAY_PROVIDE_DIRECT = 1;

    /**
     * Единая карта backoff-состояний и реестр для централизованного драйвера подписок.
     * Слабые ключи и значения: выгрузка чанка/GC сами убирают запись — отдельная
     * периодическая чистка не нужна (раньше тут был второй strong-key HashMap с
     * full-scan removeIf при каждом update() — главный пожиратель TPS).
     */
    private static final Map<BlockEntity, BackoffState> REGISTRY =
            new MapMaker().concurrencyLevel(1).weakKeys().weakValues().makeMap();

    private static final class BackoffState {
        int receiverDelay;
        int providerDelay;
        long lastUpdate = Long.MIN_VALUE;
    }

    /** Регистрация BE в драйвере (вызывается из setLevel на серверной стороне). */
    public static void register(BlockEntity be) {
        REGISTRY.putIfAbsent(be, new BackoffState());
    }

    /**
     * Централизованный драйвер подписок (вызывается раз в серверный тик из MainRegistry,
     * ДО UniNodespace.updateNodespace). Покрывает ВСЕ машины с capability энергии,
     * включая те, чей собственный тик не вызывает {@code ensureNetworkInitialized}.
     *
     * Дедупликация: если машина уже обновилась в этом тике через свой тик,
     * повторный вызов пропускается (метка времени в BackoffState).
     */
    public static void tickAll(MinecraftServer server) {
        List<BlockEntity> dead = null;
        for (Map.Entry<BlockEntity, BackoffState> e : REGISTRY.entrySet()) {
            BlockEntity be = e.getKey();
            if (be == null || be.isRemoved() || !(be.getLevel() instanceof ServerLevel)) {
                if (be != null && be.isRemoved()) {
                    if (dead == null) dead = new ArrayList<>();
                    dead.add(be);
                }
                continue;
            }
            // Фильтр: только участники энергосети-подписчики
            boolean conductor = be instanceof PowerConductor && !(be instanceof PowerBuffer);
            if (conductor) continue;
            if (!(be instanceof IEnergyReceiver) && !(be instanceof IEnergyProvider)) continue;

            // Чанк машины не тикает (выгружен/не готов): update() ходит getBlockEntity
            // по соседям и может синхронно загрузить чанк обратно — пропускаем,
            // подписка восстановится, когда чанк снова начнет тикать машину.
            if (!be.getLevel().isLoaded(be.getBlockPos())) continue;

            if (be instanceof com.hbm_m.blockentity.BaseMachineBlockEntity machine) {
                machine.ensureNetworkInitialized();
            } else {
                update(be);
            }
        }
        if (dead != null) for (BlockEntity b : dead) REGISTRY.remove(b);
    }

    /**
     * Вызывать каждый серверный тик из тика машины.
     * Сканирует 6 граней позиции BE (и дополнительные позиции мультиблока)
     * и поддерживает подписки receiver/provider.
     */
    public static void update(BlockEntity be, BlockPos... extraPositions) {
        if (!(be.getLevel() instanceof ServerLevel level) || be.isRemoved()) return;

        BackoffState st = REGISTRY.computeIfAbsent(be, k -> new BackoffState());

        // Дедупликация: одна машина обновляется не чаще одного раза за игровой тик
        // (собственный тик машины + централизованный драйвер могут вызвать update дважды)
        long now = level.getGameTime();
        if (st.lastUpdate == now) return;
        st.lastUpdate = now;

        boolean isReceiver = be instanceof IEnergyReceiver;
        // PowerBuffer — одновременно проводник и накопитель: в небуферных режимах
        // подписывается как обычная машина (input/output), поэтому не исключаем его.
        boolean isProvider = be instanceof IEnergyProvider && !(be instanceof PowerConductor && !(be instanceof PowerBuffer));

        // --- Буферный режим батареи: junction-узел (аналог TileEntityMachineBattery, mode_buffer) ---
        // Батарея становится «кабельным блоком»: создает собственный нод и подписывает
        // себя как provider+receiver в собственную сеть.
        if (be instanceof PowerBuffer buffer) {
            int mode = (be instanceof com.hbm_m.interfaces.IEnergyModeHolder holder) ? holder.getCurrentMode() : 0;
            if (mode == 0) { // BOTH == buffer
                Nodespace.PowerNode node = Nodespace.getNode(level, be.getBlockPos());
                if (node == null) {
                    node = buffer.createNode(be.getBlockPos());
                    Nodespace.createNode(level, node);
                }
                if (node.net != null && node.net.isValid()) {
                    if (be instanceof IEnergyProvider prov) node.net.addProvider(prov);
                    if (be instanceof IEnergyReceiver rec) node.net.addReceiver(rec);
                }
                return;
            } else {
                // Режим сменился с буферного — уничтожаем junction-нод (сеть пересоберется сама)
                Nodespace.destroyNode(level, be.getBlockPos());
            }
        }

        if (!isReceiver && !isProvider) return;

        // --- Подписка как получатель ---
        // Паритет с оригиналом (autoPort): попытка подписки выполняется безусловно,
        // даже если приемник полон — так обновляется timestamp подписки и запись
        // не выпадает по таймауту 3с (нулевой спрос отфильтруется в PowerNet.update).
        if (isReceiver) {
            if (--st.receiverDelay <= 0) {
                IEnergyReceiver rec = (IEnergyReceiver) be;
                boolean any = subscribeAt(level, rec, be.getBlockPos(), extraPositions);
                st.receiverDelay = any ? DELAY_CONNECT_NET : DELAY_NOTHING;
            }
        }

        // --- Подписка как поставщик ---
        // PROVIDE_DIRECT = 1 тик: при прямом питании соседа ретраем каждый тик.
        if (isProvider) {
            if (--st.providerDelay <= 0) {
                IEnergyProvider prov = (IEnergyProvider) be;
                boolean any = provideAt(level, prov, be.getBlockPos(), extraPositions);
                st.providerDelay = any ? DELAY_PROVIDE_DIRECT : DELAY_NOTHING;
            }
        }
    }

    /**
     * Полностью снять подписки BE при удалении, включая дополнительные порты мультиблока.
     * Паритет с оригиналом: там машина сама снимает записи со всех соседних нодов
     * (TileEntityMachineBattery, ветки else при смене режима / invalidate).
     */
    public static void unsubscribeAll(BlockEntity be) {
        unsubscribeAll(be, new BlockPos[0]);
    }

    public static void unsubscribeAll(BlockEntity be, BlockPos... extraPositions) {
        REGISTRY.remove(be);
        if (!(be.getLevel() instanceof ServerLevel level)) return;

        // Junction-нод буферной батареи тоже уничтожается (аналог invalidate в оригинале)
        if (be instanceof PowerBuffer) {
            Nodespace.destroyNode(level, be.getBlockPos());
        }

        // ВАЖНО: этот метод вызывается из setRemoved/onChunkUnloaded, в том числе при
        // выгрузке мира/остановке сервера. Здесь ЗАПРЕЩЕНЫ обращения к миру
        // (level.getBlockEntity по соседям): на границе с уже выгруженным чанком это
        // дает синхронную загрузку чанка в момент остановки сервера => дедлок при
        // выходе из мира. Поэтому снимаем подписку напрямую через карту узлов:
        // наличие PowerNode в позиции гарантирует проводник (узлы создают только они).
        if (be instanceof IEnergyReceiver rec) {
            for (BlockPos port : collectPorts(be.getBlockPos(), extraPositions)) {
                Nodespace.PowerNode node = Nodespace.getNode(level, port);
                if (node != null && node.net != null) {
                    node.net.removeReceiver(rec);
                }
            }
        }
        if (be instanceof IEnergyProvider prov && (!(be instanceof PowerConductor) || be instanceof PowerBuffer)) {
            for (BlockPos port : collectPorts(be.getBlockPos(), extraPositions)) {
                Nodespace.PowerNode node = Nodespace.getNode(level, port);
                if (node != null && node.net != null) node.net.removeProvider(prov);
            }
        }
    }

    private static BlockPos[] collectPorts(BlockPos center, BlockPos[] extra) {
        BlockPos[] ports = new BlockPos[(extra.length + 1) * 6];
        int i = 0;
        for (Direction dir : Direction.values()) {
            ports[i++] = center.relative(dir);
        }
        for (BlockPos p : extra) {
            for (Direction dir : Direction.values()) {
                ports[i++] = p.relative(dir);
            }
        }
        return ports;
    }

    private static boolean subscribeAt(ServerLevel level, IEnergyReceiver rec, BlockPos center, BlockPos[] extra) {
        boolean any = false;
        for (Direction dir : Direction.values()) {
            BlockPos p = center.relative(dir);
            any |= rec.trySubscribe(level, p.getX(), p.getY(), p.getZ(), dir);
        }
        for (BlockPos pos : extra) {
            for (Direction dir : Direction.values()) {
                BlockPos p = pos.relative(dir);
                any |= rec.trySubscribe(level, p.getX(), p.getY(), p.getZ(), dir);
            }
        }
        return any;
    }

    private static boolean provideAt(ServerLevel level, IEnergyProvider prov, BlockPos center, BlockPos[] extra) {
        boolean any = false;
        for (Direction dir : Direction.values()) {
            BlockPos p = center.relative(dir);
            any |= prov.tryProvide(level, p.getX(), p.getY(), p.getZ(), dir);
        }
        for (BlockPos pos : extra) {
            for (Direction dir : Direction.values()) {
                BlockPos p = pos.relative(dir);
                any |= prov.tryProvide(level, p.getX(), p.getY(), p.getZ(), dir);
            }
        }
        return any;
    }
}
