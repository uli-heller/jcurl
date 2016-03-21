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

import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Erich Eichinger
 * @since 01/03/2016
 */
public class OkHttpEngine implements Engine {
    @Override
    public ResponseEntity<String> submit(JCurlRequestOptions requestOptions) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(2000, TimeUnit.MILLISECONDS)
            .sslSocketFactory(SSLContext.getDefault().getSocketFactory())
            .followSslRedirects(true)
            .build();

        ResponseEntity<String> responseEntity = null;

        for (int i=0;i< requestOptions.getCount();i++) {
            final Request.Builder requestBuilder = new Request.Builder()
                .url(requestOptions.getUrl())
                .headers(Headers.of(requestOptions.getHeaderMap()))
                .get();
            Request request = requestBuilder.build();

            System.out.println("\nSending 'GET' request to URL : " + requestOptions.getUrl());
            Response response = client.newCall(request).execute();

            int responseCode = response.code();
            System.out.println("Response Code : " + responseCode);

            final InputStream is = response.body().byteStream();
            String responseContent = IOUtils.toString(is);
            is.close();

            //print result
            System.out.println(responseContent);

            responseEntity = new ResponseEntity<String>(responseContent, HttpStatus.valueOf(responseCode));
        }
        return responseEntity;
    }

}
