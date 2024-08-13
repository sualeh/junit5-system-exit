package us.fatehi;

public class SystemExit {

  public static void exit(final int exitCode) {
    throw new SystemExitException(exitCode, null);
  }
}
