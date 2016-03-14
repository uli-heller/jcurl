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

import java.util.Collections;
import java.util.Map;

import javax.net.ssl.SSLContext;

import lombok.NonNull;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

/**
 * @author Erich Eichinger
 * @since 01/03/2016
 */
public class HCNIOEngine implements Engine {
    @Override
    public ResponseEntity<String> submit(@NonNull String url, int count, @NonNull Map<String, String> headerMap) throws Exception {
        ResponseEntity<String> stringResponseEntity = null;
        try (CloseableHttpAsyncClient hc = createCloseableHttpAsyncClient()) {
            for (int i = 0; i < count; i++) {
                final HttpHeaders headers = new HttpHeaders();
                for (Map.Entry<String, String> e : headerMap.entrySet()) {
                    headers.put(e.getKey(), Collections.singletonList(e.getValue()));
                }

                final HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                AsyncRestTemplate template = new AsyncRestTemplate(new HttpComponentsAsyncClientHttpRequestFactory(hc));
                final ListenableFuture<ResponseEntity<String>> exchange = template.exchange(url, HttpMethod.GET, requestEntity, String.class);
                stringResponseEntity = exchange.get();
                System.out.println(stringResponseEntity.getBody());

            }
            return stringResponseEntity;
        }
    }

    private CloseableHttpAsyncClient createCloseableHttpAsyncClient() throws Exception {
        HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create();
        builder.useSystemProperties();
        builder.setSSLContext(SSLContext.getDefault());
        builder.setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE);
        builder.setMaxConnPerRoute(2);
        builder.setMaxConnTotal(2);
        builder.setDefaultRequestConfig(RequestConfig
            .custom()
            .setConnectionRequestTimeout(1000)
            .setConnectTimeout(2000)
            .setSocketTimeout(2000)
        .build()
        );
//        builder.setHttpProcessor()
        CloseableHttpAsyncClient hc = builder.build();
        hc.start();
        return hc;
    }

}
