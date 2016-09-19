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

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Mojo implementation for processing test classes using Javassist.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
@Mojo(name = "test-process", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.TEST)
public class ProcessTestMojo extends AbstractProcessMojo {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Path getOutputDirectory(final MavenProject project) {
        return Paths.get(project.getBuild().getTestOutputDirectory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getClasspathElementsToProcess(final MavenProject project) throws MojoExecutionException {
        return Collections.singletonList(project.getBuild().getTestOutputDirectory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getClasspathElementsToLoad(final MavenProject project) throws MojoExecutionException {
        try {
            return ImmutableList.copyOf(project.getTestClasspathElements());
        } catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Unable to load test class path elements", e);
        }
    }
}
