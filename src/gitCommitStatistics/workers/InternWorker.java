package gitCommitStatistics.workers;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import gitCommitStatistics.directoryManager.DirectoryManager;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.report.GeneralReport;

public class InternWorker implements Callable<ArrayList<ArrayList<String>>> {
    
    protected String workerId;
    protected String resultDirectory;
    protected String filePathAux;
    protected String path;

    public InternWorker(String workerId, String filePath) {
        this.workerId = workerId;
        path = DirectoryManager.WORKERS_PATH + System.getProperty("file.separator") + workerId;
        this.resultDirectory = path + System.getProperty("file.separator") + "results-" + workerId;
        this.filePathAux = path + System.getProperty("file.separator") + filePath;
    }

    @Override
    public ArrayList<ArrayList<String>> call() throws Exception {
        String filename = filePathAux.substring(filePathAux.lastIndexOf(
                System.getProperty("file.separator")) + 1);
        File file = new File(filePathAux);
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        ArrayList<String> pathToArray = new ArrayList<String>();
        pathToArray.add(filename);
        result.add(pathToArray);
        if (file.exists()) {
            if (createSRCML(filePathAux)) {
                result.add(executeMacros(filePathAux));
                return result;
            }
        }
        
        return null;
    }

    public String getPath() {
        return path;
    }

    public boolean createSRCML(String item) {
        String filename = item.substring(item.lastIndexOf(System.getProperty("file.separator")) + 1),
                fileResult = resultDirectory + System.getProperty("file.separator") + filename.replace(".c", ".xml");
        try {
            File directory = new File(resultDirectory);
            if (!directory.exists()) {
                if (!directory.mkdir()) {
                    return false;
                }
            }
        } catch (Exception e) {
            GeneralReport.getInstance()
                    .reportError("Não foi possível criar o diretório de resultados para o woker: " + workerId);
        }
        //String command = "./"+PropertiesManager.getPropertie("path.src2srcml") + " " + item + " " + SRC2SRCML_WRITE_SYMBOL +
        //      " " + resultDirectory + System.getProperty("file.separator") + filename.replace(".c", ".xml");
        String command = PropertiesManager.getPropertie("path.src2srcml") + " " + item;
        
        ProccessManager proccessManager = new ProccessManager(command, true);
        return (!proccessManager.hasError() && DirectoryManager.getInstance().writeFile(fileResult, proccessManager.getOutput()));
    }

    public ArrayList<String> executeMacros(String filePath) {
        String filename = filePath.substring(filePath.lastIndexOf(System.getProperty("file.separator")) + 1),
                fileResult = resultDirectory + System.getProperty("file.separator") + filename.replace(".c", ".xml");
        String command = PropertiesManager.getPropertie("path.dmacros") + " " + fileResult;
        // ProccessManager proccessManager = new ProccessManager(command, true);
        ProccessManager proccessManager = new ProccessManager(command, true);
        if (proccessManager.hasError()) {
            DirectoryManager.getInstance().deleteFile(new File(fileResult));
            return null;
        } else {
            String dmacrosOutput = proccessManager.getOutput();
            
            ArrayList<String> result = transformDmacrosOutput(dmacrosOutput);
            DirectoryManager.getInstance().deleteFile(new File(fileResult));
            return result;
        }
    }

    protected ArrayList<String> transformDmacrosOutput(String dmacrosOutput) {
        ArrayList<String> result = new ArrayList<String>();
        
        dmacrosOutput = dmacrosOutput.substring(dmacrosOutput.lastIndexOf("["));
        dmacrosOutput = dmacrosOutput.replace("[", "").replace("]", "");
        String[] tempArray = dmacrosOutput.split(",");
        result.add(tempArray[0]);
        result.add(tempArray[1]);
        result.add("0");
        
        return result;
    }
}
