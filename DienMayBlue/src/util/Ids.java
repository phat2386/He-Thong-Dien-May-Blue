package tienichUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public final class Ids {
    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private Ids() {}

    public static String next(String prefix) {
        return prefix
             + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
             + String.format("%03d", SEQ.getAndIncrement() % 1000);
    }
}
