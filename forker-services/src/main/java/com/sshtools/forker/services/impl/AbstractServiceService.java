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
package com.sshtools.forker.services.impl;

import java.util.ArrayList;
import java.util.List;

import com.sshtools.forker.services.ExtendedServiceStatus;
import com.sshtools.forker.services.Service;
import com.sshtools.forker.services.ServiceService;
import com.sshtools.forker.services.ServicesListener;

public abstract class AbstractServiceService implements ServiceService {

    private List<ServicesListener> listeners = new ArrayList<>();

    @Override
    public void addListener(ServicesListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ServicesListener listener) {
        listeners.remove(listener);
    }

    @Override
	public void pauseService(Service service) throws Exception {
    	throw new UnsupportedOperationException();
	}

	@Override
	public void unpauseService(Service service) throws Exception {
    	throw new UnsupportedOperationException();		
	}

	protected void fireServiceRemoved(Service s) {
        // Removed
        for (ServicesListener l : listeners) {
            l.serviceRemoved(s);
        }
    }

    protected void fireStateChange(Service s) {
        // PointerState has change
        for (ServicesListener l : listeners) {
            l.stateChanged(s);
        }
    }

    protected void fireServiceAdded(Service s) {
        // This is a new service
        for (ServicesListener l : listeners) {
            l.serviceAdded(s);
        }
    }

    protected void fireExtendedServiceStatusChanged(Service service, ExtendedServiceStatus newStatus) {
        for (ServicesListener l : listeners) {
            l.extendedStatusChanged(service, newStatus);
        }
    }

}
