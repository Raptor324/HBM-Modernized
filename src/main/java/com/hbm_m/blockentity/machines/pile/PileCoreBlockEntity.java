package com.hbm_m.blockentity.machines.pile;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.PileBlock;
import com.hbm_m.block.machines.pile.PileBlockType;
import com.hbm_m.block.machines.MachinePWRControllerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.LoadedMachineBlockEntity;
import com.hbm_m.item.machine.ItemPileRodMK2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPileCore} (1.7.10): der Kern des Uranmeilers, also des Chicago Pile.
 *
 * <p>Der Meiler ist ein massiver Graphitwuerfel, in den der Spieler mit dem Handbohrer Kanaele
 * treibt. Welche Art Kanal dabei entsteht, haengt allein an der Richtung: senkrecht gibt einen
 * Steuerkanal, laengs der Bauachse einen Brennstoffkanal, quer dazu einen Lueftungskanal. Der Kern
 * merkt sich alle drei Listen und rechnet daraus jeden Tick die Kettenreaktion.</p>
 *
 * <p><b>Die Rechnung in drei Schritten.</b> Zuerst reagiert jeder Brennstoffkanal fuer sich: jeder
 * Stab bekommt den Zufluss des Kanals geteilt durch dessen Laenge, antwortet ueber die gedaempfte
 * Wurzelkurve des Stabs und heizt den Kanal. Dann verteilen sich die Neutronen frei innerhalb einer
 * senkrechten Scheibe. Zuletzt wandern sie von Scheibe zu Scheibe nach links und rechts, wobei
 * jede dazwischenliegende Steuerscheibe den Fluss um ihren Anteil ausgefahrener Staebe daempft -
 * hoechstens jedoch um die Haelfte.</p>
 *
 * <p>Gekuehlt wird ueber die Lueftungskanaele: Luft in einem Kanal senkt die Hitze aller
 * Brennstoffkanaele, die hoechstens einen Block darueber oder darunter liegen. Ohne Luft faellt die
 * Hitze nur um ein Promille je Tick und niemals unter 20 Grad. Ueberschreitet ein Kanal
 * {@link #MAX_HEAT}, geht der ganze Meiler hoch.</p>
 *
 * <p>Geht er hoch, bleibt es nicht bei dem einen Knall: fuenfzehn brennende Graphitbrocken
 * ({@link com.hbm_m.entity.projectile.PileDebrisEntity}) steigen aus der Mitte auf und sprengen
 * dort, wo sie wieder herunterkommen, ein weiteres Loch. Wer einen Meiler durchgehen laesst,
 * verliert nicht nur den Reaktor, sondern das halbe Gelaende.</p>
 */
public class PileCoreBlockEntity extends LoadedMachineBlockEntity {

    /** Original: {@code MAX_HEAT = 800}. */
    public static final int MAX_HEAT = 800;

    /** Wird waehrend der Explosion gesetzt, damit die zerberstenden Bloecke nicht nachziehen. */
    public static boolean meltingDown = false;

    private PileOrientation orientation = PileOrientation.NEITHER;

    private int height;
    private int width;
    private int depth;
    private int left;
    private int right;
    private int up;

    public final List<PileChannel> fuelChannels = new ArrayList<>();
    public final List<PileChannel> ventilationChannels = new ArrayList<>();
    public final List<PileChannel> controlChannels = new ArrayList<>();

    /** Scheiben von links nach rechts, siehe {@link PileSegment}. */
    private PileSegment[] segments = new PileSegment[0];

    private double highestHeat;

    public PileCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_CORE_BE.get(), pos, state);
    }

    // ── Geometrie ───────────────────────────────────────────────────────────

    /** Original: {@code setupSize}. Wird einmal beim Zusammenbau gerufen. */
    public PileCoreBlockEntity setupSize(int up, int down, int left, int right, int depth) {
        this.height = up + 1 + down;
        this.width = left + 1 + right;
        this.depth = depth;
        this.left = left;
        this.right = right;
        this.up = up;
        this.segments = new PileSegment[width];
        return this;
    }

    public void setOrientation(PileOrientation orientation) {
        this.orientation = orientation;
    }

    public PileOrientation getOrientation() { return orientation; }
    public int getHeight() { return height; }
    public int getWidth()  { return width; }
    public int getDepth()  { return depth; }
    public int getUp()     { return up; }
    public double getHighestHeat() { return highestHeat; }

    /** Original: die Kanallaenge ergibt sich aus der Achse - Hoehe, Tiefe oder Breite. */
    public int lengthForType(PileChannelType type) {
        return switch (type) {
            case CONTROL -> height;
            case FUEL -> depth;
            case VENTILATION -> width;
        };
    }

    public List<PileChannel> getChannelList(PileChannelType type) {
        return switch (type) {
            case FUEL -> fuelChannels;
            case VENTILATION -> ventilationChannels;
            case CONTROL -> controlChannels;
        };
    }

    @Nullable public PileChannel getFuelChannel(BlockPos pos)        { return getChannel(pos, fuelChannels); }
    @Nullable public PileChannel getVentilationChannel(BlockPos pos) { return getChannel(pos, ventilationChannels); }
    @Nullable public PileChannel getControlChannel(BlockPos pos)     { return getChannel(pos, controlChannels); }

    @Nullable
    private static PileChannel getChannel(BlockPos pos, List<PileChannel> list) {
        for (PileChannel chan : list) if (chan.isAt(pos)) return chan;
        return null;
    }

    public int getFuelChannelNum(PileChannel chan)        { return fuelChannels.indexOf(chan); }
    public int getVentilationChannelNum(PileChannel chan) { return ventilationChannels.indexOf(chan); }
    public int getControlChannelNum(PileChannel chan)     { return controlChannels.indexOf(chan); }

    // ── Bohren ──────────────────────────────────────────────────────────────

    /**
     * 1:1-Port von {@code drillChannel}: der Handbohrer setzt einen Kanal - oder nimmt ihn wieder
     * heraus, wenn er auf dessen Eingang angesetzt wird.
     */
    public boolean drillChannel(Level level, BlockPos start, Direction dir, @Nullable Player player) {
        PileBlockType startType = typeAt(level, start);
        PileChannelType type = PileChannelType.of(dir, orientation);
        int size = lengthForType(type);
        List<PileChannel> list = getChannelList(type);

        // Auf einem Kanaleingang angesetzt: den Kanal wieder zuschuetten.
        if (startType == PileBlockType.FUEL_IN || startType == PileBlockType.AIR_IN
                || startType == PileBlockType.CONTROL) {

            for (int i = 0; i < list.size(); i++) {
                PileChannel chan = list.get(i);
                if (!chan.isAt(start) || chan.dir != dir) continue;

                if (chan.type == PileChannelType.FUEL) chan.ejectAll(level);
                list.remove(i);

                for (int j = 0; j < size; j++) {
                    setType(level, start.relative(dir, j), PileBlockType.DUMMY);
                }

                level.playSound(null, start, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 1F, 0.75F);
                recalculateSegments();
                setChanged();
                return true;
            }
        }

        boolean error = false;
        for (int i = 0; i < size; i++) {
            BlockPos at = start.relative(dir, i);

            if (!level.getBlockState(at).is(ModBlocks.PILE_BLOCK.get())) {
                MachinePWRControllerBlock.sendError(level, at, "Foreign block in reactor", player);
                error = true;
                continue;
            }

            PileBlockType meta = typeAt(level, at);
            if (meta == PileBlockType.EDGE) {
                MachinePWRControllerBlock.sendError(level, at, "Cannot drill along edge", player);
                error = true;
            } else if (meta == PileBlockType.CORE) {
                MachinePWRControllerBlock.sendError(level, at, "Cannot intersect core", player);
                error = true;
            } else if (meta == PileBlockType.CHANNEL) {
                MachinePWRControllerBlock.sendError(level, at, "Cannot intersect channel", player);
                error = true;
            } else if (meta != PileBlockType.DUMMY) {
                MachinePWRControllerBlock.sendError(level, at, "Cannot intersect channel IO", player);
                error = true;
            }
        }

        if (error) return false;

        for (int i = 0; i < size; i++) {
            BlockPos at = start.relative(dir, i);

            if (i == 0) {
                setType(level, at, switch (type) {
                    case FUEL -> PileBlockType.FUEL_IN;
                    case VENTILATION -> PileBlockType.AIR_IN;
                    case CONTROL -> PileBlockType.CONTROL;
                });
            } else if (i == size - 1) {
                setType(level, at, switch (type) {
                    case FUEL -> PileBlockType.FUEL_OUT;
                    case VENTILATION -> PileBlockType.AIR_OUT;
                    case CONTROL -> PileBlockType.CONTROL;
                });
            } else {
                setType(level, at, PileBlockType.CHANNEL);
            }
        }

        list.add(new PileChannel(start, dir, size, type));

        level.playSound(null, start, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 1F, 1.25F);
        recalculateSegments();
        setChanged();
        return true;
    }

    private static PileBlockType typeAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(PileBlock.TYPE) ? state.getValue(PileBlock.TYPE) : PileBlockType.DUMMY;
    }

    private static void setType(Level level, BlockPos pos, PileBlockType type) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(PileBlock.TYPE)) {
            level.setBlock(pos, state.setValue(PileBlock.TYPE, type), 3);
        }
    }

    // ── Tick ────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, PileCoreBlockEntity be) {
        if (level.isClientSide()) return;

        be.runSimulation();
        be.handleVentilation(level, pos);
        be.handleMeltdown(level, pos);

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** 1:1-Port von {@code runSimulation}: Reaktion, Scheibe, Nachbarscheiben. */
    protected void runSimulation() {

        // Reaktion je Kanal
        for (PileChannel chan : fuelChannels) {
            if (chan.length <= 0) continue;

            double producedNeutrons = 0D;
            for (int i = 0; i < chan.rods.length; i++) {
                ItemStack stack = chan.rods[i];
                if (stack == null || stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof ItemPileRodMK2)) continue;

                double neut = ItemPileRodMK2.getReactivity(stack, chan.incomingNeutrons / chan.length);
                producedNeutrons += neut;
                chan.heat += neut * ItemPileRodMK2.getHeatPerNeutron(stack);
                chan.rods[i] = ItemPileRodMK2.react(stack, neut);
            }

            chan.outgoingNeutrons = producedNeutrons;
            chan.incomingNeutrons = 0D;
        }

        // Innerhalb einer Scheibe: alles fliesst in alles
        for (PileSegment seg : segments) {
            if (seg == null || seg.segType != PileChannelType.FUEL) continue;

            double outgoing = 0D;
            for (PileChannel chan : seg.channels) outgoing += chan.outgoingNeutrons;
            for (PileChannel chan : seg.channels) chan.incomingNeutrons += outgoing;
        }

        // Von Scheibe zu Scheibe. Die Raender bleiben aussen vor, dort kann kein Kanal liegen.
        for (int i = 1; i < segments.length - 1; i++) {
            PileSegment seg = segments[i];
            if (seg == null || seg.segType != PileChannelType.FUEL) continue;

            double outgoing = 0D;
            for (PileChannel chan : seg.channels) outgoing += chan.outgoingNeutrons;

            double mult = 1D;
            for (int j = i - 1; j >= 1; j--) { // nach links
                PileSegment neighbor = segments[j];
                if (neighbor == null) continue;
                mult *= neighbor.getNeutronMult(this);
                if (neighbor.segType == PileChannelType.FUEL) {
                    for (PileChannel chan : neighbor.channels) chan.incomingNeutrons += outgoing * mult;
                }
            }

            mult = 1D;
            for (int j = i + 1; j < segments.length - 1; j++) { // nach rechts
                PileSegment neighbor = segments[j];
                if (neighbor == null) continue;
                mult *= neighbor.getNeutronMult(this);
                if (neighbor.segType == PileChannelType.FUEL) {
                    for (PileChannel chan : neighbor.channels) chan.incomingNeutrons += outgoing * mult;
                }
            }
        }
    }

    /** 1:1-Port von {@code handleVentilation}. */
    protected void handleVentilation(Level level, BlockPos pos) {

        for (PileChannel chan : ventilationChannels) {
            if (chan.air <= 0) continue;

            double airCap = (double) chan.air / (double) PileChannel.MAX_AIR;

            // Ein voller Kanal kuehlt alles auf gleicher Hoehe um fuenf Prozent je Tick.
            for (PileChannel fuel : fuelChannels) {
                if (Math.abs(fuel.entry.getY() - chan.entry.getY()) <= 1) {
                    fuel.heat *= (1D - airCap * 0.05D);
                }
            }

            int toUse = (int) Math.ceil(airCap * 5D);
            chan.air -= toUse;
            if (chan.air < 0) chan.air = 0;

            if (level.getGameTime() % 3 != 0) continue;

            // Die Dampfschwade aus dem Auslass - je voller der Kanal, desto breiter.
            double x = chan.entry.getX() + 0.5D + chan.dir.getStepX() * (width - 0.375D);
            double y = chan.entry.getY() + 0.5D;
            double z = chan.entry.getZ() + 0.5D + chan.dir.getStepZ() * (width - 0.375D);

            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                data.putString("type", "tower");
                data.putFloat("lift", 1F);
                data.putFloat("base", (0.125F + level.getRandom().nextFloat() * 0.125F) * (float) airCap);
                data.putFloat("max", 1F * (float) airCap);
                data.putFloat("strafe", 0.0025F);
                data.putBoolean("noWind", true);
                data.putInt("life", 20 + level.getRandom().nextInt(30));
                data.putInt("color", 0xa0a0a0);

                com.hbm_m.particle.helper.IParticleCreator.sendPacket(serverLevel, x, y, z, 150, data);
            }
        }

        // Ohne Luft faellt die Hitze nur langsam und niemals unter Raumtemperatur.
        for (PileChannel chan : fuelChannels) {
            chan.heat *= 0.999D;
            if (chan.heat < 20D) chan.heat = 20D;
        }
    }

    /** 1:1-Port von {@code handleMeltdown}. */
    protected void handleMeltdown(Level level, BlockPos pos) {

        highestHeat = 0D;
        for (PileChannel chan : fuelChannels) {
            if (chan.heat > highestHeat) highestHeat = chan.heat;
        }

        if (highestHeat <= MAX_HEAT || fuelChannels.isEmpty()) return;

        destroy(level, pos);

        // Der Knall sitzt in der Mitte aller Brennstoffkanaele, nicht auf dem Kern.
        double avgX = 0D;
        double avgZ = 0D;
        for (PileChannel chan : fuelChannels) {
            avgX += chan.entry.getX() + 0.5D + chan.dir.getStepX() * (chan.length - 1) / 2D;
            avgZ += chan.entry.getZ() + 0.5D + chan.dir.getStepZ() * (chan.length - 1) / 2D;
        }
        avgX /= fuelChannels.size();
        avgZ /= fuelChannels.size();

        meltingDown = true;
        level.explode(null, avgX, pos.getY() + up, avgZ, 15F, true, Level.ExplosionInteraction.BLOCK);
        meltingDown = false;

        // Original: fuenfzehn brennende Graphitbrocken steigen senkrecht auf und kommen irgendwo
        // wieder herunter - jeder von ihnen mit einer eigenen kleinen Explosion.
        for (int i = 0; i < 15; i++) {
            double mY = level.getRandom().nextDouble() * 0.5D + 1D;
            level.addFreshEntity(com.hbm_m.entity.projectile.PileDebrisEntity.create(
                    level, avgX, pos.getY() + up + 1, avgZ, 0D, mY, 0D));
        }
    }

    /** Original: {@code destroy} - der Kern faellt in einen gewoehnlichen Graphitziegel zurueck. */
    public void destroy(Level level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.PILE_BRICK.get().defaultBlockState(), 3);
    }

    // ── Scheiben ────────────────────────────────────────────────────────────

    /** 1:1-Port von {@code recalculateSegments}. */
    protected void recalculateSegments() {
        segments = new PileSegment[Math.max(0, width)];

        assignSegments(fuelChannels, PileChannelType.FUEL);
        assignSegments(controlChannels, PileChannelType.CONTROL);
    }

    private void assignSegments(List<PileChannel> list, PileChannelType type) {
        for (PileChannel chan : list) {
            int index = getChannelVerticalIndex(chan);
            if (index < 0 || index >= segments.length) continue;

            if (segments[index] == null) {
                segments[index] = new PileSegment(type).addChan(chan);
            } else if (segments[index].segType == type) {
                segments[index].addChan(chan);
            }
        }
    }

    /**
     * 1:1-Port von {@code getChannelVerticalIndex}: in welche Scheibe - von links nach rechts vor
     * dem Meiler stehend - ein Kanal faellt.
     */
    protected int getChannelVerticalIndex(PileChannel chan) {
        Direction right = chan.dir.getClockWise();
        int deltaX = (chan.entry.getX() - worldPosition.getX()) * right.getStepX();
        int deltaZ = (chan.entry.getZ() - worldPosition.getZ()) * right.getStepZ();
        int abs = deltaX == 0 ? deltaZ : deltaX;
        return abs + left;
    }

    // ── Speichern ───────────────────────────────────────────────────────────

    @Override
    public void setRemoved() {
        // Original: {@code invalidate} wirft alle Brennstaebe aus.
        if (level != null && !level.isClientSide()) {
            for (PileChannel chan : fuelChannels) chan.ejectAll(level);
        }
        super.setRemoved();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("height", height);
        tag.putInt("width", width);
        tag.putInt("depth", depth);
        tag.putInt("left", left);
        tag.putInt("right", right);
        tag.putInt("up", up);
        tag.putInt("orientation", orientation.ordinal());
        tag.putDouble("highestHeat", highestHeat);

        tag.putByte("fc", (byte) fuelChannels.size());
        tag.putByte("vc", (byte) ventilationChannels.size());
        tag.putByte("cc", (byte) controlChannels.size());

        for (int i = 0; i < fuelChannels.size(); i++)         fuelChannels.get(i).save(tag, "f" + i);
        for (int i = 0; i < ventilationChannels.size(); i++)  ventilationChannels.get(i).save(tag, "v" + i);
        for (int i = 0; i < controlChannels.size(); i++)      controlChannels.get(i).save(tag, "c" + i);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        height = tag.getInt("height");
        width = tag.getInt("width");
        depth = tag.getInt("depth");
        left = tag.getInt("left");
        right = tag.getInt("right");
        up = tag.getInt("up");
        highestHeat = tag.getDouble("highestHeat");

        PileOrientation[] values = PileOrientation.values();
        int ordinal = tag.getInt("orientation");
        orientation = ordinal >= 0 && ordinal < values.length ? values[ordinal] : PileOrientation.NEITHER;

        segments = new PileSegment[Math.max(0, width)];

        fuelChannels.clear();
        ventilationChannels.clear();
        controlChannels.clear();

        int fuelCount = tag.getByte("fc") & 0xFF;
        int ventCount = tag.getByte("vc") & 0xFF;
        int contCount = tag.getByte("cc") & 0xFF;

        for (int i = 0; i < fuelCount; i++) fuelChannels.add(PileChannel.load(tag, "f" + i, this));
        for (int i = 0; i < ventCount; i++) ventilationChannels.add(PileChannel.load(tag, "v" + i, this));
        for (int i = 0; i < contCount; i++) controlChannels.add(PileChannel.load(tag, "c" + i, this));

        recalculateSegments();
    }
}
