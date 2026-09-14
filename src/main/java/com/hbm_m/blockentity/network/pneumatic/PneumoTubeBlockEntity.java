package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.api.pneumatic.PneumaticNet;
import com.hbm_m.api.pneumatic.PneumaticNetProvider;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.PneumoTubeMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPneumoTube} (1.7.10): das Druckluftrohr.
 *
 * <p>Ein Rohr kann drei Rollen haben, und alle drei zugleich:</p>
 * <ul>
 *   <li><b>Leitung</b> - blosse Verbindung. Rohre, die aneinandergrenzen, bilden ein gemeinsames
 *       Netz, egal wie lang die Strecke ist.</li>
 *   <li><b>Verdichter</b>, sobald eine {@link #insertionDir Einzugsrichtung} gesetzt ist: das Rohr
 *       zieht Druckluft aus dem Rohrnetz und schickt damit alle fuenf Ticks einen Schwung
 *       Gegenstaende aus dem Inventar auf der Einzugsseite los. Ein Schwung kostet 50 mB, und nur
 *       wenn wirklich etwas bewegt wurde.</li>
 *   <li><b>Auswurf</b>, sobald eine {@link #ejectionDir Auswurfrichtung} gesetzt ist: das Rohr
 *       meldet das Inventar auf dieser Seite alle zehn Ticks als Ziel beim Netz an.</li>
 * </ul>
 *
 * <p>Beide Rollen haben denselben Filter aus fuenfzehn Vorlagenplaetzen. Beim Senden gilt der
 * Filter des Verdichters, beim Ankommen zusaetzlich der des Auswurfrohrs - so laesst sich
 * sortieren, ohne fuer jedes Ziel ein eigenes Netz zu bauen.</p>
 *
 * <p>Die eingestellte <b>Druckstufe</b> bestimmt allein die Reichweite, siehe
 * {@link #getRangeFromPressure(int)}: von zehn Bloecken bei Stufe eins bis tausend bei Stufe
 * fuenf.</p>
 *
 * <p>Beim Senden spielt es {@code weapon.reload.tube_fwoomp} - denselben Klang wie im Original,
 * mit leicht streuender Tonhoehe.</p>
 */
public class PneumoTubeBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardReceiverMK2 {

    /** Original: fuenfzehn Filterplaetze. */
    public static final int FILTER_SLOTS = 15;
    /** Original: {@code new FluidTank(Fluids.AIR, 4_000).withPressure(1)}. */
    private static final int TANK_CAPACITY = 4_000;
    /** Original: ein Sendevorgang kostet 50 mB. */
    private static final int AIR_PER_SEND = 50;

    private final ModulePatternMatcher matcher = new ModulePatternMatcher(FILTER_SLOTS);
    private final FluidTank compair;

    /** {@code null} = keine Rolle. Original: {@code ForgeDirection.UNKNOWN}. */
    @Nullable private Direction insertionDir = null;
    @Nullable private Direction ejectionDir = null;

    private boolean whitelist = false;
    /** Original: kehrt die Redstonebedingung um. */
    private boolean redstone = false;
    private byte sendOrder = 0;
    private byte receiveOrder = 0;
    private int soundDelay = 0;
    /** Original: {@code muffled} aus {@code TileEntityMachineBase} - der Schalldaempfer. */
    private boolean muffled = false;
    private int sendCounter = 0;

    @Nullable
    private GenNode<PneumaticNet> node;

    public PneumoTubeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PNEUMO_TUBE_BE.get(), pos, state);
    }

    /** Fuer die bemalbare Fassung, die dieselbe Logik mit eigenem Typ braucht. */
    protected PneumoTubeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                    BlockPos pos, BlockState state) {
        super(type, pos, state, FILTER_SLOTS, 0L, 0L, 0L);
        this.compair = new FluidTank(ModFluids.AIR.getSource(), TANK_CAPACITY).withPressure(1);
    }

    // ── Rollen ──────────────────────────────────────────────────────────────

    public boolean isCompressor() { return insertionDir != null; }
    public boolean isEndpoint()   { return ejectionDir != null; }

    @Nullable public Direction getInsertionDir() { return insertionDir; }
    @Nullable public Direction getEjectionDir()  { return ejectionDir; }

    public void setInsertionDir(@Nullable Direction dir) { insertionDir = dir; setChanged(); sendUpdateToClient(); }
    public void setEjectionDir(@Nullable Direction dir)  { ejectionDir = dir; setChanged(); sendUpdateToClient(); }

    /**
     * Original: {@code getRangeFromPressure}. Die Druckstufe kauft nur Reichweite, nicht
     * Durchsatz - ein Rohr auf Stufe eins schafft genausoviel je Vorgang wie eines auf Stufe fuenf.
     */
    public static int getRangeFromPressure(int pressure) {
        return switch (pressure) {
            case 1 -> 10;
            case 2 -> 25;
            case 3 -> 100;
            case 4 -> 250;
            case 5 -> 1_000;
            default -> 0;
        };
    }

    // ── Filter ──────────────────────────────────────────────────────────────

    /** Original: {@code matchesFilter} - trifft der Gegenstand auf eine der Vorlagen zu? */
    public boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack filter = getInventory().getStackInSlot(i);
            if (!filter.isEmpty() && matcher.isValidForFilter(filter, i, stack)) return true;
        }
        return false;
    }

    /**
     * Original: die Bedingung {@code (match && !whitelist) || (!match && whitelist)} bedeutet
     * "durchlassen". Hier einmal ausformuliert, weil sie an drei Stellen gebraucht wird.
     */
    public boolean passesFilter(ItemStack stack) {
        return matchesFilter(stack) == whitelist;
    }

    public ModulePatternMatcher getMatcher() { return matcher; }
    public boolean isWhitelist()  { return whitelist; }
    public boolean isRedstone()   { return redstone; }
    public byte getSendOrder()    { return sendOrder; }
    public byte getReceiveOrder() { return receiveOrder; }
    public FluidTank getTank()    { return compair; }

    public boolean isMuffled()    { return muffled; }
    public void setMuffled(boolean muffled) { this.muffled = muffled; setChanged(); }

    public void toggleWhitelist() { whitelist = !whitelist; setChanged(); }
    public void toggleRedstone()  { redstone = !redstone; setChanged(); }

    /** Original: die Druckstufe laeuft im Kreis von 1 bis 5. */
    public void nextPressure() {
        int pressure = compair.getPressure() + 1;
        if (pressure > 5) pressure = 1;
        compair.withPressure(pressure);
        setChanged();
    }

    public void nextSendOrder()    { sendOrder = (byte) ((sendOrder + 1) % 3); setChanged(); }
    public void nextReceiveOrder() { receiveOrder = (byte) ((receiveOrder + 1) % 2); setChanged(); }
    public void nextFilterMode(int index) {
        if (index >= 0 && index < FILTER_SLOTS) {
            matcher.nextMode(index);
            setChanged();
        }
    }

    // ── Tick ────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, PneumoTubeBlockEntity be) {
        if (level.isClientSide()) return;

        if (be.soundDelay > 0) be.soundDelay--;

        be.ensureNode(level, pos);

        if (be.isCompressor() && (!level.hasNeighborSignal(pos) ^ be.redstone)) {
            be.tickCompressor(level, pos);
        }

        if (be.isEndpoint() && be.node != null && be.node.net != null && level.getGameTime() % 10 == 0) {
            // Original: das Auswurfrohr meldet sein Ziel regelmaessig an, damit der Eintrag
            // nicht durch den Zeitablauf des Netzes herausfaellt.
            BlockPos target = pos.relative(be.ejectionDir);
            if (level.getBlockEntity(target) != null) {
                be.node.net.addPneumaticReceiver(target, be.ejectionDir, be);
            }
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    private void tickCompressor(Level level, BlockPos pos) {
        // Druckluft von allen Seiten ziehen, ausser den beiden Arbeitsseiten.
        if (level.getGameTime() % 10 == 0) {
            for (Direction dir : Direction.values()) {
                if (dir == insertionDir || dir == ejectionDir) continue;
                trySubscribe(compair.getTankType(), level, pos.relative(dir), dir);
            }
        }

        // Original: die Rohre eines Netzes senden versetzt, damit sie sich nicht alle im selben
        // Tick um dieselben Ziele balgen - daher die Streuzahl der Position im Takt.
        int randTime = Math.abs((int) (level.getGameTime() + PneumaticNet.identifier(pos)));
        if (randTime % 5 != 0) return;

        if (node == null || node.expired || node.net == null) return;
        if (compair.getFill() < AIR_PER_SEND) return;

        BlockPos sourcePos = pos.relative(insertionDir);
        if (level.getBlockEntity(sourcePos) == null) return;

        boolean sent = node.net.send(level, sourcePos, this, insertionDir.getOpposite(),
                sendOrder, receiveOrder, getRangeFromPressure(compair.getPressure()), sendCounter);

        if (sent) {
            compair.setFill(compair.getFill() - AIR_PER_SEND);

            // Original: playSoundEffect(..., "hbm:weapon.reload.tubeFwoomp", 0.25F, 0.9F + rand * 0.2F)
            if (soundDelay <= 0 && !isMuffled()) {
                level.playSound(null, pos, com.hbm_m.sound.ModSounds.TUBE_FWOOMP.get(), SoundSource.BLOCKS,
                        0.25F, 0.9F + level.random.nextFloat() * 0.2F);
                soundDelay = 20;
            }
        }

        sendCounter++;
    }

    /** Original: der Knoten wird beim ersten Tick angelegt und verbindet alle sechs Seiten. */
    private void ensureNode(Level level, BlockPos pos) {
        if (node != null && !node.expired) return;
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        node = UniNodespace.getNode(serverLevel, pos, PneumaticNetProvider.THE_PROVIDER);

        if (node == null || node.expired) {
            GenNode<PneumaticNet> fresh = new GenNode<>(PneumaticNetProvider.THE_PROVIDER, pos);
            NodeDirPos[] conns = new NodeDirPos[6];
            int i = 0;
            for (Direction dir : Direction.values()) {
                conns[i++] = new NodeDirPos(pos.relative(dir), dir);
            }
            fresh.setConnections(conns);
            UniNodespace.createNode(serverLevel, fresh);
            node = fresh;
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && node != null) {
            UniNodespace.destroyNode(serverLevel, worldPosition, PneumaticNetProvider.THE_PROVIDER);
        }
        super.setRemoved();
    }

    // ── IFluidStandardReceiverMK2 ───────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()       { return new FluidTank[] { compair }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { compair }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Original: nur ein Verdichter nimmt Luft an, und nicht ueber seine Arbeitsseiten. */
    @Override
    public boolean canConnect(Fluid type, Direction dir) {
        return dir != insertionDir && dir != ejectionDir
                && type == ModFluids.AIR.getSource() && isCompressor();
    }

    @Override
    public long getReceiverSpeed(Fluid type, int pressure) {
        return Math.max(1, Math.min(100, (compair.getMaxFill() - compair.getFill()) / 25));
    }

    // ── Inventar und Speichern ──────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Die fuenfzehn Plaetze halten nur Vorlagen - sie werden nie tatsaechlich verbraucht.
        return slot >= 0 && slot < FILTER_SLOTS;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putByte("insertionDir", (byte) (insertionDir == null ? -1 : insertionDir.ordinal()));
        tag.putByte("ejectionDir", (byte) (ejectionDir == null ? -1 : ejectionDir.ordinal()));
        compair.writeToNBT(tag, "tank");
        matcher.writeToNBT(tag);

        tag.putByte("sendOrder", sendOrder);
        tag.putByte("receiveOrder", receiveOrder);
        tag.putInt("sendCounter", sendCounter);
        tag.putBoolean("whitelist", whitelist);
        tag.putBoolean("redstone", redstone);
        tag.putBoolean("muffled", muffled);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        insertionDir = dirOf(tag.getByte("insertionDir"));
        ejectionDir = dirOf(tag.getByte("ejectionDir"));
        compair.readFromNBT(tag, "tank");
        matcher.readFromNBT(tag);

        sendOrder = tag.getByte("sendOrder");
        receiveOrder = tag.getByte("receiveOrder");
        sendCounter = tag.getInt("sendCounter");
        whitelist = tag.getBoolean("whitelist");
        redstone = tag.getBoolean("redstone");
        muffled = tag.getBoolean("muffled");
    }

    @Nullable
    private static Direction dirOf(byte ordinal) {
        return ordinal < 0 || ordinal >= Direction.values().length ? null : Direction.values()[ordinal];
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pneumo_tube");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PneumoTubeMenu(id, inv, this);
    }
}
