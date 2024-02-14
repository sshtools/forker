package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class PropertyIPAddressAllowStruct extends Struct {
    @Position(0)
    private final int member0;
    @Position(1)
    private final List<Byte> member1;
    @Position(2)
    private final UInt32 member2;

    public PropertyIPAddressAllowStruct(int member0, List<Byte> member1, UInt32 member2) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
    }


    public int getMember0() {
        return member0;
    }

    public List<Byte> getMember1() {
        return member1;
    }

    public UInt32 getMember2() {
        return member2;
    }


}
