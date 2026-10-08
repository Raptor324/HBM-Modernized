package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.inventory.menu.LaunchPadLargeMenu;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityLaunchPadLarge} (1.7.10): grosse Startrampe mit Aufrichter. Liegt eine gueltige Rakete im
 * Schacht, klappt der Aufrichter (erector, 90 -> 0 Grad) hoch, der Hubtisch (lift, 1 -> 0) faehrt ein und die
 * Rakete steht ({@code erected}); nach dem Start faehrt alles zurueck. Alles nur mit mindestens 75.000 HE.
 */
public class LaunchPadLargeBlockEntity extends LaunchPadBaseBlockEntity {

    public int formFactor = -1;
    /** Whether the missile has already been placed on the launchpad. Missile will render statically on the pad if true */
    public boolean erected = false;
    /** Whether the missile can be lifted. Missile will not render at all if false and not erected */
    public boolean readyToLoad = false;
    /** Instead of setting erected to true outright, this ties into the delay to prevent a jerky transition */
    public boolean scheduleErect = false;
    public float lift = 1F;
    public float erector = 90F;
    public float prevLift = 1F;
    public float prevErector = 90F;
    public float syncLift = 1F;
    public float syncErector = 90F;
    private int sync;

    protected boolean liftMoving = false;
    protected boolean erectorMoving = false;

    public LaunchPadLargeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_PAD_LARGE_BE.get(), pos, state);
        // Original: "Delay between erector movements" startet bei 20
        this.delay = 20;
    }

    @Override
    protected boolean isReadyForLaunch() {
        return this.erected && this.readyToLoad;
    }

    @Override
    protected double getLaunchOffset() {
        return 2D;
    }

    @Override
    protected boolean usesPadDelayLogic() {
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchPadLargeBlockEntity be) {
        if (level.isClientSide) {
            be.clientTick(level, pos, state);
        } else {
            be.serverTick(level, pos, state);
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        this.prevLift = this.lift;
        this.prevErector = this.erector;

        float erectorSpeed = 1.5F;
        float liftSpeed = 0.025F;

        ItemStack slot0 = inventory.getStackInSlot(SLOT_MISSILE);

        if (this.isMissileValid()) {
            if (slot0.getItem() instanceof MissileItem missile) {
                this.formFactor = missile.formFactor.ordinal();

                if (missile.formFactor == MissileItem.MissileFormFactor.ATLAS || missile.formFactor == MissileItem.MissileFormFactor.HUGE) {
                    erectorSpeed /= 2F;
                    liftSpeed /= 2F;
                }
            }

            if (this.erector == 90F && this.lift == 1F) {
                this.readyToLoad = true;
            }
        } else {
            readyToLoad = false;
            erected = false;
            delay = 20;
        }

        if (this.energy >= 75_000) {
            if (delay > 0) {
                delay--;

                if (delay < 10 && scheduleErect) {
                    this.erected = true;
                    this.scheduleErect = false;
                }

                // if there is no missile or the missile isn't ready (i.e. the erector hasn't returned to zero position yet), retract
                if (slot0.isEmpty() || !readyToLoad) {
                    //fold back erector
                    if (erector < 90F) {
                        erector = Math.min(erector + erectorSpeed, 90F);
                        if (erector == 90F) delay = 20;
                    //extend lift
                    } else if (lift < 1F) {
                        lift = Math.min(lift + liftSpeed, 1F);
                        if (erector == 1F) {
                            //if the lift is fully extended, the loading can begin
                            readyToLoad = true;
                            delay = 20;
                        }
                    }
                }

            } else {

                //only extend if the erector isn't up yet and the missile can be loaded
                if (!erected && readyToLoad) {
                    this.state = STATE_LOADING;

                    //first, rotate the erector
                    if (erector != 0F) {
                        erector = Math.max(erector - erectorSpeed, 0F);
                        if (erector == 0F) delay = 20;
                    //then retract the lift
                    } else if (lift > 0) {
                        lift = Math.max(lift - liftSpeed, 0F);
                        if (lift == 0F) {
                            //once the lift is at the bottom, the missile is deployed
                            scheduleErect = true;
                            delay = 20;
                        }
                    }
                } else {
                    //first, fold back the erector
                    if (erector < 90F) {
                        erector = Math.min(erector + erectorSpeed, 90F);
                        if (erector == 90F) delay = 20;
                    //then extend the lift again
                    } else if (lift < 1F) {
                        lift = Math.min(lift + liftSpeed, 1F);
                        if (erector == 1F) {
                            //if the lift is fully extended, the loading can begin
                            readyToLoad = true;
                            delay = 20;
                        }
                    }
                }
            }
        }

        if (!this.hasFuel() || !this.isMissileValid()) this.state = STATE_MISSING;
        if (this.erected && this.canLaunch()) this.state = STATE_READY;

        boolean prevLiftMoving = this.liftMoving;
        boolean prevErectorMoving = this.erectorMoving;
        this.liftMoving = false;
        this.erectorMoving = false;
        if (this.prevLift != this.lift) this.liftMoving = true;
        if (this.prevErector != this.erector) this.erectorMoving = true;

        if (prevLiftMoving && !this.liftMoving) level.playSound(null, pos, HbmSoundsNT.get("hbm:door.wgh_stop"), SoundSource.BLOCKS, 2F, 1F);
        if (prevErectorMoving && !this.erectorMoving) level.playSound(null, pos, HbmSoundsNT.get("hbm:door.garage_stop"), SoundSource.BLOCKS, 2F, 1F);

        // super.updateEntity(): Netze, Batterie, Tanks, Treibstoffart, Redstone-Flanke, Sync
        commonServerTick(level, pos, state, this);

        // networkPackNT: Bewegung jeden Tick an den Client (dort ueber 3 Ticks geglaettet)
        if (this.liftMoving || this.erectorMoving || prevLiftMoving != this.liftMoving || prevErectorMoving != this.erectorMoving) {
            sendUpdateToClient();
        }
    }

    private void clientTick(Level level, BlockPos pos, BlockState state) {
        this.prevLift = this.lift;
        this.prevErector = this.erector;

        if (this.sync > 0) {
            this.lift = this.lift + ((this.syncLift - this.lift) / (float) this.sync);
            this.erector = this.erector + ((this.syncErector - this.erector) / (float) this.sync);
            --this.sync;
        } else {
            this.lift = this.syncLift;
            this.erector = this.syncErector;
        }

        ClientSoundBootstrap.updateDoorSoundRaw(level, pos, "lpl_lift", this.liftMoving,
                () -> loopSound(pos, "hbm:door.wgh_start", 0.75F, 1.0F));
        ClientSoundBootstrap.updateDoorSoundRaw(level, pos, "lpl_erector", this.erectorMoving,
                () -> loopSound(pos, "hbm:door.garage_move", 1.5F, 1.0F));

        if (this.erected && (this.formFactor == MissileItem.MissileFormFactor.HUGE.ordinal() || this.formFactor == MissileItem.MissileFormFactor.ATLAS.ordinal())
                && this.tanks[1].getFill() > 0) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "tower");
            data.putFloat("lift", 0F);
            data.putFloat("base", 0.5F);
            data.putFloat("max", 2F);
            data.putInt("life", 70 + level.random.nextInt(30));
            data.putDouble("posX", pos.getX() + 0.5 + level.random.nextGaussian() * 0.5);
            data.putDouble("posZ", pos.getZ() + 0.5 + level.random.nextGaussian() * 0.5);
            data.putDouble("posY", pos.getY() + 2);
            data.putBoolean("noWind", true);
            data.putFloat("alphaMod", 2F);
            data.putFloat("strafe", 0.05F);
            for (int i = 0; i < 3; i++) com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }

        List<MissileBaseEntity> entities = level.getEntitiesOfClass(MissileBaseEntity.class,
                new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 10, pos.getZ() + 1.5));

        if (!entities.isEmpty()) {
            for (int i = 0; i < 15; i++) {
                Direction dir = state.getValue(HorizontalDirectionalBlock.FACING);
                if (level.random.nextBoolean()) dir = dir.getOpposite();
                float moX = (float) (level.random.nextGaussian() * 0.15F + 0.75) * dir.getStepX();
                float moZ = (float) (level.random.nextGaussian() * 0.15F + 0.75) * dir.getStepZ();
                level.addParticle(com.hbm_m.particle.ModParticleTypes.SMOKE_COLUMN.get(), pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, moX, 0.0D, moZ);
            }
        }
    }

    private static Object loopSound(BlockPos pos, String sound, float volume, float pitch) {
        try {
            return Class.forName("com.hbm_m.client.sound.ArcFurnaceLoopSoundFactory")
                    .getMethod("create", BlockPos.class, String.class, float.class, float.class).invoke(null, pos, sound, volume, pitch);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.stopSpecificSound(level, worldPosition, "lpl_lift");
            ClientSoundBootstrap.stopSpecificSound(level, worldPosition, "lpl_erector");
        }
    }

    @Override
    protected void finalizeLaunch(Entity missile) {
        // Basis setzt die Abklingzeit der kleinen Rampe (delay = 100) - das Original der grossen Rampe nicht.
        int keep = this.delay;
        super.finalizeLaunch(missile);
        this.delay = keep;
        this.erected = false;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("erected", erected);
        tag.putBoolean("readyToLoad", readyToLoad);
        tag.putFloat("lift", lift);
        tag.putFloat("erector", erector);
        tag.putInt("formFactor", formFactor);
        tag.putBoolean("liftMoving", liftMoving);
        tag.putBoolean("erectorMoving", erectorMoving);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.erected = tag.getBoolean("erected");
        this.readyToLoad = tag.getBoolean("readyToLoad");
        this.formFactor = tag.contains("formFactor") ? tag.getInt("formFactor") : -1;
        this.liftMoving = tag.getBoolean("liftMoving");
        this.erectorMoving = tag.getBoolean("erectorMoving");
        float l = tag.contains("lift") ? tag.getFloat("lift") : 1F;
        float e = tag.contains("erector") ? tag.getFloat("erector") : 90F;
        if (level != null && level.isClientSide) {
            // deserialize: Zielwerte, Glaettung ueber drei Ticks
            this.syncLift = l;
            this.syncErector = e;
            if (this.lift != this.syncLift || this.erector != this.syncErector) this.sync = 3;
        } else {
            this.lift = l;
            this.erector = e;
            this.syncLift = l;
            this.syncErector = e;
        }
    }

    @Override
    public NodeDirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new NodeDirPos[] {
                new NodeDirPos(x + 5, y, z - 2, Direction.EAST),
                new NodeDirPos(x + 5, y, z + 2, Direction.EAST),
                new NodeDirPos(x - 5, y, z - 2, Direction.WEST),
                new NodeDirPos(x - 5, y, z + 2, Direction.WEST),
                new NodeDirPos(x - 2, y, z + 5, Direction.SOUTH),
                new NodeDirPos(x + 2, y, z + 5, Direction.SOUTH),
                new NodeDirPos(x - 2, y, z - 5, Direction.NORTH),
                new NodeDirPos(x + 2, y, z - 5, Direction.NORTH)
        };
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 10, worldPosition.getY(), worldPosition.getZ() - 10,
                worldPosition.getX() + 11, worldPosition.getY() + 15, worldPosition.getZ() + 11);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchPadLarge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new LaunchPadLargeMenu(containerId, inv, this);
    }
}
