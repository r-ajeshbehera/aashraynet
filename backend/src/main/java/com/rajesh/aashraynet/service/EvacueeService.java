package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.response.EvacueeResponse;
import com.rajesh.aashraynet.entity.Evacuee;
import com.rajesh.aashraynet.entity.Shelter;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.EvacueeRepository;
import com.rajesh.aashraynet.security.AccessPolicy;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of evacuees. Writes (check-in/out) live in CapacityService because they change occupancy. */
@Service
public class EvacueeService {
    private final EvacueeRepository evacuees;
    private final ShelterService shelters;
    private final AccessPolicy access;

    public EvacueeService(EvacueeRepository evacuees, ShelterService shelters, AccessPolicy access) {
        this.evacuees = evacuees;
        this.shelters = shelters;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public Page<EvacueeResponse> listForShelter(Long shelterId, boolean activeOnly, String q, Pageable pageable,
                                                AppUserPrincipal actor) {
        Shelter shelter = shelters.load(shelterId);
        access.assertCanView(actor, shelter);
        boolean sensitive = access.canSeeSensitive(actor);
        String query = q == null || q.isBlank() ? null : TextNormalizer.normalize(q);
        return evacuees.findForShelter(shelterId, activeOnly, query, pageable).map(e -> EvacueeResponse.from(e, sensitive));
    }

    @Transactional(readOnly = true)
    public EvacueeResponse get(Long id, AppUserPrincipal actor) {
        Evacuee e = evacuees.findWithShelter(id).orElseThrow(() -> new ResourceNotFoundException("Evacuee", id));
        access.assertCanOperate(actor, e.getShelter());
        return EvacueeResponse.from(e, access.canSeeSensitive(actor));
    }
}
