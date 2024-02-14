package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class PropertyJobStruct extends Struct {
    @Position(0)
    private final UInt32 member0;
    @Position(1)
    private final DBusPath member1;

    public PropertyJobStruct(UInt32 member0, DBusPath member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public UInt32 getMember0() {
        return member0;
    }

    public DBusPath getMember1() {
        return member1;
    }


}
