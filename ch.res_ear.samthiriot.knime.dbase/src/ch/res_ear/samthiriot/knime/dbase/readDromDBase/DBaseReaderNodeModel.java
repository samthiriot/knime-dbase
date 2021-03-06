package ch.res_ear.samthiriot.knime.dbase.readDromDBase;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import nl.knaw.dans.common.dbflib.DbfLibException;
import nl.knaw.dans.common.dbflib.Field;
import nl.knaw.dans.common.dbflib.IfNonExistent;
import nl.knaw.dans.common.dbflib.Record;
import nl.knaw.dans.common.dbflib.Table;


/**
 * This is the model implementation of DBaseReader.
 * Reads data from a dBase database file or URL location. 
 * Upon executing the node will scan the input file to determine number 
 * and types of the columns and output a table with the auto-guessed structure.
 *
 * @author Samuel Thiriot
 */
public class DBaseReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(DBaseReaderNodeModel.class);
        
    /**
     * Stores our current settings
     */
    private DBaseReaderConfig m_config;

    
    /**
     * Constructor for the node model.
     */
    protected DBaseReaderNodeModel() {
    
        super(0, 1);
        
    }
    
    /**
     * @param context the node creation context
     */
    public DBaseReaderNodeModel(final NodeCreationContext context) {
        this();
        m_config = new DBaseReaderConfig();
        m_config.setLocation(context.getUrl().toString());
    }

    /** @return the config */
    protected DBaseReaderConfig getConfig() {
        return m_config;
    }

    protected static final DataType getKnimeDataTypeForDBaseField(Field currentField) {
    	
    	DataType knimeType;
    	    	
    	switch (currentField.getType()) {
    		
    	case NUMBER: 
    	case FLOAT:
    		// this is an integer in case the count of decimals is null
    		if (currentField.getDecimalCount() == 0)
    			knimeType = IntCell.TYPE;
    		else
    			knimeType = DoubleCell.TYPE;
    		break;
    	case CHARACTER:
    	case MEMO:
    		knimeType = StringCell.TYPE;
    		break;
    	case LOGICAL:
    		knimeType = BooleanCell.TYPE;
    		break;
    	case DATE:
    		// TODO replace with org.knime.time?
    		knimeType = DateAndTimeCell.TYPE;
    		break;
    	case PICTURE:
    		logger.warn("Column "+currentField.getName()+" contains pictures in the DBase format, "+
    					"but will be considered as binary");
    	case GENERAL:
    	case BINARY:
	    		knimeType = BinaryObjectDataCell.TYPE;
    		break;
    	default:
    		logger.warn("Column "+currentField.getName()+" contains an unexpected type and will be ignored.");
    		knimeType = null; 
    		break;
    	}
    	
    	return knimeType;
    }
    
    private static final DataColumnSpec[] getKnimeDataColSpecsForDBaseFields(List<Field> fields) {
	    
    	// will create the data columns in KNIME based on the specs from the DBase file 
    	DataColumnSpec[] allColSpecs = new DataColumnSpec[fields.size()];

	    // ... and creata a map going from the col idx to the field name
	    for (int i=0; i<fields.size(); i++) {
	    	
	    	final Field currentField = fields.get(i);
	    	
	    	DataType knimeType = getKnimeDataTypeForDBaseField(currentField);
	    	
	    	allColSpecs[i] = new DataColumnSpecCreator(
	    			currentField.getName(), 
	    			knimeType
	    			).createSpec();
	    	
	    }
	    
	    return allColSpecs;
    }
    
    private final static DataCell[] decodeDBFields(
    									final List<Field> fields, 
    									Record currentRecord, 
    									ExecutionContext ctxt,
    									boolean trimStrings) {
    	
    	BinaryObjectCellFactory binaryCellFactory = new BinaryObjectCellFactory(ctxt);
    	
    	DataCell[] results = new DataCell[fields.size()];
    	
    	int currentIdx = 0;
    	int skipped = 0;
    	for (Field currentField: fields) {
    		
    		switch (currentField.getType()) {
        		//results[currentIdx] = IntCellFactory.create(currentRecord.getNumberValue(currentField.getName()).intValue());  
        		//break;
        	case NUMBER:
        	case FLOAT:
        		if (currentField.getDecimalCount() == 0)
        			results[currentIdx] = IntCellFactory.create(currentRecord.getNumberValue(currentField.getName()).intValue());
        		else 
        			results[currentIdx] = DoubleCellFactory.create(currentRecord.getNumberValue(currentField.getName()).doubleValue());
        		break;
        	case CHARACTER:
        	case MEMO:
        		String strVal = currentRecord.getStringValue(currentField.getName());
        		if (trimStrings)
        			strVal = strVal.trim();
        		results[currentIdx] = StringCellFactory.create(strVal);
        		break;
        	case LOGICAL:
        		results[currentIdx] = BooleanCellFactory.create(currentField.getName());
        		break;
        	case DATE:
        		// TODO replace with org.knime.time?
        		// TODO avoid Java deprecated?!
        		final Date currentValue = currentRecord.getDateValue(currentField.getName());
        		results[currentIdx] = new DateAndTimeCell(
        				currentValue.getYear(), 
        				currentValue.getMonth(), 
        				currentValue.getDay(), 
        				currentValue.getHours(), 
        				currentValue.getMinutes(), 
        				currentValue.getSeconds());
        		break;
        	case PICTURE:
        	case GENERAL:
        	case BINARY:
        		try {
					results[currentIdx] = binaryCellFactory.create(currentRecord.getRawValue(currentField));
				} catch (IOException e) {
					logger.warn("unable to convert the binary value for "+currentField.getName(), e);
				} catch (DbfLibException e) {
					logger.warn("unable to convert the binary value for "+currentField.getName(), e);
				}
        		break;
        	default:
        		// quietly skip it
        		skipped++;
        		break;
        	}
    		
    		currentIdx++;
    	}
    	
    	if (skipped > 0) {
    		return Arrays.copyOfRange(results, 0, results.length-skipped);
    	}
    	
    	return results;
    }

    protected Table getDBaseTable() throws InvalidSettingsException {
 
    	final String filename = m_config.getLocation();
    	if (filename == null)
	    	throw new InvalidSettingsException("No filename provided");

    	logger.debug("opening DBaser file "+filename);

    	// check this path can be accessed
        CheckUtils.checkSourceFile(filename);

        // convert to an url, so we accept path relative to workflows
        URL url;
		try {
			url = FileUtil.toURL(filename);
		} catch (InvalidPathException | MalformedURLException e2) {
			e2.printStackTrace();
			throw new InvalidSettingsException("unable to open URL "+filename+": "+e2.getMessage());
		}
        
		File file = null;
		if (FileUtil.isWindowsNetworkMount(url)) {
			
			// there are errors on network mount if the file is open by 
			// several programs. 
			// create a local tmp copy, its easier and better.=
			logger.debug("doing a local copy of the file to avoid windows share issues");
			try {
				file = FileUtil.createTempFile("dbase_", ".dbf", true);
				
				FileUtils.copyInputStreamToFile(
						url.openStream(), 
						file);
				
			} catch (IOException e) {

				throw new RuntimeException("error when reading the DBase file "+filename+": "+e.getMessage());
			}
			

		} else {
			// just read this file.
			file = new File(filename);
	        if (!file.canRead())
		    	throw new InvalidSettingsException("Cannot read file "+file);

		}
		
        // open the table
        Table table = new Table(file);
		try {
		    table.open(IfNonExistent.ERROR);
		} catch (CorruptedTableException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("the database "+filename+" seems corrupted", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("error reading data from database "+filename, e);
		} 

		return table;
		
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {


    	Table table = null;
    	try {
	        // open the table
	    	table = getDBaseTable();
	
	    	// read the fields from the table...
		    final List<Field> fields = table.getFields();
		    
		    // will create the data columns in KNIME based on the specs from the DBase file 
		    DataColumnSpec[] allColSpecs = getKnimeDataColSpecsForDBaseFields(fields);
		 
	        // the data table spec of the single output table, 
	        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
	        
	        // the execution context will provide us with storage capacity, in this
	        // case a data container to which we will add rows sequentially
	        // Note, this container can also handle arbitrary big data tables, it
	        // will buffer to disc if necessary.
	        BufferedDataContainer container = exec.createDataContainer(outputSpec);
	        
	        
	        final boolean readDeleted = m_config.getReadDeletedRows();
	        final boolean trimStrings = m_config.getTrimStrings();
	        
	        Iterator<Record> itRecords = table.recordIterator(readDeleted);
	        
	        int maxLines = m_config.getLimitRowsCount();
	        if (maxLines < 0) {
	        	maxLines = table.getRecordCount();
	        	logger.debug("will read all the "+maxLines+" lines");
	        } else {
	        	logger.info("will only read the "+maxLines+" first lines (as defined by settings)");
	        }
	        int currentLine = 0;
	        
	        
	        while (itRecords.hasNext() && (currentLine < maxLines)) {
	        	Record currentRecord = itRecords.next();
	        	
	        	container.addRowToTable(
	        			new DefaultRow(
		        			new RowKey("Row " + currentLine), 
		        			decodeDBFields(fields, currentRecord, exec, trimStrings)
		        			)
	        			);
	            
	        	currentLine++;
	        	
	        	if (currentLine % 10 == 0) { 
		            // check if the execution monitor was canceled
		            exec.checkCanceled();
		            exec.setProgress(
		            		(double)currentLine / maxLines, 
		            		"Adding row " + currentLine);
	        	}
	        }
	        
	        pushFlowVariableInt("count", maxLines);
	        
	        // once we are done, we close the container and return its table
	        container.close();
	        BufferedDataTable out = container.getTable();
	        return new BufferedDataTable[]{out};
	        
    	} finally {
    		if (table != null)
				try {
					table.close();
				} catch (IOException e) {
					logger.warn("error when closing the dbase file "+ m_config.getLocation()+": "+e.getMessage());
				}
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
    	if (m_config == null) {
            throw new InvalidSettingsException("No settings available");
        }
    	
    	// user parameters are available.
    	// we try to decode the fields without reading the actual data.
    	Table table = null;
    	try {
	    	table = getDBaseTable();

	    	// read the fields from the table...
		    final List<Field> fields = table.getFields();
		    
		    // will create the data columns in KNIME based on the specs from the DBase file 
		    DataColumnSpec[] allColSpecs = getKnimeDataColSpecsForDBaseFields(fields);
		 
	        // the data table spec of the single output table, 
	        DataTableSpec outputSpecs[] = new DataTableSpec[] { new DataTableSpec(allColSpecs) };
	        
	        return outputSpecs;
	        
    	} catch (RuntimeException e) {
    		e.printStackTrace();
    		// if anything goes wrong, we are not able to provide more detailed info
    		throw new InvalidSettingsException("error while reading the table: "+e.getMessage());
            //return new DataTableSpec[]{null};
    	} finally {
    		if (table != null)
				try {
					table.close();
				} catch (IOException e) {
					logger.warn("error when closing the dbase file "+ m_config.getLocation()+": "+e.getMessage());
				}
    	}
        
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        if (m_config != null) {
            m_config.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
                    
        DBaseReaderConfig config = new DBaseReaderConfig();
        config.loadSettingsInModel(settings);
        m_config = config;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        new DBaseReaderConfig().loadSettingsInModel(settings);

        // TODO valide !
        // for instance no file ?
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
       // nothing to do
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
       // nothing to do

    }

}

