package com.nstut.openui.controls;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AreaChartTest {

    @Test
    void areaChartInitializesWithDataPointsAndCalculatesPreferredSize() {
        AreaChart chart = new AreaChart(List.of(10.0, 25.0, 15.0, 40.0));
        chart.point("May", 55.0);

        assertEquals(180, chart.preferredWidth(null));
        assertEquals(100, chart.preferredHeight(null));

        chart.layout(0, 0, 200, 120);
        assertEquals(200, chart.getWidth());
        assertEquals(120, chart.getHeight());
    }
}
