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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.apache.maven.plugin.logging.Log;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Runnable task for transforming classes using Javassist.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
/* package private */ final class ClassProcessorTask implements Runnable, CompletableFuture.AsynchronousCompletionTask {

    /**
     * Constructor.
     *
     * @param context The {@code BuildContext} instance.
     * @param ctClass The {@code CtClass} instance to conditionally process.
     * @param processor The {@code ClassProcessor} instance to apply if included.
     * @param includePredicate The {@code Predicate} to include an element in processing.
     * @param excludePredicate The {@code Predicate} to exclude an element from processing.
     * @param outputDirectory The output directory to write the transformed class to.
     * @param log The {@code Log} instance to record processing to.
     */
    ClassProcessorTask(
            final BuildContext context,
            final CtClass ctClass,
            final ClassProcessor processor,
            final Predicate<CtClass> includePredicate,
            final Predicate<CtClass> excludePredicate,
            final Path outputDirectory,
            final Log log) {
        _context = context;
        _ctClass = ctClass;
        _processor = processor;
        _includePredicate = includePredicate;
        _excludePredicate = excludePredicate;
        _outputDirectory = outputDirectory;
        _log = log;
    }

    @Override
    public void run() {
        // Check if the class is accepted
        if (!accept()) {
            return;
        }

        // Process the class
        _log.info("Processing class: " + _ctClass.getName());
        _processor.process(_ctClass);

        // Mark the class as processed by this processor
        markAsProcessed(_ctClass, _processor);

        // Finalize the class
        _ctClass.getClassFile().compact();
        _ctClass.rebuildClassFile();

        // Write the modified class
        writeClass(_ctClass, _outputDirectory, _context);
    }

    /* package private */ boolean accept() {
        // Assert that the classpath element is included
        if (!_includePredicate.test(_ctClass)) {
            _log.debug("Class is not included: " + _ctClass.getName());
            return false;
        }

        // Assert that the classpath element is not excluded
        if (_excludePredicate.test(_ctClass)) {
            _log.debug("Class is excluded: " + _ctClass.getName());
            return false;
        }

        // Assert that the class is not frozen
        if (_ctClass.isFrozen()) {
            _log.debug("Class is frozen: " + _ctClass.getName());
            return false;
        }

        // Assert that the processor accepts the classpath element
        if (!_processor.accept(_ctClass)) {
            _log.debug("Class is not accepted: " + _ctClass.getName());
            return false;
        }

        // Assert that this processor has not already been run
        if (isAlreadyProcessed(_ctClass, _processor)) {
            _log.info("Class already processed: " + _ctClass.getName());
            return false;
        }

        return true;
    }

    /* package private */ void markAsProcessed(
            final CtClass ctClass,
            final ClassProcessor processor) {
        final ClassFile classFile = ctClass.getClassFile();
        AnnotationsAttribute annotationAttribute = null;
        for (final Object attributeObject : classFile.getAttributes()) {
            if (attributeObject instanceof AnnotationsAttribute) {
                annotationAttribute = (AnnotationsAttribute) attributeObject;
                break;
            }
        }
        if (annotationAttribute == null) {
            _log.debug("Creating annotation attribute on: " + ctClass.getName());
            annotationAttribute = new AnnotationsAttribute(classFile.getConstPool(), AnnotationsAttribute.visibleTag);
            classFile.addAttribute(annotationAttribute);
        }
        Annotation annotation = annotationAttribute.getAnnotation(PROCESSED_ANNOTATION_CLASS);
        if (annotation == null) {
            _log.debug("Creating processed annotation on: " + ctClass.getName());
            annotation = new Annotation(PROCESSED_ANNOTATION_CLASS, classFile.getConstPool());
        }
        MemberValue value = annotation.getMemberValue("value");
        if (value == null) {
            value = new ArrayMemberValue(classFile.getConstPool());
            annotation.addMemberValue("value", value);
        }
        final ArrayMemberValue valueArray = (ArrayMemberValue) value;
        final MemberValue[] existingProcessorValues = valueArray.getValue();
        final MemberValue[] newProcessorValues;
        if (existingProcessorValues != null) {
            newProcessorValues = Arrays.copyOf(valueArray.getValue(), valueArray.getValue().length + 1);
        } else {
            newProcessorValues = new MemberValue[1];
        }
        newProcessorValues[newProcessorValues.length - 1] = new StringMemberValue(
                processor.getClass().getName(),
                classFile.getConstPool());
        valueArray.setValue(newProcessorValues);
        _log.debug("New processed annotation value on: " + ctClass.getName() + " = " + valueArray);
        annotationAttribute.addAnnotation(annotation);
    }

    /* package private */ boolean isAlreadyProcessed(
            final CtClass ctClass,
            final ClassProcessor processor) {
        final ClassFile classFile = ctClass.getClassFile();
        AnnotationsAttribute annotationAttribute = null;
        for (final Object attributeObject : classFile.getAttributes()) {
            if (attributeObject instanceof AnnotationsAttribute) {
                annotationAttribute = (AnnotationsAttribute) attributeObject;
                break;
            }
        }
        if (annotationAttribute != null) {
            final Annotation annotation = annotationAttribute.getAnnotation(PROCESSED_ANNOTATION_CLASS);
            if (annotation != null) {
                final MemberValue value = annotation.getMemberValue("value");
                _log.debug("Existing processed annotation on: " + ctClass.getName() + " = " + value);
                final ArrayMemberValue valueArray = (ArrayMemberValue) value;
                for (final MemberValue processorValue : valueArray.getValue()) {
                    final StringMemberValue processorValueString = (StringMemberValue) processorValue;
                    if (processor.getClass().getName().equals(processorValueString.getValue())) {
                        return true;
                    }
                }
            }
        }
        _log.debug("No annotation attribute or processed annotation found on: " + ctClass.getName());
        return false;
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    /* package private */ void writeClass(
            final CtClass ctClass,
            final Path outputDirectory,
            final BuildContext context) {
        // Translate the class name to a file path
        final File classFile = outputDirectory.resolve(
                Paths.get(ctClass.getName().replace('.', '/') + ".class")).toFile();

        // Ensure the containing directory structure exsits
        final File classDirectory = classFile.getParentFile();
        classDirectory.mkdirs();

        // Write the class to the file
        try (DataOutputStream outputStream = new DataOutputStream(
                new BufferedOutputStream(context.newFileOutputStream(classFile)))) {
            _ctClass.toBytecode(outputStream);
        } catch (final IOException | CannotCompileException e) {
            throw new RuntimeException(e);
        }

        // Update the build context
        context.refresh(classDirectory);
    }

    private final BuildContext _context;
    private final CtClass _ctClass;
    private final ClassProcessor _processor;
    private final Predicate<CtClass> _includePredicate;
    private final Predicate<CtClass> _excludePredicate;
    private final Path _outputDirectory;
    private final Log _log;

    private static final String PROCESSED_ANNOTATION_CLASS = "com.arpnetworking.commons.maven.javassist.Processed";
}
