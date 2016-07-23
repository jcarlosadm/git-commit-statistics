package gitCommitStatistics.directoryManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.report.GeneralReport;
import gitCommitStatistics.workers.MainWorker;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class DirectoryManager {
    private static DirectoryManager instance;
    private static String PATH = PropertiesManager.getPropertie("path");
    public static String WORKERS_PATH = PATH + System.getProperty("file.separator") + "workers";
    public static String BACKUP_PATH = PATH + System.getProperty("file.separator") + "backup";
    public static String RESULT_PATH = PATH + System.getProperty("file.separator") + "results";
    private DirectoryManager(){
    }
    public static DirectoryManager getInstance() {
        if (instance == null) {
            instance = new DirectoryManager();
        }
        return instance;
    }

    public boolean createMainDirectories() {
        GeneralReport.getInstance().reportInfo("Criando diretórios");
        try {
            File file = new File(PATH);
            File fileWorker = new File(WORKERS_PATH);
            File fileBackup = new File(BACKUP_PATH);
            File fileResult = new File(RESULT_PATH);
            if (file.exists() || file.mkdir()) {
                if (fileWorker.exists() || fileWorker.mkdir()) {
                    if (fileBackup.exists() || fileBackup.mkdir()) {
                        if (fileResult.exists() || fileResult.mkdir()) {
                            File jquery = new File(RESULT_PATH + System.getProperty("file.separator") + "jquery-1.11.3.min.js");
                            File chartFunctions =new File(RESULT_PATH + System.getProperty("file.separator") + "chartFunctions.js" );
                            if(!jquery.exists()) {
                                copyFolder(new File("./view/" + "jquery-1.11.3.min.js"), jquery);
                            }
                            if(!chartFunctions.exists()) {
                                copyFolder(new File("./view/" + "chartFunctions.js"), chartFunctions);
                            }
                            return true;
                        } else {
                            GeneralReport.getInstance().reportError("Não foi possível criar diretório dos resultados");
                            return false;
                        }
                    } else {
                        GeneralReport.getInstance().reportError("Não foi possível criar diretório dos backups");
                        return false;
                    }
                } else {
                    GeneralReport.getInstance().reportError("Não foi possível criar diretório dos workers");
                    return false;
                }

            } else {
                GeneralReport.getInstance().reportError("Não foi possível criar diretório principal");
                return false;
            }


        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível criar diretório principal");
            return false;
        }

    }

    public boolean reCreateWorkers() {
        GeneralReport.getInstance().reportInfo("Criando diretórios");
        try {
            File fileWorker = new File(WORKERS_PATH);

                if (fileWorker.exists()){
                    deleteFile(fileWorker);
                }
                if(fileWorker.mkdir()) {
                    return true;
                } else {
                    GeneralReport.getInstance().reportError("Não foi possível criar diretório dos workers");
                    return false;
                }



        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível criar diretório principal");
            return false;
        }

    }

    public boolean cloneProject(String dest) {
        try {
            File projectDir = new File(PATH + System.getProperty("file.separator") + "project" + File.separator + MainWorker.getCurrentRepoName());
            File workerDir = new File(WORKERS_PATH + System.getProperty("file.separator") + dest);
            if (!projectDir.exists()) {
                GeneralReport.getInstance().reportError("Projeto não encontrado");
                return false;
            } else {
                if (!workerDir.exists()) {
                    workerDir.mkdir();
                }
                copyFolder(projectDir, workerDir);
            }
            return true;
        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi possível clonar projeto para " + dest);
            return false;
        }
    }
    public boolean testIfExists(String workerPath) {
        File file = new File(workerPath);
        return file.exists();
    }

    public void copyFolder(File src, File dest) throws IOException {

        if(src.isDirectory()){
            if(!dest.exists()){
                dest.mkdir();
            }

            String files[] = src.list();

            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyFolder(srcFile,destFile);
            }
        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            while ((length = in.read(buffer)) > 0){
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }

    public void deleteFile(File f) {
        try {

        } catch (Exception e) {

        }
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                try {
                    deleteFile(c);
                } catch (Exception e) {
                    e.printStackTrace();
                    GeneralReport.getInstance().reportError("Failed to delete file: " + f);
                }
            }

        }
        try {
            f.delete();
        } catch (Exception e) {
            e.printStackTrace();
            GeneralReport.getInstance().reportError("Failed to delete file: " + f);
        }



    }

    public boolean writeFile(String file, String text) {
        try {
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.println(text);
            writer.close();
            return true;
        } catch (Exception e) {
            GeneralReport.getInstance().reportError("Não foi pissível criar arquivo: " + file);
        }
        return false;
    }

    public boolean writeBackup(Hashtable<String, Hashtable<String, ArrayList<String>>> backupMap) {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject o = parser.parse(backupMap.toString().replaceAll("'","")).getAsJsonObject();


        try {
            //write converted json data to a file named "file.json"
            FileWriter writer = new FileWriter(BACKUP_PATH + System.getProperty("file.separator") + "backupJson");
            writer.write("var info = ");
            writer.write(o.toString());
            writer.write(";");
            writer.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;

    }
    public boolean writeBackupResume(String repo, String tasks) {

        try {
            FileWriter writer = new FileWriter(BACKUP_PATH + System.getProperty("file.separator") + "backupTasks");
            writer.write(repo + ";" + tasks);
            writer.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;

    }
    public boolean isAResume(String repo) {
        BufferedReader br = null;
        boolean result = false;
        try {
            File backup = new File(BACKUP_PATH + System.getProperty("file.separator") + "backupTasks");
            if (backup.exists()) {
                String sCurrentLine;

                br = new BufferedReader(new FileReader(backup));

                if((sCurrentLine = br.readLine()) != null && sCurrentLine.contains(repo)) {
                    result = true;
                }

                if (br != null)br.close();
            }
        } catch (Exception e) {

        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return result;
    }

    public int getResumeTasks(String repo) {
        BufferedReader br = null;
        int result = 0;
        try {
            File backup = new File(BACKUP_PATH + System.getProperty("file.separator") + "backupTasks");
            if (backup.exists()) {
                String sCurrentLine;

                br = new BufferedReader(new FileReader(backup));

                if((sCurrentLine = br.readLine()) != null && sCurrentLine.contains(repo)) {
                    System.out.println(sCurrentLine);
                    String[] repoSplit = sCurrentLine.split(";");
                    result = Integer.parseInt(repoSplit[1]);
                }

                if (br != null)br.close();
            }
        } catch (Exception e) {

        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return result;
    }


    public boolean writeResults(Hashtable<String, Hashtable<String, ArrayList<String>>> resultMap, String repoLink) {
        String name = repoLink.substring(repoLink.lastIndexOf("/") + 1).replace(".git", "");
        String commitLink = repoLink.replace(".git", "/commit/");
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonObject o = parser.parse(resultMap.toString().replaceAll("'","")).getAsJsonObject();


        try {
            //write converted json data to a file named "file.json"
            FileWriter writer = new FileWriter(RESULT_PATH + System.getProperty("file.separator") + "resultJson" + name);
            writer.write("var info = ");
            writer.write(o.toString());
            writer.write(";");
            writer.close();



            BufferedReader br = null;
            String sCurrentLine;

            br = new BufferedReader(new FileReader("./view/chart.html"));
            writer = new FileWriter(RESULT_PATH + System.getProperty("file.separator") + name + ".html");
            while ((sCurrentLine = br.readLine()) != null) {
                if(sCurrentLine.indexOf("resultJson") != -1) {
                    sCurrentLine = sCurrentLine.replace("resultJson", "resultJson" + name);
                } else if (sCurrentLine.indexOf("var gitLink = '';") != -1) {
                    sCurrentLine = sCurrentLine.replace("var gitLink = '';", "var gitLink = '" + commitLink + "';");
                }
                writer.write(sCurrentLine);
            }
            if (br != null)br.close();
            writer.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    public static void main(String args[]) {
        System.out.println(DirectoryManager.getInstance().createMainDirectories());
        System.out.println(DirectoryManager.getInstance().cloneProject("workerTeste"));
    }
}


