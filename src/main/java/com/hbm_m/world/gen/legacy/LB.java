package com.hbm_m.world.gen.legacy;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.main.MainRegistry;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port-Hilfe fuer die aus 1.7.10 uebernommenen Weltgen-Klassen (Schematic-zu-Java-Verliese, Bauteil-Strukturen):
 * ein 1.7.10-Block samt Meta-Tabelle (Meta 0-15 -> 1.20-Blockzustand). Die Tabellen stehen in {@link MB} und
 * {@link VB}; die Zustaende werden beim ersten Gebrauch geparst.
 *
 * <p>{@link #forState} liefert zu einem Weltblock das kanonische LB (fuer die {@code ==}-Vergleiche der Altklassen).</p>
 */
public final class LB {

    public final String name;
    private final String[] states;
    private BlockState[] cache;

    private static final List<LB> ALL = new ArrayList<>();
    private static Map<Block, LB> reverse;

    private LB(String name, String[] states) {
        this.name = name;
        this.states = states;
        synchronized (ALL) {
            ALL.add(this);
        }
    }

    public static LB of(String name, String... states) {
        return new LB(name, states);
    }

    /** Fuer Bloecke ohne Tabelle (z.B. per Weltabfrage gefunden). */
    private static LB wrap(Block block) {
        return new LB(BuiltInRegistries.BLOCK.getKey(block).toString(), new String[] { BuiltInRegistries.BLOCK.getKey(block).toString() });
    }

    private synchronized void parse() {
        if (cache != null) return;
        BlockState[] c = new BlockState[states.length];
        for (int i = 0; i < states.length; i++) {
            try {
                c[i] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), states[i], false).blockState();
            } catch (Exception ex) {
                MainRegistry.LOGGER.warn("[LegacyBlocks] Zustand '{}' fuer {} nicht lesbar", states[i], name);
                c[i] = Blocks.AIR.defaultBlockState();
            }
        }
        cache = c;
    }

    /** Blockzustand fuer das 1.7.10-Metadatum. */
    public BlockState state(int meta) {
        if (cache == null) parse();
        if (cache.length == 1) return cache[0];
        return cache[meta & 15];
    }

    public BlockState state() {
        return state(0);
    }

    public Block block() {
        return state(0).getBlock();
    }

    /** Ermittelt das 1.7.10-Metadatum eines Weltzustands (erster passender Eintrag, sonst 0). */
    public int metaOf(BlockState s) {
        if (cache == null) parse();
        for (int i = 0; i < cache.length; i++) if (cache[i] == s) return i;
        for (int i = 0; i < cache.length; i++) if (cache[i].getBlock() == s.getBlock()) return i;
        return 0;
    }

    /** Kanonisches LB zu einem Weltzustand (Vanilla-Tabelle hat Vorrang). */
    public static LB forState(BlockState s) {
        Map<Block, LB> map = reverse;
        if (map == null) map = buildReverse();
        LB lb = map.get(s.getBlock());
        if (lb == null) {
            synchronized (LB.class) {
                lb = reverse.computeIfAbsent(s.getBlock(), LB::wrap);
            }
        }
        return lb;
    }

    private static synchronized Map<Block, LB> buildReverse() {
        if (reverse != null) return reverse;
        // beide Tabellen laden (Vanilla zuerst)
        Object o = VB.air;
        o = MB.concrete_smooth;
        Map<Block, LB> map = new IdentityHashMap<>();
        List<LB> list;
        synchronized (ALL) {
            list = new ArrayList<>(ALL);
        }
        list.sort((a, b) -> Boolean.compare(a.isMod(), b.isMod()));
        for (LB lb : list) {
            lb.parse();
            for (BlockState s : lb.cache) map.putIfAbsent(s.getBlock(), lb);
        }
        // 1.20 kennt Hoehlen- und Leerenluft - beides war in 1.7.10 "air"
        map.put(Blocks.CAVE_AIR, VB.air);
        map.put(Blocks.VOID_AIR, VB.air);
        reverse = map;
        return map;
    }

    private boolean isMod() {
        return states.length > 0 && states[0].startsWith("hbm_m:");
    }

    // ===================== Material-/Eigenschaftsabfragen der 1.7.10-Block-API =====================

    public LMaterial getMaterial() {
        return LMaterial.of(state(0));
    }

    public boolean isOpaqueCube() {
        return state(0).canOcclude();
    }

    public boolean isNormalCube() {
        BlockState s = state(0);
        return s.canOcclude() && !s.isAir() && s.getFluidState().isEmpty();
    }

    public boolean canPlaceTorchOnTop(LevelAccessor world, int x, int y, int z) {
        if (L.virtual() != null) return L.doesBlockHaveSolidTopSurface(world, x, y, z);
        return Block.canSupportCenter(world, new BlockPos(x, y, z), Direction.UP);
    }

    public boolean isReplaceable(LevelAccessor world, int x, int y, int z) {
        return L.getState(world, x, y, z).canBeReplaced();
    }

    public boolean isAir(LevelAccessor world, int x, int y, int z) {
        return L.getState(world, x, y, z).isAir();
    }

    /** {@code isReplaceableOreGen(world, x, y, z, target)}. */
    public boolean isReplaceableOreGen(LevelAccessor world, int x, int y, int z, LB target) {
        return L.getState(world, x, y, z).getBlock() == target.block();
    }

    public boolean canBlockStay(LevelAccessor world, int x, int y, int z) {
        if (L.virtual() != null) return true; // Port: im Bau-Puffer nicht pruefbar
        return state(0).canSurvive(world, new BlockPos(x, y, z));
    }

    public boolean canPlaceBlockAt(LevelAccessor world, int x, int y, int z) {
        if (L.virtual() != null) return L.getState(world, x, y, z).canBeReplaced(); // Port: Puffer
        BlockPos pos = new BlockPos(x, y, z);
        return world.getBlockState(pos).canBeReplaced() && state(0).canSurvive(world, pos);
    }

    @Override
    public String toString() {
        return "LB[" + name + "]";
    }
}
