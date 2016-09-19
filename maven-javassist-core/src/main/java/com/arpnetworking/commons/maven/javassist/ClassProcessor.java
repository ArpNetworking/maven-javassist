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
package com.arpnetworking.commons.maven.javassist;

import javassist.CtClass;

/**
 * Interface for performing post-processing of classes using the Maven
 * Javassist Plugin.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
public interface ClassProcessor {

    /**
     * Determines whether the provided {@code CtClass} should be processed.
     * Simply return {@code true} if all user specified classes can be
     * processed. Alternatively, inspect the provided {@code CtClass} and return
     * {@code true} if and only if it can be processed. Only the set of classes
     * selected by the user will be evaluated.
     *
     * @param ctClass the {@code CtClass} to be analyzed for possible processing.
     * @return True if and only if the provided {@code CtClass} should be processed.
     */
    boolean accept(CtClass ctClass);

    /**
     * Process the specified {@code CtClass}. If the user selected the
     * {@code CtClass} and the {@code accept} method returned {@code true}
     * then the {@code process} method will be invoked for that {@code Class}.
     *
     * @param ctClass the {@code CtClass} to be processed.
     */
    void process(CtClass ctClass);
}
