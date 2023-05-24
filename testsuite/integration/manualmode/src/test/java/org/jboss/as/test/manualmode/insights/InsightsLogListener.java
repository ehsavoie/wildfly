/*
 * Copyright 2023 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.manualmode.insights;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class InsightsLogListener extends TailerListenerAdapter {
    private final List<Pattern> patterns;
    private final List<String> matchedMessages = new ArrayList<>();

    public InsightsLogListener(Pattern... patterns) {
        this.patterns = Arrays.asList(patterns);
    }

    public void handle(String line) {
        if (patterns.stream().filter(pattern -> pattern.matcher(line).matches()).findAny().isPresent()) {
            matchedMessages.add(line);
        }
    }

    public List<String> getMatchedLines() {
        return matchedMessages;
    }

    public boolean foundMatchForPattern(Pattern pattern) {
        return matchedMessages.stream().filter(message -> pattern.matcher(message).matches()).findAny().isPresent();
    }

    public boolean allPatternsMatched() {
        for (Pattern p : patterns) {
            if (!foundMatchForPattern(p)) {
                return false;
            }
        }
        return true;
    }

    public void assertPatternMatched(Pattern pattern) {
        assertTrue("There should be a match for pattern " + pattern.toString(), foundMatchForPattern(pattern));
    }

    // TODO do this better
    public void assertAllPatternsMatched() {
        boolean allPatternsMatched = true;
        for (Pattern p : patterns) {
            if (!foundMatchForPattern(p)) {
                Assert.fail("No match found for pattern: " + p.toString());
            }
        }
        assertTrue(allPatternsMatched);
    }
}
