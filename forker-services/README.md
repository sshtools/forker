# Forker Services

## Introduction

Forker Services provides a way of starting, stopping and querying local system services. Multiple
implementations are provided for the 3 main operating systems the Forker suite supportes. On 
Windows, the *sc* command is currently used (with polling). On Linux and Unix there are multiple
options, with the most efficient being the SystemD DBus based one.  
   
## Adding Forker Services To Your Project

To include the Forker Services in your project, you will need the module :-

### Maven

```
<dependencies>
	<dependency>
		<groupId>com.sshtools</groupId>
		<artifactId>forker-services</artifactId>
		<version>1.6</version>
	</dependency>
</dependencies>
```

## Usage

The API is very simple. You just need to obtain an instane of a `ServiceService` and call it's various methods to control the local services.

### Quick Start

For the impatient :-

```java

// List services
ServiceService ss = Services.get();
for(Service s : ss.getServices()) {
	System.out.println(s.getNativeName());
}

// Stop a service
ss.stopService(ss.getService("ntp"));

// Start a service
ss.startService(ss.getService("ntp"));

// Listen for changes in service state
ss.addListener(new ServicesListener() {

    void stateChanged(Service service) {
    	// Services has started, stopped, etc
    }

    void extendedStatusChanged(Service service, ExtendedServiceStatus extStatus) {
    	// Extended information about the state change if available, called after stateChange()
    }

    void serviceAdded(Service service) {
    	// New service has appeared
    }

    void serviceRemoved(Service service) {
    	// Service has disappeated
    }
});

```

### Usage

#### Obtaining ServiceService

##### Using The Services Helper

The default and easiest way to initialize and obtain a `ServiceService` is to call `Services.get()`. By default, this will detected the best implementation to use for your platform.

You can specify you own implementation by setting the system property `forker.services.impl` before `get()` is called. 

You can also call `Services.set(myServiceService)`, and again, this must be done before `get()` is called.

##### Manually

If you have you own platform detecting logic, simply instantiate the appropriate implementation yourself.

```java

ServiceService ss;
if(getMyOS().equals("WINDOWS")) {
	ss = new Win32ServiceService();
}
else {
	// TODO detect other OSs
}

// You must then initialize with your own instance of ServicesContext
ss.configure(new MyContext());

```
