package com.hbm_m.item.tool;

import java.util.List;

import com.hbm_m.entity.item.EntityBoatRubber;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.items.tool.ItemBoatRubber}: setzt ein {@link EntityBoatRubber} auf den angeschauten Block
 * (Reichweite 5, Wasserquellen zaehlen als Treffer), Blickrichtung auf 90 Grad gerundet.
 */
public class ItemBoatRubber extends Item {

    public ItemBoatRubber(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        Vec3 pos = player.getEyePosition();
        float compZ = Mth.cos(-yaw * 0.017453292F - (float) Math.PI);
        float compX = Mth.sin(-yaw * 0.017453292F - (float) Math.PI);
        float mult = -Mth.cos(-pitch * 0.017453292F);
        float lookY = Mth.sin(-pitch * 0.017453292F);
        float lookX = compX * mult;
        float lookZ = compZ * mult;
        double reach = 5.0D;

        Vec3 target = pos.add((double) lookX * reach, (double) lookY * reach, (double) lookZ * reach);
        BlockHitResult mop = world.clip(new ClipContext(pos, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));

        if (mop.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 look = player.getViewVector(1.0F);
        boolean flag = false;
        double width = 1.0D;
        List<Entity> list = world.getEntities(player, player.getBoundingBox().expandTowards(look.x * reach, look.y * reach, look.z * reach).inflate(width));

        for (Entity entity : list) {
            if (entity.isPickable()) {
                float f10 = entity.getPickRadius();
                AABB axisalignedbb = entity.getBoundingBox().inflate(f10);

                if (axisalignedbb.contains(pos)) {
                    flag = true;
                }
            }
        }

        if (flag) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos hit = mop.getBlockPos();
        int x = hit.getX();
        int y = hit.getY();
        int z = hit.getZ();

        if (world.getBlockState(hit).is(Blocks.SNOW)) {
            --y;
        }

        EntityBoatRubber entityboat = new EntityBoatRubber(world, (double) ((float) x + 0.5F), (double) ((float) y + 1.0F), (double) ((float) z + 0.5F));
        entityboat.setYRot((float) (((Mth.floor((double) (player.getYRot() * 4.0F / 360.0F) + 0.5D) & 3) - 1) * 90));

        if (!world.noCollision(entityboat, entityboat.getBoundingBox().deflate(0.1D))) {
            return InteractionResultHolder.pass(stack);
        }

        if (!world.isClientSide) {
            world.addFreshEntity(entityboat);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Original gibt nur den Stapel zurueck (kein Armschwung)
        return InteractionResultHolder.consume(stack);
    }
}
