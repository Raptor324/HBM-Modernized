package com.hbm_m.blockentity.machines.dfc;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.DFCStabilizerMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.machine.ItemAMSLens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityCoreStabilizer} (1.7.10): der Feldstabilisator des Dunklen
 * Fusionsreaktors.
 *
 * <p>Er schiesst einen Strahl in seine Blickrichtung und haelt damit das Feld des Kerns aufrecht -
 * ohne ihn sprengt sich der Kern beim ersten Tick, in dem Hitze anliegt. Der Strahl reicht
 * {@value #RANGE} Bloecke und wird von jedem festen Block aufgehalten.</p>
 *
 * <p><b>Der Preis ist steil.</b> Der Verbrauch ist {@code Watt hoch vier}: ein Watt kostet eine
 * Energieeinheit je Tick, hundert Watt kosten hundert Millionen. Wer den Kern schwer belastet,
 * zahlt also nicht doppelt, sondern um Groessenordnungen mehr. Dazu verschleisst die
 * {@link ItemAMSLens Linse} um die eingestellten Watt je Tick.</p>
 *
 * <p>Mehrere Stabilisatoren summieren sich nicht - der Kern nimmt jeweils den <b>hoechsten</b>
 * gelieferten Wert. Sie dienen der Ausfallsicherheit, nicht der Steigerung.</p>
 *
 * <p>Eingestellt wird in der Oberflaeche oder ueber Redstone-over-Radio: {@code setwatts} setzt
 * die Wattzahl, {@code watts}, {@code beam} und {@code power} lassen sich auslesen. Damit laesst
 * sich das Feld an die tatsaechliche Hitze des Kerns anpassen, statt dauerhaft auf Maximum zu
 * fahren - bei einem Verbrauch in der vierten Potenz lohnt das erheblich.</p>
 */
public class DFCStabilizerBlockEntity extends BaseMachineBlockEntity
        implements com.hbm_m.api.redstoneoverradio.IRORValueProvider,
                   com.hbm_m.api.redstoneoverradio.IRORInteractive {

    public static final int SLOT_LENS = 0;
    public static final int INVENTORY_SIZE = 1;

    /** Original: {@code maxPower = 2_500_000_000L}. */
    public static final long MAX_POWER = 2_500_000_000L;
    /** Original: {@code range = 15}. */
    public static final int RANGE = 15;

    private int watts = 1;
    /** Wie weit der Strahl zuletzt kam - nur zur Anzeige. */
    private int beam;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> watts;
                case 1 -> beam;
                // Der Energiestand sprengt den int-Bereich, darum als Promille.
                case 2 -> (int) (getMaxEnergyStored() > 0
                        ? getEnergyStored() * 1000L / getMaxEnergyStored() : 0L);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) setWatts(value);
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public DFCStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DFC_STABILIZER_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER, 0L);
    }

    public int getWatts() { return watts; }
    public int getBeam()  { return beam; }
    public ContainerData getContainerData() { return data; }

    /** Original: {@code MathHelper.clamp_int(watts, 1, 100)}. */
    public void setWatts(int watts) {
        this.watts = Math.max(1, Math.min(100, watts));
        setChanged();
    }

    /** Original: {@code demand = Math.pow(watts, 4)} - der Verbrauch steigt in der vierten Potenz. */
    public long getDemand() {
        return (long) Math.pow(watts, 4);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DFCStabilizerBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.watts = Math.max(1, Math.min(100, be.watts));
        be.beam = 0;

        long demand = be.getDemand();
        ItemStack lens = be.getInventory().getStackInSlot(SLOT_LENS);

        if (be.getEnergyStored() >= demand && lens.is(ModItems.AMS_LENS.get()) && ItemAMSLens.isUsable(lens)) {
            be.fireBeam(level, pos, lens, demand);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** 1:1-Port der Strahlschleife: der erste Kern auf der Achse bekommt das Feld. */
    private void fireBeam(Level level, BlockPos pos, ItemStack lens, long demand) {
        Direction dir = getBeamDirection();

        for (int i = 1; i <= RANGE; i++) {
            BlockPos at = pos.relative(dir, i);
            BlockEntity be = level.getBlockEntity(at);

            if (be instanceof DFCCoreBlockEntity core) {
                core.raiseField(watts);
                setEnergyStored(getEnergyStored() - demand);
                beam = i;

                // Die Linse verschleisst um die gefahrenen Watt - bei hundert also hundertfach.
                long damage = ItemAMSLens.getLensDamage(lens) + watts;
                if (damage >= ItemAMSLens.maxDamageOf(lens)) {
                    getInventory().setStackInSlot(SLOT_LENS, ItemStack.EMPTY);
                } else {
                    ItemAMSLens.setLensDamage(lens, damage);
                }
                return;
            }

            // Original: alles Feste haelt den Strahl auf.
            if (!level.getBlockState(at).isAir()) return;
        }
    }

    /** Die Blickrichtung des Blocks - im Original die Metadatenzahl. */
    public Direction getBeamDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
                ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
                : Direction.NORTH;
    }

    // ── Redstone-over-Radio ──

    /** 1:1 aus {@code TileEntityCoreStabilizer}. */
    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "watts",
                PREFIX_VALUE + "beam",
                PREFIX_VALUE + "power",
                PREFIX_FUNCTION + "setwatts" + NAME_SEPARATOR + "watts"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "watts").equals(name)) return "" + watts;
        if ((PREFIX_VALUE + "beam").equals(name))  return "" + beam;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + getEnergyStored();
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setwatts").equals(name) && params.length > 0) {
            setWatts(com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 1, 100));
        }
        return null;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_LENS && stack.is(ModItems.AMS_LENS.get());
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(RANGE + 1);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("watts", watts);
        tag.putInt("beam", beam);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        watts = tag.contains("watts") ? tag.getInt("watts") : 1;
        beam = tag.getInt("beam");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.dfc_stabilizer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DFCStabilizerMenu(id, inv, this);
    }
}
