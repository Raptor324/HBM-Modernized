package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.IPipelineBase;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.item.ITooltipProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code com.hbm.items.tool.ItemWrench}: Schwert, das Rohrleitungs-Anker verbindet und Gegner wegstoesst. */
public class ItemWrench extends SwordItem implements ITooltipProvider {

    /** Original {@code ServerProxy.ID_WRENCH}. */
    public static final int ID_WRENCH = 13;

    public ItemWrench(Tier mat, Properties properties) {
        super(mat, 4, -2.4F, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level world = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();

        if (player != null && !player.isShiftKeyDown()) {

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof IMultiblockPart part && part.getControllerPos() != null) {
                pos = part.getControllerPos();
                be = world.getBlockEntity(pos);
            }

            if (be instanceof IPipelineBase second) {

                if (stack.getTag() == null)
                    stack.setTag(new CompoundTag());

                if (!stack.getTag().contains("x")) {
                    stack.setTag(new CompoundTag());
                    stack.getTag().putInt("x", pos.getX());
                    stack.getTag().putInt("y", pos.getY());
                    stack.getTag().putInt("z", pos.getZ());

                    if (!world.isClientSide) {
                        player.sendSystemMessage(Component.literal("Pipe start"));
                    }
                } else if (!world.isClientSide) {

                    BlockPos p1 = new BlockPos(stack.getTag().getInt("x"), stack.getTag().getInt("y"), stack.getTag().getInt("z"));

                    if (world.getBlockEntity(p1) instanceof IPipelineBase first) {

                        switch (IPipelineBase.canConnect(first, second)) {
                            case 0:
                                first.addConnection(pos);
                                second.addConnection(p1);
                                player.sendSystemMessage(Component.literal("Pipe end"));
                                break;
                            case 1: player.sendSystemMessage(Component.literal("Pipe error - Pipes are not the same type")); break;
                            case 2: player.sendSystemMessage(Component.literal("Pipe error - Cannot connect to the same pipe anchor")); break;
                            case 3: player.sendSystemMessage(Component.literal("Pipe error - Pipe anchor is too far away")); break;
                            case 4: player.sendSystemMessage(Component.literal("Pipe error - Pipe anchor fluid types do not match")); break;
                            default: break;
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("Pipe error"));
                    }

                    stack.getTag().remove("x");
                    stack.getTag().remove("y");
                    stack.getTag().remove("z");
                }

                player.swing(ctx.getHand());
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity attacker) {
        Vec3 vec = attacker.getLookAngle();
        entity.setDeltaMovement(entity.getDeltaMovement().add(vec.x * 0.5, vec.y * 0.5, vec.z * 0.5));
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 0.75F);
        // Original gibt false zurueck: keine Abnutzung
        return false;
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (itemstack.getTag() != null) {
            list.add(Component.literal("Pipe start x: " + itemstack.getTag().getInt("x")));
            list.add(Component.literal("Pipe start y: " + itemstack.getTag().getInt("y")));
            list.add(Component.literal("Pipe start z: " + itemstack.getTag().getInt("z")));
        } else {
            list.add(Component.literal("Right-click anchor to connect"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean inhand) {
        if (world.isClientSide && stack.getTag() != null) {
            Vec3 vec = new Vec3(
                    entity.getX() - stack.getTag().getInt("x"),
                    entity.getY() - stack.getTag().getInt("y"),
                    entity.getZ() - stack.getTag().getInt("z"));
            com.hbm_m.client.overlay.OverlayInfoToast.show(stack.getHoverName().copy().append(": " + ((int) vec.length()) + "m"), 20, ID_WRENCH);
        }
    }
}
