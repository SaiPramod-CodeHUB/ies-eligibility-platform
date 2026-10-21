package com.sgopireddy.ies.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "CASEWORKER")
@DisplayName("Eligibility API end to end")
class EligibilityApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    /**
     * The jurisdiction is part of the household record, not of the deployment,
     * so one running instance serves several of them.
     */
    private static String household(String jurisdiction) {
        return """
            {
              "jurisdictionCode": "%s",
              "locality": "Example County",
              "countableResources": 500.00,
              "heatingCostResponsible": true,
              "members": [
                {"firstName":"Ada","lastName":"Doe","dateOfBirth":"1990-05-01",
                 "disabled":false,"receivesSsi":false},
                {"firstName":"Kid","lastName":"Doe","dateOfBirth":"2020-03-01",
                 "disabled":false,"receivesSsi":false}
              ],
              "income": [
                {"type":"EARNED","monthlyGross":1500.00,"description":"wages"}
              ]
            }
            """.formatted(jurisdiction);
    }

    private long createHousehold(String jurisdiction) throws Exception {
        String created = mvc.perform(post("/api/v1/households")
                        .contentType(MediaType.APPLICATION_JSON).content(household(jurisdiction)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jurisdictionCode").value(jurisdiction))
                .andExpect(jsonPath("$.size").value(2))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(created).get("id").asLong();
    }

    @Test
    @DisplayName("create household, determine every offered program, read back the audit trail")
    void fullFlow() throws Exception {
        long id = createHousehold("NY");

        mvc.perform(post("/api/v1/households/{id}/determine", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].programType").exists())
                .andExpect(jsonPath("$[*].jurisdictionCode").exists())
                .andExpect(jsonPath("$[*].firedRules[*].policyReference").exists());

        mvc.perform(get("/api/v1/households/{id}/determinations", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("single-program determination returns the local program name and the rules that fired")
    void singleProgram() throws Exception {
        long id = createHousehold("NY");

        mvc.perform(post("/api/v1/households/{id}/determine/{programType}", id, "FOOD_ASSISTANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programType").value("FOOD_ASSISTANCE"))
                .andExpect(jsonPath("$.jurisdictionCode").value("NY"))
                // Configuration, not code, decides what the program is called here.
                .andExpect(jsonPath("$.programName")
                        .value("Supplemental Nutrition Assistance Program (SNAP)"))
                .andExpect(jsonPath("$.outcome").value("ELIGIBLE"))
                .andExpect(jsonPath("$.firedRules.length()").value(3))
                .andExpect(jsonPath("$.ruleSetVersion").exists());
    }

    @Test
    @DisplayName("a second jurisdiction is served by the same instance, with its own "
               + "program names and its own set of programs")
    void secondJurisdictionIsServedByTheSameInstance() throws Exception {
        long id = createHousehold("TX");

        mvc.perform(post("/api/v1/households/{id}/determine/{programType}", id, "FOOD_ASSISTANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programName").value("SNAP Food Benefits"));

        // Texas is configured without an energy program, so determineAll runs two.
        mvc.perform(post("/api/v1/households/{id}/determine", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("a program the jurisdiction does not administer returns 409, "
               + "rather than quietly falling back to federal rules")
    void programNotOfferedReturnsConflict() throws Exception {
        long id = createHousehold("TX");

        mvc.perform(post("/api/v1/households/{id}/determine/{programType}", id, "ENERGY_ASSISTANCE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("an unconfigured jurisdiction is refused at determination time")
    void unknownJurisdictionIsRefused() throws Exception {
        long id = createHousehold("ZZ");

        mvc.perform(post("/api/v1/households/{id}/determine", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("validation failure returns 400 with field detail")
    void validationError() throws Exception {
        mvc.perform(post("/api/v1/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jurisdictionCode\":\"\",\"countableResources\":0,\"members\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("unknown household returns 404")
    void notFound() throws Exception {
        mvc.perform(get("/api/v1/households/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
