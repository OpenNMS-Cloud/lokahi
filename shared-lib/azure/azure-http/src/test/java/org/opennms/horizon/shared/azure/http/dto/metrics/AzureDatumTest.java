package org.opennms.horizon.shared.azure.http.dto.metrics;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AzureDatumTest {

    @Test
    public void testDatumWithTotal() {
        AzureDatum datumWithTotal = getDatumWithTotal();
        assertEquals(123d, datumWithTotal.getValue(), 0d);
        assertEquals(123d, datumWithTotal.getTotal(), 0d);
        assertNull(datumWithTotal.getAverage());
    }

    @Test
    public void testDatumWithAverage() {
        AzureDatum datumWithAverage = getDatumWithAverage();
        assertEquals(456d, datumWithAverage.getValue(), 0d);
        assertEquals(456d, datumWithAverage.getAverage(), 0d);
        assertNull(datumWithAverage.getTotal());
    }


    private AzureDatum getDatumWithTotal() {
        AzureDatum datum = new AzureDatum();
        datum.setTimeStamp(Instant.now().toString());
        datum.setTotal(123d);
        return datum;
    }

    private AzureDatum getDatumWithAverage() {
        AzureDatum datum = new AzureDatum();
        datum.setTimeStamp(Instant.now().toString());
        datum.setAverage(456d);
        return datum;
    }
}
