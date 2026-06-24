package dev.adastratech.mercuryshop.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita execução assíncrona (@Async) — usado pelo envio de e-mails. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
