/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.queue;

import net.openhft.chronicle.core.OS;
import org.junit.Rule;
import org.junit.rules.*;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

public class ChronicleQueueTestBase {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ChronicleQueueTestBase.class);
    protected static final boolean TRACE_TEST_EXECUTION = Boolean.getBoolean("queue.traceTestExecution");

    // *************************************************************************
    // JUNIT Rules
    // *************************************************************************

    @Rule
    public final TestName testName = new TestName();

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder(new File(System.getProperty("java.io.tmpdir")));

    @Rule
    public final ErrorCollector errorCollector = new ErrorCollector();

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            if(TRACE_TEST_EXECUTION) {
                LOGGER.info("Starting test: {}.{}",
                    description.getClassName(),
                    description.getMethodName()
                );
            }
        }
    };

    // *************************************************************************
    //
    // *************************************************************************

    protected File getTmpDir() {
        try {
            final File tmpDir = Files.createTempDirectory(getClass().getSimpleName() + "-").toFile();

            DeleteStatic.INSTANCE.add(tmpDir);

            // Log the temporary directory in OSX as it is quite obscure
            if(OS.isMacOSX()) {
                LOGGER.info("Tmp dir: {}", tmpDir);
            }

            return tmpDir;
        } catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // *************************************************************************
    //
    // *************************************************************************

    enum DeleteStatic {
        INSTANCE;
        final Set<File> toDeleteList = new LinkedHashSet<>();

        {
            Runtime.getRuntime().addShutdownHook(new Thread(
                () -> toDeleteList.forEach(ChronicleQueueTestBase::deleteDir)
            ));
        }

        synchronized void add(File path) {
            toDeleteList.add(path);
        }
    }

    private static void deleteDir(File dir) {
        if(dir.isDirectory()) {
            File[] files = dir.listFiles();
            if(files != null) {
                File[] arr$ = files;
                int len$ = files.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    File file = arr$[i$];
                    if(file.isDirectory()) {
                        deleteDir(file);
                    } else if(!file.delete()) {
                        LOGGER.info("... unable to delete {}", file);
                    }
                }
            }
        }

        dir.delete();
    }
}
