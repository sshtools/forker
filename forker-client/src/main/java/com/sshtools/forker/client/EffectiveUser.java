package com.sshtools.forker.client;

import java.io.IOException;

import com.sshtools.forker.common.Command;



public interface EffectiveUser {
	void elevate(ForkerBuilder builder, Process process, Command command);
	void descend();
}