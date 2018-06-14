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
package me.kenzierocks.ourtube.rpc;

import com.google.auto.value.AutoValue;

public interface RpcEventHandler<ARGS> {

    public static <ARGS> Typed<ARGS> typed(Class<ARGS> argType, RpcEventHandler<ARGS> handler) {
        return new AutoValue_RpcEventHandler_Typed<>(argType, handler);
    }

    @AutoValue
    abstract class Typed<ARGS> implements RpcEventHandler<ARGS> {
        /*
         * Why is there a pointless implementation when this information could
         * be extracted from the ARGS typing in the eventual implementation?
         * 
         * Apparently lambdas do not hold their generic information. So, I
         * resorted to this to maintain the lambda facade. This carries the
         * information so that there's no need to extract it, at the cost of
         * simplicity. It's still more readable than requiring a full class
         * preamble.
         */

        public abstract Class<ARGS> argsClass();

        abstract RpcEventHandler<ARGS> delegate();

        @Override
        public void handleEvent(RpcClient client, ARGS args) {
            delegate().handleEvent(client, args);
        }
    }

    void handleEvent(RpcClient client, ARGS args);

}
