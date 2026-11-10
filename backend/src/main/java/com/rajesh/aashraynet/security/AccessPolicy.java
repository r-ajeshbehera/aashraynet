package com.rajesh.aashraynet.security;

import com.rajesh.aashraynet.entity.Role;
import com.rajesh.aashraynet.entity.Shelter;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * All "may this user touch this record?" rules in one place.
 * Out-of-scope records produce 404 (not 403) so a user cannot discover data in another district.
 */
@Component
public class AccessPolicy {

    /** STATE_ADMIN = null (all districts); everyone else = own district. Use this to scope repository queries. */
    public Long districtScope(AppUserPrincipal user) {
        return user.isStateAdmin() ? null : user.districtId();
    }

    public void assertDistrict(AppUserPrincipal user, Long districtId) {
        if (!user.isStateAdmin() && !districtId.equals(user.districtId())) {
            throw new ResourceNotFoundException("District", districtId);
        }
    }

    public boolean canView(AppUserPrincipal user, Shelter shelter) {
        return switch (user.role()) {
            case STATE_ADMIN -> true;
            case DISTRICT_ADMIN, VOLUNTEER -> shelter.getDistrictId().equals(user.districtId());
            case SHELTER_INCHARGE -> shelter.isInchargeUser(user.id());
        };
    }

    public void assertCanView(AppUserPrincipal user, Shelter shelter) {
        if (!canView(user, shelter)) throw new ResourceNotFoundException("Shelter", shelter.getId());
    }

    /** Check-in: volunteers of the same district, or the shelter's own in-charge. */
    public void assertCanCheckIn(AppUserPrincipal user, Shelter shelter) {
        assertCanView(user, shelter);
        if (user.role() != Role.VOLUNTEER && user.role() != Role.SHELTER_INCHARGE) {
            throw new AccessDeniedException("Only volunteers and shelter in-charges register evacuees");
        }
    }

    /** Check-out, facilities, stock, viewing evacuees: in-charge of the shelter or admin of its district. */
    public void assertCanOperate(AppUserPrincipal user, Shelter shelter) {
        assertCanView(user, shelter);
        if (user.role() == Role.VOLUNTEER) throw new AccessDeniedException("Volunteers cannot perform this action");
    }

    /** Create/edit shelters, approve/dispatch supplies: district admin of that district, or state admin. */
    public void assertDistrictAdmin(AppUserPrincipal user, Shelter shelter) {
        assertCanView(user, shelter);
        if (user.role() != Role.DISTRICT_ADMIN && user.role() != Role.STATE_ADMIN) {
            throw new AccessDeniedException("District administrator required");
        }
    }

    /** Requesting and receiving supplies: only the shelter's own in-charge. */
    public void assertIncharge(AppUserPrincipal user, Shelter shelter) {
        assertCanView(user, shelter);
        if (user.role() != Role.SHELTER_INCHARGE) throw new AccessDeniedException("Shelter in-charge required");
    }

    /** Special-needs / medical information is hidden from volunteers. */
    public boolean canSeeSensitive(AppUserPrincipal user) {
        return user.role() != Role.VOLUNTEER;
    }
}
