/**
 * Copyright © 2015 - 2021 SSHTOOLS Limited (support@sshtools.com)
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
package com.sshtools.forker.services;

public interface Service {
    
    public interface Listener {
        void extendedServiceStatusChanged(Service service, ExtendedServiceStatus newStatus);
    }

    public enum Status {
        STARTED, STARTING, STOPPED, STOPPING, PAUSING, PAUSED, UNPAUSING, UNKNOWN;

        public boolean isRunning() {
            return this == STARTED || this == STARTING || this == PAUSED || this ==PAUSING || this == PAUSED;
        }
    }
    
    void addListener(Listener l);
    
    void removeListener(Listener l);
    
    void configure(ServiceService service);

    String getNativeName();

    Status getStatus();
    
    ExtendedServiceStatus getExtendedStatus();
}
