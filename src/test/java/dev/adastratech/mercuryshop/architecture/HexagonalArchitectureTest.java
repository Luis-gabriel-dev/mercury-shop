package dev.adastratech.mercuryshop.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
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

    // O domínio pode carregar as anotações estruturais do Spring Modulith (@NamedInterface etc.) nos
    // package-info — são metadados de fronteira em tempo de build, não acoplamento ao Spring de runtime.
    @ArchTest
    static final ArchRule dominio_nao_depende_de_spring =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat(resideInAPackage("org.springframework..")
                            .and(not(resideInAPackage("org.springframework.modulith.."))))
                    .as("o domínio não deve depender de Spring (exceto anotações do Spring Modulith)");

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
