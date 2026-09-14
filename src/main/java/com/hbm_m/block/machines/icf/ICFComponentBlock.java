package com.hbm_m.block.machines.icf;

import java.util.List;
import java.util.function.Supplier;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port des Werkzeugverhaltens von {@code BlockICFComponent} / {@code BlockToolConversion}
 * (1.7.10): die Huellenteile des ICF-Reaktors werden <b>an Ort und Stelle</b> fertiggestellt.
 *
 * <p>Die Umwandlungstabelle steht unveraendert in {@code BlockToolConversion.registerRecipes()}:</p>
 * <ul>
 *   <li>Schneidbrenner auf die <b>Gefaesswand</b>, dazu ein Bismutbronze-Gussblech im Gepaeck,
 *       ergibt die verschweisste Wand.</li>
 *   <li>Nietwerkzeug auf das <b>Bauteil</b>, dazu ein Stahl-Gussblech und vier Bolzen, ergibt das
 *       vernietete Bauteil.</li>
 * </ul>
 *
 * <p>Das ist kein Beiwerk: der {@link ICFStructBlock Aufbaukern} verlangt ausdruecklich die
 * <b>fertigen</b> Teile - mit rohen Waenden setzt sich der Reaktor nicht zusammen.</p>
 *
 * <p><b>Anmerkung:</b> im Original sind das fuenf Metadaten eines Blocks, hier fuenf eigene
 * Bloecke - die Umwandlung setzt darum den Nachfolgeblock statt der Metadatenzahl. Die Zutaten
 * sind unveraendert.</p>
 */
public class ICFComponentBlock extends Block {

    /** Womit umgewandelt wird - im Original {@code ToolType}. */
    public enum Tool { TORCH, BOLT }

    @Nullable private final Tool tool;
    @Nullable private final Supplier<Block> result;

    /** Ein Huellenteil ohne weitere Ausbaustufe. */
    public ICFComponentBlock(Properties properties) {
        this(properties, null, null);
    }

    public ICFComponentBlock(Properties properties, @Nullable Tool tool, @Nullable Supplier<Block> result) {
        super(properties);
        this.tool = tool;
        this.result = result;
    }

    private boolean matchesTool(ItemStack stack) {
        if (tool == null) return false;
        return switch (tool) {
            case TORCH -> stack.is(ModItems.BLOWTORCH.get()) || stack.is(ModItems.ACETYLENE_TORCH.get());
            case BOLT -> stack.is(ModItems.WRENCH.get()) || stack.is(ModItems.WRENCH_ARCHINEER.get());
        };
    }

    /** Das Material, das die Umwandlung kostet - Gegenstand und Anzahl. */
    private List<CostEntry> cost() {
        if (tool == Tool.TORCH) {
            return List.of(new CostEntry(
                    ModMaterialItems.item(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT), 1));
        }
        return List.of(
                new CostEntry(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST), 1),
                // Original: DURA.bolt() - Durastahl heisst in diesem Port High-Speed Steel.
                new CostEntry(ModItems.BOLT_HIGHSPEED_STEEL.get(), 4));
    }

    private record CostEntry(Item item, int count) {}

    private InteractionResult convert(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (result == null || !matchesTool(player.getItemInHand(hand))) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Original: InventoryUtil.doesPlayerHaveAStacks(player, list, true) - erst pruefen, dann nehmen.
        List<CostEntry> cost = cost();
        if (!player.isCreative()) {
            for (CostEntry entry : cost) {
                if (countInInventory(player, entry.item()) < entry.count()) return InteractionResult.CONSUME;
            }
            for (CostEntry entry : cost) consume(player, entry.item(), entry.count());
        }

        level.setBlock(pos, result.get().defaultBlockState(), 3);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.5F);
        return InteractionResult.CONSUME;
    }

    private static int countInInventory(Player player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void consume(Player player, Item item, int amount) {
        for (int i = 0; i < player.getInventory().getContainerSize() && amount > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item)) continue;

            int taken = Math.min(amount, stack.getCount());
            stack.shrink(taken);
            amount -= taken;
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return convert(level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return convert(level, pos, player, hand);
    }
    *///?}
}
