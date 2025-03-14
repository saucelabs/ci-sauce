package com.saucelabs.ci.sauceconnect;


import java.io.*;


public class DefaultProcessOutputPrinter implements ProcessOutputPrinter {
    public Runnable getStdoutPrinter(InputStream stdout, PrintStream printStream) {
        return () -> {
            if (stdout == null || printStream == null) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    print(printStream, "[sauceconnect] [stdout] " + line);
                }
            } catch (IOException e) {
                //
            }
        };
    }

    public Runnable getStderrPrinter(InputStream stderr, PrintStream printStream) {
        return () -> {
            if (stderr == null || printStream == null) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    print(printStream, "[sauceconnect] [stderr] " + line);
                }
            } catch (IOException e) {
                //
            }
        };
    }

    private void print(PrintStream printStream, String line) {
        if (printStream != null) {
            printStream.println(line);
        }
    }
}
