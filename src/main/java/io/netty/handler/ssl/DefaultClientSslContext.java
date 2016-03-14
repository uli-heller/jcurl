
/*
 *  Copyright 2015-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.netty.handler.ssl;

import lombok.SneakyThrows;

import javax.net.ssl.SSLContext;

/**
 * @author Erich Eichinger
 * @since 01/03/2016
 */
public class DefaultClientSslContext extends JdkSslContext {

    SSLContext ctx;

    @SneakyThrows
    public DefaultClientSslContext() {
        super(null, IdentityCipherSuiteFilter.INSTANCE, toNegotiator(null, false), ClientAuth.NONE);
        ctx = SSLContext.getDefault();
    }

    @Override
    public SSLContext context() {
        return ctx;
    }

    @Override
    public boolean isClient() {
        return true;
    }
}
