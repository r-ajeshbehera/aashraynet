package com.rajesh.aashraynet.unit;

import com.rajesh.aashraynet.entity.*;
import com.rajesh.aashraynet.exception.InvalidStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplyRequestStateMachineTest {
    private final User incharge = new User("In-charge", "i@x", "h", Role.SHELTER_INCHARGE, TestData.PURI);
    private final User admin = new User("Admin", "a@x", "h", Role.DISTRICT_ADMIN, TestData.PURI);
    private SupplyRequest request;
    private SupplyItem rice;

    @BeforeEach
    void setUp() {
        DisasterEvent event = new DisasterEvent("Demo", LocalDate.of(2026, 10, 23), null, admin);
        rice = new SupplyItem("Rice", "kg", new BigDecimal("0.40"));
        rice.setIdForTest(1L);
        request = new SupplyRequest(event, TestData.shelter(1, "A", 19.6, 85.4, 10, 5), incharge, null);
        request.addLine(rice, new BigDecimal("100"));
    }

    @Test void newRequestIsRequested() {
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REQUESTED);
        assertThat(request.getLines()).hasSize(1);
    }

    @Test void approvalRecordsApprover() {
        request.approve(admin);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getApprovedBy()).isSameAs(admin);
    }

    @Test void rejectionRecordsReason() {
        request.reject(admin, "Duplicate request");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getRejectionReason()).isEqualTo("Duplicate request");
    }

    @Test void fullFlowToReceived() {
        request.approve(admin);
        request.dispatch(admin);
        request.receive(incharge);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.RECEIVED);
        assertThat(request.getReceivedAt()).isNotNull();
    }

    @Test void receiveBeforeDispatchIsRejected() {
        request.approve(admin);
        assertThatThrownBy(() -> request.receive(incharge)).isInstanceOf(InvalidStateException.class);
    }

    @Test void duplicateReceiptIsRejected() {
        request.approve(admin);
        request.dispatch(admin);
        request.receive(incharge);
        assertThatThrownBy(() -> request.receive(incharge)).isInstanceOf(InvalidStateException.class);
    }

    @Test void rejectedRequestCannotBeApproved() {
        request.reject(admin, "no");
        assertThatThrownBy(() -> request.approve(admin)).isInstanceOf(InvalidStateException.class);
    }

    @Test void dispatchRequiresApproval() {
        assertThatThrownBy(() -> request.dispatch(admin)).isInstanceOf(InvalidStateException.class);
    }

    @Test void sameItemTwiceIsRejected() {
        assertThatThrownBy(() -> request.addLine(rice, BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void nonPositiveQuantityIsRejected() {
        SupplyItem water = new SupplyItem("Water", "litre", new BigDecimal("4"));
        water.setIdForTest(2L);
        assertThatThrownBy(() -> request.addLine(water, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
