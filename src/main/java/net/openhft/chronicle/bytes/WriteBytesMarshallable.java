/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

/**
 * Write data directly as Bytes.
 */
@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface WriteBytesMarshallable extends CommonMarshallable {
    /**
     * Write to Bytes.  This can be used as an interface to extend or a lambda
     *
     * @param bytes to write to.
     */
    void writeMarshallable(BytesOut bytes);
}
