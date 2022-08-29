package com.sshtools.forker.updater;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DesktopShortcut {

	public enum Type {
		APPLICATION, AUTOSTART, LINK, DIRECTORY;
	}

	private final String id;
	private String name;
	private final Map<Locale, String> names = new HashMap<>();
	private String comment;
	private final Map<Locale, String> comments = new HashMap<>();
	private String icon;
	private final Map<Locale, String> icons = new HashMap<>();
	private boolean autoStart;
	private String executable;
	private final List<String> arguments = new ArrayList<>();
	private final List<String> categories = new ArrayList<>();
	private final List<String> keywords = new ArrayList<>();
	private Type type = Type.APPLICATION;

	public DesktopShortcut(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public DesktopShortcut name(String name) {
		this.name = name;
		return this;
	}

	public String comment() {
		return comment;
	}

	public DesktopShortcut comment(String comment) {
		this.comment = comment;
		return this;
	}

	public String icon() {
		return icon;
	}

	public DesktopShortcut icon(String icon) {
		this.icon = icon;
		return this;
	}

	public Map<Locale, String> names() {
		return names;
	}

	public Map<Locale, String> comments() {
		return comments;
	}

	public Map<Locale, String> icons() {
		return icons;
	}

	public boolean autoStart() {
		return autoStart;
	}

	public DesktopShortcut autoStart(boolean autoStart) {
		this.autoStart = autoStart;
		return this;
	}

	public String executable() {
		return executable;
	}

	public DesktopShortcut executable(String executable) {
		this.executable = executable;
		return this;
	}

	public List<String> arguments() {
		return arguments;
	}

	public DesktopShortcut addCategories(String... categories) {
		this.categories.addAll(Arrays.asList(categories));
		return this;
	}

	public DesktopShortcut addKeywords(String... keywords) {
		this.keywords.addAll(Arrays.asList(keywords));
		return this;
	}

	public DesktopShortcut addArguments(String... arguments) {
		this.arguments.addAll(Arrays.asList(arguments));
		return this;
	}

	public DesktopShortcut addName(Locale locale, String name) {
		this.names.put(locale, name);
		return this;
	}

	public DesktopShortcut addComment(Locale locale, String comment) {
		this.comments.put(locale, comment);
		return this;
	}

	public DesktopShortcut addIcon(Locale locale, String icon) {
		this.icons.put(locale, icon);
		return this;
	}

	public List<String> categories() {
		return categories;
	}

	public List<String> keywords() {
		return keywords;
	}

	public Type type() {
		return type;
	}

	public DesktopShortcut type(Type type) {
		this.type = type;
		return this;
	}

}
