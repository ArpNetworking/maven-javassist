/**
 * Copyright 2016 Inscope Metrics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.commons.maven.javassist.plugin;

import com.arpnetworking.commons.maven.javassist.ClassProcessor;
import javassist.CtClass;

/**
 * {@link ClassProcessor} test implementation.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
public final class TestProcessor implements ClassProcessor {

    @Override
    public boolean accept(final CtClass ctClass) {
        return false;
    }

    @Override
    public void process(final CtClass ctClass) {
        // Nothing to do
    }
}
