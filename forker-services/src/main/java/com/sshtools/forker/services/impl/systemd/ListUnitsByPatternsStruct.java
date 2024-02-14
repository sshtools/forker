package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class ListUnitsByPatternsStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final String member1;
    @Position(2)
    private final String member2;
    @Position(3)
    private final String member3;
    @Position(4)
    private final String member4;
    @Position(5)
    private final String member5;
    @Position(6)
    private final DBusPath member6;
    @Position(7)
    private final UInt32 member7;
    @Position(8)
    private final String member8;
    @Position(9)
    private final DBusPath member9;

    public ListUnitsByPatternsStruct(String member0, String member1, String member2, String member3, String member4, String member5, DBusPath member6, UInt32 member7, String member8, DBusPath member9) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
        this.member3 = member3;
        this.member4 = member4;
        this.member5 = member5;
        this.member6 = member6;
        this.member7 = member7;
        this.member8 = member8;
        this.member9 = member9;
    }


    public String getMember0() {
        return member0;
    }

    public String getMember1() {
        return member1;
    }

    public String getMember2() {
        return member2;
    }

    public String getMember3() {
        return member3;
    }

    public String getMember4() {
        return member4;
    }

    public String getMember5() {
        return member5;
    }

    public DBusPath getMember6() {
        return member6;
    }

    public UInt32 getMember7() {
        return member7;
    }

    public String getMember8() {
        return member8;
    }

    public DBusPath getMember9() {
        return member9;
    }


}
