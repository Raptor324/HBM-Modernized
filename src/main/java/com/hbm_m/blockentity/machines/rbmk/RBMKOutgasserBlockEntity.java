package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKNeutronStream;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.menu.RBMKOutgasserMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKOutgasserBlockEntity extends RBMKColumnBlockEntity
        implements IRBMKFluxReceiver, IRBMKLoadable, MenuProvider {

    public ItemStack rodSlot   = ItemStack.EMPTY;
    public double   fluxBuffer = 0;

    public RBMKOutgasserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_OUTGASSER_BE.get(), pos, state);
    }

    public boolean canProcess() {
        return !rodSlot.isEmpty()
            && rodSlot.getItem() instanceof RBMKRodItem
            && RBMKRodItem.getPoison(rodSlot) > 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKOutgasserBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;
        if (be.canProcess() && be.fluxBuffer > 0) {
            double removal = Math.min(be.fluxBuffer * RBMKDials.getOutgasserMod(level), RBMKRodItem.getPoison(be.rodSlot));
            RBMKRodItem.setPoison(be.rodSlot, RBMKRodItem.getPoison(be.rodSlot) - removal);
            be.fluxBuffer = 0;
            be.setChanged();
        }
    }

    @Override
    public void receiveFlux(RBMKNeutronStream stream) {
        fluxBuffer += stream.fluxQuantity;
        stream.fluxQuantity = 0;
    }

    @Override public boolean canLoad(ItemStack s)  { return !s.isEmpty() && s.getItem() instanceof RBMKRodItem && rodSlot.isEmpty(); }
    @Override public void    load(ItemStack s)     { rodSlot = s.copy(); setChanged(); }
    @Override public boolean canUnload()           { return !rodSlot.isEmpty(); }
    @Override public ItemStack provideNext()       { return rodSlot; }
    @Override public void    unload()              { rodSlot = ItemStack.EMPTY; setChanged(); }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_outgasser"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKOutgasserMenu(id, inv, this); }
    @Override public RBMKType getRBMKType()      { return RBMKType.OUTGASSER; }
    @Override public ColumnType getConsoleType() { return ColumnType.OUTGASSER; }

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (!rodSlot.isEmpty()) tag.put("rodSlot", com.hbm_m.platform.PlatformHooks.safeItemSave(rodSlot, registries));
        tag.putDouble("fluxBuffer", fluxBuffer);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("rodSlot")) rodSlot = com.hbm_m.platform.PlatformHooks.itemStackOf(tag.getCompound("rodSlot"), registries);
        fluxBuffer = tag.getDouble("fluxBuffer");
    }
}

