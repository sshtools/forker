package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PropertySELinuxContextStruct extends Struct {
    @Position(0)
    private final boolean member0;
    @Position(1)
    private final String member1;

    public PropertySELinuxContextStruct(boolean member0, String member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public boolean getMember0() {
        return member0;
    }

    public String getMember1() {
        return member1;
    }


}
