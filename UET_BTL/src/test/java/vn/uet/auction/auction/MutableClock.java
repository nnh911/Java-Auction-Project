package vn.uet.auction.auction;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Đồng hồ giả cho test: tự chỉnh giờ, không phải chờ thật. (TV4 sẽ có bản chung của nhóm; thay sau.) */
public final class MutableClock extends Clock {
    private volatile Instant now;

    public MutableClock(Instant start) { this.now = start; }

    public void set(Instant instant) { this.now = instant; }

    public void advance(Duration d) { this.now = now.plus(d); }

    @Override public Instant instant() { return now; }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
}