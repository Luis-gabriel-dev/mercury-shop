package dev.adastratech.mercuryshop.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Fornece o par de chaves RSA usado para assinar/validar o JWT.
 * Se as chaves (PEM) vierem das propriedades/env, usa-as; caso contrário (dev/test),
 * gera um par EFÊMERO no boot — nunca usar par efêmero em produção.
 */
@Component
public class JwtKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyProvider.class);

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public JwtKeyProvider(SecurityProperties properties) {
        SecurityProperties.Jwt jwt = properties.jwt();
        if (hasText(jwt.publicKey()) && hasText(jwt.privateKey())) {
            this.publicKey = parsePublicKey(jwt.publicKey());
            this.privateKey = parsePrivateKey(jwt.privateKey());
            log.info("JWT: usando par de chaves RSA configurado.");
        } else {
            KeyPair keyPair = generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            log.warn("JWT: nenhuma chave RSA configurada — par EFÊMERO gerado (apenas dev/test).");
        }
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar par RSA", e);
        }
    }

    private static RSAPublicKey parsePublicKey(String pem) {
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decode(pem)));
        } catch (Exception e) {
            throw new IllegalStateException("Chave pública RSA inválida", e);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String pem) {
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decode(pem)));
        } catch (Exception e) {
            throw new IllegalStateException("Chave privada RSA inválida", e);
        }
    }

    private static byte[] decode(String pem) {
        String base64 = pem
                .replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
