package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.entity.*;
import com.rajesh.aashraynet.repository.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortageDetectionTest {
    @Mock ShelterRepository shelters;
    @Mock SupplyItemRepository items;
    @Mock ShelterStockRepository stocks;
    @Mock ShortageAlertRepository alerts;
    @Mock DisasterEventRepository events;
    @InjectMocks ShortageService service;

    private Shelter satapada;
    private SupplyItem rice;
    private ShelterStock riceStock;

    @BeforeEach
    void setUp() {
        satapada = TestData.shelter(1, "Satapada", 19.6, 85.4, 60, 59);
        rice = new SupplyItem("Rice", "kg", new BigDecimal("0.40"));
        rice.setIdForTest(1L);
        riceStock = new ShelterStock(satapada, rice);
        riceStock.apply(new BigDecimal("20"));          // 20 / (59 x 0.4) = 0.84 days -> shortage
        User admin = new User("A", "a@x", "h", Role.STATE_ADMIN, null);
        when(events.findFirstByActiveTrueOrderByStartDateDesc())
                .thenReturn(Optional.of(new DisasterEvent("Demo", LocalDate.now(), null, admin)));
        when(shelters.findScoped(null, true)).thenReturn(List.of(satapada));
        when(items.findByActiveTrueOrderByName()).thenReturn(List.of(rice));
        when(stocks.findForShelters(anyCollection())).thenReturn(List.of(riceStock));
    }

    @Test
    void firstRunOpensAlertSecondRunOnlyRefreshes() {
        when(alerts.findByResolvedAtIsNull()).thenReturn(List.of());
        ShortageService.DetectionResult first = service.detect();
        assertThat(first.opened()).isEqualTo(1);
        verify(alerts, times(1)).save(any(ShortageAlert.class));

        ShortageAlert open = new ShortageAlert(null, 1L, 1L, new BigDecimal("0.84"), new BigDecimal("20"), 59);
        when(alerts.findByResolvedAtIsNull()).thenReturn(List.of(open));
        ShortageService.DetectionResult second = service.detect();
        assertThat(second.opened()).isZero();
        assertThat(second.refreshed()).isEqualTo(1);
        verify(alerts, times(1)).save(any(ShortageAlert.class));   // still only the first save
    }

    @Test
    void alertResolvesWhenStockIsReplenished() {
        riceStock.apply(new BigDecimal("100"));           // 120 / 23.6 = 5 days -> no shortage
        ShortageAlert open = new ShortageAlert(null, 1L, 1L, new BigDecimal("0.84"), new BigDecimal("20"), 59);
        when(alerts.findByResolvedAtIsNull()).thenReturn(List.of(open));
        ShortageService.DetectionResult result = service.detect();
        assertThat(result.resolved()).isEqualTo(1);
        assertThat(open.isOpen()).isFalse();
    }
}
