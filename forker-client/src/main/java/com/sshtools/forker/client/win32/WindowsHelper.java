package com.sshtools.forker.client.win32;

import java.io.IOException;

import com.sshtools.forker.common.XAdvapi32;
import com.sshtools.forker.common.XAdvapi32.SID_IDENTIFIER_AUTHORITY;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.platform.win32.WinNT.PSID;
import com.sun.jna.platform.win32.WinNT.TOKEN_PRIVILEGES;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class WindowsHelper {

	
	public static void main(String[] args) {
		
		
		try {
			createToken("lee", null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static HANDLE createToken(String username, String domain) throws IOException {
		
		HANDLEByReference processToken = new HANDLEByReference();
		HANDLE authenticatedToken = WinBase.INVALID_HANDLE_VALUE;
		
		PSID userSID;
		
		if(!XAdvapi32.INSTANCE.OpenProcessToken(
				Kernel32.INSTANCE.GetCurrentProcess(), 
				WinNT.TOKEN_QUERY | WinNT.TOKEN_ADJUST_PRIVILEGES, processToken)){ 
			throwLastError("OpenProcessToken");
		}
		
		TOKEN_PRIVILEGES tokpriv = new TOKEN_PRIVILEGES(1);
		if(!XAdvapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_CREATE_TOKEN_NAME, tokpriv.Privileges[0].Luid)) {
			throwLastError("LookupPrivilegeValue");
		}
		
		tokpriv.Privileges[0].Attributes = new DWORD(WinNT.SE_PRIVILEGE_ENABLED);
		
		if(!XAdvapi32.INSTANCE.AdjustTokenPrivileges(processToken.getValue(), false,  tokpriv, 0, null, null)) {
			throwLastError("AdjustTokenPrivileges");;
		}
		
		userSID = getSid(username, domain);

		return null;
	}
	
	static PSID getSid(String username, String domain) throws IOException {

		PSID psid = new PSID();
	    SID_IDENTIFIER_AUTHORITY nt = new XAdvapi32.SID_IDENTIFIER_AUTHORITY();
	    nt.Value = new byte[] {0,0,0,0,0,5};

		if(!XAdvapi32.INSTANCE.AllocateAndInitializeSid(nt, (byte)8, 0, 0, 0, 0, 0, 0, 0, 0, psid.getPointer())) {
			throwLastError("AllocateAndInitializeSid");
		}
		
		if(!XAdvapi32.INSTANCE.IsValidSid(psid)) {
			throwLastError("IsValidSid");
		}

		IntByReference pSid = new IntByReference(0);
		IntByReference pDomain = new IntByReference(0);
		PointerByReference peUse = new PointerByReference();
		
		String accountName;
		if(domain==null || domain.isEmpty()) {
			accountName = username;
		} else {
			accountName = String.format("%s\\%s", domain, username);
		}
		if(!XAdvapi32.INSTANCE.LookupAccountName(null, accountName, psid, pSid, null, pDomain, peUse)) {
			throwLastError("LookupAccountName");
		}

		return psid;
	}


	static void throwLastError(String method) throws IOException {
		throw new IOException(String.format("%s failed with error code %d", method, Kernel32.INSTANCE.GetLastError()));
	}
}
