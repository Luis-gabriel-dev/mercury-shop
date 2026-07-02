package dev.adastratech.mercuryshop;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith (Fase 11): verifica as fronteiras dos módulos em runtime — features só acessam a
 * API (base + named interfaces) umas das outras, nunca os internos (application/adapter); o shared é
 * um leaf OPEN; sem ciclos. Também gera a documentação (PlantUML + module canvas) em
 * target/spring-modulith-docs. Análise estática — não sobe contexto Spring nem containers.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(MercuryShopApplication.class);

    @Test
    void modulesRespectBoundaries() {
        modules.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
