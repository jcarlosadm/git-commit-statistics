package gitCommitStatistics.workers;


import gitCommitStatistics.directoryManager.DirectoryManager;
import gitCommitStatistics.git.GitManager;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.report.GeneralReport;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Worker implements Callable<Hashtable<String, Hashtable<String, ArrayList<String>>>> {
    private String hashId;
    private String workerId;
    private String path;
    private String resultDirectory;
    private ArrayList<String> filesToAnalise;
    private List<Callable<ArrayList<ArrayList<String>>>> interns;
    private ExecutorService executorService;
    private int numberOfInterns;
    public Worker(String workerId, String hashId, ArrayList<String> filesToAnalise) {
        this.filesToAnalise = filesToAnalise;
        this.workerId = workerId;
        this.hashId = hashId;
        this.path = DirectoryManager.WORKERS_PATH + System.getProperty("file.separator") + workerId;
        this.resultDirectory = path + System.getProperty("file.separator") + "results-" + workerId;
        if (!DirectoryManager.getInstance().testIfExists(path)) {
            DirectoryManager.getInstance().cloneProject(workerId);
        }
        GitManager gitManager = new GitManager();
        gitManager.setRepository(path);
        gitManager.checkout(hashId);
        numberOfInterns = Integer.parseInt(PropertiesManager.getPropertie("number.of.interns"));
        interns = new ArrayList<Callable<ArrayList<ArrayList<String>>>>();
        executorService = Executors.newFixedThreadPool(numberOfInterns);
    }
    @Override
    public Hashtable<String, Hashtable<String, ArrayList<String>>> call() {
        Hashtable<String, ArrayList<String>> resultsTemp = new Hashtable<String, ArrayList<String>>();
        Hashtable<String, Hashtable<String, ArrayList<String>>> result = new Hashtable<String, Hashtable<String, ArrayList<String>>>();

        for(int i = 0; i < filesToAnalise.size(); i++) {
            try {
                interns.add(new InternWorker(workerId, filesToAnalise.get(i)));
                if(i > 0 && i% numberOfInterns == 0) {
                    List<Future<ArrayList<ArrayList<String>>>> tasks = executorService.invokeAll(interns);
                    for(Future<ArrayList<ArrayList<String>>> task : tasks)
                    {
                        if (task.get() != null) {
                            resultsTemp.put(task.get().get(0).get(0), task.get().get(1));
                        }
                    }
                    interns = new ArrayList<Callable<ArrayList<ArrayList<String>>>>();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        result.put(hashId, resultsTemp);

        return result;
    }


}
