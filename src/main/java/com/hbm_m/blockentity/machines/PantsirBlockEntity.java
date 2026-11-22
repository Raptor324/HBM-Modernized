package com.hbm_m.blockentity.machines;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntityPantsir;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Der fruehere feste Pantsir-S1. Der Pantsir ist jetzt ein Fahrzeug ({@link EntityPantsir}); dieser Block
 * existiert nur noch in alten Welten und wandelt sich beim ersten Tick samt Munition und Besitzer um.
 */
public class PantsirBlockEntity extends BaseMachineBlockEntity {

    private boolean active = true;
    private boolean iff = true;
    private int ammo = EntityPantsir.MAX_AMMO;
    private int shells = EntityPantsir.MAX_SHELLS / 2;
    @Nullable
    private UUID owner;

    public PantsirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PANTSIR_S1_BE.get(), pos, state, 1, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PantsirBlockEntity be) {
        if (level instanceof ServerLevel server) {
            be.convert(server, pos, state);
        }
    }

    private void convert(ServerLevel server, BlockPos pos, BlockState state) {
        EntityPantsir vehicle = ModEntities.PANTSIR.get().create(server);
        if (vehicle == null) {
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        vehicle.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, facing.toYRot(), 0.0F);
        vehicle.restore(this.active, this.iff, this.ammo, this.shells, this.owner);
        server.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
        server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        server.addFreshEntity(vehicle);
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pantsir_s1");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return null;
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.active = !tag.contains("pantsir_active") || tag.getBoolean("pantsir_active");
        this.iff = !tag.contains("pantsir_iff") || tag.getBoolean("pantsir_iff");
        this.owner = tag.hasUUID("owner_uuid") ? tag.getUUID("owner_uuid") : null;
        this.ammo = tag.contains("pantsir_ammo") ? tag.getInt("pantsir_ammo") : EntityPantsir.MAX_AMMO;
        this.shells = tag.contains("pantsir_shells") ? tag.getInt("pantsir_shells") : EntityPantsir.MAX_SHELLS / 2;
    }
}
