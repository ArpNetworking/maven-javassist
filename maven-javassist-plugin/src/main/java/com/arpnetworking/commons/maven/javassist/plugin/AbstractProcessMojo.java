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
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Abstract mojo implementation for processing classes using Javassist.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
public abstract class AbstractProcessMojo extends AbstractMojo {

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            final Path outputDirectory = getOutputDirectory(project);
            final List<String> classpathElementsToLoad = getClasspathElementsToLoad(project);
            final ClassPool classPool = createClassPool(classpathElementsToLoad);
            final URLClassLoader pluginClassLoader = createUrlClassLoader(
                    classpathElementsToLoad,
                    originalContextClassLoader);
            Thread.currentThread().setContextClassLoader(pluginClassLoader);

            final ClassProcessor classProcessor = createProcessor(pluginClassLoader, processor);
            final Predicate<CtClass> includePredicate = createIncludePredicate(includes);
            final Predicate<CtClass> excludePredicate = createExcludePredicate(excludes);
            final ExecutorService executorService = createExecutorService(threads);

            final CompletableFuture<?>[] completableFutures = getClasspathElementsToProcess(project).stream()
                    .flatMap(classpathElement -> findClasses(classPool, classpathElement).stream())
                    .map(ctClass -> CompletableFuture.runAsync(
                            new ClassProcessorTask(
                                    buildContext,
                                    ctClass,
                                    classProcessor,
                                    includePredicate,
                                    excludePredicate,
                                    outputDirectory,
                                    getLog()),
                            executorService))
                    .toArray(CompletableFuture[]::new);

            try {
                CompletableFuture.allOf(completableFutures).get();
            } catch (final InterruptedException e) {
                throw new MojoExecutionException("Class processing interrupted", e);
            } catch (final CompletionException | ExecutionException e) {
                throw unwrapTaskException(e, e.getCause());
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    /**
     * Return the output directory for this goal.
     *
     * @param mavenProject the {@code MavenProject} instance.
     * @return Output directory.
     */
    protected abstract Path getOutputDirectory(final MavenProject mavenProject);

    /**
     * Return all the applicable class path elements to process for this goal.
     *
     * @param mavenProject the {@code MavenProject} instance.
     * @return {@code List} of class path elements.
     * @throws MojoExecutionException if classpath elements cannot be retrieved.
     */
    protected abstract List<String> getClasspathElementsToProcess(final MavenProject mavenProject) throws MojoExecutionException;

    /**
     * Return all the applicable class path elements to load for this goal.
     *
     * @param mavenProject the {@code MavenProject} instance.
     * @return {@code List} of class path elements.
     * @throws MojoExecutionException if classpath elements cannot be retrieved.
     */
    protected abstract List<String> getClasspathElementsToLoad(final MavenProject mavenProject) throws MojoExecutionException;

    /* package private */ URLClassLoader createUrlClassLoader(
            final List<String> classpathElements,
            final ClassLoader contextClassLoader)
            throws MojoExecutionException {
        try {
            final URL[] urls = new URL[classpathElements.size()];
            int i = 0;
            for (final String classpathElement : classpathElements) {
                urls[i++] = new File(classpathElement).toURI().toURL();
            }
            return URLClassLoader.newInstance(urls, contextClassLoader);
        } catch (final MalformedURLException e) {
            throw new MojoExecutionException("Unable to instantiate class loader", e);
        }
    }

    /* package private */ ClassPool createClassPool(final List<String> classpathElements) throws MojoExecutionException {
        final ClassPool classPool = new ClassPool(ClassPool.getDefault());
        for (final String classPathElement : classpathElements) {
            try {
                classPool.appendClassPath(classPathElement);
            } catch (final NotFoundException e) {
                throw new MojoExecutionException("Unable to add class path to pool: " + classPathElement, e);
            }
        }
        return classPool;
    }

    /* package private */ List<CtClass> findClasses(
            final ClassPool classPool,
            final String classPathElement) {

        // TODO(ville): If we ever support finding classes in other ways extract this method into a strategy.
        getLog().debug(String.format("Searching classpath element: %s", classPathElement));
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.{class}");
        final Path basePath = Paths.get(classPathElement);
        final List<CtClass> ctClasses = new ArrayList<>();

        try {
            Files.walkFileTree(
                    basePath,
                    new ClasspathSimpleFileVisitor(matcher, ctClasses, classPool, basePath));
        } catch (final IOException e) {
            throw new CompletionException("Unable to resolve compile classpath elements", e);
        }

        return ctClasses;
    }

    /* package private */ static ExecutorService createExecutorService(final String threads) {
        final int threadCount;
        if (threads.endsWith("C")) {
            threadCount = Integer.parseInt(threads.substring(0, threads.length() - 1))
                    * Runtime.getRuntime().availableProcessors();
        } else {
            threadCount = Integer.parseInt(threads);
        }
        final ClassLoader currentThreadContextClassloader = Thread.currentThread().getContextClassLoader();
        final AtomicInteger threadIndex = new AtomicInteger(1);
        return Executors.newFixedThreadPool(
                threadCount,
                r -> {
                    final Thread thread = new Thread(r);
                    thread.setName("javassist-processor-" + threadIndex.getAndIncrement());
                    thread.setContextClassLoader(currentThreadContextClassloader);
                    return thread;
                });
    }

    /* package private */ static ClassProcessor createProcessor(
            final ClassLoader classLoader,
            final String processorClassName)
            throws MojoExecutionException {
        try {
            final Object classProcessor = classLoader.loadClass(processorClassName).newInstance();
            if (classProcessor instanceof ClassProcessor) {
                return (ClassProcessor) classProcessor;
            }
            throw new MojoExecutionException("Invalid class processor: " + processorClassName);
        } catch (final ClassNotFoundException e) {
            throw new MojoExecutionException("Class processor not found: " + processorClassName, e);
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (final Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            throw new MojoExecutionException("Unable to instantiate processor: " + processorClassName, e);
        }
    }

    /* package private */ static MojoExecutionException unwrapTaskException(
            final Exception exception,
            @Nullable final Throwable cause) {
        if (cause instanceof CompletionException || cause instanceof ExecutionException) {
            return unwrapTaskException((Exception) cause, cause.getCause());
        } else if (cause != null) {
            return new MojoExecutionException("Class processing failed", cause);
        } else {
            return new MojoExecutionException("Unknown class processing exception", exception);
        }
    }

    /* package private */ static Predicate<CtClass> createIncludePredicate(@Nullable final String[] classes) {
        final Set<PathMatcher> includeSet;
        if (classes != null && classes.length != 0) {
            includeSet = createPathMatcherSet(classes);
        } else {
            return ctClass -> true;
        }
        return createMatchingPredicate(includeSet);
    }

    /* package private */ static Predicate<CtClass> createExcludePredicate(@Nullable final String[] classes) {
        final Set<PathMatcher> excludeSet;
        if (classes != null && classes.length != 0) {
            excludeSet = createPathMatcherSet(classes);
        } else {
            return ctClass -> false;
        }
        return createMatchingPredicate(excludeSet);
    }

    /* package private */ static Predicate<CtClass> createMatchingPredicate(final Set<PathMatcher> pathMatchers) {
        return ctClass -> {
            for (final PathMatcher pathMatcher : pathMatchers) {
                if (pathMatcher.matches(Paths.get(ctClass.getName()))) {
                    return true;
                }
            }
            return false;
        };
    }

    /* package private */ static Set<PathMatcher> createPathMatcherSet(final String[] targets) {
        return Arrays.stream(targets)
                .map(inclusion -> FileSystems.getDefault().getPathMatcher("glob:" + inclusion))
                .collect(Collectors.toSet());
    }

    // CHECKSTYLE.OFF: MemberName - Member names are mapped to plugin configuration.

    @Component
    private BuildContext buildContext;

    // Implementation of {@code com.arpnetworking.commons.maven.javassist.ClassProcessor}
    @Parameter(property = "processor", required = true)
    private String processor;

    // List of {@code Class} names to be included; by default all classes
    // within the goal scope are included. Use this property to select the
    // desired classes.
    @Parameter(property = "includes")
    private String[] includes;

    // List of {@code Class} names to be excluded. Use this property to narrow
    // either all the classes in the goal scope or the included classes.
    @Parameter(property = "excludes")
    private String[] excludes;

    // The number of threads to use for class processing. Suffix the number
    // with "C" to indicate the number of threads per core.
    @Parameter(property = "threads", defaultValue = "1")
    private String threads;

    // The {@code MavenProject} being built.
    @Parameter(property = "project", defaultValue = "${project}", readonly =  true)
    private MavenProject project;

    // CHECKSTYLE.ON: MemberName

    private static class ClasspathSimpleFileVisitor extends SimpleFileVisitor<Path> {

        ClasspathSimpleFileVisitor(
                final PathMatcher matcher,
                final List<CtClass> ctClasses,
                final ClassPool classPool,
                final Path basePath) {
            _matcher = matcher;
            _ctClasses = ctClasses;
            _classPool = classPool;
            _basePath = basePath;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            if (_matcher.matches(file)) {
                final String relativeFileName = _basePath.relativize(file).toString();
                final String className = relativeFileName.substring(0, relativeFileName.lastIndexOf("."))
                        .replace(File.separator, ".");
                try {
                    _ctClasses.add(_classPool.get(className));
                } catch (final NotFoundException e) {
                    throw new IOException("Unable to load class: " + file + " (" + className + ")", e);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        private final PathMatcher _matcher;
        private final List<CtClass> _ctClasses;
        private final ClassPool _classPool;
        private final Path _basePath;
    }
}
