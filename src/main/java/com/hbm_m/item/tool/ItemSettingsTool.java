package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.interfaces.ICopiable;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.network.InfoToastPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code com.hbm.items.tool.ItemSettingsTool}: Schleich-Rechtsklick kopiert die Einstellungen einer
 * {@link ICopiable}-Maschine, Rechtsklick fuegt sie ein; die Alt-Taste (TOOL_ALT) waehlt den Eintrag.
 */
public class ItemSettingsTool extends Item implements ITooltipProvider {

    public ItemSettingsTool(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int i, boolean bool) {
        if (!(entity instanceof ServerPlayer player)) return;

        if (player.getMainHandItem() == stack && StackNbt.has(stack)) {
            int delay = StackNbt.read(stack).getInt("inputDelay");
            delay++;
            ListTag displayInfo = StackNbt.tag(stack).getList("displayInfo", 10);

            if (HbmPlayerProps.get(player).getKeyPressed(EnumKeybind.TOOL_ALT) && delay > 4) {
                int index = StackNbt.read(stack).getInt("copyIndex") + 1;
                if (index > displayInfo.size() - 1) index = 0;
                StackNbt.tag(stack).putInt("copyIndex", index);
                delay = 0;
            }

            StackNbt.tag(stack).putInt("inputDelay", delay);
            if (world.getGameTime() % 5 != 0) return;

            for (int j = 0; j < displayInfo.size(); j++) {
                CompoundTag nbt = displayInfo.getCompound(j);
                ChatFormatting format = StackNbt.read(stack).getInt("copyIndex") == j ? ChatFormatting.AQUA : ChatFormatting.YELLOW;
                InfoToastPacket.sendTo(player, Component.translatable(nbt.getString("info")).withStyle(format), 80, 897 + j, 0xFFFFFF);
            }
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Can copy the settings (filters, fluid ID, etc) of machines"));
        list.add(Component.literal("Shift right-click to copy, right click to paste"));
        list.add(Component.literal("Ctrl click on pipes to paste settings to multiple pipes"));
        if (StackNbt.read(stack) != null) {
            CompoundTag nbt = StackNbt.read(stack);
            if (nbt.contains("tileName")) {
                list.add(Component.translatable(nbt.getString("tileName")).withStyle(ChatFormatting.BLUE));
            } else {
                list.add(Component.literal(" None ").withStyle(ChatFormatting.RED));
            }
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null) return InteractionResult.PASS;

        ICopiable copiable = getCopyInfoSource(world, pos);
        if (copiable == null) return InteractionResult.PASS;
        Block block = world.getBlockState(pos).getBlock();

        Component prefix = Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));

        if (player.isShiftKeyDown()) {
            CompoundTag settings = copiable.getSettings(world, pos);
            StackNbt.set(stack, settings);
            if (settings != null) {
                settings.putString("tileName", copiable.getSettingsSourceID(block));
                settings.putInt("copyIndex", 0);
                settings.putInt("inputDelay", 0);
                String[] info = copiable.infoForDisplay(world, pos);
                if (info != null) {
                    ListTag displayInfo = new ListTag();
                    for (String str : info) {
                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("info", str);
                        displayInfo.add(nbt);
                    }
                    settings.put("displayInfo", displayInfo);
                }
                // 1.21.1: set() kopiert das Tag -> nach dem Befuellen erneut setzen (1.20.1: dasselbe Objekt, wirkungslos)
                StackNbt.set(stack, settings);
                if (world.isClientSide) {
                    player.sendSystemMessage(prefix.copy().append(Component.literal("Copied settings of ").append(copiable.getSettingsSourceDisplay(block)).withStyle(ChatFormatting.AQUA)));
                }
            } else {
                player.sendSystemMessage(prefix.copy().append(Component.literal("Copy failed, machine has no settings tool support: ").append(copiable.getSettingsSourceDisplay(block)).withStyle(ChatFormatting.RED)));
            }
        } else if (StackNbt.has(stack)) {
            int index = StackNbt.read(stack).getInt("copyIndex");
            copiable.pasteSettings(StackNbt.tag(stack), index, world, player, pos);
        }

        return world.isClientSide ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    @Nullable
    private ICopiable getCopyInfoSource(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof ICopiable c) return c;
        if (world.getBlockState(pos).getBlock() instanceof ICopiable c) return c;
        return null;
    }
}
