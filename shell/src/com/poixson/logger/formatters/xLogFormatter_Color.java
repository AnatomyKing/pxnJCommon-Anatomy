/*
package com.poixson.logger.formatters;

import com.poixson.logger.xLevel;
import com.poixson.logger.records.xLogRecord_Msg;
import com.poixson.utils.StringUtils;
import com.poixson.utils.Utils;


public class xLogFormatter_Color extends xLogFormatter {



	public xLogFormatter_Color() {
		super();
	}



	@Override
	public String[] formatMessage(final xLogRecord_Msg record) {
		final xLevel level = record.getLevel();
		// publish plain message
		if (level == null) {
			return record.getLines();
		}
		// [[ title ]]
		if (xLevel.TITLE.equals(level)) {
			return
				this.genTitle(
					record,
					" @|FG_MAGENTA [[|@ @|FG_CYAN ",
					"|@ @|FG_MAGENTA ]]|@ "
				);
		}
		// format message lines
		final String[] lines = record.getLines();
		final String[] result = new String[ lines.length ];
		for (int index = 0; index < lines.length; index++) {
			// timestamp [level] [crumbs] message
			result[index] =
				StringUtils.MergeStrings(
					' ',
					// timestamp
					this.genTimestamp(
						record,
						"D yyyy-MM-dd HH:mm:ss",
						"@|FG_WHITE ",
						"|@"
					),
					// [level]
					this.genLevelColored(record),
					// [crumbs]
					this.genCrumbsColored(record),
					// message
					lines[index]
				);
		}
		return result;
	}



	// -------------------------------------------------------------------------------
	// generate parts



	// [level]
	protected String genLevelColored(final xLogRecord_Msg record) {
		return (new StringBuilder())
			.append("@|FG_BLACK,BOLD [|@@|")
			.append( this.getLevelColor(record.level) )
			.append(' ')
			.append( StringUtils.PadCenter(7, record.getLevelStr(), ' ') )
			.append("|@@|FG_BLACK,BOLD ]|@")
			.toString();
	}
	protected String getLevelColor(final xLevel level) {
		if (level == null)
			return "FG_BLACK,BOLD";
		// all, finest, finer, fine
		if (level.isLoggable(xLevel.FINE))
			return "FG_BLACK,BOLD";
		// info
		if (level.isLoggable(xLevel.INFO))
			return "FG_CYAN";
		// warning
		if (level.isLoggable(xLevel.WARNING))
			return "FG_RED";
		// severe
		if (level.isLoggable(xLevel.SEVERE))
			return "FG_RED,BOLD";
		// fatal
		if (level.isLoggable(xLevel.FATAL))
			return "FG_RED,BOLD,UNDERLINE";
		// stdout
		if (level.isLoggable(xLevel.STDOUT))
			return "FG_GREEN";
		// stderr
		if (level.isLoggable(xLevel.STDERR))
			return "FG_YELLOW";
		// off
		return "FG_BLACK,BOLD";
	}



	// crumbs
	protected String genCrumbsColored(final xLogRecord_Msg record) {
		final String crumbStr = super.genCrumbs(record, "[", "] [", "]");
		if (Utils.isBlank(crumbStr))
			return "";
		return (new StringBuilder())
			.append("@|FG_BLACK,BOLD ")
			.append(crumbStr)
			.append("|@")
			.toString();
	}



}
*/
