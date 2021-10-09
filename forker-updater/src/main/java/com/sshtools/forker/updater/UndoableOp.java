package com.sshtools.forker.updater;

import java.io.IOException;

public interface UndoableOp {
	void undo() throws IOException;

	default void cleanUp() {
	}
}