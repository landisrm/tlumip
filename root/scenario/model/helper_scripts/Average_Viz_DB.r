# Project
#   TLUMIP SWIM VIZ 

# About
#   Create output average VIZ DB based on a series of queries.

# Description:
#   This script averages data fields across all specified SWIM VIZ databases. 
#   The data fields to average are specified in a ".csv" file (input to the script). 
#   There is also an option to write a list of tables and files from the database, from 
#   which the above ".csv" file is created. 

#   This script works in two parts and at any given time only one part is run. The first 
#   part is run  if "masterTable = T", writes out the contents of the databases to a ".csv" file. 
#   Then the user needs to specify the fields to query in "key_fld" column with values "KEYS", 
#   "FLDS" and "NULL". The second part is run if "masterTable = F", the user defined ".csv" file
#   is used as the input in creating the averages.

# Input and Outputs : 
#   1. Input : VIZ DBs to average
#   2. Output: SQLite database filename
#   3. Option: Select option to wrtie-out VIZ DB contents
#   4. Input : CSV file with a list of fields to query
#   5. Option: Only compare common TSTEPs across DBs

# Steps:
#   1. Connects to databases (Inputs/Output)
#   2. If masterTable option is true, then writes out database structure (all databases have same structure)
#   3. Then user can open outMasterTable a ".csv" file in Excel and can specify fields to query in column "key_fld"
#      By default column "key_fld" contains "NULL" and user must replace "NULL" with "KEYS" and "FLDS" as desired.
#         a) "KEYS"  : The key fields of the table (market segment) by which the query is run
#         b) "FLDS"  : The data fields (value fields) that are to be selected (or averaged) 
#         c) "NULL"  : The fields that are neither "KEYS" nor "FLDS" are called "NULL"
#   4. Reads the above file (from step 3) and creates 3 vital piecies of a query blocks called "Table", "Keys" and "Fields"
#   5. Loops through each query and averages data fields across all specified VIZ DBs

# Authors
#   Ben Stabler, stabler@pbworld.com, 081810
#   Amar Sarvepalli, sarvepalli@pbworld.com, 062411
#   Nagendra Dhakar, nagendra.dhakar@rsginc.com, 02162017

# Revisions
#   10/6/11 to optionally allow comparisons for TSTEPs common to all databases
#   02/16/17 to make the output look like a regular viz db. also, error checks for field names and field types

#=========================================================================================================
# INPUT AND OUTPUT FILES

  # Specify all input database file names as list 
    dbFileNames = c("E:\\Projects\\Clients\\odot\\Tasks\\AverageDatabases\\data\\Reference.db",
                    "E:\\Projects\\Clients\\odot\\Tasks\\AverageDatabases\\data\\Pessimistic.db")
    
      
  # Specify an output database file name
    outputDbFileName = "E:\\Projects\\Clients\\odot\\Tasks\\AverageDatabases\\data\\average_test.db"
  
  # Option to write out contents of the database (list of tables and fields) 
    masterTable = F 
    
  # Option to only compare common TSTEPs across years
    onlyCommonTstep = F
  
  # Specify the master table (creates this file if the "masterTable = T" or else it's an input file)   
    outMasterTable = ("E:\\Projects\\Clients\\odot\\Tasks\\AverageDatabases\\data\\DataBaseDescription_0.csv")
    
  # Specify output file for the database checker
    outCheckFile = ("E:\\Projects\\Clients\\odot\\Tasks\\AverageDatabases\\data\\CheckDatabaseFiles.csv")
#=========================================================================================================
# CREATE OUTPUT DATABASE

# Load SQL libraries
  library(RSQLite)
  m = dbDriver("SQLite")
  
  #Create database
  if (masterTable==F){
    cat(paste("Create Average SWIM VIZ DB", outputDbFileName, "at", Sys.time(), "\n"))
    if(file.exists(outputDbFileName)) {
      file.remove(outputDbFileName)
    }
    file.create(outputDbFileName)
    db = dbConnect(m, dbname = outputDbFileName) 
  }
  
# FUNCTIONS
  buildQuery = function(averageFieldList, tsteps=NA) {
  
    #Get info
    tableName = averageFieldList["tableName"]
    
    if(is.na(tsteps)) {
      query = paste("SELECT * FROM ", tableName)
    } else {
      query = paste("SELECT * FROM ", tableName, "WHERE TSTEP IN (", paste(tsteps, collapse=","), ")")
    }
    return(query)
  } 
  
  
#=========================================================================================================
# Check if tables and length of tables in the input dabases are of same length
# writes output file to the 
#=========================================================================================================
for (d in 1:length(dbFileNames)){
    # Establish connection to input database
      inDB = dbConnect(m, dbname = dbFileNames[d]) 
      fileName <- unlist(strsplit(dbFileNames[d],"\\\\"))
      n = length(unlist(strsplit(dbFileNames[d],"\\\\")))
      
        #get common years
        if(onlyCommonTstep) {
          if(d==1) {
        		tsteps = dbGetQuery(inDB, "SELECT TSTEP FROM TSTEP")$TSTEP
        	} else {
        	  tsteps = dbGetQuery(inDB, paste("SELECT TSTEP FROM TSTEP WHERE TSTEP IN (", paste(tsteps, collapse=","), ")"))$TSTEP
        	}
        }
  
        # Get list of tables in the input database
           tablesDB = dbListTables(inDB,"SELECT ALL")
           
         # Get list of all fields in all tables
           for (f in 1:length(tablesDB)){
             tempdbn = fileName[n]
             tempflds = dbListFields(inDB, tablesDB[f] , "SELECT ALL")
             temptnum  = rep(f,length(tempflds))
             temptname = rep(tablesDB[f], length(tempflds))
             tempfnum  = c(1: length(tempflds))
             temprow  = rep(nrow(dbGetQuery(inDB, toupper(paste("SELECT ",tempflds[1], " FROM ",tablesDB[f], sep = "")))), length(tempflds))              
             tempDes = cbind(tempdbn,temptnum,temptname,tempfnum,tempflds,temprow)
             
             if(f==1){
               tableDes = tempDes
               } else {
               tableDes  = rbind(tableDes ,tempDes)
               }}  
                
         # Convert into dataframe and add a blank column
           t <- as.data.frame(tableDes)
         # add "id" table (tnum) + fields (fnum)
           t$tfname <- paste(t$temptnum ,t$tempfnum,sep = "_")
           colnames(t) <- c(paste("dbname",d,sep="_"),paste("tnum",d,sep="_"), paste("tname",d,sep="_"),paste("fnum",d,sep="_"), paste("fname",d,sep="_"),paste("records",d,sep="_"), paste("id",d,sep="_")) 

   # Add all tables, fields from all database files (Here: all databases have same tables but check for length)    
   continueFurther = T   
   if(d==1) {
        checkTables <- t
     } else{
      # Check if the listed output is same to join ?
        lengthCheck <- dim(checkTables)== dim(t)
        if(lengthCheck[1] == T){
          checkTables <- cbind(checkTables,t) 
        } else {
          continueFurther = F  
          print(paste("Database files 1", dbFileNames[d]," and", d, dbFileNames[d-1], "have different tables and/or fields"))

        }  
     }
}
if( continueFurther == T ) {
# If tables are same then check if length of fields are of same length
for (d in 2:length(dbFileNames)){
    id_n       = paste("id_",d,sep="")
    records_n  = paste("records_",d,sep="")
    flgTname = paste("flgTname_",d,sep="")
    flgFrecords = paste("flgFrecords_",d,sep="")
          
    # check if table-field names of db1 and db[2...n] are same
     checkTables[,flgTname]  <-  checkTables[ ,"id_1"] == checkTables[ ,id_n]
     # if they are same then check if the length of fields are same
     for (r in 1:length(checkTables[,flgTname])){
       if(checkTables[,flgTname][r]== T) {        
        checkTables[,flgFrecords] <-  all(checkTables[ ,"records_1"] %in% checkTables[ ,records_n],checkTables[ ,records_n] %in% checkTables[ ,"records_1"])
       } else {
        checkTables[,flgFrecords][r] <- "FALSE"
       }
     }  
 }
write.csv(checkTables,outCheckFile,row.names=F)
}
#=========================================================================================================
# LIST TABLES AND FIELDS IN THE DATABASE  
  if (masterTable==T & continueFurther == T) {
    # Establish connection to input database
      inDB = dbConnect(m, dbname = dbFileNames[1])
    
    # Get list of tables in the input database
      tablesDB = dbListTables(inDB,"SELECT ALL")
    
    # Get list of all fields in all tables
      for (f in 1:length(tablesDB)){
        tempflds = dbListFields(inDB, tablesDB[f] , "SELECT ALL")
        temptnum  = rep(f,length(tempflds))
        temptname = rep(tablesDB[f], length(tempflds))
        tempfnum  = c(1: length(tempflds))
        temprow  = rep(nrow(dbGetQuery(inDB, toupper(paste("SELECT ",tempflds[1], " FROM ",tablesDB[f], sep = "")))), length(tempflds))
        tempDes = cbind(temptnum,temptname,tempfnum,tempflds,temprow)
        
        if(f==1){
          tableDes = tempDes
          } else {
          tableDes  = rbind(tableDes ,tempDes)
          }}   
    # Convert into dataframe and add a blank column
      t <- as.data.frame(tableDes)
      t$keys <- "NULL" 
      colnames(t) <- c("tnum", "tname", "fnum", "fname", "records","key_fld")
     
    # Write out the list of fields and tables
      write.csv(t, outMasterTable, row.names=F)
    
    # Close database connection 
      dbDisconnect(inDB)
  } 
   
#=========================================================================================================
  if (masterTable==F & continueFurther == T) {
# CREATE QUERY BUILDER FIELDS
  # Read table
    t <- read.csv(outMasterTable)
    colnames(t) <- c("tnum", "tname", "fnum", "fname", "records","key_fld")
  
  # Add keys and fields columns  
    t$keys   = " "
    t$fields = " "
  
  # Loop by tables
    for (i in 1:max(t$tnum)) {
      # split dataframe by table
        t_temp <- t[t$tnum==i,] 
        cntK = 0  
        cntF = 0
        keyName = " "
        fldName = " "
      # loop by field names (rows in the temp table)
        for (f in 1: max(t_temp$fnum)){
          tempFldName <- as.character(unlist(t_temp$fname[f]))
        
            # Check for Keys and get fields for Keys
            if(t_temp$key_fld[f] == "FLDS") {
              cntF=cntF+1
              if (cntF == 1){
                fldName = tempFldName
              } else {
                fldName = paste(as.character(fldName), tempFldName,sep=",")
              }

            } else if (t_temp$key_fld[f] == "KEYS") {
              #"KEYS" or "NULL"
              cntK=cntK+1
              if (cntK == 1) {
                keyName = tempFldName
              } else {
                keyName = paste(as.character(keyName), tempFldName, sep=",")
              }
              
            } # end if statement            

        } # End loop by field      
         
       # Get the table name (use first record, since all table names are same for the subset) 
         tname <- as.character(t_temp$tname[1])
           
       # Make a table with only "TableNames"
         tempSet <- cbind(toupper(tname),toupper(keyName),toupper(fldName))
        
         if(i==1){
          sqlSet  <- tempSet  
         }else {
          sqlSet  <- rbind(sqlSet,tempSet) 
         } 
    }# End loop by table 
    
  # Since all three "TableName", "Keys" and "Fields" are required for SQL query, keep only those rows with all the three  
    sqlSet <- as.data.frame(sqlSet)
    colnames(sqlSet) <- c("TABLE", "KEY", "FLD")
    sqlSet <- sqlSet[sqlSet$KEY != " " & sqlSet$FLD != " ",]
    
#=========================================================================================================
# LOOP THROUGH DBS AND QUERY AND AVERAGE 

# Get query builder fields into a list
  averageFields = list()
  for (a in 1:length(sqlSet$TABLE)){
    averageFields[[a]] = c(tableName=as.character(sqlSet$TABLE[a]),
                           keys=as.character(sqlSet$KEY[a]),
                           fields=as.character(sqlSet$FLD[a]))
  }

# Loop through queries
  for (tableName in tablesDB){
    cat(paste("Table being processed: ", tableName, "\n"))
    
    table_exist = FALSE  #TRUE if table exists in average fields list. Dafault is FALSE
    skip = FALSE # TRUE if number of records in the databases are not same. Default is FALSE
    
    for (i in 1:length(averageFields)){
      if (toupper(tableName) == averageFields[[i]]["tableName"]){
        table_exist = TRUE #table exist in average fields
        tableIndex = i
      }
    }
      
    if (table_exist){
      # Loop through DBs
      for(j in 1:length(dbFileNames)) {
        
        # Connect to input db and query
        cat(paste("Connect to", dbFileNames[j], "\n"))
        inDB = dbConnect(m, dbname = dbFileNames[j])
        
        # Build query with TSTEP filter if desired
        if(onlyCommonTstep) {
          query = buildQuery(averageFields[[tableIndex]], tsteps)
        } else {
          query = buildQuery(averageFields[[tableIndex]])
        }
        cat(paste(query, "\n"))
        tableData = dbGetQuery(inDB, query)
        colnames(tableData) = toupper(colnames(tableData))
        
        # Get fields to average as a vector
        fieldsAsVector = strsplit(averageFields[[tableIndex]]["fields"],",")[[1]]
        
        #convert character/factor fields to numeric
        for(f in fieldsAsVector) {
          tableData[,f] <- as.numeric(as.character(tableData[,f]))
        }        
        
        # Add together
        if(j==1) {
          sumData = tableData
        } else {
          if(nrow(sumData) == nrow(tableData)) {
            #loop through fields
            for(f in fieldsAsVector) {
              cat(paste("Add", f, "\n"))
              sumData[,f] = sumData[,f] + tableData[,f] #assumes row order is the same across DBs
            }
          } else {
            cat("Different number of resulting records across databases for the query. Skipping. \n")
            sumData = tableData
            skip = TRUE
          }
        }
        dbDisconnect(inDB)
      }
      
      if (!skip){
        # Simple average of results
        for(f in fieldsAsVector) {
          cat(paste("Average", f, "\n"))
          sumData[,f] = sumData[,f] / length(dbFileNames)
        }        
      }

    } else {
      print(paste("Table is NULL in the master table: ", tableName))
      inDB = dbConnect(m, dbname = dbFileNames[1]) #get data of the first database
      sumData = dbGetQuery(inDB, paste("SELECT * FROM ", tableName))
    }
    
    # Write result to output db
    dbWriteTable(db, tableName, sumData, row.names=F, append=F)
       
  }
  
}
#=========================================================================================================
# CLOSE AND COMPLETE
  # Close database
  if (masterTable==F){
    dbDisconnect(db)
  }
    
    cat(paste("SWIM Average VIZ DB Complete at", Sys.time(), "\n"))
if (continueFurther == F) {
   print(paste("Database files ", dbFileNames[d]," and", d, dbFileNames[d-1], "have different tables"))
   }
#=========================================================================================================