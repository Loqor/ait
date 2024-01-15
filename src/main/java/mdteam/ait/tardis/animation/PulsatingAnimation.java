package mdteam.ait.tardis.animation;

import mdteam.ait.AITMod;
import mdteam.ait.core.AITSounds;
import mdteam.ait.core.blockentities.ExteriorBlockEntity;
import mdteam.ait.core.sounds.MatSound;
import mdteam.ait.tardis.TardisTravel;
import mdteam.ait.tardis.util.TardisUtil;

public class PulsatingAnimation extends ExteriorAnimation {
    private int pulses = 0;
    private int PULSE_LENGTH = 20;
    private float frequency, intensity;

    public PulsatingAnimation(ExteriorBlockEntity exterior) {
        super(exterior);
    }

    @Override
    public void tick() {
        if (exterior.getTardis() == null)
            return;

        TardisTravel.State state = TardisUtil.isClient() ? exterior.getClientTardis().getTravel().getState() : exterior.getTardis().getTravel().getState();


        if (this.timeLeft < 0)
            this.setupAnimation(state); // @TODO: is a jank fix for the timeLeft going negative on client

        if (state == TardisTravel.State.DEMAT) {
            this.setAlpha(1f - getPulseAlpha());
            timeLeft--;

            runAlphaChecks(state);
        } else if (state == TardisTravel.State.MAT) {
            timeLeft--;

            if (timeLeft < startTime)
                this.setAlpha(getPulseAlpha());
            else
                this.setAlpha(0f);

            runAlphaChecks(state);
        } else if (state == TardisTravel.State.LANDED/* && alpha != 1f*/) {
            this.setAlpha(1f);
        }
    }

    public float getPulseAlpha() {
        if (timeLeft != maxTime && timeLeft % PULSE_LENGTH == 0)
            pulses++;

        return (float) ((float) (pulses / Math.floor((double) maxTime / PULSE_LENGTH)) + (Math.cos(timeLeft * frequency) * intensity)); // @TODO find alternative math or ask cwaig if we're allowed to use this, loqor says "its just math" but im still saying this just in case.
    }

    @Override
    public void setupAnimation(TardisTravel.State state) {
        if (exterior.getClientTardis() == null || exterior.getClientTardis().getExterior().getExteriorSchema() == null) {
            AITMod.LOGGER.error("Tardis for exterior " + exterior + " was null! Panic!!!!");
            alpha = 0f; // just make me vanish.
            return;
        }

        MatSound sound = TardisUtil.isClient() ? exterior.getClientTardis().getExterior().getExteriorVariantSchema().getSound(state) : exterior.getTardis().getExterior().getVariant().getSound(state);

        if (TardisUtil.isClient() ? exterior.getClientTardis().isCrashing() : exterior.getTardis().getTravel().isCrashing()) {
            sound = AITSounds.GHOST_MAT_ANIM;
        }

        this.tellClientsToSetup(state);

        timeLeft = sound.timeLeft();
        maxTime = sound.maxTime();
        frequency = sound.frequency();
        intensity = sound.intensity();
        startTime = sound.startTime();

        if (state == TardisTravel.State.DEMAT) {
            alpha = 1f;
        } else if (state == TardisTravel.State.MAT) {
            alpha = 0f;
        } else if (state == TardisTravel.State.LANDED) {
            alpha = 1f;
        }

        pulses = 0;
    }
}
