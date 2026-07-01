package dev.adastratech.mercuryshop.user.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import dev.adastratech.mercuryshop.support.RecordingMailDelivery;
import dev.adastratech.mercuryshop.user.domain.Totp;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "Str0ng!Passw0rd";

    @Autowired
    private RecordingMailDelivery mail;

    @Test
    void registerHidesSecretsAndStartsPending() throws Exception {
        String email = newEmail();
        mockMvc.perform(post("/v1/auth/register")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void verifyThenLoginThenAccessMe() throws Exception {
        String email = newEmail();
        registerAndVerify(email);

        MvcResult login = login(email, PASSWORD);
        String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        assertThat(login.getResponse().getCookie("refresh_token")).isNotNull();

        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginBeforeVerificationIsForbidden() throws Exception {
        String email = newEmail();
        mockMvc.perform(post("/v1/auth/register")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/auth/login")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void loginWithWrongPasswordIsGenericUnauthorized() throws Exception {
        String email = newEmail();
        registerAndVerify(email);

        mockMvc.perform(post("/v1/auth/login")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "Wr0ng!Passw0rd")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("E-mail ou senha inválidos"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminListForbiddenForCustomerButOkForAdmin() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        String customerToken = JsonPath.read(login(email, PASSWORD).getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/v1/users").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/users").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void refreshRotatesAndInvalidatesOldToken() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        Cookie refresh = login(email, PASSWORD).getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        mockMvc.perform(post("/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenReuseRevokesTheWholeFamily() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        Cookie tokenA = login(email, PASSWORD).getResponse().getCookie("refresh_token");

        // Rotação legítima A → B.
        Cookie tokenB = mockMvc.perform(post("/v1/auth/refresh").cookie(tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("refresh_token");

        // Reapresentar A (já rotacionado) é reuso → 401 e dispara a revogação da família.
        mockMvc.perform(post("/v1/auth/refresh").cookie(tokenA))
                .andExpect(status().isUnauthorized());

        // B era legítimo, mas foi revogado junto com a família → também 401.
        mockMvc.perform(post("/v1/auth/refresh").cookie(tokenB))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitsLoginAttempts() throws Exception {
        MvcResult last = null;
        for (int i = 0; i < 16; i++) {
            last = mockMvc.perform(post("/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("203.0.113.7");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("ratelimit@example.com", PASSWORD)))
                    .andReturn();
        }
        assertThat(last).isNotNull();
        assertThat(last.getResponse().getStatus()).isEqualTo(429);
    }

    @Test
    void spoofedXForwardedForCannotBypassRateLimit() throws Exception {
        // Mesmo cliente (remoteAddr fixo) forjando um "IP novo" via X-Forwarded-For a cada request:
        // o header é ignorado (o bucket é pelo endereço resolvido), então o limite ainda dispara.
        MvcResult last = null;
        for (int i = 0; i < 16; i++) {
            last = mockMvc.perform(post("/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("203.0.113.9");
                                return request;
                            })
                            .header("X-Forwarded-For", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("spoof@example.com", PASSWORD)))
                    .andReturn();
        }
        assertThat(last).isNotNull();
        assertThat(last.getResponse().getStatus()).isEqualTo(429);
    }

    @Test
    void changeEmailRequiresConfirmationOnTheNewAddress() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        String accessToken = JsonPath.read(login(email, PASSWORD).getResponse().getContentAsString(), "$.accessToken");
        String newAddress = newEmail();

        // Solicita a troca (autenticado, com a senha atual).
        mockMvc.perform(post("/v1/users/me/change-email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\": \"%s\", \"currentPassword\": \"%s\"}".formatted(newAddress, PASSWORD)))
                .andExpect(status().isOk());

        // A confirmação é enviada ao NOVO endereço; só então a troca se efetiva.
        await().atMost(Duration.ofSeconds(10)).until(() -> mail.emailChangeToken(newAddress) != null);
        mockMvc.perform(get("/v1/auth/confirm-email-change").param("token", mail.emailChangeToken(newAddress)))
                .andExpect(status().isOk());

        // Agora o login funciona com o novo e-mail e /me reflete a troca.
        String newAccess = JsonPath.read(login(newAddress, PASSWORD).getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newAddress));
    }

    @Test
    void changeEmailWithWrongPasswordIsUnauthorized() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        String accessToken = JsonPath.read(login(email, PASSWORD).getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/v1/users/me/change-email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\": \"%s\", \"currentPassword\": \"Wr0ng!Passw0rd\"}".formatted(newEmail())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mfaEnrollmentMakesLoginRequireTotp() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        String accessToken = JsonPath.read(login(email, PASSWORD).getResponse().getContentAsString(), "$.accessToken");
        String secret = enableMfa(accessToken);

        // Com o MFA ativo, o login não emite tokens — devolve um desafio.
        MvcResult challenge = mockMvc.perform(post("/v1/auth/login")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();
        String mfaToken = JsonPath.read(challenge.getResponse().getContentAsString(), "$.mfaToken");

        // Confirma o código TOTP → tokens emitidos.
        mockMvc.perform(post("/v1/auth/login/mfa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\": \"%s\", \"code\": \"%s\"}"
                                .formatted(mfaToken, Totp.currentCode(secret))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void loginMfaWithWrongCodeIsUnauthorized() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        String accessToken = JsonPath.read(login(email, PASSWORD).getResponse().getContentAsString(), "$.accessToken");
        enableMfa(accessToken);

        String mfaToken = JsonPath.read(mockMvc.perform(post("/v1/auth/login")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andReturn().getResponse().getContentAsString(), "$.mfaToken");

        mockMvc.perform(post("/v1/auth/login/mfa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\": \"%s\", \"code\": \"000000\"}".formatted(mfaToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportReturnsOwnDataWithoutSecrets() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        String token = JsonPath.read(login(email, PASSWORD).getResponse().getContentAsString(), "$.accessToken");

        String body = mockMvc.perform(get("/v1/users/me/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.email").value(email))
                .andExpect(jsonPath("$.orders").isArray())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("passwordHash").doesNotContain("mfaSecret");
    }

    @Test
    void deleteAccountAnonymizesAndBlocksLogin() throws Exception {
        String email = newEmail();
        registerAndVerify(email);
        String token = JsonPath.read(login(email, PASSWORD).getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(delete("/v1/users/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isOk());

        // E-mail anonimizado: as credenciais antigas não autenticam mais.
        mockMvc.perform(post("/v1/auth/login")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    /** Simula um cliente distinto por requisição (remoteAddr único) — isola os buckets de rate limit. */
    private static RequestPostProcessor freshClient() {
        String clientId = UUID.randomUUID().toString();
        return request -> {
            request.setRemoteAddr(clientId);
            return request;
        };
    }

    /** Faz o setup + ativação do MFA e devolve o segredo TOTP. */
    private String enableMfa(String accessToken) throws Exception {
        String secret = JsonPath.read(mockMvc.perform(post("/v1/users/me/mfa/setup")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.secret");
        mockMvc.perform(post("/v1/users/me/mfa/enable")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"%s\"}".formatted(Totp.currentCode(secret))))
                .andExpect(status().isOk());
        return secret;
    }

    private void registerAndVerify(String email) throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());
        // e-mail de verificação chega de forma assíncrona (fila → worker → entrega capturada).
        await().atMost(Duration.ofSeconds(10)).until(() -> mail.verificationToken(email) != null);
        mockMvc.perform(get("/v1/auth/verify").param("token", mail.verificationToken(email)))
                .andExpect(status().isOk());
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/v1/auth/login")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String registerBody(String email) {
        return """
                {"email": "%s", "password": "%s", "fullName": "Fulano"}
                """.formatted(email, PASSWORD);
    }

    private static String loginBody(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    private static String newEmail() {
        return "u" + UUID.randomUUID().toString().replace("-", "") + "@example.com";
    }
}
