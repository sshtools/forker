package com.sshtools.forker.services;

public interface ExtendedServiceStatus {
	int getMessageType();

	String getId();

	String getText();

	Throwable getError();

	Object getAccessory();
}
