package dev.adastratech.mercuryshop.shared.audit;

import dev.adastratech.mercuryshop.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A trilha de auditoria é persistida (append-only) quando ocorre um evento de segurança. */
class AuditPersistenceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registerPersistsAuditEvent() throws Exception {
        long before = count();
        String email = "u" + UUID.randomUUID().toString().replace("-", "") + "@example.com";

        mockMvc.perform(post("/v1/auth/register")
                        .with(request -> {
                            request.setRemoteAddr(UUID.randomUUID().toString());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"Str0ng!Passw0rd\", \"fullName\": \"Fulano\"}"
                                .formatted(email)))
                .andExpect(status().isCreated());

        assertThat(count()).isGreaterThan(before);
        Long registered = jdbc.queryForObject(
                "select count(*) from audit_event where event = 'USER_REGISTERED'", Long.class);
        assertThat(registered).isGreaterThanOrEqualTo(1);
    }

    private long count() {
        Long total = jdbc.queryForObject("select count(*) from audit_event", Long.class);
        return total == null ? 0 : total;
    }
}
