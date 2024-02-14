package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class EnableUnitFilesTuple extends Tuple {
    @Position(0)
    private boolean carriesInstallInfo;
    @Position(1)
    private List<EnableUnitFilesStruct> changes;

    public EnableUnitFilesTuple(boolean carriesInstallInfo, List<EnableUnitFilesStruct> changes) {
        this.carriesInstallInfo = carriesInstallInfo;
        this.changes = changes;
    }

    public void setCarriesInstallInfo(boolean arg) {
        carriesInstallInfo = arg;
    }

    public boolean getCarriesInstallInfo() {
        return carriesInstallInfo;
    }
    public void setChanges(List<EnableUnitFilesStruct> arg) {
        changes = arg;
    }

    public List<EnableUnitFilesStruct> getChanges() {
        return changes;
    }


}
