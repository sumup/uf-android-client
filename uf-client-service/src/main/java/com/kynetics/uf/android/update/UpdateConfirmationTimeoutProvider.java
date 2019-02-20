/*
 *
 *  Copyright © 2017-2019  Kynetics  LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.uf.android.update;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface UpdateConfirmationTimeoutProvider {
    class Timeout{
        public final long value;
        public final TimeUnit timeUnit;

        public Timeout(long value, TimeUnit timeUnit) {
            this.value = value;
            this.timeUnit = timeUnit;
        }
    }

    Timeout getTimeout(List<File> files);

    class FixedTimeProvider implements UpdateConfirmationTimeoutProvider{

        public static UpdateConfirmationTimeoutProvider ofSeconds(long seconds){
            return new FixedTimeProvider(seconds);
        }

        final long timeout;

        private FixedTimeProvider(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public Timeout getTimeout(List<File> files) {
            return new Timeout(timeout, TimeUnit.SECONDS);
        }
    }
}
