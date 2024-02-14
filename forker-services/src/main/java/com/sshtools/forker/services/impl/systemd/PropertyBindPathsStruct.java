package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt64;

/**
 * Auto-generated class.
 */
public class PropertyBindPathsStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final String member1;
    @Position(2)
    private final boolean member2;
    @Position(3)
    private final UInt64 member3;

    public PropertyBindPathsStruct(String member0, String member1, boolean member2, UInt64 member3) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
        this.member3 = member3;
    }


    public String getMember0() {
        return member0;
    }

    public String getMember1() {
        return member1;
    }

    public boolean getMember2() {
        return member2;
    }

    public UInt64 getMember3() {
        return member3;
    }


}
