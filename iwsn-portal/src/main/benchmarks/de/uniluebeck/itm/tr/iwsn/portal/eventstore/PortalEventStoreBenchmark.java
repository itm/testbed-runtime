package de.uniluebeck.itm.tr.iwsn.portal.eventstore;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.base.Function;
import de.uniluebeck.itm.eventstore.EventStore;
import de.uniluebeck.itm.eventstore.EventStoreFactory;
import net.openhft.chronicle.tools.ChronicleTools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class PortalEventStoreBenchmark {

    @Param({"500000", "1000000", "5000000"})
    int number;

    private EventStore<String> store;

    @BeforeExperiment
    private void createEventStore() {
        final String basePath = System.getProperty("java.io.tmpdir") + "/SimpleChronicle_" + System.currentTimeMillis();
        ChronicleTools.deleteOnExit(basePath);
        try {
            //noinspection unchecked
            store = EventStoreFactory.<String>create()
                    .eventStoreWithBasePath(basePath)
                    .withSerializers(createSerializers())
                    .andDeserializers(createDeserializers())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void writer(int reps) {
        try {
            for (int i = 0; i < reps; i++) {
                for (int k = 0; k < number; k++) {
                    store.storeEvent("Test" + i);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @AfterExperiment
    private void destroyEventStore() {
        try {
            store.close();
            store = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<Class<?>, Function<byte[], ?>> createDeserializers() {
        Map<Class<?>, Function<byte[], ?>> deserializers = newHashMap();
        deserializers.put(String.class, new Function<byte[], String>() {
                    @Override
                    public String apply(byte[] bytes) {
                        try {
                            return new String(bytes, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            return null;
                        }
                    }
                }
        );
        deserializers.put(BigInteger.class, new Function<byte[], BigInteger>() {
                    @Override
                    public BigInteger apply(byte[] bytes) {
                        return new BigInteger(bytes);
                    }
                }
        );
        return deserializers;
    }

    private static Map<Class<?>, Function<?, byte[]>> createSerializers() {
        Map<Class<?>, Function<?, byte[]>> serializers = newHashMap();
        serializers.put(String.class, new Function<String, byte[]>() {
                    @Override
                    public byte[] apply(String string) {
                        return string.getBytes();
                    }
                }
        );
        serializers.put(BigInteger.class, new Function<BigInteger, byte[]>() {
                    @Override
                    public byte[] apply(BigInteger o) {
                        return o.toByteArray();
                    }
                }
        );
        return serializers;
    }
}
