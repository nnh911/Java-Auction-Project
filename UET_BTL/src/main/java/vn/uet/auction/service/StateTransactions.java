package vn.uet.auction.service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.persistence.StateStore;

/** Owns the live snapshot and the lock shared by the application services. */
public final class StateTransactions {
    private final StateStore store;
    private final ReentrantLock lock = new ReentrantLock();
    private final ApplicationState state;

    public StateTransactions(StateStore store) throws IOException {
        this.store = Objects.requireNonNull(store, "store");
        this.state = store.load();
    }

    public ReentrantLock lock() {
        return lock;
    }

    ApplicationState state() {
        return state;
    }

    public <T> T read(Function<ApplicationState, T> operation) {
        lock.lock();
        try {
            return operation.apply(state);
        } finally {
            lock.unlock();
        }
    }

    public <T> T write(Function<ApplicationState, T> operation, Predicate<T> changed)
            throws IOException {
        lock.lock();
        try {
            ApplicationState candidate = new ApplicationState(state);
            T result = operation.apply(candidate);
            if (changed.test(result)) {
                store.save(candidate);
                replace(state.getUsers(), candidate.getUsers());
                replace(state.getItems(), candidate.getItems());
                replace(state.getAccounts(), candidate.getAccounts());
                replace(state.getAuctions(), candidate.getAuctions());
                replace(state.getBids(), candidate.getBids());
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    private static <K, V> void replace(Map<K, V> live, Map<K, V> saved) {
        live.clear();
        live.putAll(saved);
    }
}
