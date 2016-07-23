package gitCommitStatistics.git;

import gitCommitStatistics.directoryManager.DirectoryManager;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.report.GeneralReport;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class GitManager {
    public static final String PROJECT_PATH = PropertiesManager.getPropertie("path") + System.getProperty("file.separator") + "project"; //Ex git@github.com:me/mytestrepo.git
    private Iterable<RevCommit> commitList;
    private Repository repository;
    private Git git;
    private Hashtable<String, ArrayList<String>> changeMap;
    private ArrayList<String> changeMapKeys;

    //remotePath; //Ex git@github.com:me/mytestrepo.git
    //localPath; //Ex /home/repos/...
    public GitManager() {
        changeMap = new Hashtable<String, ArrayList<String>>();
        changeMapKeys = new ArrayList<String>();
    }
    public static boolean cloneRepo(String remotePath, String localPath) {
        try {
            File projectFile = new File(localPath);
            if (projectFile.exists()) {
                GeneralReport.getInstance().reportInfo("Deletando: " + localPath);
                DirectoryManager.getInstance().deleteFile(projectFile);
            }
            GeneralReport.getInstance().reportInfo("Clonando: " + remotePath);
            Git.cloneRepository().setURI(remotePath)
                    .setDirectory(projectFile).call();
            return true;
        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível clonar repositório");
            return false;
        }

    }

    public void setRepository(String path){
        try {
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            File localRepo = new File(path + System.getProperty("file.separator") + ".git");
            repositoryBuilder.setGitDir(localRepo);
            repositoryBuilder.readEnvironment();

            repository = repositoryBuilder.build();
            git = new Git(repository);

        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível acessar repositório" + path);
        }
    }

    public Repository getRepository() {
        return repository;
    }

    public Iterable<RevCommit> getCommitList() {
        try {
            commitList = git.log().call();
        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível acessar commits do repositório");
        }


        return commitList;
    }
    public ArrayList<String> getCommitHashList () {
        ArrayList<String> commitIds = new ArrayList<String>();
        getCommitList();
        for (RevCommit commit : commitList) {
            commitIds.add(commit.getId().getName());
        }
        return commitIds;
    }

    public void generateChangedFilesMap() {
        ;
        String commitId = "";
        try {
            RevWalk rw = new RevWalk(repository);
            ObjectId head = repository.resolve(Constants.HEAD);
            DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
            df.setRepository(repository);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(true);

            for(RevCommit commit: getCommitList()) {
                try {
                    commitId = commit.getId().getName();
                    RevCommit parent = rw.parseCommit(commit.getParent(0).getId());

                    List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
                    ArrayList<String> changedFiles = new ArrayList<String>();
                    for (DiffEntry diff : diffs) {
                        // TODO add?
                        if ((diff.getChangeType().name().equals("MODIFY") || diff.getChangeType().name().equals("ADD")) && diff.getNewPath().substring(diff.getNewPath().length() - 2).equals(".c")) {
                            changedFiles.add(diff.getNewPath());
                        }
                    }
                    if (changedFiles.size() > 0) {
                        changeMap.put(commitId, changedFiles);
                        changeMapKeys.add(commitId);
                    }
                } catch (Exception e) {
                    GeneralReport.getInstance().reportError("Não foi possível acessar as diferenças do commit " + commitId);
                }


            }


        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível acessar as diferenças nos commits do repositório");
        }

    }

    public Hashtable<String, ArrayList<String>> getChangedFilesMap () {
        generateChangedFilesMap();
        return changeMap;
    }
    public ArrayList<String> getChangeMapKeys() {
        return changeMapKeys;
    }

    public boolean checkout(String hash) {
        try {
            git.checkout().setName(hash).call();
            return true;
        } catch(Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível dar o checkout no commit: " + hash + ", resetando para tentar de novo");
        }
        try {
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            git.checkout().setName(hash).call();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            GeneralReport.getInstance().reportError("Não foi possível dar o checkout no commit: " + hash);
            return false;
        }
    }
    public static void main(String args[]) {
        GitManager test = new GitManager();
        test.setRepository(PROJECT_PATH);
//        System.out.println(test.cloneRepo("https://github.com/mate-desktop/marco.git", "localPath"));
        System.out.println(test.getCommitList());
        test.generateChangedFilesMap();
        test.checkout("28a029a4990d2a84f9d6a0b890eba812ea503998");
    }
}

