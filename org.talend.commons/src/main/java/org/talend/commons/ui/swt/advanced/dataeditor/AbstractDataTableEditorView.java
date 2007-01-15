// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.commons.ui.swt.advanced.dataeditor;

import java.util.List;

import javax.swing.plaf.FontUIResource;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.talend.commons.ui.swt.advanced.dataeditor.commands.ExtendedTableRemoveCommand;
import org.talend.commons.ui.swt.extended.table.AbstractExtendedTableViewer;
import org.talend.commons.ui.swt.extended.table.ExtendedTableModel;
import org.talend.commons.ui.swt.tableviewer.TableViewerCreator;
import org.talend.commons.ui.swt.tableviewer.TableViewerCreatorColumn;
import org.talend.commons.utils.data.list.ListenableListEvent;

/**
 * This class is an abstraction of a group formed by a Label in top, a Table and a Toolbar at bottom. <br/>
 * 
 * $Id$
 * 
 * @param <B> Type of beans
 */
public abstract class AbstractDataTableEditorView<B> {

    private boolean readOnly;

    private Label titleLabel;

    private String title;

    private Composite mainComposite;

    private int mainCompositeStyle;

    private Composite parentComposite;

    private AbstractExtendedTableViewer<B> extendedTableViewer;

    private ExtendedTableModel<B> extendedTableModel;

    private AbstractExtendedToolbar abstractExtendedToolbar;

    private boolean toolbarVisible = true;

    private boolean labelVisible = true;

    private KeyListener tableKeyListener;

    // private IExtendedModelListener modelNameListener = new IExtendedModelListener() {
    //
    // public void handleEvent(ExtendedModelEvent event) {
    // if (AbstractExtendedControlModel.NAME_CHANGED.equals(event.getType())) {
    // titleLabel.setText(extendedTableModel.getName());
    // }
    // }
    //
    // };
    //
    /**
     * 
     * This constructor init graphics components, then load model.
     * 
     * @param parentComposite
     * @param mainCompositeStyle
     * @param extendedTableModel
     * @param labelVisible TODO
     */
    public AbstractDataTableEditorView(Composite parentComposite, int mainCompositeStyle, ExtendedTableModel<B> extendedTableModel,
            boolean readOnly, boolean toolbarVisible, boolean labelVisible) {
        super();
        this.parentComposite = parentComposite;
        this.mainCompositeStyle = mainCompositeStyle;
        this.extendedTableModel = extendedTableModel;
        this.readOnly = readOnly;
        this.toolbarVisible = toolbarVisible;
        this.labelVisible = labelVisible;
        initGraphicComponents();
        setExtendedTableModel(extendedTableModel);
    }

    /**
     * 
     * This constructor init graphics components, then load model.
     * 
     * Table data will be writable and toolbar will be visible.
     * 
     * @param parentComposite
     * @param mainCompositeStyle
     * @param extendedTableModel
     */
    public AbstractDataTableEditorView(Composite parentComposite, int mainCompositeStyle, ExtendedTableModel<B> extendedTableModel) {
        this(parentComposite, mainCompositeStyle, extendedTableModel, false, true, true);
    }

    /**
     * This constructor doesn't initialize graphics components and model.
     * 
     * @param parentComposite
     * @param mainCompositeStyle
     */
    public AbstractDataTableEditorView(Composite parentComposite, int mainCompositeStyle) {
        this(parentComposite, mainCompositeStyle, true);
    }

    /**
     * This constructor doesn't initialize graphics components and model.
     * 
     * @param parentComposite
     * @param mainCompositeStyle
     */
    public AbstractDataTableEditorView(Composite parentComposite, int mainCompositeStyle, boolean initGraphicsComponents) {
        this.parentComposite = parentComposite;
        this.mainCompositeStyle = mainCompositeStyle;
        if (initGraphicsComponents) {
            initGraphicComponents();
        }
    }

    /**
     * DOC amaumont AbstractDataTableEditorView constructor comment.
     */
    protected AbstractDataTableEditorView() {
        super();
    }

    public void initGraphicComponents() {

        mainComposite = new Composite(parentComposite, mainCompositeStyle);
        if (parentComposite.getBackground() != null && !parentComposite.getBackground().equals(mainComposite.getBackground())) {
            mainComposite.setBackground(parentComposite.getBackground());
        }
        GridLayout layout = new GridLayout();
        mainComposite.setLayout(layout);

        if (this.labelVisible) {
            titleLabel = new Label(mainComposite, SWT.NONE);
            titleLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (parentComposite.getBackground() != null && !parentComposite.getBackground().equals(titleLabel.getBackground())) {
                titleLabel.setBackground(parentComposite.getBackground());
            }
            titleLabel.setVisible(false);
        }

        initTable();

        this.extendedTableViewer.getTableViewerCreator().getTableViewer().setComparer(createElementComparer());

        this.extendedTableViewer.getTableViewerCreator().getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

        if (toolbarVisible) {
            this.abstractExtendedToolbar = initToolBar();
        }

        addListeners();
    }

    /**
     * DOC amaumont Comment method "createElementComparer".
     */
    protected IElementComparer createElementComparer() {
        return new IElementComparer() {

            public boolean equals(Object a, Object b) {
                return a == b;
            }

            public int hashCode(Object element) {
                return 0;
            }

        };
    }

    /**
     * DOC amaumont Comment method "initToolBar".
     */
    protected AbstractExtendedToolbar initToolBar() {
        return null;
    }

    /**
     * DOC amaumont Comment method "initTable".
     */
    protected void initTable() {
        this.extendedTableViewer = new AbstractExtendedTableViewer<B>(this.extendedTableModel, mainComposite) {

            @Override
            protected void createColumns(TableViewerCreator<B> tableViewerCreator, Table table) {
                AbstractDataTableEditorView.this.createColumns(tableViewerCreator, table);
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.talend.commons.ui.swt.extended.macrotable.AbstractExtendedTableViewer#setTableViewerCreatorOptions(org.talend.commons.ui.swt.tableviewer.TableViewerCreator)
             */
            @Override
            protected void setTableViewerCreatorOptions(TableViewerCreator<B> newTableViewerCreator) {
                super.setTableViewerCreatorOptions(newTableViewerCreator);
                AbstractDataTableEditorView.this.setTableViewerCreatorOptions(newTableViewerCreator);
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.talend.commons.ui.swt.extended.macrotable.AbstractExtendedTableViewer#handleBeforeListenableListOperationEvent(org.talend.commons.utils.data.list.ListenableListEvent)
             */
            @Override
            protected void handleBeforeListenableListOperationEvent(ListenableListEvent<B> event) {
                AbstractDataTableEditorView.this.handleBeforeListenableListOperationEvent(event);
                super.handleBeforeListenableListOperationEvent(event);
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.talend.commons.ui.swt.extended.macrotable.AbstractExtendedTableViewer#handleAfterListenableListOperationEvent(org.talend.commons.utils.data.list.ListenableListEvent)
             */
            @Override
            protected void handleAfterListenableListOperationEvent(ListenableListEvent<B> event) {
                super.handleAfterListenableListOperationEvent(event);
                AbstractDataTableEditorView.this.handleAfterListenableListOperationEvent(event);
            }

        };
    }

    /**
     * DOC amaumont Comment method "handleBeforeListenableListOperationEvent".
     * 
     * @param event
     */
    protected void handleBeforeListenableListOperationEvent(ListenableListEvent<B> event) {

    }

    /**
     * DOC amaumont Comment method "handleAfterListenableListOperationEvent".
     * 
     * @param event
     */
    protected void handleAfterListenableListOperationEvent(ListenableListEvent<B> event) {

    }

    /**
     * DOC amaumont Comment method "setTableViewerCreatorOptions".
     * 
     * @param newTableViewerCreator
     */
    protected void setTableViewerCreatorOptions(TableViewerCreator<B> newTableViewerCreator) {
        // newTableViewerCreator.setUseCustomItemColoring(true);
        // newTableViewerCreator.setFirstVisibleColumnIsSelection(true);
    }

    /**
     * DOC amaumont Comment method "addListeners".
     */
    protected void addListeners() {

        final Table table = extendedTableViewer.getTable();
        if (table != null) {

            this.tableKeyListener = new KeyListener() {

                public void keyPressed(KeyEvent e) {

                }

                public void keyReleased(KeyEvent e) {
                    if (e.character == SWT.DEL) {
                        ExtendedTableModel model = extendedTableViewer.getExtendedTableModel();
                        if (model != null && model.isDataRegistered()) {
                            ExtendedTableRemoveCommand command = new ExtendedTableRemoveCommand(model, table.getSelectionIndices());
                            extendedTableViewer.executeCommand(command);
                        }
                    }
                }

            };
            table.addKeyListener(this.tableKeyListener);

        }
    }

    /**
     * DOC ocarbone Comment method "setGridDataSize".
     * 
     * @param minimumWidth
     * @param minimumHeight
     */
    public void setGridDataSize(final int minimumWidth, final int minimumHeight) {
        mainComposite.setSize(minimumWidth, minimumHeight);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.minimumWidth = minimumWidth;
        gridData.minimumHeight = minimumHeight;
        mainComposite.setLayoutData(gridData);

    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (abstractExtendedToolbar != null) {
            abstractExtendedToolbar.setReadOnly(readOnly);
        }

        TableViewerCreator<B> tableViewerCreator = extendedTableViewer.getTableViewerCreator();

        List<TableViewerCreatorColumn> columns = tableViewerCreator.getColumns();
        for (TableViewerCreatorColumn column : columns) {
            column.setModifiable(!readOnly);
        }
    }

    /**
     * Getter for composite.
     * 
     * @return the composite
     */
    public Composite getMainComposite() {
        return this.mainComposite;
    }

    protected abstract void createColumns(TableViewerCreator<B> tableViewerCreator, Table table);

    /**
     * @return
     * @see org.talend.commons.ui.swt.extended.table.AbstractExtendedTableViewer#getExtendedTableModel()
     */
    public TableViewerCreator<B> getTableViewerCreator() {
        if (this.extendedTableViewer == null) {
            return null;
        }
        return this.extendedTableViewer.getTableViewerCreator();
    }

    public Table getTable() {
        TableViewerCreator<B> tableViewerCreator = getTableViewerCreator();
        if (tableViewerCreator == null) {
            return null;
        }
        return tableViewerCreator.getTable();
    }

    /**
     * Getter for extendedTableModel.
     * 
     * @return the extendedTableModel
     */
    public ExtendedTableModel<B> getExtendedTableModel() {
        if (extendedTableModel != null) {
            return extendedTableModel;
        }
        if (extendedTableViewer != null && extendedTableViewer.getExtendedTableModel() != null) {
            return extendedTableViewer.getExtendedTableModel();
        }
        return null;
    }

    /**
     * Sets the extendedTableModel.
     * 
     * @param extendedTableModel the extendedTableModel to set
     */
    public void setExtendedTableModel(ExtendedTableModel<B> extendedTableModel) {
        extendedTableViewer.setExtendedControlModel(extendedTableModel);
        if (titleLabel != null) {
            if (extendedTableModel != null) {
                titleLabel.setText(extendedTableModel.getName() == null ? "" : extendedTableModel.getName());
            } else {
                titleLabel.setText("");
            }
        }
    }

    /**
     * Getter for extendedTableViewer.
     * 
     * @return the extendedTableViewer
     */
    public AbstractExtendedTableViewer<B> getExtendedTableViewer() {
        return this.extendedTableViewer;
    }

    /**
     * Getter for abstractExtendedToolbar.
     * 
     * @return the abstractExtendedToolbar
     */
    public AbstractExtendedToolbar getExtendedToolbar() {
        return this.abstractExtendedToolbar;
    }

    /**
     * Getter for readOnly.
     * 
     * @return the readOnly
     */
    public boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * Getter for title.
     * 
     * @return the title
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Sets the title.
     * 
     * @param title the title to set
     */
    public void setTitle(String title) {
        if (title == null) {
            titleLabel.setVisible(false);
            titleLabel.setText("");
        } else {
            titleLabel.setVisible(true);
            titleLabel.setText(title);
        }
        this.title = title;
    }

}
