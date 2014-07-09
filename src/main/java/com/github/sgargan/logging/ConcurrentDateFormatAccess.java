package com.github.sgargan.logging;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * http://www.javacodegeeks.com/2010/07/java-best-practices-dateformat-in.html
 */
public class ConcurrentDateFormatAccess {

  private String format = "yyyy-MM-dd'T'HH:mm:ssXXX";

  private DateFormatSymbols symbols = DateFormatSymbols.getInstance();

  public ConcurrentDateFormatAccess(String format) {
    this.format = format;
  }

  public ConcurrentDateFormatAccess(String format, DateFormatSymbols symbols) {
    this.format = format;
    this.symbols = symbols;
  }

  private ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {

    @Override
    public DateFormat get() {
      return super.get();
    }

    @Override
    protected DateFormat initialValue() {
      return ConcurrentDateFormatAccess.this.symbols != null ?
          new SimpleDateFormat(format, symbols) : new SimpleDateFormat(format);
    }

    @Override
    public void remove() {
      super.remove();
    }

    @Override
    public void set(DateFormat value) {
      super.set(value);
    }

  };

  public String format(Date date) {
    return df.get().format(date);
  }

  public Date parse(String date) throws ParseException {
    return df.get().parse(date);
  }

}