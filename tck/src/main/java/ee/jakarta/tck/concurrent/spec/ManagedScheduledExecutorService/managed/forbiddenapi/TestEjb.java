/*
 * Copyright (c) 2013, 2023 Oracle and/or its affiliates. All rights reserved.
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

package ee.jakarta.tck.concurrent.spec.ManagedScheduledExecutorService.managed.forbiddenapi;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import ee.jakarta.tck.concurrent.framework.TestConstants;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;

@Stateless
public class TestEjb implements TestEjbInterface {

    private static final String DIDNOT_CATCH_ILLEGALSTATEEXCEPTION = "IllegalStateException expected";

    @Resource(lookup = TestConstants.defaultManagedScheduledExecutorService)
    public ManagedScheduledExecutorService scheduledExecutor;

    public void testAwaitTermination() {
        try {
            scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        } catch (IllegalStateException e) {
            return;
        }
        fail(DIDNOT_CATCH_ILLEGALSTATEEXCEPTION);
    }

    public void testIsShutdown() {
        try {
            scheduledExecutor.isShutdown();
        } catch (IllegalStateException e) {
            return;
        }
        fail(DIDNOT_CATCH_ILLEGALSTATEEXCEPTION);
    }

    public void testIsTerminated() {
        try {
            scheduledExecutor.isTerminated();
        } catch (IllegalStateException e) {
            return;
        }
        fail(DIDNOT_CATCH_ILLEGALSTATEEXCEPTION);
    }

    public void testShutdown() {
        try {
            scheduledExecutor.shutdown();
        } catch (IllegalStateException e) {
            return;
        }
        fail(DIDNOT_CATCH_ILLEGALSTATEEXCEPTION);
    }

    public void testShutdownNow() {
        try {
            scheduledExecutor.shutdownNow();
        } catch (IllegalStateException e) {
            return;
        }
        fail(DIDNOT_CATCH_ILLEGALSTATEEXCEPTION);
    }

}
