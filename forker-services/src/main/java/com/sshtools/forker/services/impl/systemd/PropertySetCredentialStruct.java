package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PropertySetCredentialStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final List<Byte> member1;

    public PropertySetCredentialStruct(String member0, List<Byte> member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public String getMember0() {
        return member0;
    }

    public List<Byte> getMember1() {
        return member1;
    }


}
