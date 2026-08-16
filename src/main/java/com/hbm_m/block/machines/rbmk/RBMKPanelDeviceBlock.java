package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKPanelDeviceBlockEntity;
import com.hbm_m.item.tools_and_armor.ScrewdriverItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Shared block class for the 7 RTTY panel devices. Each original type (Gauge/Indicator/Lever/
 * Numitron/Graph/Terminal/KeyPad) was a distinct block class; here one parameterized class
 * covers all 7, since the only real per-type differences are the block entity, its config
 * screen, and (for Lever/KeyPad) a primary left-click action - everything else (screwdriver
 * opens config, RTTY tick wiring) is identical.
 */
public class RBMKPanelDeviceBlock extends RBMKColumnBlock {

    /** Fires the device's primary action (lever flip / keypad press) on a plain right-click. */
    public interface PrimaryClick {
        void click(RBMKPanelDeviceBlockEntity be, Level level, BlockPos pos, Player player, BlockHitResult hit);
    }

    private final BiFunction<BlockPos, BlockState, BlockEntity> factory;
    private final Supplier<BlockEntityType<?>> typeSupplier;
    /** Dispatch key for {@code RBMKPanelScreenOpener.open(screenId, pos)} (client-only). Kept as a
     *  plain string, never a method reference, so this common-code class never touches Minecraft
     *  client classes even indirectly - matches the {@code EnvExecutor.runInEnv} caution already
     *  used by {@code RadioTorchSenderBlock#use}. */
    private final String screenId;
    @Nullable private final PrimaryClick primaryClick;
    /**
     * Matches the original: Gauge/Indicator/Numitron/Graph implement {@code IToolable} and only
     * ever open their config GUI from {@code onScrew} - a plain right-click does nothing at all
     * for them. Terminal has no primary-click action but (per {@code TileEntityRBMKTerminal}'s
     * block) opens on any plain right-click, no screwdriver required. Lever/KeyPad ignore this
     * flag entirely since {@link #primaryClick} already takes over non-screwdriver clicks.
     */
    private final boolean requireScrewdriver;

    public RBMKPanelDeviceBlock(Properties props, BiFunction<BlockPos, BlockState, BlockEntity> factory,
                                 Supplier<BlockEntityType<?>> typeSupplier, String screenId,
                                 boolean requireScrewdriver, @Nullable PrimaryClick primaryClick) {
        super(props);
        this.factory = factory;
        this.typeSupplier = typeSupplier;
        this.screenId = screenId;
        this.requireScrewdriver = requireScrewdriver;
        this.primaryClick = primaryClick;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, (BlockEntityType<T>) typeSupplier.get(),
                (lvl, pos, st, be) -> { if (be instanceof RBMKPanelDeviceBlockEntity p) p.tickPanel(lvl, pos); });
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RBMKPanelDeviceBlockEntity panel)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        boolean screwdriver = held.getItem() instanceof ScrewdriverItem;

        // Original behavior: Gauge/Indicator/Numitron/Graph open their config screen only via
        // screwdriver (IToolable#onScrew); Lever/KeyPad fire their primary action on a plain
        // click and fall back to the screwdriver for configuration; Terminal opens on any click.
        // Matches the original's explicit `if(player.isSneaking()) return false;` in
        // RBMKLever#onBlockActivated - a sneak-click never fires the primary action.
        if (primaryClick != null && !screwdriver && !player.isShiftKeyDown()) {
            if (!level.isClientSide) primaryClick.click(panel, level, pos, player, hit);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (requireScrewdriver && !screwdriver) return InteractionResult.PASS;

        if (level.isClientSide) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                    com.hbm_m.client.gui.rbmk.RBMKPanelScreenOpener.open(screenId, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
