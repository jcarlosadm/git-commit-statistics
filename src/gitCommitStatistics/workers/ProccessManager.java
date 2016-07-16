package gitCommitStatistics.workers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import gitCommitStatistics.report.GeneralReport;

public class ProccessManager {
    // TODO constant ARRAYLIST_LIMITED
    private static final boolean NUMBER_OF_LINES_LIMITED = true;
    // TODO constant MAX_ARRAYLIST_SIZE
    private static final int MAX_NUMBER_OF_LINES = 10000;
    private String output;
    private String error;

    public ProccessManager(String command, boolean withReturn) {
        BufferedReader b;
        try {
            output = "";
            error = "";
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(command);
            String line = "";
            b = new BufferedReader(new InputStreamReader(p.getInputStream()));
            boolean killedProcess = false;
            boolean continueWhile = true;
            int counter = 0;
            if (withReturn) {
                while ((line = b.readLine()) != null && continueWhile) {
                    if (NUMBER_OF_LINES_LIMITED && counter >= MAX_NUMBER_OF_LINES) {
                        continueWhile = false;
                        p.destroy();
                        killedProcess = true;
                    } else {
                        output += line + System.getProperty("line.separator");
                        ++counter;
                    }
                }
                p.waitFor();
            }

            if (!killedProcess) {
                b = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                counter = 0;
                continueWhile = true;
                while ((line = b.readLine()) != null && continueWhile) {
                    if (NUMBER_OF_LINES_LIMITED && counter >= MAX_NUMBER_OF_LINES) {
                        continueWhile = false;
                    } else {
                        error += line + System.getProperty("line.separator");
                        ++counter;
                    }
                }
            }

            b.close();
        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível executar o comando: " + command);
        }

    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        // return output.isEmpty() && !error.isEmpty();
        return (output.isEmpty() || !error.isEmpty());
    }
}
