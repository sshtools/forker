package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PropertyEnvironmentFilesStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final boolean member1;

    public PropertyEnvironmentFilesStruct(String member0, boolean member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public String getMember0() {
        return member0;
    }

    public boolean getMember1() {
        return member1;
    }


}
