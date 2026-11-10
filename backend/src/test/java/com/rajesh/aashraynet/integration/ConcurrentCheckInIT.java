package com.rajesh.aashraynet.integration;

import com.rajesh.aashraynet.repository.ShelterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The critical test: Satapada MCS (shelter 1) has exactly ONE free place in demo data (59/60).
 * Two volunteers press "check in" at the same moment. Exactly one may succeed.
 */
class ConcurrentCheckInIT extends IntegrationTestBase {
    @Autowired ShelterRepository shelters;

    @Test
    void lastBedCannotBeTakenTwice() throws Exception {
        assertThat(shelters.findById(1L).orElseThrow().freePlaces()).isEqualTo(1);
        String token = token(PURI_VOLUNTEER);
        String body = "{\"eventId\":1,\"fullName\":\"Racer %d\",\"age\":30,\"gender\":\"FEMALE\",\"village\":\"Satapada\"}";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int n = i;
            results.add(pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/shelters/1/evacuees").header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON).content(body.formatted(n)))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();   // release both threads together
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) statuses.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(statuses).containsExactlyInAnyOrder(201, 422);
        var s = shelters.findById(1L).orElseThrow();
        assertThat(s.getOccupancy()).isEqualTo(s.getCapacity());
    }
}
