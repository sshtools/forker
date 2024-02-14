package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class EnqueueJobTuple extends Tuple {
    @Position(0)
    private UInt32 jobId;
    @Position(1)
    private DBusPath jobPath;
    @Position(2)
    private String unitId;
    @Position(3)
    private DBusPath unitPath;
    @Position(4)
    private String jobType;
    @Position(5)
    private List<EnqueueJobStruct> affectedJobs;

    public EnqueueJobTuple(UInt32 jobId, DBusPath jobPath, String unitId, DBusPath unitPath, String jobType, List<EnqueueJobStruct> affectedJobs) {
        this.jobId = jobId;
        this.jobPath = jobPath;
        this.unitId = unitId;
        this.unitPath = unitPath;
        this.jobType = jobType;
        this.affectedJobs = affectedJobs;
    }

    public void setJobId(UInt32 arg) {
        jobId = arg;
    }

    public UInt32 getJobId() {
        return jobId;
    }
    public void setJobPath(DBusPath arg) {
        jobPath = arg;
    }

    public DBusPath getJobPath() {
        return jobPath;
    }
    public void setUnitId(String arg) {
        unitId = arg;
    }

    public String getUnitId() {
        return unitId;
    }
    public void setUnitPath(DBusPath arg) {
        unitPath = arg;
    }

    public DBusPath getUnitPath() {
        return unitPath;
    }
    public void setJobType(String arg) {
        jobType = arg;
    }

    public String getJobType() {
        return jobType;
    }
    public void setAffectedJobs(List<EnqueueJobStruct> arg) {
        affectedJobs = arg;
    }

    public List<EnqueueJobStruct> getAffectedJobs() {
        return affectedJobs;
    }


}
