package com.hbm_m.platform;

/**
 * Серверно-безопасные «рендер»-уведомления, вызываемые из ОБЩЕГО кода
 * (Block/BlockEntity). Живут ОТДЕЛЬНО от {@link RenderHooks}: сигнатуры методов
 * этого класса ссылаются только на common-типы, поэтому загрузка класса на
 * dedicated server верификатором JVM безопасна (у RenderHooks в дескрипторах
 * методов — Minecraft/BakedModel/BufferSource и другой client-only классовый
 * набор, его загрузка на сервере риск ´NoClassDefFoundError´).
 *
 * <p>Тела клиентских веток ссылаются на client-классы (NucleusDispatcherBypass,
 * DoorAnimationDelayHelper) лениво — как в ванильных dist-хелперах: ссылки
 * резолвятся HotSpot только при фактическом выполнении ветки (клиент).
 */
public final class CommonRenderHooks {
    private CommonRenderHooks() {}

    /**
     * Client-safe notification when a multiblock part/casing is removed.
     * Routes to NucleusDispatcherBypass on the client side without referencing
     * client classes on dedicated servers.
     */
    public static void onMultiblockPartRemoved(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos partPos, net.minecraft.core.BlockPos controllerPos) {
        if (level != null && level.isClientSide() && controllerPos != null) {
            com.hbm_m.multiblock.TeardownTrace.log("channel B: CommonRenderHooks.onMultiblockPartRemoved part={} -> controller={}",
                    partPos.toShortString(), controllerPos.toShortString());
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                com.hbm_m.client.render.NucleusDispatcherBypass.onMachineControllerRemoved(level, controllerPos);
            }
        } else {
            com.hbm_m.multiblock.TeardownTrace.trace(
                    "channel B: onMultiblockPartRemoved NO-OP levelNull={} clientSide={} controllerPosNull={} part={}",
                    level == null, level != null && level.isClientSide(), controllerPos == null,
                    partPos == null ? "?" : partPos.toShortString());
        }
    }

    public static void onDoorSkinChanged(net.minecraft.world.level.block.entity.BlockEntity door) {
        if (door != null && door.getLevel() != null && door.getLevel().isClientSide()) {
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                if (door instanceof com.hbm_m.block.entity.doors.DoorBlockEntity dbe) {
                    com.hbm_m.client.overlay.DoorAnimationDelayHelper.clear(dbe);
                }
                com.hbm_m.client.render.NucleusDispatcherBypass.onDoorSkinChanged(door);
            }
        }
    }

    public static void onDoorRemoved(net.minecraft.world.level.block.entity.BlockEntity door) {
        if (door != null && door.getLevel() != null && door.getLevel().isClientSide()) {
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                if (door instanceof com.hbm_m.block.entity.doors.DoorBlockEntity dbe) {
                    com.hbm_m.client.overlay.DoorAnimationDelayHelper.clear(dbe);
                }
                com.hbm_m.client.render.NucleusDispatcherBypass.onMachineRemoved(door);
            }
        }
    }
}
