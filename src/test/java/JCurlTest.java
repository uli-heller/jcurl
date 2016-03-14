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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * @author Erich Eichinger
 * @since 25/02/2016
 */
public class JCurlTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void printsHelp() throws Exception {
        new JCurl().execute();
    }

    @Test
    public void request_using_url() throws Exception {
        executeTest("url");
    }

    @Test
    public void request_using_httpcomponents() throws Exception {
        executeTest("hc");
    }

    @Test
    public void request_using_httpcomponents_nio() throws Exception {
        executeTest("hcnio");
    }

    @Test
    public void request_using_netty_nio() throws Exception {
        executeTest("nnio");
    }

    @Test
    public void request_using_okhttp() throws Exception {
        executeTest("okhttp");
    }

    @Test
    public void request_using_jetty() throws Exception {
        executeTest("jetty");
    }

    protected void executeTest(String engine) throws Exception {

        stubFor(WireMock.get(WireMock.urlPathMatching("/some/url"))
                        .withHeader("accept", WireMock.equalTo("application/xml"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("" +
                                        "<payload>" +
                                        "    <value>Body Text</value>" +
                                        "</payload>"
                                )
                        ));

        ResponseEntity<String> response = new JCurl()
            .execute(
                "--engine", engine
                , "-H", "accept: application/xml"
                , "--verbose"
                , "--count", "3"
                , "http://localhost:" + wireMockRule.port() + "/some/url");

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }
}
