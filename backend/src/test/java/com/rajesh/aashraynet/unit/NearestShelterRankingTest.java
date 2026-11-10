package com.rajesh.aashraynet.unit;

import com.rajesh.aashraynet.dto.response.NearestShelterResponse;
import com.rajesh.aashraynet.entity.Shelter;
import com.rajesh.aashraynet.service.NearestShelterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NearestShelterRankingTest {
    // origin: Satapada (full)
    private final Shelter satapada = TestData.shelter(1, "Satapada", 19.667, 85.433, 60, 60);
    private final Shelter krushna = TestData.shelter(3, "Krushnaprasad", 19.702, 85.378, 50, 10);  // ~7 km, 40 free
    private final Shelter brahma = TestData.shelter(2, "Brahmagiri", 19.733, 85.680, 80, 30);      // ~27 km, 50 free
    private final Shelter puriTown = TestData.shelter(4, "Puri Town", 19.810, 85.830, 40, 30);     // ~45 km, 10 free

    private List<NearestShelterResponse> rank(List<Shelter> candidates, int people, int count) {
        return NearestShelterService.rank(1L, 19.667, 85.433, candidates, people, count, 100);
    }

    @Test void currentShelterIsExcluded() {
        assertThat(rank(List.of(satapada, krushna), 1, 5)).extracting(NearestShelterResponse::shelterId).containsExactly(3L);
    }

    @Test void sortedNearestFirst() {
        assertThat(rank(List.of(puriTown, brahma, krushna), 1, 5))
                .extracting(NearestShelterResponse::name).containsExactly("Krushnaprasad", "Brahmagiri", "Puri Town");
    }

    @Test void familyOfTwentySkipsShelterWithOnlyTenFree() {
        assertThat(rank(List.of(puriTown, brahma, krushna), 20, 5))
                .extracting(NearestShelterResponse::name).containsExactly("Krushnaprasad", "Brahmagiri");
    }

    @Test void insufficientCapacityExcluded() {
        assertThat(rank(List.of(puriTown), 11, 5)).isEmpty();
    }

    @Test void noAlternativesGivesEmptyList() {
        assertThat(rank(List.of(), 1, 3)).isEmpty();
    }

    @Test void respectsCountAndRadius() {
        assertThat(rank(List.of(puriTown, brahma, krushna), 1, 2)).hasSize(2);
        assertThat(NearestShelterService.rank(1L, 19.667, 85.433, List.of(puriTown, brahma, krushna), 1, 5, 10))
                .extracting(NearestShelterResponse::name).containsExactly("Krushnaprasad");
    }

    @Test void inactiveSheltersExcluded() {
        krushna.setActive(false);
        assertThat(rank(List.of(krushna, brahma), 1, 5)).extracting(NearestShelterResponse::name).containsExactly("Brahmagiri");
    }

    @Test void resultContainsDistanceAndFreePlaces() {
        NearestShelterResponse r = rank(List.of(krushna), 1, 1).get(0);
        assertThat(r.distanceKm()).isBetween(6.0, 8.0);
        assertThat(r.freePlaces()).isEqualTo(40);
        assertThat(r.district()).isEqualTo("Puri");
    }
}
