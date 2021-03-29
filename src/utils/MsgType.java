package utils;

public enum MsgType {
    AUTH_REQUEST(0),
    AUTH_CHALLENGE(1),
    AUTH_FAIL(2),
    AUTH_SUCCESS(3),
    QUERY_EXIT(4),
    QUERY_IMAGE(5),
    QUERY_ASK(6),
    QUERY_INFO(7),
    QUERY_SOL(8);

    public final int value;

    MsgType(int i) {
        this.value = i;
    }
}


