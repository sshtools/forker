package com.sshtools.forker.services;

import javax.swing.JComponent;

public interface ExtendedServiceStatus {
	int getMessageType();

	String getId();

	String getText();

	Throwable getError();

	JComponent getAccessory();
}
