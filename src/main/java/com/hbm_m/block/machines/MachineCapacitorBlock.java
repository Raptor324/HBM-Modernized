package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCapacitorBlockEntity;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import com.hbm_m.platform.StackNbt;

/** Port of {@code MachineCapacitor} (1.7.10 Original). Directional HE buffer, no GUI. */
public class MachineCapacitorBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final long capacity;

    public MachineCapacitorBlock(Properties properties, long capacity) {
        super(properties);
        this.capacity = capacity;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public long getCapacity() {
        return capacity;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCapacitorBlockEntity(pos, state);
    }

    /**
     * Server-Ticker: rollt die Durchflusszaehler und synchronisiert den Client alle 15 Ticks
     * (Original: {@code networkPackNT(15)} in {@code TileEntityCapacitor.updateEntity}).
     */
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_CAPACITOR_BE.get(), MachineCapacitorBlockEntity::tick);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        // Реллинк шлейфа capacitor_bus: цепочка портов зависит от blockstate СОСЕДЕЙ,
        // который не меняет blockstate самого конденсатора — инвалидируем кэш явно.
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineCapacitorBlockEntity cap) {
            cap.invalidateEnergyPortCache();
        }
    }

    /** Port of {@code MachineCapacitor.printHook}: charge level, percent and the REAL per-tick flow. */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineCapacitorBlockEntity battery)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal(
                EnergyFormatter.format(battery.getEnergyStored()) + " / "
                        + EnergyFormatter.format(battery.getMaxEnergyStored()) + "HE"));

        double percent = (double) battery.getEnergyStored() / (double) battery.getMaxEnergyStored();
        int charge = (int) Math.floor(percent * 10_000D);
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
        text.add(net.minecraft.network.chat.Component.literal((charge / 100D) + "%")
                .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(color))));
        // Original printHook zeigt die Zaehler powerReceived/powerSent, nicht die Speed-Caps.
        text.add(net.minecraft.network.chat.Component.literal("-> ").withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(net.minecraft.network.chat.Component.literal("+" + EnergyFormatter.format(battery.powerReceived) + "HE/t")));
        text.add(net.minecraft.network.chat.Component.literal("<- ").withStyle(net.minecraft.ChatFormatting.RED)
                .append(net.minecraft.network.chat.Component.literal("-" + EnergyFormatter.format(battery.powerSent) + "HE/t")));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    // ═══════════════════ Persistenter Ladestand (Original: IPersistentNBT) ═══════════════════

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);

        if (!pLevel.isClientSide) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof MachineCapacitorBlockEntity capacitor) {
                CompoundTag itemNbt = StackNbt.read(pStack);
                if (itemNbt != null && itemNbt.contains("BlockEntityTag")) {
                    //? if < 1.21.1 {
                    capacitor.load(itemNbt.getCompound("BlockEntityTag"));
                    //?} else {
                    /*capacitor.loadWithComponents(itemNbt.getCompound("BlockEntityTag"), pLevel.registryAccess());
                    *///?}
                    capacitor.setChanged();
                }
            }
        }
    }

    //? if >= 1.21.1 {
    /*// Auf 1.21.1 ist die Loot-Tabelle des Kondensators leer (copy_nbt entfernt), der Zustand
    // wandert per Code in den Drop - nach dem Batterie-Muster.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineCapacitorBlockEntity capacitor) {
                ItemStack stack = new ItemStack(this);
                capacitor.saveToItemStack(stack);
                popResource(level, pos, stack);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    /**
     * Port of {@code MachineCapacitor.addInformation}: gold stat lines (with the corrected
     * /100 + /300 speeds), the yellow persistent-charge line, and the shift-desc on LSHIFT.
     */
    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack pStack, net.minecraft.world.item.Item.TooltipContext pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
    *///?}
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);

        pTooltip.add(Component.translatable("tooltip.hbm_m.capacitor.capacity", EnergyFormatter.format(this.capacity))
                .withStyle(ChatFormatting.GOLD));
        pTooltip.add(Component.translatable("tooltip.hbm_m.capacitor.charge_speed", EnergyFormatter.format(this.capacity / 100))
                .withStyle(ChatFormatting.GOLD));
        pTooltip.add(Component.translatable("tooltip.hbm_m.capacitor.discharge_speed", EnergyFormatter.format(this.capacity / 300))
                .withStyle(ChatFormatting.GOLD));

        long stored = 0;
        CompoundTag nbt = StackNbt.read(pStack);
        if (nbt != null && nbt.contains("BlockEntityTag")) {
            CompoundTag beTag = nbt.getCompound("BlockEntityTag");
            stored = beTag.getLong("energy");
        }
        pTooltip.add(Component.translatable("tooltip.hbm_m.capacitor.stored",
                        EnergyFormatter.format(stored), EnergyFormatter.format(this.capacity))
                .withStyle(ChatFormatting.YELLOW));

        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            for (String line : Component.translatable("tooltip.hbm_m.capacitor.desc").getString().split("\n")) {
                pTooltip.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
            }
        } else {
            pTooltip.add(Component.translatable("tooltip.hbm_m.hold_shift_for_details")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    //? if >1.20.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(p -> new MachineCapacitorBlock(p, this.capacity));
    }
    *///?}

}
