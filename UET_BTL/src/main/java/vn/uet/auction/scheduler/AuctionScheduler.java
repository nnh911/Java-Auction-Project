package vn.uet.auction.scheduler;

import java.util.List;
import vn.uet.auction.service.StatusChange;

/** Week 7 manual tick boundary. No background thread or lifecycle is started. */
@FunctionalInterface
public interface AuctionScheduler {
    List<StatusChange> tick();
}
