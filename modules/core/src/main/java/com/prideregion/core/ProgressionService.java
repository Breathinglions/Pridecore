package com.prideregion.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Single in-process progression gateway. Execute on the Minecraft server thread.
 * Every change is persisted before reporting success; no rewards or external
 * command tags are mutated here. Synchronized to serialize code paths in this JVM.
 *
 * Not a multi-server database and NOT an exactly-once reward dispatcher.
 */
public final class ProgressionService {
    public record Change(ProfileState profile, boolean changed) {}
    private final ProfileStore store;

    public ProgressionService(ProfileStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public synchronized ProfileState get(UUID playerId) throws IOException {
        return store.load(playerId);
    }

    public synchronized Change transition(UUID playerId, String stage,
                                          Collection<String> grant, Collection<String> revoke)
            throws IOException {
        ProfileState before = store.load(playerId);
        ProfileState after = before.transition(stage, grant, revoke);
        if (after == before) return new Change(before, false);
        store.save(after);
        return new Change(after, true);
    }

    public Change setStage(UUID playerId, String stage) throws IOException {
        return transition(playerId, stage, Set.of(), Set.of());
    }

    public Change grant(UUID playerId, String fact) throws IOException {
        return transition(playerId, null, Set.of(fact), Set.of());
    }

    public Change revoke(UUID playerId, String fact) throws IOException {
        return transition(playerId, null, Set.of(), Set.of(fact));
    }

    /** Legacy tags are read-only and never imported into Pride Core ownership. */
    public synchronized ProgressionView view(UUID playerId, Collection<String> commandTags)
            throws IOException {
        return new ProgressionView(store.load(playerId),
                LegacyTagView.progressionTags(commandTags));
    }
}
