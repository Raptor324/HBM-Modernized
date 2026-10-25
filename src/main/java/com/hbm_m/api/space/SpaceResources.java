package com.hbm_m.api.space;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * Erweiterungspunkt fuer koerpergebundene Ressourcen (NTM Space: {@code CelestialBedrockOre}, {@code BlockOreFluid},
 * Pumpen-{@code validBlocks}). Reine Registrierung - ohne Addon ist alles leer, und HBM verhaelt sich wie bisher.
 *
 * <p>Stand R0: Die Abfragen sind angelegt; die HBM-Aufrufstellen (Bedrock-Erz, Oelbohrer, Pumpjack, Fracking,
 * Bergbaulaser, Erzdichtescanner) werden in R8 verdrahtet. Bereits aktiv: {@link #isExtraPumpGround(Block)} in
 * {@code PumpGroundCheck}.
 *
 * <p>Registrierungen gehoeren in die Common-Setup-Phase des Addons (Bloecke ueber {@link Supplier}, damit die Registry
 * erst bei Bedarf aufgeloest wird).
 */
public final class SpaceResources {

    private SpaceResources() {}

    // ───────────────────────────── Bedrock-Erze je Koerper ─────────────────────────────

    /**
     * Ein Bedrock-Erztyp auf einem Koerper (Original {@code CelestialBedrockOre.registerOre(body, type, weight)}).
     *
     * @param oreType Kennung des Erztyps (Addon-eigen, z.B. {@code space_m:duna_iron})
     * @param weight  Gewicht in der Zufallsauswahl
     */
    public record BedrockOre(ResourceLocation body, ResourceLocation oreType, int weight) { }

    private static final Map<ResourceLocation, List<BedrockOre>> BEDROCK_ORES = new ConcurrentHashMap<>();

    public static void registerBedrockOre(ResourceLocation body, ResourceLocation oreType, int weight) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(oreType, "oreType");
        BEDROCK_ORES.computeIfAbsent(body, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new BedrockOre(body, oreType, weight));
    }

    /** Registrierte Bedrock-Erze eines Koerpers (unveraenderlich, leer = HBM-Standardverhalten). */
    public static List<BedrockOre> bedrockOres(ResourceLocation body) {
        List<BedrockOre> l = BEDROCK_ORES.get(body);
        if (l == null) return List.of();
        synchronized (l) {
            return List.copyOf(l);
        }
    }

    // ───────────────────────────── Fluid-Lagerstaetten (BlockOreFluid) ─────────────────────────────

    /** Original {@code BlockOreFluid.ReserveType}. */
    public enum ReserveType { OIL, GAS, BRINE }

    /**
     * Lagerstaetten-Block (voll) und sein erschoepfter Zustand (Original {@code BlockOreFluid(mat, empty, type)}).
     * {@code empty} darf {@code null} liefern bzw. selbst null sein (Bedrock-Oel: nur Fracking, erschoepft nie).
     */
    public record FluidDeposit(ReserveType type, Supplier<Block> full, @Nullable Supplier<Block> empty) {
        @Nullable
        public Block emptyBlock() {
            return empty == null ? null : empty.get();
        }
    }

    /**
     * Original {@code com.hbm.blocks.generic.BlockOreFluid} (W-R8B-2): ein Addon-Lagerstaettenblock implementiert diese
     * Schnittstelle; Pumpjack, Oelbohrturm, Fracking-Turm und Oeldetektor fragen sie per {@code instanceof} ab
     * ({@link #fluidDepositBlock(Block)}). HBM-eigene Bloecke ({@code ore_oil}, {@code ore_bedrock_oil}) implementieren
     * sie nicht und behalten das HBM-Verhalten.
     */
    public interface FluidDepositBlock {
        ReserveType getReserveType();

        /** Original {@code getPrimaryFluid(meta)} als Fluid-Registry-ID, null = {@code Fluids.NONE}. */
        @Nullable ResourceLocation getPrimaryFluid();

        /** Original {@code getSecondaryFluid(meta)}, null = {@code Fluids.NONE}. */
        @Nullable ResourceLocation getSecondaryFluid();

        /** Original {@code getPrimaryFluidAmount(meta)} (mB je Foerderung, vor Maschinen-Multiplikator). */
        int getPrimaryFluidAmount();

        /** Original {@code getSecondaryFluidAmount(meta)} (darf zufaellig sein). */
        int getSecondaryFluidAmount();

        /** Original {@code drain(world, x, y, z, meta, chanceMultiplier)}: erschoepft den Block mit Wahrscheinlichkeit. */
        void drain(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, double chanceMultiplier);

        /** Original {@code requiresFracking(meta)} (= kein leerer Block, Bedrock-Oel). */
        boolean requiresFracking();

        /** Original {@code getUnlocalizedReserveType()}: Suffix der Oeldetektor-Meldung ("", "_gas", "_brine"). */
        default String getUnlocalizedReserveType() {
            return switch (getReserveType()) {
                case GAS -> "_gas";
                case BRINE -> "_brine";
                default -> "";
            };
        }
    }

    /** Lagerstaette eines Blocks (Addon-{@link FluidDepositBlock}), sonst null (HBM-Bloecke, alles andere). */
    @Nullable
    public static FluidDepositBlock fluidDepositBlock(Block block) {
        return block instanceof FluidDepositBlock d ? d : null;
    }

    /** Original {@code BlockOreFluid.getFullBlock(b) != null}: erschoepfter Addon-Lagerstaettenblock. */
    public static boolean isEmptyFluidDeposit(Block block) {
        return !DEPOSITS.isEmpty() && fullBlockOf(block) != null;
    }

    private static final List<FluidDeposit> DEPOSITS = Collections.synchronizedList(new ArrayList<>());
    /** Koerper -> Typ -> {primaer, sekundaer} (Original {@code getPrimaryFluid/getSecondaryFluid(meta)}). */
    private static final Map<ResourceLocation, Map<ReserveType, ResourceLocation[]>> DEPOSIT_FLUIDS = new ConcurrentHashMap<>();

    /** {@code empty} darf null sein (W-R8B-2 b: Bedrock-Oel, nur Fracking). */
    public static void registerFluidDeposit(ReserveType type, Supplier<Block> full, @Nullable Supplier<Block> empty) {
        DEPOSITS.add(new FluidDeposit(Objects.requireNonNull(type), Objects.requireNonNull(full), empty));
    }

    /** Lagerstaette zu einem vollen Block, sonst null. */
    @Nullable
    public static FluidDeposit depositOf(Block full) {
        synchronized (DEPOSITS) {
            for (FluidDeposit d : DEPOSITS) if (d.full().get() == full) return d;
        }
        return null;
    }

    /** Original {@code BlockOreFluid.getFullBlock(empty)}: voller Block zu einem erschoepften, sonst null. */
    @Nullable
    public static Block fullBlockOf(Block empty) {
        synchronized (DEPOSITS) {
            for (FluidDeposit d : DEPOSITS) if (d.emptyBlock() != null && d.emptyBlock() == empty) return d.full().get();
        }
        return null;
    }

    public static List<FluidDeposit> fluidDeposits() {
        synchronized (DEPOSITS) {
            return List.copyOf(DEPOSITS);
        }
    }

    /** Foerderfluide einer Lagerstaette auf einem Koerper (Fluid-Registry-IDs, {@code secondary} darf null sein). */
    public static void registerDepositFluids(ResourceLocation body, ReserveType type, ResourceLocation primary, @Nullable ResourceLocation secondary) {
        DEPOSIT_FLUIDS.computeIfAbsent(body, k -> Collections.synchronizedMap(new LinkedHashMap<>()))
                .put(type, new ResourceLocation[] { Objects.requireNonNull(primary), secondary });
    }

    /** Primaer-Fluid der Lagerstaette auf dem Koerper; null = HBM-Standard (Erde). */
    @Nullable
    public static ResourceLocation depositPrimaryFluid(ResourceLocation body, ReserveType type) {
        Map<ReserveType, ResourceLocation[]> m = DEPOSIT_FLUIDS.get(body);
        ResourceLocation[] f = m == null ? null : m.get(type);
        return f == null ? null : f[0];
    }

    /** Sekundaer-Fluid der Lagerstaette auf dem Koerper; null = keines bzw. HBM-Standard. */
    @Nullable
    public static ResourceLocation depositSecondaryFluid(ResourceLocation body, ReserveType type) {
        Map<ReserveType, ResourceLocation[]> m = DEPOSIT_FLUIDS.get(body);
        ResourceLocation[] f = m == null ? null : m.get(type);
        return f == null ? null : f[1];
    }

    // ───────────────────────────── Pumpen-Untergrund ─────────────────────────────

    private static final List<Supplier<Block>> PUMP_GROUND = Collections.synchronizedList(new ArrayList<>());
    private static volatile Set<Block> pumpGroundResolved = null;

    /**
     * Zusaetzlicher wasserfuehrender Untergrund fuer Pumpen (Original {@code TileEntityMachinePumpBase.validBlocks}:
     * eve_silt, duna_sands, moon_turf ...). Wirkt in {@code PumpGroundCheck}.
     */
    public static void registerPumpGround(Supplier<Block> block) {
        PUMP_GROUND.add(Objects.requireNonNull(block));
        pumpGroundResolved = null;
    }

    public static boolean isExtraPumpGround(Block block) {
        if (PUMP_GROUND.isEmpty()) return false;
        Set<Block> s = pumpGroundResolved;
        if (s == null) {
            Map<Block, Boolean> m = new HashMap<>();
            synchronized (PUMP_GROUND) {
                for (Supplier<Block> b : PUMP_GROUND) m.put(b.get(), Boolean.TRUE);
            }
            s = Set.copyOf(m.keySet());
            pumpGroundResolved = s;
        }
        return s.contains(block);
    }

    // ───────────────────────────── Ziegelofen-Geschwindigkeit ─────────────────────────────

    private static final Map<Supplier<? extends ItemLike>, Integer> BRICK_FURNACE_SPEED = new ConcurrentHashMap<>();
    private static volatile Map<Item, Integer> brickFurnaceResolved = null;

    /**
     * Zusaetzlicher Eintrag in {@code TileEntityFurnaceBrick.burnSpeed} (Original NTM Space: duna_cobble, dres_rock,
     * ike_regolith, eve_rock, moho_regolith, moon_rock, minmus_regolith, duna_sands, laythe_silt, eve_silt, moon_turf
     * je 2). Wirkt in {@code MachineFurnaceBrickBlockEntity.getBurnSpeed} nach den HBM-eigenen Eintraegen.
     */
    public static void registerBrickFurnaceSpeed(Supplier<? extends ItemLike> item, int speed) {
        BRICK_FURNACE_SPEED.put(Objects.requireNonNull(item), speed);
        brickFurnaceResolved = null;
    }

    /** Registrierte Ziegelofen-Geschwindigkeit, 0 = keine (HBM-Standard 1). */
    public static int brickFurnaceSpeed(Item item) {
        if (BRICK_FURNACE_SPEED.isEmpty()) return 0;
        Map<Item, Integer> m = brickFurnaceResolved;
        if (m == null) {
            Map<Item, Integer> r = new HashMap<>();
            BRICK_FURNACE_SPEED.forEach((k, v) -> r.put(k.get().asItem(), v));
            m = Map.copyOf(r);
            brickFurnaceResolved = m;
        }
        return m.getOrDefault(item, 0);
    }
}
