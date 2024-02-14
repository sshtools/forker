package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.systemd1.Manager")
public interface Manager extends DBusInterface {


    @DBusBoundProperty
    public String getVersion();
    @DBusBoundProperty
    public String getFeatures();
    @DBusBoundProperty
    public String getVirtualization();
    @DBusBoundProperty
    public String getArchitecture();
    @DBusBoundProperty
    public String getTainted();
    @DBusBoundProperty
    public UInt64 getFirmwareTimestamp();
    @DBusBoundProperty
    public UInt64 getFirmwareTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getLoaderTimestamp();
    @DBusBoundProperty
    public UInt64 getLoaderTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getKernelTimestamp();
    @DBusBoundProperty
    public UInt64 getKernelTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInitRDTimestamp();
    @DBusBoundProperty
    public UInt64 getInitRDTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getUserspaceTimestamp();
    @DBusBoundProperty
    public UInt64 getUserspaceTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getFinishTimestamp();
    @DBusBoundProperty
    public UInt64 getFinishTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getSecurityStartTimestamp();
    @DBusBoundProperty
    public UInt64 getSecurityStartTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getSecurityFinishTimestamp();
    @DBusBoundProperty
    public UInt64 getSecurityFinishTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getGeneratorsStartTimestamp();
    @DBusBoundProperty
    public UInt64 getGeneratorsStartTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getGeneratorsFinishTimestamp();
    @DBusBoundProperty
    public UInt64 getGeneratorsFinishTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getUnitsLoadStartTimestamp();
    @DBusBoundProperty
    public UInt64 getUnitsLoadStartTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getUnitsLoadFinishTimestamp();
    @DBusBoundProperty
    public UInt64 getUnitsLoadFinishTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInitRDSecurityStartTimestamp();
    @DBusBoundProperty
    public UInt64 getInitRDSecurityStartTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInitRDSecurityFinishTimestamp();
    @DBusBoundProperty
    public UInt64 getInitRDSecurityFinishTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInitRDGeneratorsStartTimestamp();
    @DBusBoundProperty
    public UInt64 getInitRDGeneratorsStartTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInitRDGeneratorsFinishTimestamp();
    @DBusBoundProperty
    public UInt64 getInitRDGeneratorsFinishTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInitRDUnitsLoadStartTimestamp();
    @DBusBoundProperty
    public UInt64 getInitRDUnitsLoadStartTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInitRDUnitsLoadFinishTimestamp();
    @DBusBoundProperty
    public UInt64 getInitRDUnitsLoadFinishTimestampMonotonic();
    @DBusBoundProperty
    public String getLogLevel();
    @DBusBoundProperty
    public void setLogLevel(String logLevel);
    @DBusBoundProperty
    public String getLogTarget();
    @DBusBoundProperty
    public void setLogTarget(String logTarget);
    @DBusBoundProperty
    public UInt32 getNNames();
    @DBusBoundProperty
    public UInt32 getNFailedUnits();
    @DBusBoundProperty
    public UInt32 getNJobs();
    @DBusBoundProperty
    public UInt32 getNInstalledJobs();
    @DBusBoundProperty
    public UInt32 getNFailedJobs();
    @DBusBoundProperty
    public double getProgress();
    @DBusBoundProperty
    public PropertyEnvironmentType getEnvironment();
    @DBusBoundProperty
    public boolean isConfirmSpawn();
    @DBusBoundProperty
    public boolean isShowStatus();
    @DBusBoundProperty
    public PropertyUnitPathType getUnitPath();
    @DBusBoundProperty
    public String getDefaultStandardOutput();
    @DBusBoundProperty
    public String getDefaultStandardError();
    @DBusBoundProperty
    public UInt64 getRuntimeWatchdogUSec();
    @DBusBoundProperty
    public void setRuntimeWatchdogUSec(UInt64 runtimeWatchdogUSec);
    @DBusBoundProperty
    public UInt64 getRebootWatchdogUSec();
    @DBusBoundProperty
    public void setRebootWatchdogUSec(UInt64 rebootWatchdogUSec);
    @DBusBoundProperty
    public UInt64 getKExecWatchdogUSec();
    @DBusBoundProperty
    public void setKExecWatchdogUSec(UInt64 kExecWatchdogUSec);
    @DBusBoundProperty
    public boolean isServiceWatchdogs();
    @DBusBoundProperty
    public void setServiceWatchdogs(boolean serviceWatchdogs);
    @DBusBoundProperty
    public String getControlGroup();
    @DBusBoundProperty
    public String getSystemState();
    @DBusBoundProperty
    public byte getExitCode();
    @DBusBoundProperty
    public UInt64 getDefaultTimerAccuracyUSec();
    @DBusBoundProperty
    public UInt64 getDefaultTimeoutStartUSec();
    @DBusBoundProperty
    public UInt64 getDefaultTimeoutStopUSec();
    @DBusBoundProperty
    public UInt64 getDefaultTimeoutAbortUSec();
    @DBusBoundProperty
    public UInt64 getDefaultRestartUSec();
    @DBusBoundProperty
    public UInt64 getDefaultStartLimitIntervalUSec();
    @DBusBoundProperty
    public UInt32 getDefaultStartLimitBurst();
    @DBusBoundProperty
    public boolean isDefaultCPUAccounting();
    @DBusBoundProperty
    public boolean isDefaultBlockIOAccounting();
    @DBusBoundProperty
    public boolean isDefaultMemoryAccounting();
    @DBusBoundProperty
    public boolean isDefaultTasksAccounting();
    @DBusBoundProperty
    public UInt64 getDefaultLimitCPU();
    @DBusBoundProperty
    public UInt64 getDefaultLimitCPUSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitFSIZE();
    @DBusBoundProperty
    public UInt64 getDefaultLimitFSIZESoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitDATA();
    @DBusBoundProperty
    public UInt64 getDefaultLimitDATASoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitSTACK();
    @DBusBoundProperty
    public UInt64 getDefaultLimitSTACKSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitCORE();
    @DBusBoundProperty
    public UInt64 getDefaultLimitCORESoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitRSS();
    @DBusBoundProperty
    public UInt64 getDefaultLimitRSSSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitNOFILE();
    @DBusBoundProperty
    public UInt64 getDefaultLimitNOFILESoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitAS();
    @DBusBoundProperty
    public UInt64 getDefaultLimitASSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitNPROC();
    @DBusBoundProperty
    public UInt64 getDefaultLimitNPROCSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitMEMLOCK();
    @DBusBoundProperty
    public UInt64 getDefaultLimitMEMLOCKSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitLOCKS();
    @DBusBoundProperty
    public UInt64 getDefaultLimitLOCKSSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitSIGPENDING();
    @DBusBoundProperty
    public UInt64 getDefaultLimitSIGPENDINGSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitMSGQUEUE();
    @DBusBoundProperty
    public UInt64 getDefaultLimitMSGQUEUESoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitNICE();
    @DBusBoundProperty
    public UInt64 getDefaultLimitNICESoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitRTPRIO();
    @DBusBoundProperty
    public UInt64 getDefaultLimitRTPRIOSoft();
    @DBusBoundProperty
    public UInt64 getDefaultLimitRTTIME();
    @DBusBoundProperty
    public UInt64 getDefaultLimitRTTIMESoft();
    @DBusBoundProperty
    public UInt64 getDefaultTasksMax();
    @DBusBoundProperty
    public UInt64 getTimerSlackNSec();
    @DBusBoundProperty
    public String getDefaultOOMPolicy();
    @DBusBoundProperty
    public String getCtrlAltDelBurstAction();
    public Unit GetUnit(String name);
    public Unit GetUnitByPID(UInt32 pid);
    public Unit GetUnitByInvocationID(List<Byte> invocationId);
    public Unit GetUnitByControlGroup(String cgroup);
    public Unit LoadUnit(String name);
    public Unit StartUnit(String name, String mode);
    public Unit StartUnitReplace(String oldUnit, String newUnit, String mode);
    public Unit StopUnit(String name, String mode);
    public Unit ReloadUnit(String name, String mode);
    public Unit RestartUnit(String name, String mode);
    public Unit TryRestartUnit(String name, String mode);
    public Unit ReloadOrRestartUnit(String name, String mode);
    public Unit ReloadOrTryRestartUnit(String name, String mode);
    public EnqueueUnitJobTuple EnqueueUnitJob(String name, String jobType, String jobMode);
    public void KillUnit(String name, String whom, int signal);
    public void CleanUnit(String name, List<String> mask);
    public void FreezeUnit(String name);
    public void ThawUnit(String name);
    public void ResetFailedUnit(String name);
    public void SetUnitProperties(String name, boolean runtime, List<SetUnitPropertiesStruct> properties);
    public void BindMountUnit(String name, String source, String destination, boolean readOnly, boolean mkdir);
    public void MountImageUnit(String name, String source, String destination, boolean readOnly, boolean mkdir, List<MountImageUnitStruct> options);
    public void RefUnit(String name);
    public void UnrefUnit(String name);
    public DBusPath StartTransientUnit(String name, String mode, List<StartTransientUnitStruct> properties, List<StartTransientUnitStruct> aux);
    public List<GetUnitProcessesStruct> GetUnitProcesses(String name);
    public void AttachProcessesToUnit(String unitName, String subcgroup, List<UInt32> pids);
    public void AbandonScope(String name);
    public DBusPath GetJob(UInt32 id);
    public List<GetJobAfterStruct> GetJobAfter(UInt32 id);
    public List<GetJobBeforeStruct> GetJobBefore(UInt32 id);
    public void CancelJob(UInt32 id);
    public void ClearJobs();
    public void ResetFailed();
    public void SetShowStatus(String mode);
    public List<ListUnitsStruct> ListUnits();
    public List<ListUnitsFilteredStruct> ListUnitsFiltered(List<String> states);
    public List<ListUnitsByPatternsStruct> ListUnitsByPatterns(List<String> states, List<String> patterns);
    public List<ListUnitsByNamesStruct> ListUnitsByNames(List<String> names);
    public List<ListJobsStruct> ListJobs();
    public void Subscribe();
    public void Unsubscribe();
    public String Dump();
    public FileDescriptor DumpByFileDescriptor();
    public void Reload();
    public void Reexecute();
    public void Exit();
    public void Reboot();
    public void PowerOff();
    public void Halt();
    public void KExec();
    public void SwitchRoot(String newRoot, String init);
    public void SetEnvironment(List<String> assignments);
    public void UnsetEnvironment(List<String> names);
    public void UnsetAndSetEnvironment(List<String> names, List<String> assignments);
    public List<DBusPath> EnqueueMarkedJobs();
    public List<ListUnitFilesStruct> ListUnitFiles();
    public List<ListUnitFilesByPatternsStruct> ListUnitFilesByPatterns(List<String> states, List<String> patterns);
    public String GetUnitFileState(String file);
    public EnableUnitFilesTuple EnableUnitFiles(List<String> files, boolean runtime, boolean force);
    public List<DisableUnitFilesStruct> DisableUnitFiles(List<String> files, boolean runtime);
    public EnableUnitFilesWithFlagsTuple EnableUnitFilesWithFlags(List<String> files, UInt64 flags);
    public List<DisableUnitFilesWithFlagsStruct> DisableUnitFilesWithFlags(List<String> files, UInt64 flags);
    public ReenableUnitFilesTuple ReenableUnitFiles(List<String> files, boolean runtime, boolean force);
    public List<LinkUnitFilesStruct> LinkUnitFiles(List<String> files, boolean runtime, boolean force);
    public PresetUnitFilesTuple PresetUnitFiles(List<String> files, boolean runtime, boolean force);
    public PresetUnitFilesWithModeTuple PresetUnitFilesWithMode(List<String> files, String mode, boolean runtime, boolean force);
    public List<MaskUnitFilesStruct> MaskUnitFiles(List<String> files, boolean runtime, boolean force);
    public List<UnmaskUnitFilesStruct> UnmaskUnitFiles(List<String> files, boolean runtime);
    public List<RevertUnitFilesStruct> RevertUnitFiles(List<String> files);
    public List<SetDefaultTargetStruct> SetDefaultTarget(String name, boolean force);
    public String GetDefaultTarget();
    public List<PresetAllUnitFilesStruct> PresetAllUnitFiles(String mode, boolean runtime, boolean force);
    public List<AddDependencyUnitFilesStruct> AddDependencyUnitFiles(List<String> files, String target, String type, boolean runtime, boolean force);
    public List<String> GetUnitFileLinks(String name, boolean runtime);
    public void SetExitCode(byte number);
    public UInt32 LookupDynamicUserByName(String name);
    public String LookupDynamicUserByUID(UInt32 uid);
    public List<GetDynamicUsersStruct> GetDynamicUsers();


    public static interface PropertyEnvironmentType extends TypeRef<List<String>> {




    }

    public static interface PropertyUnitPathType extends TypeRef<List<String>> {




    }

    public static class UnitNew extends DBusSignal {

        private final String id;
        private final DBusPath unit;

        public UnitNew(String _path, String _id, DBusPath _unit) throws DBusException {
            super(_path, _id, _unit);
            this.id = _id;
            this.unit = _unit;
        }


        public String getId() {
            return id;
        }

        public DBusPath getUnit() {
            return unit;
        }


    }

    public static class UnitRemoved extends DBusSignal {

        private final String id;
        private final DBusPath unit;

        public UnitRemoved(String _path, String _id, DBusPath _unit) throws DBusException {
            super(_path, _id, _unit);
            this.id = _id;
            this.unit = _unit;
        }


        public String getId() {
            return id;
        }

        public DBusPath getUnit() {
            return unit;
        }


    }

    public static class JobNew extends DBusSignal {

        private final UInt32 id;
        private final DBusPath job;
        private final String unit;

        public JobNew(String _path, UInt32 _id, DBusPath _job, String _unit) throws DBusException {
            super(_path, _id, _job, _unit);
            this.id = _id;
            this.job = _job;
            this.unit = _unit;
        }


        public UInt32 getId() {
            return id;
        }

        public DBusPath getJob() {
            return job;
        }

        public String getUnit() {
            return unit;
        }


    }

    public static class JobRemoved extends DBusSignal {

        private final UInt32 id;
        private final DBusPath job;
        private final String unit;
        private final String result;

        public JobRemoved(String _path, UInt32 _id, DBusPath _job, String _unit, String _result) throws DBusException {
            super(_path, _id, _job, _unit, _result);
            this.id = _id;
            this.job = _job;
            this.unit = _unit;
            this.result = _result;
        }


        public UInt32 getId() {
            return id;
        }

        public DBusPath getJob() {
            return job;
        }

        public String getUnit() {
            return unit;
        }

        public String getResult() {
            return result;
        }


    }

    public static class StartupFinished extends DBusSignal {

        private final UInt64 firmware;
        private final UInt64 loader;
        private final UInt64 kernel;
        private final UInt64 initrd;
        private final UInt64 userspace;
        private final UInt64 total;

        public StartupFinished(String _path, UInt64 _firmware, UInt64 _loader, UInt64 _kernel, UInt64 _initrd, UInt64 _userspace, UInt64 _total) throws DBusException {
            super(_path, _firmware, _loader, _kernel, _initrd, _userspace, _total);
            this.firmware = _firmware;
            this.loader = _loader;
            this.kernel = _kernel;
            this.initrd = _initrd;
            this.userspace = _userspace;
            this.total = _total;
        }


        public UInt64 getFirmware() {
            return firmware;
        }

        public UInt64 getLoader() {
            return loader;
        }

        public UInt64 getKernel() {
            return kernel;
        }

        public UInt64 getInitrd() {
            return initrd;
        }

        public UInt64 getUserspace() {
            return userspace;
        }

        public UInt64 getTotal() {
            return total;
        }


    }

    public static class UnitFilesChanged extends DBusSignal {


        public UnitFilesChanged(String _path) throws DBusException {
            super(_path);
        }



    }

    public static class Reloading extends DBusSignal {

        private final boolean active;

        public Reloading(String _path, boolean _active) throws DBusException {
            super(_path, _active);
            this.active = _active;
        }


        public boolean getActive() {
            return active;
        }


    }
}
