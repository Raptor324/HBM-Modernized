package com.hbm_m.item.custom.tools_and_armor;

import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.client.overlay.DoorModelSelectionScreen;
import com.hbm_m.multiblock.IMultiblockPart;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Отвертка для настройки дверей.
 * ПКМ по двери (контроллеру или любой части) — открывает GUI, дверь не открывается.
 */
public class ScrewdriverItem extends Item {

    public ScrewdriverItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        DoorBlockEntity doorEntity = resolveDoorController(level, pos);
        if (doorEntity == null) return InteractionResult.PASS;

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> openGui(doorEntity));
        }
        // CONSUME явно помечает взаимодействие как обработанное — блок use() не вызывается
        return InteractionResult.CONSUME;
    }

    private static DoorBlockEntity resolveDoorController(Level level, BlockPos clickedPos) {
        BlockEntity be = level.getBlockEntity(clickedPos);
        if (be instanceof DoorBlockEntity doorBE && doorBE.isController()) {
            return doorBE;
        }
        if (be instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockEntity ctrlBe = level.getBlockEntity(controllerPos);
                if (ctrlBe instanceof DoorBlockEntity doorBE && doorBE.isController()) {
                    return doorBE;
                }
            }
        }
        return null;
    }

    private static void openGui(DoorBlockEntity doorEntity) {
        Minecraft.getInstance().setScreen(new DoorModelSelectionScreen(
                doorEntity.getBlockPos(),
                doorEntity.getDoorDeclId(),
                doorEntity.getModelSelection()
        ));
    }
}
