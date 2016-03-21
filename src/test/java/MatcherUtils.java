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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.ThrowableCauseMatcher;

/**
 * @author Erich Eichinger
 * @since 21/03/2016
 */
public class MatcherUtils {

    @Factory
    public static <T extends Throwable> Matcher<T> hasRootCause(final Matcher<? extends Throwable> matcher) {
        return new ThrowableCauseMatcher<T>(matcher) {
            @Override
            protected boolean matchesSafely(T item) {
                return matcher.matches(ExceptionUtils.getRootCause(item));
            }
        };
    }
}
