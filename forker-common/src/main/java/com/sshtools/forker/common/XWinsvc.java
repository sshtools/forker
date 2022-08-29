package com.sshtools.forker.common;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.win32.W32APITypeMapper;

public interface XWinsvc extends Winsvc {

	@FieldOrder({ "fDelayedAutostart" })
	public class SERVICE_DELAYED_AUTO_START_INFO extends ChangeServiceConfig2Info {
		public static class ByReference extends SERVICE_DELAYED_AUTO_START_INFO implements Structure.ByReference {
		}

		public boolean fDelayedAutostart;
	}
	
	public final static DWORD SERVICE_SID_TYPE_NONE = new DWORD(0x00000000);
	public final static DWORD SERVICE_SID_TYPE_RESTRICTED = new DWORD(0x00000003);
	public final static DWORD SERVICE_SID_TYPE_UNRESTRICTED = new DWORD(0x00000001);
	
	@FieldOrder({ "dwServiceSidType" })
	public class SERVICE_SID_INFO extends ChangeServiceConfig2Info {
		public static class ByReference extends SERVICE_SID_INFO implements Structure.ByReference {
		}

		public DWORD dwServiceSidType;
	}

	@FieldOrder({ "lpDescription" })
	public class SERVICE_DESCRIPTION extends ChangeServiceConfig2Info {
		public static class ByReference extends SERVICE_DESCRIPTION implements Structure.ByReference {
		}

		public String lpDescription;
	}

	@FieldOrder({ "lpServiceName", "lpDisplayName", "ServiceStatusProcess" })
	public static class ENUM_SERVICE_STATUS_PROCESS extends Structure {
		public Pointer lpServiceName;
		public Pointer lpDisplayName;
		public SERVICE_STATUS_PROCESS ServiceStatusProcess;

		public ENUM_SERVICE_STATUS_PROCESS() {
			super(W32APITypeMapper.DEFAULT);
		}

		public ENUM_SERVICE_STATUS_PROCESS(Pointer pointer) {
			super(pointer);
			read();
		}
	}
}
