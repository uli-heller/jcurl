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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runners.model.TestTimedOutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Erich Eichinger
 * @since 21/03/2016
 */
public class JCurlParallelTest {

    static final int CONTAINER_THREADS = 10;
    static final int JETTY_ACCEPTORS = 1;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
        .disableRequestJournal()
        .containerThreads(CONTAINER_THREADS + JETTY_ACCEPTORS + 1) // acceptors are taken from containerThread pool!
        .jettyAcceptors(JETTY_ACCEPTORS)
        .jettyAcceptQueueSize(0)
    );

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test(timeout = 1200)
    public void url_submit_requests_parallel() throws Exception {
        executeTest("url", CONTAINER_THREADS, CONTAINER_THREADS);
    }

    @Test
    public void url_too_many_requests_for_connectionpoolsize_just_blocks() throws Exception {
        executeTest("url", 5, 2);
    }

    @Test(timeout = 1200)
    public void hcnio_submit_requests_parallel() throws Exception {
        executeTest("hcnio", CONTAINER_THREADS, CONTAINER_THREADS);
    }

    @Test
    public void hcnio_too_many_requests_for_connectionpoolsize_throw_connectRequestTimeout() throws Exception {
        thrown.expect(MatcherUtils.hasRootCause(isA(TimeoutException.class)));

        executeTest("hcnio", 5, 2);
    }

    protected void executeTest(String engine, int count, int poolSize) throws Exception {

        stubFor(WireMock.get(WireMock.urlPathMatching("/some/url"))
                        .withHeader("accept", WireMock.equalTo("application/xml"))
                        .willReturn(aResponse()
                                .withFixedDelay(500)
                                .withStatus(200)
                                .withBody("" +
                                        "<payload>" +
                                        "    <value>Body Text</value>" +
                                        "</payload>"
                                )
                        ));

        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", ""+poolSize);

        ResponseEntity<String> response = new JCurl()
            .execute(
                "--engine", engine
                , "-H", "accept: application/xml"
                , "--verbose"
                , "--connectionRequestTimeout", "1000"
                , "--connectTimeout", "1500"
                , "--socketTimeout", "2000"
                , "--parallel", "true"
                , "--count", "" + count
                , "http://localhost:" + wireMockRule.port() + "/some/url");

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }


}
