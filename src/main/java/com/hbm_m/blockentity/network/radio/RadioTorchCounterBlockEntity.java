package com.hbm_m.blockentity.network.radio;

import com.hbm_m.block.machines.radio.RadioTorchBaseBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.radio.RadioTorchCounterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * Port of {@code TileEntityRadioTorchCounter} (1.7.10 Original) - 3 independent filter-pattern
 * slots, each with its own channel: counts matching items in the backing block's inventory and
 * broadcasts the count whenever it changes (or every tick if polling).
 */
public class RadioTorchCounterBlockEntity extends BaseMachineBlockEntity implements IRadioTorchConfigurable {

    public static final int SLOT_COUNT = 3;

    public final String[] channel = new String[SLOT_COUNT];
    public final int[] lastCount = new int[SLOT_COUNT];
    public boolean polling = false;
    private final ModulePatternMatcher matcher = new ModulePatternMatcher(SLOT_COUNT);

    public RadioTorchCounterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_COUNTER_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
        java.util.Arrays.fill(channel, "");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTorchCounterBlockEntity be) {
        if (level.isClientSide) return;
        RTTYNetwork.tickIfNeeded(level.getGameTime());

        Direction facing = state.hasProperty(RadioTorchBaseBlock.FACING) ? state.getValue(RadioTorchBaseBlock.FACING) : Direction.UP;
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        BlockEntity sourceBe = level.getBlockEntity(sourcePos);
        if (sourceBe == null) return;

        //? if forge {
        IItemHandler handler = sourceBe.getCapability(ForgeCapabilities.ITEM_HANDLER, facing).orElse(null);
        if (handler == null) return;

        for (int i = 0; i < SLOT_COUNT; i++) {
            if (be.channel[i] == null || be.channel[i].isEmpty()) continue;
            ItemStack pattern = be.inventory.getStackInSlot(i);
            if (pattern.isEmpty()) continue;

            int count = 0;
            for (int j = 0; j < handler.getSlots(); j++) {
                ItemStack stack = handler.getStackInSlot(j);
                if (!stack.isEmpty() && be.matcher.isValidForFilter(pattern, i, stack)) count += stack.getCount();
            }

            if (be.polling || be.lastCount[i] != count) {
                RTTYNetwork.broadcast(level, be.channel[i], String.valueOf(count));
            }
            be.lastCount[i] = count;
        }
        //?}
    }

    public ModulePatternMatcher getMatcher() { return matcher; }

    public void setFilterSlot(int index, ItemStack stack) {
        inventory.setStackInSlot(index, stack);
        matcher.initPattern(index, stack);
        setChanged();
    }

    public void nextFilterMode(int index) {
        matcher.nextMode(index);
        setChanged();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = data.getBoolean("polling");
        for (int i = 0; i < SLOT_COUNT; i++) if (data.contains("channel" + i)) channel[i] = data.getString("channel" + i);
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putBoolean("polling", polling);
        for (int i = 0; i < SLOT_COUNT; i++) {
            tag.putString("channel" + i, channel[i]);
            tag.putInt("lastCount" + i, lastCount[i]);
        }
        matcher.writeToNBT(tag);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.putBoolean("polling", polling);
    for (int i = 0; i < SLOT_COUNT; i++) {
    tag.putString("channel" + i, channel[i]);
    tag.putInt("lastCount" + i, lastCount[i]);
    }
    matcher.writeToNBT(tag);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        polling = tag.getBoolean("polling");
        for (int i = 0; i < SLOT_COUNT; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            lastCount[i] = tag.getInt("lastCount" + i);
        }
        matcher.readFromNBT(tag);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.radio_torch_counter");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return RadioTorchCounterMenu.create(id, inventory, this);
    }
}
