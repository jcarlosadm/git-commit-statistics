package gitCommitStatistics.workers;


import gitCommitStatistics.directoryManager.DirectoryManager;
import gitCommitStatistics.git.GitManager;
import gitCommitStatistics.git.RepoManager;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.report.GeneralReport;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainWorker {
    private ArrayList<String> repos;
    private Hashtable<String,String> commitStatus;
    private Hashtable<String,String> commitResults;
    private List<Callable<Hashtable<String, ArrayList<String>>>> workers;
    private ExecutorService executorService;
    private static MainWorker instance;
    private int numberOfWorkers;
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
                workers = new ArrayList<Callable<Hashtable<String, ArrayList<String>>>>();
                executorService = Executors.newFixedThreadPool(numberOfWorkers);
            } catch (Exception e) {
                GeneralReport.getInstance().reportError("Não foi possível criar os workers");
            }


            executeJob();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            GeneralReport.getInstance().reportError("Dependências não encontradas");
        }

    }
    public static MainWorker getInstance() {
        if (instance == null) {
            System.out.println("Created Main Worker");
            instance = new MainWorker();
        }
        return instance;
    }
    private boolean executeJob () {

        if(DirectoryManager.getInstance().createMainDirectories()) { //Creating directories
            repos = RepoManager.getInstance().getRepoList(); //Getting repo list
            for(String repo : repos) {
//                if(GitManager.cloneRepo(repo, GitManager.PROJECT_PATH)) { //Cloning repo
                GitManager gitManager = new GitManager();
                gitManager.setRepository(GitManager.PROJECT_PATH);
                executeTasks(gitManager.getCommitHashList(), gitManager.getChangedFilesMap());

                    return true;
//                }
            }
        }
        return false;
    }

    public void executeTasks(ArrayList<String> commits, Hashtable<String, ArrayList<String>> changeMap) {
        for(int i = 0; i < commits.size(); i++) {
            try {
                workers.add(new Worker("Worker-" + i % numberOfWorkers, commits.get(i), changeMap.get(commits.get(i))));
                if(i > 0 && i% numberOfWorkers == 0) {
                    List<Future<Hashtable<String, ArrayList<String>>>> tasks = executorService.invokeAll(workers);
                    for(Future<Hashtable<String, ArrayList<String>>> task : tasks)
                    {
                        System.out.println(task);
                    }
                    workers = new ArrayList<Callable<Hashtable<String, ArrayList<String>>>>();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    public void beep() {
        System.out.println("Beep");
    }

}
