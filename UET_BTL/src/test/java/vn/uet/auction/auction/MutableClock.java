package vn.uet.auction.auction;

import java.time.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Đồng hồ chỉnh được giờ: test không phải chờ thời gian thật. */
public final class MutableClock extends Clock {
    private final AtomicReference<Instant> currentTime;
    private final ZoneId zone;

    public MutableClock(Instant start) {
        this(new AtomicReference<>(Objects.requireNonNull(start)), ZoneOffset.UTC);
    }

    private MutableClock(AtomicReference<Instant> currentTime, ZoneId zone) {
        this.currentTime = currentTime;
        this.zone = Objects.requireNonNull(zone);
    }

    public void set(Instant time) {
        currentTime.set(Objects.requireNonNull(time));
    }

    public void advance(Duration duration) {
        Objects.requireNonNull(duration);
        currentTime.updateAndGet(time -> time.plus(duration));
    }

    @Override
    public Instant instant() { return currentTime.get(); }

    @Override
    public ZoneId getZone() { return zone; }

    @Override
    public Clock withZone(ZoneId newZone) {
        // Khác múi giờ nhưng cùng một thời điểm đang được điều khiển.
        return new MutableClock(currentTime, newZone);
    }
}
