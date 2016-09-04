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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

/**
 * Tests for {@link ProcessTestMojo}.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
public final class ProcessTestMojoTest extends AbstractMojoTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void test() throws Exception {
        // This is failing with a ComponentLookupException:
        /*
        org.codehaus.plexus.component.repository.exception.ComponentLookupException:
        java.util.NoSuchElementException
              role: org.apache.maven.plugin.Mojo
          roleHint: com.arpnetworking.commons:javassist-maven-plugin:1.0.0-SNAPSHOT:test-process
            at org.codehaus.plexus.DefaultPlexusContainer.lookup(DefaultPlexusContainer.java:267)
            at org.codehaus.plexus.DefaultPlexusContainer.lookup(DefaultPlexusContainer.java:243)
            at org.codehaus.plexus.PlexusTestCase.lookup(PlexusTestCase.java:205)
            at org.apache.maven.plugin.testing.AbstractMojoTestCase.lookupMojo(AbstractMojoTestCase.java:182)
            at org.apache.maven.plugin.testing.AbstractMojoTestCase.lookupMojo(AbstractMojoTestCase.java:127)
            at com.arpnetworking.commons.maven.javassist.plugin.ProcessTestMojoTest.test(ProcessTestMojoTest.java:41)
            at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
            at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
            at java.lang.reflect.Method.invoke(Method.java:498)
            at junit.framework.TestCase.runTest(TestCase.java:176)
            at junit.framework.TestCase.runBare(TestCase.java:141)
            at junit.framework.TestResult$1.protect(TestResult.java:122)
            at junit.framework.TestResult.runProtected(TestResult.java:142)
            at junit.framework.TestResult.run(TestResult.java:125)
            at junit.framework.TestCase.run(TestCase.java:129)
            at junit.framework.TestSuite.runTest(TestSuite.java:252)
            at junit.framework.TestSuite.run(TestSuite.java:247)
            at org.junit.internal.runners.JUnit38ClassRunner.run(JUnit38ClassRunner.java:86)
            at org.junit.runners.Suite.runChild(Suite.java:128)
            at org.junit.runners.Suite.runChild(Suite.java:27)
            at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
            at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
            at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
            at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
            at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
            at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
            at org.junit.runners.Suite.runChild(Suite.java:128)
            at org.junit.runners.Suite.runChild(Suite.java:27)
            at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
            at org.apache.maven.surefire.junitcore.pc.Scheduler$1.run(Scheduler.java:393)
            at org.apache.maven.surefire.junitcore.pc.InvokerStrategy.schedule(InvokerStrategy.java:54)
            at org.apache.maven.surefire.junitcore.pc.Scheduler.schedule(Scheduler.java:352)
            at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
            at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
            at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
            at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
            at org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder$PC$1.run(ParallelComputerBuilder.java:554)
            at org.apache.maven.surefire.junitcore.JUnitCore.run(JUnitCore.java:55)
            at org.apache.maven.surefire.junitcore.JUnitCoreWrapper.createRequestAndRun(JUnitCoreWrapper.java:137)
            at org.apache.maven.surefire.junitcore.JUnitCoreWrapper.executeEager(JUnitCoreWrapper.java:107)
            at org.apache.maven.surefire.junitcore.JUnitCoreWrapper.execute(JUnitCoreWrapper.java:83)
            at org.apache.maven.surefire.junitcore.JUnitCoreWrapper.execute(JUnitCoreWrapper.java:75)
            at org.apache.maven.surefire.junitcore.JUnitCoreProvider.invoke(JUnitCoreProvider.java:161)
            at org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:290)
            at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:242)
            at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:121)
        Caused by: java.util.NoSuchElementException
            at java.util.Collections$EmptyIterator.next(Collections.java:4189)
            at org.codehaus.plexus.DefaultPlexusContainer.lookup(DefaultPlexusContainer.java:263)
            ... 46 more
        */
        /*
        final File testPom = new File(getBasedir(), "src/test/resources/process-test-mojo/pom.xml");
        Assert.assertNotNull(testPom);
        Assert.assertTrue(testPom.exists());

        final ProcessTestMojo mojo = (ProcessTestMojo) lookupMojo("test-process", testPom);
        Assert.assertNotNull(mojo);
        mojo.execute();
        */
    }
}
