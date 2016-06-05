package com.sshtools.forker.common;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.PointerType;

public interface CSystem extends Library {
    CSystem INSTANCE = (CSystem) Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), CSystem.class);

    int system(String cmd);
    
    FILE popen(String command, String type);
    
    String fgets(Memory memory, int size, FILE fp);
    
    int fputs(String content, FILE fp);
    
    int pclose(FILE fp);
    
    int setuid(int uid);
    
    int getuid();
    
    int getpid();
    
    int seteuid(int uid);
    
    int geteuid();
    

    public class FILE extends PointerType {
    }
}
