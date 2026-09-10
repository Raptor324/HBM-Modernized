package com.hbm_m.blockentity.machines.dfc;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.DFCCoreMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.entity.effect.EntityCloudFleija;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.item.machine.ItemAMSCatalyst;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityCore} (1.7.10): der Kern des Dunklen Fusionsreaktors.
 *
 * <p>Der Kern erzeugt selbst nichts. Er ist die Stelle, an der die Emitter ihre Energie einspeisen
 * ({@link #burn(long)}), und er gibt ein Vielfaches davon zurueck - das die Emitter dann an die
 * Empfaenger weiterreichen. Was er dabei verlangt, ist streng:</p>
 *
 * <ul>
 *   <li>Ein <b>AMS-Kern</b> im mittleren Platz. Er allein bestimmt den Grundfaktor: Singularitaet
 *       500, Wurmloch 650, Auge der Harmonie 800, das Ding 2500.</li>
 *   <li><b>Zwei Katalysatoren</b> aussen. Sie geben der Reaktion ihre Farbe - und ohne Farbe
 *       laeuft gar nichts.</li>
 *   <li><b>Zwei Brennstoffe</b> in den Tanks, beide mit einem Wirkungsgrad ueber null. Deuterium
 *       und Tritium sind die uebliche Wahl; Balefire und Astatschrabidium sind die besten.</li>
 * </ul>
 *
 * <p><b>Das Feld.</b> Jeder Tick, in dem Hitze anliegt und das Feld sie <b>nicht</b> uebersteigt,
 * sprengt den Kern - und zwar mit einem Radius, der mit Fuellstand und Hitze waechst. Das Feld
 * kommt einzig von den {@link DFCStabilizerBlockEntity Stabilisatoren} und faellt jeden Tick um
 * eins; sie muessen also dauerhaft nachliefern. Die Hitze wird am Tickende auf null gesetzt, ein
 * Kern ist also immer nur einen Tick lang gefaehrdet.</p>
 *
 * <p>Solange Hitze anliegt, ist der Kern toedlich: alles im Umkreis von zehn Bloecken mit freier
 * Sicht nimmt schweren Schaden und faengt Feuer, alles in drei Bloecken Naehe stirbt sofort.</p>
 *
 * <p><b>Die Sperre.</b> Zwei Kerne, die gleichzeitig hochgehen, wuerden den Server zerlegen -
 * darum merkt sich der Reaktor jede Atomexplosion und laesst innerhalb von 300 Bloecken keine
 * zweite zu. Ein Kern, dem das widerfaehrt, explodiert nicht, sondern <b>schmilzt durch</b>: er
 * verstrahlt seine Umgebung, richtet ueber die groessere Reichweite Schaden an und versucht es im
 * naechsten Tick erneut.</p>
 *
 * <p>Solange Hitze anliegt, traegt sich der Kern alle hundert Ticks in die
 * {@link com.hbm_m.satellite.RayScanEvents Ereignisliste} ein - er ist aus dem Orbit als
 * Teilchenquelle zu sehen.</p>
 */
public class DFCCoreBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    /** 0 und 2 Katalysatoren, 1 der AMS-Kern. */
    public static final int SLOT_CATALYST_A = 0;
    public static final int SLOT_AMS_CORE = 1;
    public static final int SLOT_CATALYST_B = 2;
    public static final int INVENTORY_SIZE = 3;

    /** Original: beide Tanks fassen 128.000 mB. */
    private static final int TANK_CAPACITY = 128_000;

    private final FluidTank[] tanks = new FluidTank[2];

    /** Die Feldstaerke, die die Stabilisatoren aufrechterhalten. Faellt jeden Tick um eins. */
    private int field;
    /** Die Hitze dieses Ticks - wird am Ende jedes Ticks zurueckgesetzt. */
    private int heat;
    /** Die Mischfarbe beider Katalysatoren; 0 heisst "kein Betrieb moeglich". */
    private int color;
    private boolean meltdownTick = false;

    /**
     * 1:1-Port von {@code EntityNukeExplosionMK3.at}: jede Atomexplosion traegt sich hier ein und
     * sperrt fuer eine Weile alle weiteren im Umkreis von {@value #EXPLOSION_LOCK_RANGE} Bloecken.
     */
    private static final java.util.Map<ExplosionMark, Long> RECENT_EXPLOSIONS = new java.util.HashMap<>();
    /** Original: {@code vec.lengthVector() < 300}. */
    private static final double EXPLOSION_LOCK_RANGE = 300D;
    /** Wie lange ein Eintrag gilt - im Original setzt {@code EntityNukeExplosionMK3} den Ablauf. */
    private static final long EXPLOSION_LOCK_TICKS = 200L;

    /** Ort und Dimension einer Atomexplosion, wie {@code EntityNukeExplosionMK3.ATEntry}. */
    private record ExplosionMark(net.minecraft.resources.ResourceKey<Level> dim, double x, double y, double z) {}
    private int consumption;
    private int prevConsumption;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> field;
                case 1 -> heat;
                case 2 -> color;
                case 3 -> prevConsumption;
                case 4 -> tanks[0].getFill();
                case 5 -> tanks[1].getFill();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public DFCCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DFC_CORE_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
        tanks[0] = new FluidTank(ModFluids.DEUTERIUM.getSource(), TANK_CAPACITY);
        tanks[1] = new FluidTank(ModFluids.TRITIUM.getSource(), TANK_CAPACITY);
    }

    public int getField()  { return field; }
    public int getHeat()   { return heat; }
    public int getColor()  { return color; }
    public FluidTank[] getTanks() { return tanks; }
    public ContainerData getContainerData() { return data; }

    /** Original: die Stabilisatoren heben das Feld auf ihren Wert an, senken es aber nie. */
    public void raiseField(int watts) {
        this.field = Math.max(this.field, watts);
    }

    // ── Tick ────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, DFCCoreBlockEntity be) {
        if (level.isClientSide()) return;

        be.prevConsumption = be.consumption;
        be.consumption = 0;
        be.meltdownTick = false;

        // Original: Hitze ohne ausreichendes Feld sprengt den Kern.
        if (be.heat > 0 && be.heat >= be.field) {
            be.explode(level, pos);
        }

        // Die Farbe entsteht aus beiden Katalysatoren; fehlt einer, ist sie null.
        ItemStack catA = be.getInventory().getStackInSlot(SLOT_CATALYST_A);
        ItemStack catB = be.getInventory().getStackInSlot(SLOT_CATALYST_B);

        if (catA.getItem() instanceof ItemAMSCatalyst && catB.getItem() instanceof ItemAMSCatalyst) {
            be.color = averageColor(ItemAMSCatalyst.colorOf(catA), ItemAMSCatalyst.colorOf(catB));
        } else {
            be.color = 0;
        }

        if (be.heat > 0) {
            be.harmNearby(level, pos);
            // Original: radiation() laeuft, solange Hitze anliegt.
            com.hbm_m.radiation.ChunkRadiationManager.incrementRad(
                    level, pos.getX(), pos.getY(), pos.getZ(), be.meltdownTick ? 100F : 1F);

            if (level.getGameTime() % 100 == 0) {
                com.hbm_m.satellite.RayScanEvents.reportEvent(level, pos, com.hbm_m.satellite.RayScanEvents.INFO_PARTICLE, 200);
            }
        }

        be.heat = 0;
        if (be.field > 0) be.field--;

        be.setChanged();
        be.sendUpdateToClient();
    }

    /**
     * 1:1-Port des Explosionsteils: der Radius waechst mit Fuellstand und Hitze und liegt immer
     * zwischen 50 und 1000. Steht in der Naehe schon eine Atomexplosion, wird daraus stattdessen
     * ein Durchschmelzen.
     */
    private void explode(Level level, BlockPos pos) {
        int fill = tanks[0].getFill() + tanks[1].getFill();
        int max = tanks[0].getMaxFill() + tanks[1].getMaxFill();
        int mod = heat * 10;

        int size = Math.max(Math.min(fill * mod / max, 1000), 50);

        if (!canExplode(level, pos)) {
            // Original: meltdownTick = true; incrementRad(..., 100)
            meltdownTick = true;
            com.hbm_m.radiation.ChunkRadiationManager.incrementRad(
                    level, pos.getX(), pos.getY(), pos.getZ(), 100F);
            return;
        }

        markExplosion(level, pos);

        EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, size);
        level.addFreshEntity(ex);

        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.BLOCKS, 100000.0F, 1.0F);

        // Original: EntityCloudFleijaRainbow mit dem Radius als Groesse.
        EntityCloudFleija cloud = new EntityCloudFleija(
                com.hbm_m.entity.ModEntities.CLOUD_FLEIJA.get(), level, size);
        cloud.setPos(pos.getX(), pos.getY(), pos.getZ());
        level.addFreshEntity(cloud);
    }

    /** 1:1-Port der Schleife ueber {@code EntityNukeExplosionMK3.at}. */
    private static boolean canExplode(Level level, BlockPos pos) {
        long now = level.getGameTime();
        RECENT_EXPLOSIONS.entrySet().removeIf(e -> e.getValue() < now);

        for (ExplosionMark mark : RECENT_EXPLOSIONS.keySet()) {
            if (mark.dim() != level.dimension()) continue;

            double dx = pos.getX() + 0.5 - mark.x();
            double dy = pos.getY() + 0.5 - mark.y();
            double dz = pos.getZ() + 0.5 - mark.z();

            if (Math.sqrt(dx * dx + dy * dy + dz * dz) < EXPLOSION_LOCK_RANGE) return false;
        }
        return true;
    }

    private static void markExplosion(Level level, BlockPos pos) {
        RECENT_EXPLOSIONS.put(
                new ExplosionMark(level.dimension(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                level.getGameTime() + EXPLOSION_LOCK_TICKS);
    }

    /** 1:1-Port von {@code radiation}: zwei Ringe, aussen versengend, innen toedlich. */
    private void harmNearby(Level level, BlockPos pos) {
        double range = meltdownTick ? 50D : 10D;
        double scale = meltdownTick ? 5D : 3D;

        AABB outer = new AABB(pos).inflate(range);
        for (Entity e : level.getEntitiesOfClass(Entity.class, outer)) {
            // Original prueft hier auf Hazmat und freie Sicht; beides bleibt hier weg.
            e.hurt(level.damageSources().onFire(), 1000F);
            e.setSecondsOnFire(3);
        }

        AABB inner = new AABB(pos).inflate(scale);
        for (Entity e : level.getEntitiesOfClass(Entity.class, inner)) {
            e.hurt(level.damageSources().genericKill(), 10000F);
        }
    }

    // ── Verbrennung ─────────────────────────────────────────────────────────

    /**
     * 1:1-Port von {@code isReady}: alles muss stimmen, sonst passiert gar nichts.
     */
    public boolean isReady() {
        if (getCoreStrength() == 0) return false;
        if (color == 0) return false;
        if (getFuelEfficiency(tanks[0].getTankType()) <= 0F) return false;
        if (getFuelEfficiency(tanks[1].getTankType()) <= 0F) return false;
        return true;
    }

    /**
     * 1:1-Port von {@code burn}: die Emitter reichen ihre Energie herein und bekommen das
     * Vielfache zurueck.
     *
     * <p>Original-Merksatz aus dem Quelltext: <i>100 Emitter-Watt = 10.000 Joule = 1 Hitze =
     * 10 mB verbrannt</i>. Kann der Kern nicht arbeiten, kommt die Energie unveraendert zurueck -
     * der Emitter hat sie dann umsonst verschossen.</p>
     */
    public long burn(long joules) {
        if (!isReady()) return joules;

        int demand = (int) Math.ceil(joules / 1000D);
        if (tanks[0].getFill() < demand || tanks[1].getFill() < demand) return joules;

        consumption += demand;
        heat += (int) Math.ceil(joules / 10000D);

        tanks[0].setFill(tanks[0].getFill() - demand);
        tanks[1].setFill(tanks[1].getFill() - demand);

        return (long) (joules * getCoreStrength()
                * getFuelEfficiency(tanks[0].getTankType())
                * getFuelEfficiency(tanks[1].getTankType()));
    }

    /** 1:1-Port von {@code getFuelEfficiency}. Was hier nicht steht, taugt nicht als Brennstoff. */
    public static float getFuelEfficiency(Fluid type) {
        if (type == ModFluids.HYDROGEN.getSource())  return 1.0F;
        if (type == ModFluids.DEUTERIUM.getSource()) return 1.5F;
        if (type == ModFluids.TRITIUM.getSource())   return 1.7F;
        if (type == ModFluids.OXYGEN.getSource())    return 1.2F;
        if (type == ModFluids.PEROXIDE.getSource())  return 1.4F;
        if (type == ModFluids.XENON.getSource())     return 1.5F;
        if (type == ModFluids.SAS3.getSource())      return 2.0F;
        if (type == ModFluids.BALEFIRE.getSource())  return 2.5F;
        if (type == ModFluids.AMAT.getSource())      return 2.2F;
        if (type == ModFluids.ASCHRAB.getSource())   return 2.7F;
        return 0F;
    }

    /** 1:1-Port von {@code getCore}: der eingesetzte AMS-Kern gibt den Grundfaktor. */
    public int getCoreStrength() {
        ItemStack stack = getInventory().getStackInSlot(SLOT_AMS_CORE);
        if (stack.isEmpty()) return 0;

        if (stack.is(ModItems.AMS_CORE_SING.get()))         return 500;
        if (stack.is(ModItems.AMS_CORE_WORMHOLE.get()))     return 650;
        if (stack.is(ModItems.AMS_CORE_EYEOFHARMONY.get())) return 800;
        if (stack.is(ModItems.AMS_CORE_THINGY.get()))       return 2500;
        return 0;
    }

    /** 1:1-Port von {@code calcAvgHex}: der Mittelwert je Farbkanal. */
    private static int averageColor(int a, int b) {
        int r = (((a & 0xFF0000) >> 16) + ((b & 0xFF0000) >> 16)) / 2;
        int g = (((a & 0x00FF00) >> 8) + ((b & 0x00FF00) >> 8)) / 2;
        int bl = ((a & 0xFF) + (b & 0xFF)) / 2;
        return r << 16 | g << 8 | bl;
    }

    // ── Schnittstellen ──────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()       { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_AMS_CORE) {
            return stack.is(ModItems.AMS_CORE_SING.get()) || stack.is(ModItems.AMS_CORE_WORMHOLE.get())
                    || stack.is(ModItems.AMS_CORE_EYEOFHARMONY.get()) || stack.is(ModItems.AMS_CORE_THINGY.get());
        }
        return stack.getItem() instanceof ItemAMSCatalyst;
    }

    @Override
    public AABB getRenderBoundingBox() {
        // Original: der Kern zeichnet sich vier Bloecke in jede Richtung.
        return new AABB(worldPosition).inflate(5D);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "fuel1");
        tanks[1].writeToNBT(tag, "fuel2");
        tag.putInt("field", field);
        tag.putInt("color", color);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "fuel1");
        tanks[1].readFromNBT(tag, "fuel2");
        field = tag.getInt("field");
        color = tag.getInt("color");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.dfc_core");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DFCCoreMenu(id, inv, this);
    }
}
