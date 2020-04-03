/*
 * This file is part of OurTube-serverj, licensed under the MIT License (MIT).
 *
 * Copyright (c) Kenzie Togami (kenzierocks) <https://kenzierocks.me>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.kenzierocks.ourtube;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

public class AuditLog {

    private static final Logger LOGGER = LoggerFactory.getLogger("AuditLog");

    public interface Action {

        Action log(String state);

        default Action attempted() {
            return log("attempted");
        }

        default Action performed() {
            return log("performed");
        }

        default Action denied() {
            return log("denied");
        }

    }

    private static class ActionImpl implements Action {

        private final String userId;
        private final String action;

        public ActionImpl(String userId, String action) {
            this.userId = userId;
            this.action = action;
        }

        @Override
        public Action log(String state) {
            LOGGER.info("User {}: action '{}' {}", userId, action, state);
            return this;
        }
    }

    public static Action action(String userId, String action, Object... fmtArgs) {
        return new ActionImpl(userId, String.format(action, fmtArgs));
    }

    public static String songInfo(String songId) {
        SongData data = Futures.getUnchecked(YoutubeAccess.INSTANCE.getVideoDataCached(songId));
        return String.format("%s[%s]", songId, data.getName());
    }

}
