package gitCommitStatistics.workers;


import gitCommitStatistics.directoryManager.DirectoryManager;
import gitCommitStatistics.git.GitManager;
import gitCommitStatistics.git.RepoManager;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.report.GeneralReport;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainWorker {
    private ArrayList<String> repos;
    private Hashtable<String,String> commitStatus;
    private Hashtable<String,String> commitResults;
    private List<Callable<Hashtable<String, Hashtable<String, ArrayList<String>>>>> workers;
    private ExecutorService executorService;
    private static MainWorker instance;
    private int numberOfWorkers;
    private Hashtable<String, Hashtable<String, ArrayList<String>>> resultMap;
    private Hashtable<String, Hashtable<String, ArrayList<String>>> backupMap;
    private ArrayList<String> commitsChecked;
    private MainWorker() {
        //Check if src2srcml, srcml2src, dmacros exists,
        boolean exists = false;
        try {
            File file = new File (PropertiesManager.getPropertie("path.src2srcml"));
            if(!file.exists()) {
                GeneralReport.getInstance().reportError("Src2srcml não encontrado");
                throw new FileNotFoundException();
            }
            file = new File (PropertiesManager.getPropertie("path.srcml2src"));
            if(!file.exists()) {
                GeneralReport.getInstance().reportError("srcml2src não encontrado");
                throw new FileNotFoundException();
            }
            file = new File (PropertiesManager.getPropertie("path.dmacros"));
            if(!file.exists()) {
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
    private boolean executeJob () {

        if(DirectoryManager.getInstance().createMainDirectories()) { //Creating directories
            repos = RepoManager.getInstance().getRepoList(); //Getting repo list
            for(String repo : repos) {
                backupMap = new Hashtable<String, Hashtable<String, ArrayList<String>>>();
                resultMap = new Hashtable<String, Hashtable<String, ArrayList<String>>>();
                commitsChecked = new ArrayList<String>();
                if(GitManager.cloneRepo(repo, GitManager.PROJECT_PATH)) { //Cloning repo
                    GitManager gitManager = new GitManager();
                    gitManager.setRepository(GitManager.PROJECT_PATH);
                    executeTasks(gitManager.getChangeMapKeys(), gitManager.getChangedFilesMap());
                    DirectoryManager.getInstance().writeResults(backupMap, repo);
                    GeneralReport.getInstance().reportInfo(repo + ": finalizado");
                }
            }
        }
        return false;
    }

    public void executeTasks(ArrayList<String> commits, Hashtable<String, ArrayList<String>> changeMap) {
        for(int i = 0; i < commits.size(); i++) {
            try {
                workers.add(new Worker("Worker-" + i % numberOfWorkers, commits.get(i), changeMap.get(commits.get(i))));
                if(i > 0 && i% numberOfWorkers == 0) {
                    System.out.println(i + " tasks");
                    List<Future<Hashtable<String, Hashtable<String, ArrayList<String>>>>> tasks = executorService.invokeAll(workers);
                    for(Future<Hashtable<String, Hashtable<String, ArrayList<String>>>> task : tasks)
                    {
                        if(task.get() != null) {
                            try {
                                Enumeration<String> e = task.get().keys();
                                String key = e.nextElement();
                                resultMap.put(key, task.get().get(key));
                            } catch (Exception e) {
                                GeneralReport.getInstance().reportError("Não foi possível pegar os dados do commit" + task.get());
                            }
                        }
                    }
                    createBackup();
                    workers = new ArrayList<Callable<Hashtable<String, Hashtable<String, ArrayList<String>>>>>();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        DirectoryManager.getInstance().reCreateWorkers();
    }

    public void createBackup() {
        GitManager gitManager = new GitManager();
        gitManager.setRepository(GitManager.PROJECT_PATH);
        ArrayList<String> keyList = new ArrayList<String>();
        for (String key: gitManager.getCommitHashList()) {
            if(commitsChecked.indexOf(key) == -1 && resultMap.containsKey(key)) {
                keyList.add(key);
            }

        }

        for(String commitId : keyList){

            Hashtable<String, ArrayList<String>> temp = resultMap.get(commitId);
            if (temp != null) {
                Enumeration<String> e = temp.keys();
                while(e.hasMoreElements()){
                    String fileName = (String) e.nextElement();
                    Hashtable<String, ArrayList<String>> tempResults = new Hashtable<String, ArrayList<String>>();
                    ArrayList<String> xAxis = new ArrayList<String>();
                    xAxis.add(commitId);
                    ArrayList<String> yAxis = new ArrayList<String>();
                    String tempYValue = "[";
                    for (String yValue : resultMap.get(commitId).get(fileName)) {
                        tempYValue += yValue +",";
                    }
                    tempYValue = tempYValue.substring(0,tempYValue.length() -1) + "]";
                    yAxis.add(tempYValue);
                    tempResults.put("x",xAxis);
                    tempResults.put("y",yAxis);

                    if (backupMap.get(fileName) == null) {
                        backupMap.put(fileName, tempResults);
                    } else if (!backupMap.get(fileName).get("y").get(backupMap.get(fileName).get("y").size() - 1).equals(tempYValue)) {
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

        Hashtable<String, ArrayList<String>> teste = new Hashtable<String, ArrayList<String>>();
        System.out.println(teste);
        teste.put("a", new ArrayList<String>());
        System.out.println(teste);
        teste.get("a").add("12");
        System.out.println(teste);
        teste.get("a").add("13");
        System.out.println(teste);
        teste.get("a").add("14");

    }
}