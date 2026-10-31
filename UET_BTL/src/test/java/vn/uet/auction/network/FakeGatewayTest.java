package vn.uet.auction.network;

import org.junit.jupiter.api.Test;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.protocol.ErrorCode;
import vn.uet.auction.protocol.Request;
import vn.uet.auction.protocol.Response;
import vn.uet.auction.protocol.Command;
import vn.uet.auction.protocol.dto.UserSummary;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FakeGatewayTest {

    // Giả lập server nhận request và trả response.
    private Response<UserSummary> fakeLogin(Request request) {
        if (request.command() != Command.LOGIN
                || !(request.payload()
                instanceof Request.LoginPayload payload)) {

            return Response.fail(
                    request.requestId(),
                    ErrorCode.INVALID_REQUEST,
                    "Cần gửi LOGIN cùng LoginPayload"
            );
        }

        // Trả user mẫu, chưa kiểm tra mật khẩu thật.
        UserSummary user = new UserSummary(
                UUID.randomUUID(),
                payload.username(),
                Role.BIDDER
        );

        return Response.ok(request.requestId(), user);
    }

    @Test
    void loginRequestReceivesMatchingResponse() {
        // Tạo yêu cầu đăng nhập.
        Request request = new Request(
                UUID.randomUUID(),
                Command.LOGIN,
                new Request.LoginPayload("thinh_demo", "demo_password")
        );

        // Gửi đến server giả.
        Response<UserSummary> response = fakeLogin(request);

        // Kiểm tra phản hồi.
        assertTrue(response.success());
        assertEquals(request.requestId(), response.requestId());
        assertEquals("thinh_demo", response.data().username());
        assertEquals(Role.BIDDER, response.data().role());
        assertNull(response.errorCode());
    }
}
