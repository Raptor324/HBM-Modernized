package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Port of {@code TileEntityRadiobox} (1.7.10 Original) - "Rosenberg Pest Control Box". HE-powered
 * anti-mob damage field: while on and sufficiently charged, deals damage to every hostile mob in a
 * 15-block cube each tick.
 * <p>
 * SCOPE-Vereinfachung: Das Original schliesst zwei eigene Entity-Typen ({@code EntityFBI}/
 * {@code EntityFBIDrone}) von der Zielerfassung aus - existieren in diesem Port nicht, daher
 * betrifft der Filter hier einfach alle {@link Enemy}-Mobs. Richtungsabhaengige Hitbox/Textur je
 * nach Platzierungs-Rotation entfaellt (statischer Wuerfel).
 */
public class RadioboxBlockEntity extends BaseMachineBlockEntity {

    private static final long CAPACITY = 500_000L;
    private static final long MAX_RECEIVE = 4_096L;
    private static final long ACTIVATION_THRESHOLD = 25_000L;
    private static final double RADIUS = 7.5D;
    private static final float DAMAGE = 20.0F;

    private boolean isOn = false;
    private boolean infinite = false;

    public RadioboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIOBOX_BE.get(), pos, state, 0, CAPACITY, MAX_RECEIVE, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioboxBlockEntity be) {
        if (level.isClientSide) return;
        if (!be.isOn) return;
        if (!be.infinite && be.getEnergyStored() < ACTIVATION_THRESHOLD) return;

        AABB box = new AABB(pos).inflate(RADIUS);
        for (Enemy mob : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box, e -> e instanceof Enemy)
                .stream().map(e -> (Enemy) e).toList()) {
            ((net.minecraft.world.entity.LivingEntity) mob).hurt(ModDamageSources.enervation(level), DAMAGE);
        }
    }

    public void toggleOn(Player player) {
        isOn = !isOn;
        setChanged();
    }

    public boolean activateInfinite(ItemStack sparkBattery) {
        if (infinite) return false;
        infinite = true;
        sparkBattery.shrink(1);
        setChanged();
        return true;
    }

    public boolean isOn() { return isOn; }
    public boolean isInfinite() { return infinite; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.radiobox");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("infinite", infinite);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        isOn = tag.getBoolean("isOn");
        infinite = tag.getBoolean("infinite");
    }
}
