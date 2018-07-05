/* This is a test JavaScript wrapper configuration file.
 * You can mix code and configuration, but this script
 * must evaluate to (return) an object containing the configuration
 * properties (i.e. a map of all the same properties that might
 * be provided by other methods) 
 */

java.lang.System.out.println('Im configured by a script!');

/* Return the configuration object. The outer brackets are required. */

({
	main: 'com.nervepoint.forker.examples.WrappedTest',
	level: 'WARNING',
	arg: [
		'arg1',
		'arg2'
	]
})