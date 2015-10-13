/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.tcp;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ChronicleQueueBuilder;
import net.openhft.chronicle.ExcerptAppender;
import net.openhft.chronicle.ExcerptTailer;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static net.openhft.chronicle.ChronicleQueueBuilder.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StatelessVanillaChronicleTailerTest extends StatelessChronicleTestBase {

    @Test(expected = UnsupportedOperationException.class)
    public void testVanillaStatelessExceptionOnCreateAppender() throws IOException, InterruptedException {
        ChronicleQueueBuilder.remoteTailer()
            .connectAddress("localhost", 9876)
            .build()
            .createAppender();
    }

    @Test(expected = IllegalStateException.class)
    public void testVanillaStatelessExceptionOnCreatTailerTwice() throws IOException, InterruptedException {
        Chronicle ch = ChronicleQueueBuilder.remoteTailer()
            .connectAddress("localhost", 9876)
            .build();

        ch.createTailer();
        ch.createTailer();
    }

    @Test
    public void testVanillaStatelessSink_001() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle source = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        final int port = portSupplier.getAndAssertOnError();
        final Chronicle sink = ChronicleQueueBuilder.remoteTailer()
            .connectAddress("localhost", port)
            .build();

        final int items = 1000000;
        final ExcerptAppender appender = source.createAppender();

        try {
            for (long i = 1; i <= items; i++) {
                appender.startExcerpt(8);
                appender.writeLong(i);
                appender.finish();
            }

            appender.close();

            final ExcerptTailer tailer1 = sink.createTailer().toStart();
            assertEquals(-1,tailer1.index());

            for (long i = 1; i <= items; i++) {
                assertTrue(tailer1.nextIndex());
                assertEquals(i, tailer1.readLong());
                tailer1.finish();
            }

            assertFalse(tailer1.nextIndex());
            tailer1.close();

            final ExcerptTailer tailer2 = sink.createTailer().toEnd();
            assertEquals(items, tailer2.readLong());
            assertFalse(tailer2.nextIndex());
            tailer2.close();

            sink.close();
            sink.clear();
        } finally {
            source.close();
            source.clear();

            assertFalse(new File(basePathSource).exists());
        }
    }

    @Test
    public void testVanillaStatelessSink_002() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle source = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        final int port = portSupplier.getAndAssertOnError();
        final Chronicle sink = ChronicleQueueBuilder.remoteTailer()
            .connectAddress("localhost", port)
            .build();

        try {
            final ExcerptAppender appender = source.createAppender();
            appender.startExcerpt(8);
            appender.writeLong(1);
            appender.finish();
            appender.startExcerpt(8);
            appender.writeLong(2);
            appender.finish();

            final ExcerptTailer tailer = sink.createTailer().toEnd();
            assertFalse(tailer.nextIndex());

            appender.startExcerpt(8);
            appender.writeLong(3);
            appender.finish();

            while(!tailer.nextIndex());

            assertEquals(3, tailer.readLong());
            tailer.finish();
            tailer.close();

            appender.close();

            sink.close();
            sink.clear();
        } finally {
            source.close();
            source.clear();

            assertFalse(new File(basePathSource).exists());
        }
    }

    @Test
    public void testVanillaStatelessSink_004() throws IOException, InterruptedException {
        final int tailers = 4;
        final int items = 1000000;

        final String basePathSource = getVanillaTestPath("source");
        final ExecutorService executor = Executors.newFixedThreadPool(tailers);
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle source = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        final int port = portSupplier.getAndAssertOnError();
        try {
            for(int i=0;i<tailers;i++) {
                executor.submit(new Runnable() {
                    public void run() {
                        try {
                            final Chronicle sink = ChronicleQueueBuilder.remoteTailer()
                                .connectAddress("localhost", port)
                                .build();

                            final ExcerptTailer tailer = sink.createTailer().toStart();
                            for (long i = 1; i <= items; ) {
                                if (tailer.nextIndex()) {
                                    assertEquals(i, tailer.readLong());
                                    tailer.finish();

                                    i++;
                                }
                            }

                            tailer.close();

                            sink.close();
                            sink.clear();
                        } catch (Exception e) {
                            errorCollector.addError(e);
                        } catch (AssertionError e) {
                            errorCollector.addError(e);
                        }
                    }
                });
            }

            Thread.sleep(100);

            final ExcerptAppender appender = source.createAppender();

            for (int i=1; i<=items; i++) {
                appender.startExcerpt(8);
                appender.writeLong(i);
                appender.finish();
            }

            appender.close();

            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } finally {
            source.close();
            source.clear();

            assertFalse(new File(basePathSource).exists());
        }
    }

    @Test
    public void testVanillaStatelessSink_005() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle source = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        final int port = portSupplier.getAndAssertOnError();
        final Chronicle sink = ChronicleQueueBuilder.remoteTailer()
            .connectAddress("localhost", port)
            .build();

        final int items = 1000;
        final ExcerptAppender appender = source.createAppender();
        final ExcerptTailer st = source.createTailer().toStart();
        final ExcerptTailer tailer = sink.createTailer();

        try {
            for (int i = 0; i < items; i++) {
                appender.startExcerpt(8);
                appender.writeLong(i);
                appender.finish();

                st.nextIndex();
                st.finish();

                assertTrue(tailer.index(st.index()));
                assertEquals(i, tailer.readLong());
            }

            appender.close();
            tailer.close();
            st.close();

            sink.close();
            sink.clear();
        } finally {
            source.close();
            source.clear();

            assertFalse(new File(basePathSource).exists());
        }
    }

    @Test
    public void testVanillaStatelessSink_006() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle source = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        final int port = portSupplier.getAndAssertOnError();
        final Chronicle sink = ChronicleQueueBuilder.remoteTailer()
            .connectAddress("localhost", port)
            .build();

        final int items = 1000000;
        final ExcerptAppender appender = source.createAppender();
        final ExcerptTailer st = source.createTailer().toStart();

        long startIndex = Long.MIN_VALUE;
        long endIndex = Long.MIN_VALUE;

        try {
            for (int i = 1; i <= items; i++) {
                appender.startExcerpt(8);
                appender.writeLong(i);
                appender.finish();

                st.nextIndex();
                st.finish();

                if(i == 1) {
                    startIndex = st.index();

                } else if(i == items) {
                    endIndex = st.index();
                }
            }

            appender.close();
            st.close();

            final ExcerptTailer tailer1 = sink.createTailer().toStart();
            assertEquals(-1,tailer1.index());
            assertTrue(tailer1.nextIndex());
            assertEquals(startIndex, tailer1.index());
            assertEquals(1, tailer1.readLong());
            tailer1.finish();
            tailer1.close();

            final ExcerptTailer tailer2 = sink.createTailer().toEnd();
            assertEquals(endIndex, tailer2.index());
            assertEquals(items, tailer2.readLong());
            tailer2.finish();
            tailer2.close();

            sink.close();
            sink.clear();
        } finally {
            source.close();
            source.clear();

            assertFalse(new File(basePathSource).exists());
        }
    }

    // *************************************************************************
    //
    // *************************************************************************

    @Test
    public void testStatelessVanillaNonBlockingClient() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final ChronicleQueueBuilder sourceBuilder = vanilla(basePathSource)
                .source()
                .bindAddress(0)
                .connectionListener(portSupplier);

        final Chronicle source = sourceBuilder.build();

        final ReplicaChronicleQueueBuilder sinkBuilder = remoteTailer()
                .connectAddress("localhost", portSupplier.getAndAssertOnError())
                .readSpinCount(5);

        final Chronicle sinnk = sinkBuilder.build();

        testNonBlockingClient(
                source,
                sinnk,
                sinkBuilder.heartbeatIntervalMillis()
        );
    }

    // *************************************************************************
    // JIRA
    // *************************************************************************

    /*
     * https://higherfrequencytrading.atlassian.net/browse/CHRON-104
     */
    @Test
    public void testVanillaClientReconnection() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();
        final int items = 20;
        final CountDownLatch latch = new CountDownLatch(items);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Chronicle sink = remoteTailer()
                        .connectAddressProvider(new AddressProvider() {
                            @Override
                            public InetSocketAddress get() {
                                return new InetSocketAddress(
                                    "localhost",
                                    portSupplier.getAndAssertOnError());
                            }
                        })
                        .build();

                    ExcerptTailer tailer = sink.createTailer();
                    while(latch.getCount() > 0) {
                        if(tailer.nextIndex()) {
                            long expected = items - latch.getCount() + 1;
                            long actual = tailer.readLong();
                            assertEquals(expected, actual);
                            tailer.finish();
                            latch.countDown();

                        } else {
                            Thread.sleep(100);
                        }
                    }

                    tailer.close();
                    sink.close();
                    sink.clear();
                } catch (Exception e) {
                    LOGGER.warn("", e);
                    errorCollector.addError(e);
                }
            }
        });

        t.start();

        // Source 1
        final Chronicle source1 = vanilla(basePathSource)
                .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
                .build();

        ExcerptAppender appender1 = source1.createAppender();
        for(long i=0; i < items / 2 ; i++) {
            appender1.startExcerpt(8);
            appender1.writeLong(i + 1);
            appender1.finish();
        }

        appender1.close();

        while(latch.getCount() > 10) {
            Thread.sleep(25);
        }

        source1.close();

        portSupplier.reset();

        // Source 2
        final Chronicle source2 = vanilla(basePathSource)
                .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
                .build();

        ExcerptAppender appender2 = source2.createAppender();
        for(long i=items / 2; i < items; i++) {
            appender2.startExcerpt(8);
            appender2.writeLong(i + 1);
            appender2.finish();
        }

        appender2.close();

        final Chronicle check = vanilla(basePathSource).build();
        final ExcerptTailer checkTailer = check.createTailer();
        for (long i = 1; i <= items; ) {
            if(checkTailer.nextIndex()) {
                long actual = checkTailer.readLong();
                assertEquals(i, actual);
                checkTailer.finish();
                i++;
            }
        }

        checkTailer.close();

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());

        check.close();

        source2.close();
        source2.clear();
    }

    /*
     * https://higherfrequencytrading.atlassian.net/browse/CHRON-74
     */
    @Test
    public void testVanillaJiraChron74() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle chronicle = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        testJiraChron74(portSupplier.getAndAssertOnError(), chronicle);
    }

    /*
     * https://higherfrequencytrading.atlassian.net/browse/CHRON-75
     */
    @Test
    public void testVanillaJiraChron75() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle chronicle = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        testJiraChron75(portSupplier.getAndAssertOnError(), chronicle);
    }

    /*
     * https://higherfrequencytrading.atlassian.net/browse/CHRON-78
     */
    @Test
    public void testVanillaJiraChron78() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle chronicle = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        testJiraChron78(portSupplier.getAndAssertOnError(), chronicle);
    }

    /*
     * https://higherfrequencytrading.atlassian.net/browse/CHRON-81
     */
    @Test
    public void testVanillaJiraChron81() throws IOException, InterruptedException {
        final String basePathSource = getVanillaTestPath("source");
        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle chronicle = ChronicleQueueBuilder.vanilla(basePathSource)
            .source()
                .bindAddress(0)
                .connectionListener(portSupplier)
            .build();

        testJiraChron81(portSupplier.getAndAssertOnError(), chronicle);
    }
}
