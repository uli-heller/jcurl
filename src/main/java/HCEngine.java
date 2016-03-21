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

import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author Erich Eichinger
 * @since 01/03/2016
 */
public class HCEngine implements Engine {
    @Override
    public ResponseEntity<String> submit(JCurlRequestOptions requestOptions) throws Exception {
        ResponseEntity<String> stringResponseEntity = null;
        try (CloseableHttpClient hc = createCloseableHttpClient()) {
            for (int i = 0; i < requestOptions.getCount(); i++) {
                final HttpHeaders headers = new HttpHeaders();
                for(Map.Entry<String,String> e : requestOptions.getHeaderMap().entrySet()) {
                    headers.put(e.getKey(), Collections.singletonList(e.getValue()));
                }

                final HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

                RestTemplate template = new RestTemplate(new HttpComponentsClientHttpRequestFactory(hc));

                stringResponseEntity = template.exchange(requestOptions.getUrl(), HttpMethod.GET, requestEntity, String.class);
                System.out.println(stringResponseEntity.getBody());

            }
            return stringResponseEntity;
        }
    }

    private CloseableHttpClient createCloseableHttpClient() throws Exception {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.useSystemProperties();
        builder.setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);
        builder.setSSLContext(SSLContext.getDefault());
        CloseableHttpClient hc = builder.build();
        return hc;
    }

}
