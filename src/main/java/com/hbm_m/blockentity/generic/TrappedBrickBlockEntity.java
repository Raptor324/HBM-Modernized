package com.hbm_m.blockentity.generic;

import com.hbm_m.platform.PlatformHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.JungleBricks.Trapped;
import com.hbm_m.block.generic.JungleBricks.Trapped.Trap;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.projectile.RubbleEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code TileEntityTrappedBrick}: Melder-Fallen (Steinschlag, Pfeile, Saeule, Zombie, Spinnen) loesen bei Spielern im Meldebereich aus. */
public class TrappedBrickBlockEntity extends BlockEntity {

    private AABB detector = null;
    private Direction dir = null;

    public TrappedBrickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAPPED_BRICK.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, TrappedBrickBlockEntity te) {
        if (te.detector == null) te.setDetector();
        if (!world.getEntitiesOfClass(Player.class, te.detector).isEmpty()) te.trigger();
    }

    private Trap trap() { return getBlockState().getValue(Trapped.TRAP); }

    private void trigger() {
        Level w = level;
        int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();

        switch (trap()) {
            case FALLING_ROCKS -> {
                for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++)
                    w.addFreshEntity(RubbleEntity.create(w, xCoord - 0.5 + x, yCoord - 0.5, zCoord - 0.5 + z, ModBlocks.REINFORCED_STONE.get().defaultBlockState()));
            }
            case ARROW, FLAMING_ARROW -> {
                int ox = dir == null ? 0 : dir.getStepX(), oz = dir == null ? 0 : dir.getStepZ();
                //? if < 1.21.1 {
                Arrow arrow = new Arrow(w, xCoord + 0.5 + ox, yCoord + 0.5, zCoord + 0.5 + oz);
                //?} else {
                /*Arrow arrow = new Arrow(w, xCoord + 0.5 + ox, yCoord + 0.5, zCoord + 0.5 + oz, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
                *///?}
                arrow.setDeltaMovement(ox, 0, oz);
                if (trap() == Trap.FLAMING_ARROW) PlatformHooks.setSecondsOnFire(arrow, 60);
                w.addFreshEntity(arrow);
            }
            case PILLAR -> {
                for (int i = 0; i < 3; i++) w.setBlockAndUpdate(worldPosition.below(1 + i), ModBlocks.CONCRETE_PILLAR.get().defaultBlockState());
            }
            case POISON_DART -> { } // TBI im Original
            case ZOMBIE -> {
                Zombie zombie = EntityType.ZOMBIE.create(w);
                if (zombie != null) {
                    zombie.setPos(xCoord + 0.5, yCoord + 1, zCoord + 0.5);
                    ItemStack held = switch (w.random.nextInt(3)) {
                        case 0 -> new ItemStack(ModItems.CHERNOBYLSIGN.get());
                        case 1 -> new ItemStack(ModItems.COBALT_SWORD.get());
                        default -> new ItemStack(ModItems.CMB_HOE.get());
                    };
                    zombie.setItemSlot(EquipmentSlot.MAINHAND, held);
                    zombie.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
                    w.addFreshEntity(zombie);
                }
            }
            case SPIDERS -> {
                for (int i = 0; i < 3; i++) {
                    CaveSpider spider = EntityType.CAVE_SPIDER.create(w);
                    if (spider == null) continue;
                    spider.setPos(xCoord + 0.5, yCoord - 1, zCoord + 0.5);
                    w.addFreshEntity(spider);
                }
            }
            default -> { }
        }

        w.playSound(null, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 0.6F);
        w.setBlockAndUpdate(worldPosition, ModBlocks.BRICK_JUNGLE.get().defaultBlockState());
    }

    private void setDetector() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        switch (trap()) {
            case FALLING_ROCKS, SPIDERS -> { detector = new AABB(x - 1, y - 3, z - 1, x + 2, y, z + 2); return; }
            case PILLAR -> { detector = new AABB(x + 0.2, y - 3, z + 0.2, x + 0.8, y, z + 0.8); return; }
            case ARROW, FLAMING_ARROW, POISON_DART -> { setDetectorDirectional(); return; }
            case ZOMBIE -> { detector = new AABB(x - 1, y + 1, z - 1, x + 2, y + 2, z + 2); return; }
            default -> { }
        }
        detector = new AABB(x, y, z, x + 1, y + 1, z + 1);
    }

    private void setDetectorDirectional() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        List<Direction> dirs = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        Collections.shuffle(dirs);

        for (Direction d : dirs) {
            if (level.getBlockState(worldPosition.relative(d)).isAir()) {
                double minX = x + 0.4, minY = y + 0.4, minZ = z + 0.4, maxX = x + 0.6, maxY = y + 0.6, maxZ = z + 0.6;
                if (d.getStepX() > 0) maxX += 3; else if (d.getStepX() < 0) minX -= 3;
                if (d.getStepZ() > 0) maxZ += 3; else if (d.getStepZ() < 0) minZ -= 3;
                detector = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
                dir = d;
                return;
            }
        }
        detector = new AABB(x, y, z, x + 1, y + 1, z + 1);
    }
}
