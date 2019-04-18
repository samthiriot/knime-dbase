package ch.res_ear.samthiriot.knime.dbase.readDromDBase;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable;

/**
 * <code>NodeDialog</code> for the "DBaseReader" Node.
 * Reads data from a dBase database file or URL location. Upon executing the node will scan the input file to determine number and types of the columns and output a table with the auto-guessed structure.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Samuel Thiriot
 */
public class DBaseReaderNodeDialog extends NodeDialogPane {

    private final FilesHistoryPanel m_filePanel;
    private final JCheckBox m_limitRowsChecker;
    private final JSpinner m_limitRowsSpinner;
    private final JCheckBox m_readDeletedRows;
    private final JCheckBox m_trimStrings;

    /**
     * New pane for configuring DBaseReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected DBaseReaderNodeDialog() {
        super();
        
        
        m_filePanel =
                new FilesHistoryPanel(createFlowVariableModel("url", FlowVariable.Type.STRING),
                    "dbf_read", LocationValidation.FileInput, ".dbf", ".dbase");
        m_filePanel.setDialogType(JFileChooser.OPEN_DIALOG);
        m_filePanel.setShowConnectTimeoutField(false); // TODO ?

        m_limitRowsChecker = new JCheckBox("Limit rows ");
        m_limitRowsSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, Integer.MAX_VALUE, 50));
        m_limitRowsChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                m_limitRowsSpinner.setEnabled(m_limitRowsChecker.isSelected());
            }
        });
        
        m_readDeletedRows = new JCheckBox("Read rows marked as deleted");
        
        m_trimStrings = new JCheckBox("Trim strings");
        
        //m_limitRowsChecker.doClick();

        addTab("Settings", initLayout());

        addTab("Limit Rows", getLimitRowsPanel());


    }
    
    /**
     * @param limitRowsTab
     * @return
     */
    private JPanel getLimitRowsPanel() {
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 0;
        optionsPanel.add(getInFlowLayout(m_limitRowsChecker), gbc);
        gbc.gridx += 1;
        optionsPanel.add(getInFlowLayout(m_limitRowsSpinner), gbc);
        gbc.gridy += 1;

        //empty panel to eat up extra space
        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weighty = 1;
        optionsPanel.add(new JPanel(), gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(5));
        panel.add(optionsPanel);

        return panel;
    }


    private static JPanel getInFlowLayout(final JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            p.add(c);
        }
        return p;
    }


    private JPanel initLayout() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Input location:"));
        filePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, m_filePanel.getPreferredSize().height));
        filePanel.add(m_filePanel);
        filePanel.add(Box.createHorizontalGlue());

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
            .createEtchedBorder(), "Reader options:"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        optionsPanel.add(m_readDeletedRows);
        optionsPanel.add(m_trimStrings);
                
        //empty panel to eat up extra space
        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weighty = 1;
        optionsPanel.add(new JPanel(), gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(filePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(optionsPanel);

        return panel;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        
    	DBaseReaderConfig config = new DBaseReaderConfig();
        
        config.loadSettingsInDialog(settings);
        m_filePanel.updateHistory();
        m_filePanel.setSelectedFile(config.getLocation());
        
        long limitRowsCount = config.getLimitRowsCount();
        if (limitRowsCount >= 0) { // 0 is allowed -- will only read header
            m_limitRowsChecker.setSelected(true);
            m_limitRowsSpinner.setValue(limitRowsCount);
        } else {
            m_limitRowsChecker.setSelected(false);
            m_limitRowsSpinner.setValue(10000);
        }
        
        m_readDeletedRows.setSelected(config.getReadDeletedRows());
        m_trimStrings.setSelected(config.getTrimStrings());
    }
    

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        
    	DBaseReaderConfig config = new DBaseReaderConfig();
        
    	String fileS = m_filePanel.getSelectedFile().trim();
        config.setLocation(fileS);
        
        int limitRows = (Integer)(m_limitRowsChecker.isSelected() ? m_limitRowsSpinner.getValue() : -1);
        config.setLimitRowsCount(limitRows);

        config.setReadDeletedRows(m_readDeletedRows.isSelected());
        config.setTrimStrings(m_trimStrings.isSelected());
        
        config.saveSettingsTo(settings);
        m_filePanel.addToHistory();
    }
    
}

