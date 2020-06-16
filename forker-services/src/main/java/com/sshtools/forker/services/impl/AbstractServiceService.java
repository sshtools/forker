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
