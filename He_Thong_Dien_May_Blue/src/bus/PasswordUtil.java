package bus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Dùng SHA-256 để khớp dữ liệu demo trong 04_seed_data.sql.
 * Với hệ thống thực tế nên thay bằng BCrypt/PBKDF2/Argon2.
 */
public final class PasswordUtil {
    private PasswordUtil() {}

    public static String sha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    rawPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    public static boolean matches(String rawPassword, String storedHash) {
        return storedHash != null
                && sha256(rawPassword).equalsIgnoreCase(storedHash);
    }

    public static boolean isStrongEnough(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        return hasLetter && hasDigit;
    }
}
