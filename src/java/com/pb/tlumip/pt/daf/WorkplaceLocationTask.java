/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.pt.daf;

/**
 * WorkplaceLocationTask
 *
 * @author Freedman
 * @version Aug 11, 2004
 * 
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import java.util.Date;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.tlumip.model.ModeChoiceLogsums;
import com.pb.tlumip.pt.LaborFlows;
import com.pb.tlumip.pt.LogsumManager;
import com.pb.tlumip.pt.PTModelInputs;
import com.pb.tlumip.pt.PTPerson;
import com.pb.tlumip.pt.WorkplaceLocationModel;

public class WorkplaceLocationTask  extends MessageProcessingTask{

    final static Logger logger = Logger.getLogger("com.pb.tlumip.pt.daf");
    protected static Object lock = new Object();
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static boolean initialized = false;
    String fileWriterQueue = "FileWriterQueue";
    LogsumManager logsumManager;
//    protected static WorkLogsumMap logsumMap = new WorkLogsumMap();
    WorkplaceLocationModel workLocationModel;

    /**
     * Onstart method sets up model
     */
    public void onStart() {
        synchronized (lock) {
            logger.info( "***" + getName() + " started");
            //in cases where there are multiple tasks in a single vm, need to make sure only initilizing once!
            if (!initialized) {
                String scenarioName;
                int timeInterval;
                String pathToPtRb;
                String pathToGlobalRb;
                
                logger.info("Reading RunParams.properties file");
                ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
                scenarioName = ResourceUtil.getProperty(runParamsRb,"scenarioName");
                logger.info("\tScenario Name: " + scenarioName);
                timeInterval = Integer.parseInt(ResourceUtil.getProperty(runParamsRb,"timeInterval"));
                logger.info("\tTime Interval: " + timeInterval);
                pathToPtRb = ResourceUtil.getProperty(runParamsRb,"pathToAppRb");
                logger.info("\tResourceBundle Path: " + pathToPtRb);
                pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,"pathToGlobalRb");
                logger.info("\tResourceBundle Path: " + pathToGlobalRb);
                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));
                
                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));

                PTModelInputs ptInputs = new PTModelInputs(ptRb, globalRb);
                logger.info("Setting up the workplace model");
                ptInputs.setSeed(2002);
                ptInputs.getParameters();
                ptInputs.readSkims();
                ptInputs.readTazData();
                LaborFlows lf = new LaborFlows(ptRb);
                lf.setZoneMap(TableDataSetLoader.loadTableDataSet(globalRb,"alpha2beta.file"));
                lf.readAlphaValues(TableDataSetLoader.loadTableDataSet(ptRb,"productionValues.file"),
                    TableDataSetLoader.loadTableDataSet(ptRb,"consumptionValues.file"));
                lf.readBetaLaborFlows();
                initialized = true;
            }
            workLocationModel = new WorkplaceLocationModel(ptRb);
            logsumManager = new LogsumManager(ptRb);
            logger.info( "***" + getName() + " finished onStart()");
        }
    }
    /**
     * A worker bee that will process a block of households.
     *
     */
    public void onMessage(Message msg) {
        logger.info("********" + getName() + " received messageId=" + msg.getId() +
            " message from=" + msg.getSender() + " at " + new Date());

        if (msg.getId().equals(MessageID.CALCULATE_WORKPLACE_LOCATIONS)) {
                    createLaborFlowMatrix(msg);
                } 
    }

        /**
         * Create labor flow matrices for a particular occupation, hh segment, and person array
         * @param msg
         */
        public void createLaborFlowMatrix(Message msg) {
            //getting message information
            if(logger.isDebugEnabled()) {
                logger.debug("Free memory before creating labor flow matrix: " +
                Runtime.getRuntime().freeMemory());
            }


            Integer occupation = (Integer) msg.getValue("occupation");
            Integer segment = (Integer) msg.getValue("segment");
            PTPerson[] persons = (PTPerson[]) msg.getValue("persons");

            ModeChoiceLogsums mcl = new ModeChoiceLogsums(ptRb);
            mcl.readLogsums('w',segment.intValue());
            Matrix modeChoiceLogsum =mcl.getMatrix();

            //Write Labor Flow Probabilities to Disk
            logger.info("Calculating AZ Labor flows.");

            Matrix m = LaborFlows.calculateAlphaLaborFlowsMatrix(modeChoiceLogsum,
                    segment.intValue(), occupation.intValue());
            persons = calculateWorkplaceLocation(persons, m);

            Message laborFlowMessage = createMessage();
            laborFlowMessage.setId(MessageID.WORKPLACE_LOCATIONS_CALCULATED);
            laborFlowMessage.setValue("persons", persons);
            sendTo("TaskMasterQueue", laborFlowMessage);
            if(logger.isDebugEnabled()) {
                logger.debug("Free memory after creating labor flow matrix: " +
                Runtime.getRuntime().freeMemory());
            }
            m = null;
        }

        /**
         * Calculate workplace locations for the array of persons given the logsum accessibility matrix
         * @param persons
         * @param logsumMatrix
         * @return
         */
        public PTPerson[] calculateWorkplaceLocation(PTPerson[] persons,Matrix logsumMatrix) {

            for (int p = 0; p < persons.length; p++) {
                if (persons[p].employed) {
                    persons[p].workTaz = workLocationModel.chooseWorkplace(logsumMatrix,
                            persons[p], PTModelInputs.tazs);

                    if (persons[p].worksTwoJobs) {
                        persons[p].workTaz2 = workLocationModel.chooseWorkplace(logsumMatrix,
                                persons[p], PTModelInputs.tazs);
                    }
                }
            }
            return persons;
        }


}
