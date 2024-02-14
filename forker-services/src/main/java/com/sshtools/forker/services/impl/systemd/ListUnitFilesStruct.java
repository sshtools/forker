package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class ListUnitFilesStruct extends Struct {
    @Position(0)
    private final String name;
    @Position(1)
    private final String state;

    public ListUnitFilesStruct(String name, String state) {
        this.name = name;
        this.state = state;
    }


    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }


}
