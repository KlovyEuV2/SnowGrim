package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.BoatUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.math.GrimMath;
import org.jetbrains.annotations.NotNull;

@CheckData(
        name = "SimulationJump",
        setback = 0.0D,
        decay = 0.02D,
        description = "Jump Simulation"
)
public class SimulationJump extends Check implements PostPredictionCheck {
    private boolean wasFlagged = false;
    private boolean tpb;

    private boolean isJumping;
    private String reason;

    public SimulationJump(@NotNull GrimPlayer player) {
        super(player);
    }
    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked() || predictionComplete.getData().isTeleport()
                || player.packetStateData.lastPacketWasTeleport || player.getSetbackTeleportUtil().blockOffsets
                || player.predictedVelocity.isExplosion() || player.predictedVelocity.isKnockback()
                || player.predictedVelocity.isTrident() || !player.predictedVelocity.isJump()) return;

        boolean isExempt = this.player.isClimbing || this.player.wasClimbing || this.player.compensatedWorld.isNearHardEntity(this.player.boundingBox)
                || this.player.uncertaintyHandler.isStepMovement || this.player.uncertaintyHandler.wasStepMovement ||
                this.player.packetStateData.lastPacketWasTeleport || this.player.getSetbackTeleportUtil().blockOffsets || this.player.wasGliding != this.player.isGliding
                ||this.player.inVehicle() || BoatUtil.getBoatStep(player.move()) != null
                || player.uncertaintyHandler.isSteppingNearBubbleColumn;
        if (player.disableGrim || isExempt) return;

        boolean flagged = false;

        boolean inVehicle = this.player.inVehicle();
        isJumping = this.player.predictedVelocity.isJump();

        // check
        flagged = checkYJump();

        if (flagged) {
            flagAndAlert(reason);
            if (tpb) {
                this.player.getSetbackTeleportUtil().teleportBack();
            }
        } else {
            removeOffsetLenience();
        }

        wasFlagged = flagged;
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

        double deltaY = player.deltaY();
        double maxY_Speed = GrimMath.getPlayerMBPT(player, GrimMath.SpeedType.Y_UP);

        isCheat = isJumping && deltaY > maxY_Speed;
        if (isCheat) {
            reason = "jump, delta=" + String.format("%.4f",deltaY) + "/" + String.format("%.4f",maxY_Speed);
            if (tpb) giveOffsetLenienceNextTick(deltaY);
        }
        return isCheat;
    }
    public void onReload(ConfigManager config) {
        this.tpb = config.getBooleanElse(this.getConfigName() + ".setback", true);
    }
}
