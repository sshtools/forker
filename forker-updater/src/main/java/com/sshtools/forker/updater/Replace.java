package com.sshtools.forker.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Replace {
	public static final String DEFAULT_VARIABLE_REPLACEMENT = "\\$\\{[^\\]]*\\}";

	public interface ExceptionHandler {
		void exception(Exception exception) throws Exception;
	}

	public interface Replacer {
		String replace(Pattern pattern, Matcher matcher, String replacementPattern) throws Exception;
	}

	private final StringBuffer inputBuffer = new StringBuffer();
	private final StringBuffer workBuffer = new StringBuffer();
	private List<Replacement> replacementsList = new ArrayList<>();
	private boolean caseSensitive = true;
	private boolean dotAll = false;
	private String charset;
	private ExceptionHandler exceptionHandler;

	public ExceptionHandler exceptionHandler() {
		return exceptionHandler;
	}

	public Replace exceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	public Replace encoding(String charset) {
		this.charset = charset;
		return this;
	}

	public Replace dotAll(boolean dotAll) {
		this.dotAll = dotAll;
		return this;
	}

	public Replace caseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		return this;
	}

	public synchronized Replace pattern(String patternText, Replacer replacer) {
		pattern(patternText, replacer, null);
		return this;
	}

	public synchronized Replace pattern(String patternText, Replacer replacer, String replacementPattern) {
		Pattern pattern = Pattern.compile(patternText,
				(caseSensitive ? 0 : Pattern.CASE_INSENSITIVE) | (dotAll ? 0 : Pattern.DOTALL));
		replacementsList.add(new Replacement(replacer, pattern, replacementPattern));
		return this;
	}

	public synchronized String replace(String input) {
		Iterator<Replacement> it = replacementsList.iterator();

		inputBuffer.setLength(0);
		inputBuffer.append(input);

		workBuffer.setLength(0);
		workBuffer.ensureCapacity(input.length());

		while (it.hasNext()) {
			Replacement op = (Replacement) it.next();

			try {
				replaceInto(op.pattern, op.replacePattern, op.replacer, inputBuffer, workBuffer);
			} catch (Exception t) {
				if (exceptionHandler == null) {
					if (t instanceof RuntimeException)
						throw (RuntimeException) t;
					else
						throw new IllegalStateException("Script failed.", t);
				} else {
					try {
						exceptionHandler.exception(t);
					} catch (RuntimeException re) {
						throw re;
					} catch (Exception e) {
						throw new IllegalStateException("Script failed.", e);
					}
				}
			} finally {
			}
			inputBuffer.setLength(0);
			inputBuffer.append(workBuffer);
		}
		return (inputBuffer.toString());
	}

	public long replace(InputStream in, OutputStream out) throws IOException {
		StringBuffer str = new StringBuffer(4096);
		byte[] buf = new byte[32768];
		int read;
		while ((read = in.read(buf)) > -1) {
			str.append(charset == null ? new String(buf, 0, read) : new String(buf, 0, read, charset));
		}
		byte[] b = charset == null ? replace(str.toString()).getBytes() : replace(str.toString()).getBytes(charset);
		out.write(b);
		return b.length;
	}

	private void replaceInto(Pattern pattern, String replacementPattern, Replacer replacer, StringBuffer input,
			StringBuffer work) throws Exception {
		work.ensureCapacity(input.length());
		work.setLength(0);
		Matcher m = pattern.matcher(input);
		while (m.find()) {
			String repl = replacer.replace(pattern, m, replacementPattern);
			if (repl != null) {
				m.appendReplacement(work, escapeForRegexpReplacement(repl));
			}
		}
		m.appendTail(work);
	}

	public static String escapeForRegexpReplacement(String repl) {
		StringBuffer buf = new StringBuffer(repl.length());
		char ch;
		int len = repl.length();
		for (int i = 0; i < len; i++) {
			ch = repl.charAt(i);
			if (ch == '\\') {
				buf.append(ch);
			} else if (ch == '$') {
				buf.append('\\');
			}
			buf.append(ch);
		}
		return buf.toString();
	}

	class Replacement {
		Pattern pattern;
		Replacer replacer;
		String replacePattern;

		Replacement(Replacer replacer, Pattern pattern, String replacePattern) {
			this.replacer = replacer;
			this.pattern = pattern;
			this.replacePattern = replacePattern;
		}
	}

}