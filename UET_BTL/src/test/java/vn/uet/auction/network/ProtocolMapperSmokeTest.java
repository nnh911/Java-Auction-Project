package vn.uet.auction.network;

import java.io.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import vn.uet.auction.model.auction.*;
import vn.uet.auction.model.item.*;
import vn.uet.auction.model.user.*;
import vn.uet.auction.service.AuctionSnapshot;
import vn.uet.auction.protocol.mapper.ProtocolMapper;

/** Test console độc lập Maven; không dùng fake production hoặc private API. */
public final class ProtocolMapperSmokeTest {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Mapping mismatch");
    }

    public static void main(String[] args) throws Exception {
        UUID seller = UUID.randomUUID(), itemId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID(), bidder = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-01T00:00:00Z"), end = start.plusSeconds(60);
        var user = ProtocolMapper.toUserSummary(new Bidder(bidder, "bidder_demo"));
        check(user.id().equals(bidder) && user.role() == Role.BIDDER
                && user.username().equals("bidder_demo"));
        Item item = new Art(itemId, seller, "Painting", "Description");
        var summary = ProtocolMapper.toItemSummary(item);
        var detail = ProtocolMapper.toItemDetail(item);
        check(summary.id().equals(itemId) && summary.sellerId().equals(seller)
                && summary.name().equals("Painting") && summary.type() == ItemType.ART);
        check(detail.description().equals("Description") && detail.id().equals(summary.id())
                && detail.sellerId().equals(seller) && detail.type() == summary.type()
                && detail.name().equals(summary.name()));
        Bid bid = new Bid(UUID.randomUUID(), auctionId, bidder, new BigDecimal("200"), start);
        var mappedBid = ProtocolMapper.toBidSummary(bid);
        check(mappedBid.id().equals(bid.getId()) && mappedBid.auctionId().equals(auctionId)
                && mappedBid.bidderId().equals(bidder) && mappedBid.amount().equals(bid.getAmount())
                && mappedBid.acceptedAt().equals(start));
        var snapshot = new AuctionSnapshot(auctionId, itemId, seller, new BigDecimal("100"),
                bid.getAmount(), start, end, AuctionStatus.FINISHED, bidder, bidder, List.of(bid));
        var auction = ProtocolMapper.toAuctionDetail(snapshot);
        var row = ProtocolMapper.toAuctionSummary(snapshot);
        check(auction.id().equals(auctionId) && auction.itemId().equals(itemId)
                && auction.sellerId().equals(seller) && auction.startTime().equals(start)
                && auction.endTime().equals(end) && auction.winnerId().equals(bidder)
                && auction.startingPrice().equals(snapshot.startingPrice())
                && auction.currentPrice().equals(snapshot.currentPrice())
                && auction.status() == snapshot.status() && auction.history().equals(List.of(mappedBid)));
        check(row.id().equals(auctionId) && row.itemId().equals(itemId)
                && row.sellerId().equals(seller) && row.currentPrice().equals(snapshot.currentPrice())
                && row.status() == snapshot.status() && row.startTime().equals(start)
                && row.endTime().equals(end));
        var empty = new AuctionSnapshot(auctionId, itemId, seller, new BigDecimal("100"),
                new BigDecimal("100"), start, end, AuctionStatus.OPEN, null, null, List.of());
        check(ProtocolMapper.toAuctionDetail(empty).winnerId() == null
                && ProtocolMapper.toAuctionDetail(empty).history().isEmpty());
        try { auction.history().clear(); throw new AssertionError("Mutable history"); }
        catch (UnsupportedOperationException expected) { }
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) { output.writeObject(auction); }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            check(auction.equals(input.readObject()));
        }
        List<Runnable> nullCalls = List.of(
                () -> ProtocolMapper.toUserSummary(null), () -> ProtocolMapper.toItemSummary(null),
                () -> ProtocolMapper.toItemDetail(null), () -> ProtocolMapper.toBidSummary(null),
                () -> ProtocolMapper.toAuctionSummary(null), () -> ProtocolMapper.toAuctionDetail(null));
        for (Runnable call : nullCalls) {
            try { call.run(); throw new AssertionError("Accepted null"); }
            catch (NullPointerException expected) { }
        }
        System.out.println("ProtocolMapperSmokeTest PASS");
    }
}
