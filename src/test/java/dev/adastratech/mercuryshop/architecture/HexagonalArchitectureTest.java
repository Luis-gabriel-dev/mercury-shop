package dev.adastratech.mercuryshop.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Testes de arquitetura (ArchUnit) — travam as fronteiras hexagonais do projeto:
 * <ul>
 *   <li>o <b>domínio</b> permanece puro: não depende de Spring nem de JPA;</li>
 *   <li>nem o <b>domínio</b> nem a <b>aplicação</b> dependem dos <b>adapters</b> (a dependência só
 *       aponta de fora para dentro — adapter → application → domain).</li>
 * </ul>
 * Se alguém cruzar essas fronteiras ao adicionar código, o build falha. Introduzido na Fase 6 como
 * base barata que protege a arquitetura conforme o projeto cresce.
 */
@AnalyzeClasses(
        packages = "dev.adastratech.mercuryshop",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule dominio_nao_depende_de_spring =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .as("o domínio não deve depender de Spring");

    @ArchTest
    static final ArchRule dominio_nao_depende_de_jpa =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..")
                    .as("o domínio não deve depender de JPA");

    @ArchTest
    static final ArchRule dominio_nao_depende_de_adapter =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .as("o domínio não deve depender de adapters");

    @ArchTest
    static final ArchRule aplicacao_nao_depende_de_adapter =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .as("a aplicação não deve depender de adapters");
}
