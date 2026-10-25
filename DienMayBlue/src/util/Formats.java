package tienichUtil;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formats {
    private Formats() {}

    private static final NumberFormat VND =
            NumberFormat.getNumberInstance(new Locale("vi","VN"));

    public static String money(Object value) {
        if (value == null) return "0đ";
        BigDecimal n;
        if (value instanceof BigDecimal) n = (BigDecimal)value;
        else if (value instanceof Number) n = BigDecimal.valueOf(((Number)value).doubleValue());
        else n = new BigDecimal(String.valueOf(value));
        return VND.format(n) + "đ";
    }

    public static String date(Object value) {
        if (value == null) return "";
        if (value instanceof java.sql.Date)
            return ((java.sql.Date)value).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (value instanceof java.sql.Timestamp)
            return ((java.sql.Timestamp)value).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return String.valueOf(value);
    }

    public static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
