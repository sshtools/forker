package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt64;

/**
 * Auto-generated class.
 */
public class PropertyBlockIODeviceWeightStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final UInt64 member1;

    public PropertyBlockIODeviceWeightStruct(String member0, UInt64 member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public String getMember0() {
        return member0;
    }

    public UInt64 getMember1() {
        return member1;
    }


}
