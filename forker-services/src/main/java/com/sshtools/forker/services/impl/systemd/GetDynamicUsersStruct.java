package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class GetDynamicUsersStruct extends Struct {
    @Position(0)
    private final UInt32 member0;
    @Position(1)
    private final String member1;

    public GetDynamicUsersStruct(UInt32 member0, String member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public UInt32 getMember0() {
        return member0;
    }

    public String getMember1() {
        return member1;
    }


}
