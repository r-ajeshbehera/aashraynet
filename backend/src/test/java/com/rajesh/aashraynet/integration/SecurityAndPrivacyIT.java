package com.rajesh.aashraynet.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityAndPrivacyIT extends IntegrationTestBase {

    @Test void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/shelters")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test void wrongPasswordGives401WithGenericMessage() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + PURI_ADMIN + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test void wrongRoleGets403() throws Exception {
        mvc.perform(post("/api/events").header("Authorization", token(PURI_VOLUNTEER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"startDate\":\"2026-11-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void districtAdminCannotSeeAnotherDistrictsShelter() throws Exception {
        // shelter 5 = Mahakalapada (Kendrapara)
        mvc.perform(get("/api/shelters/5").header("Authorization", token(PURI_ADMIN)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/shelters").header("Authorization", token(PURI_ADMIN)))
                .andExpect(jsonPath("$[*].districtName", everyItem(is("Puri"))));
    }

    @Test void districtAdminCannotOpenAnotherDistrictsDashboard() throws Exception {
        mvc.perform(get("/api/dashboard/district/2").header("Authorization", token(PURI_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test void publicFinderReturnsOnlyShelterAndDistrict() throws Exception {
        mvc.perform(get("/api/public/find").param("name", "  laxmi  NAYAK").param("village", "satapada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].shelterName").value("Satapada MCS"))
                .andExpect(jsonPath("$.matches[0].district").value("Puri"))
                .andExpect(jsonPath("$.matches[0].age").doesNotExist())
                .andExpect(jsonPath("$.matches[0].specialNeed").doesNotExist())
                .andExpect(jsonPath("$.matches[0].id").doesNotExist())
                .andExpect(jsonPath("$.matches[0].*", hasSize(2)));
    }

    @Test void publicFinderGivesSameShapeWhenNothingFound() throws Exception {
        mvc.perform(get("/api/public/find").param("name", "Nobody Here").param("village", "Nowhere"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches", hasSize(0)))
                .andExpect(jsonPath("$.notice", containsString("Demonstration")));
    }

    @Test void volunteerDoesNotSeeSpecialNeeds() throws Exception {
        mvc.perform(get("/api/shelters/1/evacuees").header("Authorization", token(PURI_VOLUNTEER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].specialNeed").doesNotExist());
        mvc.perform(get("/api/shelters/1/evacuees").header("Authorization", token(SATAPADA_INCHARGE)))
                .andExpect(jsonPath("$.content[0].specialNeed").exists());
    }

    @Test void fullShelterReturnsBusinessErrorWithAlternatives() throws Exception {
        // shelter 5 (Mahakalapada) is FULL in demo data
        mvc.perform(post("/api/shelters/5/evacuees").header("Authorization", token(KENDRAPARA_VOLUNTEER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"fullName\":\"Test Person\",\"age\":40,\"gender\":\"MALE\",\"village\":\"Mahakalapada\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SHELTER_FULL"))
                .andExpect(jsonPath("$.details.alternatives[0].name").value("Rajnagar MCS"));
    }

    @Test void invalidSupplyRequestReturns400() throws Exception {
        mvc.perform(post("/api/shelters/1/supply-requests").header("Authorization", token(SATAPADA_INCHARGE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"lines\":[{\"itemId\":1,\"quantity\":-5}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test void logoutRevokesToken() throws Exception {
        String t = token(PURI_ADMIN);
        mvc.perform(get("/api/auth/me").header("Authorization", t)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization", t)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", t)).andExpect(status().isUnauthorized());
    }
}
