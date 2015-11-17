package com.sshtools.forker.client;

import com.sshtools.forker.common.Command;



public interface EffectiveUser {
	void elevate(ForkerBuilder builder, Process process, Command command);
	void descend();
}