package com.pb.despair.ao;

import com.pb.common.util.ResourceUtil;

import java.io.*;
import java.util.logging.Logger;
import java.util.ResourceBundle;

/**
 * This class starts the nodes, the cluster and then the application
 * that is passed in as an argument by writing to a command file that the
 * FileMonitor class is monitoring.  The FileMonitor must be running on the
 * machines that will host the daf nodes before this class can be called.
 * <p>
 * StartDafApplication will check for the existence of a file called
 * 'appNameDone'.  When this file appears, the class will exit.
 * Before exiting, this class will shut down the nodes.
 *
 * @author Christi Willison
 * @version Jun 7, 2004
 */
public class StartDafApplication {
    private static Logger logger = Logger.getLogger("com.pb.despair.ao");
    private ResourceBundle rb; //this is ao.properties
    private long commandSleepTime = 45000;
    private long fileCheckSleepTime = 2000;
    private int t;
    String pathPrefix;
    String doneFilePath;
    File commandFile;
    File appDone;   //the file name should be something like "pidaf_done.txt"
    String appName; //should be in all lower-case
    String scenarioName; //should be the same as designated on the command line
    String nodeName; //name of the node that will start the cluster - can be any node
    String rootDir;

    public StartDafApplication(String appName, int timeInterval, ResourceBundle rb){
        this(appName, "node0", timeInterval, rb);
    }

    public StartDafApplication(String appName, String nodeName, int timeInterval, ResourceBundle rb){
        this.rb = rb;
        this.appName = appName;
        this.nodeName = nodeName;
        this.t = timeInterval;

        this.rootDir =  ResourceUtil.getProperty(rb, "root.dir");
        this.scenarioName = ResourceUtil.getProperty(rb, "scenario.name");

        this.pathPrefix = rootDir + "scenario_" + scenarioName+ "/";

    }

    private File getCommandFile(String cmdFilePath){
        File cmdFile = new File(cmdFilePath+"commandFile.txt");
//        File cmdFile = new File("/home/christi/JavaModules/commandFile.txt");
        if(!cmdFile.exists()){
            logger.severe("The file used by the FileMonitor class does not exist - check path");
            System.exit(10);
        }
        logger.info("Command file has been found");
        return cmdFile;
    }

    private void deleteAppDoneFile(File doneFile){
        if(doneFile.exists()){
            logger.info("Deleting the "+appName+"_done.txt file");
            doneFile.delete();
            if(doneFile.exists()) logger.info(appName+"_done.txt file still exists");
            return;
        }
    }
    
    private void writeCommands(){
        writeCommandToCmdFile(Entry.START_NODE);
        try {
            Thread.sleep(commandSleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writeCommandToCmdFile(Entry.START_CLUSTER);
        try {
            Thread.sleep(commandSleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writeCommandToCmdFile(Entry.START_APPLICATION);

        logger.info("Wait here for the application to finish");
        long waitTime = System.currentTimeMillis();
        waitForAppDoneFile();
        logger.info("Application has finished. Time in seconds: "+(System.currentTimeMillis()-waitTime)/1000.0);
    }



    private void writeCommandToCmdFile(String entry){
        logger.info("Writing '"+ entry + "' to command file");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(commandFile));

            if(entry.equals(Entry.START_CLUSTER)){
                writer.println(Entry.START_CLUSTER);
                writer.println(nodeName);

            }else if(entry.equals(Entry.START_APPLICATION)){
                writer.println(Entry.START_APPLICATION);
                writer.println(nodeName);
                writer.println(appName.toLowerCase()+"_"+scenarioName);

            }else{
                writer.println(entry); //all other commands have just a single entry with no arguments
            }

            writer.close();

        } catch (IOException e) {
            logger.severe("Could not open command file or was not able to write to it - check file properties");
            e.printStackTrace();
        }

    }

    private void waitForAppDoneFile(){
        boolean stopRequested = false;

        while(! stopRequested) {
            try {
                Thread.sleep(fileCheckSleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //Check that the file exists
            if (appDone.exists()) {
                stopRequested=true;
            }
        }
    }


    private void cleanUpAndExit(){
        writeCommandToCmdFile(Entry.STOP_NODE);
    }

    public void run(){
        //get the path to the command file and make sure the file exists
        String cmdPath = rootDir  + ResourceUtil.getProperty(rb,"command.file.dir");
        logger.info("CommandFile Path: "+ cmdPath);
        commandFile = getCommandFile(cmdPath);

        //construct the path to the $appName_done.txt file
        //and delete the file if it already exists
        String doneFile = pathPrefix + "t" + t + "/" + appName.substring(0,2) + "/" + appName + "_done.txt";
        logger.info("DoneFile Path: " + doneFile);
        appDone = new File(doneFile);
        deleteAppDoneFile(appDone);

        //begin the daf application by writing the correct
        //commands to the command file
        logger.info("Starting nodes, cluster and application.  Time between commands is " + commandSleepTime + " ms");
        writeCommands();

        logger.info("Ending application");
        //end daf application by writing 'StopNode' into the command file
        cleanUpAndExit();

    }




    public static void main(String[] args) {
        String appName = args[0];
        String nodeName = args[1];
        int t = Integer.parseInt(args[2]);
        ResourceBundle rb = ResourceUtil.getPropertyBundle(new File("c:/code/JavaModules/tlumip/config/ao/ao.properties"));
        logger.info("appName: "+ appName);
        logger.info("nodeName: "+ nodeName);
        logger.info("timeInterval " + t);
        StartDafApplication appRunner = new StartDafApplication(appName,nodeName,t, rb);
        appRunner.run();


    }

}
