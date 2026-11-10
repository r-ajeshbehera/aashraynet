package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.entity.*;
import com.rajesh.aashraynet.exception.InvalidStateException;
import com.rajesh.aashraynet.repository.*;
import com.rajesh.aashraynet.security.AccessPolicy;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import com.rajesh.aashraynet.unit.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplyServiceReceiptTest {
    @Mock SupplyRequestRepository requests;
    @Mock SupplyRequestHistoryRepository history;
    @Mock SupplyItemRepository items;
    @Mock UserRepository users;
    @Mock ShelterService shelters;
    @Mock EventService events;
    @Mock StockService stock;
    @Mock AccessPolicy access;
    @Mock AuditService audit;
    @InjectMocks SupplyService service;

    private final AppUserPrincipal incharge = new AppUserPrincipal(4L, "i@x", "h", Role.SHELTER_INCHARGE, 1L, true);
    private SupplyRequest request;

    @BeforeEach
    void setUp() {
        User admin = new User("Admin", "a@x", "h", Role.DISTRICT_ADMIN, TestData.PURI);
        User inchargeUser = new User("In-charge", "i@x", "h", Role.SHELTER_INCHARGE, TestData.PURI);
        SupplyItem rice = new SupplyItem("Rice", "kg", new BigDecimal("0.4"));
        rice.setIdForTest(1L);
        SupplyItem water = new SupplyItem("Water", "litre", new BigDecimal("4"));
        water.setIdForTest(2L);
        request = new SupplyRequest(new DisasterEvent("Demo", LocalDate.now(), null, admin),
                TestData.shelter(1, "Satapada", 19.6, 85.4, 60, 50), inchargeUser, null);
        request.addLine(rice, new BigDecimal("100"));
        request.addLine(water, new BigDecimal("500"));
        request.approve(admin);
        request.dispatch(admin);
        when(requests.findWithShelter(7L)).thenReturn(Optional.of(request));
        lenient().when(users.getReferenceById(4L)).thenReturn(inchargeUser);
        lenient().when(history.findByRequestIdOrderByChangedAtAsc(any())).thenReturn(List.of());
    }

    @Test
    void receiptAddsEachLineToStockExactlyOnce() {
        service.receive(7L, incharge);
        verify(stock, times(1)).addReceipt(any(), argThat(i -> i.getId() == 1L), eq(new BigDecimal("100")), any(), any(), eq(4L));
        verify(stock, times(1)).addReceipt(any(), argThat(i -> i.getId() == 2L), eq(new BigDecimal("500")), any(), any(), eq(4L));
        verify(requests).saveAndFlush(request);
    }

    @Test
    void secondReceiptIsRejectedWithoutTouchingStock() {
        service.receive(7L, incharge);
        clearInvocations(stock);
        assertThatThrownBy(() -> service.receive(7L, incharge)).isInstanceOf(InvalidStateException.class);
        verifyNoInteractions(stock);
    }
}
