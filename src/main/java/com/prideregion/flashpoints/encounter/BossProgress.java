package com.prideregion.flashpoints.encounter;

import java.util.Objects;
import java.util.UUID;

/**
 * A deliberately manual staging transition engine.
 * NO Cobblemon callbacks, rewards or player Pokemon modifications occur here.
 */
public final class BossProgress {
    public enum Stage { READY, ACTIVE, CAPTURED, DEFEATED, WIPED }
    public enum Action { START, CAPTURE, DEFEAT, WIPE, RETRY }

    public record Snapshot(EncounterLedger.Key key,Stage stage,int attempts,UUID operator,long updatedAtMillis) {
        public Snapshot {
            Objects.requireNonNull(key,"key");
            Objects.requireNonNull(stage,"stage");
            Objects.requireNonNull(operator,"operator");
            if(attempts<0 || attempts>10000 || updatedAtMillis<0)
                throw new IllegalArgumentException("Invalid attempt count or timestamp");
            if(stage!=Stage.READY && attempts==0)
                throw new IllegalArgumentException("A non-ready boss requires at least one attempt");
        }
        public boolean terminal() {return stage==Stage.CAPTURED || stage==Stage.DEFEATED;}
    }

    private BossProgress() {}

    public static Snapshot initial(EncounterLedger.Key key,UUID operator) {
        return new Snapshot(key,Stage.READY,0,operator,0L);
    }

    /**
     * No implicit transitions. Terminal outcomes are final, and a wipe
     * requires a separate RETRY action before another resolution.
     */
    public static Snapshot transition(Snapshot prior,Action action,UUID operator,long timestamp) {
        Objects.requireNonNull(prior);
        Objects.requireNonNull(action);
        Objects.requireNonNull(operator);
        if(timestamp<0)throw new IllegalArgumentException("Invalid event timestamp");
        Stage next;
        int attempts=prior.attempts();
        switch(action) {
            case START -> {
                if(prior.stage()!=Stage.READY)throw new IllegalStateException("Only READY can START");
                next=Stage.ACTIVE;attempts++;
            }
            case RETRY -> {
                if(prior.stage()!=Stage.WIPED)throw new IllegalStateException("Only WIPED can RETRY");
                next=Stage.ACTIVE;attempts++;
            }
            case CAPTURE -> {
                if(prior.stage()!=Stage.ACTIVE)throw new IllegalStateException("CAPTURE requires ACTIVE");
                next=Stage.CAPTURED;
            }
            case DEFEAT -> {
                if(prior.stage()!=Stage.ACTIVE)throw new IllegalStateException("DEFEAT requires ACTIVE");
                next=Stage.DEFEATED;
            }
            case WIPE -> {
                if(prior.stage()!=Stage.ACTIVE)throw new IllegalStateException("WIPE requires ACTIVE");
                next=Stage.WIPED;
            }
            default -> throw new IllegalArgumentException("Unknown transition");
        }
        return new Snapshot(prior.key(),next,attempts,operator,timestamp);
    }
}
