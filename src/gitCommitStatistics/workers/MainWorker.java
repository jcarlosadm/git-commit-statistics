package gitCommitStatistics.workers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import gitCommitStatistics.directoryManager.DirectoryManager;
import gitCommitStatistics.git.GitManager;
import gitCommitStatistics.git.RepoManager;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.report.GeneralReport;

public class MainWorker {
    protected ArrayList<String> repos;
    //private Hashtable<String, String> commitStatus;
    //private Hashtable<String, String> commitResults;
    protected List<Callable<Hashtable<String, Hashtable<String, ArrayList<String>>>>> workers;
    protected ExecutorService executorService;
    private static MainWorker instance;
    protected int numberOfWorkers;
    protected boolean isAResume;
    protected int resumeTasks;
    protected Hashtable<String, Hashtable<String, ArrayList<String>>> resultMap;
    protected Hashtable<String, Hashtable<String, ArrayList<String>>> backupMap;
    protected ArrayList<String> commitsChecked;

    protected MainWorker() {
        // Check if src2srcml, srcml2src, dmacros exists,
        // boolean exists = false;
        try {
            File file = new File(PropertiesManager.getPropertie("path.src2srcml"));
            if (!file.exists()) {
                GeneralReport.getInstance().reportError("Src2srcml não encontrado");
                throw new FileNotFoundException();
            }
            file = new File(PropertiesManager.getPropertie("path.srcml2src"));
            if (!file.exists()) {
                GeneralReport.getInstance().reportError("srcml2src não encontrado");
                throw new FileNotFoundException();
            }
            file = new File(PropertiesManager.getPropertie("path.dmacros"));
            if (!file.exists()) {
                GeneralReport.getInstance().reportError("dmacros não encontrado");
                throw new FileNotFoundException();
            }
            try {
                numberOfWorkers = Integer.parseInt(PropertiesManager.getPropertie("number.of.workers"));
                workers = new ArrayList<Callable<Hashtable<String, Hashtable<String, ArrayList<String>>>>>();
                executorService = Executors.newFixedThreadPool(numberOfWorkers);
            } catch (Exception e) {
                GeneralReport.getInstance().reportError("Não foi possível criar os workers");
            }

            executeJob();

            executorService.shutdown();
        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Dependências não encontradas");
        }

    }

    public static MainWorker getInstance() {
        if (instance == null) {
            instance = new MainWorker();
        }
        return instance;
    }

    private boolean executeJob() {

        if (DirectoryManager.getInstance().createMainDirectories()) { // Creating
                                                                      // directories
            repos = RepoManager.getInstance().getRepoList(); // Getting repo
                                                             // list
            for (String repo : repos) {
                backupMap = new Hashtable<String, Hashtable<String, ArrayList<String>>>();
                resultMap = new Hashtable<String, Hashtable<String, ArrayList<String>>>();
                commitsChecked = new ArrayList<String>();
                System.out.println(repo);
                isAResume = DirectoryManager.getInstance().isAResume(repo);
                if (isAResume) {
                    resumeTasks = DirectoryManager.getInstance().getResumeTasks(repo);
                }
                System.out.println(DirectoryManager.getInstance().getResumeTasks(repo));
                if (GitManager.cloneRepo(repo, GitManager.PROJECT_PATH)) { // Cloning
                                                                           // repo
                    GitManager gitManager = new GitManager();
                    gitManager.setRepository(GitManager.PROJECT_PATH);
                    executeTasks(gitManager.getChangeMapKeys(), gitManager.getChangedFilesMap());
                    writeResults(repo);
                    GeneralReport.getInstance().reportInfo(repo + ": finalizado");
                    this.makeAnalysisOnCurrentRepo(gitManager, repo);
                }
            }
            return true;
        }
        return false;
    }

    protected boolean writeResults(String repo) {
        return DirectoryManager.getInstance().writeResults(backupMap, repo);
    }

    protected void makeAnalysisOnCurrentRepo(GitManager gitManager, String repo) {
    }

    public void executeTasks(ArrayList<String> commits, Hashtable<String, ArrayList<String>> changeMap) {
        int i = 0;
        if (isAResume) {
            i = resumeTasks;
        }
        for (; i < commits.size(); i++) {
            try {
                workers.add(getWorkerInstance(commits, changeMap, i));
                if (i > 0 && i % numberOfWorkers == 0) {
                    System.out.println(i + " tasks");
                    List<Future<Hashtable<String, Hashtable<String, ArrayList<String>>>>> tasks = executorService
                            .invokeAll(workers);
                    for (Future<Hashtable<String, Hashtable<String, ArrayList<String>>>> task : tasks) {
                        if (task.get() != null) {
                            try {
                                Enumeration<String> e = task.get().keys();
                                String key = e.nextElement();
                                resultMap.put(key, task.get().get(key));
                            } catch (Exception e) {
                                GeneralReport.getInstance()
                                        .reportError("Não foi possível pegar os dados do commit" + task.get());
                            }
                        }
                    }
                    
                    this.createBackup();
                    workers = new ArrayList<Callable<Hashtable<String, Hashtable<String, ArrayList<String>>>>>();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        DirectoryManager.getInstance().reCreateWorkers();
    }

    protected Worker getWorkerInstance(ArrayList<String> commits, Hashtable<String, ArrayList<String>> changeMap,
            int commitIndex) {
        return new Worker("Worker-" + commitIndex % numberOfWorkers, commits.get(commitIndex),
                changeMap.get(commits.get(commitIndex)));
    }

    public void createBackup() {
        GitManager gitManager = new GitManager();
        gitManager.setRepository(GitManager.PROJECT_PATH);
        ArrayList<String> keyList = new ArrayList<String>();
        for (String key : gitManager.getCommitHashList()) {
            if (commitsChecked.indexOf(key) == -1 && resultMap.containsKey(key)) {
                keyList.add(key);
            }

        }

        for (String commitId : keyList) {

            Hashtable<String, ArrayList<String>> temp = resultMap.get(commitId);
            if (temp != null) {
                Enumeration<String> e = temp.keys();
                while (e.hasMoreElements()) {
                    String fileName = (String) e.nextElement();
                    Hashtable<String, ArrayList<String>> tempResults = new Hashtable<String, ArrayList<String>>();
                    ArrayList<String> xAxis = new ArrayList<String>();
                    xAxis.add(commitId);
                    ArrayList<String> yAxis = new ArrayList<String>();
                    String tempYValue = "[";
                    for (String yValue : resultMap.get(commitId).get(fileName)) {
                        tempYValue += yValue + ",";
                    }
                    tempYValue = tempYValue.substring(0, tempYValue.length() - 1) + "]";
                    yAxis.add(tempYValue);
                    tempResults.put("x", xAxis);
                    tempResults.put("y", yAxis);

                    if (backupMap.get(fileName) == null) {
                        backupMap.put(fileName, tempResults);
                    } else if (!backupMap.get(fileName).get("y").get(backupMap.get(fileName).get("y").size() - 1)
                            .equals(tempYValue)) {
                        backupMap.get(fileName).get("x").add(commitId);
                        backupMap.get(fileName).get("y").add(tempYValue);
                    }
                }
            }
            commitsChecked.add(commitId);

        }
        DirectoryManager.getInstance().writeBackup(backupMap);

    }

    public static void main(String args[]) {

        /*
         * Hashtable<String, ArrayList<String>> teste = new Hashtable<String,
         * ArrayList<String>>(); System.out.println(teste); teste.put("a", new
         * ArrayList<String>()); System.out.println(teste);
         * teste.get("a").add("12"); System.out.println(teste);
         * teste.get("a").add("13"); System.out.println(teste);
         * teste.get("a").add("14");
         */
        ArrayList<ArrayList<String>> test = new ArrayList<ArrayList<String>>();
        System.out.println(test.size() >= 2 && !test.get(0).isEmpty() && !test.get(1).isEmpty());
        test.add(new ArrayList<String>());
        test.add(new ArrayList<String>());
        test.get(0).add("something");
        test.get(1).add("something");
        test.get(1).add("something");
        test.get(1).add("something");
        System.out.println(test.size() >= 2 && !test.get(0).isEmpty() && !test.get(1).isEmpty());
        System.out.println(test.get(3));

    }
}
