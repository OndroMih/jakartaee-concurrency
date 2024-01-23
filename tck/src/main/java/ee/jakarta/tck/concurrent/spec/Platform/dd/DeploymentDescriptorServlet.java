/*
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package ee.jakarta.tck.concurrent.spec.Platform.dd;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import ee.jakarta.tck.concurrent.common.context.IntContext;
import ee.jakarta.tck.concurrent.common.context.StringContext;
import ee.jakarta.tck.concurrent.common.qualifiers.CustomQualifier1;
import ee.jakarta.tck.concurrent.common.qualifiers.CustomQualifier2;
import ee.jakarta.tck.concurrent.common.qualifiers.InvalidQualifier3;
import ee.jakarta.tck.concurrent.framework.TestServlet;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

@WebServlet("/DeploymentDescriptorServlet")
public class DeploymentDescriptorServlet extends TestServlet {
    private static final long serialVersionUID = 1L;
    private static final long MAX_WAIT_SECONDS = TimeUnit.MINUTES.toSeconds(2);
    
    private static final Runnable NOOP_RUNNABLE = () -> { };

    @EJB
    private DeploymentDescriptorTestBeanInterface enterpriseBean;

    @Resource
    private UserTransaction tx;

    // Context Services
    @Inject
    private ContextService injectedDefContextSvc;

    @Inject
    @CustomQualifier1
    private ContextService injectedContextD;
    
    // Managed Executor Services
    @Inject
    private ManagedExecutorService injectedDefMES;

    @Inject
    @CustomQualifier2
    private ManagedExecutorService injectedMESD;
    
    // Managed Scheduled Executor Services
    @Inject
    private ManagedScheduledExecutorService injectedDefMSES;

    @Inject
    @CustomQualifier1
    @CustomQualifier2
    private ManagedScheduledExecutorService injectedMSESD;
    
    // Managed Thread Factory
    @Inject
    private ManagedThreadFactory injectedDefMTF;

    @Resource(lookup = "java:app/concurrent/ThreadFactoryD")
    private ManagedThreadFactory resourceMTFD;
    
    private Integer executeCallableWithContext(final ContextService svc, final int value) throws Exception {
        try {
            IntContext.set(value);
            Callable<Integer> result = svc.contextualCallable(() -> {
                return IntContext.get();
            });
            return result.call();
        } finally {
            IntContext.set(0);
        }
    }

    @SuppressWarnings("unused")
    public void testDeploymentDescriptorDefinesQualifiers() throws Throwable {
        
        assertAll("Context Service Tests",
                () -> assertNotNull(injectedDefContextSvc,
                        "Default contextService was not registered with default qualifier."),
                () -> assertNotNull(injectedContextD,
                        "Deployment Descriptor defined contextService was not registered with required qualifier."),
                () -> assertTrue(CDI.current().select(ContextService.class, InvalidQualifier3.Literal.get()).isUnsatisfied(),
                        "A contextService was satisfied with a qualifier which was not defined in it's deployment descriptior")
                );
        
        //  Verify injected and lookup default context service are the same
        ContextService lookupDefContextSvc = InitialContext.doLookup("java:comp/DefaultContextService");
        
        Integer expected1 = executeCallableWithContext(lookupDefContextSvc, 95);
        Integer actual1 = executeCallableWithContext(injectedDefContextSvc, 95);
        
        assertEquals(expected1, actual1, "Default Context Service behavior differed between injection and lookup");
        
        //  Verify injected and lookup deployment descriptor defined context service are the same
        ContextService lookupContextD = InitialContext.doLookup("java:app/concurrent/ContextD");
        
        assertEquals(Integer.valueOf(0), executeCallableWithContext(lookupContextD, 65),
                "Deployment descriptor defined Context Service that was looked up did not clear the IntContext as configured.");
        assertEquals(Integer.valueOf(0), executeCallableWithContext(injectedContextD, 85),
                "Deployment descriptor defined Context Service that was injected based on a qualifier did not clear the IntContext as configured.");

        ManagedExecutorService lookupDefMES = InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
        ManagedExecutorService lookupMESD = InitialContext.doLookup("java:app/concurrent/ExecutorD");
        
        assertAll("Managed Executor Service Tests",
            () -> assertNotNull(injectedDefMES,
                    "Default managedExecutorService was not registered with default qualifier."),
            () -> assertNotNull(injectedMESD,
                    "Deployment Descriptor defined managedExecutorService was not registered with required qualifiers."),
            () -> assertTrue(CDI.current().select(ManagedExecutorService.class, CustomQualifier2.Literal.get(), InvalidQualifier3.Literal.get()).isUnsatisfied(),
                    "A managedExecutorService was satisfied with both a required and non-required qualifier.")
        );
        
        //TODO verify injected vs lookup services behave the same
        
        ManagedScheduledExecutorService lookupDefMSES = InitialContext.doLookup("java:comp/DefaultManagedScheduledExecutorService");
        ManagedScheduledExecutorService lookupMSESD = InitialContext.doLookup("java:app/concurrent/ScheduledExecutorD");
        
        assertAll("Managed Scheduled Executor Service Tests",
                () -> assertNotNull(injectedDefMSES,
                        "Default managedScheduledExecutorService was not registered with default qualifier."),
                () -> assertNotNull(injectedMSESD,
                        "Deployment Descriptor defined managedScheduledExecutorService was not registered with required qualifiers."),
                () -> assertTrue(CDI.current().select(ManagedScheduledExecutorService.class, CustomQualifier1.Literal.get()).isUnsatisfied(),
                        "A managedScheduledExecutorService was satisfied with one of two required qualifiers.")
        );
        
        //TODO verify injected vs lookup services behave the same

        ManagedThreadFactory lookupDefMTF = InitialContext.doLookup("java:comp/DefaultManagedThreadFactory");
        ManagedThreadFactory lookupMTFD = InitialContext.doLookup("java:app/concurrent/ThreadFactoryD");
        
        assertAll("Thread Factory Tests",
            () -> assertNotNull(injectedDefMTF,
                    "Default managedThreadFactory was not registered with default qualifier."),
            () -> assertEquals(lookupDefMTF.newThread(NOOP_RUNNABLE).getPriority(), injectedDefMTF.newThread(NOOP_RUNNABLE).getPriority(),
                    "Default managedThreadFactory from injection and lookup did not have the same priority."),
            () -> assertNotNull(resourceMTFD,
                    "Deployment Descriptor defined managedThreadFactory with no qualifiers could not be found via @Resource."),
            () -> assertEquals(lookupMTFD.newThread(NOOP_RUNNABLE).getPriority(), resourceMTFD.newThread(NOOP_RUNNABLE).getPriority(),
                    "The managedThreadFactory from resource injection and lookup did not have the same priority."),
            () -> assertTrue(CDI.current().select(ManagedThreadFactory.class, CustomQualifier1.Literal.get()).isUnsatisfied(),
                    "A managedThreadFactory was satisfied with a required qualifier that should have been overriden by the deployment descriptor.")
        );
    }

    public void testDeploymentDescriptorDefinesContextService() throws Throwable {
        ContextService contextSvc = InitialContext.doLookup("java:app/concurrent/ContextD");

        Callable<Integer> checkContextAndGetTransactionStatus;

        tx.begin();
        try {
            IntContext.set(1001);
            StringContext.set("testDeploymentDescriptorDefinesContextService-1");

            checkContextAndGetTransactionStatus = contextSvc.contextualCallable(() -> {
                assertEquals(IntContext.get(), 0); // cleared
                assertEquals(StringContext.get(), "testDeploymentDescriptorDefinesContextService-1"); // propagated
                assertNotNull(InitialContext.doLookup("java:app/concurrent/ExecutorD")); // propagated
                return tx.getStatus(); // unchanged
            });

            StringContext.set("testDeploymentDescriptorDefinesContextService-2");

            int status = checkContextAndGetTransactionStatus.call();
            assertEquals(status, Status.STATUS_ACTIVE);

            assertEquals(IntContext.get(), 1001); // restored
            assertEquals(StringContext.get(), "testDeploymentDescriptorDefinesContextService-2"); // restored
        } finally {
            IntContext.set(0);
            StringContext.set(null);
            tx.rollback();
        }

        int status = checkContextAndGetTransactionStatus.call();
        assertEquals(status, Status.STATUS_NO_TRANSACTION);
    }

    public void testDeploymentDescriptorDefinesManagedExecutor() throws Throwable {
        LinkedBlockingQueue<Object> started = new LinkedBlockingQueue<Object>();
        CountDownLatch taskCanEnd = new CountDownLatch(1);

        Supplier<String> task = () -> {
            try {
                started.add(InitialContext.doLookup("java:app/concurrent/ExecutorD")); // requires Application context
                assertTrue(taskCanEnd.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException | NamingException x) {
                throw new CompletionException(x);
            }
            return StringContext.get();
        };

        ManagedExecutorService executor = InitialContext.doLookup("java:app/concurrent/ExecutorD");

        try {
            StringContext.set("testDeploymentDescriptorDefinesManagedExecutor-1");
            CompletableFuture<String> future1 = executor.supplyAsync(task);

            StringContext.set("testDeploymentDescriptorDefinesManagedExecutor-2");
            CompletableFuture<String> future2 = executor.supplyAsync(task);

            StringContext.set("testDeploymentDescriptorDefinesManagedExecutor-3");
            CompletableFuture<String> future3 = executor.supplyAsync(task);

            StringContext.set("testDeploymentDescriptorDefinesManagedExecutor-4");
            CompletableFuture<String> future4 = executor.supplyAsync(task);

            StringContext.set("testDeploymentDescriptorDefinesManagedExecutor-5");

            // 3 can start per max-async
            assertNotNull(started.poll(MAX_WAIT_SECONDS, TimeUnit.SECONDS));
            assertNotNull(started.poll(MAX_WAIT_SECONDS, TimeUnit.SECONDS));
            assertNotNull(started.poll(MAX_WAIT_SECONDS, TimeUnit.SECONDS));
            assertEquals(started.poll(1, TimeUnit.SECONDS), null);

            taskCanEnd.countDown();

            assertEquals(future1.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS),
                    "testDeploymentDescriptorDefinesManagedExecutor-1");
            assertEquals(future2.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS),
                    "testDeploymentDescriptorDefinesManagedExecutor-2");
            assertEquals(future3.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS),
                    "testDeploymentDescriptorDefinesManagedExecutor-3");
            assertEquals(future4.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS),
                    "testDeploymentDescriptorDefinesManagedExecutor-4");

            assertNotNull(started.poll(MAX_WAIT_SECONDS, TimeUnit.SECONDS));
        } finally {
            IntContext.set(0);
            StringContext.set(null);
        }
    }

    public void testDeploymentDescriptorDefinesManagedScheduledExecutor() throws Throwable {
        enterpriseBean.testDeploymentDescriptorDefinesManagedScheduledExecutor();
    }

    public void testDeploymentDescriptorDefinesManagedThreadFactory() throws Throwable {
        enterpriseBean.testDeploymentDescriptorDefinesManagedThreadFactory();
    }
}
