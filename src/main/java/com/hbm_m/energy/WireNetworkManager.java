package com.hbm_m.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import com.hbm_m.block.entity.WireBlockEntity;

import java.util.*;

/**
 * Инкрементальный менеджер сетей проводов.
 * Поддерживает:
 * - граф проводов (adjacency),
 * - остовный лес (spanning tree) для каждой компоненты,
 * - списки источников энергии (pos + side) для каждой компоненты.
 *
 * При удалении остовного ребра пытается найти заменяющее ребро через локальный BFS;
 * если замены нет — пересобирает компоненты локально (только для старой компоненты).
 */
public class WireNetworkManager {

    private static final WireNetworkManager INSTANCE = new WireNetworkManager();
    public static WireNetworkManager get() { return INSTANCE; }

    private final Map<Level, PerLevel> levels = Collections.synchronizedMap(new WeakHashMap<>());

    public void onWireAdded(Level level, BlockPos pos) {
        PerLevel pl = levels.computeIfAbsent(level, l -> new PerLevel());
        pl.addWire(level, pos);
    }

    public void onWireRemoved(Level level, BlockPos pos) {
        PerLevel pl = levels.get(level);
        if (pl != null) pl.removeWire(level, pos);
    }

    public void onWireChanged(Level level, BlockPos pos) {
        // При изменении состояния провода — проверяем, есть ли провод и вызываем add/remove
        if (level.getBlockState(pos).getBlock() instanceof com.hbm_m.block.WireBlock) {
            onWireAdded(level, pos);
        } else {
            onWireRemoved(level, pos);
        }
    }

    public int requestEnergy(Level level, BlockPos start, int maxRequest, boolean simulate) {
        PerLevel pl = levels.get(level);
        if (pl == null) return 0;
        return pl.requestEnergy(level, start, maxRequest, simulate);
    }

    // --- Per-level structures ---
    private static class PerLevel {
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
            BlockPos r = parent.get(p);
            if (r == null) return null;
            if (r.equals(p)) return r;
            BlockPos root = find(r);
            parent.put(p, root);
            return root;
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

        void removeWire(Level level, BlockPos pos) {
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
                    continue;
                }
                boolean stillConnected = isReachableExcludingEdge(a, b, removed);
                if (stillConnected) {
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
                    // обработали разделение — остальные удалённыеTreeEdges уже учтём по мере итерации
                }
            }
            // Наконец — обновим источники для компонента(й), которые остались
            // (rebuildComponents уже вызовет updateSourcesForRoot)
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
                ArrayDeque<BlockPos> q = new ArrayDeque<>();
                Set<BlockPos> comp = new HashSet<>();
                q.add(start);
                visited.add(start);
                comp.add(start);
                while (!q.isEmpty()) {
                    BlockPos cur = q.poll();
                    for (BlockPos nb : adj.getOrDefault(cur, Collections.emptySet())) {
                        if (!nodes.contains(nb)) continue; // только внутри leftover
                        if (visited.add(nb)) {
                            visited.add(nb);
                            q.add(nb);
                            comp.add(nb);
                        }
                    }
                }
                // создаём новый корень
                BlockPos root = comp.iterator().next();
                for (BlockPos p : comp) parent.put(p, root);
                members.put(root, comp);
                // восстановим дерево ребёр (создаём spanning tree простым BFS)
                buildSpanningTreeForComponent(root, comp);
                // обновим источники
                updateSourcesForRoot(level, root);
            }
        }

        // Построение остовного дерева для компоненты (root, members) простым BFS; помечаем treeEdges
        private void buildSpanningTreeForComponent(BlockPos root, Set<BlockPos> comp) {
            ArrayDeque<BlockPos> q = new ArrayDeque<>();
            Set<BlockPos> vis = new HashSet<>();
            q.add(root);
            vis.add(root);
            while (!q.isEmpty()) {
                BlockPos cur = q.poll();
                for (BlockPos nb : adj.getOrDefault(cur, Collections.emptySet())) {
                    if (!comp.contains(nb) || vis.contains(nb)) continue;
                    // добавляем ребро в остов
                    treeEdges.add(new Edge(cur, nb));
                    parent.put(nb, root); // временно указываем на корень — find реализует path compression позже
                    members.get(root).add(nb);
                    q.add(nb);
                    vis.add(nb);
                }
            }
            // корректируем parent для корня
            parent.put(root, root);
        }

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
                        if (nbe.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).isPresent()) {
                            if (list.size() < MAX_SOURCES_PER_COMPONENT) {
                                // DETERMINE PRIORITY: check if neighbor is MachineBatteryBlockEntity
                                int pr = 1; // default NORMAL
                                if (nbe instanceof com.hbm_m.block.entity.MachineBatteryBlockEntity mb) {
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

        int requestEnergy(Level level, BlockPos start, int need, boolean simulate) {
            BlockPos root = find(start);
            if (root == null) {
                // fallback: быстрая локальная BFS-запрос (в редких случаях)
                return bfsRequest(level, start, need, simulate, 2000);
            }
            List<EnergySource> list = sources.get(root);
            if (list == null || list.isEmpty()) return 0;

            // NEW: сортируем по приоритету (HIGH -> NORMAL -> LOW)
            list.sort(Comparator.comparingInt((EnergySource es) -> es.priority()).reversed());

            int taken = 0;
            for (EnergySource es : list) {
                if (taken >= need) break;
                BlockEntity be = level.getBlockEntity(es.pos());
                if (be == null) continue;
                IEnergyStorage storage = be.getCapability(ForgeCapabilities.ENERGY, es.side()).resolve().orElse(null);
                if (storage == null || !storage.canExtract()) continue;
                try {
                    int got = storage.extractEnergy(need - taken, simulate);
                    if (got > 0) taken += got;
                } catch (Exception ignored) { }
            }
            return taken;
        }

        private int bfsRequest(Level level, BlockPos start, int need, boolean simulate, int maxNodes) {
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
                    if (nbe.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).isPresent()) {
                        // determine priority if battery
                        int pr = 1;
                        if (nbe instanceof com.hbm_m.block.entity.MachineBatteryBlockEntity mb) {
                            try { pr = mb.priority.ordinal(); } catch (Exception ignored) {}
                        }
                        found.add(new EnergySource(np, d.getOpposite(), pr));
                    }
                }
            }

            // NEW: сортировка по приоритету перед извлечением
            found.sort(Comparator.comparingInt((EnergySource es) -> es.priority()).reversed());

            int taken = 0;
            for (EnergySource es : found) {
                if (taken >= need) break;
                BlockEntity be = level.getBlockEntity(es.pos());
                if (be == null) continue;
                IEnergyStorage storage = be.getCapability(ForgeCapabilities.ENERGY, es.side()).resolve().orElse(null);
                if (storage == null || !storage.canExtract()) continue;
                try {
                    int got = storage.extractEnergy(need - taken, simulate);
                    if (got > 0) taken += got;
                } catch (Exception ignored) { }
            }
            return taken;
        }
    }
}
