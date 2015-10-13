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

package vanilla.java.intserver.api;

import net.openhft.chronicle.ExcerptAppender;

public class S2CWriter implements IClient {
    static final char RESPONSE = 'r';
    final ExcerptAppender excerpt;

    public S2CWriter(ExcerptAppender excerpt) {
        this.excerpt = excerpt;
    }

    @Override
    public void response(int request, int response, Object... args) {
        excerpt.startExcerpt();
        excerpt.writeByte(RESPONSE);
        excerpt.writeInt(request);
        excerpt.writeInt(response);
        excerpt.writeInt(args.length);
        for (Object arg : args) {
            excerpt.writeObject(arg);
        }
        excerpt.finish();
    }
}
