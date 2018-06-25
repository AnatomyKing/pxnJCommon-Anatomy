package com.poixson.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.poixson.app.xApp;
import com.poixson.app.xVars;
import com.poixson.logger.xLog;
import com.poixson.logger.xLogRoot;


public final class xAppUtils {
	private xAppUtils() {}

	public static enum AnsiColor {
		BLACK,
		RED,
		GREEN,
		YELLOW,
		BLUE,
		MAGENTA,
		CYAN,
		WHITE
	};



	public static Map<String, String> getStartupVars(final xApp app) {
		final Map<String, String> result =
				new LinkedHashMap<String, String>();
		result.put( "Pid",         Integer.toString(ProcUtils.getPid()) );
		result.put( "Version",     app.getVersion()                     );
		result.put( "Commit",      app.getCommitHashShort()             );
		result.put( "Running as",  System.getProperty("user.name")      );
		result.put( "Current dir", System.getProperty("user.dir")       );
		result.put( "java home",   System.getProperty("java.home")      );
		if (xVars.isDebug())
			result.put("Debug", "true");
		return result;
//TODO:
//		if (Utils.notEmpty(args)) {
//			out.println();
//			out.println(utilsString.addStrings(" ", args));
//		}
	}
	public static void DisplayStartupVars(final xApp app, final xLog log) {
		final Map<String, String> varsMap =
			getStartupVars(app);
		final Iterator<Entry<String, String>> it =
			varsMap.entrySet().iterator();
		final int maxLineSize =
			StringUtils.FindLongestLine(
				varsMap.keySet().toArray(new String[0])
			) + 1;
		final StringBuilder str = new StringBuilder();
		while (it.hasNext()) {
			final Entry<String, String> entry = it.next();
			final String key = entry.getKey();
			final String val = entry.getValue();
			str.setLength(0);
			str.append(key)
				.append(':')
				.append( StringUtils.Repeat(maxLineSize - key.length(), ' ') )
				.append(val);
			log.publish( str.toString() );
		}
	}



	public static void DisplayColors() {
		final StringBuilder[] lines = new StringBuilder[] {
			new StringBuilder(),
			new StringBuilder(),
			new StringBuilder()
		};
		for (final AnsiColor c : AnsiColor.values()) {
			final String str = c.toString().toLowerCase();
			final String name = StringUtils.PadCenter(7, str, ' ');
			lines[0].append("  @|")     .append(str).append("  ")      .append(name).append(" |@");
			lines[2].append("  @|bg_")  .append(str).append(",black  ").append(name).append(" |@");
			lines[1].append("  @|bold,").append(str).append("  ")      .append(name).append(" |@");
		}
		xLogRoot.Get()
			.publish(lines);
	}



}
