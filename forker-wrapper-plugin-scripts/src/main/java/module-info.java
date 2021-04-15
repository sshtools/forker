module com.sshtools.forker.wrapper.plugin.scripts {
	requires transitive com.sshtools.forker.wrapper;
	requires java.scripting;
	provides com.sshtools.forker.wrapper.WrapperPlugin with com.sshtools.forker.wrapper.plugin.scripts.ScriptWrapperPlugin;
}