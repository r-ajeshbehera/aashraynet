package com.rajesh.aashraynet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end through the HTTP API:
 * create event -> create shelter -> assign in-charge -> fill shelter -> nearest alternatives ->
 * supply request -> approve -> dispatch -> receive (stock updated once) -> shortage detection.
 */
class FullWorkflowIT extends IntegrationTestBase {

    @Test
    void completeReliefWorkflow() throws Exception {
        String state = token(STATE);
        String puriAdmin = token(PURI_ADMIN);

        // 1. state admin creates an event
        JsonNode event = read(mvc.perform(post("/api/events").header("Authorization", state)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Workflow Test Cyclone (DEMO)\",\"startDate\":\"2026-11-01\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long eventId = event.get("id").asLong();

        // 2. district admin creates a tiny shelter next to Satapada and makes Brahmagiri's in-charge run it
        JsonNode shelter = read(mvc.perform(post("/api/shelters").header("Authorization", puriAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"districtId\":1,\"name\":\"Test School\",\"village\":\"Satapada\",\"latitude\":19.668,"
                                + "\"longitude\":85.434,\"capacity\":2,\"hasMedical\":false,\"hasRamp\":true,\"hasGenerator\":false}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long shelterId = shelter.get("id").asLong();
        mvc.perform(patch("/api/shelters/" + shelterId + "/incharge").header("Authorization", puriAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":5}"))
                .andExpect(status().isOk());
        String incharge = token("brahmagiri.incharge@aashraynet.demo");

        // 3. fill it (capacity 2) and check the status
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/shelters/" + shelterId + "/evacuees").header("Authorization", incharge)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"eventId\":" + eventId + ",\"fullName\":\"Person " + i + "\",\"age\":30,"
                                    + "\"gender\":\"MALE\",\"village\":\"Satapada\",\"specialNeed\":\"ELDERLY\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/shelters/" + shelterId).header("Authorization", puriAdmin))
                .andExpect(jsonPath("$.status").value("FULL"))
                .andExpect(jsonPath("$.freePlaces").value(0));

        // 4. nearest alternatives for a family of 20: Satapada (1 free) must be skipped
        mvc.perform(get("/api/shelters/" + shelterId + "/nearest-available").param("people", "20").param("count", "3")
                        .header("Authorization", puriAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Krushnaprasad MCS"));

        // 5. supply workflow
        JsonNode req = read(mvc.perform(post("/api/shelters/" + shelterId + "/supply-requests").header("Authorization", incharge)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":" + eventId + ",\"lines\":[{\"itemId\":1,\"quantity\":5},{\"itemId\":2,\"quantity\":3}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long reqId = req.get("id").asLong();
        mvc.perform(post("/api/supply-requests/" + reqId + "/receive").header("Authorization", incharge))
                .andExpect(status().isConflict());                                   // not dispatched yet
        mvc.perform(post("/api/supply-requests/" + reqId + "/approve").header("Authorization", puriAdmin))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/supply-requests/" + reqId + "/dispatch").header("Authorization", puriAdmin))
                .andExpect(jsonPath("$.status").value("DISPATCHED"));
        mvc.perform(post("/api/supply-requests/" + reqId + "/receive").header("Authorization", incharge))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.history.length()").value(4));
        mvc.perform(post("/api/supply-requests/" + reqId + "/receive").header("Authorization", incharge))
                .andExpect(status().isConflict());                                   // duplicate receipt

        // 6. stock updated exactly once: rice 5 kg, water 3 L
        JsonNode stock = read(mvc.perform(get("/api/shelters/" + shelterId + "/stock").header("Authorization", incharge))
                .andReturn().getResponse().getContentAsString());
        for (JsonNode row : stock) {
            if (row.get("itemId").asLong() == 1) assertThat(row.get("quantity").decimalValue()).isEqualByComparingTo("5");
            if (row.get("itemId").asLong() == 2) assertThat(row.get("quantity").decimalValue()).isEqualByComparingTo("3");
        }

        // 7. water: 3 L for 2 people at 4 L/day = 0.37 days -> shortage listed for this shelter
        mvc.perform(get("/api/shortages").header("Authorization", puriAdmin))
                .andExpect(jsonPath("$[?(@.shelterId == " + shelterId + " && @.itemName == 'Drinking water')]").exists());

        // 8. checkout frees a place
        JsonNode list = read(mvc.perform(get("/api/shelters/" + shelterId + "/evacuees").header("Authorization", incharge))
                .andReturn().getResponse().getContentAsString());
        long evacueeId = list.get("content").get(0).get("id").asLong();
        mvc.perform(post("/api/evacuees/" + evacueeId + "/checkout").header("Authorization", incharge))
                .andExpect(jsonPath("$.shelter.occupancy").value(1));
        mvc.perform(post("/api/evacuees/" + evacueeId + "/checkout").header("Authorization", incharge))
                .andExpect(status().isConflict());
    }
}
