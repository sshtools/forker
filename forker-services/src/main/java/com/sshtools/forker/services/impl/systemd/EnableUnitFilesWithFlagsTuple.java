package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class EnableUnitFilesWithFlagsTuple extends Tuple {
    @Position(0)
    private boolean carriesInstallInfo;
    @Position(1)
    private List<EnableUnitFilesWithFlagsStruct> changes;

    public EnableUnitFilesWithFlagsTuple(boolean carriesInstallInfo, List<EnableUnitFilesWithFlagsStruct> changes) {
        this.carriesInstallInfo = carriesInstallInfo;
        this.changes = changes;
    }

    public void setCarriesInstallInfo(boolean arg) {
        carriesInstallInfo = arg;
    }

    public boolean getCarriesInstallInfo() {
        return carriesInstallInfo;
    }
    public void setChanges(List<EnableUnitFilesWithFlagsStruct> arg) {
        changes = arg;
    }

    public List<EnableUnitFilesWithFlagsStruct> getChanges() {
        return changes;
    }


}
