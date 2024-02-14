package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PropertyMountImagesStructStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final String member1;

    public PropertyMountImagesStructStruct(String member0, String member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public String getMember0() {
        return member0;
    }

    public String getMember1() {
        return member1;
    }


}
