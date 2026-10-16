package com.hbm_m.item.tools_and_armor;

import com.hbm_m.entity.drone.EntityDeliveryDrone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Port von {@code com.hbm.items.tool.ItemDrone} (1.7.10 Original). Platziert per Rechtsklick auf die
 * Oberseite eines Blocks eine {@link EntityDeliveryDrone} - eine der vier "Patrol"-Varianten
 * (Express x/ Chunk-Loading x). Die 5. Original-Variante (REQUEST) wird NICHT hier abgedeckt: im
 * Original ist sie ebenfalls dieses Item, spawnt aber nie per Rechtsklick (nur indirekt aus
 * {@code TileEntityDroneDock}) - hier als eigenes, nicht direkt platzierbares Item (siehe
 * {@code ModItems.DRONE_REQUEST}) modelliert, da 1.20.1 kein Metadaten-System mehr hat und diese
 * fuenf Varianten deshalb (wie ueberall sonst in diesem Port, z.B. Upgrade-Tiers) als eigenstaendige
 * Items statt Damage-Werten gefuehrt werden.
 */
public class ItemDrone extends Item {

    private final boolean express;
    private final boolean chunkLoading;

    public ItemDrone(Properties properties, boolean express, boolean chunkLoading) {
        super(properties);
        this.express = express;
        this.chunkLoading = chunkLoading;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        if (!level.isClientSide) {
            BlockPos pos = context.getClickedPos();
            EntityDeliveryDrone drone = EntityDeliveryDrone.create(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, express, chunkLoading);
            level.addFreshEntity(drone);

            if (player != null && !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
