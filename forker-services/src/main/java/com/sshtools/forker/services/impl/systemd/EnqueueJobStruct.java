package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class EnqueueJobStruct extends Struct {
    @Position(0)
    private final UInt32 member0;
    @Position(1)
    private final DBusPath member1;
    @Position(2)
    private final String member2;
    @Position(3)
    private final DBusPath member3;
    @Position(4)
    private final String member4;

    public EnqueueJobStruct(UInt32 member0, DBusPath member1, String member2, DBusPath member3, String member4) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
        this.member3 = member3;
        this.member4 = member4;
    }


    public UInt32 getMember0() {
        return member0;
    }

    public DBusPath getMember1() {
        return member1;
    }

    public String getMember2() {
        return member2;
    }

    public DBusPath getMember3() {
        return member3;
    }

    public String getMember4() {
        return member4;
    }


}
