package com.pb.despair.ts.assign;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixViewer;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.DataReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.MessageWindow;



public class Skims {

	protected static Logger logger = Logger.getLogger("com.pb.despair.ts.assign");

	MessageWindow mw;
	HashMap propertyMap;
	
	boolean useMessageWindow = false;
	
    Network g;

    int[] alphaNumberArray = null;
	int[] betaNumberArray = null;
	int[] zonesToSkim = null;

    int[] externalToAlphaInternal = null;
    int[] alphaExternalNumbers = null;
	
	Matrix newSkimMatrix;

    public Skims ( ResourceBundle rb, String timePeriod ) {

        propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

		String networkDiskObjectFile = (String)propertyMap.get("NetworkDiskObject.file");
		
		// if no network DiskObject file exists, no previous assignments
		// have been done, so build a new Network object which initialize 
		// the congested time field for computing time related skims.
		if ( networkDiskObjectFile == null ) {
			g = new Network( propertyMap, timePeriod );
		}
		// otherwise, read the DiskObject file and use the congested time field
		// for computing time related skims.
		else {
			g = (Network) DataReader.readDiskObject ( networkDiskObjectFile, "highwayNetwork_" + timePeriod );
		}
		

        initSkims ();

    }


    public Skims ( Network g, HashMap map ) {

        this.g = g;
        this.propertyMap = map;

        initSkims ();

    }



    private void initSkims () {

		// take a column of alpha zone numbers from a TableDataSet and puts them into an array for
	    // purposes of setting external numbers.	     */
		String zoneCorrespondenceFile = (String)propertyMap.get("zoneIndex.fileName");
		try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(zoneCorrespondenceFile));
	        alphaNumberArray = table.getColumnAsInt( 1 );
	        betaNumberArray = table.getColumnAsInt( 2 );
        } catch (IOException e) {
            logger.severe("Can't get zone numbers from zonal correspondence file");
            e.printStackTrace();
        }

    
        // define which of the total set of centroids are within the Halo area and should have skim trees built
	    zonesToSkim = new int[g.getMaxCentroid()+1];
	    externalToAlphaInternal = new int[g.getMaxCentroid()+1];
	    alphaExternalNumbers = new int[alphaNumberArray.length+1];
		Arrays.fill ( zonesToSkim, 0 );
		Arrays.fill ( externalToAlphaInternal, -1 );
	    for (int i=0; i < alphaNumberArray.length; i++) {
	    	zonesToSkim[alphaNumberArray[i]] = 1;
	    	externalToAlphaInternal[alphaNumberArray[i]] = i;
	    	alphaExternalNumbers[i+1] = alphaNumberArray[i];
	    }

    }



    /**
	 * write out peak and off-peak, alpha and beta SOV distance skim matrices
	 */
	public void writeSovDistSkimMatrices () {

		String fileName;
		MatrixWriter mw;
		Matrix mSqueezed;
		
		// set the highway network attribute on which to skim the network - distance in this case
		double[] linkCost = g.getDist();
		
        // get the skims as a double[][] array 
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost );

        // copy the array to a ones-based float[][] for conversion to Matrix object
        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

	    // create a Matrix from the peak alpha distance skims array and write to disk
	    fileName = (String)propertyMap.get( "pkHwyDistSkim.fileName" );
        newSkimMatrix = new Matrix( "pkdist", "Peak SOV Distance Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbersZeroBased( alphaNumberArray );
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);

	    // create a squeezed beta skims Matrix from the peak alpha distance skims Matrix and write to disk
	    fileName = (String)propertyMap.get( "pkHwyDistBetaSkim.fileName" );
        mSqueezed = getSqueezedMatrix(newSkimMatrix);
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(mSqueezed);
        
	    // create a Matrix from the off-peak alpha distance skims array and write to disk
	    fileName = (String)propertyMap.get( "opHwyDistSkim.fileName" );
        newSkimMatrix = new Matrix( "opdist", "Off-peak SOV Distance Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);

	}


    /**
	 * write out peak alpha and beta SOV time skim matrices
	 */
	public void writePeakSovTimeSkimMatrices () {

		String fileName;
		MatrixWriter mw;
		Matrix mSqueezed;
		
		// set the highway network attribute on which to skim the network - congested time in this case
		double[] linkCost = g.getCongestedTime();
		
        // get the skims as a double[][] array dimensioned to number of centroids (2984)
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost );

        // convert to a float[][] dimensioned to number of alpha zones (2950)
        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

	    // create a Matrix from the peak alpha congested time skims array and write to disk
	    fileName = (String)propertyMap.get( "pkHwyTimeSkim.fileName" );
        newSkimMatrix = new Matrix( "pktime", "Peak SOV Time Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);

	    // create a squeezed beta skims Matrix from the peak alpha distance skims Matrix and write to disk
	    fileName = (String)propertyMap.get( "pkHwyTimeBetaSkim.fileName" );
        mSqueezed = getSqueezedMatrix(newSkimMatrix);
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(mSqueezed);
        
	}


    /**
	 * write out off-peak alpha and beta SOV time skim matrices
	 */
	public void writeOffPeakSovTimeSkimMatrices () {

		String fileName;
		MatrixWriter mw;
		Matrix mSqueezed;
		
		// set the highway network attribute on which to skim the network - free flow time in this case
		double[] linkCost = g.getFreeFlowTime();
		
        // get the skims as a double[][] array 
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost );

        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

	    // create a Matrix from the off-peak alpha congested time skims array and write to disk
	    fileName = (String)propertyMap.get( "opHwyTimeSkim.fileName" );
        newSkimMatrix = new Matrix( "optime", "Off-peak SOV Time Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );
        mw = MatrixWriter.createWriter( MatrixType.ZIP, new File(fileName) );
        mw.writeMatrix(newSkimMatrix);

	}


    /**
	 * get peak alpha zone SOV distance skim matrix
	 */
	public Matrix getSovDistSkimAsMatrix () {

		// set the highway network attribute on which to skim the network - distance in this case
		double[] linkCost = g.getDist();
		
        // get the skims as a double[][] array
		// skims are generated between all centroids in entire network (2985 total centroids)
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost );

        float[][] zeroBasedFloatArray = getZeroBasedFloatArray ( zeroBasedDoubleArray );

	    // create a Matrix from the peak alpha distance skims array and return
        newSkimMatrix = new Matrix( "pkdist", "Peak SOV Distance Skims", zeroBasedFloatArray );
	    newSkimMatrix.setExternalNumbers( alphaExternalNumbers );

	    return newSkimMatrix;
	    
	}


    /**
	 * get peak alpha zone SOV distance skim array
	 */
	public double[][] getSovDistSkims () {

		// set the highway network attribute on which to skim the network - distance in this case
		double[] linkCost = g.getDist();
		
        // get the skims as a double[][] array 
        double[][] zeroBasedDoubleArray = buildHwySkimMatrix( linkCost );

	    return zeroBasedDoubleArray;
	    
	}


    private float[][] getOnesBasedFloatArray ( double[][] zeroBasedDoubleArray ) {

    	int z;
    	int[] inToEx = g.getIndexNode();

	    // copy the array to a ones-based float[][] for conversion to Matrix object
	    // only the zones within the study area are saved to skim matrix (2950 total skim zones)
		float[][] onesBasedFloatArray = new float[alphaNumberArray.length+1][alphaNumberArray.length+1];
		int r = 1;
	    for (int i=0; i < zeroBasedDoubleArray.length; i++) {
			int c = 1;
			z = inToEx[i];
	    	if ( zonesToSkim[z] == 1 ) {
	    		for (int j=0; j < zeroBasedDoubleArray[i].length; j++) {
	    			z = inToEx[j];
	    	    	if ( zonesToSkim[z] == 1 ) {
	    	    		onesBasedFloatArray[r][c] = (float)zeroBasedDoubleArray[i][j];
	    	    		c++;
	    	    	}
	    		}
	    		r++;
	    	}
	    }
	    zeroBasedDoubleArray = null;

	    return onesBasedFloatArray;

    }

    private float[][] getZeroBasedFloatArray ( double[][] zeroBasedDoubleArray ) {

    	int[] skimsInternalToExternal = g.getIndexNode();
    	int maxZone = g.getMaxCentroid();
    	
		// convert the zero-based double[2983][2983] produced by the skimming procedure
    	// to a zero-based float[2950][2950] for alpha zones to be written to skims file.
		float[][] zeroBasedFloatArray = new float[alphaNumberArray.length][alphaNumberArray.length];
		
        int exRow;
        int exCol;
        int inRow;
        int inCol;
		for (int i=0; i < zeroBasedDoubleArray.length; i++) {
			exRow = skimsInternalToExternal[i];
	    	if ( zonesToSkim[exRow] == 1 ) {
				inRow = externalToAlphaInternal[exRow];
                for (int j=0; j < zeroBasedDoubleArray[i].length; j++) {
                	exCol = skimsInternalToExternal[j];
	    	    	if ( zonesToSkim[exCol] == 1 ) {
	    				inCol = externalToAlphaInternal[exCol];
	    				zeroBasedFloatArray[inRow][inCol] = (float)zeroBasedDoubleArray[i][j];
	    	    	}
	    		}
	    	}
	    }
	    zeroBasedDoubleArray = null;

	    return zeroBasedFloatArray;

    }


    /**
	 * build network skim array, return as double[][].
	 * the highway network attribute on which to skim the network is passed in.
	 */
	private double[][] buildHwySkimMatrix ( double[] linkCost ) {
		
		
		// specify which links are valid parts of paths for this skim matrix
		boolean[] validLinks = new boolean[linkCost.length];
		Arrays.fill (validLinks, false);
		String[] mode = g.getMode();
		for (int i=0; i < validLinks.length; i++) {
			if ( mode[i].indexOf('a') >= 0 )
				validLinks[i] = true;
		}
		
		double[][] skimMatrix =  hwySkim ( linkCost, validLinks, alphaNumberArray );
		
		// set intrazonal values to 0.5*nearest neighbor
		for (int i=0; i < skimMatrix.length; i++) {
			
			// find minimum valued row element
			double minValue = Double.MAX_VALUE;
			for (int j=0; j < skimMatrix.length; j++)
				if ( skimMatrix[i][j] < minValue )
					minValue = skimMatrix[i][j]; 
			
			// set intrazonal value
			skimMatrix[i][i] = 0.5*minValue;
			
		}
		
		return skimMatrix;
       
	}


	/**
	 * highway network skimming procedure
	 */
	private double[][] hwySkim ( double[] linkCost, boolean[] validLinks, int[] alphaNumberArray ) {

	    int i;
	    
	    
	    
		double[][] skimMatrix = new double[g.getNumCentroids()][];

		// create a ShortestPathTreeH object
		ShortestPathTreeH sp = new ShortestPathTreeH( g );


		// build shortest path trees and get distance skims for each origin zone.
		sp.setLinkCost( linkCost );
		sp.setValidLinks( validLinks );
		
		
		for (i=0; i < g.getNumCentroids(); i++) {
			if (useMessageWindow) mw.setMessage2 ( "Skimming shortest paths from zone " + (i+1) + " of " + alphaNumberArray.length + " zones." );
			sp.buildTree( i );
			skimMatrix[i] = sp.getSkim();
		}

		return skimMatrix;
        
	}


	public Matrix getSqueezedMatrix (Matrix aMatrix) {
		
        // alphaNumberArray and betaNumberArray were read in from correspondence file and are zero-based
        AlphaToBeta a2b = new AlphaToBeta (alphaNumberArray, betaNumberArray);

        // create a MatrixCompression object and return the squeezed matrix
        MatrixCompression squeeze = new MatrixCompression(a2b);
        return squeeze.getCompressedMatrix(aMatrix, "MEAN");
        
	}
	
	

	public static void main(String[] args) {

    	
    	ResourceBundle rb = ResourceUtil.getPropertyBundle( new File("/jim/util/svn_workspace/projects/tlumip/config/ts.properties") );

    	logger.info ("creating Skims object.");
        Skims s = new Skims ( rb, "peak" );

    	logger.info ("skimming network and creating Matrix object.");
        Matrix m = s.getSovDistSkimAsMatrix();

    	logger.info ("squeezing the alpha matrix to a beta matrix.");
        Matrix mSqueezed = s.getSqueezedMatrix(m);

        
    	// use a MatrixViewer to examione the skims matrices created here
	    JFrame frame = new JFrame("MatrixViewer - " + m.getDescription());
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
        MatrixViewer matrixContentPane = new MatrixViewer( mSqueezed );

	    matrixContentPane.setOpaque(true); //content panes must be opaque
	    frame.setContentPane(matrixContentPane);
	
	    frame.pack();
	    frame.setVisible(true);

    }
}
