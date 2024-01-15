package mdteam.ait.tardis.animation;

import mdteam.ait.AITMod;
import mdteam.ait.core.AITSounds;
import mdteam.ait.core.blockentities.ExteriorBlockEntity;
import mdteam.ait.core.sounds.MatSound;
import mdteam.ait.tardis.TardisTravel;
import mdteam.ait.tardis.util.TardisUtil;

@Deprecated
public class ClassicAnimation extends ExteriorAnimation {
    public ClassicAnimation(ExteriorBlockEntity exterior) {
        super(exterior);
    }

    @Override
    public void tick() {
        if ((TardisUtil.isClient() ? exterior.getClientTardis() : exterior.getTardis()) == null)
            return;

        TardisTravel.State state = TardisUtil.isClient() ? exterior.getClientTardis().getTravel().getState() : exterior.getTardis().getTravel().getState();

        if (this.timeLeft < 0)
            this.setupAnimation(state); // fixme is a jank fix for the timeLeft going negative on client

        if (state == TardisTravel.State.DEMAT) {
            this.alpha = (float) this.timeLeft / (this.startTime);
            this.timeLeft--;

            runAlphaChecks(state);
        } else if (state == TardisTravel.State.MAT) {
            // Maybe this will fix the class animation taking too long
            this.alpha = ((float) this.timeLeft / (this.startTime) - 1) * -1;
            this.timeLeft--;
            this.setAlpha(this.alpha);
            runAlphaChecks(state);
        } else if (state == TardisTravel.State.LANDED/* && alpha != 1f*/) {
            this.setAlpha(1f);
        }
    }

    @Override
    public void setupAnimation(TardisTravel.State state) {
        if ((TardisUtil.isClient() ? exterior.getClientTardis() : exterior.getTardis()) == null) {
            AITMod.LOGGER.error("Tardis for exterior " + exterior + " was null! Panic!!!!");
            alpha = 0f; // just make me vanish.
            return;
        }
        MatSound sound = TardisUtil.isClient() ? exterior.getClientTardis().getExterior().getExteriorVariantSchema().getSound(state) : exterior.getTardis().getExterior().getVariant().getSound(state);

        if ((TardisUtil.isClient() ? exterior.getClientTardis().isCrashing() : exterior.getTardis().getTravel().isCrashing())) {
            sound = AITSounds.GHOST_MAT_ANIM;
        }

        this.timeLeft = sound.timeLeft();
        this.maxTime = sound.maxTime();
        this.startTime = sound.startTime();

        if (state == TardisTravel.State.DEMAT) {
            this.timeLeft = 390;
            this.maxTime = 390;
            this.startTime = 390;
            this.alpha = 1f;
        } else if (state == TardisTravel.State.MAT) {
            this.alpha = 0f;
        } else if (state == TardisTravel.State.LANDED) {
            this.alpha = 1f;
        }
    }
}
