package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PresetUnitFilesWithModeTuple extends Tuple {
    @Position(0)
    private boolean carriesInstallInfo;
    @Position(1)
    private List<PresetUnitFilesWithModeStruct> changes;

    public PresetUnitFilesWithModeTuple(boolean carriesInstallInfo, List<PresetUnitFilesWithModeStruct> changes) {
        this.carriesInstallInfo = carriesInstallInfo;
        this.changes = changes;
    }

    public void setCarriesInstallInfo(boolean arg) {
        carriesInstallInfo = arg;
    }

    public boolean getCarriesInstallInfo() {
        return carriesInstallInfo;
    }
    public void setChanges(List<PresetUnitFilesWithModeStruct> arg) {
        changes = arg;
    }

    public List<PresetUnitFilesWithModeStruct> getChanges() {
        return changes;
    }


}
