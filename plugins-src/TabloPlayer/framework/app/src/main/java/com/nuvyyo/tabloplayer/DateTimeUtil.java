package com.nuvyyo.tabloplayer;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateTimeUtil
{
    public static interface Formatter<T> {
        public String format(T value);
    }
    
	public static final SimpleDateFormat	DATE_TIME_FORMAT_SHORT		= new SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault());
	public static final SimpleDateFormat	DATE_TIME_FORMAT_LONG		= new SimpleDateFormat("EEEE, MMMM d 'at' h:mm a", Locale.getDefault());
	
	public static final SimpleDateFormat	DATE_FORMAT_SHORT			= new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
	public static final SimpleDateFormat	DATE_FORMAT_LONG			= new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
	
	public static final SimpleDateFormat	TIME_FORMAT					= new SimpleDateFormat("h:mm a", Locale.getDefault());
	
	public static final SimpleDateFormat	PARSE_FORMAT_WITH_SECONDS 	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
	public static final SimpleDateFormat	PARSE_FORMAT_WITHOUT_SECONDS= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.getDefault());
	
	public static final Formatter<Calendar> TIME_FORMATTER = new Formatter<Calendar>()
	{
		@Override
		public String format(Calendar value)
		{
			if( value == null )
				return "";
			
			return TIME_FORMAT.format(value.getTime());
		}
	};
	
	public static final Formatter<Calendar> DATE_TIME_FORMATTER = new Formatter<Calendar>(){
		@Override
		public String format(Calendar value)
		{
			if( value == null )
				return "";
			
			SimpleDateFormat formatClone = (SimpleDateFormat)DATE_TIME_FORMAT_SHORT.clone();
			formatClone.setTimeZone(value.getTimeZone());
			return formatClone.format(value.getTime());
		}
	};
	
	public static final Formatter<Calendar> DATE_TIME_FORMATTER_LONG = new Formatter<Calendar>(){
		@Override
		public String format(Calendar value)
		{
			if( value == null )
				return "";
			
			return DATE_TIME_FORMAT_LONG.format(value.getTime());
		}
	};
	
	public static final Formatter<Calendar> DATE_FORMATTER = new Formatter<Calendar>(){
		@Override
		public String format(Calendar value)
		{
			if( value == null )
				return "";
			
			return DATE_FORMAT_SHORT.format(value.getTime());
		}
	};
	
	public static final Formatter<Calendar> DATE_FORMATTER_LONG = new Formatter<Calendar>(){
		@Override
		public String format(Calendar value)
		{
			if( value == null )
				return "";
			
			return DATE_FORMAT_LONG.format(value.getTime());
		}
	};
	
	public static final Formatter<Float> DURATION_FORMATTER_LONG = new Formatter<Float>(){
		@Override
		public String format(Float value)
		{
			if( value == null || value < 0.f )
				return "";
			
			// value is measured in seconds, so 1 hour = 3600
			int hours = (int)(value / 3600.f);
			int mins = (int)(value - (hours * 3600)) / 60;
			int secs = (int)(value - (hours * 3600.f) - (mins * 60.f));
			
			StringBuilder b = new StringBuilder();
			
			if( hours > 0 )
			{
				b.append(hours);
				b.append(" hour");
				if( hours > 1 )
					b.append("s");
				b.append(" ");
			}
			
			if( mins > 0 )
			{
				b.append(mins);
				b.append(" minute");
				if( mins > 1 )
					b.append("s");
				b.append(" ");
			}
			
			if( hours < 1 && mins < 1 )
			{
				b.append(secs);
				b.append(" second");
				if( secs != 1 )
					b.append("s");
			}
			
			return b.toString();
		}
	};

	/**
	 * Format the given millisecond duration as a timestamp. Supports negative durations.
	 * Example: milliseconds = 2128619 will be formatted as '35:28'
	 */
	public static final Formatter<Long> DURATION_FORMATTER_NUMERIC_SHORT = new Formatter<Long>(){
		@Override
		public String format(Long milliseconds)
		{
			// Handle negatives by detecting < 0, then re-applying '-' after formatting
			boolean isNegative = milliseconds < 0;
			
			// Make milliseconds positive for format
			milliseconds = Math.abs(milliseconds);
			
			long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(hours);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes);
			
			// Using a StringBuilder to assemble the return value.
			// NOTE: String.format() is explicitly not used because of the 
			// 		 performance penalty when compared to StringBuilders. (~10x)
			//
			//		 Since this formatter may be used very rapidly, it must 
			//		 be efficient.
			StringBuilder builder = new StringBuilder();
			
			// Hours ( only displayed if not 0 )
			if( hours > 0 )
			{
				builder.append(hours);
				builder.append(':');
			}
			
			// Minutes
			if( minutes < 10 )	// Leading 0
				builder.append('0');
			builder.append(minutes);
			builder.append(':');
			
			// Seconds
			if( seconds < 10 )	// Leading 0
				builder.append('0');
			builder.append(seconds);
			
			// Apply negative if necessary
			if( isNegative )
				builder.insert(0, '-');
			
			// All formatted! :)
			return builder.toString();
		}
	};

    public static class DateTimeIntervalPhraser {

        public static class Values {

            public static final Long SECOND = 1000l;
            public static final Long MINUTE = 60 * SECOND;
            public static final Long HOUR = 60 * MINUTE;
            public static final Long DAY = 24 * HOUR;
            public static final Long WEEK = 7 * DAY;
            public static final Long YEAR = 365 * DAY;
            public static final Long MONTH = YEAR / 12;

            private static final Long[] INTERVAL = new Long[]{
                    YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND
            };

            private static final String[] LABEL = new String[]{
                    "year", "month", "week", "day", "hour", "minute", "second"
            };

        }

        private static final String PREFIX_IN = "in ";
        private static final String PREFIX_WAS = "was ";
        private static final String SUFFIX_AGO = " ago";

        public static String from(final Calendar event, final boolean applyPrefix, final Long maxInterValue) throws DateTimeIntervalPhraserException {

            if (event == null)
                throw new DateTimeIntervalPhraserException("Event date cannot be null....");

            if (!Arrays.asList(Values.INTERVAL).contains(maxInterValue))
                throw new DateTimeIntervalPhraserException("MaxIntervalValue must be in DateTimeIntervalPhraser.Values.INTERVAL");

            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("utc"));

            if (event.before(now)) return formatPastDate(event, now, applyPrefix, maxInterValue);
            else return formatFutureDate(event, now, applyPrefix, maxInterValue);
        }

        private static String formatPastDate(Calendar event, Calendar now, boolean applyPrefix, final long max) {
            return getUnitAndValue(now.getTimeInMillis() - event.getTimeInMillis(), applyPrefix ? PREFIX_WAS : "", applyPrefix ? SUFFIX_AGO : "", max);
        }

        private static String formatFutureDate(Calendar event, Calendar now, boolean applyPrefix, final long max) {
            return getUnitAndValue(event.getTimeInMillis() - now.getTimeInMillis(), applyPrefix ? PREFIX_IN : "", "", max);
        }

        private static String getUnitAndValue(final long diff, final String prefix, final String suffix, final long max) {
            for (int i = 0; i < Values.INTERVAL.length; ++i) {
                final long interval = Values.INTERVAL[i];
                final long val = diff / interval;

                if (interval == max || val >= 1)
                    return getFormatted(Values.LABEL[i], val, prefix, suffix);
            }

            return "";
        }

        private static String getFormatted(final String unit, final long count, final String prefix, final String suffix) {
            return prefix + String.valueOf(count) + " " + unit + (count != 1 ? "s" : "") + suffix;
        }

        public static class DateTimeIntervalPhraserException extends RuntimeException {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public DateTimeIntervalPhraserException(String detailMessage) {
                super(detailMessage);
            }
        }
    }

}
