package com.hbm_m.block.machines.fusion;

import com.hbm_m.block.ModBlocks;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1-Port des Werkzeugverhaltens von {@code BlockFusionComponent} / {@code BlockToolConversion}
 * (1.7.10): Ein Schneidbrenner verschweisst die rohe BSCCO-Spule zum fertigen Bauteil, wenn der
 * Spieler eine gegossene Stahlplatte dabei hat.
 *
 * <p>Im Original sind das vier Metadaten eines Blocks; in diesem Port existieren die Varianten seit
 * jeher als eigene Bloecke, deshalb setzt die Umwandlung hier den Nachbarblock statt der Metadaten.
 * Die Umwandlungstabelle steht 1:1 in {@code BlockToolConversion.registerRecipes()}:
 * {@code ToolType.TORCH + fusion_component:0 + STEEL.plateCast() -> fusion_component:1}.</p>
 */
public class FusionComponentBlock extends Block implements com.hbm_m.interfaces.ILookOverlay, com.hbm_m.api.block.IToolable {

    /**
     * 1:1-Port von {@code BlockToolConversion.printHook}: solange der Spieler den Brenner in der
     * Hand haelt, steht am Fadenkreuz, was die Umwandlung kostet. Ohne diese Anzeige gibt es
     * im Spiel keinen Hinweis darauf, dass die rohe Spule ueberhaupt verschweisst werden muss -
     * und der Toruskern nimmt nur die verschweisste Variante an.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        if (!isTorch(player.getMainHandItem()) && !isTorch(player.getOffhandItem())) return;

        ItemStack plate = ModMaterialItems.stack(ModMaterials.STEEL, MaterialShape.PLATE_CAST, 1);

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal("Requires:")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        text.add(net.minecraft.network.chat.Component.literal("- ")
                .withStyle(net.minecraft.ChatFormatting.BLUE)
                .append(new ItemStack(ModItems.BLOWTORCH.get()).getHoverName()));
        text.add(net.minecraft.network.chat.Component.literal("- ")
                .append(plate.getHoverName())
                .append(net.minecraft.network.chat.Component.literal(" x1")));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()),
                0xffff00, 0x404000, text);
    }

    public FusionComponentBlock(Properties properties) {
        super(properties);
    }

    private static boolean isTorch(ItemStack stack) {
        return stack.is(ModItems.BLOWTORCH.get()) || stack.is(ModItems.ACETYLENE_TORCH.get());
    }

    /** 1:1 {@code BlockToolConversion.onScrew}: TORCH + STEEL.plateCast() -> verschweisste Spule. */
    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, net.minecraft.core.Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, com.hbm_m.api.block.IToolable.ToolType tool) {
        if (level.isClientSide()) return false;
        if (tool != com.hbm_m.api.block.IToolable.ToolType.TORCH) return false;

        // Original: InventoryUtil.doesPlayerHaveAStacks(player, list, true) - Material wird verbraucht.
        if (!consumePlate(player)) return false;

        level.setBlock(pos, ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get().defaultBlockState(), 3);
        return true;
    }

    private static boolean consumePlate(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST))) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
