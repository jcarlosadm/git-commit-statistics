package gitCommitStatistics.directoryManager;

import gitCommitStatistics.properties.PropertiesManager;
import gitCommitStatistics.report.GeneralReport;

import java.io.*;

public class DirectoryManager {
    private static DirectoryManager instance;
    private static String PATH = PropertiesManager.getPropertie("path");
    public static String WORKERS_PATH = PATH + System.getProperty("file.separator") + "workers";
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
            if (file.exists() || file.mkdir()) {
                if (fileWorker.exists() || fileWorker.mkdir()) {
                    return true;
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

    public boolean cloneProject(String dest) {
        try {
            File projectDir = new File(PATH + System.getProperty("file.separator") + "project");
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
                System.out.println("Directory copied from "
                        + src + "  to " + dest);
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

    public static void main(String args[]) {
        System.out.println(DirectoryManager.getInstance().createMainDirectories());
        System.out.println(DirectoryManager.getInstance().cloneProject("workerTeste"));
    }
}
