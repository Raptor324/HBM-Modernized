package com.hbm_m.item.tools_and_armor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.drone.EntityDeliveryDrone;
import com.hbm_m.item.ITooltipProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Port von {@code com.hbm.items.tool.ItemDrone} (1.7.10 Original). Platziert per Rechtsklick auf die
 * Oberseite eines Blocks eine {@link EntityDeliveryDrone} - eine der vier "Patrol"-Varianten
 * (Express x/ Chunk-Loading x). Die 5. Variante (REQUEST, {@code drone_request}) ist ebenfalls dieses Item:
 * wie im Original spawnt sie per Rechtsklick nichts (nur ueber {@code TileEntityDroneDock}), der Stapel
 * wird aber trotzdem verringert. Meta-Varianten sind im Port eigene Items.
 */
public class ItemDrone extends Item implements ITooltipProvider {

    private final boolean express;
    private final boolean chunkLoading;
    private final boolean request;

    public ItemDrone(Properties properties, boolean express, boolean chunkLoading) {
        this(properties, express, chunkLoading, false);
    }

    public ItemDrone(Properties properties, boolean express, boolean chunkLoading, boolean request) {
        super(properties);
        this.express = express;
        this.chunkLoading = chunkLoading;
        this.request = request;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        // stack.getItemDamage() < 4: nur die Patrol-Varianten spawnen eine Drohne
        if (!request) {
            BlockPos pos = context.getClickedPos();
            EntityDeliveryDrone drone = EntityDeliveryDrone.create(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, express, chunkLoading);
            level.addFreshEntity(drone);
        }

        // Original: stack.stackSize-- ohne Kreativ-Pruefung
        context.getItemInHand().shrink(1);

        return InteractionResult.PASS;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            String key = "item.hbm_m." + BuiltInRegistries.ITEM.getKey(this).getPath() + ".desc";
            for (String s : Component.translatable(key).getString().split("\\$"))
                list.add(Component.literal(s).withStyle(ChatFormatting.YELLOW));
        } else {
            list.add(Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                    .append(Component.literal("> to display more info").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }
    }
}
