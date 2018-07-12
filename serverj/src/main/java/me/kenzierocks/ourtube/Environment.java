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

import static com.google.common.base.Preconditions.checkState;

import com.techshroom.jungle.EnvConfigOption;
import com.techshroom.jungle.Loaders;

// worlds worst env class
public class Environment {

    public static final EnvConfigOption<String> YOUTUBE_API_KEY =
            EnvConfigOption.create("YOUTUBE_API_KEY", Loaders.forString(), "");
    public static final EnvConfigOption<String> DISCORD_TOKEN =
            EnvConfigOption.create("DISCORD_TOKEN", Loaders.forString(), "");
    public static final EnvConfigOption<Boolean> INTERNAL_STREAMS_ERROR_OUTPUT =
            EnvConfigOption.create("INTERNAL_STREAMS_ERROR_OUTPUT", Loaders.forBoolean(), Boolean.FALSE);
    static {
        checkState(YOUTUBE_API_KEY.get().length() > 0, "No YOUTUBE_API_KEY provided.");
        checkState(DISCORD_TOKEN.get().length() > 0, "No DISCORD_TOKEN provided.");
    }

}
