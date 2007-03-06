package com.pb.tlumip.ts;

import java.util.Vector;

import org.apache.xmlrpc.WebServer;

import org.apache.log4j.Logger;



/**
 * This data server exposes methods can be called by remote xml-rpc clients written in any language.
 * The methods accept xml-rpc compliant arguments and return xml-rpc compliant data types.
 * 
 * @author JHicks
 *
 */
public class NetworkDataServer {

    protected static Logger logger = Logger.getLogger(NetworkDataServer.class);
    
    WebServer server = null;
    NetworkHandlerIF nh = null;
    
    int[] nodeIndex = null;
    int[] indexNode = null;
    int[] ipa = null;
    int[] ia = null;
    int[] ib = null;
    int[] linkIndexArray = null;
    
    
    private NetworkDataServer () {
    }
    
    
    // Factory Method to return instance only
    public static NetworkDataServer getInstance( NetworkHandlerIF nh, int port, String handlerName ) {
        
        NetworkDataServer ns =  new NetworkDataServer();
        
        ns.nh = nh;

        ns.server = new WebServer(port);
        ns.server.addHandler( handlerName, ns );

        return ns;
        
    }
    
    public void startServer() {
        server.start();
    }
    
    public void stopServer() {
        server.shutdown();
    }
    
    public int getLinkCount() {
        return nh.getLinkCount();
    }
    
    public int getNumCentroids() {
        return nh.getNumCentroids();
    }

    public int getLinkId(int an, int bn) {
        return nh.getLinkIndex(an,bn);
    }
    
    public String[] getUserClassStrings () {
        char[] array = nh.getUserClasses();
        String[] returnArray = new String[array.length];
        for (int i=0; i < array.length; i++) {
            returnArray[i] = Character.toString( array[i] );
        }
        return returnArray;     
    }
    
    public Vector getIa() {
        if ( ia == null )
            ia = nh.getIa();
        return Util.intVector( ia );
    }

    public Vector getIb() {
        if ( ib == null )
            ib = nh.getIb();
        return Util.intVector( ib );
    }

    public Vector getLinkIndexArray() {
        
        if ( linkIndexArray == null ) {
            if ( ia == null )
                ia = nh.getIa();
            if ( ib == null )
                ib = nh.getIb();
            if ( indexNode == null )
                indexNode = nh.getIndexNode();
            
            linkIndexArray = new int[ia.length];
            for (int i=0; i < ia.length; i++) {
                linkIndexArray[i] = nh.getLinkIndex( indexNode[ia[i]], indexNode[ib[i]] );
            }
        }
        
        return Util.intVector( linkIndexArray );
    }

    public String[] getMode () {
        return nh.getMode();
    }
    
    public Vector getDist () {
        return Util.doubleVector( nh.getDist() );
    }

    public Vector getVolau () {
        return Util.doubleVector( nh.getVolau() );
    }

    public Vector getCongestedTime () {
        return Util.doubleVector( nh.getCongestedTime() );
    }

    public Vector getLanes () {
        return Util.doubleVector( nh.getLanes() );
    }

    public Vector getCapacity () {
        return Util.doubleVector( nh.getCapacity() );
    }

    public Vector getOriginalCapacity () {
        return Util.doubleVector( nh.getOriginalCapacity() );
    }

    public Vector getTotalCapacity () {
        return Util.doubleVector( nh.getTotalCapacity() );
    }

    public Vector getFreeFlowTime () {
        return Util.doubleVector( nh.getFreeFlowTime() );
    }

    public Vector getFreeFlowSpeed () {
        return Util.doubleVector( nh.getFreeFlowSpeed() );
    }

    public Vector getLinkType () {
        return Util.intVector( nh.getLinkType() );
    }

    public Vector getVdfIndex () {
        return Util.intVector( nh.getVdfIndex() );
    }

    public Vector getNodeIndex () {
        if ( nodeIndex == null )
            nodeIndex = nh.getNodeIndex();
        return Util.intVector( nodeIndex );
    }
    
    public Vector getIndexNode () {
        if ( indexNode == null )
            indexNode = nh.getIndexNode();
        return Util.intVector( indexNode );
    }
    
    public Vector getNodes () {
        return Util.intVector( nh.getNodes() );
    }
    
    public Vector getNodeX () {
        return Util.doubleVector( nh.getNodeX() );
    }
    
    public Vector getNodeY () {
        return Util.doubleVector( nh.getNodeY() );
    }
    
}