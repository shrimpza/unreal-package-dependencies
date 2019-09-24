package net.shrimpworks.unreal.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLI {

	private static final String OPTION_PATTERN = "--([a-zA-Z0-9-_]+)=(.+)?";

	private final String[] args;
	private final Map<String, String> options;

	public CLI(String[] args, Map<String, String> options) {
		this.args = args;
		this.options = options;
	}

	public static CLI parse(Map<String, String> defOptions, String... args) {
		final List<String> parsedArgs = new ArrayList<>();
		final Map<String, String> props = new HashMap<>();

		// populate default options
		props.putAll(defOptions);

		Pattern optPattern = Pattern.compile(OPTION_PATTERN);

		for (String arg : args) {
			Matcher optMatcher = optPattern.matcher(arg);

			if (optMatcher.matches()) {
				props.put(optMatcher.group(1), optMatcher.group(2) == null ? "" : optMatcher.group(2));
			} else {
				parsedArgs.add(arg);
			}
		}

		return new CLI(parsedArgs.toArray(new String[0]), props);
	}

	public String option(String key, String defaultValue) {
		return options.getOrDefault(key, defaultValue);
	}

	public String[] args() {
		return args;
	}

}
