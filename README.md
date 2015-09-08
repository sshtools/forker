# Forker

Forker is a set of utilities and helpers for launching processes in the most efficient way

Used as a replacement to ProcessBuilder and Runtime.exec(), it uses a number of different techniques to reduce the memory impact of these operations, as well as a number of utilities for common process execution requirements, and facilities  to run processes in the background and with elevated privileges.

Depending on whether input, output, or I/O is needed (which should be provided as hint to the API), popen, system or a standard process will be used.

When both input and output to a process is required, and you want low fork costs, a separate optional daemon process may be used. The daemon waits for a command and set of arguments , and executes them. Because the virtual machine for the daemon is MUCH smaller, the impact of the fork is greatly lessened.  

