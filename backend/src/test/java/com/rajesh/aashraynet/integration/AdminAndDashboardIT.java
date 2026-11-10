package com.rajesh.aashraynet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** User management limits, events, shelter administration, stock rules, rejection, public pages and dashboards. */
class AdminAndDashboardIT extends IntegrationTestBase {

    private JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String token, String body, int expected) throws Exception {
        if (token != null) b.header("Authorization", token);
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(body);
        String s = mvc.perform(b).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return s.isBlank() ? null : read(s);
    }

    @Test
    void userManagementRespectsDistrictAndRoleLimits() throws Exception {
        String puri = token(PURI_ADMIN);
        JsonNode v = call(post("/api/users"), puri, "{\"fullName\":\"New Vol\",\"email\":\"newvol@puri.test\",\"password\":\"Password123\",\"role\":\"VOLUNTEER\"}", 201);
        assertThat(v.get("districtName").asText()).isEqualTo("Puri");
        call(post("/api/users"), puri, "{\"fullName\":\"X\",\"email\":\"x1@t\",\"password\":\"Password123\",\"role\":\"DISTRICT_ADMIN\"}", 403);
        call(post("/api/users"), puri, "{\"fullName\":\"X\",\"email\":\"x2@t\",\"password\":\"Password123\",\"role\":\"VOLUNTEER\",\"districtId\":2}", 404);
        call(post("/api/users"), puri, "{\"fullName\":\"X\",\"email\":\"newvol@puri.test\",\"password\":\"Password123\",\"role\":\"VOLUNTEER\"}", 422);
        call(post("/api/users"), token(STATE), "{\"fullName\":\"X\",\"email\":\"x3@t\",\"password\":\"Password123\",\"role\":\"STATE_ADMIN\",\"districtId\":1}", 422);
        call(post("/api/users"), token(STATE), "{\"fullName\":\"K Vol\",\"email\":\"kvol@t.test\",\"password\":\"Password123\",\"role\":\"VOLUNTEER\",\"districtId\":2}", 201);
        mvc.perform(get("/api/users").header("Authorization", puri)).andExpect(jsonPath("$.content[*].districtName", everyItem(is("Puri"))));
        call(patch("/api/users/" + v.get("id").asLong()), puri, "{\"active\":false,\"fullName\":\"Renamed\"}", 200);
        call(patch("/api/users/3"), puri, "{\"active\":false}", 404);       // Kendrapara admin is out of scope
        call(patch("/api/users/2"), puri, "{\"active\":false}", 403);       // cannot manage admins (incl. self)
        call(patch("/api/users/me/password"), puri, "{\"currentPassword\":\"wrong\",\"newPassword\":\"Password123\"}", 422);
    }

    @Test
    void eventsSheltersStockAndRejection() throws Exception {
        String state = token(STATE);
        String puri = token(PURI_ADMIN);
        JsonNode ev = call(post("/api/events"), state, "{\"name\":\"Admin Test Cyclone (DEMO)\",\"startDate\":\"2026-11-01\"}", 201);
        call(post("/api/events"), state, "{\"name\":\"Admin Test Cyclone (DEMO)\",\"startDate\":\"2026-11-01\"}", 422);
        call(put("/api/events/" + ev.get("id").asLong()), state, "{\"name\":\"Admin Test Cyclone (DEMO)\",\"startDate\":\"2026-11-01\",\"endDate\":\"2026-11-05\",\"active\":false}", 200);
        call(put("/api/events/" + ev.get("id").asLong()), state, "{\"name\":\"Admin Test Cyclone (DEMO)\",\"startDate\":\"2026-11-01\",\"endDate\":\"2026-10-01\"}", 422);
        mvc.perform(get("/api/events").param("active", "true").header("Authorization", puri)).andExpect(jsonPath("$[*].name", not(hasItem("Admin Test Cyclone (DEMO)"))));

        call(post("/api/shelters"), puri, "{\"districtId\":2,\"name\":\"X\",\"village\":\"X\",\"latitude\":20.4,\"longitude\":86.6,\"capacity\":10,\"hasMedical\":false,\"hasRamp\":false,\"hasGenerator\":false}", 404);
        JsonNode s = call(put("/api/shelters/3"), puri, "{\"districtId\":1,\"name\":\"Krushnaprasad MCS\",\"village\":\"Krushnaprasad\",\"latitude\":19.702,\"longitude\":85.378,\"capacity\":55,\"hasMedical\":true,\"hasRamp\":true,\"hasGenerator\":false}", 200);
        assertThat(s.get("capacity").asInt()).isEqualTo(55);
        call(put("/api/shelters/3"), puri, "{\"districtId\":1,\"name\":\"Krushnaprasad MCS\",\"village\":\"Krushnaprasad\",\"latitude\":19.702,\"longitude\":85.378,\"capacity\":1,\"hasMedical\":true,\"hasRamp\":true,\"hasGenerator\":false}", 400);
        call(put("/api/shelters/3"), puri, "{\"districtId\":2,\"name\":\"Krushnaprasad MCS\",\"village\":\"Krushnaprasad\",\"latitude\":19.702,\"longitude\":85.378,\"capacity\":55,\"hasMedical\":true,\"hasRamp\":true,\"hasGenerator\":false}", 422);
        call(patch("/api/shelters/1/facilities"), token(SATAPADA_INCHARGE), "{\"hasMedical\":true,\"hasRamp\":true,\"hasGenerator\":false}", 200);
        call(patch("/api/shelters/1/incharge"), puri, "{\"userId\":7}", 422);     // a volunteer cannot be in-charge
        mvc.perform(get("/api/shelters").param("status", "GREEN").header("Authorization", puri)).andExpect(jsonPath("$[*].status", everyItem(is("GREEN"))));
        mvc.perform(get("/api/shelters/1").header("Authorization", token(SATAPADA_INCHARGE))).andExpect(status().isOk());
        mvc.perform(get("/api/shelters/2").header("Authorization", token(SATAPADA_INCHARGE))).andExpect(status().isNotFound());

        String inc = token(SATAPADA_INCHARGE);
        call(post("/api/shelters/1/stock/adjustments"), inc, "{\"itemId\":3,\"type\":\"CONSUMPTION\",\"quantity\":1000}", 422);   // would go negative
        call(post("/api/shelters/1/stock/adjustments"), inc, "{\"itemId\":3,\"type\":\"RECEIPT\",\"quantity\":5}", 422);          // receipts only via requests
        JsonNode mv = call(post("/api/shelters/1/stock/adjustments"), inc, "{\"itemId\":3,\"type\":\"ADJUSTMENT_IN\",\"quantity\":4,\"reason\":\"donation\"}", 201);
        assertThat(mv.get("balanceAfter").decimalValue()).isEqualByComparingTo("14");
        mvc.perform(get("/api/shelters/1/stock/movements").header("Authorization", inc)).andExpect(jsonPath("$.content[0].reason").value("donation"));
        mvc.perform(get("/api/evacuees/1").header("Authorization", inc)).andExpect(jsonPath("$.specialNeed").exists());

        // supply request rejected with a reason; rejected requests cannot be dispatched
        JsonNode r = call(post("/api/shelters/1/supply-requests"), inc, "{\"eventId\":1,\"lines\":[{\"itemId\":4,\"quantity\":10}]}", 201);
        call(post("/api/supply-requests/" + r.get("id").asLong() + "/reject"), puri, "{\"reason\":\"Blankets already sent\"}", 200);
        call(post("/api/supply-requests/" + r.get("id").asLong() + "/dispatch"), puri, null, 409);
        call(post("/api/supply-requests/" + r.get("id").asLong() + "/approve"), token(KENDRAPARA_ADMIN), null, 404);
        mvc.perform(get("/api/supply-requests").param("status", "REJECTED").header("Authorization", puri))
                .andExpect(jsonPath("$.content[*].rejectionReason", hasItem("Blankets already sent")));
        mvc.perform(get("/api/supply-requests").param("shelter", "1").header("Authorization", inc)).andExpect(status().isOk());
        mvc.perform(get("/api/supply-requests/" + r.get("id").asLong()).header("Authorization", inc)).andExpect(jsonPath("$.history.length()").value(2));
        call(post("/api/supply-items"), state, "{\"name\":\"ORS packets\",\"unit\":\"packet\",\"perPersonPerDay\":0.5}", 201);
        mvc.perform(get("/api/supply-items").header("Authorization", inc)).andExpect(jsonPath("$[*].name", hasItem("ORS packets")));
    }

    @Test
    void dashboardsAndPublicPagesUseRealData() throws Exception {
        mvc.perform(get("/api/dashboard/district/1").param("event", "1").param("from", "2026-01-01").param("to", "2027-12-31")
                        .header("Authorization", token(PURI_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.districtName").value("Puri"))
                .andExpect(jsonPath("$.totalEvacuees", greaterThan(100)))
                .andExpect(jsonPath("$.specialNeeds.ELDERLY", greaterThan(0)))
                .andExpect(jsonPath("$.shortages[*].shelterName", hasItem("Satapada MCS")));
        mvc.perform(get("/api/dashboard/state").header("Authorization", token(STATE)))
                .andExpect(jsonPath("$.districts.length()").value(2))
                .andExpect(jsonPath("$.sheltersByStatus.FULL", greaterThanOrEqualTo(1)));
        mvc.perform(get("/api/dashboard/state").header("Authorization", token(PURI_ADMIN))).andExpect(status().isForbidden());
        mvc.perform(get("/api/public/shelters/available").param("district", "2"))
                .andExpect(jsonPath("$.dataNotice", containsString("Demonstration")))
                .andExpect(jsonPath("$.shelters[*].shelterName", not(hasItem("Mahakalapada MCS"))));   // full shelters hidden
        mvc.perform(get("/api/districts")).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/alerts").param("status", "ALL").header("Authorization", token(PURI_ADMIN))).andExpect(status().isOk());
        mvc.perform(get("/api/shortages").header("Authorization", token(SATAPADA_INCHARGE)))
                .andExpect(jsonPath("$[*].shelterName", everyItem(is("Satapada MCS"))));
    }
}
