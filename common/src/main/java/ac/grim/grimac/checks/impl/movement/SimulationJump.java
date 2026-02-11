package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.BoatUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.GrimMath.SpeedType;
import ac.grim.grimac.utils.simulation.SimulationUtil;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

@CheckData(
        name = "SimulationJump",
        setback = 0.0D,
        decay = 0.02D,
        description = "Jump Simulation"
)
public class SimulationJump extends Check implements PostPredictionCheck {
    private boolean wasFlagged = false;
    private boolean tpb;
    private double offset;
    private boolean isJumping;
    private String reason = "";

    public SimulationJump(@NotNull GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (predictionComplete.isChecked() && !predictionComplete.getData().isTeleport() && !this.player.packetStateData.lastPacketWasTeleport && !this.player.getSetbackTeleportUtil().blockOffsets && !this.player.predictedVelocity.isExplosion() && !this.player.predictedVelocity.isKnockback() && !this.player.predictedVelocity.isTrident() && this.player.predictedVelocity.isJump()) {
            boolean isExempt = this.player.isClimbing || this.player.wasClimbing || this.player.uncertaintyHandler.isStepMovement || this.player.uncertaintyHandler.wasStepMovement || this.player.packetStateData.lastPacketWasTeleport || this.player.getSetbackTeleportUtil().blockOffsets || this.player.wasGliding != this.player.isGliding || this.player.inVehicle() || BoatUtil.getBoatStep(this.player.move()) != null || this.player.uncertaintyHandler.isSteppingNearBubbleColumn;
            if (!this.player.disableGrim && !isExempt) {
                boolean flagged = false;
                boolean inVehicle = this.player.inVehicle();
                this.isJumping = this.player.predictedVelocity.isJump();
                flagged = this.checkYJump();
                boolean jumpExempt = this.player.wasTouchingLava || this.player.wasTouchingWater
                        || SimulationUtil.isBoatJumpCollision(player.deltaY(), 0.0001);

                OptionalInt slvl = player.compensatedEntities.self.getPotionEffectLevel(PotionTypes.JUMP_BOOST);
                if (!SimulationUtil.isJump(slvl.isPresent() ? slvl.getAsInt() : 0, player.deltaY(), 0.0001) && !jumpExempt) {
                    flagged = true;
                    String var10001 = !this.reason.isEmpty() ? this.reason + "; " : "";
                    this.reason = var10001 + " move-y=" + this.player.move().y;
                }

                if (flagged) {
                    this.flagAndAlert(this.reason);
                    if (this.tpb) {
                        this.player.getSetbackTeleportUtil().teleportBack();
                    }
                } else {
                    this.removeOffsetLenience();
                }

                this.wasFlagged = flagged;
                this.isJumping = false;
                this.reason = "";
            }
        }
    }

    private void giveOffsetLenienceNextTick(double offset) {
        double minimizedOffset = Math.min(offset, 1.0D);
        this.player.uncertaintyHandler.lastHorizontalOffset = minimizedOffset;
        this.player.uncertaintyHandler.lastVerticalOffset = minimizedOffset;
    }

    private void removeOffsetLenience() {
        this.player.uncertaintyHandler.lastHorizontalOffset = 0.0D;
        this.player.uncertaintyHandler.lastVerticalOffset = 0.0D;
    }

    public boolean checkYJump() {
        boolean isCheat = false;
        double deltaY = this.player.move().y;
        double maxY_Speed = GrimMath.getPlayerMBPT(this.player, SpeedType.Y_UP);
        isCheat = this.isJumping && deltaY > maxY_Speed;
        if (isCheat) {
            String var10001 = !GrimAPI.DEV_MODE ? String.format("%.4f", deltaY) : String.valueOf(deltaY);
            this.reason = "jump, delta=" + var10001 + "/" + String.format("%.4f", maxY_Speed);
            if (this.tpb) {
                this.giveOffsetLenienceNextTick(deltaY);
            }
        }

        return isCheat;
    }

    public void onReload(ConfigManager config) {
        this.tpb = config.getBooleanElse(this.getConfigName() + ".setback", true);
        this.offset = config.getDoubleElse(this.getConfigName() + ".offset", 0.001D);
    }
}
