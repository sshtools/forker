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
