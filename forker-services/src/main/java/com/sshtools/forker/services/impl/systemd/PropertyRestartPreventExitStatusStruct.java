package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PropertyRestartPreventExitStatusStruct extends Struct {
    @Position(0)
    private final List<Integer> member0;
    @Position(1)
    private final List<Integer> member1;

    public PropertyRestartPreventExitStatusStruct(List<Integer> member0, List<Integer> member1) {
        this.member0 = member0;
        this.member1 = member1;
    }


    public List<Integer> getMember0() {
        return member0;
    }

    public List<Integer> getMember1() {
        return member1;
    }


}
