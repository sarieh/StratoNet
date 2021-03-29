package utils;

public enum MsgPhase {
    INIT(0),
    QUERY(1);

    public final int value;

    MsgPhase(int i) {
        value = i;
    }
}