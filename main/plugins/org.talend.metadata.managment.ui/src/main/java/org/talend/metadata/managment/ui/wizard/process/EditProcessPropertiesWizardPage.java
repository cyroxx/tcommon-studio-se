// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.metadata.managment.ui.wizard.process;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.utils.ConvertJobsUtil;
import org.talend.core.repository.utils.ConvertJobsUtil.JobType;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.metadata.managment.ui.i18n.Messages;
import org.talend.metadata.managment.ui.wizard.PropertiesWizard;
import org.talend.metadata.managment.ui.wizard.PropertiesWizardPage;
import org.talend.repository.model.IProxyRepositoryService;

/**
 * Created by Marvin Wang on Feb 18, 2013.
 */
public class EditProcessPropertiesWizardPage extends PropertiesWizardPage {

    private Button convertBtn;

    protected CCombo jobTypeCCombo;

    protected CCombo framework;

    /**
     * DOC marvin EditProcessPropertiesWizardPage constructor comment.
     * 
     * @param pageName
     * @param property
     * @param destinationPath
     */
    public EditProcessPropertiesWizardPage(String pageName, Property property, IPath destinationPath) {
        super(pageName, property, destinationPath);
        // TODO Auto-generated constructor stub
    }

    /**
     * DOC marvin EditProcessPropertiesWizardPage constructor comment.
     * 
     * @param pageName
     * @param property
     * @param destinationPath
     * @param readOnly
     * @param editPath
     */
    public EditProcessPropertiesWizardPage(String pageName, Property property, IPath destinationPath, boolean readOnly,
            boolean editPath) {
        super(pageName, property, destinationPath, readOnly, editPath);
        // TODO Auto-generated constructor stub
    }

    /**
     * DOC marvin EditProcessPropertiesWizardPage constructor comment.
     * 
     * @param pageName
     * @param property
     * @param destinationPath
     * @param readOnly
     * @param editPath
     * @param lastVersionFound
     */
    public EditProcessPropertiesWizardPage(String pageName, Property property, IPath destinationPath, boolean readOnly,
            boolean editPath, String lastVersionFound) {
        super(pageName, property, destinationPath, readOnly, editPath, lastVersionFound);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        boolean alreadyEditedByUser = ((EditProcessPropertiesWizard) this.getWizard()).isAlreadyEditedByUser();
        if (alreadyEditedByUser) {
            Label label = new Label(container, SWT.NONE);
            label.setForeground(ColorConstants.red);
            label.setText(Messages.getString("PropertiesWizard.alreadyLockedByUser")); //$NON-NLS-1$
            GridData gridData = new GridData();
            gridData.horizontalSpan = 2;
            label.setLayoutData(gridData);
        }

        GridData data;

        // Name
        Label nameLab = new Label(container, SWT.NONE);
        nameLab.setText(Messages.getString("PropertiesWizardPage.Name")); //$NON-NLS-1$

        nameText = new Text(container, SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameText.setEditable(!readOnly);

        // Job type
        Label jobTypeLabel = new Label(container, SWT.NONE);
        jobTypeLabel.setText(Messages.getString("PropertiesWizardPage.jobTypeLabel")); //$NON-NLS-1$

        Composite typeGroup = new Composite(container, SWT.NONE);
        typeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout typeGroupLayout = new GridLayout(3, false);
        typeGroupLayout.marginHeight = 0;
        typeGroupLayout.marginWidth = 0;
        typeGroupLayout.horizontalSpacing = 0;
        typeGroup.setLayout(typeGroupLayout);

        jobTypeCCombo = new CCombo(typeGroup, SWT.BORDER);
        jobTypeCCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        jobTypeCCombo.setEditable(false);
        jobTypeCCombo.setItems(JobType.getJobTypeToDispaly());
        jobTypeCCombo.setText(JobType.STANDARD.getDisplayName());

        // Framework
        Label label = new Label(typeGroup, SWT.NONE);
        label.setText("  " + Messages.getString("PropertiesWizardPage.framework") + "  "); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

        framework = new CCombo(typeGroup, SWT.BORDER);
        framework.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        framework.setEditable(false);
        framework.setItems(new String[0]);

        // Purpose
        Label purposeLab = new Label(container, SWT.NONE);
        purposeLab.setText(Messages.getString("PropertiesWizardPage.Purpose")); //$NON-NLS-1$

        purposeText = new Text(container, SWT.BORDER);
        purposeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        purposeText.setEditable(!readOnly);

        // Description
        Label descriptionLab = new Label(container, SWT.NONE);
        descriptionLab.setText(Messages.getString("PropertiesWizardPage.Description")); //$NON-NLS-1$
        descriptionLab.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        descriptionText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.heightHint = 60;
        descriptionText.setLayoutData(data);
        descriptionText.setEditable(!readOnly);

        // Author
        Label authorLab = new Label(container, SWT.NONE);
        authorLab.setText(Messages.getString("PropertiesWizardPage.Author")); //$NON-NLS-1$

        authorText = new Text(container, SWT.BORDER);
        authorText.setEnabled(false);
        authorText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Locker
        Label lockerLab = new Label(container, SWT.NONE);
        lockerLab.setText(Messages.getString("PropertiesWizardPage.Locker")); //$NON-NLS-1$

        lockerText = new Text(container, SWT.BORDER);
        lockerText.setEnabled(false);
        lockerText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Version
        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        allowVerchange = brandingService.getBrandingConfiguration().isAllowChengeVersion();
        if (allowVerchange) {
            Label versionLab = new Label(container, SWT.NONE);
            versionLab.setText(Messages.getString("PropertiesWizardPage.Version")); //$NON-NLS-1$

            Composite versionContainer = new Composite(container, SWT.NONE);
            versionContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            GridLayout versionLayout = new GridLayout(3, false);
            versionLayout.marginHeight = 0;
            versionLayout.marginWidth = 0;
            versionLayout.horizontalSpacing = 0;
            versionContainer.setLayout(versionLayout);

            versionText = new Text(versionContainer, SWT.BORDER);
            versionText.setEnabled(false);
            versionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            versionMajorBtn = new Button(versionContainer, SWT.PUSH);
            versionMajorBtn.setText(Messages.getString("PropertiesWizardPage.Version.Major")); //$NON-NLS-1$
            versionMajorBtn.setEnabled(!readOnly && allowVerchange);

            versionMinorBtn = new Button(versionContainer, SWT.PUSH);
            versionMinorBtn.setText(Messages.getString("PropertiesWizardPage.Version.Minor")); //$NON-NLS-1$
            versionMinorBtn.setEnabled(!readOnly && allowVerchange);
        }

        // Status
        Label statusLab = new Label(container, SWT.NONE);
        statusLab.setText(Messages.getString("PropertiesWizardPage.Status")); //$NON-NLS-1$

        statusText = new CCombo(container, SWT.BORDER);
        statusText.setEditable(false);
        List<org.talend.core.model.properties.Status> statusList;
        try {
            if (property != null) {
                statusList = statusHelper.getStatusList(property);
                statusText.setItems(toArray(statusList));
                statusText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                // statusText.setEditable(!readOnly);
                statusText.setEnabled(!readOnly);
            }
        } catch (PersistenceException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            ExceptionHandler.process(e);
        }

        // Path:
        Label pathLab = new Label(container, SWT.NONE);
        pathLab.setText(Messages.getString("PropertiesWizardPage.Path")); //$NON-NLS-1$

        Composite pathContainer = new Composite(container, SWT.NONE);
        pathContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout pathLayout = new GridLayout(2, false);
        pathLayout.marginHeight = 0;
        pathLayout.marginWidth = 0;
        pathLayout.horizontalSpacing = 0;
        pathContainer.setLayout(pathLayout);

        pathText = new Text(pathContainer, SWT.BORDER);
        pathText.setEnabled(false);
        pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (editPath) {
            Button button = new Button(pathContainer, SWT.PUSH);
            button.setText(Messages.getString("PropertiesWizardPage.Select")); //$NON-NLS-1$

            button.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    openFolderSelectionDialog(NEED_CANCEL_BUTTON);
                }
            });

            if (destinationPath == null) {
                openFolderSelectionDialog(!NEED_CANCEL_BUTTON);
            }

        }

        // Added by Marvin Wang on Jan. 29, 2013.
        // createBottomPart(container);

        setControl(container);
        updateContent();
        addListeners();
        setPageComplete(false);
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        jobTypeCCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                ConvertJobsUtil.updateJobFrameworkPart(jobTypeCCombo.getText(), framework);
                updatePageStatus();
            }
        });

        framework.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                updatePageStatus();
            }
        });
    }

    @Override
    protected void createBottomPart(Composite parent) {
        // Added by Marvin Wang on Feb. 18 , 2013 for common process to add a convert button which can convert the
        // common process to M/R process.
        convertBtn = new Button(parent, SWT.PUSH);
        convertBtn.setText(Messages.getString("EditProcessPropertiesWizardPage.convert.button.name")); //$NON-NLS-1$
        GridDataFactory.swtDefaults().span(2, 1).align(SWT.CENTER, SWT.CENTER).grab(false, false).applyTo(convertBtn);
        convertBtn.setEnabled(!isReadOnly());
        convertBtn.setVisible(PluginChecker.isMapReducePluginLoader());
        convertBtn.setVisible(false);
    }

    @Override
    protected void regListeners() {
        // regConvertBtnListener();
    }

    /**
     * Registers a listener for convert button. Added by Marvin Wang on Jan 29, 2013.
     */
    protected void regConvertBtnListener() {
        convertBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                PropertiesWizard propWizard = ((PropertiesWizard) getWizard());
                try {

                    doConvert();
                    propWizard.setUnlockRequired(!converter.isOriginalItemDeleted());
                    propWizard.getContainer().getShell().close();
                } catch (PersistenceException e1) {
                    propWizard.setUnlockRequired(!converter.isOriginalItemDeleted());
                    propWizard.getContainer().getShell().close();
                    e1.printStackTrace();
                    e1.printStackTrace();
                } catch (BusinessException e2) {
                    propWizard.setUnlockRequired(!converter.isOriginalItemDeleted());
                    propWizard.getContainer().getShell().close();
                    e2.printStackTrace();
                }
            }
        });
    }

    /**
     * Converts m/r process to common process. Added by Marvin Wang on Jan 29, 2013.
     */
    protected void doConvert() throws PersistenceException, BusinessException {
        IWizard wizard = this.getWizard();
        if (wizard instanceof EditProcessPropertiesWizard) {
            EditProcessPropertiesWizard mrPropWizard = (EditProcessPropertiesWizard) wizard;
            IRepositoryViewObject repViewObject = mrPropWizard.getRepositoryViewObject();
            converter.convertFromProcess(getItem(), repViewObject);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.wizards.PropertiesWizardPage#getRepositoryObjectType()
     */
    @Override
    public ERepositoryObjectType getRepositoryObjectType() {
        EditProcessPropertiesWizard wizard = (EditProcessPropertiesWizard) getWizard();
        return wizard.getObject().getRepositoryObjectType();
    }

    @Override
    protected List<IRepositoryViewObject> loadRepViewObjectWithOtherTypes() throws PersistenceException {
        List<IRepositoryViewObject> list = new ArrayList<IRepositoryViewObject>();

        // List for all other processes
        List<IRepositoryViewObject> processList = getAllProcessTypeObjectsWithoutCurrentType();
        if (processList != null && !processList.isEmpty()) {
            list.addAll(processList);
        }

        // List for routine
        if (ERepositoryObjectType.ROUTINES != null) {
            if (GlobalServiceRegister.getDefault().isServiceRegistered(IProxyRepositoryService.class)) {
                IProxyRepositoryService service = (IProxyRepositoryService) GlobalServiceRegister.getDefault().getService(
                        IProxyRepositoryService.class);

                List<IRepositoryViewObject> mrList = service.getProxyRepositoryFactory().getAll(ERepositoryObjectType.ROUTINES,
                        true, false);
                if (mrList != null && mrList.size() > 0) {
                    list.addAll(mrList);
                }
            }
        }

        // List for esb route
        ERepositoryObjectType routeType = ERepositoryObjectType.valueOf(ERepositoryObjectType.class, "ROUTES");
        if (routeType != null) {
            if (GlobalServiceRegister.getDefault().isServiceRegistered(IProxyRepositoryService.class)) {
                IProxyRepositoryService service = (IProxyRepositoryService) GlobalServiceRegister.getDefault().getService(
                        IProxyRepositoryService.class);
                List<IRepositoryViewObject> routeList = service.getProxyRepositoryFactory().getAll(routeType, true, false);
                if (routeList != null && routeList.size() > 0) {
                    list.addAll(routeList);
                }
            }
        }
        return list;
    }

}
