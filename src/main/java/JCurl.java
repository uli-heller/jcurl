
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

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.LogManager;

import static java.util.Arrays.asList;

public class JCurl {

    private static final String USER_AGENT = "Mozilla/5.0; jcurl";

    enum EngineType {
        URL(new UrlEngine()),
        HC(new HCEngine()),
        HCNIO(new HCNIOEngine()),
        NNIO(new NNIOEngine()),
        OKHTTP(new OkHttpEngine()),
        JETTY(new JettyEngine());

        private final Engine engine;

        public Engine getEngine() {
            return engine;
        }

        EngineType(@NonNull Engine engine) {
            this.engine = engine;
        }
    }

    public static void main(String[] args) throws Exception {
        new JCurl().execute(args);
    }

    public static class JCurlLogConfig {}

    public ResponseEntity<String> execute(String... args) throws Exception {
        final LoggerContext context = initLogging();

        System.out.println("Starting jCurl in " + System.getProperty("user.dir"));


        OptionParser parser = new OptionParser();
        parser.acceptsAll( asList("help", "h"), "print help" );
        OptionSpec<String> headerSpec =
            parser.acceptsAll(asList("header", "H"), "header syntax equivalent to curl, e.g. '-H \"Accept: application/json\"'").withRequiredArg().ofType(String.class);
        parser.acceptsAll(asList("verbose", "v"), "activate verbose logging");
//        parser.accepts( "level" ).withOptionalArg();
        parser.acceptsAll( asList("count", "c"), "repeat call x times" ).withRequiredArg().ofType(Integer.class);
        OptionSpec<String> engineOptionSpec = parser.acceptsAll(asList("engine", "e"), "which engine to use ('url': java.net.URL, 'hcnio': Apache AsyncHttpClient), default is 'url'").withRequiredArg().ofType(String.class);

        OptionSet options = parser.parse(args);

        if (args.length == 0 || options.has("help")) {
            printUsage(parser);
            return null;
        }

        EngineType engineType = EngineType.URL;
        if (options.has("engine")) {
            engineType = EngineType.valueOf(options.valueOf(engineOptionSpec).toUpperCase());
        }

        if (options.has("verbose")) {
            LogManager.getLogManager().getLogger("").setLevel(java.util.logging.Level.ALL);
            context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.ALL);
        }

        int count = 1;
        if (options.has("count")) {
            count = Integer.valueOf("" + options.valueOf("count"));
        }

        final List<?> nonOpts = options.nonOptionArguments();
        if (nonOpts.size() != 1) {
            System.err.println("missing <url> argument");
            printUsage(parser);
        }
        String url = String.valueOf(nonOpts.get(0));

        Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", "*/*");

        if (options.has("header")) {
            for(String header : options.valuesOf(headerSpec)) {
                String[] vals = header.split(":");
                if (vals.length == 1) {
                    headers.put(vals[0].trim(), null);
                } else {
                    headers.put(vals[0].trim(), vals[1].trim());
                }
            }
        }

        return engineType.getEngine().submit(url, count, headers);
    }

    protected void printUsage(OptionParser parser) throws IOException {
        System.out.println("Usage: jcurl [options...] <url>");
        System.out.println();
        System.out.println("Note: options may be used in their long form with 2 hyphens or abbreviated using 1 hyphen, e.g. '--count 3' and '-c 3' are equivalent");
        System.out.println();
        parser.printHelpOn(System.out);
    }

    protected LoggerContext initLogging() throws IOException {
        LogManager.getLogManager().reset();
        LogManager.getLogManager().readConfiguration();
        LogManager.getLogManager().getLogger("").setLevel(java.util.logging.Level.INFO);

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        BasicConfigurator configurator = new BasicConfigurator();
        configurator.setContext(context);
        configurator.configure(context);
        context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
        org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
        org.slf4j.bridge.SLF4JBridgeHandler.install();
        return context;
    }
}
