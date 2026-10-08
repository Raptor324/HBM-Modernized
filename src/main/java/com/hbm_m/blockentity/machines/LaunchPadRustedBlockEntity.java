package com.hbm_m.blockentity.machines;

import com.hbm_m.api.bomb.IBomb.BombReturnCode;
import com.hbm_m.api.item.IDesignatorItem;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.missile.MissileTier4;
import com.hbm_m.inventory.menu.LaunchPadRustedMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityLaunchPadRusted}: kein Strom, kein Treibstoff. Die Rakete ist fest eingebaut
 * ({@link #missileLoaded}, gesetzt von der Silo-Struktur) - Slot 0 Ausgabe ("Release Missile"),
 * 1 Startcodes, 2 Startschluessel, 3 Zielgeber. Gestartet wird per Redstone-Flanke oder IBomb.
 */
public class LaunchPadRustedBlockEntity extends LaunchPadBaseBlockEntity implements IControlReceiver {

    private static final int RUSTED_SLOT_DESIGNATOR = 3;

    /** Original: {@code missileLoaded} - die verrostete Doomsday-Rakete steht auf der Rampe. */
    public boolean missileLoaded;

    @Override
    public int getDesignatorSlot() {
        return RUSTED_SLOT_DESIGNATOR;
    }

    public LaunchPadRustedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_PAD_RUSTED_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchPadRustedBlockEntity be) {
        if (level.isClientSide) {
            clientLaunchPadSmokeTick(level, pos, state);
        } else {
            if (be.redstonePower > 0 && be.prevRedstonePower <= 0) {
                be.launch();
            }
            be.prevRedstonePower = be.redstonePower;
            // Original networkPackNT(250)
            if (level.getGameTime() % 10 == 0) be.sendUpdateToClient();
        }
    }

    /** Original {@code launch()}. */
    public BombReturnCode launch() {
        if (level == null || level.isClientSide) return BombReturnCode.ERROR_MISSING_COMPONENT;

        ItemStack codes = inventory.getStackInSlot(1);
        ItemStack key = inventory.getStackInSlot(2);
        ItemStack des = inventory.getStackInSlot(3);

        if (!codes.isEmpty() && !key.isEmpty() && !des.isEmpty() && this.missileLoaded) {
            if (codes.getItem() == ModItems.LAUNCH_CODE.get() && key.getItem() == ModItems.LAUNCH_KEY.get()) {
                if (des.getItem() instanceof IDesignatorItem designator) {
                    int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
                    if (!designator.isReady(level, des, x, y, z)) return BombReturnCode.ERROR_MISSING_COMPONENT;

                    net.minecraft.world.phys.Vec3 coords = designator.getCoords(level, des, x, y, z);
                    int targetX = (int) Math.floor(coords.x);
                    int targetZ = (int) Math.floor(coords.z);

                    MissileTier4.MissileDoomsdayRusted missile = ModEntities.MISSILE_DOOMSDAY_RUSTED.get().create(level);
                    if (missile == null) return BombReturnCode.ERROR_MISSING_COMPONENT;
                    missile.initLaunch(x + 0.5F, y + 1F, z + 0.5F, targetX, targetZ);
                    BlockState padState = getBlockState();
                    if (padState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                        missile.setLaunchFacing(padState.getValue(HorizontalDirectionalBlock.FACING));
                    }
                    level.addFreshEntity(missile);
                    level.playSound(null, x + 0.5, y, z + 0.5, com.hbm_m.sound.ModSounds.MISSILE_TAKEOFF.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                    this.missileLoaded = false;
                    inventory.extractItem(1, 1, false);
                    this.setChanged();
                    this.sendUpdateToClient();

                    return BombReturnCode.LAUNCHED;
                }
            }
        }

        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    @Override
    public BombReturnCode triggerLaunch() {
        return launch();
    }

    /** Das Modell auf der Rampe (BER) zeigt die eingebaute Rakete, nicht den Ausgabeslot. */
    @Override
    public ItemStack getMissilePreviewStack() {
        return this.missileLoaded ? new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED.get()) : ItemStack.EMPTY;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    /** Original {@code receiveControl}: "release" gibt die Rakete als Gegenstand in Slot 0 aus. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("release")) {
            if (this.missileLoaded && inventory.getStackInSlot(0).isEmpty()) {
                this.missileLoaded = false;
                inventory.setStackInSlot(0, new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED.get()));
                this.setChanged();
                this.sendUpdateToClient();
            }
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("missileLoaded", missileLoaded);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.missileLoaded = tag.getBoolean("missileLoaded");
    }

    @Override
    protected Component getDefaultName() {
        // В оригинале использовался отдельный GUI, поэтому даём отдельный ключ
        return Component.translatable("container.launchPadRusted");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new LaunchPadRustedMenu(containerId, inv, this);
    }

    /** Original: kein Strom - die verrostete Rampe ist kein Energieverbraucher. */
    @Override
    public boolean canConnectEnergy(Direction side) {
        return false;
    }

    @Override
    protected boolean isReadyForLaunch() {
        return this.missileLoaded;
    }

    @Override
    public boolean canLaunch() {
        return this.missileLoaded;
    }

    /** Original ist kein IRadarCommandReceiver: Radar-/Koordinatenbefehle starten hier nichts. */
    @Override
    public boolean launchToCoordinate(int targetX, int targetZ) {
        return false;
    }

    @Override
    public boolean launchToEntity(Entity entity) {
        return false;
    }

    @Override
    public NodeDirPos[] getConPos() {
        return new NodeDirPos[0];
    }
}
