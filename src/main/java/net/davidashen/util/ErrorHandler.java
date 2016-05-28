/* $Id: ErrorHandler.java,v 1.2 2003/08/20 16:12:32 dvd Exp $ */
package net.davidashen.util;

/** Generic Error Handler interface */
public interface ErrorHandler {
 /** debug 
  @param guard a string used to display debugging information selectively
  @param s debugging information */
  public void debug(String guard,String s);
 /** say something
  @param s the thing to say */
  public void info(String s);
 /** report a warning
  @param s explanation */
  public void warning(String s);
 /** report an error
  @param s explanation */
  public void error(String s);
 /** report an error caused by a caught exception;
  @param s explanation
  @param e exception */
  public void exception(String s,Exception e);
 /** check whether a guard is turned on */
  public boolean isDebugged(String guard);

  public static class NotSetException  extends RuntimeException {
    public NotSetException() {}
    public NotSetException(Exception e) {super(e.toString());}
    public NotSetException(String s) {super(s);}

  }

 /** default error handler purpousefully throws an exception each time it is called */
  public static final ErrorHandler DEFAULT=new ErrorHandler() {
    public void debug(String guard,String s) {throw new NotSetException(s);}
    public void info(String s) {throw new NotSetException(s);}
    public void warning(String s) {throw new NotSetException(s);}
    public void error(String s) {throw new NotSetException(s);}
    public void exception(String s,Exception e) {throw new NotSetException(s);}
    public boolean isDebugged(String guard) {throw new NotSetException(guard);}
  };
}

/*
* $Log: ErrorHandler.java,v $
* Revision 1.2  2003/08/20 16:12:32  dvd
* java docs
*
* Revision 1.1  2003/08/17 21:55:24  dvd
* Hyphenator.java is a java program
*
*/
