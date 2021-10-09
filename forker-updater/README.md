# Forker Updater

This is the core module for Forker Updater System. This extends Forker Wrapper to allow self extracting installers or archives for updateable applications with minimal configuration for Maven projects. 

## Goals

 * Support 3 major operating systems initially. WIP - Linux complete, Windows partial. Others TBG
 * As close to zero configuration as possible, either discovering or using convention over configuration. WIP - More analysis to discover system modules, quirks etc.
 * Non-destructive, installation and updates can rollback at any failure point.
 * Efficient. Only download files that need updating.
 * Consistent. A freshly installed application has exactly the same layout as files in the update repository. 
 * Familiar. Apps can be distributed as simple archives, or self extracting executables. WIP more operating system support and formats required.
 * No special update server required, it can be just an HTTP server.
 * Multiple toolkit support. Depending on needs, simple console, colour console or SWT UIs can be used. Further toolkits can be easily added (Swing planned, JavaFX may be ported back from Snake project).
 


