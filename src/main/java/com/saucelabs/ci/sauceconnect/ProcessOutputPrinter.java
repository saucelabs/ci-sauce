package com.saucelabs.ci.sauceconnect;

import java.io.InputStream;
import java.io.PrintStream;

public interface ProcessOutputPrinter {
  Runnable getStdoutPrinter(InputStream stdout, PrintStream printStream);
  Runnable getStderrPrinter(InputStream stderr, PrintStream printStream);
}
