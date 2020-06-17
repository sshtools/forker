package com.sshtools.forker.services;

public interface ServicesListener {

    default void stateChanged(Service service) {
    }

    default void extendedStatusChanged(Service service, ExtendedServiceStatus extStatus) {
    }

    default void serviceAdded(Service service) {
    }

    default void serviceRemoved(Service service) {
    }
}
