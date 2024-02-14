package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class EnableUnitFilesStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final String member1;
    @Position(2)
    private final String member2;

    public EnableUnitFilesStruct(String member0, String member1, String member2) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
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


}
