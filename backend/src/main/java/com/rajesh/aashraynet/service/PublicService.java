package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.response.PublicFindResponse;
import com.rajesh.aashraynet.dto.response.PublicMatch;
import com.rajesh.aashraynet.dto.response.PublicShelterList;
import com.rajesh.aashraynet.dto.response.PublicShelterResponse;
import com.rajesh.aashraynet.repository.EvacueeRepository;
import com.rajesh.aashraynet.repository.ShelterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Public (no login) features. Privacy rules:
 *  - family finder returns ONLY shelter name + district (never age, gender, needs, ids),
 *  - identical response shape whether or not someone was found,
 *  - exact normalised matching (no partial search => no browsing of the evacuee list),
 *  - rate-limited by RateLimitFilter.
 */
@Service
public class PublicService {
    public static final int MAX_MATCHES = 5;
    public static final String DATA_NOTICE =
            "Demonstration data for a portfolio project. Not an official source of shelter information.";
    public static final String FINDER_NOTICE =
            "Only the shelter name and district are shown, to protect evacuees' privacy. If nothing is found, "
          + "check the spelling or contact the district control room. " + DATA_NOTICE;

    private final EvacueeRepository evacuees;
    private final ShelterRepository shelters;

    public PublicService(EvacueeRepository evacuees, ShelterRepository shelters) {
        this.evacuees = evacuees;
        this.shelters = shelters;
    }

    @Transactional(readOnly = true)
    public PublicFindResponse find(String name, String village) {
        String n = TextNormalizer.normalize(name);
        String v = TextNormalizer.normalize(village);
        if (n == null || v == null || n.length() < 2 || v.length() < 2) {
            return new PublicFindResponse(List.of(), FINDER_NOTICE);
        }
        List<PublicMatch> matches = evacuees.publicFind(n, v, MAX_MATCHES);
        return new PublicFindResponse(matches, FINDER_NOTICE);
    }

    @Transactional(readOnly = true)
    public PublicShelterList availableShelters(Long districtId) {
        List<PublicShelterResponse> list = shelters.findScoped(districtId, true).stream()
                .filter(s -> s.freePlaces() > 0)
                .map(s -> new PublicShelterResponse(s.getName(), s.getDistrict().getName(), s.getVillage(), s.status(),
                        s.freePlaces(), s.isHasMedical(), s.isHasRamp(), s.getUpdatedAt()))
                .toList();
        return new PublicShelterList(DATA_NOTICE, Instant.now(), list);
    }
}
