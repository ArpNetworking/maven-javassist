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
import com.arpnetworking.commons.maven.javassist.Processed;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

/**
 * Tests for {@link ClassProcessorTask}.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
public final class ClassProcessorTaskTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRun() throws NotFoundException, IOException, CannotCompileException {
        final Path outputDirectory = Paths.get("./target/test-data");

        final TestProcessor testProcessor = new TestProcessor(true);

        final ClassPool classPool = createClassPool();
        final CtClass testRunCtClass = classPool.get(
                "com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$TestRunClass");

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                testRunCtClass,
                testProcessor,
                _includePredicate,
                _excludePredicate,
                outputDirectory,
                _log);

        Mockito.doReturn(true).when(_includePredicate).test(testRunCtClass);
        Mockito.doReturn(false).when(_excludePredicate).test(testRunCtClass);
        Mockito.doAnswer(invocationOnMock -> new FileOutputStream((File) invocationOnMock.getArguments()[0]))
                .when(_context).newFileOutputStream(Mockito.any(File.class));

        classProcessorTask.run();

        Assert.assertEquals(1, testProcessor.getAcceptCount());
        Assert.assertEquals(1, testProcessor.getProcessCount());
        Mockito.verify(_context).refresh(Mockito.any(File.class));

        Assert.assertTrue(testRunCtClass.isFrozen());
        testRunCtClass.defrost();
        Assert.assertTrue(classProcessorTask.isAlreadyProcessed(testRunCtClass, testProcessor));

        final Path outputFile = outputDirectory.resolve(
                "com/arpnetworking/commons/maven/javassist/plugin/ClassProcessorTaskTest$TestRunClass.class");
        Assert.assertTrue(Files.exists(outputFile));
        final byte[] actualByteCode = Files.readAllBytes(outputFile);
        final byte[] expectedByteCode = testRunCtClass.toBytecode();
        Assert.assertArrayEquals(expectedByteCode, actualByteCode);
    }

    @Test
    public void testRunNotAccepted() throws NotFoundException, IOException, CannotCompileException {
        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                _ctClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);

        Mockito.doReturn(false).when(_includePredicate).test(_ctClass);

        classProcessorTask.run();

        Mockito.verify(_processor, Mockito.never()).process(Mockito.any(CtClass.class));
    }

    @Test
    public void testAcceptNotIncluded() {
        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                _ctClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);
        Mockito.doReturn(false).when(_includePredicate).test(_ctClass);
        Assert.assertFalse(classProcessorTask.accept());
    }

    @Test
    public void testAcceptExcluded() {
        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                _ctClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);
        Mockito.doReturn(true).when(_includePredicate).test(_ctClass);
        Mockito.doReturn(true).when(_excludePredicate).test(_ctClass);
        Assert.assertFalse(classProcessorTask.accept());
    }

    @Test
    public void testAcceptFrozen() {
        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                _ctClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);
        Mockito.doReturn(true).when(_includePredicate).test(_ctClass);
        Mockito.doReturn(false).when(_excludePredicate).test(_ctClass);
        Mockito.doReturn(true).when(_ctClass).isFrozen();
        Assert.assertFalse(classProcessorTask.accept());
    }

    @Test
    public void testAcceptProcessorRejects() {
        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                _ctClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);
        Mockito.doReturn(true).when(_includePredicate).test(_ctClass);
        Mockito.doReturn(false).when(_excludePredicate).test(_ctClass);
        Mockito.doReturn(false).when(_ctClass).isFrozen();
        Mockito.doReturn(false).when(_processor).accept(_ctClass);
        Assert.assertFalse(classProcessorTask.accept());
    }

    @Test
    public void testAcceptAlreadyProcessed() throws NotFoundException {
        final ClassPool classPool = createClassPool();
        final CtClass alreadyProcessedCtClass = classPool.get(
                "com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$ReadOnlyAlreadyProcessedClass");

        final ClassProcessor testProcessor = new TestProcessor(true);

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                alreadyProcessedCtClass,
                testProcessor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);

        Mockito.doReturn(true).when(_includePredicate).test(alreadyProcessedCtClass);
        Mockito.doReturn(false).when(_excludePredicate).test(alreadyProcessedCtClass);

        Assert.assertFalse(classProcessorTask.accept());
    }

    @Test
    public void testAccepted() throws NotFoundException {
        final ClassPool classPool = createClassPool();
        final CtClass unprocessedCtClass = classPool.get(
                "com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$UnprocessedClass");

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                unprocessedCtClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);

        Mockito.doReturn(true).when(_includePredicate).test(unprocessedCtClass);
        Mockito.doReturn(false).when(_excludePredicate).test(unprocessedCtClass);
        Mockito.doReturn(true).when(_processor).accept(unprocessedCtClass);
        Assert.assertTrue(classProcessorTask.accept());
    }

    @Test
    public void testIsAlreadyProcessedProcessedBySomethingElse() throws NotFoundException {
        final ClassPool classPool = createClassPool();
        final CtClass processedBySomethingElseCtClass = classPool.get(
                "com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$ProcessedBySomethingElseClass");

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                processedBySomethingElseCtClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);

        Assert.assertFalse(classProcessorTask.isAlreadyProcessed(processedBySomethingElseCtClass, _processor));
    }

    @Test
    public void testIsAlreadyProcessedProcessedEmpty() throws NotFoundException {
        final ClassPool classPool = createClassPool();
        final CtClass processedEmptyCtClass = classPool.get(
                "com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$ProcessedEmptyClass");

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                processedEmptyCtClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);

        Assert.assertFalse(classProcessorTask.isAlreadyProcessed(processedEmptyCtClass, _processor));
    }

    @Test
    public void testIsAlreadyProcessedCheckAnnotationType() throws NotFoundException {
        final ClassPool classPool = createClassPool();
        final CtClass differentlyAnnotatedCtClass = classPool.get(
                "com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$ReadOnlyDifferentlyAnnotatedClass");

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                differentlyAnnotatedCtClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);

        Assert.assertFalse(classProcessorTask.isAlreadyProcessed(differentlyAnnotatedCtClass, _processor));
    }

    @Test
    public void testMarkAsProcessedAlreadyMarked() throws NotFoundException {
        final ClassPool classPool = createClassPool();
        final CtClass alreadyProcessedCtClass = classPool.get(
                "com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$AlreadyProcessedClass");

        final ClassProcessor testProcessor = new TestProcessor(true);

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                alreadyProcessedCtClass,
                testProcessor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);

        classProcessorTask.markAsProcessed(alreadyProcessedCtClass, testProcessor);

        alreadyProcessedCtClass.defrost();
        Assert.assertTrue(classProcessorTask.isAlreadyProcessed(alreadyProcessedCtClass, testProcessor));
    }

    @Test
    public void testMarkAsProcessedExistingAnnotation() throws NotFoundException {
        final ClassPool classPool = createClassPool();
        final CtClass existingAnnotationCtClass = classPool.get(
                "com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$DifferentlyAnnotatedClass");

        final ClassProcessor testProcessor = new TestProcessor(true);

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                existingAnnotationCtClass,
                testProcessor,
                _includePredicate,
                _excludePredicate,
                _outputDirectory,
                _log);

        classProcessorTask.markAsProcessed(existingAnnotationCtClass, testProcessor);

        existingAnnotationCtClass.defrost();
        Assert.assertTrue(classProcessorTask.isAlreadyProcessed(existingAnnotationCtClass, testProcessor));
    }

    @Test
    public void testWriteFileFailure() throws NotFoundException, IOException, CannotCompileException {
        final Path outputDirectory = Paths.get("./target/test-data");

        final ClassProcessorTask classProcessorTask = new ClassProcessorTask(
                _context,
                _ctClass,
                _processor,
                _includePredicate,
                _excludePredicate,
                outputDirectory,
                _log);

        Mockito.doReturn("com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$WriteFileFailure")
                .when(_ctClass).getName();
        Mockito.doThrow(new IOException("Simulated Failure"))
                .when(_ctClass).toBytecode(Mockito.any());

        try {
            classProcessorTask.writeClass(_ctClass, outputDirectory, _context);
            Assert.fail("Expected exception not thrown");
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (final RuntimeException e) {
            // CHECKSTYLE.ON: IllegalCatch
            Assert.assertTrue(e.getCause() instanceof IOException);
        }
    }

    private static ClassPool createClassPool() {
        final ClassPool classPool = new ClassPool(ClassPool.getDefault());
        classPool.appendClassPath(
                new LoaderClassPath(Thread.currentThread()
                        .getContextClassLoader()));
        return classPool;
    }

    @Mock
    private BuildContext _context;
    @Mock
    private CtClass _ctClass;
    @Mock
    private ClassProcessor _processor;
    @Mock
    private Predicate<CtClass> _includePredicate;
    @Mock
    private Predicate<CtClass> _excludePredicate;
    @Mock
    private Path _outputDirectory;
    @Mock
    private Log _log;

    private static final class TestProcessor implements ClassProcessor {

        TestProcessor(final boolean accept) {
            _accept = accept;
        }

        @Override
        public boolean accept(final CtClass ctClass) {
            ++_acceptCount;
            return _accept;
        }

        @Override
        public void process(final CtClass ctClass) {
            ++_processCount;
            // Do nothing
        }

        public int getAcceptCount() {
            return _acceptCount;
        }

        public int getProcessCount() {
            return _processCount;
        }

        private final boolean _accept;
        private int _acceptCount = 0;
        private int _processCount = 0;
    }

    private static final class TestRunClass {}

    private static final class UnprocessedClass {}

    @Processed(value = {"com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$TestProcessor"})
    private static final class AlreadyProcessedClass {}
    
    @Processed(value = {"com.arpnetworking.commons.maven.javassist.plugin.ClassProcessorTaskTest$TestProcessor"})
    private static final class ReadOnlyAlreadyProcessedClass {}

    @Processed(value = {"com.example.MyClassProcessor"})
    private static final class ProcessedBySomethingElseClass {}

    @Processed(value = {})
    private static final class ProcessedEmptyClass {}

    /**
     * @deprecated Just for testing.
     */
    @Deprecated
    private static final class DifferentlyAnnotatedClass {}

    /**
     * @deprecated Just for testing.
     */
    @Deprecated
    private static final class ReadOnlyDifferentlyAnnotatedClass {}
}
