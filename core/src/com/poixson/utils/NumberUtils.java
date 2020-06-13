package com.poixson.utils;

import java.text.DecimalFormat;
import java.util.Random;

import com.poixson.tools.Keeper;


public final class NumberUtils {
	private NumberUtils() {}
	static { Keeper.add(new NumberUtils()); }

	/**
	 * Max valid tcp/udp port number.
	 */
	public static final int MAX_PORT = 65535;



	// true;
	public static final String[] TRUE_VALUES = new String[] {
		"true",
		"enabled",
		"yes",
		"on",
	};
	public static final char[] T_VALUES = new char[] {
		'1',
		't',
		'e',
		'y'
	};
	// false
	public static final String[] FALSE_VALUES = new String[] {
		"false",
		"disabled",
		"no",
		"off"
	};
	public static final char[] F_VALUES = new char[] {
		'0',
		'f',
		'd',
		'n'
	};



	public static boolean EqualsExact(final Integer a, final Integer b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return a.intValue() == b.intValue();
	}
	public static boolean EqualsExact(final Boolean a, final Boolean b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return ( a.booleanValue() == b.booleanValue() );
	}
	public static boolean EqualsExact(final Byte a, final Byte b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return ( a.byteValue() == b.byteValue() );
	}
	public static boolean EqualsExact(final Short a, final Short b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return ( a.shortValue() == b.shortValue() );
	}
	public static boolean EqualsExact(final Long a, final Long b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return ( a.longValue() == b.longValue() );
	}
	public static boolean EqualsExact(final Double a, final Double b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return ( a.doubleValue() == b.doubleValue() );
	}
	public static boolean EqualsExact(final Float a, final Float b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return ( a.floatValue() == b.floatValue() );
	}



	public static int SafeToInt(final long value) {
		// < int min
		if ( value < ((long)Integer.MIN_VALUE) )
			return Integer.MIN_VALUE;
		// > int max
		if ( value > ((long)Integer.MAX_VALUE) )
			return Integer.MAX_VALUE;
		// within int range
		return (int) value;
	}



	// parse number
	public static Integer ToInteger(final String value) {
		if (Utils.isEmpty(value)) return null;
		try {
			return new Integer(Integer.parseInt(value));
		} catch (NumberFormatException ignore) {}
		return null;
	}
	public static int ToInteger(final String value, final int def) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignore) {}
		return def;
	}

	// parse byte
	public static Byte ToByte(final String value) {
		if (Utils.isEmpty(value)) return null;
		try {
			return new Byte(Byte.parseByte(value));
		} catch (NumberFormatException ignore) {}
		return null;
	}
	public static byte ToByte(final String value, final byte def) {
		try {
			return Byte.parseByte(value);
		} catch (NumberFormatException ignore) {}
		return def;
	}

	// parse short
	public static Short ToShort(final String value) {
		if (Utils.isEmpty(value)) return null;
		try {
			return new Short(Short.parseShort(value));
		} catch (NumberFormatException ignore) {}
		return null;
	}
	public static short ToShort(final String value, final short def) {
		try {
			return Short.parseShort(value);
		} catch (NumberFormatException ignore) {}
		return def;
	}

	// parse long
	public static Long ToLong(final String value) {
		if (Utils.isEmpty(value)) return null;
		try {
			return new Long(Long.parseLong(value));
		} catch (NumberFormatException ignore) {}
		return null;
	}
	public static long ToLong(final String value, final long def) {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException ignore) {}
		return def;
	}

	// parse double
	public static Double ToDouble(final String value) {
		if (Utils.isEmpty(value)) return null;
		try {
			return new Double(Double.parseDouble(value));
		} catch (NumberFormatException ignore) {}
		return null;
	}
	public static double ToDouble(final String value, final double def) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ignore) {}
		return def;
	}

	// parse float
	public static Float ToFloat(final String value) {
		if (Utils.isEmpty(value)) return null;
		try {
			return new Float(Float.parseFloat(value));
		} catch (NumberFormatException ignore) {}
		return null;
	}
	public static float ToFloat(final String value, final float def) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException ignore) {}
		return def;
	}

	// parse boolean
	public static Boolean ToBoolean(final String value) {
		if (Utils.isEmpty(value)) return null;
		final String val = value.trim().toLowerCase();
		for (final String v : TRUE_VALUES) {
			if (val.equals(v))
				return Boolean.TRUE;
		}
		for (final String v : FALSE_VALUES) {
			if (val.equals(v))
				return Boolean.FALSE;
		}
		final char chr = val.charAt(0);
		for (final char c : T_VALUES) {
			if (chr == c)
				return Boolean.TRUE;
		}
		for (final char c : F_VALUES) {
			if (chr == c)
				return Boolean.FALSE;
		}
		return null;
	}
	public static boolean ToBoolean(final String value, final boolean def) {
		final Boolean bool = ToBoolean(value);
		if (bool == null) return def;
		return bool.booleanValue();
	}



	// is number
	public static boolean IsNumeric(final String value) {
		if (Utils.isEmpty(value)) return false;
		return (ToLong(value) != null);
	}
	// is boolean
	public static boolean IsBoolean(final String value) {
		return (ToBoolean(value) != null);
	}



	// formatDecimal("0.00", double)
	public static String FormatDecimal(final String format, final double value) {
		return (new DecimalFormat(format)
				.format(value));
	}
	// formatDecimal("0.00", float)
	public static String FormatDecimal(final String format, final float value) {
		return (new DecimalFormat(format)
				.format(value));
	}



	public static double Round(final double value, final double product) {
		final double val = Math.round( value / product );
		return val * product;
	}
	public static double Floor(final double value, final double product) {
		final double val = Math.floor( value / product );
		return val * product;
	}
	public static double Ceil(final double value, final double product) {
		final double val = Math.ceil( value / product );
		return val * product;
	}



	public static int Round(final int value, final int product) {
		return (int)
			Round( (double) value, (double) product );
	}
	public static int Floor(final int value, final int product) {
		return (int)
			Floor( (double) value, (double) product );
	}
	public static int Ceil(final int value, final int product) {
		return (int)
			Ceil( (double) value, (double) product );
	}



	public static long Round(final long value, final int product) {
		return (long)
			Round( (double) value, (double) product );
	}
	public static long Floor(final long value, final int product) {
		return (long)
			Floor( (double) value, (double) product );
	}
	public static long Ceil(final long value, final int product) {
		return (long)
			Ceil( (double) value, (double) product );
	}



	// min/max value
	public static int MinMax(final int value, final int min, final int max) {
		if (min   > max) throw new IllegalArgumentException("min cannot be greater than max");
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	public static byte MinMax(final byte value, final byte min, final byte max) {
		if (min   > max) throw new IllegalArgumentException("min cannot be greater than max");
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	public static short MinMax(final short value, final short min, final short max) {
		if (min   > max) throw new IllegalArgumentException("min cannot be greater than max");
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	public static long MinMax(final long value, final long min, final long max) {
		if (min   > max) throw new IllegalArgumentException("min cannot be greater than max");
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	public static double MinMax(final double value, final double min, final double max) {
		if (min   > max) throw new IllegalArgumentException("min cannot be greater than max");
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	public static float MinMax(final float value, final float min, final float max) {
		if (min   > max) throw new IllegalArgumentException("min cannot be greater than max");
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}



	// is within range
	public static boolean IsMinMax(final int value, final int min, final int max) {
		return (value == MinMax(value, min, max));
	}
	public static boolean IsMinMax(final byte value, final byte min, final byte max) {
		return (value == MinMax(value, min, max));
	}
	public static boolean IsMinMax(final short value, final short min, final short max) {
		return (value == MinMax(value, min, max));
	}
	public static boolean IsMinMax(final long value, final long min, final long max) {
		return (value == MinMax(value, min, max));
	}
	public static boolean IsMinMax(final double value, final double min, final double max) {
		return (value == MinMax(value, min, max));
	}
	public static boolean IsMinMax(final float value, final float min, final float max) {
		return (value == MinMax(value, min, max));
	}



	// random number
	public static int GetRandom(final int minNumber, final int maxNumber) {
		final Random gen = new Random(Utils.getSystemMillis());
		return gen.nextInt(maxNumber - minNumber) + minNumber;
	}
	// random number (not last)
	public static int getNewRandom(final int minNumber, final int maxNumber, final int oldNumber) {
		if (minNumber == maxNumber)
			return minNumber;
		if ((maxNumber - minNumber) == 1) {
			return (
				oldNumber == minNumber
				? maxNumber
				: minNumber
			);
		}
		int newNumber;
		for (int i=0; i<100; i++) {
			newNumber = GetRandom(minNumber, maxNumber);
			if (newNumber != oldNumber) return newNumber;
		}
		throw new IllegalAccessError("Failed to generate a random number");
	}



}
