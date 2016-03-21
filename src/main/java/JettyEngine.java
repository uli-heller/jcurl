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

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.net.ssl.SSLContext;
import java.util.Map;

/**
 * @author Erich Eichinger
 * @since 01/03/2016
 */
public class JettyEngine implements Engine {
    @Override
    public ResponseEntity<String> submit(JCurlRequestOptions requestOptions) throws Exception {

        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setSslContext(SSLContext.getDefault());
        HttpClient httpClient = new HttpClient(sslContextFactory);
        // Configure HttpClient here
        httpClient.start();

        ResponseEntity<String> responseEntity = null;

        for (int i=0;i< requestOptions.getCount();i++) {
            final Request request = httpClient
                .newRequest(requestOptions.getUrl());

            for(Map.Entry<String,String> e : requestOptions.getHeaderMap().entrySet()) {
                request.header(e.getKey(), e.getValue());
            }

            System.out.println("\nSending 'GET' request to URL : " + requestOptions.getUrl());
            final ContentResponse response = request.send();

            int responseCode = response.getStatus();
            System.out.println("Response Code : " + responseCode);

            String responseContent = IOUtils.toString(response.getContent(), "utf-8");

            //print result
            System.out.println(responseContent);

            responseEntity = new ResponseEntity<String>(responseContent, HttpStatus.valueOf(responseCode));
        }

        httpClient.stop();
        return responseEntity;
    }

}
