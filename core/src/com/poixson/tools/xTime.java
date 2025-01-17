package com.poixson.tools;

import static com.poixson.utils.Utils.GetMS;
import static com.poixson.utils.Utils.IsEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.poixson.exceptions.RequiredArgumentException;
import com.poixson.utils.StringUtils;


public class xTime {

	// stored value in ms
	public final AtomicLong value = new AtomicLong(0L);



	public static xTime Now() {
		return new xTime(GetMS());
	}



	public xTime() {
		this.set(0L);
	}
	public xTime(final long ms) {
		this.set(ms);
	}
	public xTime(final long value, final xTimeU xunit) {
		this.set(value, xunit);
	}
	public xTime(final long value, final TimeUnit unit) {
		this.set(value, unit);
	}
	public xTime(final String value) {
		this.set(value);
	}
	public xTime(final xTime time) {
		this.set(time);
	}



	// clone object
	@Override
	public xTime clone() {
		return new xTime(this);
	}



	// -------------------------------------------------------------------------------
	// get/set value



	// get value
	public long get(final xTimeU xunit) {
		if (xunit == null) throw new RequiredArgumentException("xunit");
		return xunit.convertTo( this.value.get() );
	}
	public long get(final TimeUnit unit) {
		if (unit == null)  throw new RequiredArgumentException("unit");
		final xTimeU xunit = xTimeU.GetUnit(unit);
		if (xunit == null) throw new RuntimeException("Unknown time unit: "+unit.toString());
		return xunit.convertTo( this.value.get() );
	}



	public long ms() {
		return this.value.get();
	}
	public long ticks(final long tick_length) {
		return this.value.get() / tick_length;
	}



	// set value
	public xTime set(final long ms) {
		this.value.set(ms);
		return this;
	}
	public xTime set(final long value, final xTimeU xunit) {
		if (xunit == null) throw new RequiredArgumentException("unit");
		this.value.set( xunit.convertTo(value) );
		return this;
	}
	public xTime set(final long value, final TimeUnit unit) {
		if (unit == null)      throw new RequiredArgumentException("unit");
		final xTimeU xunit = xTimeU.GetUnit(unit);
		if (xunit == null)     throw new RuntimeException("Unknown time unit: "+unit.toString());
		this.value.set( xunit.convertTo(value) );
		return this;
	}
	public xTime set(final String str) {
		if (IsEmpty(str)) throw new RequiredArgumentException("str");
		this.value.set( ParseToLong(str) );
		return this;
	}
	public xTime set(final xTime time) {
		if (time == null)      throw new RequiredArgumentException("time");
		this.value.set( time.ms() );
		return this;
	}



	// reset value to 0
	public xTime reset() {
		this.value.set(0L);
		return this;
	}



	// add time
	public void add(final long ms) {
		this.value.addAndGet(ms);
	}
	public void add(final long value, final xTimeU xunit) {
		if (xunit == null)     throw new RequiredArgumentException("unit");
		this.value.addAndGet( xunit.convertTo(value) );
	}
	public void add(final long value, final TimeUnit unit) {
		if (unit == null)      throw new RequiredArgumentException("unit");
		final xTimeU xunit = xTimeU.GetUnit(unit);
		if (xunit == null)     throw new RuntimeException("Unknown time unit: "+unit.toString());
		this.value.addAndGet( xunit.convertTo(value) );
	}
	public void add(final String str) {
		if (IsEmpty(str)) throw new RequiredArgumentException("str");
		this.value.addAndGet( ParseToLong(str) );
	}
	public void add(final xTime time) {
		if (time == null)      throw new RequiredArgumentException("time");
		this.value.addAndGet( time.ms() );
	}



	// -------------------------------------------------------------------------------
	// to/from string



	// parse time from string
	public static xTime Parse(final String str) {
		if (IsEmpty(str)) return null;
		return new xTime( ParseToLong(str) );
	}
	public static long ParseToLong(final String str) {
		if (IsEmpty(str)) throw new RequiredArgumentException("str");
		long value = 0L;
		// split into parts
		final StringBuilder bufValue = new StringBuilder();
		final StringBuilder bufUnit  = new StringBuilder();
		boolean moreNumbers = true;
		boolean decimal = false;
		for (char chr : (str+"  ").toCharArray()) {
			if (moreNumbers) {
				if (chr == '.') {
					decimal = true;
					bufValue.append(chr);
					continue;
				}
				if (chr == ',') continue;
				if (chr == '-') {
					if (bufValue.length() == 0) {
						bufValue.append('-');
						continue;
					}
				}
				if (Character.isDigit(chr)) {
					bufValue.append(chr);
					continue;
				}
			}
			if (chr == ' ') {
				if (moreNumbers) {
					moreNumbers = false;
				} else {
					final xTimeU xunit;
					if (bufUnit.length() == 0) {
						xunit = xTimeU.MS;
					} else {
						xunit = xTimeU.GetUnit( bufUnit.toString() );
						if (xunit == null) throw new NumberFormatException("Unknown unit: "+bufUnit.toString());
					}
					if (bufValue.length() > 0) {
						if (decimal) {
							final Double val = Double.valueOf( bufValue.toString() );
							if (val == null) throw new NumberFormatException("Invalid number: "+bufValue.toString());
							value += (long) xunit.convertFrom( val.doubleValue() );
						} else {
							final Long val = Long.valueOf( bufValue.toString() );
							if (val == null) throw new NumberFormatException("Invalid number: "+bufValue.toString());
							value += xunit.convertTo( val.longValue() );
						}
					}
					// reset buffers
					bufValue.setLength(0);
					bufUnit.setLength(0);
					moreNumbers = true;
					decimal = false;
				}
				continue;
			}
			if (Character.isLetter(chr)) {
				moreNumbers = false;
				bufUnit.append(chr);
				continue;
			}
			throw new NumberFormatException(
				(new StringBuilder())
				.append("Unknown character ").append(chr)
				.append(" in value: ").append(str)
				.toString()
			);
		}
		return value;
	}



	@Override
	public String toString() {
		return ToString(this.ms(), false, false);
	}
	public String toRoundedString() {
		return ToString(this.ms(), true, true);
	}
	public String toFullString() {
		return ToString(this.ms(), true, false);
	}



	public static String ToString(final xTime time,
			final boolean fullFormat, final boolean roundedFormat) {
		if (time == null) return null;
		return ToString(time.ms(), fullFormat, roundedFormat);
	}
	public static String ToString(final long ms,
			final boolean fullFormat, final boolean roundedFormat) {
		long tmp = ms;
		final List<String> result = new ArrayList<String>();
		for (final xTimeU xunit : xTimeU.xunits) {
			if (xTimeU.T.equals(xunit)) continue;
			if (xTimeU.W.equals(xunit)) continue;
			if (tmp < xunit.value) continue;
			final long val = Math.floorDiv(tmp, xunit.value);
			if (val == 0L) continue;
			tmp -= (val * xunit.value);
			final StringBuilder str = new StringBuilder();
			// full format
			if (fullFormat) {
				str.append(val)
					.append(' ')
					.append(xunit.name);
				if (val != 1L)
					str.append('s');
			// short format
			} else {
				str.append(val);
				if (xTimeU.MS.equals(xunit)) {
					str.append(xunit.name);
				} else {
					str.append(xunit.chr);
				}
			}
			if (roundedFormat) {
				return str.toString();
			}
			result.add(str.toString());
		}
		return StringUtils.MergeStrings( ' ', result.toArray(new String[0]) );
	}



}
