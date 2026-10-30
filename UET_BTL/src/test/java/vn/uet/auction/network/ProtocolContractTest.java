// FILE KIỂM THỬ KHUNG — CHƯA CÓ TEST CHẠY ĐƯỢC.
// Phụ trách: Thành viên 3. Tuần học kỳ: 8–10.
// Package dự kiến: vn.uet.auction.network
// Kịch bản cần tự viết: Command/payload/DTO/errorCode/requestId theo hợp đồng.
// TODO: Tự khai báo package/class, JUnit test, fixture và assertion.
// Chưa có kết quả PASS hoặc coverage cho dự án khung này.
package vn.uet.auction.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.protocol.Command;
import vn.uet.auction.protocol.ErrorCode;
import vn.uet.auction.protocol.EventMessage;
import vn.uet.auction.protocol.Request;
import vn.uet.auction.protocol.Request.CreateAuctionPayload;
import vn.uet.auction.protocol.Request.CreateItemPayload;
import vn.uet.auction.protocol.Request.LoginPayload;
import vn.uet.auction.protocol.Request.PlaceBidPayload;
import vn.uet.auction.protocol.Response;
import vn.uet.auction.protocol.dto.AuctionDetail;
import vn.uet.auction.protocol.dto.AuctionSummary;
import vn.uet.auction.protocol.dto.BidSummary;
import vn.uet.auction.protocol.dto.ItemDetail;
import vn.uet.auction.protocol.dto.ItemSummary;
import vn.uet.auction.protocol.dto.UserSummary;

/**
 * Kiểm thử hợp đồng package {@code vn.uet.auction.protocol} (TV3, tuần 6).
 *
 * <p>Mỗi lớp @Nested kiểm thử đúng một file. Chỉ khẳng định hành vi code hiện có và
 * các điều kiện ghi trong kế hoạch mục 3.4; api-contract.md còn TODO nên những quy tắc
 * chưa được nhóm duyệt (độ dài tên, giá > 0, start < end...) KHÔNG được khẳng định ở đây.
 *
 * <p>Role/ItemType do TV1 sở hữu nên test dùng {@code values()[0]} thay vì gọi tên hằng.
 */
class ProtocolContractTest {

    private static final UUID ID_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID ID_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID ID_C = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID ID_D = UUID.fromString("00000000-0000-0000-0000-00000000000d");
    private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-01T11:00:00Z");
    private static final BigDecimal STARTING = new BigDecimal("100000");
    private static final BigDecimal CURRENT = new BigDecimal("150000");
    private static final String SECRET = "s3cret-pw";

    // ---------- dữ liệu mẫu ----------

    private static Role anyRole() {
        return Role.values()[0];
    }

    private static ItemType anyItemType() {
        return ItemType.values()[0];
    }

    private static UserSummary user() {
        return new UserSummary(ID_A, "alice", anyRole());
    }

    private static ItemSummary itemSummary() {
        return new ItemSummary(ID_B, ID_A, "Laptop", anyItemType());
    }

    private static ItemDetail itemDetail() {
        return new ItemDetail(ID_B, ID_A, "Laptop", "Máy còn bảo hành", anyItemType());
    }

    private static BidSummary bid() {
        return new BidSummary(ID_C, ID_D, ID_A, CURRENT, T0);
    }

    private static AuctionSummary auctionSummary() {
        return new AuctionSummary(ID_D, ID_B, ID_A, CURRENT, AuctionStatus.RUNNING, T0, T1);
    }

    private static AuctionDetail auctionDetail(UUID winnerId, List<BidSummary> history) {
        return new AuctionDetail(ID_D, ID_B, ID_A, STARTING, CURRENT,
                AuctionStatus.FINISHED, T0, T1, winnerId, history);
    }

    // ---------- tiện ích ----------

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    private static void assertNpe(Executable action) {
        assertThrows(NullPointerException.class, action);
    }

    private static Set<String> namesOf(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toCollection(TreeSet::new));
    }

    private static void assertContainsAll(Set<String> actual, Set<String> required) {
        Set<String> missing = new TreeSet<>(required);
        missing.removeAll(actual);
        assertTrue(missing.isEmpty(), "Thiếu: " + missing);
    }

    // =====================================================================
    // Command.java
    // =====================================================================
    @Nested
    class CommandContract {
        @Test
        void containsEveryCommandOfPlanSection34() {
            Set<String> required = Set.of(
                    "REGISTER", "LOGIN", "LIST_USERS", "SET_USER_ACTIVE",
                    "CREATE_ITEM", "LIST_ITEMS", "GET_ITEM", "UPDATE_ITEM", "DELETE_ITEM",
                    "CREATE_AUCTION", "LIST_AUCTIONS", "GET_AUCTION",
                    "PLACE_BID", "CONFIRM_PAYMENT", "CANCEL_AUCTION");
            assertContainsAll(namesOf(Command.values()), required);
        }
    }

    // =====================================================================
    // ErrorCode.java  (ĐỀ XUẤT, chưa được TV1/TV2/TV4 duyệt)
    // =====================================================================
    @Nested
    class ErrorCodeContract {
        @Test
        void containsProposedCodes() {
            Set<String> proposed = Set.of(
                    "INVALID_REQUEST", "UNSUPPORTED_COMMAND", "AUTHENTICATION_FAILED",
                    "UNAUTHENTICATED", "FORBIDDEN", "NOT_FOUND", "CONFLICT",
                    "INVALID_BID", "AUCTION_CLOSED", "INTERNAL_ERROR");
            assertContainsAll(namesOf(ErrorCode.values()), proposed);
        }

        @Test
        void everyCodeCanBeCarriedByAFailureResponse() {
            for (ErrorCode code : ErrorCode.values()) {
                Response<UserSummary> response = Response.fail(ID_A, code, "lỗi");
                assertEquals(code, response.errorCode());
            }
        }
    }

    // =====================================================================
    // Request.java và các payload
    // =====================================================================
    @Nested
    class RequestContract {
        @Test
        void keepsRequestIdCommandAndPayload() {
            LoginPayload payload = new LoginPayload("alice", SECRET);
            Request request = new Request(ID_A, Command.LOGIN, payload);
            assertEquals(ID_A, request.requestId());
            assertEquals(Command.LOGIN, request.command());
            assertEquals(payload, request.payload());
        }

        @Test
        void rejectsNullComponents() {
            LoginPayload payload = new LoginPayload("alice", SECRET);
            assertNpe(() -> new Request(null, Command.LOGIN, payload));
            assertNpe(() -> new Request(ID_A, null, payload));
            assertNpe(() -> new Request(ID_A, Command.LOGIN, null));
        }

        @Test
        void loginPayloadRejectsNullFields() {
            assertNpe(() -> new LoginPayload(null, SECRET));
            assertNpe(() -> new LoginPayload("alice", null));
        }

        @Test
        void loginPayloadAndRequestNeverPrintPassword() {
            LoginPayload payload = new LoginPayload("alice", SECRET);
            assertFalse(payload.toString().contains(SECRET), "LoginPayload.toString lộ password");
            Request request = new Request(ID_A, Command.LOGIN, payload);
            assertFalse(request.toString().contains(SECRET), "Request.toString lộ password");
        }

        @Test
        void createItemPayloadRejectsNullFields() {
            assertNpe(() -> new CreateItemPayload(null, "Laptop", "mô tả"));
            assertNpe(() -> new CreateItemPayload(anyItemType(), null, "mô tả"));
            assertNpe(() -> new CreateItemPayload(anyItemType(), "Laptop", null));
        }

        @Test
        void createAuctionPayloadRejectsNullFields() {
            assertNpe(() -> new CreateAuctionPayload(null, STARTING, T0, T1));
            assertNpe(() -> new CreateAuctionPayload(ID_B, null, T0, T1));
            assertNpe(() -> new CreateAuctionPayload(ID_B, STARTING, null, T1));
            assertNpe(() -> new CreateAuctionPayload(ID_B, STARTING, T0, null));
        }

        @Test
        void placeBidPayloadRejectsNullFields() {
            assertNpe(() -> new PlaceBidPayload(null, CURRENT));
            assertNpe(() -> new PlaceBidPayload(ID_D, null));
        }

        @Test
        void payloadInterfaceIsSealedAndPermitsCurrentPayloads() {
            assertTrue(Request.Payload.class.isSealed());
            List<Class<?>> permitted = Arrays.asList(Request.Payload.class.getPermittedSubclasses());
            assertTrue(permitted.containsAll(List.of(LoginPayload.class, CreateItemPayload.class,
                    CreateAuctionPayload.class, PlaceBidPayload.class)));
        }

        @Test
        void exampleRequestsSurviveSerialization() throws Exception {
            List<Request> examples = List.of(
                    new Request(ID_A, Command.LOGIN, new LoginPayload("alice", SECRET)),
                    new Request(ID_A, Command.CREATE_ITEM,
                            new CreateItemPayload(anyItemType(), "Laptop", "mô tả")),
                    new Request(ID_A, Command.CREATE_AUCTION,
                            new CreateAuctionPayload(ID_B, STARTING, T0, T1)),
                    new Request(ID_A, Command.PLACE_BID, new PlaceBidPayload(ID_D, CURRENT)));
            for (Request request : examples) {
                assertEquals(request, roundTrip(request), request.command().name());
            }
        }
    }

    // =====================================================================
    // Response.java
    // =====================================================================
    @Nested
    class ResponseContract {
        @Test
        void okCarriesDataAndNoError() {
            UserSummary data = user();
            Response<UserSummary> response = Response.ok(ID_A, data);
            assertEquals(ID_A, response.requestId());
            assertTrue(response.success());
            assertNull(response.errorCode());
            assertEquals("", response.message());
            assertEquals(data, response.data());
        }

        @Test
        void okRejectsNullData() {
            assertNpe(() -> Response.<UserSummary>ok(ID_A, null));
        }

        @Test
        void failCarriesErrorCodeAndMessageButNoData() {
            Response<UserSummary> response = Response.fail(ID_A, ErrorCode.NOT_FOUND, "Không thấy");
            assertFalse(response.success());
            assertEquals(ErrorCode.NOT_FOUND, response.errorCode());
            assertEquals("Không thấy", response.message());
            assertNull(response.data());
        }

        @Test
        void failRejectsMissingErrorCodeOrMessage() {
            assertNpe(() -> Response.<UserSummary>fail(ID_A, null, "lỗi"));
            assertNpe(() -> Response.<UserSummary>fail(ID_A, ErrorCode.NOT_FOUND, null));
            assertThrows(IllegalArgumentException.class,
                    () -> Response.<UserSummary>fail(ID_A, ErrorCode.NOT_FOUND, ""));
            assertThrows(IllegalArgumentException.class,
                    () -> Response.<UserSummary>fail(ID_A, ErrorCode.NOT_FOUND, "   "));
        }

        @Test
        void rejectsRequestIdNull() {
            assertNpe(() -> Response.ok(null, user()));
            assertNpe(() -> Response.<UserSummary>fail(null, ErrorCode.NOT_FOUND, "lỗi"));
        }

        @Test
        void constructorRejectsInconsistentSuccessAndFailure() {
            // success nhưng có errorCode
            assertThrows(IllegalArgumentException.class,
                    () -> new Response<UserSummary>(ID_A, true, ErrorCode.NOT_FOUND, "", user()));
            // success nhưng thiếu data
            assertNpe(() -> new Response<UserSummary>(ID_A, true, null, "", null));
            // failure nhưng có data
            assertThrows(IllegalArgumentException.class,
                    () -> new Response<UserSummary>(ID_A, false, ErrorCode.NOT_FOUND, "lỗi", user()));
            // failure nhưng thiếu errorCode
            assertNpe(() -> new Response<UserSummary>(ID_A, false, null, "lỗi", null));
        }

        /**
         * Response yêu cầu T extends Serializable, mà List/List.of không phải Serializable.
         * Vì vậy LIST_USERS/LIST_ITEMS/LIST_AUCTIONS phải trả ArrayList (hoặc DTO bọc danh sách).
         * Cần nhóm chốt cách nào; test này chỉ chứng minh ArrayList dùng được.
         */
        @Test
        void listResultsWorkWhenWrappedInSerializableContainer() throws Exception {
            ArrayList<ItemSummary> items = new ArrayList<>(List.of(itemSummary()));
            Response<ArrayList<ItemSummary>> response = Response.ok(ID_A, items);
            assertEquals(response, roundTrip(response));
        }

        @Test
        void successAndFailureSurviveSerialization() throws Exception {
            Response<UserSummary> ok = Response.ok(ID_A, user());
            Response<UserSummary> fail = Response.fail(ID_A, ErrorCode.FORBIDDEN, "Không có quyền");
            assertEquals(ok, roundTrip(ok));
            assertEquals(fail, roundTrip(fail));
        }
    }

    // =====================================================================
    // EventMessage.java
    // =====================================================================
    @Nested
    class EventMessageContract {
        @Test
        void keepsFields() {
            EventMessage event = new EventMessage(EventMessage.EventType.BID_ACCEPTED, ID_D, T0);
            assertEquals(EventMessage.EventType.BID_ACCEPTED, event.eventType());
            assertEquals(ID_D, event.auctionId());
            assertEquals(T0, event.occurredAt());
        }

        @Test
        void rejectsNullFields() {
            assertNpe(() -> new EventMessage(null, ID_D, T0));
            assertNpe(() -> new EventMessage(EventMessage.EventType.BID_ACCEPTED, null, T0));
            assertNpe(() -> new EventMessage(EventMessage.EventType.BID_ACCEPTED, ID_D, null));
        }

        @Test
        void containsEventTypesOfPlanSection34() {
            assertContainsAll(namesOf(EventMessage.EventType.values()), Set.of(
                    "BID_ACCEPTED", "AUCTION_STARTED", "AUCTION_FINISHED", "AUCTION_CANCELED"));
        }

        @Test
        void survivesSerialization() throws Exception {
            EventMessage event = new EventMessage(EventMessage.EventType.AUCTION_FINISHED, ID_D, T1);
            assertEquals(event, roundTrip(event));
        }
    }

    // =====================================================================
    // dto/UserSummary.java
    // =====================================================================
    @Nested
    class UserSummaryContract {
        @Test
        void keepsFieldsAndRejectsNull() {
            UserSummary summary = user();
            assertEquals(ID_A, summary.id());
            assertEquals("alice", summary.username());
            assertEquals(anyRole(), summary.role());
            assertNpe(() -> new UserSummary(null, "alice", anyRole()));
            assertNpe(() -> new UserSummary(ID_A, null, anyRole()));
            assertNpe(() -> new UserSummary(ID_A, "alice", null));
        }

        @Test
        void survivesSerialization() throws Exception {
            assertEquals(user(), roundTrip(user()));
        }
    }

    // =====================================================================
    // dto/ItemSummary.java
    // =====================================================================
    @Nested
    class ItemSummaryContract {
        @Test
        void keepsFieldsAndRejectsNull() {
            ItemSummary summary = itemSummary();
            assertEquals(ID_B, summary.id());
            assertEquals(ID_A, summary.sellerId());
            assertEquals("Laptop", summary.name());
            assertEquals(anyItemType(), summary.type());
            assertNpe(() -> new ItemSummary(null, ID_A, "Laptop", anyItemType()));
            assertNpe(() -> new ItemSummary(ID_B, null, "Laptop", anyItemType()));
            assertNpe(() -> new ItemSummary(ID_B, ID_A, null, anyItemType()));
            assertNpe(() -> new ItemSummary(ID_B, ID_A, "Laptop", null));
        }

        @Test
        void survivesSerialization() throws Exception {
            assertEquals(itemSummary(), roundTrip(itemSummary()));
        }
    }

    // =====================================================================
    // dto/ItemDetail.java
    // =====================================================================
    @Nested
    class ItemDetailContract {
        @Test
        void keepsFieldsAndRejectsNull() {
            ItemDetail detail = itemDetail();
            assertEquals(ID_B, detail.id());
            assertEquals(ID_A, detail.sellerId());
            assertEquals("Laptop", detail.name());
            assertEquals("Máy còn bảo hành", detail.description());
            assertEquals(anyItemType(), detail.type());
            assertNpe(() -> new ItemDetail(null, ID_A, "Laptop", "mô tả", anyItemType()));
            assertNpe(() -> new ItemDetail(ID_B, null, "Laptop", "mô tả", anyItemType()));
            assertNpe(() -> new ItemDetail(ID_B, ID_A, null, "mô tả", anyItemType()));
            assertNpe(() -> new ItemDetail(ID_B, ID_A, "Laptop", null, anyItemType()));
            assertNpe(() -> new ItemDetail(ID_B, ID_A, "Laptop", "mô tả", null));
        }

        @Test
        void survivesSerialization() throws Exception {
            assertEquals(itemDetail(), roundTrip(itemDetail()));
        }
    }

    // =====================================================================
    // dto/BidSummary.java
    // =====================================================================
    @Nested
    class BidSummaryContract {
        @Test
        void keepsFieldsAndRejectsNull() {
            BidSummary bid = bid();
            assertEquals(ID_C, bid.id());
            assertEquals(ID_D, bid.auctionId());
            assertEquals(ID_A, bid.bidderId());
            assertEquals(CURRENT, bid.amount());
            assertEquals(T0, bid.acceptedAt());
            assertNpe(() -> new BidSummary(null, ID_D, ID_A, CURRENT, T0));
            assertNpe(() -> new BidSummary(ID_C, null, ID_A, CURRENT, T0));
            assertNpe(() -> new BidSummary(ID_C, ID_D, null, CURRENT, T0));
            assertNpe(() -> new BidSummary(ID_C, ID_D, ID_A, null, T0));
            assertNpe(() -> new BidSummary(ID_C, ID_D, ID_A, CURRENT, null));
        }

        @Test
        void survivesSerialization() throws Exception {
            assertEquals(bid(), roundTrip(bid()));
        }
    }

    // =====================================================================
    // dto/AuctionSummary.java
    // =====================================================================
    @Nested
    class AuctionSummaryContract {
        @Test
        void keepsFields() {
            AuctionSummary summary = auctionSummary();
            assertEquals(ID_D, summary.id());
            assertEquals(ID_B, summary.itemId());
            assertEquals(ID_A, summary.sellerId());
            assertEquals(CURRENT, summary.currentPrice());
            assertEquals(AuctionStatus.RUNNING, summary.status());
            assertEquals(T0, summary.startTime());
            assertEquals(T1, summary.endTime());
        }

        @Test
        void rejectsNullFields() {
            AuctionStatus s = AuctionStatus.OPEN;
            assertNpe(() -> new AuctionSummary(null, ID_B, ID_A, CURRENT, s, T0, T1));
            assertNpe(() -> new AuctionSummary(ID_D, null, ID_A, CURRENT, s, T0, T1));
            assertNpe(() -> new AuctionSummary(ID_D, ID_B, null, CURRENT, s, T0, T1));
            assertNpe(() -> new AuctionSummary(ID_D, ID_B, ID_A, null, s, T0, T1));
            assertNpe(() -> new AuctionSummary(ID_D, ID_B, ID_A, CURRENT, null, T0, T1));
            assertNpe(() -> new AuctionSummary(ID_D, ID_B, ID_A, CURRENT, s, null, T1));
            assertNpe(() -> new AuctionSummary(ID_D, ID_B, ID_A, CURRENT, s, T0, null));
        }

        @Test
        void survivesSerialization() throws Exception {
            assertEquals(auctionSummary(), roundTrip(auctionSummary()));
        }
    }

    // =====================================================================
    // dto/AuctionDetail.java
    // =====================================================================
    @Nested
    class AuctionDetailContract {
        @Test
        void keepsFieldsAndHistoryOrder() {
            BidSummary first = bid();
            BidSummary second = new BidSummary(UUID.randomUUID(), ID_D, ID_A,
                    new BigDecimal("160000"), T1);
            AuctionDetail detail = auctionDetail(ID_A, List.of(first, second));
            assertEquals(STARTING, detail.startingPrice());
            assertEquals(CURRENT, detail.currentPrice());
            assertEquals(ID_A, detail.winnerId());
            assertEquals(List.of(first, second), detail.history());
        }

        @Test
        void winnerMayBeNullAndHistoryMayBeEmpty() {
            AuctionDetail detail = auctionDetail(null, List.of());
            assertNull(detail.winnerId());
            assertTrue(detail.history().isEmpty());
        }

        @Test
        void rejectsNullRequiredFields() {
            List<BidSummary> h = List.of();
            AuctionStatus s = AuctionStatus.OPEN;
            assertNpe(() -> new AuctionDetail(null, ID_B, ID_A, STARTING, CURRENT, s, T0, T1, null, h));
            assertNpe(() -> new AuctionDetail(ID_D, null, ID_A, STARTING, CURRENT, s, T0, T1, null, h));
            assertNpe(() -> new AuctionDetail(ID_D, ID_B, null, STARTING, CURRENT, s, T0, T1, null, h));
            assertNpe(() -> new AuctionDetail(ID_D, ID_B, ID_A, null, CURRENT, s, T0, T1, null, h));
            assertNpe(() -> new AuctionDetail(ID_D, ID_B, ID_A, STARTING, null, s, T0, T1, null, h));
            assertNpe(() -> new AuctionDetail(ID_D, ID_B, ID_A, STARTING, CURRENT, null, T0, T1, null, h));
            assertNpe(() -> new AuctionDetail(ID_D, ID_B, ID_A, STARTING, CURRENT, s, null, T1, null, h));
            assertNpe(() -> new AuctionDetail(ID_D, ID_B, ID_A, STARTING, CURRENT, s, T0, null, null, h));
            assertNpe(() -> new AuctionDetail(ID_D, ID_B, ID_A, STARTING, CURRENT, s, T0, T1, null, null));
            List<BidSummary> withNull = new ArrayList<>();
            withNull.add(null);
            assertNpe(() -> auctionDetail(null, withNull));
        }

        @Test
        void historyIsDefensivelyCopiedAndUnmodifiable() {
            List<BidSummary> source = new ArrayList<>(List.of(bid()));
            AuctionDetail detail = auctionDetail(ID_A, source);
            source.clear();
            assertEquals(1, detail.history().size());
            assertThrows(UnsupportedOperationException.class, () -> detail.history().add(bid()));
        }

        @Test
        void survivesSerializationAndStaysUnmodifiable() throws Exception {
            AuctionDetail withWinner = auctionDetail(ID_A, List.of(bid()));
            AuctionDetail noWinner = auctionDetail(null, List.of());
            assertEquals(withWinner, roundTrip(withWinner));
            assertEquals(noWinner, roundTrip(noWinner));
            AuctionDetail copy = roundTrip(withWinner);
            assertThrows(UnsupportedOperationException.class, () -> copy.history().add(bid()));
        }
    }

    // =====================================================================
    // Quy tắc chung của mọi DTO/message gửi qua mạng (kế hoạch 3.2, 3.4, tuần 6 TV3)
    // =====================================================================
    @Nested
    class DtoSafetyContract {
        private final List<Class<?>> wireTypes = List.of(
                UserSummary.class, ItemSummary.class, ItemDetail.class,
                AuctionSummary.class, AuctionDetail.class, BidSummary.class,
                EventMessage.class, Response.class, Request.class);

        @Test
        void everyWireTypeIsASerializableRecord() {
            for (Class<?> type : wireTypes) {
                assertTrue(type.isRecord(), type.getSimpleName() + " phải là record bất biến");
                assertTrue(Serializable.class.isAssignableFrom(type),
                        type.getSimpleName() + " phải Serializable");
            }
        }

        /** Request không nằm trong danh sách này vì LoginPayload cần mang password lên server. */
        @Test
        void noServerToClientTypeHasPasswordOrHashComponent() {
            List<Class<?>> serverToClient = wireTypes.stream()
                    .filter(type -> type != Request.class).collect(Collectors.toList());
            for (Class<?> type : serverToClient) {
                for (RecordComponent component : type.getRecordComponents()) {
                    String name = component.getName().toLowerCase(Locale.ROOT);
                    assertFalse(name.contains("password") || name.contains("hash")
                                    || name.contains("secret"),
                            type.getSimpleName() + "." + component.getName());
                }
            }
        }

        @Test
        void noWireTypeExposesAMutableDomainEntity() {
            for (Class<?> type : wireTypes) {
                for (RecordComponent component : type.getRecordComponents()) {
                    Class<?> componentType = component.getType();
                    boolean isDomainClass = componentType.getName().startsWith("vn.uet.auction.model.")
                            && !componentType.isEnum();
                    assertFalse(isDomainClass,
                            type.getSimpleName() + "." + component.getName() + " là entity "
                                    + componentType.getSimpleName());
                }
            }
        }
    }
}