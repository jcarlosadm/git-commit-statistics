package gitCommitStatistics.workers;


import gitCommitStatistics.report.GeneralReport;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ProccessManager {
    private String output;
    private String error;
    public ProccessManager(String command, boolean withReturn) {
        BufferedReader b;
        try {
            output = "";
            error = "";
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(command);
            if(withReturn) {
                p.waitFor();
            }
            b = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";

            while ((line = b.readLine()) != null) {
                output += line + System.getProperty("line.separator");
            }

            b = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line = b.readLine()) != null) {
                error += line + System.getProperty("line.separator");
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
        //return output.isEmpty() && !error.isEmpty();
        return (output.isEmpty() || !error.isEmpty());
    }
}
