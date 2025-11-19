package com.hbm_m.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.hbm_m.block.entity.WireBlockEntity;
import com.hbm_m.capability.ModCapabilities;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Инкрементальный менеджер сетей проводов.
 */
public class WireNetworkManager {

    private static final WireNetworkManager INSTANCE = new WireNetworkManager();
    public static WireNetworkManager get() { return INSTANCE; }

    private final Map<Level, PerLevel> levels = Collections.synchronizedMap(new WeakHashMap<>());
    private final ReentrantReadWriteLock levelLock = new ReentrantReadWriteLock();

    public void onWireAdded(Level level, BlockPos pos) {
        levelLock.writeLock().lock();
        try {
            PerLevel pl = levels.computeIfAbsent(level, l -> new PerLevel());
            pl.addWire(level, pos);
        } finally {
            levelLock.writeLock().unlock();
        }
    }
    
    public void onWireRemoved(Level level, BlockPos pos) {
        levelLock.writeLock().lock();
        try {
            PerLevel pl = levels.get(level);
            if (pl != null) pl.removeWire(level, pos);
        } finally {
            levelLock.writeLock().unlock();
        }
    }
    
    public void onWireChanged(Level level, BlockPos pos) {
        levelLock.writeLock().lock();
        try {
            PerLevel pl = levels.computeIfAbsent(level, l -> new PerLevel());
            if (level.getBlockState(pos).getBlock() instanceof com.hbm_m.block.WireBlock) {
                pl.addWire(level, pos);
            } else {
                pl.removeWire(level, pos);
            }
        } finally {
            levelLock.writeLock().unlock();
        }
    }

    // ИЗМЕНЕНИЕ: Принимает и возвращает long
    public long requestEnergy(Level level, BlockPos start, long maxRequest, boolean simulate) {
        levelLock.readLock().lock();  // ← Читаем Level
        try {
            PerLevel pl = levels.get(level);
            if (pl == null) return 0L;
            return pl.requestEnergy(level, start, maxRequest, simulate);
        } finally {
            levelLock.readLock().unlock();
        }
    }

    // --- Per-level structures ---
    private static class PerLevel {
        private final Object graphLock = new Object(); // Добавить глобальный лок
        // Граф: для каждой вершины — множество соседних вершин (только провода)
        private final Map<BlockPos, Set<BlockPos>> adj = new HashMap<>();

        // Spanning forest: хранение parent для каждого узла (root -> itself)
        private final Map<BlockPos, BlockPos> parent = new HashMap<>();
        // members[root] -> множество членов компоненты (быстрая навигация)
        private final Map<BlockPos, Set<BlockPos>> members = new HashMap<>();
        // Спецификация ребра в остове — представим как пару упорядоченных позиций
        private final Set<Edge> treeEdges = new HashSet<>();

        // Для каждой корневой компоненты хранится список источников энергии (pos, side)
        private final Map<BlockPos, List<EnergySource>> sources = new HashMap<>();

        // UPDATED: EnergySource теперь хранит приоритет
        private static record EnergySource(BlockPos pos, Direction side, int priority) {}
        private static final int MAX_SOURCES_PER_COMPONENT = 2048;

        // Вспомогательная структура для представления ребра
        private static final class Edge {
            final BlockPos a, b;
            Edge(BlockPos a, BlockPos b) {
                if (a.hashCode() <= b.hashCode()) { this.a = a; this.b = b; }
                else { this.a = b; this.b = a; }
            }
            @Override public boolean equals(Object o) {
                if (!(o instanceof Edge)) return false;
                Edge e = (Edge)o;
                return a.equals(e.a) && b.equals(e.b);
            }
            @Override public int hashCode() { return Objects.hash(a, b); }
        }

        private BlockPos find(BlockPos p) {
            synchronized(graphLock) {
                BlockPos r = parent.get(p);
                if (r == null) return null;
                if (r.equals(p)) return r;
                BlockPos root = find(r);
                parent.put(p, root);
                return root;
            }
        }

        private void makeSet(BlockPos p) {
            parent.put(p, p);
            Set<BlockPos> s = new HashSet<>();
            s.add(p);
            members.put(p, s);
            // ensure adj entry exists
            adj.computeIfAbsent(p, k -> new HashSet<>());
            sources.put(p, new ArrayList<>());
        }

        private void unionRoots(BlockPos r1, BlockPos r2) {
            if (r1.equals(r2)) return;
            Set<BlockPos> s1 = members.get(r1), s2 = members.get(r2);
            if (s1 == null || s2 == null) return;
            if (s1.size() < s2.size()) {
                BlockPos tmp = r1; r1 = r2; r2 = tmp;
                Set<BlockPos> tmpSet = s1; s1 = s2; s2 = tmpSet;
            }
            // attach r2 -> r1
            for (BlockPos p : s2) parent.put(p, r1);
            s1.addAll(s2);
            members.remove(r2);

            // merge sources
            List<EnergySource> l1 = sources.get(r1);
            List<EnergySource> l2 = sources.get(r2);
            if (l1 == null) l1 = new ArrayList<>();
            if (l2 != null) {
                for (EnergySource es : l2) {
                    if (l1.size() < MAX_SOURCES_PER_COMPONENT) l1.add(es);
                }
            }
            sources.put(r1, l1);
            sources.remove(r2);
        }

        void addWire(Level level, BlockPos pos) {
            synchronized(graphLock) {
                if (parent.containsKey(pos)) {
                    // уже существует — возможно изменились соседи / источники — обновим источники для компоненты
                    updateSourcesForComponent(level, pos);
                    return;
                }
    
                // добавляем вершину
                makeSet(pos);
    
                // scan neighbors: создаем рёбра в графе, при необходимости union
                for (Direction d : Direction.values()) {
                    BlockPos npos = pos.relative(d);
                    if (!parent.containsKey(npos) && !adj.containsKey(npos)) {
                        // сосед может быть незарегистрирован, но всё равно есть в мире — учитываем позже при его добавлении
                    }
                    // если у нас уже есть сосед в графе проводов, добавим двунаправленную связь
                    if (adj.containsKey(npos)) {
                        adj.get(npos).add(pos);
                        adj.get(pos).add(npos);
                        // если соседи в разных компонентах — соединяем остовным ребром
                        BlockPos r1 = find(pos);
                        BlockPos r2 = find(npos);
                        if (r1 != null && r2 != null && !r1.equals(r2)) {
                            // пометим ребро как tree edge и объединим корни
                            treeEdges.add(new Edge(pos, npos));
                            unionRoots(r1, r2);
                        }
                    }
                }
    
                // Обновим источники энергии для конечного root
                BlockPos root = find(pos);
                if (root != null) updateSourcesForRoot(level, root);
            }
        }

        void removeWire(Level level, BlockPos pos) {
            synchronized(graphLock) {
                if (!parent.containsKey(pos)) return;

                // 1) Получаем текущий корень/членов старой компоненты
                BlockPos root = find(pos);
                Set<BlockPos> compMembers = members.get(root);
                if (compMembers == null) compMembers = Set.of(pos);

                // СДЕЛАЕМ СНИЖЕНИЕ: снимок множества членов, чтобы не захватывать изменяемую переменную в лямбдах
                final Set<BlockPos> compSnapshot = new HashSet<>(compMembers);

                // 2) Удаляем вершину из графа и собираем список затронутых древовидных рёбер
                List<Edge> removedTreeEdges = new ArrayList<>();
                Set<BlockPos> neighbors = adj.getOrDefault(pos, Collections.emptySet());
                for (BlockPos nb : new ArrayList<>(neighbors)) {
                    // remove adjacency both ways
                    adj.getOrDefault(nb, Collections.emptySet()).remove(pos);
                    adj.getOrDefault(pos, Collections.emptySet()).remove(nb);
                    Edge e = new Edge(pos, nb);
                    if (treeEdges.remove(e)) {
                        removedTreeEdges.add(e);
                    }
                }
                // удаляем сам узел из структур
                adj.remove(pos);
                parent.remove(pos);
                if (members.get(root) != null) members.get(root).remove(pos);

                // 3) Если не было tree-edges — лёгкий случай: удаление не ломает остов
                if (removedTreeEdges.isEmpty()) {
                    if (members.get(root) != null) updateSourcesForRoot(level, root);
                    return;
                }

                // 4) Для каждого удалённого остовного ребра проверяем, сломало ли оно компоненту.
                for (Edge removed : removedTreeEdges) {
                    BlockPos a = removed.a;
                    BlockPos b = removed.b;
                    if (a.equals(pos) || b.equals(pos)) {
                        continue; // (pos уже удален, это ребро обрабатывать не нужно)
                    }

                    // Проверяем, достижимы ли 'a' и 'b' (которые были соединены) в графе БЕЗ удаленного ребра
                    boolean stillConnected = isReachableExcludingEdge(a, b, removed);
                    if (stillConnected) {
                        // Они все еще соединены другим путем (не-остовным ребром)
                        // Нам нужно найти этот путь и добавить его в остов

                        // (ПРИМЕЧАНИЕ: для 100% корректности здесь нужен сложный поиск заменяющего ребра.
                        // Но для простоты мы можем просто перестроить компоненту,
                        // если isReachableExcludingEdge вернет false, как сделано ниже)
                        continue;

                    } else {
                        // ребро действительно разрезало компоненту -> нужно разделить старую component на несколько
                        // Соберём оставшиеся вершины старой компоненты (используем снимок compSnapshot)
                        Set<BlockPos> leftover = new HashSet<>(compSnapshot);
                        leftover.remove(pos); // уже удалён

                        // Удалим всех старых членов из parent/members
                        for (BlockPos p : compSnapshot) {
                            parent.remove(p);
                            members.remove(p);
                        }
                        // Удаляем все treeEdges, которые затрагивали старую компоненту
                        treeEdges.removeIf(e -> compSnapshot.contains(e.a) || compSnapshot.contains(e.b));

                        // Rebuild components from leftover nodes by BFS on adj graph (adj currently содержит связи без pos)
                        rebuildComponents(level, leftover);
                        // обработали разделение — выходим из цикла, т.к. rebuildComponents сделала всю работу
                        return;
                    }
                }

                // Если мы дошли сюда, значит удаление pos не разрезало граф (или pos был один)
                // Нам все равно нужно перестроить остов и источники для оставшихся членов
                Set<BlockPos> leftover = new HashSet<>(compSnapshot);
                leftover.remove(pos);

                if (!leftover.isEmpty()) {
                    for (BlockPos p : compSnapshot) {
                        parent.remove(p);
                        members.remove(p);
                    }
                    treeEdges.removeIf(e -> compSnapshot.contains(e.a) || compSnapshot.contains(e.b));
                    rebuildComponents(level, leftover);
                }
            }
        }

        // Попытка определения достижимости b из a при исключении конкретного ребра
        private boolean isReachableExcludingEdge(BlockPos start, BlockPos target, Edge excluded) {
            ArrayDeque<BlockPos> q = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            q.add(start);
            visited.add(start);
            while (!q.isEmpty()) {
                BlockPos cur = q.poll();
                if (cur.equals(target)) return true;
                for (BlockPos nb : adj.getOrDefault(cur, Collections.emptySet())) {
                    Edge ecur = new Edge(cur, nb);
                    if (ecur.equals(excluded)) continue;
                    if (visited.add(nb)) q.add(nb);
                }
            }
            return false;
        }

        // Локальная пересборка компонент из множества вершин (используется при разрезании)
        private void rebuildComponents(Level level, Set<BlockPos> nodes) {
            Set<BlockPos> visited = new HashSet<>();
            for (BlockPos start : nodes) {
                if (visited.contains(start)) continue;

                // Используем BFS для поиска всех достижимых узлов в этой новой компоненте
                ArrayDeque<BlockPos> q = new ArrayDeque<>();
                Set<BlockPos> comp = new HashSet<>();
                q.add(start);
                visited.add(start);
                comp.add(start);

                // Выбираем корень (первый узел)
                BlockPos root = start;
                makeSet(root); // Создаем новую компоненту с 'start' как корнем
                members.put(root, comp); // Убедимся, что members инициализирован

                while (!q.isEmpty()) {
                    BlockPos cur = q.poll();
                    for (BlockPos nb : adj.getOrDefault(cur, Collections.emptySet())) {
                        if (!nodes.contains(nb)) continue; // только внутри leftover

                        if (visited.add(nb)) {
                            // nb - новый узел
                            q.add(nb);
                            comp.add(nb);

                            // Логика построения остовного дерева (Spanning Tree) прямо здесь:
                            // Так как мы нашли nb из cur, ребро (cur, nb) - остовное
                            treeEdges.add(new Edge(cur, nb));
                            parent.put(nb, root); // Присоединяем к корню новой компоненты
                        }
                    }
                }
                // Обновляем members для корня
                members.put(root, comp);
                // обновим источники
                updateSourcesForRoot(level, root);
            }
        }

        // (Метод buildSpanningTreeForComponent больше не нужен, т.к. логика встроена в rebuildComponents)

        private void updateSourcesForComponent(Level level, BlockPos anyMember) {
            BlockPos r = find(anyMember);
            if (r != null) updateSourcesForRoot(level, r);
        }

        private void updateSourcesForRoot(Level level, BlockPos root) {
            Set<BlockPos> mem = members.get(root);
            if (mem == null) return;
            List<EnergySource> list = new ArrayList<>();
            for (BlockPos p : mem) {
                for (Direction d : Direction.values()) {
                    BlockPos np = p.relative(d);
                    BlockEntity nbe = level.getBlockEntity(np);
                    if (nbe == null) continue;
                    if (!(nbe instanceof WireBlockEntity)) {

                        // ИЗМЕНЕНИЕ: Проверяем ЛЮБУЮ из двух capability
                        boolean hasEnergyCap = nbe.getCapability(ModCapabilities.LONG_ENERGY, d.getOpposite()).isPresent() ||
                                nbe.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).isPresent();

                        if (hasEnergyCap) {
                            if (list.size() < MAX_SOURCES_PER_COMPONENT) {
                                // DETERMINE PRIORITY: check if neighbor is MachineBatteryBlockEntity
                                int pr = 1; // default NORMAL
                                if (nbe instanceof com.hbm_m.block.entity.machine.MachineBatteryBlockEntity mb) {
                                    try {
                                        pr = mb.priority.ordinal();
                                    } catch (Exception ignored) {}
                                }
                                list.add(new EnergySource(np, d.getOpposite(), pr));
                            }
                        }
                    }
                }
            }
            sources.put(root, list);
        }

        long requestEnergy(Level level, BlockPos start, long need, boolean simulate) {
            synchronized(graphLock) {
                BlockPos root = find(start);
                if (root == null) {
                    return bfsRequest(level, start, need, simulate, 2000);
                }
                
                // Создаем копию с двойной защитой
                List<EnergySource> sources_ = sources.get(root);
                if (sources_ == null || sources_.isEmpty()) return 0L;
                
                // Копируем ИЗ СИНХРОНИЗИРОВАННОГО КОНТЕКСТА
                List<EnergySource> list = new ArrayList<>(sources_);
                list.sort(Comparator.comparingInt((EnergySource es) -> es.priority()).reversed());        
                long taken = 0L;
                
                for (EnergySource es : list) {
                    if (taken >= need) break;
                    BlockEntity be = level.getBlockEntity(es.pos());
                    if (be == null) continue;

                    // --- НОВАЯ ЛОГИКА ПОИСКА CAPABILITY ---
                    ILongEnergyStorage storage = null;

                    // 1. Пытаемся найти нашу long-систему
                    LazyOptional<ILongEnergyStorage> longCap = be.getCapability(ModCapabilities.LONG_ENERGY, es.side());
                    if (longCap.isPresent()) {
                        storage = longCap.resolve().orElse(null);
                    } else {
                        // 2. Если не нашли, ищем старую Forge-систему
                        LazyOptional<IEnergyStorage> forgeCap = be.getCapability(ForgeCapabilities.ENERGY, es.side());
                        if (forgeCap.isPresent()) {
                            // 3. Оборачиваем int-хранилище в long-обертку
                            IEnergyStorage intStorage = forgeCap.resolve().orElse(null);
                            if(intStorage != null) {
                                storage = new ForgeToLongWrapper(intStorage);
                            }
                        }
                    }
                    // --- КОНЕЦ НОВОЙ ЛОГИКИ ---

                    if (storage == null || !storage.canExtract()) continue;

                    try {
                        // ИЗМЕНЕНИЕ: long
                        long got = storage.extractEnergy(need - taken, simulate);
                        if (got > 0) taken += got;
                    } catch (Exception ignored) { }
                }
                return taken;
            }
        }

        // ИЗМЕНЕНИЕ: Принимает и возвращает long
        private long bfsRequest(Level level, BlockPos start, long need, boolean simulate, int maxNodes) {
            ArrayDeque<BlockPos> q = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            List<EnergySource> found = new ArrayList<>();
            q.add(start);
            visited.add(start);
            int nodes = 0;
            while (!q.isEmpty() && nodes < maxNodes && found.size() < MAX_SOURCES_PER_COMPONENT) {
                BlockPos cur = q.poll();
                nodes++;
                for (Direction d : Direction.values()) {
                    BlockPos np = cur.relative(d);
                    if (!visited.add(np)) continue;
                    BlockEntity nbe = level.getBlockEntity(np);
                    if (nbe == null) continue;
                    if (nbe instanceof WireBlockEntity) {
                        q.add(np);
                        continue;
                    }

                    // ИЗМЕНЕНИЕ: Проверяем ЛЮБУЮ из двух capability
                    boolean hasEnergyCap = nbe.getCapability(ModCapabilities.LONG_ENERGY, d.getOpposite()).isPresent() ||
                            nbe.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).isPresent();

                    if (hasEnergyCap) {
                        // determine priority if battery
                        int pr = 1;
                        if (nbe instanceof com.hbm_m.block.entity.machine.MachineBatteryBlockEntity mb) {
                            try { pr = mb.priority.ordinal(); } catch (Exception ignored) {}
                        }
                        found.add(new EnergySource(np, d.getOpposite(), pr));
                    }
                }
            }

            // NEW: сортировка по приоритету перед извлечением
            found.sort(Comparator.comparingInt((EnergySource es) -> es.priority()).reversed());

            long taken = 0L; // ИЗМЕНЕНИЕ: long
            for (EnergySource es : found) {
                if (taken >= need) break;
                BlockEntity be = level.getBlockEntity(es.pos());
                if (be == null) continue;

                // --- НОВАЯ ЛОГИКА ПОИСКА CAPABILITY (такая же, как в requestEnergy) ---
                ILongEnergyStorage storage = null;
                LazyOptional<ILongEnergyStorage> longCap = be.getCapability(ModCapabilities.LONG_ENERGY, es.side());
                if (longCap.isPresent()) {
                    storage = longCap.resolve().orElse(null);
                } else {
                    LazyOptional<IEnergyStorage> forgeCap = be.getCapability(ForgeCapabilities.ENERGY, es.side());
                    if (forgeCap.isPresent()) {
                        IEnergyStorage intStorage = forgeCap.resolve().orElse(null);
                        if(intStorage != null) {
                            storage = new ForgeToLongWrapper(intStorage);
                        }
                    }
                }
                // --- КОНЕЦ НОВОЙ ЛОГИКИ ---

                if (storage == null || !storage.canExtract()) continue;
                try {
                    // ИЗМЕНЕНИЕ: long
                    long got = storage.extractEnergy(need - taken, simulate);
                    if (got > 0) taken += got;
                } catch (Exception ignored) { }
            }
            return taken;
        }
    }
}