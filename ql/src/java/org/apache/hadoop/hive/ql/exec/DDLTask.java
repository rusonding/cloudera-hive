/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.exec;

import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.MetaStoreUtils;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.InvalidOperationException;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.ql.metadata.CheckResult;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.HiveMetaStoreChecker;
import org.apache.hadoop.hive.ql.metadata.InvalidTableException;
import org.apache.hadoop.hive.ql.metadata.Partition;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.plan.AddPartitionDesc;
import org.apache.hadoop.hive.ql.plan.DDLWork;
import org.apache.hadoop.hive.ql.plan.MsckDesc;
import org.apache.hadoop.hive.ql.plan.alterTableDesc;
import org.apache.hadoop.hive.ql.plan.createTableDesc;
import org.apache.hadoop.hive.ql.plan.descTableDesc;
import org.apache.hadoop.hive.ql.plan.dropTableDesc;
import org.apache.hadoop.hive.ql.plan.showPartitionsDesc;
import org.apache.hadoop.hive.ql.plan.showTablesDesc;
import org.apache.hadoop.hive.serde.Constants;
import org.apache.hadoop.hive.serde2.MetadataTypedColumnsetSerDe;
import org.apache.hadoop.hive.serde2.dynamic_type.DynamicSerDe;
import org.apache.hadoop.util.StringUtils;

/**
 * DDLTask implementation
 * 
 **/
public class DDLTask extends Task<DDLWork> implements Serializable {
  private static final long serialVersionUID = 1L;
  static final private Log LOG = LogFactory.getLog("hive.ql.exec.DDLTask");

  transient HiveConf conf;
  static final private int separator  = Utilities.tabCode;
  static final private int singleQuote  = '\'';
  static final private int terminator = Utilities.newLineCode;
  
  public void initialize(HiveConf conf) {
    super.initialize(conf);
    this.conf = conf;
  }

  public int execute() {

    // Create the db
    Hive db;
    FileSystem fs;
    try {
      db = Hive.get(conf);
      fs = FileSystem.get(conf);

      createTableDesc crtTbl = work.getCreateTblDesc();
      if (crtTbl != null) {
        return createTable(db, crtTbl);
      }

      dropTableDesc dropTbl = work.getDropTblDesc();
      if (dropTbl != null) {
        return dropTable(db, dropTbl);
      }

      alterTableDesc alterTbl = work.getAlterTblDesc();
      if (alterTbl != null) {
        return alterTable(db, alterTbl);
      }
      
      AddPartitionDesc addPartitionDesc = work.getAddPartitionDesc();
      if (addPartitionDesc != null) {
        return addPartition(db, addPartitionDesc);
      }      
      
      MsckDesc msckDesc = work.getMsckDesc();
      if (msckDesc != null) {
        return msck(db, fs, msckDesc);
      }      

      descTableDesc descTbl = work.getDescTblDesc();
      if (descTbl != null) {
        return describeTable(db, fs, descTbl);
      }

      showTablesDesc showTbls = work.getShowTblsDesc();
      if (showTbls != null) {
        return showTables(db, fs, showTbls);
      }

      showPartitionsDesc showParts = work.getShowPartsDesc();
      if (showParts != null) {
        return showPartitions(db, fs, showParts);
      }

    } catch (InvalidTableException e) {
      console.printError("Table " + e.getTableName() + " does not exist");
      LOG.debug(StringUtils.stringifyException(e));
      return 1;
    } catch (HiveException e) {
      console.printError("FAILED: Error in metadata: " + e.getMessage(), "\n" + StringUtils.stringifyException(e));
      LOG.debug(StringUtils.stringifyException(e));
      return 1;
    } catch (Exception e) {
      console.printError("Failed with exception " +   e.getMessage(), "\n" + StringUtils.stringifyException(e));
      return (1);
    }
    assert false;
    return 0;
  }

  /**
   * Add a partition to a table.
   * @param db Database to add the partition to.
   * @param addPartitionDesc Add this partition.
   * @return Returns 0 when execution succeeds and above 0 if it fails.
   * @throws HiveException 
   */
  private int addPartition(Hive db, AddPartitionDesc addPartitionDesc) 
    throws HiveException {
    
    Table tbl = db.getTable(addPartitionDesc.getDbName(), 
        addPartitionDesc.getTableName());
    
    if(addPartitionDesc.getLocation() == null) {
      db.createPartition(tbl, addPartitionDesc.getPartSpec());
    } else {
      //set partition path relative to table
      db.createPartition(tbl, addPartitionDesc.getPartSpec(), 
          new Path(tbl.getPath(), addPartitionDesc.getLocation()));
    }
    
    return 0;
  }

  /**
   * MetastoreCheck, see if the data in the metastore matches
   * what is on the dfs.
   * Current version checks for tables and partitions that
   * are either missing on disk on in the metastore.
   * 
   * @param db The database in question.
   * @param fs FileSystem that will contain the file written.
   * @param msckDesc Information about the tables and partitions
   * we want to check for.
   * @return Returns 0 when execution succeeds and above 0 if it fails.
   */
  private int msck(Hive db, FileSystem fs, MsckDesc msckDesc) {
    
    CheckResult result = new CheckResult();
    try {
      HiveMetaStoreChecker checker = new HiveMetaStoreChecker(db, fs);
      checker.checkMetastore(
        MetaStoreUtils.DEFAULT_DATABASE_NAME, msckDesc.getTableName(), 
        msckDesc.getPartitionSpec(),
        result);
    } catch (HiveException e) {
      LOG.warn("Failed to run metacheck: ", e);
      return 1;
    } catch (IOException e) {
      LOG.warn("Failed to run metacheck: ", e);
      return 1;
    } finally {
            
      BufferedWriter resultOut = null;
      try {
        resultOut = new BufferedWriter(
            new OutputStreamWriter(fs.create(msckDesc.getResFile())));
        
        boolean firstWritten = false;
        firstWritten |= writeMsckResult(result.getTablesNotInMs(), 
            "Tables not in metastore:", resultOut, firstWritten);
        firstWritten |= writeMsckResult(result.getTablesNotOnFs(), 
            "Tables missing on filesystem:", resultOut, firstWritten);      
        firstWritten |= writeMsckResult(result.getPartitionsNotInMs(), 
            "Partitions not in metastore:", resultOut, firstWritten);
        firstWritten |= writeMsckResult(result.getPartitionsNotOnFs(), 
            "Partitions missing from filesystem:", resultOut, firstWritten);      
      } catch (IOException e) {
        LOG.warn("Failed to save metacheck output: ", e);
        return 1;
      } finally {
        if(resultOut != null) {
          try {
            resultOut.close();
          } catch (IOException e) {
            LOG.warn("Failed to close output file: ", e);
            return 1;
          }
        }
      }
    }
    
    return 0;
  }

  /**
   * Write the result of msck to a writer.
   * @param result The result we're going to write
   * @param msg Message to write.
   * @param out Writer to write to
   * @param wrote if any previous call wrote data
   * @return true if something was written
   * @throws IOException In case the writing fails
   */
  private boolean writeMsckResult(List<? extends Object> result, String msg, 
      Writer out, boolean wrote) throws IOException {
    
    if(!result.isEmpty()) { 
      if(wrote) {
        out.write(terminator);
      }
      
      out.write(msg);
      for (Object entry : result) {
        out.write(separator);
        out.write(entry.toString());
      }
      return true;
    }
    
    return false;
  }

  /**
   * Write a list of partitions to a file.
   * 
   * @param db The database in question.
   * @param fs FileSystem that will contain the file written.
   * @param showParts These are the partitions we're interested in.
   * @return Returns 0 when execution succeeds and above 0 if it fails.
   * @throws HiveException Throws this exception if an unexpected error occurs.
   */
  private int showPartitions(Hive db, FileSystem fs,
      showPartitionsDesc showParts) throws HiveException {
    // get the partitions for the table and populate the output
    String tabName = showParts.getTabName();
    Table tbl = null;
    List<String> parts = null;

    tbl = db.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, tabName);

    if (!tbl.isPartitioned()) {
      console.printError("Table " + tabName + " is not a partitioned table");
      return 1;
    }

    parts = db.getPartitionNames(MetaStoreUtils.DEFAULT_DATABASE_NAME, tbl
        .getName(), Short.MAX_VALUE);

    // write the results in the file
    try {
      DataOutput outStream = (DataOutput) fs.create(showParts.getResFile());
      Iterator<String> iterParts = parts.iterator();
      boolean firstCol = true;
      while (iterParts.hasNext()) {
        if (!firstCol)
          outStream.write(terminator);
        outStream.write(iterParts.next().getBytes("UTF-8"));
        firstCol = false;
      }
      ((FSDataOutputStream) outStream).close();
    } catch (FileNotFoundException e) {
      LOG.info("show partitions: " + StringUtils.stringifyException(e));
      return 1;
    } catch (IOException e) {
      LOG.info("show partitions: " + StringUtils.stringifyException(e));
      return 1;
    }
    return 0;
  }

  /**
   * Write a list of the tables in the database to a file.
   * 
   * @param db The database in question.
   * @param fs FileSystem that will contain the file written.
   * @param showTbls These are the tables we're interested in.
   * @return Returns 0 when execution succeeds and above 0 if it fails.
   * @throws HiveException Throws this exception if an unexpected error occurs.
   */
  private int showTables(Hive db, FileSystem fs, showTablesDesc showTbls)
      throws HiveException {
    // get the tables for the desired pattenn - populate the output stream
    List<String> tbls = null;
    if (showTbls.getPattern() != null) {
      LOG.info("pattern: " + showTbls.getPattern());
      tbls = db.getTablesByPattern(showTbls.getPattern());
      LOG.info("results : " + tbls.size());
    } else
      tbls = db.getAllTables();

    // write the results in the file
    try {
      DataOutput outStream = (DataOutput) fs.create(showTbls.getResFile());
      SortedSet<String> sortedTbls = new TreeSet<String>(tbls);
      Iterator<String> iterTbls = sortedTbls.iterator();
      boolean firstCol = true;
      while (iterTbls.hasNext()) {
        if (!firstCol)
          outStream.write(separator);
        outStream.write(iterTbls.next().getBytes("UTF-8"));
        firstCol = false;
      }
      ((FSDataOutputStream) outStream).close();
    } catch (FileNotFoundException e) {
      LOG.info("show table: " + StringUtils.stringifyException(e));
      return 1;
    } catch (IOException e) {
      LOG.info("show table: " + StringUtils.stringifyException(e));
      return 1;
    }
    return 0;
  }

  /**
   * Write the description of a table to a file.
   * 
   * @param db The database in question.
   * @param fs FileSystem that will contain the file written.
   * @param descTbl This is the table we're interested in.
   * @return Returns 0 when execution succeeds and above 0 if it fails.
   * @throws HiveException Throws this exception if an unexpected error occurs.
   */
  private int describeTable(Hive db, FileSystem fs, descTableDesc descTbl)
      throws HiveException {
    String colPath = descTbl.getTableName();
    String tableName = colPath.substring(0,
        colPath.indexOf('.') == -1 ? colPath.length() : colPath.indexOf('.'));

    // describe the table - populate the output stream
    Table tbl = db.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, tableName, false);
    Partition part = null;
    try {
      if (tbl == null) {
        DataOutput outStream = (DataOutput) fs.open(descTbl.getResFile());
        String errMsg = "Table " + tableName + " does not exist";
        outStream.write(errMsg.getBytes("UTF-8"));
        ((FSDataOutputStream) outStream).close();
        return 0;
      }
      if (descTbl.getPartSpec() != null) {
        part = db.getPartition(tbl, descTbl.getPartSpec(), false);
        if (part == null) {
          DataOutput outStream = (DataOutput) fs.open(descTbl.getResFile());
          String errMsg = "Partition " + descTbl.getPartSpec() + " for table "
              + tableName + " does not exist";
          outStream.write(errMsg.getBytes("UTF-8"));
          ((FSDataOutputStream) outStream).close();
          return 0;
        }
        tbl = part.getTable();
      }
    } catch (FileNotFoundException e) {
      LOG.info("describe table: " + StringUtils.stringifyException(e));
      return 1;
    } catch (IOException e) {
      LOG.info("describe table: " + StringUtils.stringifyException(e));
      return 1;
    }

    try {

      LOG.info("DDLTask: got data for " + tbl.getName());

      // write the results in the file
      DataOutput os = (DataOutput) fs.create(descTbl.getResFile());
      List<FieldSchema> cols = null;
      if (colPath.equals(tableName)) {
        cols = tbl.getCols();
        if (part != null) {
          cols = part.getTPartition().getSd().getCols();
        }
      } else {
        cols = Hive.getFieldsFromDeserializer(colPath, tbl.getDeserializer());
      }

      Iterator<FieldSchema> iterCols = cols.iterator();
      boolean firstCol = true;
      while (iterCols.hasNext()) {
        if (!firstCol)
          os.write(terminator);
        FieldSchema col = iterCols.next();
        os.write(col.getName().getBytes("UTF-8"));
        os.write(separator);
        os.write(col.getType().getBytes("UTF-8"));
        if (col.getComment() != null) {
          os.write(separator);
          os.write(singleQuote);
          os.write(col.getComment().getBytes("UTF-8"));
          os.write(singleQuote);
        }
        firstCol = false;
      }

      if (tableName.equals(colPath)) {
        // also return the partitioning columns
        List<FieldSchema> partCols = tbl.getPartCols();
        Iterator<FieldSchema> iterPartCols = partCols.iterator();
        while (iterPartCols.hasNext()) {
          os.write(terminator);
          FieldSchema col = iterPartCols.next();
          os.write(col.getName().getBytes("UTF-8"));
          os.write(separator);
          os.write(col.getType().getBytes("UTF-8"));
          if (col.getComment() != null) {
            os.write(separator);
            os.write(col.getComment().getBytes("UTF-8"));
          }
        }

        // if extended desc table then show the complete details of the table
        if (descTbl.isExt()) {
          if (part != null) {
            // show partition informatio
            os.write("\n\nDetailed Partition Information:\n".getBytes("UTF-8"));
            os.write(part.getTPartition().toString().getBytes("UTF-8"));
          } else {
            os.write("\nDetailed Table Information:\n".getBytes("UTF-8"));
            os.write(tbl.getTTable().toString().getBytes("UTF-8"));
          }
        }
      }

      LOG.info("DDLTask: written data for " + tbl.getName());
      ((FSDataOutputStream) os).close();

    } catch (FileNotFoundException e) {
      LOG.info("describe table: " + StringUtils.stringifyException(e));
      return 1;
    } catch (IOException e) {
      LOG.info("describe table: " + StringUtils.stringifyException(e));
      return 1;
    }
    return 0;
  }

  /**
   * Alter a given table.
   * 
   * @param db The database in question.
   * @param alterTbl This is the table we're altering.
   * @return Returns 0 when execution succeeds and above 0 if it fails.
   * @throws HiveException Throws this exception if an unexpected error occurs.
   */
  private int alterTable(Hive db, alterTableDesc alterTbl) throws HiveException {
    // alter the table
    Table tbl = db.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, alterTbl.getOldName());
    if (alterTbl.getOp() == alterTableDesc.alterTableTypes.RENAME)
      tbl.getTTable().setTableName(alterTbl.getNewName());
    else if (alterTbl.getOp() == alterTableDesc.alterTableTypes.ADDCOLS) {
      List<FieldSchema> newCols = alterTbl.getNewCols();
      List<FieldSchema> oldCols = tbl.getCols();
      if (tbl.getSerializationLib().equals("org.apache.hadoop.hive.serde.thrift.columnsetSerDe")) {
        console
            .printInfo("Replacing columns for columnsetSerDe and changing to typed SerDe");
        tbl.setSerializationLib(MetadataTypedColumnsetSerDe.class.getName());
        tbl.getTTable().getSd().setCols(newCols);
      } else {
        // make sure the columns does not already exist
        Iterator<FieldSchema> iterNewCols = newCols.iterator();
        while (iterNewCols.hasNext()) {
          FieldSchema newCol = iterNewCols.next();
          String newColName = newCol.getName();
          Iterator<FieldSchema> iterOldCols = oldCols.iterator();
          while (iterOldCols.hasNext()) {
            String oldColName = iterOldCols.next().getName();
            if (oldColName.equalsIgnoreCase(newColName)) {
              console.printError("Column '" + newColName + "' exists");
              return 1;
            }
          }
          oldCols.add(newCol);
        }
        tbl.getTTable().getSd().setCols(oldCols);
      }
    } else if (alterTbl.getOp() == alterTableDesc.alterTableTypes.REPLACECOLS) {
      // change SerDe to MetadataTypedColumnsetSerDe if it is columnsetSerDe
      if (tbl.getSerializationLib().equals("org.apache.hadoop.hive.serde.thrift.columnsetSerDe")) {
        console
            .printInfo("Replacing columns for columnsetSerDe and changing to typed SerDe");
        tbl.setSerializationLib(MetadataTypedColumnsetSerDe.class.getName());
      } else if (!tbl.getSerializationLib().equals(
          MetadataTypedColumnsetSerDe.class.getName())
          && !tbl.getSerializationLib().equals(
          DynamicSerDe.class.getName())) {
        console
            .printError("Replace columns is not supported for this table. SerDe may be incompatible.");
        return 1;
      }
      tbl.getTTable().getSd().setCols(alterTbl.getNewCols());
    } else if (alterTbl.getOp() == alterTableDesc.alterTableTypes.ADDPROPS) {
      tbl.getTTable().getParameters().putAll(alterTbl.getProps());
    } else if (alterTbl.getOp() == alterTableDesc.alterTableTypes.ADDSERDEPROPS) {
      tbl.getTTable().getSd().getSerdeInfo().getParameters().putAll(
          alterTbl.getProps());
    } else if (alterTbl.getOp() == alterTableDesc.alterTableTypes.ADDSERDE) {
      tbl.setSerializationLib(alterTbl.getSerdeName());
      if ((alterTbl.getProps() != null) && (alterTbl.getProps().size() > 0))
        tbl.getTTable().getSd().getSerdeInfo().getParameters().putAll(
            alterTbl.getProps());
      // since serde is modified then do the appropriate things to reset columns
      // etc
      tbl.reinitSerDe();
      tbl.setFields(Hive.getFieldsFromDeserializer(tbl.getName(), tbl
          .getDeserializer()));
    } else {
      console.printError("Unsupported Alter commnad");
      return 1;
    }

    // set last modified by properties
    try {
      tbl.setProperty("last_modified_by", conf.getUser());
    } catch (IOException e) {
      console.printError("Unable to get current user: " + e.getMessage(), StringUtils.stringifyException(e));
      return 1;
    }
    tbl.setProperty("last_modified_time", Long.toString(System
        .currentTimeMillis() / 1000));

    try {
      tbl.checkValidity();
    } catch (HiveException e) {
      console.printError("Invalid table columns : " + e.getMessage(), StringUtils.stringifyException(e));
      return 1;
    }

    try {
      db.alterTable(alterTbl.getOldName(), tbl);
    } catch (InvalidOperationException e) {
      console.printError("Invalid alter operation: " + e.getMessage());
      LOG.info("alter table: " + StringUtils.stringifyException(e));
      return 1;
    } catch (HiveException e) {
      return 1;
    }
    return 0;
  }

  /**
   * Drop a given table.
   * 
   * @param db The database in question.
   * @param dropTbl This is the table we're dropping.
   * @return Returns 0 when execution succeeds and above 0 if it fails.
   * @throws HiveException Throws this exception if an unexpected error occurs.
   */
  private int dropTable(Hive db, dropTableDesc dropTbl) throws HiveException {
    if (dropTbl.getPartSpecs() == null) {
      // drop the table
      db.dropTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, dropTbl.getTableName());
    } else {
      // drop partitions in the list
      Table tbl = db.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, dropTbl.getTableName());
      List<Partition> parts = new ArrayList<Partition>();
      for (Map<String, String> partSpec : dropTbl.getPartSpecs()) {
        Partition part = db.getPartition(tbl, partSpec, false);
        if (part == null) {
          console.printInfo("Partition " + partSpec + " does not exist.");
        } else {
          parts.add(part);
        }
      }
      // drop all existing partitions from the list
      for (Partition partition : parts) {
        console.printInfo("Dropping the partition " + partition.getName());
        db.dropPartition(MetaStoreUtils.DEFAULT_DATABASE_NAME, dropTbl
            .getTableName(), partition.getValues(), true); // drop data for the
                                                           // partition
      }
    }
    return 0;
  }

  /**
   * Create a new table.
   * 
   * @param db The database in question.
   * @param crtTbl This is the table we're creating.
   * @return Returns 0 when execution succeeds and above 0 if it fails.
   * @throws HiveException Throws this exception if an unexpected error occurs.
   */
  private int createTable(Hive db, createTableDesc crtTbl) throws HiveException {
    // create the table
    Table tbl = new Table(crtTbl.getTableName());
    StorageDescriptor tblStorDesc = tbl.getTTable().getSd();
    if (crtTbl.getBucketCols() != null)
      tblStorDesc.setBucketCols(crtTbl.getBucketCols());
    if (crtTbl.getSortCols() != null)
      tbl.setSortCols(crtTbl.getSortCols());
    if (crtTbl.getPartCols() != null)
      tbl.setPartCols(crtTbl.getPartCols());
    if (crtTbl.getNumBuckets() != -1)
      tblStorDesc.setNumBuckets(crtTbl.getNumBuckets());

    if (crtTbl.getSerName() != null) {
      tbl.setSerializationLib(crtTbl.getSerName());
      if (crtTbl.getMapProp() != null) {
        Iterator<Map.Entry<String, String>> iter = crtTbl.getMapProp()
            .entrySet().iterator();
        while (iter.hasNext()) {
          Map.Entry<String, String> m = (Map.Entry<String, String>) iter.next();
          tbl.setSerdeParam(m.getKey(), m.getValue());
        }
      }
    } else {
      if (crtTbl.getFieldDelim() != null) {
        tbl.setSerdeParam(Constants.FIELD_DELIM, crtTbl.getFieldDelim());
        tbl.setSerdeParam(Constants.SERIALIZATION_FORMAT, crtTbl
            .getFieldDelim());
      }

      if (crtTbl.getCollItemDelim() != null)
        tbl
            .setSerdeParam(Constants.COLLECTION_DELIM, crtTbl
                .getCollItemDelim());
      if (crtTbl.getMapKeyDelim() != null)
        tbl.setSerdeParam(Constants.MAPKEY_DELIM, crtTbl.getMapKeyDelim());
      if (crtTbl.getLineDelim() != null)
        tbl.setSerdeParam(Constants.LINE_DELIM, crtTbl.getLineDelim());
    }

    /**
     * If the user didn't specify a SerDe, and any of the columns are not of type String, 
     * we will have to use DynamicSerDe instead.
     */
    if (crtTbl.getSerName() == null) {
      boolean useDynamicSerDe = false;
      if (crtTbl.getCols() != null) {
        for (FieldSchema field: crtTbl.getCols()) {
          if (!Constants.STRING_TYPE_NAME.equalsIgnoreCase(field.getType())) {
            useDynamicSerDe = true;
          }
        }
      }
      if (useDynamicSerDe) {
        LOG.info("Default to DynamicSerDe for table " + crtTbl.getTableName() );
        tbl.setSerializationLib(org.apache.hadoop.hive.serde2.dynamic_type.DynamicSerDe.class.getName());
        tbl.setSerdeParam(org.apache.hadoop.hive.serde.Constants.SERIALIZATION_FORMAT, org.apache.hadoop.hive.serde2.thrift.TCTLSeparatedProtocol.class.getName());
      }
    }

    if (crtTbl.getComment() != null)
      tbl.setProperty("comment", crtTbl.getComment());
    if (crtTbl.getLocation() != null)
      tblStorDesc.setLocation(crtTbl.getLocation());

    tbl.setInputFormatClass(crtTbl.getInputFormat());
    tbl.setOutputFormatClass(crtTbl.getOutputFormat());

    if (crtTbl.isExternal())
      tbl.setProperty("EXTERNAL", "TRUE");

    // If the sorted columns is a superset of bucketed columns, store this fact.
    // It can be later used to
    // optimize some group-by queries. Note that, the order does not matter as
    // long as it in the first
    // 'n' columns where 'n' is the length of the bucketed columns.
    if ((tbl.getBucketCols() != null) && (tbl.getSortCols() != null)) {
      List<String> bucketCols = tbl.getBucketCols();
      List<Order> sortCols = tbl.getSortCols();

      if ((sortCols.size() > 0) && (sortCols.size() >= bucketCols.size())) {
        boolean found = true;

        Iterator<String> iterBucketCols = bucketCols.iterator();
        while (iterBucketCols.hasNext()) {
          String bucketCol = iterBucketCols.next();
          boolean colFound = false;
          for (int i = 0; i < bucketCols.size(); i++) {
            if (bucketCol.equals(sortCols.get(i).getCol())) {
              colFound = true;
              break;
            }
          }
          if (colFound == false) {
            found = false;
            break;
          }
        }
        if (found)
          tbl.setProperty("SORTBUCKETCOLSPREFIX", "TRUE");
      }
    }

    try {
      tbl.setOwner(conf.getUser());
    } catch (IOException e) {
      console.printError("Unable to get current user: " + e.getMessage(), StringUtils.stringifyException(e));
      return 1;
    }
    // set create time
    tbl.getTTable().setCreateTime((int) (System.currentTimeMillis() / 1000));

    if (crtTbl.getCols() != null) {
      tbl.setFields(crtTbl.getCols());
    }

    // create the table
    db.createTable(tbl, crtTbl.getIfNotExists());
    return 0;
  }
}
