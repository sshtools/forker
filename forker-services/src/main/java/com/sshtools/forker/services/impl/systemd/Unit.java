package com.sshtools.forker.services.impl.systemd;

import java.util.List;
import org.freedesktop.dbus.DBusPath;
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
@DBusInterfaceName("org.freedesktop.systemd1.Unit")
public interface Unit extends DBusInterface {


    @DBusBoundProperty
    public String getId();
    @DBusBoundProperty
    public PropertyNamesType getNames();
    @DBusBoundProperty
    public String getFollowing();
    @DBusBoundProperty
    public PropertyRequiresType getRequires();
    @DBusBoundProperty
    public PropertyRequisiteType getRequisite();
    @DBusBoundProperty
    public PropertyWantsType getWants();
    @DBusBoundProperty
    public PropertyBindsToType getBindsTo();
    @DBusBoundProperty
    public PropertyPartOfType getPartOf();
    @DBusBoundProperty
    public PropertyRequiredByType getRequiredBy();
    @DBusBoundProperty
    public PropertyRequisiteOfType getRequisiteOf();
    @DBusBoundProperty
    public PropertyWantedByType getWantedBy();
    @DBusBoundProperty
    public PropertyBoundByType getBoundBy();
    @DBusBoundProperty
    public PropertyConsistsOfType getConsistsOf();
    @DBusBoundProperty
    public PropertyConflictsType getConflicts();
    @DBusBoundProperty
    public PropertyConflictedByType getConflictedBy();
    @DBusBoundProperty
    public PropertyBeforeType getBefore();
    @DBusBoundProperty
    public PropertyAfterType getAfter();
    @DBusBoundProperty
    public PropertyOnFailureType getOnFailure();
    @DBusBoundProperty
    public PropertyOnFailureOfType getOnFailureOf();
    @DBusBoundProperty
    public PropertyOnSuccessType getOnSuccess();
    @DBusBoundProperty
    public PropertyOnSuccessOfType getOnSuccessOf();
    @DBusBoundProperty
    public PropertyTriggersType getTriggers();
    @DBusBoundProperty
    public PropertyTriggeredByType getTriggeredBy();
    @DBusBoundProperty
    public PropertyPropagatesReloadToType getPropagatesReloadTo();
    @DBusBoundProperty
    public PropertyReloadPropagatedFromType getReloadPropagatedFrom();
    @DBusBoundProperty
    public PropertyPropagatesStopToType getPropagatesStopTo();
    @DBusBoundProperty
    public PropertyStopPropagatedFromType getStopPropagatedFrom();
    @DBusBoundProperty
    public PropertyJoinsNamespaceOfType getJoinsNamespaceOf();
    @DBusBoundProperty
    public PropertySliceOfType getSliceOf();
    @DBusBoundProperty
    public PropertyRequiresMountsForType getRequiresMountsFor();
    @DBusBoundProperty
    public PropertyDocumentationType getDocumentation();
    @DBusBoundProperty
    public String getDescription();
    @DBusBoundProperty
    public String getLoadState();
    @DBusBoundProperty
    public String getActiveState();
    @DBusBoundProperty
    public String getFreezerState();
    @DBusBoundProperty
    public String getSubState();
    @DBusBoundProperty
    public String getFragmentPath();
    @DBusBoundProperty
    public String getSourcePath();
    @DBusBoundProperty
    public PropertyDropInPathsType getDropInPaths();
    @DBusBoundProperty
    public String getUnitFileState();
    @DBusBoundProperty
    public String getUnitFilePreset();
    @DBusBoundProperty
    public UInt64 getStateChangeTimestamp();
    @DBusBoundProperty
    public UInt64 getStateChangeTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInactiveExitTimestamp();
    @DBusBoundProperty
    public UInt64 getInactiveExitTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getActiveEnterTimestamp();
    @DBusBoundProperty
    public UInt64 getActiveEnterTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getActiveExitTimestamp();
    @DBusBoundProperty
    public UInt64 getActiveExitTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getInactiveEnterTimestamp();
    @DBusBoundProperty
    public UInt64 getInactiveEnterTimestampMonotonic();
    @DBusBoundProperty
    public boolean isCanStart();
    @DBusBoundProperty
    public boolean isCanStop();
    @DBusBoundProperty
    public boolean isCanReload();
    @DBusBoundProperty
    public boolean isCanIsolate();
    @DBusBoundProperty
    public PropertyCanCleanType getCanClean();
    @DBusBoundProperty
    public boolean isCanFreeze();
    @DBusBoundProperty
    public PropertyJobStruct getJob();
    @DBusBoundProperty
    public boolean isStopWhenUnneeded();
    @DBusBoundProperty
    public boolean isRefuseManualStart();
    @DBusBoundProperty
    public boolean isRefuseManualStop();
    @DBusBoundProperty
    public boolean isAllowIsolate();
    @DBusBoundProperty
    public boolean isDefaultDependencies();
    @DBusBoundProperty
    public String getOnSuccessJobMode();
    @DBusBoundProperty
    public String getOnFailureJobMode();
    @DBusBoundProperty
    public boolean isIgnoreOnIsolate();
    @DBusBoundProperty
    public boolean isNeedDaemonReload();
    @DBusBoundProperty
    public PropertyMarkersType getMarkers();
    @DBusBoundProperty
    public UInt64 getJobTimeoutUSec();
    @DBusBoundProperty
    public UInt64 getJobRunningTimeoutUSec();
    @DBusBoundProperty
    public String getJobTimeoutAction();
    @DBusBoundProperty
    public String getJobTimeoutRebootArgument();
    @DBusBoundProperty
    public boolean isConditionResult();
    @DBusBoundProperty
    public boolean isAssertResult();
    @DBusBoundProperty
    public UInt64 getConditionTimestamp();
    @DBusBoundProperty
    public UInt64 getConditionTimestampMonotonic();
    @DBusBoundProperty
    public UInt64 getAssertTimestamp();
    @DBusBoundProperty
    public UInt64 getAssertTimestampMonotonic();
    @DBusBoundProperty
    public PropertyConditionsType getConditions();
    @DBusBoundProperty
    public PropertyAssertsType getAsserts();
    @DBusBoundProperty
    public PropertyLoadErrorStruct getLoadError();
    @DBusBoundProperty
    public boolean isTransient();
    @DBusBoundProperty
    public boolean isPerpetual();
    @DBusBoundProperty
    public UInt64 getStartLimitIntervalUSec();
    @DBusBoundProperty
    public UInt32 getStartLimitBurst();
    @DBusBoundProperty
    public String getStartLimitAction();
    @DBusBoundProperty
    public String getFailureAction();
    @DBusBoundProperty
    public int getFailureActionExitStatus();
    @DBusBoundProperty
    public String getSuccessAction();
    @DBusBoundProperty
    public int getSuccessActionExitStatus();
    @DBusBoundProperty
    public String getRebootArgument();
    @DBusBoundProperty
    public PropertyInvocationIDType getInvocationID();
    @DBusBoundProperty
    public String getCollectMode();
    @DBusBoundProperty
    public PropertyRefsType getRefs();
    public DBusPath Start(String mode);
    public DBusPath Stop(String mode);
    public DBusPath Reload(String mode);
    public DBusPath Restart(String mode);
    public DBusPath TryRestart(String mode);
    public DBusPath ReloadOrRestart(String mode);
    public DBusPath ReloadOrTryRestart(String mode);
    public EnqueueJobTuple EnqueueJob(String jobType, String jobMode);
    public void Kill(String whom, int signal);
    public void ResetFailed();
    public void SetProperties(boolean runtime, List<SetPropertiesStruct> properties);
    public void Ref();
    public void Unref();
    public void Clean(List<String> mask);
    public void Freeze();
    public void Thaw();


    public static interface PropertyNamesType extends TypeRef<List<String>> {




    }

    public static interface PropertyRequiresType extends TypeRef<List<String>> {




    }

    public static interface PropertyRequisiteType extends TypeRef<List<String>> {




    }

    public static interface PropertyWantsType extends TypeRef<List<String>> {




    }

    public static interface PropertyBindsToType extends TypeRef<List<String>> {




    }

    public static interface PropertyPartOfType extends TypeRef<List<String>> {




    }

    public static interface PropertyRequiredByType extends TypeRef<List<String>> {




    }

    public static interface PropertyRequisiteOfType extends TypeRef<List<String>> {




    }

    public static interface PropertyWantedByType extends TypeRef<List<String>> {




    }

    public static interface PropertyBoundByType extends TypeRef<List<String>> {




    }

    public static interface PropertyConsistsOfType extends TypeRef<List<String>> {




    }

    public static interface PropertyConflictsType extends TypeRef<List<String>> {




    }

    public static interface PropertyConflictedByType extends TypeRef<List<String>> {




    }

    public static interface PropertyBeforeType extends TypeRef<List<String>> {




    }

    public static interface PropertyAfterType extends TypeRef<List<String>> {




    }

    public static interface PropertyOnFailureType extends TypeRef<List<String>> {




    }

    public static interface PropertyOnFailureOfType extends TypeRef<List<String>> {




    }

    public static interface PropertyOnSuccessType extends TypeRef<List<String>> {




    }

    public static interface PropertyOnSuccessOfType extends TypeRef<List<String>> {




    }

    public static interface PropertyTriggersType extends TypeRef<List<String>> {




    }

    public static interface PropertyTriggeredByType extends TypeRef<List<String>> {




    }

    public static interface PropertyPropagatesReloadToType extends TypeRef<List<String>> {




    }

    public static interface PropertyReloadPropagatedFromType extends TypeRef<List<String>> {




    }

    public static interface PropertyPropagatesStopToType extends TypeRef<List<String>> {




    }

    public static interface PropertyStopPropagatedFromType extends TypeRef<List<String>> {




    }

    public static interface PropertyJoinsNamespaceOfType extends TypeRef<List<String>> {




    }

    public static interface PropertySliceOfType extends TypeRef<List<String>> {




    }

    public static interface PropertyRequiresMountsForType extends TypeRef<List<String>> {




    }

    public static interface PropertyDocumentationType extends TypeRef<List<String>> {




    }

    public static interface PropertyDropInPathsType extends TypeRef<List<String>> {




    }

    public static interface PropertyCanCleanType extends TypeRef<List<String>> {




    }

    public static interface PropertyMarkersType extends TypeRef<List<String>> {




    }

    public static interface PropertyConditionsType extends TypeRef<List<PropertyConditionsStruct>> {




    }

    public static interface PropertyAssertsType extends TypeRef<List<PropertyAssertsStruct>> {




    }

    public static interface PropertyInvocationIDType extends TypeRef<List<Byte>> {




    }

    public static interface PropertyRefsType extends TypeRef<List<String>> {




    }
}
