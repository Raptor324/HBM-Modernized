package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.block.machines.RadioboxBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityRadiobox} ("Rosenberg-Schaedlingsbekaempfung"): nimmt von allen sechs Seiten Strom (500000 HE)
 * und trifft, solange der Hebel umgelegt ist, je Tick fuer 25000 HE (mit Funkenbatterie gratis) jedes feindliche
 * Wesen in einem 31er-Wuerfel mit 20 Entkraeftungsschaden. Die FBI-Truppe ist ausgenommen.
 */
public class RadioboxBlockEntity extends BaseMachineBlockEntity {

    public static long maxPower = 500000;
    public boolean infinite = false;

    public RadioboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIOBOX_BE.get(), pos, state, 0, maxPower, maxPower, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioboxBlockEntity be) {
        if (level instanceof ServerLevel server) be.serverTick(server, pos, state);
    }

    private void serverTick(ServerLevel world, BlockPos pos, BlockState state) {

        for (Direction dir : Direction.values())
            this.trySubscribe(world, pos.getX() + dir.getStepX(), pos.getY() + dir.getStepY(), pos.getZ() + dir.getStepZ(), dir);

        boolean on = state.hasProperty(RadioboxBlock.ON) && state.getValue(RadioboxBlock.ON);

        if (on && (getEnergyStored() >= 25000 || infinite)) {

            if (!infinite) {
                setEnergyStored(getEnergyStored() - 25000);
                this.setChanged();
            }

            int range = 15;

            List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos.getX() - range, pos.getY() - range, pos.getZ() - range, pos.getX() + range, pos.getY() + range, pos.getZ() + range),
                    e -> e instanceof Enemy);

            for (LivingEntity entity : entities) {

                // Original: EntityFBI/EntityFBIDrone bleiben verschont - sobald die Razzia portiert ist, hier ausnehmen
                if (isFBI(entity)) continue;

                entity.hurt(ModDamageSources.enervation(world), 20.0F);
            }
        }
    }

    private static boolean isFBI(LivingEntity e) {
        String name = e.getClass().getSimpleName();
        return name.equals("EntityFBI") || name.equals("EntityFBIDrone");
    }

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
        tag.putBoolean("infinite", infinite);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        infinite = tag.getBoolean("infinite");
    }
}
