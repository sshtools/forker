package com.sshtools.forker.client;


public interface EffectiveUser {
	void elevate(ForkerBuilder builder, Process process);
	void descend();
}