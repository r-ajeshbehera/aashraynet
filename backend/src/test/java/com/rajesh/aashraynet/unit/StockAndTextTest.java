package com.rajesh.aashraynet.unit;

import com.rajesh.aashraynet.entity.ShelterStock;
import com.rajesh.aashraynet.entity.SupplyItem;
import com.rajesh.aashraynet.service.TextNormalizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockAndTextTest {

    @Test void stockCannotGoNegative() {
        SupplyItem rice = new SupplyItem("Rice", "kg", new BigDecimal("0.4"));
        rice.setIdForTest(1L);
        ShelterStock stock = new ShelterStock(TestData.shelter(1, "A", 19.6, 85.4, 10, 1), rice);
        assertThat(stock.apply(new BigDecimal("10"))).isEqualByComparingTo("10");
        assertThatThrownBy(() -> stock.apply(new BigDecimal("-11"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(stock.getQuantity()).isEqualByComparingTo("10");
    }

    @Test void normalizerTrimsCollapsesAndLowercases() {
        assertThat(TextNormalizer.normalize("  Laxmi   NAYAK ")).isEqualTo("laxmi nayak");
        assertThat(TextNormalizer.normalize(null)).isNull();
    }
}
