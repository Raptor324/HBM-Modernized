package com.hbm_m.util;

import java.util.HashSet;
import java.util.Set;

import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.entity.mob.EntityCreeperNuclear;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.HazmatRegistry;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Утилиты контаминации. Порт {@link com.hbm.util.ContaminationUtil} (1.7.10).
 */
public final class ContaminationUtil {

    private static final Set<Class<?>> IMMUNE_ENTITIES = new HashSet<>();

    static {
        IMMUNE_ENTITIES.add(EntityCreeperNuclear.class);
        IMMUNE_ENTITIES.add(MushroomCow.class);
        IMMUNE_ENTITIES.add(Zombie.class);
        IMMUNE_ENTITIES.add(Skeleton.class);
        IMMUNE_ENTITIES.add(Ocelot.class);
        IMMUNE_ENTITIES.add(IRadiationImmune.class);
    }

    private ContaminationUtil() {
    }

    // ── Neutron activation ──────────────────────────────────────────────────
    //
    // 1:1 with CE's ContaminationUtil. An ordinary item left sitting in a neutron flux (the RBMK
    // outgasser's channel, when it holds something with no activation recipe) becomes induced-
    // radioactive: a float is written into the stack's NBT under this key and the carrier is then
    // dosed by it, on top of whatever the item is normally worth. Storage drums decay it back down.
    //
    // The port had none of this, so an unrecognised item in an outgasser simply absorbed the flux
    // and stayed inert - the mechanic that makes leaving your pickaxe in a running reactor a bad
    // idea did not exist.

    public static final String NTM_NEUTRON_NBT_KEY = "ntmNeutron";

    /** Items that are radioactive in their own right never take induced activation on top. */
    public static boolean isRadItem(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return com.hbm_m.hazard.HazardSystem.getHazardLevelFromStack(
                stack, com.hbm_m.hazard.HazardRegistry.RADIATION) > 0;
    }

    /**
     * Adds {@code rad} to the stack's stored activation after decaying what was already there by
     * {@code decay}, and prunes the tag once the value falls below the noise floor.
     *
     * <p>Only single-item stacks can be activated - CE's guard, and a necessary one: the value is
     * per-item, so activating a stack of 64 and then splitting it would multiply the dose.</p>
     *
     * @return true when the stack's NBT actually changed
     */
    public static boolean neutronActivateItem(net.minecraft.world.item.ItemStack stack, float rad, float decay) {
        if (stack == null || stack.isEmpty() || stack.getCount() != 1 || isRadItem(stack)) return false;

        // NBT ueber PlatformHooks: 1.21 hat kein com.hbm_m.platform.PlatformHooks.getItemTag(ItemStack)/setTag() mehr.
        net.minecraft.nbt.CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);
        float prevActivation = tag != null && tag.contains(NTM_NEUTRON_NBT_KEY)
                ? tag.getFloat(NTM_NEUTRON_NBT_KEY) : 0F;

        float newActivation = prevActivation * decay + (rad / stack.getCount());

        if (newActivation < 0.0001F) {
            if (prevActivation > 0 && tag != null) {
                com.hbm_m.platform.PlatformHooks.remove(stack, NTM_NEUTRON_NBT_KEY);
                return true;
            }
            return false;
        }

        if (Math.abs(newActivation - prevActivation) > 1e-6F) {
            com.hbm_m.platform.PlatformHooks.editItemTag(stack,
                    t -> t.putFloat(NTM_NEUTRON_NBT_KEY, newActivation));
            return true;
        }
        return false;
    }

    /** The whole stack's induced dose (the stored per-item value times the count). */
    public static float getNeutronRads(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty() || isRadItem(stack)) return 0F;
        net.minecraft.nbt.CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);
        if (tag == null || !tag.contains(NTM_NEUTRON_NBT_KEY)) return 0F;
        return tag.getFloat(NTM_NEUTRON_NBT_KEY) * stack.getCount();
    }

    public static boolean isContaminated(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack == null ? null : com.hbm_m.platform.PlatformHooks.getItemTag(stack);
        return tag != null && tag.contains(NTM_NEUTRON_NBT_KEY);
    }

    /** Decays every activated stack in a player's inventory except the one they are holding. */
    public static boolean neutronActivateInventory(Player player, float rad, float decay) {
        boolean changed = false;
        var inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == inv.selected) continue;
            if (neutronActivateItem(inv.items.get(i), rad, decay)) changed = true;
        }
        for (net.minecraft.world.item.ItemStack armor : inv.armor) {
            if (neutronActivateItem(armor, rad, decay)) changed = true;
        }
        return changed;
    }

    public static float calculateRadiationMod(LivingEntity entity) {
        if (entity instanceof Player player) {
            float koeff = 10.0F;
            return (float) Math.pow(koeff, -HazmatRegistry.getResistance(player));
        }
        return 1F;
    }

    public static float getRads(Entity e) {
        if (!(e instanceof LivingEntity living)) {
            return 0F;
        }
        if (isRadImmune(e)) {
            return 0F;
        }
        return HbmLivingProps.getRadiation(living);
    }

    public static boolean isRadImmune(Entity e) {
        if (!(e instanceof LivingEntity)) {
            return false;
        }

        Class<?> entityClass = e.getClass();
        for (Class<?> clazz : IMMUNE_ENTITIES) {
            if (clazz.isAssignableFrom(entityClass)) {
                return true;
            }
        }

        if ("cyano.lootable.entities.EntityLootableBody".equals(entityClass.getName())) {
            return true;
        }

        return false;
    }

    public static void applyAsbestos(Entity e, int i) {
        if (!(e instanceof LivingEntity living)) {
            return;
        }
        if (e instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (e instanceof Player player && player.tickCount < 200) {
            return;
        }
        HbmLivingProps.incrementAsbestos(living, i);
    }

    public static void applyDigammaData(Entity e, float f) {
        if (!(e instanceof LivingEntity living)) {
            return;
        }
        if (e instanceof Ocelot) {
            return;
        }
        if (e instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (e instanceof Player player && player.tickCount < 200) {
            return;
        }
        if (living instanceof Player player && ArmorUtil.checkForDigamma(player)) {
            return;
        }
        HbmLivingProps.incrementDigamma(living, f);
    }

    public static void applyDigammaDirect(Entity e, float f) {
        if (!(e instanceof LivingEntity living)) {
            return;
        }
        if (e instanceof IRadiationImmune) {
            return;
        }
        if (e instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        HbmLivingProps.incrementDigamma(living, f);
    }

    public static float getDigamma(Entity e) {
        if (!(e instanceof LivingEntity living)) {
            return 0F;
        }
        return HbmLivingProps.getDigamma(living);
    }

    public static void printGeigerData(Player player) {
        Level world = player.level();

        double eRad = ((int) (HbmLivingProps.getRadiation(player) * 10)) / 10D;
        double rads = ((int) (ChunkRadiationManager.getRadiation(world,
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY()),
                (int) Math.floor(player.getZ())) * 10)) / 10D;
        double env = ((int) (HbmLivingProps.getRadBuf(player) * 10D)) / 10D;

        double res = ((int) (10000D - calculateRadiationMod(player) * 10000D)) / 100D;
        double resKoeff = ((int) (HazmatRegistry.getResistance(player) * 100D)) / 100D;

        player.sendSystemMessage(Component.literal("===== ☢ ")
                .append(Component.translatable("geiger.title"))
                .append(Component.literal(" ☢ ====="))
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(geigerRadLine("geiger.chunkRad", rads, "RAD/s", getRadTierColor(rads)));
        player.sendSystemMessage(geigerRadLine("geiger.envRad", env, "RAD/s", getRadTierColor(env)));
        player.sendSystemMessage(geigerRadLine("geiger.playerRad", eRad, "RAD", getPlayerRadTierColor(eRad)));
        player.sendSystemMessage(Component.translatable("geiger.playerRes")
                .withStyle(ChatFormatting.YELLOW)
                .append(radValueComponent(String.valueOf(res), "% (" + resKoeff + ")", resKoeff > 0 ? ChatFormatting.GREEN : ChatFormatting.WHITE)));
    }

    public static void printDosimeterData(Player player) {
        double env = ((int) (HbmLivingProps.getRadBuf(player) * 10D)) / 10D;
        boolean limit = false;

        if (env > 3.6D) {
            env = 3.6D;
            limit = true;
        }

        player.sendSystemMessage(Component.literal("===== ☢ ")
                .append(Component.translatable("geiger.title.dosimeter"))
                .append(Component.literal(" ☢ ====="))
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("geiger.envRad")
                .withStyle(ChatFormatting.YELLOW)
                .append(radValueComponent((limit ? ">" : "") + env, "RAD/s", getRadTierColor(env))));
    }

    private static MutableComponent geigerRadLine(String labelKey, double value, String unit, ChatFormatting tierColor) {
        return Component.translatable(labelKey)
                .withStyle(ChatFormatting.YELLOW)
                .append(radValueComponent(String.valueOf(value), unit, tierColor));
    }

    private static MutableComponent radValueComponent(String valueText, String unit, ChatFormatting tierColor) {
        return Component.literal(" ")
                .append(Component.literal(valueText).withStyle(tierColor))
                .append(Component.literal(" " + unit).withStyle(tierColor));
    }

    private static ChatFormatting getRadTierColor(double rads) {
        if (rads == 0) {
            return ChatFormatting.GREEN;
        }
        if (rads < 1) {
            return ChatFormatting.YELLOW;
        }
        if (rads < 10) {
            return ChatFormatting.GOLD;
        }
        if (rads < 100) {
            return ChatFormatting.RED;
        }
        if (rads < 1000) {
            return ChatFormatting.DARK_RED;
        }
        return ChatFormatting.DARK_GRAY;
    }

    private static ChatFormatting getPlayerRadTierColor(double eRad) {
        if (eRad < 200) {
            return ChatFormatting.GREEN;
        }
        if (eRad < 400) {
            return ChatFormatting.YELLOW;
        }
        if (eRad < 600) {
            return ChatFormatting.GOLD;
        }
        if (eRad < 800) {
            return ChatFormatting.RED;
        }
        if (eRad < 1000) {
            return ChatFormatting.DARK_RED;
        }
        return ChatFormatting.DARK_GRAY;
    }

    public static String getPreffixFromRad(double rads) {
        return "" + getRadTierColor(rads);
    }

    public static void printDiagnosticData(Player player) {
        double digamma = ((int) (HbmLivingProps.getDigamma(player) * 100)) / 100D;
        double halflife = ((int) ((1D - Math.pow(0.5, digamma)) * 10000)) / 100D;

        player.sendSystemMessage(Component.literal("===== Ϝ ")
                .append(Component.translatable("digamma.title"))
                .append(Component.literal(" Ϝ ====="))
                .withStyle(ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(digammaLine("digamma.playerDigamma", digamma + " DRX"));
        player.sendSystemMessage(digammaLine("digamma.playerHealth", halflife + "%"));
        player.sendSystemMessage(Component.translatable("digamma.playerRes")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(" ").withStyle(ChatFormatting.BLUE))
                .append(Component.literal("N/A").withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    private static MutableComponent digammaLine(String labelKey, String valueText) {
        return Component.translatable(labelKey)
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(" ").withStyle(ChatFormatting.RED))
                .append(Component.literal(valueText).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public enum HazardType {
        RADIATION,
        DIGAMMA
    }

    public enum ContaminationType {
        FARADAY,
        HAZMAT,
        HAZMAT2,
        DIGAMMA,
        DIGAMMA2,
        CREATIVE,
        RAD_BYPASS,
        NONE
    }

    @SuppressWarnings("incomplete-switch")
    public static boolean contaminate(LivingEntity entity, HazardType hazard, ContaminationType cont, float amount) {
        if (hazard == HazardType.RADIATION) {
            float radEnv = HbmLivingProps.getRadEnv(entity);
            HbmLivingProps.setRadEnv(entity, radEnv + amount);
        }

        if (entity instanceof Player player) {
            switch (cont) {
                case FARADAY -> {
                    if (ArmorUtil.checkForFaraday(player)) {
                        return false;
                    }
                }
                case HAZMAT -> {
                    if (ArmorUtil.checkForHazmat(player)) {
                        return false;
                    }
                }
                case HAZMAT2 -> {
                    if (ArmorUtil.checkForHaz2(player)) {
                        return false;
                    }
                }
                case DIGAMMA -> {
                    if (ArmorUtil.checkForDigamma(player) || ArmorUtil.checkForDigamma2(player)) {
                        return false;
                    }
                }
                case DIGAMMA2 -> {
                    if (ArmorUtil.checkForDigamma2(player)) {
                        return false;
                    }
                }
                default -> {
                }
            }

            if ((player.isCreative() || player.isSpectator()) && cont != ContaminationType.NONE && cont != ContaminationType.DIGAMMA2) {
                return false;
            }

            if (player.tickCount < 200) {
                return false;
            }
        }

        if (hazard == HazardType.RADIATION && isRadImmune(entity)) {
            return false;
        }

        switch (hazard) {
            case RADIATION -> HbmLivingProps.incrementRadiation(entity,
                    amount * (cont == ContaminationType.RAD_BYPASS ? 1F : calculateRadiationMod(entity)));
            case DIGAMMA -> HbmLivingProps.incrementDigamma(entity, amount);
        }

        return true;
    }
}
