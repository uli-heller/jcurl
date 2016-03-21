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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.protocol.HttpAsyncClientExchangeHandler;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Erich Eichinger
 * @since 01/03/2016
 */
public class HCNIOEngine implements Engine {
    @Override
    public ResponseEntity<String> submit(JCurlRequestOptions requestOptions) throws Exception {
        final List<Future<HttpResponse>> responseFutures = new ArrayList<>();

        ResponseEntity<String> stringResponseEntity = null;
        try (CloseableHttpAsyncClient hc = createCloseableHttpAsyncClient(requestOptions)) {
            final List<HttpUriRequest> requests = new ArrayList<>();

            for (int i = 0; i < requestOptions.getCount(); i++) {
                HttpUriRequest httpUriRequest = new HttpGet(requestOptions.getUrl());
                for (Map.Entry<String, String> e : requestOptions.getHeaderMap().entrySet()) {
                    httpUriRequest.addHeader(e.getKey(), e.getValue());
                }
                requests.add(httpUriRequest);
            }

            for (int i = 0; i < requests.size(); i++) {
                final int nr = i;

                HttpClientContext ctx = HttpClientContext.create();
                ctx.setAttribute("requestnr", ""+nr);
                HttpUriRequest request = requests.get(i);
                System.out.println("submit request " + i);

                final Future<HttpResponse> responseFuture = hc.execute(request, ctx, new FutureCallback<HttpResponse>() {

                    @Override
                    public void completed(HttpResponse result) {
                        System.out.println("request " + nr + " complete");
                    }

                    @Override
                    public void failed(Exception ex) {
                        System.out.println("request " + nr + " failed:" + ex.getMessage());
                    }

                    @Override
                    public void cancelled() {
                        System.out.println("request " + nr + " cancelled");
                    }
                });
                if (!requestOptions.isParallel()) {
                    System.out.println("awaiting sync response " + i);
                    stringResponseEntity = extractResponseEntity(i, responseFuture);
                } else {
                    responseFutures.add(responseFuture);
                }
            }

            int errors = 0;
            Exception firstException = null;
            if (requestOptions.isParallel()) {
                for (int i = 0; i < responseFutures.size(); i++) {
                    System.out.println("awaiting parallel response " + i);
                    try {
                        Future<HttpResponse> responseFuture = responseFutures.get(i);
                        stringResponseEntity = extractResponseEntity(i, responseFuture);
                    } catch (Exception e) {
                        if (firstException == null) {
                            firstException = e;
                        }
                        errors++;
                    }
                }
            }

            System.out.println("submitted requests: " + responseFutures.size() + ", errors: " + errors);

            if (firstException != null) {
                throw firstException;
            }

            return stringResponseEntity;
        }
    }

    private ResponseEntity<String> extractResponseEntity(int responseNr, Future<HttpResponse> responseFuture) throws InterruptedException, java.util.concurrent.ExecutionException, IOException {
        try {
            final HttpResponse response = responseFuture.get();
            String responseBody = IOUtils.toString(response.getEntity().getContent());
            return new ResponseEntity<String>(responseBody, HttpStatus.valueOf(response.getStatusLine().getStatusCode()));
        } catch (Exception e) {
            throw new RuntimeException("exception awaiting response " + responseNr, e);
        }
    }

    private CloseableHttpAsyncClient createCloseableHttpAsyncClient(JCurlRequestOptions requestOptions) throws Exception {
        HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create();
        builder.useSystemProperties();
        builder.setSSLContext(SSLContext.getDefault());
        builder.setDefaultIOReactorConfig(IOReactorConfig
            .custom()
            .setConnectTimeout(requestOptions.getConnectTimeout())
            .setSoTimeout(requestOptions.getSocketTimeout())
            .build()
        );
        builder.setDefaultRequestConfig(RequestConfig
                .custom()
                .setConnectionRequestTimeout(requestOptions.getConnectionRequestTimeout())
                .setConnectTimeout(requestOptions.getConnectTimeout())
                .setSocketTimeout(requestOptions.getSocketTimeout())
                .build()
        );
        builder.setEventHandler(new HttpAsyncRequestExecutor() {
            @Override
            public void requestReady(NHttpClientConnection conn) throws IOException, HttpException {
                super.requestReady(conn);
                HttpAsyncClientExchangeHandler exh = (HttpAsyncClientExchangeHandler) conn.getContext().getAttribute(HttpAsyncRequestExecutor.HTTP_HANDLER);
                System.out.println("request submitted " + conn.getContext().getAttribute("requestnr"));
            }

            @Override
            public void outputReady(NHttpClientConnection conn, ContentEncoder encoder) throws IOException, HttpException {
                super.outputReady(conn, encoder);
                System.out.println("output ready " + conn.getContext().getAttribute("requestnr"));
            }

            @Override
            public void responseReceived(NHttpClientConnection conn) throws HttpException, IOException {
                super.responseReceived(conn);
                System.out.println("response received " + conn.getContext().getAttribute("requestnr"));
            }
        });
        CloseableHttpAsyncClient hc = builder.build();
        hc.start();
        return hc;
    }

}
