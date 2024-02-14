package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.systemd1.Service")
public interface Service extends DBusInterface {


    @DBusBoundProperty
    public String getType();
    @DBusBoundProperty
    public String getRestart();
    @DBusBoundProperty
    public String getPIDFile();
    @DBusBoundProperty
    public String getNotifyAccess();
    @DBusBoundProperty
    public UInt64 getRestartUSec();
    @DBusBoundProperty
    public UInt64 getTimeoutStartUSec();
    @DBusBoundProperty
    public UInt64 getTimeoutStopUSec();
    @DBusBoundProperty
    public UInt64 getTimeoutAbortUSec();
    @DBusBoundProperty
    public String getTimeoutStartFailureMode();
    @DBusBoundProperty
    public String getTimeoutStopFailureMode();
    @DBusBoundProperty
    public UInt64 getRuntimeMaxUSec();
    @DBusBoundProperty
    public UInt64 getWatchdogUSec();
    @DBusBoundProperty
    public UInt64 getWatchdogTimestamp();
    @DBusBoundProperty
    public UInt64 getWatchdogTimestampMonotonic();
    @DBusBoundProperty
    public boolean isRootDirectoryStartOnly();
    @DBusBoundProperty
    public boolean isRemainAfterExit();
    @DBusBoundProperty
    public boolean isGuessMainPID();
    @DBusBoundProperty
    public PropertyRestartPreventExitStatusStruct getRestartPreventExitStatus();
    @DBusBoundProperty
    public PropertyRestartForceExitStatusStruct getRestartForceExitStatus();
    @DBusBoundProperty
    public PropertySuccessExitStatusStruct getSuccessExitStatus();
    @DBusBoundProperty
    public UInt32 getMainPID();
    @DBusBoundProperty
    public UInt32 getControlPID();
    @DBusBoundProperty
    public String getBusName();
    @DBusBoundProperty
    public UInt32 getFileDescriptorStoreMax();
    @DBusBoundProperty
    public UInt32 getNFileDescriptorStore();
    @DBusBoundProperty
    public String getStatusText();
    @DBusBoundProperty
    public int getStatusErrno();
    @DBusBoundProperty
    public String getResult();
    @DBusBoundProperty
    public String getReloadResult();
    @DBusBoundProperty
    public String getCleanResult();
    @DBusBoundProperty
    public String getUSBFunctionDescriptors();
    @DBusBoundProperty
    public String getUSBFunctionStrings();
    @DBusBoundProperty
    public UInt32 getUID();
    @DBusBoundProperty
    public UInt32 getGID();
    @DBusBoundProperty
    public UInt32 getNRestarts();
    @DBusBoundProperty
    public String getOOMPolicy();
    @DBusBoundProperty
    public UInt64 getExecMainStartTimestamp();
    @DBusBoundProperty
    public UInt64 getExecMainStartTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getExecMainExitTimestamp();
    @DBusBoundProperty
    public UInt64 getExecMainExitTimestampMonotonic();
    @DBusBoundProperty
    public UInt32 getExecMainPID();
    @DBusBoundProperty
    public int getExecMainCode();
    @DBusBoundProperty
    public int getExecMainStatus();
    @DBusBoundProperty
    public PropertyExecConditionType getExecCondition();
    @DBusBoundProperty
    public PropertyExecConditionExType getExecConditionEx();
    @DBusBoundProperty
    public PropertyExecStartPreType getExecStartPre();
    @DBusBoundProperty
    public PropertyExecStartPreExType getExecStartPreEx();
    @DBusBoundProperty
    public PropertyExecStartType getExecStart();
    @DBusBoundProperty
    public PropertyExecStartExType getExecStartEx();
    @DBusBoundProperty
    public PropertyExecStartPostType getExecStartPost();
    @DBusBoundProperty
    public PropertyExecStartPostExType getExecStartPostEx();
    @DBusBoundProperty
    public PropertyExecReloadType getExecReload();
    @DBusBoundProperty
    public PropertyExecReloadExType getExecReloadEx();
    @DBusBoundProperty
    public PropertyExecStopType getExecStop();
    @DBusBoundProperty
    public PropertyExecStopExType getExecStopEx();
    @DBusBoundProperty
    public PropertyExecStopPostType getExecStopPost();
    @DBusBoundProperty
    public PropertyExecStopPostExType getExecStopPostEx();
    public void BindMount(String source, String destination, boolean readOnly, boolean mkdir);
    public void MountImage(String source, String destination, boolean readOnly, boolean mkdir, List<MountImageStruct> options);
    @DBusBoundProperty
    public String getSlice();
    @DBusBoundProperty
    public String getControlGroup();
    @DBusBoundProperty
    public UInt64 getMemoryCurrent();
    @DBusBoundProperty
    public UInt64 getMemoryAvailable();
    @DBusBoundProperty
    public UInt64 getCPUUsageNSec();
    @DBusBoundProperty
    public PropertyEffectiveCPUsType getEffectiveCPUs();
    @DBusBoundProperty
    public PropertyEffectiveMemoryNodesType getEffectiveMemoryNodes();
    @DBusBoundProperty
    public UInt64 getTasksCurrent();
    @DBusBoundProperty
    public UInt64 getIPIngressBytes();
    @DBusBoundProperty
    public UInt64 getIPIngressPackets();
    @DBusBoundProperty
    public UInt64 getIPEgressBytes();
    @DBusBoundProperty
    public UInt64 getIPEgressPackets();
    @DBusBoundProperty
    public UInt64 getIOReadBytes();
    @DBusBoundProperty
    public UInt64 getIOReadOperations();
    @DBusBoundProperty
    public UInt64 getIOWriteBytes();
    @DBusBoundProperty
    public UInt64 getIOWriteOperations();
    public List<GetProcessesStruct> GetProcesses();
    public void AttachProcesses(String subcgroup, List<UInt32> pids);
    @DBusBoundProperty
    public boolean isDelegate();
    @DBusBoundProperty
    public PropertyDelegateControllersType getDelegateControllers();
    @DBusBoundProperty
    public boolean isCPUAccounting();
    @DBusBoundProperty
    public UInt64 getCPUWeight();
    @DBusBoundProperty
    public UInt64 getStartupCPUWeight();
    @DBusBoundProperty
    public UInt64 getCPUShares();
    @DBusBoundProperty
    public UInt64 getStartupCPUShares();
    @DBusBoundProperty
    public UInt64 getCPUQuotaPerSecUSec();
    @DBusBoundProperty
    public UInt64 getCPUQuotaPeriodUSec();
    @DBusBoundProperty
    public PropertyAllowedCPUsType getAllowedCPUs();
    @DBusBoundProperty
    public PropertyAllowedMemoryNodesType getAllowedMemoryNodes();
    @DBusBoundProperty
    public boolean isIOAccounting();
    @DBusBoundProperty
    public UInt64 getIOWeight();
    @DBusBoundProperty
    public UInt64 getStartupIOWeight();
    @DBusBoundProperty
    public PropertyIODeviceWeightType getIODeviceWeight();
    @DBusBoundProperty
    public PropertyIOReadBandwidthMaxType getIOReadBandwidthMax();
    @DBusBoundProperty
    public PropertyIOWriteBandwidthMaxType getIOWriteBandwidthMax();
    @DBusBoundProperty
    public PropertyIOReadIOPSMaxType getIOReadIOPSMax();
    @DBusBoundProperty
    public PropertyIOWriteIOPSMaxType getIOWriteIOPSMax();
    @DBusBoundProperty
    public PropertyIODeviceLatencyTargetUSecType getIODeviceLatencyTargetUSec();
    @DBusBoundProperty
    public boolean isBlockIOAccounting();
    @DBusBoundProperty
    public UInt64 getBlockIOWeight();
    @DBusBoundProperty
    public UInt64 getStartupBlockIOWeight();
    @DBusBoundProperty
    public PropertyBlockIODeviceWeightType getBlockIODeviceWeight();
    @DBusBoundProperty
    public PropertyBlockIOReadBandwidthType getBlockIOReadBandwidth();
    @DBusBoundProperty
    public PropertyBlockIOWriteBandwidthType getBlockIOWriteBandwidth();
    @DBusBoundProperty
    public boolean isMemoryAccounting();
    @DBusBoundProperty
    public UInt64 getDefaultMemoryLow();
    @DBusBoundProperty
    public UInt64 getDefaultMemoryMin();
    @DBusBoundProperty
    public UInt64 getMemoryMin();
    @DBusBoundProperty
    public UInt64 getMemoryLow();
    @DBusBoundProperty
    public UInt64 getMemoryHigh();
    @DBusBoundProperty
    public UInt64 getMemoryMax();
    @DBusBoundProperty
    public UInt64 getMemorySwapMax();
    @DBusBoundProperty
    public UInt64 getMemoryLimit();
    @DBusBoundProperty
    public String getDevicePolicy();
    @DBusBoundProperty
    public PropertyDeviceAllowType getDeviceAllow();
    @DBusBoundProperty
    public boolean isTasksAccounting();
    @DBusBoundProperty
    public UInt64 getTasksMax();
    @DBusBoundProperty
    public boolean isIPAccounting();
    @DBusBoundProperty
    public PropertyIPAddressAllowType getIPAddressAllow();
    @DBusBoundProperty
    public PropertyIPAddressDenyType getIPAddressDeny();
    @DBusBoundProperty
    public PropertyIPIngressFilterPathType getIPIngressFilterPath();
    @DBusBoundProperty
    public PropertyIPEgressFilterPathType getIPEgressFilterPath();
    @DBusBoundProperty
    public PropertyDisableControllersType getDisableControllers();
    @DBusBoundProperty
    public String getManagedOOMSwap();
    @DBusBoundProperty
    public String getManagedOOMMemoryPressure();
    @DBusBoundProperty
    public UInt32 getManagedOOMMemoryPressureLimit();
    @DBusBoundProperty
    public String getManagedOOMPreference();
    @DBusBoundProperty
    public PropertyBPFProgramType getBPFProgram();
    @DBusBoundProperty
    public PropertySocketBindAllowType getSocketBindAllow();
    @DBusBoundProperty
    public PropertySocketBindDenyType getSocketBindDeny();
    @DBusBoundProperty
    public PropertyEnvironmentType getEnvironment();
    @DBusBoundProperty
    public PropertyEnvironmentFilesType getEnvironmentFiles();
    @DBusBoundProperty
    public PropertyPassEnvironmentType getPassEnvironment();
    @DBusBoundProperty
    public PropertyUnsetEnvironmentType getUnsetEnvironment();
    @DBusBoundProperty
    public UInt32 getUMask();
    @DBusBoundProperty
    public UInt64 getLimitCPU();
    @DBusBoundProperty
    public UInt64 getLimitCPUSoft();
    @DBusBoundProperty
    public UInt64 getLimitFSIZE();
    @DBusBoundProperty
    public UInt64 getLimitFSIZESoft();
    @DBusBoundProperty
    public UInt64 getLimitDATA();
    @DBusBoundProperty
    public UInt64 getLimitDATASoft();
    @DBusBoundProperty
    public UInt64 getLimitSTACK();
    @DBusBoundProperty
    public UInt64 getLimitSTACKSoft();
    @DBusBoundProperty
    public UInt64 getLimitCORE();
    @DBusBoundProperty
    public UInt64 getLimitCORESoft();
    @DBusBoundProperty
    public UInt64 getLimitRSS();
    @DBusBoundProperty
    public UInt64 getLimitRSSSoft();
    @DBusBoundProperty
    public UInt64 getLimitNOFILE();
    @DBusBoundProperty
    public UInt64 getLimitNOFILESoft();
    @DBusBoundProperty
    public UInt64 getLimitAS();
    @DBusBoundProperty
    public UInt64 getLimitASSoft();
    @DBusBoundProperty
    public UInt64 getLimitNPROC();
    @DBusBoundProperty
    public UInt64 getLimitNPROCSoft();
    @DBusBoundProperty
    public UInt64 getLimitMEMLOCK();
    @DBusBoundProperty
    public UInt64 getLimitMEMLOCKSoft();
    @DBusBoundProperty
    public UInt64 getLimitLOCKS();
    @DBusBoundProperty
    public UInt64 getLimitLOCKSSoft();
    @DBusBoundProperty
    public UInt64 getLimitSIGPENDING();
    @DBusBoundProperty
    public UInt64 getLimitSIGPENDINGSoft();
    @DBusBoundProperty
    public UInt64 getLimitMSGQUEUE();
    @DBusBoundProperty
    public UInt64 getLimitMSGQUEUESoft();
    @DBusBoundProperty
    public UInt64 getLimitNICE();
    @DBusBoundProperty
    public UInt64 getLimitNICESoft();
    @DBusBoundProperty
    public UInt64 getLimitRTPRIO();
    @DBusBoundProperty
    public UInt64 getLimitRTPRIOSoft();
    @DBusBoundProperty
    public UInt64 getLimitRTTIME();
    @DBusBoundProperty
    public UInt64 getLimitRTTIMESoft();
    @DBusBoundProperty
    public String getWorkingDirectory();
    @DBusBoundProperty
    public String getRootDirectory();
    @DBusBoundProperty
    public String getRootImage();
    @DBusBoundProperty
    public PropertyRootImageOptionsType getRootImageOptions();
    @DBusBoundProperty
    public PropertyRootHashType getRootHash();
    @DBusBoundProperty
    public String getRootHashPath();
    @DBusBoundProperty
    public PropertyRootHashSignatureType getRootHashSignature();
    @DBusBoundProperty
    public String getRootHashSignaturePath();
    @DBusBoundProperty
    public String getRootVerity();
    @DBusBoundProperty
    public PropertyExtensionImagesType getExtensionImages();
    @DBusBoundProperty
    public PropertyMountImagesType getMountImages();
    @DBusBoundProperty
    public int getOOMScoreAdjust();
    @DBusBoundProperty
    public UInt64 getCoredumpFilter();
    @DBusBoundProperty
    public int getNice();
    @DBusBoundProperty
    public int getIOSchedulingClass();
    @DBusBoundProperty
    public int getIOSchedulingPriority();
    @DBusBoundProperty
    public int getCPUSchedulingPolicy();
    @DBusBoundProperty
    public int getCPUSchedulingPriority();
    @DBusBoundProperty
    public PropertyCPUAffinityType getCPUAffinity();
    @DBusBoundProperty
    public boolean isCPUAffinityFromNUMA();
    @DBusBoundProperty
    public int getNUMAPolicy();
    @DBusBoundProperty
    public PropertyNUMAMaskType getNUMAMask();
    @DBusBoundProperty
    public UInt64 getTimerSlackNSec();
    @DBusBoundProperty
    public boolean isCPUSchedulingResetOnFork();
    @DBusBoundProperty
    public boolean isNonBlocking();
    @DBusBoundProperty
    public String getStandardInput();
    @DBusBoundProperty
    public String getStandardInputFileDescriptorName();
    @DBusBoundProperty
    public PropertyStandardInputDataType getStandardInputData();
    @DBusBoundProperty
    public String getStandardOutput();
    @DBusBoundProperty
    public String getStandardOutputFileDescriptorName();
    @DBusBoundProperty
    public String getStandardError();
    @DBusBoundProperty
    public String getStandardErrorFileDescriptorName();
    @DBusBoundProperty
    public String getTTYPath();
    @DBusBoundProperty
    public boolean isTTYReset();
    @DBusBoundProperty
    public boolean isTTYVHangup();
    @DBusBoundProperty
    public boolean isTTYVTDisallocate();
    @DBusBoundProperty
    public int getSyslogPriority();
    @DBusBoundProperty
    public String getSyslogIdentifier();
    @DBusBoundProperty
    public boolean isSyslogLevelPrefix();
    @DBusBoundProperty
    public int getSyslogLevel();
    @DBusBoundProperty
    public int getSyslogFacility();
    @DBusBoundProperty
    public int getLogLevelMax();
    @DBusBoundProperty
    public UInt64 getLogRateLimitIntervalUSec();
    @DBusBoundProperty
    public UInt32 getLogRateLimitBurst();
    @DBusBoundProperty
    public PropertyLogExtraFieldsType getLogExtraFields();
    @DBusBoundProperty
    public String getLogNamespace();
    @DBusBoundProperty
    public int getSecureBits();
    @DBusBoundProperty
    public UInt64 getCapabilityBoundingSet();
    @DBusBoundProperty
    public UInt64 getAmbientCapabilities();
    @DBusBoundProperty
    public String getUser();
    @DBusBoundProperty
    public String getGroup();
    @DBusBoundProperty
    public boolean isDynamicUser();
    @DBusBoundProperty
    public boolean isRemoveIPC();
    @DBusBoundProperty
    public PropertySetCredentialType getSetCredential();
    @DBusBoundProperty
    public PropertyLoadCredentialType getLoadCredential();
    @DBusBoundProperty
    public PropertySupplementaryGroupsType getSupplementaryGroups();
    @DBusBoundProperty
    public String getPAMName();
    @DBusBoundProperty
    public PropertyReadWritePathsType getReadWritePaths();
    @DBusBoundProperty
    public PropertyReadOnlyPathsType getReadOnlyPaths();
    @DBusBoundProperty
    public PropertyInaccessiblePathsType getInaccessiblePaths();
    @DBusBoundProperty
    public PropertyExecPathsType getExecPaths();
    @DBusBoundProperty
    public PropertyNoExecPathsType getNoExecPaths();
    @DBusBoundProperty
    public UInt64 getMountFlags();
    @DBusBoundProperty
    public boolean isPrivateTmp();
    @DBusBoundProperty
    public boolean isPrivateDevices();
    @DBusBoundProperty
    public boolean isProtectClock();
    @DBusBoundProperty
    public boolean isProtectKernelTunables();
    @DBusBoundProperty
    public boolean isProtectKernelModules();
    @DBusBoundProperty
    public boolean isProtectKernelLogs();
    @DBusBoundProperty
    public boolean isProtectControlGroups();
    @DBusBoundProperty
    public boolean isPrivateNetwork();
    @DBusBoundProperty
    public boolean isPrivateUsers();
    @DBusBoundProperty
    public boolean isPrivateMounts();
    @DBusBoundProperty
    public boolean isPrivateIPC();
    @DBusBoundProperty
    public String getProtectHome();
    @DBusBoundProperty
    public String getProtectSystem();
    @DBusBoundProperty
    public boolean isSameProcessGroup();
    @DBusBoundProperty
    public String getUtmpIdentifier();
    @DBusBoundProperty
    public String getUtmpMode();
    @DBusBoundProperty
    public PropertySELinuxContextStruct getSELinuxContext();
    @DBusBoundProperty
    public PropertyAppArmorProfileStruct getAppArmorProfile();
    @DBusBoundProperty
    public PropertySmackProcessLabelStruct getSmackProcessLabel();
    @DBusBoundProperty
    public boolean isIgnoreSIGPIPE();
    @DBusBoundProperty
    public boolean isNoNewPrivileges();
    @DBusBoundProperty
    public PropertySystemCallFilterStruct getSystemCallFilter();
    @DBusBoundProperty
    public PropertySystemCallArchitecturesType getSystemCallArchitectures();
    @DBusBoundProperty
    public int getSystemCallErrorNumber();
    @DBusBoundProperty
    public PropertySystemCallLogStruct getSystemCallLog();
    @DBusBoundProperty
    public String getPersonality();
    @DBusBoundProperty
    public boolean isLockPersonality();
    @DBusBoundProperty
    public PropertyRestrictAddressFamiliesStruct getRestrictAddressFamilies();
    @DBusBoundProperty
    public String getRuntimeDirectoryPreserve();
    @DBusBoundProperty
    public UInt32 getRuntimeDirectoryMode();
    @DBusBoundProperty
    public PropertyRuntimeDirectoryType getRuntimeDirectory();
    @DBusBoundProperty
    public UInt32 getStateDirectoryMode();
    @DBusBoundProperty
    public PropertyStateDirectoryType getStateDirectory();
    @DBusBoundProperty
    public UInt32 getCacheDirectoryMode();
    @DBusBoundProperty
    public PropertyCacheDirectoryType getCacheDirectory();
    @DBusBoundProperty
    public UInt32 getLogsDirectoryMode();
    @DBusBoundProperty
    public PropertyLogsDirectoryType getLogsDirectory();
    @DBusBoundProperty
    public UInt32 getConfigurationDirectoryMode();
    @DBusBoundProperty
    public PropertyConfigurationDirectoryType getConfigurationDirectory();
    @DBusBoundProperty
    public UInt64 getTimeoutCleanUSec();
    @DBusBoundProperty
    public boolean isMemoryDenyWriteExecute();
    @DBusBoundProperty
    public boolean isRestrictRealtime();
    @DBusBoundProperty
    public boolean isRestrictSUIDSGID();
    @DBusBoundProperty
    public UInt64 getRestrictNamespaces();
    @DBusBoundProperty
    public PropertyBindPathsType getBindPaths();
    @DBusBoundProperty
    public PropertyBindReadOnlyPathsType getBindReadOnlyPaths();
    @DBusBoundProperty
    public PropertyTemporaryFileSystemType getTemporaryFileSystem();
    @DBusBoundProperty
    public boolean isMountAPIVFS();
    @DBusBoundProperty
    public String getKeyringMode();
    @DBusBoundProperty
    public String getProtectProc();
    @DBusBoundProperty
    public String getProcSubset();
    @DBusBoundProperty
    public boolean isProtectHostname();
    @DBusBoundProperty
    public String getNetworkNamespacePath();
    @DBusBoundProperty
    public String getIPCNamespacePath();
    @DBusBoundProperty
    public String getKillMode();
    @DBusBoundProperty
    public int getKillSignal();
    @DBusBoundProperty
    public int getRestartKillSignal();
    @DBusBoundProperty
    public int getFinalKillSignal();
    @DBusBoundProperty
    public boolean isSendSIGKILL();
    @DBusBoundProperty
    public boolean isSendSIGHUP();
    @DBusBoundProperty
    public int getWatchdogSignal();


    public static interface PropertyExecConditionType extends TypeRef<List<PropertyExecConditionStruct>> {




    }

    public static interface PropertyExecConditionExType extends TypeRef<List<PropertyExecConditionExStruct>> {




    }

    public static interface PropertyExecStartPreType extends TypeRef<List<PropertyExecStartPreStruct>> {




    }

    public static interface PropertyExecStartPreExType extends TypeRef<List<PropertyExecStartPreExStruct>> {




    }

    public static interface PropertyExecStartType extends TypeRef<List<PropertyExecStartStruct>> {




    }

    public static interface PropertyExecStartExType extends TypeRef<List<PropertyExecStartExStruct>> {




    }

    public static interface PropertyExecStartPostType extends TypeRef<List<PropertyExecStartPostStruct>> {




    }

    public static interface PropertyExecStartPostExType extends TypeRef<List<PropertyExecStartPostExStruct>> {




    }

    public static interface PropertyExecReloadType extends TypeRef<List<PropertyExecReloadStruct>> {




    }

    public static interface PropertyExecReloadExType extends TypeRef<List<PropertyExecReloadExStruct>> {




    }

    public static interface PropertyExecStopType extends TypeRef<List<PropertyExecStopStruct>> {




    }

    public static interface PropertyExecStopExType extends TypeRef<List<PropertyExecStopExStruct>> {




    }

    public static interface PropertyExecStopPostType extends TypeRef<List<PropertyExecStopPostStruct>> {




    }

    public static interface PropertyExecStopPostExType extends TypeRef<List<PropertyExecStopPostExStruct>> {




    }

    public static interface PropertyEffectiveCPUsType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyEffectiveMemoryNodesType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyDelegateControllersType extends TypeRef<List<String>> {




    }

    public static interface PropertyAllowedCPUsType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyAllowedMemoryNodesType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyIODeviceWeightType extends TypeRef<List<PropertyIODeviceWeightStruct>> {




    }

    public static interface PropertyIOReadBandwidthMaxType extends TypeRef<List<PropertyIOReadBandwidthMaxStruct>> {




    }

    public static interface PropertyIOWriteBandwidthMaxType extends TypeRef<List<PropertyIOWriteBandwidthMaxStruct>> {




    }

    public static interface PropertyIOReadIOPSMaxType extends TypeRef<List<PropertyIOReadIOPSMaxStruct>> {




    }

    public static interface PropertyIOWriteIOPSMaxType extends TypeRef<List<PropertyIOWriteIOPSMaxStruct>> {




    }

    public static interface PropertyIODeviceLatencyTargetUSecType extends TypeRef<List<PropertyIODeviceLatencyTargetUSecStruct>> {




    }

    public static interface PropertyBlockIODeviceWeightType extends TypeRef<List<PropertyBlockIODeviceWeightStruct>> {




    }

    public static interface PropertyBlockIOReadBandwidthType extends TypeRef<List<PropertyBlockIOReadBandwidthStruct>> {




    }

    public static interface PropertyBlockIOWriteBandwidthType extends TypeRef<List<PropertyBlockIOWriteBandwidthStruct>> {




    }

    public static interface PropertyDeviceAllowType extends TypeRef<List<PropertyDeviceAllowStruct>> {




    }

    public static interface PropertyIPAddressAllowType extends TypeRef<List<PropertyIPAddressAllowStruct>> {




    }

    public static interface PropertyIPAddressDenyType extends TypeRef<List<PropertyIPAddressDenyStruct>> {




    }

    public static interface PropertyIPIngressFilterPathType extends TypeRef<List<String>> {




    }

    public static interface PropertyIPEgressFilterPathType extends TypeRef<List<String>> {




    }

    public static interface PropertyDisableControllersType extends TypeRef<List<String>> {




    }

    public static interface PropertyBPFProgramType extends TypeRef<List<PropertyBPFProgramStruct>> {




    }

    public static interface PropertySocketBindAllowType extends TypeRef<List<PropertySocketBindAllowStruct>> {




    }

    public static interface PropertySocketBindDenyType extends TypeRef<List<PropertySocketBindDenyStruct>> {




    }

    public static interface PropertyEnvironmentType extends TypeRef<List<String>> {




    }

    public static interface PropertyEnvironmentFilesType extends TypeRef<List<PropertyEnvironmentFilesStruct>> {




    }

    public static interface PropertyPassEnvironmentType extends TypeRef<List<String>> {




    }

    public static interface PropertyUnsetEnvironmentType extends TypeRef<List<String>> {




    }

    public static interface PropertyRootImageOptionsType extends TypeRef<List<PropertyRootImageOptionsStruct>> {




    }

    public static interface PropertyRootHashType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyRootHashSignatureType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyExtensionImagesType extends TypeRef<List<PropertyExtensionImagesStruct>> {




    }

    public static interface PropertyMountImagesType extends TypeRef<List<PropertyMountImagesStruct>> {




    }

    public static interface PropertyCPUAffinityType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyNUMAMaskType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyStandardInputDataType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyLogExtraFieldsType extends TypeRef<List<List<Byte>>> {




    }

    public static interface PropertySetCredentialType extends TypeRef<List<PropertySetCredentialStruct>> {




    }

    public static interface PropertyLoadCredentialType extends TypeRef<List<PropertyLoadCredentialStruct>> {




    }

    public static interface PropertySupplementaryGroupsType extends TypeRef<List<String>> {




    }

    public static interface PropertyReadWritePathsType extends TypeRef<List<String>> {




    }

    public static interface PropertyReadOnlyPathsType extends TypeRef<List<String>> {




    }

    public static interface PropertyInaccessiblePathsType extends TypeRef<List<String>> {




    }

    public static interface PropertyExecPathsType extends TypeRef<List<String>> {




    }

    public static interface PropertyNoExecPathsType extends TypeRef<List<String>> {




    }

    public static interface PropertySystemCallArchitecturesType extends TypeRef<List<String>> {




    }

    public static interface PropertyRuntimeDirectoryType extends TypeRef<List<String>> {




    }

    public static interface PropertyStateDirectoryType extends TypeRef<List<String>> {




    }

    public static interface PropertyCacheDirectoryType extends TypeRef<List<String>> {




    }

    public static interface PropertyLogsDirectoryType extends TypeRef<List<String>> {




    }

    public static interface PropertyConfigurationDirectoryType extends TypeRef<List<String>> {




    }

    public static interface PropertyBindPathsType extends TypeRef<List<PropertyBindPathsStruct>> {




    }

    public static interface PropertyBindReadOnlyPathsType extends TypeRef<List<PropertyBindReadOnlyPathsStruct>> {




    }

    public static interface PropertyTemporaryFileSystemType extends TypeRef<List<PropertyTemporaryFileSystemStruct>> {




    }
}
