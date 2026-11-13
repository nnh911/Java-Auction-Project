package vn.uet.auction.scheduler;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import vn.uet.auction.service.StatusChange;

/** Boundary for refreshing auction status against the service clock. */
@FunctionalInterface
public interface AuctionScheduler {
    List<StatusChange> tick();

    /** Periodic execution owned by the application component that creates it. */
    final class Periodic implements AuctionScheduler, AutoCloseable {
        private static final System.Logger LOGGER = System.getLogger(Periodic.class.getName());

        private final AuctionScheduler delegate;
        private final long intervalNanos;
        private ScheduledExecutorService executor;
        private boolean stopped;

        public Periodic(AuctionScheduler delegate, Duration interval) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.intervalNanos = Objects.requireNonNull(interval, "interval").toNanos();
            if (intervalNanos <= 0) {
                throw new IllegalArgumentException("Interval must be positive");
            }
        }

        @Override
        public List<StatusChange> tick() {
            return delegate.tick();
        }

        /** Run an immediate tick, then wait the configured interval after each tick. */
        public synchronized void start() {
            if (stopped) {
                throw new IllegalStateException("Scheduler has been stopped");
            }
            if (executor == null) {
                executor = Executors.newSingleThreadScheduledExecutor(task -> {
                    Thread thread = new Thread(task, "auction-scheduler");
                    thread.setDaemon(true);
                    return thread;
                });
                executor.scheduleWithFixedDelay(this::runTick, 0, intervalNanos, TimeUnit.NANOSECONDS);
            }
        }

        private void runTick() {
            try {
                tick();
            } catch (RuntimeException failure) {
                LOGGER.log(System.Logger.Level.WARNING, "Auction scheduler tick failed", failure);
            }
        }

        /** Cancel future ticks. An in-flight tick may finish normally. */
        public synchronized void stop() {
            stopped = true;
            if (executor != null) {
                executor.shutdown();
            }
        }

        @Override
        public void close() {
            stop();
        }
    }
}
