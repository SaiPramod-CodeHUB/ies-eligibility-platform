package com.sgopireddy.ies.api;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(
        properties = "ies.retrieval.seed=true")
@DisplayName("Access control")
class SecurityRulesTest {

    @Autowired MockMvc mvc;

    @Test
    @WithAnonymousUser
    @DisplayName("the API requires authentication")
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/households/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("health and API docs stay open for probes and discovery")
    void probesAreOpen() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASEWORKER")
    @DisplayName("a caseworker may use the assist endpoints")
    void caseworkerMayAssist() throws Exception {
        mvc.perform(get("/api/v1/assist/policy").param("q", "resource limit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASEWORKER")
    @DisplayName("a caseworker may NOT issue a notice — that starts the appeal clock")
    void caseworkerMayNotIssueNotice() throws Exception {
        mvc.perform(post("/api/v1/determinations/1/notice"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CASEWORKER")
    @DisplayName("a caseworker may NOT index policy material")
    void caseworkerMayNotIngest() throws Exception {
        mvc.perform(post("/api/v1/assist/ingest")
                        .contentType("application/json")
                        .content("{\"corpus\":\"POLICY_MANUAL\",\"source\":\"x\",\"text\":\"y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("a supervisor reaches the notice endpoint (404 here, not 403)")
    void supervisorMayIssueNotice() throws Exception {
        mvc.perform(post("/api/v1/determinations/999999/notice"))
                .andExpect(status().isNotFound());
    }
}
