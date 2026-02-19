package com.hbm_m.multiblock;

import java.util.Set;

import com.hbm_m.main.MainRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LadderClimbHandler {

    private static final String TAG_WAS_ON_LADDER = "hbm_was_on_ladder";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getAbilities().flying || player.isSpectator()) return;
        if (player.onClimbable()) return;

        Level level = player.level();
        
        // 1. ПРОВЕРКА КАСАНИЯ (с учетом разрешенных сторон)
        // false = проверяем по всему телу (ноги + голова)
        boolean touchingLadder = checkTouchingLadder(level, player, false);
        
        boolean wasOnLadder = player.getTags().contains(TAG_WAS_ON_LADDER);
        boolean isShiftKeyDown = player.isShiftKeyDown();

        // Липкая логика
        if (!touchingLadder && wasOnLadder && isShiftKeyDown) {
            if (tryFindLadderExtended(level, player)) touchingLadder = true;
        }

        if (!touchingLadder) {
            if (wasOnLadder) player.removeTag(TAG_WAS_ON_LADDER);
            return;
        }

        // 2. ФИЛЬТРЫ (Валидация начала подъема)
        if (player.onGround()) {
            // ИСПРАВЛЕНИЕ:
            // Вместо проверки блока, в котором стоит игрок (это воздух),
            // мы проверяем касание лестницы строго на уровне ног (true).
            // Если лестница только у головы, а у ног стена/пустота — не лезем.
            if (!checkTouchingLadder(level, player, true)) {
                return;
            }
        }

        if (!wasOnLadder) player.addTag(TAG_WAS_ON_LADDER);
        
        Vec3 motion = player.getDeltaMovement();
        double newY = motion.y;
        boolean horizontalCollision = player.horizontalCollision;

        // 3. ФИЗИКА
        if (horizontalCollision && newY < 0.2) {
            newY = 0.2; 
        } else if (isShiftKeyDown) {
            newY = 0.0; 
        } else if (newY < -0.15) {
            newY = -0.15; 
        }

        if (newY != motion.y) {
            player.setDeltaMovement(motion.x, newY, motion.z);
            player.fallDistance = 0;
        }
    }

    /**
     * @param feetOnly true - искать только на уровне ступней (для старта с земли), false - искать по всему хитбоксу.
     */
    private static boolean checkTouchingLadder(Level level, Player player, boolean feetOnly) {
        AABB pBox = player.getBoundingBox();
        
        // Настройка зоны поиска по высоте (Y)
        // Если feetOnly: смотрим от низа игрока до +0.25 блока вверх (уровень голени).
        // Если не feetOnly: смотрим весь хитбокс с небольшим запасом.
        double minY = pBox.minY + (feetOnly ? 0 : -0.05);
        double maxY = feetOnly ? (pBox.minY + 0.25) : (pBox.maxY + 0.05);

        // Расширяем поиск по X/Z, чтобы найти блоки, в которые мы уперлись
        BlockPos min = BlockPos.containing(pBox.minX - 0.05, minY, pBox.minZ - 0.05);
        BlockPos max = BlockPos.containing(pBox.maxX + 0.05, maxY, pBox.maxZ + 0.05);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IMultiblockPart part && part.getPartRole() == PartRole.LADDER) {
                
                Set<Direction> allowed = part.getAllowedClimbSides();
                if (allowed.isEmpty()) continue;

                for (Direction side : allowed) {
                    double x1 = pos.getX(), y1 = pos.getY(), z1 = pos.getZ();
                    double x2 = x1 + 1, y2 = y1 + 1, z2 = z1 + 1;
                    
                    double shrink = 0.02; 
                    double thickness = 0.1; 

                    AABB detectionZone;

                    switch (side) {
                        case WEST: // X-
                            detectionZone = new AABB(x1 - thickness, y1 + shrink, z1 + shrink, x1 + shrink, y2 - shrink, z2 - shrink);
                            break;
                        case EAST: // X+
                            detectionZone = new AABB(x2 - shrink, y1 + shrink, z1 + shrink, x2 + thickness, y2 - shrink, z2 - shrink);
                            break;
                        case NORTH: // Z-
                            detectionZone = new AABB(x1 + shrink, y1 + shrink, z1 - thickness, x2 - shrink, y2 - shrink, z1 + shrink);
                            break;
                        case SOUTH: // Z+
                            detectionZone = new AABB(x1 + shrink, y1 + shrink, z2 - shrink, x2 - shrink, y2 - shrink, z2 + thickness);
                            break;
                        default:
                            continue;
                    }
                    
                    if (pBox.intersects(detectionZone)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isLadderAt(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IMultiblockPart part && part.getPartRole() == PartRole.LADDER;
    }

    private static boolean tryFindLadderExtended(Level level, Player player) {
        AABB bounds = player.getBoundingBox().inflate(0.15, 0, 0.15);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), 
                                                   BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            if (isLadderAt(level, pos)) return true;
        }
        return false;
    }
}