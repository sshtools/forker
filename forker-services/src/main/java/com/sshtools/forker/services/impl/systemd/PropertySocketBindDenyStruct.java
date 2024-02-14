package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt16;

/**
 * Auto-generated class.
 */
public class PropertySocketBindDenyStruct extends Struct {
    @Position(0)
    private final int member0;
    @Position(1)
    private final int member1;
    @Position(2)
    private final UInt16 member2;
    @Position(3)
    private final UInt16 member3;

    public PropertySocketBindDenyStruct(int member0, int member1, UInt16 member2, UInt16 member3) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
        this.member3 = member3;
    }


    public int getMember0() {
        return member0;
    }

    public int getMember1() {
        return member1;
    }

    public UInt16 getMember2() {
        return member2;
    }

    public UInt16 getMember3() {
        return member3;
    }


}
