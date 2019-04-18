package ch.res_ear.samthiriot.knime.dbase.readDromDBase;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

public final class DBaseReaderConfig {

    private String m_location;

    /**
     * How many lines to read; -1 if no limit.
     */
    private int m_limitRowsCount;
    
    private boolean m_readDeletedRows;
    
    private boolean m_trimStrings;

    public DBaseReaderConfig() {
    	m_location = null;
    	m_limitRowsCount = -1;
    	m_readDeletedRows = false;
    	m_trimStrings = true;

    }
    
    /** Load settings, used in dialog (no errors).
     * @param settings To load from.
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings) {
        m_location = settings.getString("url", null);
        m_limitRowsCount = settings.getInt("limitRowsCount", m_limitRowsCount);
        m_readDeletedRows = settings.getBoolean("readDeletedRows", m_readDeletedRows);
        m_trimStrings = settings.getBoolean("trimStrings", m_trimStrings);
    }

    /** Load in model, fail if settings are invalid.
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    public void loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_location = settings.getString("url");
        m_limitRowsCount = settings.getInt("limitRowsCount", m_limitRowsCount);
        m_readDeletedRows = settings.getBoolean("readDeletedRows", m_readDeletedRows);
        m_trimStrings = settings.getBoolean("trimStrings", m_trimStrings);

    }

    /** Save configuration to argument.
     * @param settings To save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_location != null) {
            settings.addString("url", m_location.toString());
        }
        settings.addInt("limitRowsCount", m_limitRowsCount);
        settings.addBoolean("readDeletedRows", m_readDeletedRows);
        settings.addBoolean("trimStrings", m_trimStrings);
    }

    public String getLocation() {
        return m_location;
    }

    public void setLocation(final String location) {
        m_location = location;
    }

    public int getLimitRowsCount() {
        return m_limitRowsCount;
    }

    public void setLimitRowsCount(final int value) {
        m_limitRowsCount = value;
    }

    public boolean getReadDeletedRows() {
    	return m_readDeletedRows;
    }
    
    public void setReadDeletedRows(final boolean readDeletedRows) {
    	m_readDeletedRows = readDeletedRows;
    }

	public boolean getTrimStrings() {
		return m_trimStrings;
	}

	public void setTrimStrings(boolean m_trimStrings) {
		this.m_trimStrings = m_trimStrings;
	}
    
    
    
}
