package com.sshtools.forker.services;

public interface Service {
    
    public interface Listener {
        void extendedServiceStatusChanged(Service service, ExtendedServiceStatus newStatus);
    }

    public enum Status {
        STARTED, STARTING, STOPPED, STOPPING, PAUSING, PAUSED, UNPAUSING;

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
