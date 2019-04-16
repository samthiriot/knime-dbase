package ch.res_ear.samthiriot.knime.dbase.readDromDBase;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "DBaseReader" Node.
 * Reads data from a dBase database file or URL location. Upon executing the node will scan the input file to determine number and types of the columns and output a table with the auto-guessed structure.
 *
 * @author Samuel Thiriot
 */
public class DBaseReaderNodeFactory 
	extends NodeFactory<DBaseReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DBaseReaderNodeModel createNodeModel() {
        return new DBaseReaderNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<DBaseReaderNodeModel> createNodeView(final int viewIndex,
            final DBaseReaderNodeModel nodeModel) {
        return null; 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new DBaseReaderNodeDialog();
    }

}

