package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Port of {@code BlockMassStorage} (1.7.10 Original). */
public class MachineMassStorageBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    /**
     * 1:1-Port der vier Metadatenstufen aus {@code BlockMassStorage}: von der Holzkiste mit
     * hundert Plaetzen bis zum Stahlspeicher mit einer Million. Die Zahl ist der ganze
     * Unterschied - alles andere verhaelt sich gleich.
     */
    public enum Tier {
        WOOD(100L, "wood"),
        IRON(10_000L, "iron"),
        DESH(100_000L, "desh"),
        STEEL(1_000_000L, null);

        public final long capacity;
        /** Der Texturzusatz des Originals; {@code null} ist die Grundtextur. */
        public final String suffix;

        Tier(long capacity, String suffix) {
            this.capacity = capacity;
            this.suffix = suffix;
        }
    }

    private final Tier tier;

    public Tier getTier() { return tier; }

    /**
     * Port of {@code BlockMassStorage.printHook}: empty storage shows a cyan "Empty" title; a
     * filled one shows the stored item as the title plus "stockpile / capacity" and a red-to-green
     * fill percentage line (original {@code "&[color&]"}-style colored line).
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineMassStorageBlockEntity storage)) return;

        net.minecraft.world.item.ItemStack filter = storage.getFilterStack();
        boolean full = !filter.isEmpty();
        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        net.minecraft.network.chat.Component title;
        int titleCol;
        int bgCol;

        if (full) {
            title = filter.getHoverName();
            text.add(net.minecraft.network.chat.Component.literal(
                    String.format(java.util.Locale.US, "%,d", storage.getStockpile())
                            + " / " + String.format(java.util.Locale.US, "%,d", storage.getCapacity())));

            double percent = (double) storage.getStockpile() / (double) storage.getCapacity();
            int charge = (int) Math.floor(percent * 10_000D);
            int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
            text.add(net.minecraft.network.chat.Component.literal((charge / 100D) + "%")
                    .withStyle(net.minecraft.network.chat.Style.EMPTY
                            .withColor(net.minecraft.network.chat.TextColor.fromRgb(color))));

            titleCol = 0xffff00;
            bgCol = 0x404000;
        } else {
            title = net.minecraft.network.chat.Component.literal("Empty");
            titleCol = 0x00ffff;
            bgCol = 0x004040;
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics, title, titleCol, bgCol, text);
    }

    public MachineMassStorageBlock(Properties properties) {
        this(properties, Tier.STEEL);
    }

    public MachineMassStorageBlock(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineMassStorageBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_MASS_STORAGE_BE.get(),
                (lvl, pos, st, be) -> MachineMassStorageBlockEntity.tick(lvl, pos, st, (MachineMassStorageBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineMassStorageBlock> CODEC = simpleCodec(MachineMassStorageBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    @Override
    public void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
                            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            com.hbm_m.block.MachineDrops.dropInventory(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
