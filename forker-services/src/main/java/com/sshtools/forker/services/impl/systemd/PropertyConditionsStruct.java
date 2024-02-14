package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PropertyConditionsStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final boolean member1;
    @Position(2)
    private final boolean member2;
    @Position(3)
    private final String member3;
    @Position(4)
    private final int member4;

    public PropertyConditionsStruct(String member0, boolean member1, boolean member2, String member3, int member4) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
        this.member3 = member3;
        this.member4 = member4;
    }


    public String getMember0() {
        return member0;
    }

    public boolean getMember1() {
        return member1;
    }

    public boolean getMember2() {
        return member2;
    }

    public String getMember3() {
        return member3;
    }

    public int getMember4() {
        return member4;
    }


}
