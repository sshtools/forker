package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class StartTransientUnitStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final List<StartTransientUnitStructStruct> member1;

    public StartTransientUnitStruct(String member0, List<StartTransientUnitStructStruct> member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public String getMember0() {
        return member0;
    }

    public List<StartTransientUnitStructStruct> getMember1() {
        return member1;
    }


}
