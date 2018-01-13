package me.kenzierocks.ourtube;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class Log {

	public static Logger get() {
		return LoggerFactory.getLogger(getCallingMethod());
	}

	private static String getCallingMethod() {
		return Throwables.lazyStackTrace(new Throwable()).get(2).getClassName();
	}

}
