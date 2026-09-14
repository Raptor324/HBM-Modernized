package com.hbm_m.blockentity.machines.albion;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code TileEntityPABeamline} (1.7.10): das schlichte Strahlrohr.
 *
 * <p>Es tut nichts, ausser das Teilchen drei Bloecke weiterzureichen - es braucht weder Energie
 * noch Kuehlung. Das Fenster ({@link #window}) ist reine Optik und wird im Original beim Bauen
 * gesetzt.</p>
 *
 * <p>Die Strahlachse steht quer zur Blickrichtung des Blocks.</p>
 * <p><b>Es ist ein Multiblock.</b> Wie im Original steht das Teilchen nie auf dem Kern, sondern
 * auf einer seiner Dummyzellen; der Kern wird von dort aus gesucht. Daraus ergeben sich die
 * Spruenge von zwei bis fuenf Feldern - und damit die tatsaechliche Groesse eines Rings.</p>
 */
public class PABeamlineBlockEntity extends BaseMachineBlockEntity implements IParticleUser {

    /** Original: {@code addDistance(3)}. */
    private static final int DISTANCE = 3;
    /** Original: die Eingangszelle liegt ein Feld vor dem Kern. */
    private static final int ENTRY_OFFSET = -1;
    /** Original: {@code offset(beamlineDir, 2)} - ein Feld hinter der hinteren Dummyzelle. */
    private static final int EXIT_OFFSET = 2;

    /** Original: {@code window} - ob dieses Segment ein Sichtfenster hat. */
    private boolean window = false;
    /** Original: {@code didPass} - fuer das kurze Aufleuchten beim Durchflug. */
    private boolean didPass = false;

    public PABeamlineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_BEAMLINE_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    private Direction beamAxis() {
        return PAOrientation.beamAxis(getBlockState());
    }

    @Override
    public boolean canParticleEnter(Particle particle, Direction dir, int x, int y, int z) {
        // 1:1-Port: die Eingangszelle liegt ein Feld vor dem Kern, und die Richtung muss stimmen.
        BlockPos input = worldPosition.relative(beamAxis(), ENTRY_OFFSET);
        return input.getX() == x && input.getY() == y && input.getZ() == z && beamAxis() == dir;
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        particle.addDistance(DISTANCE);
        this.didPass = true;
        setChanged();
    }

    @Override
    public BlockPos getExitPos(Particle particle) {
        return worldPosition.relative(beamAxis(), EXIT_OFFSET);
    }

    public boolean hasWindow() {
        return window;
    }

    public void setWindow(boolean window) {
        this.window = window;
        setChanged();
    }

    /** Wird vom Renderer abgefragt und dabei zurueckgesetzt - wie das {@code didPass} des Originals. */
    public boolean consumeDidPass() {
        boolean passed = didPass;
        didPass = false;
        return passed;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("window", window);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        window = tag.getBoolean("window");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.beamline");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null; // Original: kein GUI.
    }
}
