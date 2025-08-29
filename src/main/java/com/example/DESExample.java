package com.example;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Class tiện ích để mã hóa/giải mã DES
 */
public class DESExample {

    private static SecretKey buildDESKey(String key8) throws Exception {
        if (key8.length() != 8) {
            throw new IllegalArgumentException("Khóa DES phải đúng 8 ký tự!");
        }
        DESKeySpec spec = new DESKeySpec(key8.getBytes(StandardCharsets.UTF_8));
        SecretKeyFactory f = SecretKeyFactory.getInstance("DES");
        return f.generateSecret(spec);
    }

    /** Mã hóa DES + Base64 */
    public static String encryptDESBase64(String data, String key8) throws Exception {
        SecretKey k = buildDESKey(key8);
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, k);
        byte[] enc = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(enc);
    }

    /** Giải mã DES từ Base64 */
    public static String decryptDESBase64(String base64, String key8) throws Exception {
        SecretKey k = buildDESKey(key8);
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, k);
        byte[] dec = cipher.doFinal(Base64.getDecoder().decode(base64));
        return new String(dec, StandardCharsets.UTF_8);
    }
}