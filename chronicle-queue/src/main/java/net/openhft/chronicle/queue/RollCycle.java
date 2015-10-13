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

import java.time.ZoneId;
import java.util.Objects;

/**
 * Created by peter.lawrey on 19/09/2015.
 */
public interface RollCycle {
    static RollCycle forLength(int length) {
        for (int i = RollCycles.VALUES.length - 1; i >= 0; i--) {
            if (RollCycles.VALUES[i].length == length) {
                return RollCycles.VALUES[i];
            }
        }

        throw new IllegalArgumentException("Unknown value for CycleLength (" + length + ")");
    }

    static RollCycle forFormat(String format) {
        for (int i = RollCycles.VALUES.length - 1; i >= 0; i--) {
            if (Objects.equals(RollCycles.VALUES[i].format, format) || RollCycles.VALUES[i].format.equals(format)) {
                return RollCycles.VALUES[i];
            }
        }

        throw new IllegalArgumentException("Unknown value for CycleFormat (" + format + ")");
    }

    /**
     * TODO: refactor
     *
     * @param length
     * @param format
     * @param zone
     * @return
     */
    static RollCycle from(final int length, final String format, final ZoneId zone) {
        return new RollCycle() {
            @Override
            public String format() {
                return format;
            }
            @Override
            public int length() {
                return length;
            }
            @Override
            public ZoneId zone() {
                return zone;
            }
        };
    }

    String format();

    int length();

    ZoneId zone();


    default int current() {
        return (int) (System.currentTimeMillis() / length());
    }
}
