package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PresetUnitFilesTuple extends Tuple {
    @Position(0)
    private boolean carriesInstallInfo;
    @Position(1)
    private List<PresetUnitFilesStruct> changes;

    public PresetUnitFilesTuple(boolean carriesInstallInfo, List<PresetUnitFilesStruct> changes) {
        this.carriesInstallInfo = carriesInstallInfo;
        this.changes = changes;
    }

    public void setCarriesInstallInfo(boolean arg) {
        carriesInstallInfo = arg;
    }

    public boolean getCarriesInstallInfo() {
        return carriesInstallInfo;
    }
    public void setChanges(List<PresetUnitFilesStruct> arg) {
        changes = arg;
    }

    public List<PresetUnitFilesStruct> getChanges() {
        return changes;
    }


}
