package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;

public class PortalEventStore2CaliperBenchmarkHarness {

    @Param({"5", "10", "15"})
    int number;

    @Benchmark
    public void timeMyOperation(int reps) {
        for (int i = 0; i < reps; i++) {
            System.out.println(number);
        }
    }
}
