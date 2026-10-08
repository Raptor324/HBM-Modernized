package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

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
        //? if < 1.21.1 {
        super(mat, 4, -2.4F, properties);
        //?} else {
        /*super(mat, properties.attributes(SwordItem.createAttributes(mat, 4, -2.4F)));
        *///?}
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

                if (StackNbt.read(stack) == null)
                    StackNbt.set(stack, new CompoundTag());

                if (!StackNbt.read(stack).contains("x")) {
                    StackNbt.set(stack, new CompoundTag());
                    StackNbt.tag(stack).putInt("x", pos.getX());
                    StackNbt.tag(stack).putInt("y", pos.getY());
                    StackNbt.tag(stack).putInt("z", pos.getZ());

                    if (!world.isClientSide) {
                        player.sendSystemMessage(Component.literal("Pipe start"));
                    }
                } else if (!world.isClientSide) {

                    BlockPos p1 = new BlockPos(StackNbt.read(stack).getInt("x"), StackNbt.read(stack).getInt("y"), StackNbt.read(stack).getInt("z"));

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

                    StackNbt.tag(stack).remove("x");
                    StackNbt.tag(stack).remove("y");
                    StackNbt.tag(stack).remove("z");
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
        if (StackNbt.read(itemstack) != null) {
            list.add(Component.literal("Pipe start x: " + StackNbt.read(itemstack).getInt("x")));
            list.add(Component.literal("Pipe start y: " + StackNbt.read(itemstack).getInt("y")));
            list.add(Component.literal("Pipe start z: " + StackNbt.read(itemstack).getInt("z")));
        } else {
            list.add(Component.literal("Right-click anchor to connect"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean inhand) {
        if (world.isClientSide && StackNbt.read(stack) != null) {
            Vec3 vec = new Vec3(
                    entity.getX() - StackNbt.read(stack).getInt("x"),
                    entity.getY() - StackNbt.read(stack).getInt("y"),
                    entity.getZ() - StackNbt.read(stack).getInt("z"));
            com.hbm_m.client.overlay.OverlayInfoToast.show(stack.getHoverName().copy().append(": " + ((int) vec.length()) + "m"), 20, ID_WRENCH);
        }
    }
}
