package de.gesellix.docker.client.util

// see https://tools.ietf.org/html/rfc6455#section-11.7
enum WebsocketStatusCode {
    NORMAL_CLOSURE(1000),
    GOING_AWAY(1001),
    PROTOCOL_ERROR(1002),
    UNSUPPORTED_DATA(1003),
    NO_STATUS_RCVD(1005),
    ABNORMAL_CLOSURE(1006),
    INVALID_FRAME_PAYLOAD_DATA(1007),
    POLICY_VIOLATION(1008),
    MESSAGE_TOO_BIG(1009),
    MANDATORY_EXT(1010),
    INTERNAL_SERVER_ERROR(1011),
    TLS_HANDSHAKE(1015)

    private int code

    private WebsocketStatusCode(int code) {
        this.code = code
    }

    public int getCode() {
        return code
    }
}
