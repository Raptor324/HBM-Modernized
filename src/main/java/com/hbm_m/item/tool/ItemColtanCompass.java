package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.worldgen.ColtanDepositFeature;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemColtanCompass} ({@code coltan_tool}): der Server schreibt beim ersten Tick die
 * Lage der Coltan-Lagerstaette ({@code colX}/{@code colZ}, siehe {@link ColtanDepositFeature}) ins NBT, der Client
 * zeigt die Entfernung an und die Nadel ({@code TextureColtass}, Eigenschaft {@code hbm_m:angle}) zeigt dorthin.
 */
public class ItemColtanCompass extends Item {

    public static final int ID_COMPASS = 2;

    public static int lastX = 0;
    public static int lastZ = 0;
    public static long lease = 0;

    public ItemColtanCompass(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Points towards the coltan deposit."));
        list.add(Component.literal("The deposit is a large area where coltan ore spawns like standard ore,"));
        list.add(Component.literal("it's not one large blob of ore on that exact location."));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean inhand) {

        if (world.isClientSide) {
            if (stack.hasTag()) {
                lastX = stack.getTag().getInt("colX");
                lastZ = stack.getTag().getInt("colZ");
                lease = System.currentTimeMillis() + 1000;

                double dx = entity.getX() - lastX;
                double dz = entity.getZ() - lastZ;
                com.hbm_m.client.overlay.OverlayInfoToast.show(Component.literal(((int) Math.sqrt(dx * dx + dz * dz)) + "m"), 20, ID_COMPASS);
            }

            if (lease < System.currentTimeMillis()) {
                lastX = 0;
                lastZ = 0;
            }

        } else {
            if (!stack.hasTag() && world instanceof ServerLevel server) {
                CompoundTag tag = new CompoundTag();
                int[] col = ColtanDepositFeature.depositCenter(server.getSeed());
                tag.putInt("colX", col[0]);
                tag.putInt("colZ", col[1]);
                stack.setTag(tag);
            }
        }
    }

    // --- TextureColtass.updateCompass (Client) ---
    private static double currentAngle;
    private static double angleDelta;
    private static long lastFrame = -1;

    /**
     * Bildnummer 0..frames-1 wie {@code TextureColtass.updateAnimation}: Ziel angepeilt, solange die Lease gilt und die
     * Welt eine Oberwelt ist, sonst Zufallswinkel; die Nadel schwingt gedaempft nach (Delta 0.1, Daempfung 0.8).
     * Einmal pro Spieltick fortgeschrieben, wie die Texturanimation.
     */
    public static float needleFrame(@Nullable Level world, double x, double z, double yaw, int frames) {
        long now = world != null ? world.getGameTime() : 0;
        if (now != lastFrame) {
            lastFrame = now;
            double angle;
            boolean hasTarget = !(lastX == 0 && lastZ == 0);

            if (world != null && hasTarget && world.dimensionType().natural() && lease > System.currentTimeMillis()) {
                double d4 = (double) lastX - x;
                double d5 = (double) lastZ - z;
                yaw %= 360.0D;
                angle = -((yaw - 90.0D) * Math.PI / 180.0D - Math.atan2(d5, d4));
            } else {
                angle = Math.random() * Math.PI * 2.0D;
            }

            double d6;
            for (d6 = angle - currentAngle; d6 < -Math.PI; d6 += (Math.PI * 2D)) { }
            while (d6 >= Math.PI) d6 -= (Math.PI * 2D);
            if (d6 < -1.0D) d6 = -1.0D;
            if (d6 > 1.0D) d6 = 1.0D;

            angleDelta += d6 * 0.1D;
            angleDelta *= 0.8D;
            currentAngle += angleDelta;
        }

        int i;
        for (i = (int) ((currentAngle / (Math.PI * 2D) + 1.0D) * (double) frames) % frames; i < 0; i = (i + frames) % frames) { }
        return i;
    }
}
