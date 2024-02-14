package com.sshtools.forker.services.impl.systemd;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class ListUnitsStruct extends Struct {
    @Position(0)
    private final String name;
    @Position(1)
    private final String description;
    @Position(2)
    private final String loadState;
    @Position(3)
    private final String activeState;
    @Position(4)
    private final String subState;
    @Position(5)
    private final String follower;
    @Position(6)
    private final Unit unit;
    @Position(7)
    private final UInt32 jobId;
    @Position(8)
    private final String jobType;
    @Position(9)
    private final DBusPath job;

    public ListUnitsStruct(String name, String description, String loadState, String activeState, String subState, String follower, Unit unit, UInt32 jobId, String jobType, DBusPath job) {
        this.name = name;
        this.description = description;
        this.loadState = loadState;
        this.activeState = activeState;
        this.subState = subState;
        this.follower = follower;
        this.unit = unit;
        this.jobId = jobId;
        this.jobType = jobType;
        this.job = job;
    }


    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLoadState() {
        return loadState;
    }

    public String getActiveState() {
        return activeState;
    }

    public String getSubState() {
        return subState;
    }

    public String getFollower() {
        return follower;
    }

    public Unit getUnit() {
        return unit;
    }

    public UInt32 getJobId() {
        return jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public DBusPath getJob() {
        return job;
    }


}
