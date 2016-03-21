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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Erich Eichinger
 * @since 01/03/2016
 */
public class UrlEngine implements Engine {
    @Override
    public ResponseEntity<String> submit(JCurlRequestOptions requestOptions) throws Exception {
        System.setProperty("http.keepAlive", "true");

        URL obj = new URL(requestOptions.getUrl());

        int threadCount = requestOptions.isParallel() ? Integer.valueOf(System.getProperty("http.maxConnections", "5")) : 1;
        final ExecutorService scheduler = Executors.newFixedThreadPool(threadCount);

        List<Future<ResponseEntity<String>>> responseFutures = new ArrayList<>();

        for (int i=0;i< requestOptions.getCount();i++) {
            final int nr = i;
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // add request header
            con.setRequestMethod("GET");
            con.setConnectTimeout(requestOptions.getConnectTimeout());
            con.setReadTimeout(requestOptions.getSocketTimeout());

            for(Map.Entry<String,String> e : requestOptions.getHeaderMap().entrySet()) {
                con.setRequestProperty(e.getKey(), e.getValue());
            }

            responseFutures.add(scheduler.submit(() -> {
                System.out.println("\nSending 'GET' request to URL : " + requestOptions.getUrl());
                try {
                    con.connect();

                    int responseCode = con.getResponseCode();
                    System.out.println("received response: " +  nr + " - Status " + responseCode);

                    final InputStream is = (responseCode < 400) ? con.getInputStream() : con.getErrorStream();
                    String response = IOUtils.toString(is);
                    is.close();

                    return new ResponseEntity<String>(response, HttpStatus.valueOf(responseCode));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }));
        }

        int errors = 0;
        Exception ex = null;
        for(int i=0;i<responseFutures.size();i++) {
            System.out.println("awaiting response " + i);
            try {
                responseFutures.get(i).get();
            } catch(Exception e) {
                if (ex == null) ex =e;
                errors++;
            }
        }

        System.out.println("submitted requests: " + responseFutures.size() + ", errors: " + errors);

        scheduler.shutdown();

        if (ex != null) {
            throw ex;
        }

        return responseFutures.get(0).get();
    }
}
