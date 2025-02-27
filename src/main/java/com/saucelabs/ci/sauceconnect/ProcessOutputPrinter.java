package com.saucelabs.ci.sauceconnect;


import java.io.*;
import java.util.function.Consumer;


public interface ProcessOutputPrinter {
    Runnable getStdoutPrinter(InputStream stdout, PrintStream printStream);
    Runnable getStderrPrinter(InputStream stderr, PrintStream printStream);
}
