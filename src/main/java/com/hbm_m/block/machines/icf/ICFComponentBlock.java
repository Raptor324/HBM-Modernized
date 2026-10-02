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
public class ICFComponentBlock extends Block implements com.hbm_m.api.block.IToolable {

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

    /** Das Material, das die Umwandlung kostet ({@code BlockToolConversion.registerRecipes}). */
    private List<CostEntry> cost() {
        if (tool == Tool.TORCH) {
            // ANY_BISMOIDBRONZE.plateCast() = Bismut- oder Arsenbronze-Gussplatte
            return List.of(new CostEntry(List.of(
                    ModMaterialItems.item(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST),
                    ModMaterialItems.item(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST)), 1));
        }
        return List.of(
                new CostEntry(List.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST)), 1),
                // Original: DURA.bolt() - Durastahl heisst in diesem Port High-Speed Steel.
                new CostEntry(List.of(ModItems.BOLT_HIGHSPEED_STEEL.get()), 4));
    }

    private record CostEntry(List<Item> items, int count) {}

    /** 1:1 {@code BlockToolConversion.onScrew}: Werkzeug passt, Material vorhanden -> Nachfolgeblock. */
    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, net.minecraft.core.Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, com.hbm_m.api.block.IToolable.ToolType toolType) {
        if (level.isClientSide()) return false;
        if (result == null || tool == null || !tool.name().equals(toolType.name())) return false;

        // Original: InventoryUtil.doesPlayerHaveAStacks(player, list, true) - erst pruefen, dann nehmen.
        List<CostEntry> cost = cost();
        for (CostEntry entry : cost) {
            if (countInInventory(player, entry.items()) < entry.count()) return false;
        }
        for (CostEntry entry : cost) consume(player, entry.items(), entry.count());

        level.setBlock(pos, result.get().defaultBlockState(), 3);
        return true;
    }

    private static int countInInventory(Player player, List<Item> items) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (items.contains(stack.getItem())) count += stack.getCount();
        }
        return count;
    }

    private static void consume(Player player, List<Item> items, int amount) {
        for (int i = 0; i < player.getInventory().getContainerSize() && amount > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!items.contains(stack.getItem())) continue;

            int taken = Math.min(amount, stack.getCount());
            stack.shrink(taken);
            amount -= taken;
        }
    }

}
