package com.hbm_m.item.tool;

import java.io.InputStream;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.world.gen.nbt.NBTStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 {@code ItemWandD} (Debug-Zauberstab): Rechtsklick baut {@code StructureManager.crane}
 * ({@code crane_mod.nbt}) zentriert auf der Oberflaeche ueber dem anvisierten Block (bis 500 m).
 * Die auskommentierten Testaufrufe des Originals sind nicht uebernommen (dort ebenfalls inaktiv).
 */
public class ItemWandD extends Item implements ITooltipProvider {

    /** {@code StructureManager.crane}: im Port liegt crane_mod.nbt unter data/hbm_m/structures. */
    private static NBTStructure crane;

    private static NBTStructure crane() {
        if (crane == null) {
            InputStream stream = ItemWandD.class.getResourceAsStream("/data/hbm_m/structures/crane_mod.nbt");
            crane = stream != null ? new NBTStructure("crane_mod.nbt", stream) : new NBTStructure(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", "structures/crane_mod.nbt"));
        }
        return crane;
    }

    public ItemWandD(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide)
            return InteractionResultHolder.pass(stack);

        // Library.rayTrace(player, 500, 1, false, true, false)
        HitResult pos = player.pick(500, 1F, false);

        if (pos instanceof BlockHitResult bhr && pos.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            // world.getHeightValue(x, z)
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, bp.getX(), bp.getZ());

            crane().build(world, bp.getX(), y, bp.getZ());
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Used for debugging purposes."));
    }
}
