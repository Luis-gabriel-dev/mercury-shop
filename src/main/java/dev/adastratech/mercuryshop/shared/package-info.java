/**
 * Módulo transversal (shared kernel) — OPEN: todos os subpacotes (exception, messaging, security,
 * idempotency, audit, web, config, observability) são API acessível pelas features. Não depende de
 * nenhuma feature (é um leaf), o que evita ciclos entre módulos.
 */
@ApplicationModule(type = Type.OPEN)
package dev.adastratech.mercuryshop.shared;

import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.ApplicationModule.Type;
