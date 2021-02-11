package com.sshtools.forker.services;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractService implements Service {

    private String nativeName;
    private Status status = null;
    private ExtendedServiceStatus extendedStatus;
    private List<Listener> listeners = new ArrayList<Listener>();

    public AbstractService(String nativeName) {
        this(nativeName, null);
    }

    public AbstractService(String nativeName, Status status) {
        super();
        this.nativeName = nativeName;
        this.status = status;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    @Override
    public void configure(ServiceService service) {
    }

    public ExtendedServiceStatus getExtendedStatus() {
        return extendedStatus;
    }

    public void setExtendedStatus(ExtendedServiceStatus extendedStatus) {
        this.extendedStatus = extendedStatus;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nativeName == null) ? 0 : nativeName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        AbstractService other = (AbstractService) obj;
        if (nativeName == null) {
            if (other.nativeName != null)
                return false;
        } else if (!nativeName.equals(other.nativeName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AbstractService [nativeName=" + getNativeName() + ", status=" + getStatus() + "]";
    }

    protected void fireExtendedServiceStatusChanged(ExtendedServiceStatus newStatus) {
        for (Listener l : listeners) {
            l.extendedServiceStatusChanged(this, newStatus);
        }
    }

}
