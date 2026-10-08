package com.hbm_m.item.tool;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.SolarMirrorBlockEntity;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.multiblock.MultiblockInteractionHelper;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemMirrorTool}: Klick auf den Solarkessel merkt dessen Position (Kern, y + 1), Klick auf einen
 * Spiegel richtet ihn auf den Kessel aus (max. 100 m, Winkel min. 45 Grad). Haelt +2 Angriffsschaden.
 */
public class ItemMirrorTool extends Item implements ITooltipProvider {

    public ItemMirrorTool(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        BlockPos clicked = ctx.getClickedPos();

        // BlockDummyable.findCore (im Port sind die Nebenbloecke Multiblock-Teile, daher erst den Kern aufloesen)
        BlockPos pos = MultiblockInteractionHelper.resolveControllerPos(world, clicked);

        if (world.getBlockState(pos).is(ModBlocks.SOLAR_BOILER.get())) {

            if (!world.isClientSide) {

                PlatformHooks.putInt(stack, "posX", pos.getX());
                PlatformHooks.putInt(stack, "posY", pos.getY() + 1);
                PlatformHooks.putInt(stack, "posZ", pos.getZ());

                if (player != null) player.sendSystemMessage(Component.translatable("item.hbm_m.mirror_tool.linked").withStyle(ChatFormatting.YELLOW));
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        if (world.getBlockState(clicked).is(ModBlocks.SOLAR_MIRROR.get()) && PlatformHooks.hasItemTag(stack)) {

            if (!world.isClientSide) {
                int x = clicked.getX(), y = clicked.getY(), z = clicked.getZ();
                int tx = PlatformHooks.getInt(stack, "posX");
                int ty = PlatformHooks.getInt(stack, "posY");
                int tz = PlatformHooks.getInt(stack, "posZ");

                boolean withinReach = Math.sqrt((double) (x - tx) * (x - tx) + (double) (y - ty) * (y - ty) + (double) (z - tz) * (z - tz)) <= 100;
                boolean withinAngle = (x - tx) * (x - tx) + (z - tz) * (z - tz) <= (y - ty) * (y - ty);

                if (!withinReach) {
                    if (player != null) player.sendSystemMessage(Component.translatable("item.hbm_m.mirror_tool.reach").withStyle(ChatFormatting.RED));
                } else if (!withinAngle) {
                    if (player != null) player.sendSystemMessage(Component.translatable("item.hbm_m.mirror_tool.angle").withStyle(ChatFormatting.RED));
                } else if (world.getBlockEntity(clicked) instanceof SolarMirrorBlockEntity mirror) {
                    mirror.setTarget(tx, ty, tz);
                }
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        for (String s : Component.translatable("item.hbm_m.mirror_tool.desc").getString().split("\\$"))
            list.add(Component.literal(s).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        return ImmutableMultimap.of(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 2, AttributeModifier.Operation.ADDITION));
    }
}
