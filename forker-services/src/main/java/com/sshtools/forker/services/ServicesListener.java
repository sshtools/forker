package com.sshtools.forker.services;

public interface ServicesListener {

    void serviceAdded(Service service);

    void serviceRemoved(Service service);

    void stateChanged(Service service);

    void extendedStatusChanged(Service service, ExtendedServiceStatus extStatus);
}
