package dev.adastratech.mercuryshop.user.domain;

/** Porta para geração de tokens opacos aleatórios (CSPRNG), URL-safe. */
public interface TokenGenerator {

    String generate();
}
