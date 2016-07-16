package gitCommitStatistics.workers;

import gitCommitStatistics.report.GeneralReport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProccessManager {
    private static final boolean ARRAYLIST_LIMITED = true;
    private static final int MAX_ARRAYLIST_SIZE = 10000;
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
            List<String> outLines = new ArrayList<>();
            boolean killedProcess = false;
            boolean continueWhile = true;
            if (withReturn) {
                while ((line = b.readLine()) != null && continueWhile) {
                    if (ARRAYLIST_LIMITED && outLines.size() >= MAX_ARRAYLIST_SIZE) {
                        continueWhile = false;
                    } else {
                        outLines.add(line);
                    }
                }
                if (ARRAYLIST_LIMITED && outLines.size() >= MAX_ARRAYLIST_SIZE) {
                    p.destroy();
                    killedProcess = true;
                }
                p.waitFor();
            }

            for (int lineIndex = 0; lineIndex < outLines.size(); ++lineIndex) {
                output += outLines.get(lineIndex) + System.getProperty("line.separator");
            }

            if (!killedProcess) {
                b = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                int counter = 0;
                continueWhile = true;
                while ((line = b.readLine()) != null && continueWhile) {
                    if (ARRAYLIST_LIMITED && counter >= MAX_ARRAYLIST_SIZE) {
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
