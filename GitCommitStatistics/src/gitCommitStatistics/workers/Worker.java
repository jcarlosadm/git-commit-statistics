package gitCommitStatistics.workers;


import gitCommitStatistics.directoryManager.DirectoryManager;
import gitCommitStatistics.git.GitManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.Callable;

public class Worker implements Callable<Hashtable<String, ArrayList<String>>> {
    private String hashId;
    private String workerId;
    private String path;
    private ArrayList<String> filesToAnalise;
    public Worker(String workerId, String hashId, ArrayList<String> filesToAnalise) {
        System.out.println(workerId + ": Work Work");
        this.filesToAnalise = filesToAnalise;
        this.workerId = workerId;
        this.hashId = hashId;
        this.path = DirectoryManager.WORKERS_PATH + System.getProperty("file.separator") + workerId;
        if (!DirectoryManager.getInstance().testIfExists(path)) {
            DirectoryManager.getInstance().cloneProject(workerId);
        }
        GitManager gitManager = new GitManager();
        gitManager.setRepository(path);
        gitManager.checkout(hashId);
    }
    @Override
    public Hashtable<String, ArrayList<String>> call() {
        for (String filePath: filesToAnalise) {
            File file = new File(path + System.getProperty("file.separator") + filePath);
            System.out.println(file.exists());
        }

        return null;
    }

}
