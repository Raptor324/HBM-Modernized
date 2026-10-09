//? if forge {
package com.hbm_m.main;

import com.hbm_m.platform.PlatformHooks;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.block.IStepTickReceiver;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ArmorUtil;
import com.hbm_m.util.ShadyUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;
import com.hbm_m.powerarmor.ModArmorFSB;
import com.hbm_m.powerarmor.IAttackHandler;
import com.hbm_m.powerarmor.IDamageHandler;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;
import net.minecraftforge.fml.common.Mod;

/**
 * Port von {@code com.hbm.main.ModEventHandler} (1.7.10), soweit nicht schon anderswo abgedeckt:
 * <ul>
 *   <li>Blockbruch (Kohlegas, Blei aus Bloecken) - {@code event/LungGasHandler}</li>
 *   <li>Gneis-Erfolg + 500 XP, Schleimball, Handwerk/Schmelzen-Erfolge, Boxcar-Creeper -
 *       {@code advancement/AchievementHandler}</li>
 *   <li>Inventar-Hazards des Spielers - {@code event/PlayerHazardHandler}, liegende Items -
 *       {@code event/HazardEventHandler}, Mod-Hazards/{@code modUpdate} - {@code ArmorModTickHandler}</li>
 *   <li>{@code EntityEffectHandler.onUpdate} - Mixin {@code LivingEntityRadiationMixin}</li>
 *   <li>Server-Tick (RTTY, Drohnen, Radar, UniNodespace) - {@code MainRegistry}</li>
 *   <li>{@code PermaSyncPacket} - {@code ExtPropPacket}, jeden Tick aus {@code EntityEffectHandler}</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HbmForgeEvents {

    private static final Random rand = new Random();
    private static final UUID fopSpeed = UUID.fromString("e5a8c95d-c7a0-4ecf-8126-76fb8c949389");

    private HbmForgeEvents() {}

    /** {@code getEquipmentInSlot(1..4)}: Stiefel, Hose, Brust, Helm. */
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    // ---------------------------------------------------------------- Custom Machines

    /** Original: Steuerungsrezepte aus hbmCustomMachines.json per CraftingManager - hier in den RecipeManager gelegt. */
    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        injectCustomMachineRecipes(event.getServer());
    }

    /** Nach /reload ist der RecipeManager neu - die Steuerungsrezepte wieder einhaengen und an alle schicken. */
    @SubscribeEvent
    public static void onDatapackSync(net.minecraftforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() == null && injectCustomMachineRecipes(event.getPlayerList().getServer())) {
            event.getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket(
                    event.getPlayerList().getServer().getRecipeManager().getRecipes()));
        }
    }

    private static boolean injectCustomMachineRecipes(net.minecraft.server.MinecraftServer server) {
        var rm = server.getRecipeManager();
        java.util.List<net.minecraft.world.item.crafting.Recipe<?>> all = new java.util.ArrayList<>(rm.getRecipes());
        boolean changed = false;
        for (var r : com.hbm_m.config.CustomMachineConfigJSON.controllerRecipes) {
            if (rm.byKey(r.getId()).isEmpty()) { all.add(r); changed = true; }
        }
        if (changed) rm.replaceRecipes(all);
        return changed;
    }

    // ---------------------------------------------------------------- Spieler-Lebenszyklus

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        HbmPlayerProps data = HbmPlayerProps.getData(event.getEntity());
        data.setKeyPressed(EnumKeybind.JETPACK, false);
        data.setKeyPressed(EnumKeybind.DASH, false);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Original serialisiert die kompletten Props des alten Spielers in den neuen.
        HbmPlayerProps old = HbmPlayerProps.getData(event.getOriginal());
        HbmPlayerProps now = HbmPlayerProps.getData(event.getEntity());
        now.read(old.write());
        now.save();
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (ShadyUtil.is(player, ShadyUtil.Dr_Nostalgia, "Dr_Nostalgia")) {
            // Original gibt zusaetzlich ModItems.hat (nossy_hat, ArmorHat) - folgt mit der Ruestungsrunde.
            if (!player.getInventory().hasAnyOf(Set.of(ModItems.BETA.get())))
                player.getInventory().add(new ItemStack(ModItems.BETA.get()));
        }
    }

    // ---------------------------------------------------------------- Tod

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityDeathFirst(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);

            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.ArmorItem && ArmorModificationHelper.hasMods(stack)) {

                ItemStack revive = ArmorModificationHelper.pryMods(stack)[ArmorModificationHelper.extra];

                if (revive != null && !revive.isEmpty()) {

                    //Classic revive
                    if (revive.getItem() instanceof com.hbm_m.armormod.item.ItemModRevive) {
                        revive.setDamageValue(revive.getDamageValue() + 1);

                        if (revive.getDamageValue() >= revive.getMaxDamage()) {
                            ArmorModificationHelper.removeMod(stack, ArmorModificationHelper.extra);
                        } else {
                            ArmorModificationHelper.applyMod(stack, revive);
                        }

                        entity.setHealth(entity.getMaxHealth());
                        entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 60, 99));
                        event.setCanceled(true);
                        return;
                    }

                    //Shackles
                    if (revive.getItem() instanceof com.hbm_m.armormod.item.ItemModShackles && HbmLivingProps.getRadiation(entity) < 1000F) {

                        revive.setDamageValue(revive.getDamageValue() + 1);

                        int dmg = revive.getDamageValue();
                        ArmorModificationHelper.applyMod(stack, revive);

                        entity.setHealth(entity.getMaxHealth());
                        HbmLivingProps.incrementRadiation(entity, dmg * dmg);
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        HbmLivingProps.setRadiation(entity, 0);

        Level level = entity.level();
        if (level.isClientSide) return;

        // Original enableCataclysm: bei jedem Tod faellt ein brennender FOEQ aus 500 Bloecken Hoehe
        if (com.hbm_m.config.ModClothConfig.get().enableCataclysm) {
            com.hbm_m.entity.projectile.EntityBurningFOEQ foeq = com.hbm_m.entity.ModEntities.BURNING_FOEQ.get().create(level);
            if (foeq != null) {
                foeq.moveTo(entity.getX(), 500, entity.getZ(), 0.0F, 0.0F);
                level.addFreshEntity(foeq);
            }
        }

        if (entity.getUUID().toString().equals(ShadyUtil.HbMinecraft) || entity.getName().getString().equals("HbMinecraft")) {
            entity.spawnAtLocation(ModItems.BOOK_OF_.get(), 0);
        }

        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            if (event.getSource().getEntity() instanceof Player && !(event.getSource().getEntity() instanceof FakePlayer)) {

                var rng = entity.getRandom();

                if (entity instanceof Spider && rng.nextInt(500) == 0) {
                    entity.spawnAtLocation(ModItems.SPIDER_MILK.get(), 0);
                }

                if (entity instanceof CaveSpider && rng.nextInt(100) == 0) {
                    entity.spawnAtLocation(ModItems.SERUM.get(), 0);
                }

                if (entity instanceof Animal && rng.nextInt(500) == 0) {
                    entity.spawnAtLocation(ModItems.BANDAID.get(), 0);
                }

                if (entity instanceof Enemy) {
                    if (rng.nextInt(1000) == 0) entity.spawnAtLocation(ModItems.HEART_PIECE.get(), 0);
                    if (rng.nextInt(250) == 0) entity.spawnAtLocation(ModItems.KEY_RED_CRACKED.get(), 0);
                    if (rng.nextInt(250) == 0) entity.spawnAtLocation(ModItems.LAUNCH_CODE_PIECE.get(), 0);
                }

                // EntityCyberCrab -> wd40 (1/500) folgt mit dem Cyber-Krebs (R8).

                if (entity instanceof Zombie) {
                    if (rng.nextInt(200) == 0) entity.spawnAtLocation(Items.COPPER_INGOT, 0);
                    if (rng.nextInt(200) == 0) entity.spawnAtLocation(ingot(ModMaterials.ALUMINUM), 0);
                    if (rng.nextInt(200) == 0) entity.spawnAtLocation(ingot(ModMaterials.TITANIUM), 0);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityDeathLast(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ModItems.DETONATOR_DEADMAN.get()) && stack.getTag() != null) {
                CompoundTag tag = stack.getTag();
                BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                Block b = player.level().getBlockState(pos).getBlock();
                if (!player.level().isClientSide && b instanceof IBomb bomb) {
                    bomb.explode(player.level(), pos);
                    MainRegistry.LOGGER.info("[DET] Tried to detonate block at {} / {} / {} by dead man's switch from {}!",
                            pos.getX(), pos.getY(), pos.getZ(), player.getGameProfile().getName());
                }
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity item = event.getEntity();
        ItemStack yeet = item.getItem();
        if (yeet.getItem() instanceof net.minecraft.world.item.ArmorItem && ArmorModificationHelper.hasMods(yeet)) {
            ItemStack cladding = ArmorModificationHelper.pryMods(yeet)[ArmorModificationHelper.cladding];
            if (cladding != null && cladding.is(ModItems.CLADDING_OBSIDIAN.get())) {
                item.setInvulnerable(true);
            }
        }

        if (yeet.is(ModItems.BISMUTH_TOOL.get())) {
            item.setInvulnerable(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDrop(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (HbmLivingProps.getContagion(event.getEntity()) > 0) {
            for (ItemEntity item : event.getDrops()) {
                item.getItem().getOrCreateTag().putBoolean("ntmContagion", true);
            }
        }
    }

    /** Ersatz fuer {@code EntityLivingBase.previousEquipment[0]} (nur die Gegenstandsart zaehlt). */
    private static final java.util.Map<Player, net.minecraft.world.item.Item> PREV_HELD = new java.util.WeakHashMap<>();

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();

        if (living instanceof net.minecraft.world.entity.monster.Creeper creeper && creeper.getPersistentData().getBoolean("hfr_defused")) {
            com.hbm_m.armormod.item.ItemModDefuser.castrateCreeper(creeper, null, false);
        }

        // Original: previousEquipment[0] != getHeldItem() -> IEquipReceiver.onEquip
        if (living instanceof net.minecraft.server.level.ServerPlayer sp) {
            ItemStack heldNow = sp.getMainHandItem();
            net.minecraft.world.item.Item prev = PREV_HELD.get(sp);
            net.minecraft.world.item.Item now = heldNow.isEmpty() ? null : heldNow.getItem();
            if (now != null && prev != now && now instanceof com.hbm_m.item.IEquipReceiver receiver) {
                receiver.onEquip(sp, heldNow);
            }
            if (now == null) PREV_HELD.remove(sp); else PREV_HELD.put(sp, now);
        }

        // HazardSystem.updateLivingInventory: Hand + vier Ruestungsslots von Nicht-Spielern.
        if (!living.level().isClientSide && !(living instanceof Player)) {
            ItemStack held = living.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!held.isEmpty()) HazardSystem.applyHazards(held, living);
            for (EquipmentSlot slot : ARMOR) {
                ItemStack stack = living.getItemBySlot(slot);
                if (!stack.isEmpty()) HazardSystem.applyHazards(stack, living);
            }
        }
    }

    // ---------------------------------------------------------------- Schaden

    @SubscribeEvent
    public static void onEntityAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (ArmorUtil.checkArmor(player, ModItems.EUPHEMIUM_HELMET.get(), ModItems.EUPHEMIUM_PLATE.get(), ModItems.EUPHEMIUM_LEGS.get(), ModItems.EUPHEMIUM_BOOTS.get())) {
                HbmPlayerProps.plink(player, SoundEvents.ITEM_BREAK, 0.5F, 1.0F + player.getRandom().nextFloat() * 0.5F);
                event.setCanceled(true);
            }

            ItemArmorMod.Hurt attack = new ItemArmorMod.Hurt(event.getSource(), event.getAmount());
            attack.canceled = event.isCanceled();

            ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!plate.isEmpty() && plate.getItem() instanceof ModArmorFSB fsb)
                fsb.handleAttack(player, attack);

            for (EquipmentSlot slot : ARMOR) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof IAttackHandler handler) {
                    handler.handleAttack(player, attack, stack);
                }
            }

            if (attack.canceled) event.setCanceled(true);
        }
    }

    /** Original {@code DamageResistanceHandler.onEntityAttacked}. */
    @SubscribeEvent
    public static void onEntityAttackedResistance(LivingAttackEvent event) {
        if (DamageResistanceHandler.onEntityAttacked(event.getEntity(), event.getSource(), event.getAmount()))
            event.setCanceled(true);
    }

    /** Original {@code DamageResistanceHandler.onEntityDamaged}. */
    @SubscribeEvent
    public static void onEntityDamagedResistance(LivingHurtEvent event) {
        event.setAmount(DamageResistanceHandler.onEntityDamaged(event.getEntity(), event.getSource(), event.getAmount()));
    }

    @SubscribeEvent
    public static void onEntityDamaged(LivingHurtEvent event) {
        LivingEntity e = event.getEntity();

        if (e instanceof Player player) {
            HbmPlayerProps props = HbmPlayerProps.getData(player);
            if (props.shield > 0) {
                float reduce = Math.min(props.shield, event.getAmount());
                props.shield -= reduce;
                event.setAmount(event.getAmount() - reduce);
            }
            props.lastDamage = player.tickCount;
        }

        if (HbmLivingProps.getContagion(e) > 0 && event.getAmount() < 100)
            event.setAmount(event.getAmount() * 2F);

        /// ARMOR MODS ///
        ItemArmorMod.Hurt hurt = new ItemArmorMod.Hurt(event.getSource(), event.getAmount());
        for (EquipmentSlot slot : ARMOR) {
            ItemStack armor = e.getItemBySlot(slot);
            if (!armor.isEmpty() && ArmorModificationHelper.hasMods(armor)) {
                for (ItemStack mod : ArmorModificationHelper.pryMods(armor)) {
                    if (mod != null && !mod.isEmpty() && mod.getItem() instanceof ItemArmorMod armorMod) {
                        armorMod.modDamage(e, hurt, armor);
                    }
                }
            }
        }

        if (e instanceof Player player) {

            /// FSB ARMOR ///
            ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!plate.isEmpty() && plate.getItem() instanceof ModArmorFSB fsb)
                fsb.handleHurt(player, hurt);

            for (EquipmentSlot slot : ARMOR) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof IDamageHandler handler) {
                    handler.handleDamage(player, hurt, stack);
                }
            }
        }

        event.setAmount(hurt.amount);
        if (hurt.canceled) event.setCanceled(true);
    }

    /** Original {@code ModEventHandler}: ein Schild mit dem richtigen Text wird zum versteckten Bobmazon-Katalog. */
    @SubscribeEvent
    public static void onSignClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        net.minecraft.world.level.Level world = event.getLevel();
        net.minecraft.core.BlockPos pos = event.getPos();
        if (world.isClientSide || !(world.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign)) return;

        var text = sign.getFrontText();
        String result = ShadyUtil.smoosh(text.getMessage(0, false).getString(), text.getMessage(1, false).getString(),
                text.getMessage(2, false).getString(), text.getMessage(3, false).getString());

        if (ShadyUtil.hashes.contains(result)) {
            world.destroyBlock(pos, false);
            net.minecraft.world.entity.item.ItemEntity entityitem = new net.minecraft.world.entity.item.ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new net.minecraft.world.item.ItemStack(ModItems.BOBMAZON_HIDDEN.get()));
            entityitem.setPickUpDelay(10);
            world.addFreshEntity(entityitem);
        }
    }

    @SubscribeEvent
    public static void onPlayerFall(PlayerFlyableFallEvent event) {
        Player e = event.getEntity();
        ItemStack plate = e.getItemBySlot(EquipmentSlot.CHEST);
        if (!plate.isEmpty() && plate.getItem() instanceof ModArmorFSB fsb)
            fsb.handleFall(e);
    }

    @SubscribeEvent
    public static void onEntityJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player e) {
            ItemStack plate = e.getItemBySlot(EquipmentSlot.CHEST);
            if (!plate.isEmpty() && plate.getItem() instanceof ModArmorFSB fsb)
                fsb.handleJump(e);
        }
    }

    @SubscribeEvent
    public static void onEntityFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player e) {
            ItemStack plate = e.getItemBySlot(EquipmentSlot.CHEST);
            if (!plate.isEmpty() && plate.getItem() instanceof ModArmorFSB fsb)
                fsb.handleFall(e);
        }
    }

    // ---------------------------------------------------------------- Spielertick

    @SubscribeEvent
    public static void onWingFlop(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;

        if (player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && !player.onGround()) {

            if (ShadyUtil.is(player, ShadyUtil.Barnaby99_x, "pheo7")) {

                ArmorUtil.resetFlightTime(player);
                HbmPlayerProps props = HbmPlayerProps.getData(player);

                if (props.isJetpackActive()) {
                    Vec3 m = player.getDeltaMovement();
                    double my = m.y < 0.4D ? m.y + 0.1D : m.y;
                    m = new Vec3(m.x, my, m.z);

                    Vec3 look = player.getLookAngle();

                    if (m.length() < 2) {
                        m = m.add(look.x * 0.2, look.y * 0.2, look.z * 0.2);
                        if (look.y > 0) player.fallDistance = 0;
                    }
                    player.setDeltaMovement(m);
                } else if (props.enableBackpack && !player.isShiftKeyDown()) {
                    Vec3 m = player.getDeltaMovement();
                    if (m.y < -0.2) player.setDeltaMovement(m.x, m.y + 0.075D, m.z);
                    if (player.fallDistance > 0) player.fallDistance = 0;
                }
            }

            boolean isBob = ShadyUtil.is(player, ShadyUtil.HbMinecraft, "HbMinecraft");
            boolean isOther = ShadyUtil.is(player, ShadyUtil.the_NCR, "the_NCR");

            if (isBob || isOther) {

                ArmorUtil.resetFlightTime(player);

                if (player.fallDistance > 0) player.fallDistance = 0;

                double mx = player.getDeltaMovement().x;
                double my = player.getDeltaMovement().y;
                double mz = player.getDeltaMovement().z;

                if (my < -0.4D) my = -0.4D;

                HbmPlayerProps props = HbmPlayerProps.getData(player);

                if (isBob || player.getFoodData().getFoodLevel() > 6) {

                    if (props.isJetpackActive()) {

                        double cap = (isBob ? 0.8D : 0.4D);

                        if (my < cap) my += 0.15D;
                        else my = cap + 0.15D;

                        if (isOther) {
                            if (player.getFoodData().getSaturationLevel() > 0F) player.causeFoodExhaustion(4F); //burn up saturation so that super-saturating foods have no effect
                            else player.causeFoodExhaustion(0.2F); //4:1 -> 0.05 hunger per tick or 1 per second
                        }

                    } else if (props.enableBackpack && !player.isShiftKeyDown()) {

                        if (my < -1) my += 0.4D;
                        else if (my < -0.1) my += 0.2D;
                        else if (my < 0) my = 0;

                        if (isOther && !player.onGround()) {
                            if (player.getFoodData().getSaturationLevel() > 0F) player.causeFoodExhaustion(4F);
                            else player.causeFoodExhaustion(0.04F);
                        }

                    } else if (!props.enableBackpack && player.isShiftKeyDown()) {

                        if (my < -0.08) {
                            double mo = my * (isBob ? -0.6 : -0.4);
                            my += mo;

                            Vec3 vec = player.getLookAngle().scale(mo);
                            mx += vec.x;
                            my += vec.y;
                            mz += vec.z;
                        }
                    }
                }

                Vec3 orig = player.getLookAngle();
                Vec3 look = new Vec3(orig.x, 0, orig.z).normalize();
                double mod = props.enableBackpack ? (isBob ? 0.5D : 0.25D) : 0.125D;

                if (player.zza != 0) {
                    mx += look.x * 0.35 * player.zza * mod;
                    mz += look.z * 0.35 * player.zza * mod;
                }

                if (player.xxa != 0) {
                    look = look.yRot((float) Math.PI * 0.5F);
                    mx += look.x * 0.15 * player.xxa * mod;
                    mz += look.z * 0.15 * player.xxa * mod;
                }

                player.setDeltaMovement(mx, my, mz);
            }
        }

        if (ShadyUtil.is(player, ShadyUtil.LePeeperSauvage, "LePeeperSauvage")) {
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(fopSpeed);
                if (player.isSprinting()) {
                    speed.addTransientModifier(PlatformHooks.attributeModifier(fopSpeed, "FOP SPEED", 0.5, AttributeModifier.Operation.MULTIPLY_BASE));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = player.level();

        ItemStack fsbPlate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!fsbPlate.isEmpty() && fsbPlate.getItem() instanceof ModArmorFSB fsb)
            fsb.handleTick(player);

        if (event.phase == TickEvent.Phase.START) {
            BlockPos pos = BlockPos.containing(player.getX(), player.getY() - 0.01, player.getZ());
            Block b = level.getBlockState(pos).getBlock();

            if (b instanceof IStepTickReceiver step && !player.getAbilities().flying) {
                step.onPlayerStep(level, pos, player);
            }
        }

        if (!level.isClientSide && event.phase == TickEvent.Phase.START) {

            /// GHOST FIX START ///
            if (!Float.isFinite(player.getHealth()) || !Float.isFinite(player.getAbsorptionAmount())) {
                player.sendSystemMessage(Component.literal("Your health has been restored!"));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.syringe"), SoundSource.PLAYERS, 1.0F, 1.0F);
                player.setHealth(player.getMaxHealth());
                player.setAbsorptionAmount(0);
            }
            /// GHOST FIX END ///

            /// BETA HEALTH START ///
            if (player.getInventory().hasAnyOf(Set.of(ModItems.BETA.get()))) {
                int food = player.getFoodData().getFoodLevel();
                if (food > 10) {
                    player.heal(food - 10);
                }
                if (food != 10) {
                    player.getFoodData().setFoodLevel(10);
                }
            }
            /// BETA HEALTH END ///

            /// PU RADIATION START ///
            if (player.getUUID().toString().equals(ShadyUtil.Pu_238)) {
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3, 3, 3));
                for (LivingEntity e : entities) {
                    if (e != player) {
                        e.addEffect(new MobEffectInstance(ModEffects.RADIATION.get(), 300, 2));
                    }
                }
            }
            /// PU RADIATION END ///
        }

        if (level.isClientSide && event.phase == TickEvent.Phase.START && !player.isInvisible() && !player.isShiftKeyDown()) {
            if (player.getUUID().toString().equals(ShadyUtil.Pu_238)) {
                Vec3 vec = new Vec3(3 * rand.nextDouble(), 0, 0);
                vec = vec.zRot((float) (rand.nextDouble() * Math.PI));
                vec = vec.yRot((float) (rand.nextDouble() * Math.PI * 2));
                level.addParticle(ParticleTypes.MYCELIUM, player.getX() + vec.x, player.getY() + 1 + vec.y, player.getZ() + vec.z, 0.0, 0.0, 0.0);
            }
        }
    }

    // ---------------------------------------------------------------- Handwerk / Amboss / Essen

    @SubscribeEvent
    public static void itemSmelted(PlayerEvent.ItemSmeltedEvent e) {
        Player player = e.getEntity();
        if (player.level().isClientSide) return;
        ItemStack smelted = e.getSmelting();

        if (smelted.is(Items.IRON_INGOT) && player.getRandom().nextInt(64) == 0) {
            give(player, new ItemStack(ModItems.LODESTONE.get()));
        }

        if (smelted.is(ingot(ModMaterials.URANIUM)) && player.getRandom().nextInt(64) == 0) {
            give(player, new ItemStack(ModItems.QUARTZ_PLUTONIUM.get()));
        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        // Anvil renaming no longer increments the repair cost (nur Umbenennen: rechter Slot leer).
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        ItemStack output = event.getOutput();
        if (right.isEmpty() && !output.isEmpty() && !left.isEmpty()) {
            int oldRepairCost = left.getBaseRepairCost();

            if (oldRepairCost > 0) {
                output.setRepairCost(oldRepairCost);
            } else if (output.hasTag()) {
                output.removeTagKey("RepairCost");
                if (output.getTag() != null && output.getTag().isEmpty()) {
                    output.setTag(null);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();

        if (!stack.isEmpty() && stack.isEdible() && stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag.getBoolean("ntmCyanide")) for (int i = 0; i < 10; i++) {
                player.hurt(ModDamageSources.create(player.level(), rand.nextBoolean() ? ModDamageTypes.EUTHANIZED_SELF : ModDamageTypes.EUTHANIZED_SELF_2), 1000);
            }
            if (tag.getBoolean("ntmRedPill")) for (int i = 0; i < 10; i++) {
                player.addEffect(new MobEffectInstance(ModEffects.DEATH.get(), 60 * 60 * 20, 0));
            }
        }
    }

    // ---------------------------------------------------------------- Hilfen

    private static Item ingot(ModMaterials mat) {
        return ModMaterialItems.item(mat, MaterialShape.INGOT);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        else player.inventoryMenu.broadcastChanges();
    }
}
//?}
