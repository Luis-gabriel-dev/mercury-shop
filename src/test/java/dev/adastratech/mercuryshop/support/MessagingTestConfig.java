package dev.adastratech.mercuryshop.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Substitui a entrega de e-mail por uma versão que captura os envios (compartilhada por todos os testes). */
@TestConfiguration
public class MessagingTestConfig {

    @Bean
    @Primary
    public RecordingMailDelivery recordingMailDelivery() {
        return new RecordingMailDelivery();
    }
}
