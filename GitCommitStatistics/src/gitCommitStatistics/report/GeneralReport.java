package gitCommitStatistics.report;

import java.util.ArrayList;

public class GeneralReport {
    private static GeneralReport instance;
    private ArrayList<String> errorList;
    private ArrayList<String> infoList;
    private GeneralReport() {
        infoList = new ArrayList<String>();
        errorList = new ArrayList<String>();
    }
    public static GeneralReport getInstance() {
        if (instance == null) {
            instance = new GeneralReport();
        }
        return instance;
    }
    public void reportError(String error) {
        System.out.println("[error]" + error);
        errorList.add(error);
    }
    public void reportInfo(String info) {
        System.out.println(info);
        infoList.add("[info]" + info);
    }
}
