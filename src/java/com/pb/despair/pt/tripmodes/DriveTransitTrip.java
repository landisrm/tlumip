package com.pb.despair.pt.tripmodes;

import com.pb.despair.model.Mode;
import com.pb.despair.model.ModeType;

import org.apache.log4j.Logger;
/** 
 * Driver alone mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class DriveTransitTrip extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.despair.pt.default");
    public DriveTransitTrip(){
          alternativeName=new String("DriveTransitTrip");
          type=ModeType.DRIVETRANSITTRIP;
     }
     
}

