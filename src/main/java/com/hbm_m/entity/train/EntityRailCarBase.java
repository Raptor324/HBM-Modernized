package com.hbm_m.entity.train;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.rail.IRailNTM;
import com.hbm_m.block.rail.IRailNTM.MoveContext;
import com.hbm_m.block.rail.IRailNTM.RailCheckType;
import com.hbm_m.block.rail.IRailNTM.RailContext;
import com.hbm_m.block.rail.IRailNTM.TrackGauge;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityRailCarBase}: Grundlage aller Waggons des Zugsystems. Die Waggons laufen ueber {@link IRailNTM}-Schienen,
 * gekoppelte Waggons bilden eine {@link LogicalTrainUnit}, die einmal pro Welt-Tick ({@link #updateMotion}) bewegt wird.
 * Wie im Original wird {@code motionX/motionY} (hier {@code deltaMovement}) missbraucht, um Gier- und Nickwinkel zum
 * Client zu schicken.
 */
public abstract class EntityRailCarBase extends Entity {

    public LogicalTrainUnit ltu;
    public int ltuIndex = 0;
    public boolean isOnRail = true;
    private int turnProgress;
    /* Clientseitige Zielposition fuer die weiche Interpolation */
    private double trainX;
    private double trainY;
    private double trainZ;
    private double trainYaw;
    private double trainPitch;
    private float movementYaw;
    private float movementPitch;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    /* "Echte" Position genau zwischen vorderem und hinterem Drehgestell, weicht in Kurven von der Standardposition ab */
    public double lastRenderX;
    public double lastRenderY;
    public double lastRenderZ;
    public double renderX;
    public double renderY;
    public double renderZ;
    public double cachedSpeed;

    public EntityRailCarBase coupledFront;
    public EntityRailCarBase coupledBack;

    public boolean initDummies = false;
    public BoundingBoxDummyEntity[] dummies = new BoundingBoxDummyEntity[0];

    public EntityRailCarBase(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override protected void defineSynchedData() { }
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag nbt) { }
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag nbt) { }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        return interactFirst(player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /** Original {@code interactFirst}: Kuppeln mit dem Kupplungswerkzeug. */
    public boolean interactFirst(Player player, InteractionHand hand) {

        if (player.getItemInHand(hand).getItem() == ModItems.COUPLING_TOOL.get()) {

            List<EntityRailCarBase> intersecting = level().getEntitiesOfClass(EntityRailCarBase.class, this.getBoundingBox().inflate(2D, 0D, 2D));

            for (EntityRailCarBase neighbor : intersecting) {
                if (neighbor == this) continue;
                if (neighbor.getGauge() != this.getGauge()) continue;

                TrainCoupling closestOwnCoupling = null;
                TrainCoupling closestNeighborCoupling = null;
                double closestDist = Double.POSITIVE_INFINITY;

                for (TrainCoupling ownCoupling : TrainCoupling.values()) {
                    for (TrainCoupling neighborCoupling : TrainCoupling.values()) {
                        Vec3NT ownPos = this.getCouplingPos(ownCoupling);
                        Vec3NT neighborPos = neighbor.getCouplingPos(neighborCoupling);
                        if (ownPos != null && neighborPos != null) {
                            Vec3NT delta = Vec3NT.createVectorHelper(ownPos.xCoord - neighborPos.xCoord, ownPos.yCoord - neighborPos.yCoord, ownPos.zCoord - neighborPos.zCoord);
                            double length = delta.lengthVector();

                            if (length < 1 && length < closestDist) {
                                closestDist = length;
                                closestOwnCoupling = ownCoupling;
                                closestNeighborCoupling = neighborCoupling;
                            }
                        }
                    }
                }

                if (closestOwnCoupling != null && closestNeighborCoupling != null) {
                    if (this.getCoupledTo(closestOwnCoupling) != null) continue;
                    if (neighbor.getCoupledTo(closestNeighborCoupling) != null) continue;
                    this.couple(closestOwnCoupling, neighbor);
                    neighbor.couple(closestNeighborCoupling, this);
                    if (this.ltu != null) this.ltu.dissolveTrain();
                    if (neighbor.ltu != null) neighbor.ltu.dissolveTrain();
                    player.swing(hand);

                    player.sendSystemMessage(Component.literal("Coupled " + this.hashCode() + " (" + closestOwnCoupling.name() + ") to " + neighbor.hashCode() + " (" + closestNeighborCoupling.name() + ")"));

                    return true;
                }
            }
        }

        // DEBUG des Originals (Partikelpaket auskommentiert) entfaellt

        return false;
    }

    @Override
    public void tick() {

        if (this.level().isClientSide) {

            if (this.turnProgress > 0) {
                this.yRotO = this.getYRot();
                double x = this.getX() + (this.trainX - this.getX()) / (double) this.turnProgress;
                double y = this.getY() + (this.trainY - this.getY()) / (double) this.turnProgress;
                double z = this.getZ() + (this.trainZ - this.getZ()) / (double) this.turnProgress;
                double yaw = Mth.wrapDegrees(this.trainYaw - (double) this.getYRot());
                this.setYRot((float) ((double) this.getYRot() + yaw / (double) this.turnProgress));
                this.setXRot((float) ((double) this.getXRot() + (this.trainPitch - (double) this.getXRot()) / (double) this.turnProgress));
                --this.turnProgress;
                this.setPos(x, y, z);
            } else {
                this.setPos(this.getX(), this.getY(), this.getZ());
            }

            BlockPos anchor = this.getCurrentAnchorPos();
            Vec3NT frontPos = getRelPosAlongRail(anchor, this.getLengthSpan(), new MoveContext(RailCheckType.FRONT, this.getCollisionSpan() - this.getLengthSpan()));
            Vec3NT backPos = getRelPosAlongRail(anchor, -this.getLengthSpan(), new MoveContext(RailCheckType.BACK, this.getCollisionSpan() - this.getLengthSpan()));

            this.lastRenderX = this.renderX;
            this.lastRenderY = this.renderY;
            this.lastRenderZ = this.renderZ;

            if (frontPos != null && backPos != null) {
                this.renderX = (frontPos.xCoord + backPos.xCoord) / 2D;
                this.renderY = (frontPos.yCoord + backPos.yCoord) / 2D;
                this.renderZ = (frontPos.zCoord + backPos.zCoord) / 2D;
            } else {
                this.renderX = getX();
                this.renderY = getY();
                this.renderZ = getZ();
            }

        } else {

            if (!this.isOnRail) {
                if (this.coupledFront != null) this.coupledFront.couple(this.coupledFront.getCouplingFrom(this), null);
                if (this.coupledBack != null) this.coupledBack.couple(this.coupledBack.getCouplingFrom(this), null);
                this.coupledFront = null;
                this.coupledBack = null;
            }

            if (this.coupledFront != null && this.coupledFront.isRemoved()) {
                this.coupledFront = null;
                if (this.ltu != null) this.ltu.dissolveTrain();
            }
            if (this.coupledBack != null && this.coupledBack.isRemoved()) {
                this.coupledBack = null;
                if (this.ltu != null) this.ltu.dissolveTrain();
            }

            if (this.ltu == null && (this.coupledFront == null || this.coupledBack == null) && this.isOnRail) {
                LogicalTrainUnit.generateTrain(this);
            }

            if (!this.isOnRail) {
                Vec3NT motion = Vec3NT.createVectorHelper(0, 0, this.cachedSpeed);
                motion.rotateAroundY((float) (-this.getYRot() * Math.PI / 180D));
                this.move(MoverType.SELF, new Vec3(motion.xCoord, motion.yCoord - 0.04, motion.zCoord));
                this.renderX = getX();
                this.renderY = getY();
                this.renderZ = getZ();
                this.cachedSpeed *= 0.95D;
            }

            DummyConfig[] definitions = this.getDummies();

            if (!this.initDummies) {
                this.dummies = new BoundingBoxDummyEntity[definitions.length];

                for (int i = 0; i < definitions.length; i++) {
                    DummyConfig def = definitions[i];
                    BoundingBoxDummyEntity dummy = new BoundingBoxDummyEntity(level(), this, def.width, def.height);
                    Vec3NT rot = Vec3NT.createVectorHelper(def.offset.xCoord, def.offset.yCoord, def.offset.zCoord);
                    rot.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
                    double x = getX() + rot.xCoord;
                    double y = getY() + rot.yCoord;
                    double z = getZ() + rot.zCoord;
                    dummy.setPos(x, y, z);
                    dummy.setSize(def.width, def.height);
                    level().addFreshEntity(dummy);
                    this.dummies[i] = dummy;
                }

                this.initDummies = true;
            }

            if (renderY != 0) {
                for (int i = 0; i < definitions.length; i++) {
                    DummyConfig def = definitions[i];
                    BoundingBoxDummyEntity dummy = dummies[i];
                    Vec3NT rot = Vec3NT.createVectorHelper(def.offset.xCoord, def.offset.yCoord, def.offset.zCoord);
                    rot.rotateAroundX((float) (this.getXRot() * Math.PI / 180D));
                    rot.rotateAroundY((float) (-this.getYRot() * Math.PI / 180));
                    double x = renderX + rot.xCoord;
                    double y = renderY + rot.yCoord;
                    double z = renderZ + rot.zCoord;
                    dummy.setPos(x, y, z);
                }
            }
        }
    }

    public Vec3NT getRelPosAlongRail(BlockPos anchor, double distanceToCover, MoveContext context) {
        return getRelPosAlongRail(anchor, distanceToCover, this.getGauge(), this.level(), Vec3NT.createVectorHelper(getX(), getY(), getZ()), this.getYRot(), context);
    }

    public static Vec3NT getRelPosAlongRail(BlockPos anchor, double distanceToCover, TrackGauge gauge, Level worldObj, Vec3NT next, float yaw, MoveContext context) {

        if (distanceToCover < 0) {
            distanceToCover *= -1;
            yaw += 180;
        }

        int it = 0;

        do {

            it++;

            if (it > 30) {
                return null;
            }

            int x = anchor.getX();
            int y = anchor.getY();
            int z = anchor.getZ();
            Block block = worldObj.getBlockState(new BlockPos(x, y, z)).getBlock();

            Vec3NT rot = Vec3NT.createVectorHelper(0, 0, 1);
            rot.rotateAroundY((float) (-yaw * Math.PI / 180D));

            if (block instanceof IRailNTM rail) {

                if (it == 1) {
                    next = rail.getTravelLocation(worldObj, x, y, z, next.xCoord, next.yCoord, next.zCoord, rot.xCoord, rot.yCoord, rot.zCoord, 0, new RailContext(), context);
                }

                boolean flip = distanceToCover < 0;

                if (rail.getGauge(worldObj, x, y, z) == gauge) {
                    RailContext info = new RailContext();
                    Vec3NT prev = next;
                    next = rail.getTravelLocation(worldObj, x, y, z, prev.xCoord, prev.yCoord, prev.zCoord, rot.xCoord, rot.yCoord, rot.zCoord, distanceToCover, info, context);
                    distanceToCover = info.overshoot;
                    anchor = info.pos;

                    yaw = generateYaw(next, prev) * (flip ? -1 : 1);

                } else {
                    return null;
                }
            } else {
                return null;
            }

        } while (distanceToCover != 0); // solange noch Weg uebrig ist, weiter

        return next;
    }

    public static float generateYaw(Vec3NT front, Vec3NT back) {
        double deltaX = front.xCoord - back.xCoord;
        double deltaZ = front.zCoord - back.zCoord;
        double radians = -Math.atan2(deltaX, deltaZ);
        return (float) Mth.wrapDegrees(radians * 180D / Math.PI);
    }

    /** Original {@code ModEventHandler.worldTick} (Phase END, Server): bewegt alle Zugeinheiten der Welt. */
    public static void updateMotion(ServerLevel world) {
        Set<LogicalTrainUnit> ltus = new HashSet<>();

        /* alle LTUs einsammeln */
        for (Entity o : world.getAllEntities()) {
            if (o instanceof EntityRailCarBase train) {
                if (train.ltu != null) ltus.add(train.ltu);
            }
        }

        for (LogicalTrainUnit ltu : ltus) {

            double speed = ltu.getTotalSpeed() + ltu.pushForce;

            if (Math.abs(speed) < 0.001) speed = 0;

            for (EntityRailCarBase car : ltu.trains) car.cachedSpeed = speed;

            if (ltu.trains.length == 1) {

                EntityRailCarBase train = ltu.trains[0];

                BlockPos anchor = IRailNTM.pos(train.getX(), train.getY(), train.getZ());
                Vec3NT newPos = train.getRelPosAlongRail(anchor, speed, new MoveContext(RailCheckType.CORE, 0));
                if (newPos == null) {
                    train.derail();
                    ltu.dissolveTrain();
                    continue;
                }
                train.setPos(newPos.xCoord, newPos.yCoord, newPos.zCoord);
                anchor = train.getCurrentAnchorPos();
                Vec3NT frontPos = train.getRelPosAlongRail(anchor, train.getLengthSpan(), new MoveContext(RailCheckType.FRONT, train.getCollisionSpan() - train.getLengthSpan()));
                Vec3NT backPos = train.getRelPosAlongRail(anchor, -train.getLengthSpan(), new MoveContext(RailCheckType.BACK, train.getCollisionSpan() - train.getLengthSpan()));

                if (frontPos == null || backPos == null) {
                    train.derail();
                    ltu.dissolveTrain();
                    continue;
                } else {
                    ltu.setRenderPos(train, frontPos, backPos);
                }

                ltu.pushForce = 0;
                ltu.collideTrain(speed);

                continue;
            }

            if (speed == 0) {
                ltu.combineWagons();
            } else {
                ltu.moveTrainByApproach(speed);
            }

            ltu.pushForce = 0;
            ltu.collideTrain(speed);
        }
    }

    /** Weg in Bloecken, den der Zug pro Tick zuruecklegen soll */
    public abstract double getCurrentSpeed();
    public abstract double getMaxRailSpeed();
    /** Spurweite dieses Zuges */
    public abstract TrackGauge getGauge();
    /** Abstand zwischen Kern und einem Drehgestell */
    public abstract double getLengthSpan();
    /** Abstand zwischen Kern und Kollisionspunkten */
    public abstract double getCollisionSpan();

    /** "Wahre" Position des Zuges, also der Block, auf den er einrasten will */
    public BlockPos getCurrentAnchorPos() {
        return IRailNTM.pos(getX(), getY() + 0.25, getZ());
    }

    public void derail() {
        isOnRail = false;
    }

    /** Original {@code setPositionAndRotation2}. */
    @Override
    public void lerpTo(double posX, double posY, double posZ, float yaw, float pitch, int turnProg, boolean teleport) {
        this.trainX = posX;
        this.trainY = posY;
        this.trainZ = posZ;
        this.trainPitch = pitch;
        this.turnProgress = turnProg + 2;
        this.setDeltaMovement(this.velocityX, this.velocityY, this.velocityZ);
        this.trainYaw = this.movementYaw;
        this.trainPitch = this.movementPitch;
    }

    /** Original {@code setVelocity}: liest wie dort die alte Bewegung als Gier-/Nickwinkel. */
    @Override
    public void lerpMotion(double mX, double mY, double mZ) {
        Vec3 old = this.getDeltaMovement();
        this.movementYaw = (float) old.x * 360F;
        this.movementPitch = (float) old.y * 360F;
        this.velocityX = mX;
        this.velocityY = mY;
        this.velocityZ = mZ;
        this.setDeltaMovement(mX, mY, mZ);
    }

    public DummyConfig[] getDummies() {
        return new DummyConfig[0];
    }

    public static class DummyConfig {
        public Vec3NT offset;
        public float width;
        public float height;

        public DummyConfig(float width, float height, Vec3NT offset) {
            this.width = width;
            this.height = height;
            this.offset = offset;
        }
    }

    public enum TrainCoupling {
        FRONT,
        BACK
    }

    public double getCouplingDist(TrainCoupling coupling) {
        return 0D;
    }

    public Vec3NT getCouplingPos(TrainCoupling coupling) {
        double dist = this.getCouplingDist(coupling);

        if (dist <= 0) return null;

        if (coupling == TrainCoupling.BACK) dist *= -1;

        Vec3NT rot = Vec3NT.createVectorHelper(0, 0, dist);
        rot.rotateAroundY((float) (-this.getYRot() * Math.PI / 180D));
        rot.xCoord += this.renderX;
        rot.yCoord += this.renderY;
        rot.zCoord += this.renderZ;
        return rot;
    }

    public EntityRailCarBase getCoupledTo(TrainCoupling coupling) {
        return coupling == TrainCoupling.FRONT ? this.coupledFront : coupling == TrainCoupling.BACK ? this.coupledBack : null;
    }

    public TrainCoupling getCouplingFrom(EntityRailCarBase coupledTo) {
        return coupledTo == this.coupledFront ? TrainCoupling.FRONT : coupledTo == this.coupledBack ? TrainCoupling.BACK : null;
    }

    public void couple(TrainCoupling coupling, EntityRailCarBase to) {
        if (coupling == TrainCoupling.FRONT) this.coupledFront = to;
        if (coupling == TrainCoupling.BACK) this.coupledBack = to;
    }

    //? if < 1.21.1 {
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //?}

    public static class LogicalTrainUnit {

        protected double pushForce;
        protected EntityRailCarBase[] trains;

        /** Setzt voraus, dass der Waggon ein Endpunkt ist, also nur eine Kupplung belegt ist */
        public static LogicalTrainUnit generateTrain(EntityRailCarBase train) {
            List<EntityRailCarBase> links = new ArrayList<>();
            Set<EntityRailCarBase> brake = new HashSet<>();
            LogicalTrainUnit ltu = new LogicalTrainUnit();

            if (train.coupledFront == null && train.coupledBack == null) {
                ltu.trains = new EntityRailCarBase[] {train};
                train.ltu = ltu;
                train.ltuIndex = 0;
                return ltu;
            }

            EntityRailCarBase current = train;
            EntityRailCarBase next;

            do {
                next = null;

                if (current.coupledFront != null && !brake.contains(current.coupledFront)) next = current.coupledFront;
                if (current.coupledBack != null && !brake.contains(current.coupledBack)) next = current.coupledBack;

                links.add(current);
                brake.add(current);

                current = next;

            } while (next != null);

            ltu.trains = new EntityRailCarBase[links.size()];
            for (int i = 0; i < ltu.trains.length; i++) {
                ltu.trains[i] = links.get(i);
                ltu.trains[i].ltu = ltu;
                ltu.trains[i].ltuIndex = i;
            }

            return ltu;
        }

        /** Entfernt die LTU aus allen Waggons */
        public void dissolveTrain() {
            for (EntityRailCarBase train : trains) {
                train.ltu = null;
                train.ltuIndex = 0;
            }
        }

        /** Mitte des Zuges bestimmen und alle Waggons dorthin ziehen, bis sich die Kupplungen etwa beruehren */
        public void combineWagons() {

            if (trains.length <= 1) return;

            boolean odd = trains.length % 2 == 1;
            int centerIndex = odd ? trains.length / 2 : trains.length / 2 - 1;
            EntityRailCarBase center = trains[centerIndex];
            EntityRailCarBase prev = center;

            for (int i = centerIndex - 1; i >= 0; i--) {
                EntityRailCarBase next = trains[i];
                moveWagonTo(prev, next);
                prev = next;
            }

            prev = center;
            for (int i = centerIndex + 1; i < trains.length; i++) {
                EntityRailCarBase next = trains[i];
                moveWagonTo(prev, next);
                prev = next;
            }
        }

        /** Bewegt einen Waggon zum naechsten, bis sich die Kupplungen etwa beruehren */
        public void moveWagonTo(EntityRailCarBase moveTo, EntityRailCarBase moving) {
            TrainCoupling prevCouple = moveTo.getCouplingFrom(moving);
            TrainCoupling nextCouple = moving.getCouplingFrom(moveTo);
            Vec3NT prevLoc = moveTo.getCouplingPos(prevCouple);
            Vec3NT nextLoc = moving.getCouplingPos(nextCouple);
            Vec3NT delta = Vec3NT.createVectorHelper(prevLoc.xCoord - nextLoc.xCoord, 0, prevLoc.zCoord - nextLoc.zCoord);
            double len = delta.lengthVector();
            len = (len / (0.5D / (len * len) + 1D)); // "smart suspension"
            BlockPos anchor = IRailNTM.pos(moving.getX(), moving.getY(), moving.getZ());
            Vec3NT trainPos = Vec3NT.createVectorHelper(moving.getX(), moving.getY(), moving.getZ());
            float yaw = EntityRailCarBase.generateYaw(prevLoc, nextLoc);
            Vec3NT newPos = EntityRailCarBase.getRelPosAlongRail(anchor, len, moving.getGauge(), moving.level(), trainPos, yaw, new MoveContext(RailCheckType.CORE, 0));
            // Port: im Original NPE im Welt-Tick, hier stattdessen entgleisen
            if (newPos == null) {
                moving.derail();
                this.dissolveTrain();
                return;
            }
            moving.setPos(newPos.xCoord, newPos.yCoord, newPos.zCoord);
            anchor = moving.getCurrentAnchorPos(); // Ursprung auf neue Position setzen
            Vec3NT frontPos = moving.getRelPosAlongRail(anchor, moving.getLengthSpan(), new MoveContext(RailCheckType.FRONT, moving.getCollisionSpan() - moving.getLengthSpan()));
            Vec3NT backPos = moving.getRelPosAlongRail(anchor, -moving.getLengthSpan(), new MoveContext(RailCheckType.BACK, moving.getCollisionSpan() - moving.getLengthSpan()));

            if (frontPos == null || backPos == null) {
                moving.derail();
                this.dissolveTrain();
            } else {
                setRenderPos(moving, frontPos, backPos);
            }
        }

        /** Geschwindigkeit des Zuges bestimmen und ihn entlang der Schiene bewegen (im Original ungenutzt). */
        @Deprecated public void moveTrain() {

            EntityRailCarBase prev = trains[0];
            TrainCoupling dir = prev.getCouplingFrom(null);
            double totalSpeed = 0;
            double maxSpeed = Double.POSITIVE_INFINITY;

            for (EntityRailCarBase train : this.trains) {
                boolean con = train.getCouplingFrom(prev) == dir;
                double speed = train.getCurrentSpeed();
                if (!con) speed *= -1;
                totalSpeed += speed;
                maxSpeed = Math.min(maxSpeed, train.getMaxRailSpeed());
                prev = train;
            }

            if (Math.abs(totalSpeed) > maxSpeed) {
                totalSpeed = maxSpeed * Math.signum(totalSpeed);
            }

            this.moveTrainBy(totalSpeed);
        }

        /** Bewegt den ganzen Zug um eine Geschwindigkeit entlang der Schiene (im Original ungenutzt). */
        @Deprecated public void moveTrainBy(double totalSpeed) {

            for (EntityRailCarBase train : this.trains) {

                BlockPos anchor = train.getCurrentAnchorPos();
                Vec3NT corePos = train.getRelPosAlongRail(anchor, totalSpeed, new MoveContext(RailCheckType.CORE, 0));

                if (corePos == null) {
                    train.derail();
                    this.dissolveTrain();
                    return;
                } else {
                    train.setPos(corePos.xCoord, corePos.yCoord, corePos.zCoord);
                    anchor = train.getCurrentAnchorPos();
                    Vec3NT frontPos = train.getRelPosAlongRail(anchor, train.getLengthSpan(), new MoveContext(RailCheckType.FRONT, 0));
                    Vec3NT backPos = train.getRelPosAlongRail(anchor, -train.getLengthSpan(), new MoveContext(RailCheckType.BACK, 0));

                    if (frontPos == null || backPos == null) {
                        train.derail();
                        this.dissolveTrain();
                        return;
                    } else {
                        train.renderX = (frontPos.xCoord + backPos.xCoord) / 2D;
                        train.renderY = (frontPos.yCoord + backPos.yCoord) / 2D;
                        train.renderZ = (frontPos.zCoord + backPos.zCoord) / 2D;
                        train.yRotO = train.getYRot();
                        train.movementYaw = generateYaw(frontPos, backPos);
                        train.setYRot(train.movementYaw);
                        train.setDeltaMovement(train.getYRot() / 360D, train.getDeltaMovement().y, train.getDeltaMovement().z);
                        train.hurtMarked = true;
                    }
                }
            }
        }

        /** Gesamtgeschwindigkeit der LTU, negativ wenn sie relativ zum willkuerlichen "vorderen" Waggon rueckwaerts faehrt */
        public double getTotalSpeed() {

            EntityRailCarBase prev = trains[0];
            double totalSpeed = 0;
            double maxSpeed = Double.POSITIVE_INFINITY;
            // faehrt der erste Waggon rueckwaerts, alle folgenden ebenfalls umdrehen
            boolean reverseTheReverse = prev.getCouplingFrom(null) == TrainCoupling.BACK;

            if (trains.length == 1) {
                return prev.getCurrentSpeed();
            }

            for (EntityRailCarBase train : this.trains) {
                // falsch herum verkettete Indizes -> Waggon faehrt rueckwaerts, Geschwindigkeit negativ
                boolean reverse = false;

                EntityRailCarBase conFront = train.getCoupledTo(TrainCoupling.FRONT);
                EntityRailCarBase conBack = train.getCoupledTo(TrainCoupling.BACK);

                if (conFront != null && conFront.ltuIndex > train.ltuIndex) reverse = true;
                if (conBack != null && conBack.ltuIndex < train.ltuIndex) reverse = true;

                reverse ^= reverseTheReverse;

                double speed = train.getCurrentSpeed();
                if (reverse) speed *= -1;
                totalSpeed += speed;
                maxSpeed = Math.min(maxSpeed, train.getMaxRailSpeed());
            }

            if (Math.abs(totalSpeed) > maxSpeed) {
                totalSpeed = maxSpeed * Math.signum(totalSpeed);
            }

            return totalSpeed;
        }

        /** Bestimmt den "vorderen" Waggon anhand der Bewegung, bewegt ihn und zieht alle anderen hinterher */
        public void moveTrainByApproach(double speed) {
            EntityRailCarBase previous = null;
            EntityRailCarBase first = this.trains[0];
            boolean forward = speed > 0;
            boolean order = forward ^ first.getCouplingFrom(null) == TrainCoupling.BACK;

            for (int i = order ? 0 : this.trains.length - 1; order ? i < this.trains.length : i >= 0; i += order ? 1 : -1) {
                EntityRailCarBase current = this.trains[i];

                if (previous == null) {

                    if (first == current) speed *= -1;

                    boolean inReverse = first.getCouplingFrom(null) == current.getCouplingFrom(null);
                    int sigNum = inReverse ? 1 : -1;
                    BlockPos anchor = current.getCurrentAnchorPos();

                    Vec3NT frontPos = current.getRelPosAlongRail(anchor, (speed + current.getLengthSpan()) * -sigNum, new MoveContext(RailCheckType.FRONT, current.getCollisionSpan() - current.getLengthSpan()));

                    if (frontPos == null) {
                        current.derail();
                        this.dissolveTrain();
                        return;
                    } else {
                        anchor = current.getCurrentAnchorPos(); // Ursprung auf neue Position setzen
                        Vec3NT corePos = current.getRelPosAlongRail(anchor, speed * -sigNum, new MoveContext(RailCheckType.CORE, 0));
                        // Port: im Original NPE im Welt-Tick, hier stattdessen entgleisen
                        if (corePos == null) {
                            current.derail();
                            this.dissolveTrain();
                            return;
                        }
                        current.setPos(corePos.xCoord, corePos.yCoord, corePos.zCoord);
                        Vec3NT backPos = current.getRelPosAlongRail(anchor, (speed - current.getLengthSpan()) * -sigNum, new MoveContext(RailCheckType.BACK, current.getCollisionSpan() - current.getLengthSpan()));

                        if (backPos == null) {
                            current.derail();
                            this.dissolveTrain();
                            return;
                        } else {
                            setRenderPos(current, inReverse ? backPos : frontPos, inReverse ? frontPos : backPos);
                        }
                    }

                } else {
                    this.moveWagonTo(previous, current);
                }

                previous = current;
            }
        }

        /** Renderposition und Winkel eines Waggons aus vorderem und hinterem Drehgestell */
        public void setRenderPos(EntityRailCarBase current, Vec3NT frontPos, Vec3NT backPos) {
            current.renderX = (frontPos.xCoord + backPos.xCoord) / 2D;
            current.renderY = (frontPos.yCoord + backPos.yCoord) / 2D;
            current.renderZ = (frontPos.zCoord + backPos.zCoord) / 2D;
            current.yRotO = current.getYRot();
            current.movementYaw = generateYaw(frontPos, backPos);
            current.setYRot(current.movementYaw);
            Vec3NT delta = Vec3NT.createVectorHelper(frontPos.xCoord - backPos.xCoord, frontPos.yCoord - backPos.yCoord, frontPos.zCoord - backPos.zCoord);
            current.movementPitch = (float) (Math.asin(delta.yCoord / delta.lengthVector()) * 180D / Math.PI);
            current.setXRot(current.movementPitch);
            // motionX/Y fuer die einfache Synchronisierung zweckentfremdet
            current.setDeltaMovement(current.getYRot() / 360D, current.getXRot() / 360D, current.getDeltaMovement().z);
            current.hurtMarked = true;
        }

        public void collideTrain(double speed) {
            EntityRailCarBase collidingTrain = speed > 0 ? trains[0] : trains[trains.length - 1];
            List<EntityRailCarBase> intersect = collidingTrain.level().getEntitiesOfClass(EntityRailCarBase.class, collidingTrain.getBoundingBox().inflate(1, 1, 1));
            EntityRailCarBase collidesWith = null;

            for (EntityRailCarBase train : intersect) {
                if (train.ltu != null && train.ltu != this) {
                    collidesWith = train;
                    break;
                }
            }

            if (collidesWith == null) return;

            Vec3NT delta = Vec3NT.createVectorHelper(collidingTrain.getX() - collidesWith.getX(), 0, collidingTrain.getZ() - collidesWith.getZ());
            double totalSpan = collidingTrain.getCollisionSpan() + collidesWith.getCollisionSpan();
            double diff = delta.lengthVector();
            if (diff > totalSpan) return;
            double push = (totalSpan - diff);

            EntityRailCarBase[][] whatever = new EntityRailCarBase[][] {{collidingTrain, collidesWith}, {collidesWith, collidingTrain}};
            for (EntityRailCarBase[] array : whatever) {
                LogicalTrainUnit ltu = array[0].ltu;
                if (ltu.trains.length == 1) {
                    Vec3NT rot = Vec3NT.createVectorHelper(0, 0, array[0].getCollisionSpan());
                    rot.rotateAroundX((float) (array[0].getXRot() * Math.PI / 180D));
                    rot.rotateAroundY((float) (-array[0].getYRot() * Math.PI / 180));
                    Vec3NT forward = Vec3NT.createVectorHelper(array[1].getX() - (array[0].getX() + rot.xCoord), 0, array[1].getZ() - (array[0].getZ() + rot.zCoord));
                    Vec3NT backward = Vec3NT.createVectorHelper(array[1].getX() - (array[0].getX() - rot.xCoord), 0, array[1].getZ() - (array[0].getZ() - rot.zCoord));

                    if (forward.lengthVector() > backward.lengthVector()) {
                        ltu.pushForce += push;
                    } else {
                        ltu.pushForce -= push;
                    }
                } else {

                    if (array[0].ltuIndex < ltu.trains.length / 2) {
                        ltu.pushForce -= push;
                    } else {
                        ltu.pushForce += push;
                    }
                }
            }
        }
    }
}
