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
import java.util.Map;

/**
 * @author Erich Eichinger
 * @since 01/03/2016
 */
public class UrlEngine implements Engine {
    @Override
    public ResponseEntity<String> submit(JCurlRequestOptions requestOptions) throws Exception {
        System.setProperty("http.keepAlive", "true");

        ResponseEntity<String> responseEntity = null;
        URL obj = new URL(requestOptions.getUrl());

        for (int i=0;i< requestOptions.getCount();i++) {
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // add request header
            con.setRequestMethod("GET");
            con.setConnectTimeout(2000);
            for(Map.Entry<String,String> e : requestOptions.getHeaderMap().entrySet()) {
                con.setRequestProperty(e.getKey(), e.getValue());
            }

            System.out.println("\nSending 'GET' request to URL : " + requestOptions.getUrl());
            con.connect();

            int responseCode = con.getResponseCode();
            System.out.println("Response Code : " + responseCode);

            final InputStream is = con.getInputStream();
            String response = IOUtils.toString(is);
            is.close();

            //print result
            System.out.println(response);

            responseEntity = new ResponseEntity<String>(response, HttpStatus.valueOf(responseCode));
        }
        return responseEntity;
    }

}
