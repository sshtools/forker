package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PropertyExtensionImagesStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final boolean member1;
    @Position(2)
    private final List<PropertyExtensionImagesStructStruct> member2;

    public PropertyExtensionImagesStruct(String member0, boolean member1, List<PropertyExtensionImagesStructStruct> member2) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
    }


    public String getMember0() {
        return member0;
    }

    public boolean getMember1() {
        return member1;
    }

    public List<PropertyExtensionImagesStructStruct> getMember2() {
        return member2;
    }


}
