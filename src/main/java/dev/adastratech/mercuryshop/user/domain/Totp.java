package dev.adastratech.mercuryshop.user.domain;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * TOTP (RFC 6238) puro: geração de segredo (Base32), verificação de código de 6 dígitos (passo de
 * 30s, janela de ±1 passo para tolerar relógio) e URI {@code otpauth://} para o QR Code.
 * Sem dependências externas — mantém o domínio puro (apenas JDK).
 */
public final class Totp {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20; // 160 bits (recomendado pela RFC 4226)
    private static final int DIGITS = 6;
    private static final long STEP_SECONDS = 30;
    private static final int WINDOW = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Totp() {
    }

    /** Gera um segredo aleatório em Base32 (para guardar no usuário e exibir no QR). */
    public static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** Verifica um código de 6 dígitos contra o segredo, tolerando ±1 passo de tempo. */
    public static boolean verify(String secret, String code) {
        if (secret == null || code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        long counter = Instant.now().getEpochSecond() / STEP_SECONDS;
        for (int offset = -WINDOW; offset <= WINDOW; offset++) {
            if (code.equals(generate(secret, counter + offset))) {
                return true;
            }
        }
        return false;
    }

    /** Código TOTP válido neste instante — útil para clientes de teste. */
    public static String currentCode(String secret) {
        return generate(secret, Instant.now().getEpochSecond() / STEP_SECONDS);
    }

    /** URI otpauth:// para o app autenticador (QR Code). */
    public static String otpauthUri(String issuer, String account, String secret) {
        String label = encode(issuer) + ":" + encode(account);
        return "otpauth://totp/" + label + "?secret=" + secret
                + "&issuer=" + encode(issuer) + "&digits=" + DIGITS + "&period=" + STEP_SECONDS;
    }

    static String generate(String secret, long counter) {
        byte[] data = new byte[8];
        long value = counter;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xff);
            value >>= 8;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao gerar TOTP", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                sb.append(BASE32.charAt((buffer >> bits) & 0x1f));
            }
        }
        if (bits > 0) {
            sb.append(BASE32.charAt((buffer << (5 - bits)) & 0x1f));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String secret) {
        String clean = secret.trim().toUpperCase().replace("=", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32.indexOf(clean.charAt(i));
            if (val < 0) {
                throw new IllegalArgumentException("Segredo Base32 inválido");
            }
            buffer = (buffer << 5) | val;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xff);
            }
        }
        return out.toByteArray();
    }
}
