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

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

public class StatefulChronicleTestBase extends ChronicleTcpTestBase {

    // *************************************************************************
    //
    // *************************************************************************

    public void testJira77(Chronicle chronicleSrc, Chronicle chronicleTarget) throws IOException{
        final int BYTES_LENGTH = 1 << 16;

        final Random random = new Random();

        final PortSupplier portSupplier = new PortSupplier();
        final Chronicle chronicleSource = ChronicleQueueBuilder.source(chronicleSrc)
            .minBufferSize(2 * BYTES_LENGTH)
            .bindAddress(0)
            .connectionListener(portSupplier)
            .build();

        final int port = portSupplier.getAndAssertOnError();
        final Chronicle chronicleSink = ChronicleQueueBuilder.sink(chronicleTarget)
            .minBufferSize(2 * BYTES_LENGTH)
            .connectAddress("localhost", port)
            .build();

        ExcerptAppender app = chronicleSource.createAppender();
        byte[] bytes = new byte[BYTES_LENGTH];
        random.nextBytes(bytes);
        app.startExcerpt(4 + 4 + bytes.length);
        app.writeInt(bytes.length);
        app.write(bytes);
        app.finish();

        ExcerptTailer vtail = chronicleSink.createTailer();
        byte[] bytes2 = null;
        while (vtail.nextIndex()) {
            int bytesLength = vtail.readInt();
            bytes2 = new byte[bytesLength];
            vtail.read(bytes2);
            vtail.finish();
        }

        assertArrayEquals(bytes, bytes2);

        app.close();
        vtail.close();

        chronicleSrc.close();
        chronicleSource.close();
        chronicleSink.close();

        chronicleSrc.clear();
        chronicleTarget.clear();
    }

    public void testJira80(final ChronicleQueueBuilder chronicleMasterBuilder, final ChronicleQueueBuilder chronicleSlaveBuilder) throws IOException {
        final long chunks = 4;
        final long itemsPerChunk = 100000;

        final PortSupplier portSupplier = new PortSupplier();

        final Chronicle chronicleMaster = chronicleMasterBuilder.build();
        final Chronicle chronicleSource = ChronicleQueueBuilder.source(chronicleMaster)
            .bindAddress(0)
            .connectionListener(portSupplier)
            .build();

        chronicleSource.clear();

        final int port = portSupplier.getAndAssertOnError();
        final ExcerptAppender appender = chronicleSource.createAppender();
        for (long i = 0; i < (chunks * itemsPerChunk); i++) {
            appender.startExcerpt();
            appender.writeLong(i);
            appender.writeChar('=');
            appender.append(i);
            appender.append('\n');
            appender.finish();
        }

        appender.close();

        for(long i=0; i <= chunks; i++) {
            Chronicle chronicleSink = ChronicleQueueBuilder.sink(chronicleSlaveBuilder.build())
                .connectAddress("localhost", port)
                .build();

            if(i == 0) {
                chronicleSink.clear();
            }

            final ExcerptTailer tailer = chronicleSink.createTailer();
            for(long c=0; c < (i * itemsPerChunk); c++) {
                while (!tailer.nextIndex()) { }
                long n1 = tailer.readLong();
                char ch = tailer.readChar();
                long n2 = tailer.parseLong();

                assertEquals(c , n1);
                assertEquals(ch, '=');
                assertEquals(c , n2);

                tailer.finish();
            }

            tailer.close();
            chronicleSink.close();
        }

        // compare source and sink
        final Chronicle slave  = chronicleSlaveBuilder.build();
        final ExcerptTailer slaveTailer = slave.createTailer().toStart();

        final ExcerptTailer masterTailer = chronicleMaster.createTailer().toStart();

        for (long i = 0; i < (chunks * itemsPerChunk); i++) {
            assertTrue(masterTailer.nextIndex());
            assertTrue(slaveTailer.nextIndex());

            assertEquals(masterTailer.readLong() , slaveTailer.readLong());
            assertEquals(masterTailer.readChar() , slaveTailer.readChar());
            assertEquals(masterTailer.parseLong(), slaveTailer.parseLong());

            masterTailer.finish();
            slaveTailer.finish();
        }

        masterTailer.close();
        slaveTailer.close();

        slave.close();
        chronicleSource.close();

        slave.clear();
        chronicleSource.clear();
    }
}
