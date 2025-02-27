package com.saucelabs.ci.sauceconnect;


import java.io.*;


public class DefaultProcessOutputPrinter implements ProcessOutputPrinter {
    public Runnable getStdoutPrinter(InputStream stdout, PrintStream printStream) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    printInfo(printStream, line);
                }
            } catch (IOException e) {
                printError(printStream, "Error reading process stdout: " + e.getMessage());
            }
        };
    }

    public Runnable getStderrPrinter(InputStream stderr, PrintStream printStream) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    printError(printStream, line);
                }
            } catch (IOException e) {
                printError(printStream, "Error reading process stderr: " + e.getMessage());
            }
        };
    }

    private void printInfo(PrintStream printStream, String line) {
        if (printStream != null) {
            printStream.println(line);
        }
    }

    private void printError(PrintStream printStream, String line) {
        if (printStream != null) {
            printStream.println(line);
        }
    }
}
