package com.hbm_m.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;

//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}
//? if fabric {
/*import dev.architectury.registry.menu.MenuRegistry;
*///?}

public final class NetworkHooksCompat {
    private NetworkHooksCompat() {}

    public static void openScreen(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        //? if forge {
        NetworkHooks.openScreen(player, provider, pos);
        //?}
        //? if fabric {
        /*MenuRegistry.openExtendedMenu(player, provider, buf -> buf.writeBlockPos(pos));
        *///?}
    }

    public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity) {
        //? if forge {
        return NetworkHooks.getEntitySpawningPacket(entity);
        //?}
        //? if fabric {
        /*return new ClientboundAddEntityPacket(entity);
        *///?}
    }
}

