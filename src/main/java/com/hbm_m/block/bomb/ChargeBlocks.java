package com.hbm_m.block.bomb;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.explosion.ExplosionNT;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.particle.helper.ExplosionSmallCreator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/** Die vier Haftladungen des Originals ({@code BlockChargeDynamite/Miner/C4/Semtex}). */
public final class ChargeBlocks {

    private ChargeBlocks() {}

    private static void remove(Level world, BlockPos pos) {
        BlockChargeBase.safe = true;
        world.removeBlock(pos, false);
        BlockChargeBase.safe = false;
    }

    /** 1:1 {@code BlockChargeDynamite}: ExplosionNT 4, kleine Explosionswolke. */
    public static class Dynamite extends BlockChargeBase {
        public Dynamite(Properties p) { super(p); }

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            if (world instanceof ServerLevel server) {
                remove(world, pos);
                new ExplosionNT(world, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4F).explode();
                ExplosionSmallCreator.composeEffect(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 3F, 1.25F);
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.UNDEFINED;
        }
    }

    /** 1:1 {@code BlockChargeMiner}: wie Dynamit, aber ohne Schaden und mit allen Drops. */
    public static class Miner extends BlockChargeBase {
        public Miner(Properties p) { super(p); }

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            if (world instanceof ServerLevel server) {
                remove(world, pos);
                ExplosionNT exp = new ExplosionNT(world, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4F);
                exp.addAllAttrib(ExplosionNT.ExAttrib.NOHURT, ExplosionNT.ExAttrib.ALLDROP);
                exp.explode();
                ExplosionSmallCreator.composeEffect(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 3F, 1.25F);
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.UNDEFINED;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(stack, level, list, flag);
            list.add(Component.literal("Will drop all blocks.").withStyle(ChatFormatting.BLUE));
            list.add(Component.literal("Does not do damage.").withStyle(ChatFormatting.BLUE));
        }
    }

    /** 1:1 {@code BlockChargeC4}: VNT 15 (Aufloesung 32), keine Drops, Standard-Schaden. */
    public static class C4 extends BlockChargeBase {
        public C4(Properties p) { super(p); }

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            if (world instanceof ServerLevel server) {
                remove(world, pos);
                ExplosionVNT xnt = new ExplosionVNT(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15F);
                xnt.setBlockAllocator(new BlockAllocatorStandard(32));
                xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                xnt.setEntityProcessor(new EntityProcessorStandard());
                xnt.setPlayerProcessor(new PlayerProcessorStandard());
                xnt.explode();
                ExplosionCreator.composeEffectSmall(server, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.UNDEFINED;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(stack, level, list, flag);
            list.add(Component.literal("Does not drop blocks.").withStyle(ChatFormatting.BLUE));
        }
    }

    /** 1:1 {@code BlockChargeSemtex}: VNT 10 (Aufloesung 32), alle Drops mit Glueck III, kein Schaden. */
    public static class Semtex extends BlockChargeBase {
        public Semtex(Properties p) { super(p); }

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            if (world instanceof ServerLevel server) {
                remove(world, pos);
                ExplosionVNT xnt = new ExplosionVNT(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10F);
                xnt.setBlockAllocator(new BlockAllocatorStandard(32));
                xnt.setBlockProcessor(new BlockProcessorStandard().setAllDrop().setFortune(3));
                xnt.explode();
                ExplosionCreator.composeEffectSmall(server, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.UNDEFINED;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(stack, level, list, flag);
            list.add(Component.literal("Will drop all blocks.").withStyle(ChatFormatting.BLUE));
            list.add(Component.literal("Does not do damage.").withStyle(ChatFormatting.BLUE));
            list.add(Component.literal("").withStyle(ChatFormatting.BLUE));
            list.add(Component.literal("Fortune III").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
