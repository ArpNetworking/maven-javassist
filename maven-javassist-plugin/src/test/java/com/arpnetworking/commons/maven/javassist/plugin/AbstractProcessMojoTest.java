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
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;

/**
 * Tests for {@link AbstractProcessMojo}.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
public final class AbstractProcessMojoTest {

    @Test
    public void testCreateExecutorServiceSingleThread() {
        final ExecutorService executorService = AbstractProcessMojo.createExecutorService("1");
        Assert.assertTrue(executorService instanceof ThreadPoolExecutor);
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        Assert.assertEquals(1, threadPoolExecutor.getCorePoolSize());
        Assert.assertEquals(1, threadPoolExecutor.getMaximumPoolSize());
    }

    @Test
    public void testCreateExecutorServiceMultiThread() {
        final ExecutorService executorService = AbstractProcessMojo.createExecutorService("4");
        Assert.assertTrue(executorService instanceof ThreadPoolExecutor);
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        Assert.assertEquals(4, threadPoolExecutor.getCorePoolSize());
        Assert.assertEquals(4, threadPoolExecutor.getMaximumPoolSize());
    }

    @Test
    public void testCreateExecutorServicePerCoreThread() {
        final int cores = Runtime.getRuntime().availableProcessors();
        final ExecutorService executorService = AbstractProcessMojo.createExecutorService("2C");
        Assert.assertTrue(executorService instanceof ThreadPoolExecutor);
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        Assert.assertEquals(2 * cores, threadPoolExecutor.getCorePoolSize());
        Assert.assertEquals(2 * cores, threadPoolExecutor.getMaximumPoolSize());
    }

    @Test(expected = NumberFormatException.class)
    public void testCreateExecutorServiceInvalid() {
        AbstractProcessMojo.createExecutorService("Foo");
    }

    @Test(expected = NumberFormatException.class)
    public void testCreateExecutorServiceInvalidPerCore() {
        AbstractProcessMojo.createExecutorService("FooC");
    }

    @Test
    public void testCreateProcessor() throws MojoExecutionException {
        final ClassProcessor processor = AbstractProcessMojo.createProcessor(
                Thread.currentThread().getContextClassLoader(),
                "com.arpnetworking.commons.maven.javassist.plugin.TestProcessor");
        Assert.assertTrue(processor instanceof TestProcessor);
    }

    @Test(expected = MojoExecutionException.class)
    public void testCreateProcessorNonProcessor() throws MojoExecutionException {
        AbstractProcessMojo.createProcessor(
                Thread.currentThread().getContextClassLoader(),
                "java.lang.String");
    }

    @Test(expected = MojoExecutionException.class)
    public void testCreateProcessorNotFound() throws MojoExecutionException {
        AbstractProcessMojo.createProcessor(
                Thread.currentThread().getContextClassLoader(),
                "com.example.NotFound");
    }

    @Test(expected = MojoExecutionException.class)
    public void testCreateProcessorConstructionFailure() throws MojoExecutionException {
        AbstractProcessMojo.createProcessor(
                Thread.currentThread().getContextClassLoader(),
                "com.arpnetworking.commons.maven.javassist.plugin.AbstractProcessMojoTest$BrokenProcessor");
    }

    @Test
    public void testUnwrapTaskExceptionNoOp() {
        final NullPointerException exception = new NullPointerException("Exception");
        final MojoExecutionException mee = AbstractProcessMojo.unwrapTaskException(exception, null);
        Assert.assertSame(exception, mee.getCause());
    }

    @Test
    public void testUnwrapTaskExceptionCause() {
        final NullPointerException cause = new NullPointerException("Cause");
        final NullPointerException exception = new NullPointerException("Exception");
        final MojoExecutionException mee = AbstractProcessMojo.unwrapTaskException(exception, cause);
        Assert.assertSame(cause, mee.getCause());
    }

    @Test
    public void testUnwrapTaskExceptionUnwrapCompletionException() {
        final NullPointerException wrappedCause = new NullPointerException("WrappedCause");
        final CompletionException cause = new CompletionException("Cause", wrappedCause);
        final Exception exception = new Exception("Exception", cause);
        final MojoExecutionException mee = AbstractProcessMojo.unwrapTaskException(exception, cause);
        Assert.assertSame(wrappedCause, mee.getCause());
    }

    @Test
    public void testUnwrapTaskExceptionUnwrapExecutionException() {
        final NullPointerException wrappedCause = new NullPointerException("WrappedCause");
        final ExecutionException cause = new ExecutionException("Cause", wrappedCause);
        final Exception exception = new Exception("Exception", cause);
        final MojoExecutionException mee = AbstractProcessMojo.unwrapTaskException(exception, cause);
        Assert.assertSame(wrappedCause, mee.getCause());
    }

    @Test
    public void testCreateIncludePredicateNullClasses() {
        final Predicate<CtClass> predicate = AbstractProcessMojo.createIncludePredicate(null);
        Assert.assertTrue(predicate.test(null));
    }

    @Test
    public void testCreateIncludePredicateEmptyClasses() {
        final Predicate<CtClass> predicate = AbstractProcessMojo.createIncludePredicate(new String[0]);
        Assert.assertTrue(predicate.test(null));
    }

    @Test
    public void testCreateIncludePredicate() {
        final CtClass ctClass = Mockito.mock(CtClass.class);
        final Predicate<CtClass> predicate = AbstractProcessMojo.createIncludePredicate(new String[]{"Foo*"});

        Mockito.doReturn("FooWidget").when(ctClass).getName();
        Assert.assertTrue(predicate.test(ctClass));

        Mockito.doReturn("BarWidget").when(ctClass).getName();
        Assert.assertFalse(predicate.test(ctClass));
    }

    @Test
    public void testCreateExcludePredicateNullClasses() {
        final Predicate<CtClass> predicate = AbstractProcessMojo.createExcludePredicate(null);
        Assert.assertFalse(predicate.test(null));
    }

    @Test
    public void testCreateExcludePredicateEmptyClasses() {
        final Predicate<CtClass> predicate = AbstractProcessMojo.createExcludePredicate(new String[0]);
        Assert.assertFalse(predicate.test(null));
    }

    @Test
    public void testCreateExcludePredicate() {
        final CtClass ctClass = Mockito.mock(CtClass.class);
        final Predicate<CtClass> predicate = AbstractProcessMojo.createExcludePredicate(new String[]{"Foo*"});

        Mockito.doReturn("FooWidget").when(ctClass).getName();
        Assert.assertTrue(predicate.test(ctClass));

        Mockito.doReturn("BarWidget").when(ctClass).getName();
        Assert.assertFalse(predicate.test(ctClass));
    }

    private static final class BrokenProcessor implements ClassProcessor {

        BrokenProcessor() {
            throw new IllegalStateException("This processor is always in an illegal state.");
        }

        @Override
        public boolean accept(final CtClass ctClass) {
            return false;
        }

        @Override
        public void process(final CtClass ctClass) {
            // Nothing to do
        }
    }
}
