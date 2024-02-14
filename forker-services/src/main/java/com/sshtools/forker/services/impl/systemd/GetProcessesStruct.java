package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class GetProcessesStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final UInt32 member1;
    @Position(2)
    private final String member2;

    public GetProcessesStruct(String member0, UInt32 member1, String member2) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
    }


    public String getMember0() {
        return member0;
    }

    public UInt32 getMember1() {
        return member1;
    }

    public String getMember2() {
        return member2;
    }


}
