package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.request.AssignInchargeRequest;
import com.rajesh.aashraynet.dto.request.FacilitiesRequest;
import com.rajesh.aashraynet.dto.request.ShelterRequest;
import com.rajesh.aashraynet.dto.response.ShelterResponse;
import com.rajesh.aashraynet.entity.*;
import com.rajesh.aashraynet.exception.BusinessRuleException;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.DistrictRepository;
import com.rajesh.aashraynet.repository.ShelterRepository;
import com.rajesh.aashraynet.repository.UserRepository;
import com.rajesh.aashraynet.security.AccessPolicy;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShelterService {
    private final ShelterRepository shelters;
    private final DistrictRepository districts;
    private final UserRepository users;
    private final AccessPolicy access;
    private final AuditService audit;

    public ShelterService(ShelterRepository shelters, DistrictRepository districts, UserRepository users,
                          AccessPolicy access, AuditService audit) {
        this.shelters = shelters;
        this.districts = districts;
        this.users = users;
        this.access = access;
        this.audit = audit;
    }

    @Transactional
    public ShelterResponse create(ShelterRequest req, AppUserPrincipal actor) {
        access.assertDistrict(actor, req.districtId());
        District district = districts.findById(req.districtId())
                .orElseThrow(() -> new ResourceNotFoundException("District", req.districtId()));
        Shelter s = new Shelter(district, req.name().trim(), req.village().trim(), req.latitude(), req.longitude(), req.capacity());
        s.setHasMedical(req.hasMedical());
        s.setHasRamp(req.hasRamp());
        s.setHasGenerator(req.hasGenerator());
        if (req.active() != null) s.setActive(req.active());
        s = shelters.save(s);
        audit.log(actor.id(), "SHELTER_CREATED", "SHELTER", s.getId(), null);
        return ShelterResponse.from(s);
    }

    @Transactional
    public ShelterResponse update(Long id, ShelterRequest req, AppUserPrincipal actor) {
        Shelter s = load(id);
        access.assertDistrictAdmin(actor, s);
        if (!s.getDistrictId().equals(req.districtId())) {
            throw new BusinessRuleException("DISTRICT_CHANGE", "A shelter cannot be moved to another district");
        }
        s.setName(req.name().trim());
        s.setVillage(req.village().trim());
        s.setLatitude(req.latitude());
        s.setLongitude(req.longitude());
        s.setCapacity(req.capacity());       // rejects capacity below current occupancy
        s.setHasMedical(req.hasMedical());
        s.setHasRamp(req.hasRamp());
        s.setHasGenerator(req.hasGenerator());
        if (req.active() != null) s.setActive(req.active());
        shelters.saveAndFlush(s);            // optimistic lock check happens here
        audit.log(actor.id(), "SHELTER_UPDATED", "SHELTER", id, null);
        return ShelterResponse.from(s);
    }

    @Transactional(readOnly = true)
    public List<ShelterResponse> list(Long districtFilter, OccupancyStatus status, boolean activeOnly, AppUserPrincipal actor) {
        List<Shelter> list;
        if (actor.role() == Role.SHELTER_INCHARGE) {
            list = shelters.findByInchargeIdOrderByName(actor.id());
        } else {
            Long scope = actor.isStateAdmin() ? districtFilter : actor.districtId();
            list = shelters.findScoped(scope, activeOnly);
        }
        return list.stream()
                .filter(s -> !activeOnly || s.isActive())
                .filter(s -> status == null || s.status() == status)
                .map(ShelterResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShelterResponse get(Long id, AppUserPrincipal actor) {
        Shelter s = load(id);
        access.assertCanView(actor, s);
        return ShelterResponse.from(s);
    }

    @Transactional
    public ShelterResponse updateFacilities(Long id, FacilitiesRequest req, AppUserPrincipal actor) {
        Shelter s = load(id);
        access.assertCanOperate(actor, s);
        s.setHasMedical(req.hasMedical());
        s.setHasRamp(req.hasRamp());
        s.setHasGenerator(req.hasGenerator());
        shelters.saveAndFlush(s);
        audit.log(actor.id(), "FACILITIES_UPDATED", "SHELTER", id, null);
        return ShelterResponse.from(s);
    }

    @Transactional
    public ShelterResponse assignIncharge(Long id, AssignInchargeRequest req, AppUserPrincipal actor) {
        Shelter s = load(id);
        access.assertDistrictAdmin(actor, s);
        if (req.userId() == null) {
            s.setIncharge(null);
        } else {
            User u = users.findById(req.userId()).orElseThrow(() -> new ResourceNotFoundException("User", req.userId()));
            if (u.getRole() != Role.SHELTER_INCHARGE || !u.isActive() || !s.getDistrictId().equals(u.getDistrictId())) {
                throw new BusinessRuleException("INVALID_INCHARGE",
                        "In-charge must be an active SHELTER_INCHARGE user of the same district");
            }
            s.setIncharge(u);
        }
        shelters.saveAndFlush(s);
        audit.log(actor.id(), "INCHARGE_ASSIGNED", "SHELTER", id, "user=" + req.userId());
        return ShelterResponse.from(s);
    }

    /** Loads a shelter or throws 404. */
    @Transactional(readOnly = true)
    public Shelter load(Long id) {
        return shelters.findWithDistrict(id).orElseThrow(() -> new ResourceNotFoundException("Shelter", id));
    }
}
