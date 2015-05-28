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
package org.talend.core.repository.ui.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.ui.swt.formtools.LabelledCombo;
import org.talend.commons.ui.swt.formtools.LabelledText;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.repository.utils.ConvertJobsUtil;
import org.talend.core.repository.utils.ConvertJobsUtil.JobBatchFramework;
import org.talend.core.repository.utils.ConvertJobsUtil.JobStreamingFramework;
import org.talend.core.repository.utils.ConvertJobsUtil.JobType;
import org.talend.repository.model.IRepositoryNode.EProperties;
import org.talend.repository.model.RepositoryNode;

/**
 * 
 * created by hcyi on May 25, 2015 Detailled comment
 *
 */
public class DuplicateDialog extends Dialog {

    private LabelledText nameLabel;

    private LabelledCombo jobTypeCombo;

    private LabelledCombo frameworkCombo;

    private String nameValue = null;

    private String jobTypeValue = null;

    private String frameworkValue = null;

    private RepositoryNode sourceNode;

    private String jobNameValue;

    private Composite typeGroup;

    public DuplicateDialog(Shell parentShell, RepositoryNode sourceNode, String jobNameValue) {
        super(parentShell);
        this.sourceNode = sourceNode;
        this.jobNameValue = jobNameValue;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Duplicate"); //$NON-NLS-1$
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        super.createDialogArea(parent);
        Composite nameGroup = new Composite(parent, SWT.NONE);
        GridLayout nameLayout = new GridLayout();
        nameLayout.numColumns = 2;
        nameGroup.setLayout(nameLayout);
        nameGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameLabel = new LabelledText(nameGroup, "Input new name:", 5); //$NON-NLS-1$

        typeGroup = new Composite(parent, SWT.NONE);
        GridLayout typeLayout = new GridLayout();
        typeLayout.numColumns = 6;
        typeGroup.setLayout(typeLayout);
        typeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        jobTypeCombo = new LabelledCombo(typeGroup, "Job Type:", "job type", JobType.getJobTypeToDispaly(), 2, true); //$NON-NLS-1$//$NON-NLS-2$
        frameworkCombo = new LabelledCombo(typeGroup, "Framework:", "framework", new String[0], 2, true); //$NON-NLS-1$//$NON-NLS-2$
        // add listener
        addListener();

        // init
        init();
        return parent;
    }

    private void init() {
        nameLabel.setText(jobNameValue);
        if (sourceNode != null) {
            Property property = sourceNode.getObject().getProperty();
            Item item = property.getItem();
            Object obj = ConvertJobsUtil.getFramework(item);
            if (obj != null) {
                frameworkValue = ConvertJobsUtil.getFramework(item).toString();
                frameworkCombo.setText(frameworkValue);
            }
            //
            if (sourceNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.PROCESS) {
                jobTypeValue = JobType.STANDARD.getDisplayName();
                jobTypeCombo.setText(jobTypeValue);
            } else if (sourceNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.PROCESS_MR) {
                jobTypeValue = JobType.BIGDATABATCH.getDisplayName();
                jobTypeCombo.setText(jobTypeValue);
            } else if (sourceNode.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.PROCESS_STORM) {
                jobTypeValue = JobType.BIGDATASTREAMING.getDisplayName();
                jobTypeCombo.setText(jobTypeValue);
            }
        }
    }

    private void addListener() {

        nameLabel.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                nameValue = nameLabel.getText();
                validateField();
            }
        });
        jobTypeCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                jobTypeValue = jobTypeCombo.getText();
                updateJobFrameworkPart();
                validateField();
                typeGroup.layout();
            }
        });
        frameworkCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                frameworkValue = frameworkCombo.getText();
                validateField();
            }
        });
    }

    private void validateField() {
        if (nameValue == null || "".equals(nameValue)) {
            getOKButton().setEnabled(false);
            return;
        } else if (getOKButton() != null) {
            getOKButton().setEnabled(true);
        }
    }

    public String getNameValue() {
        return this.nameValue;
    }

    public String getJobTypeValue() {
        return this.jobTypeValue;
    }

    public String getFrameworkValue() {
        return this.frameworkValue;
    }

    private void updateJobFrameworkPart() {
        if (JobType.STANDARD.getDisplayName().equals(jobTypeValue)) {
            frameworkCombo.getCombo().setItems(new String[0]);
            frameworkCombo.setReadOnly(true);
        } else if (JobType.BIGDATABATCH.getDisplayName().equals(jobTypeValue)) {
            String[] items = JobBatchFramework.getFrameworkToDispaly();
            frameworkCombo.getCombo().setItems(items);
            if (items.length > 0) {
                frameworkCombo.getCombo().select(0);
            }
            frameworkCombo.setReadOnly(false);
        } else if (JobType.BIGDATASTREAMING.getDisplayName().equals(jobTypeValue)) {
            String[] items = JobStreamingFramework.getFrameworkToDispaly();
            frameworkCombo.getCombo().setItems(items);
            if (items.length > 0) {
                frameworkCombo.getCombo().select(0);
            }
            frameworkCombo.setReadOnly(false);
        }
    }
}
