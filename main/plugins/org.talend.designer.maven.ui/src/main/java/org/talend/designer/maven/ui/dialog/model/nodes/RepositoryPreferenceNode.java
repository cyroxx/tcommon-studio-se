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
package org.talend.designer.maven.ui.dialog.model.nodes;

import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.core.runtime.projectsetting.AbstractProjectSettingPage;
import org.talend.core.runtime.projectsetting.EmptyProjectSettingPage;
import org.talend.core.ui.images.RepositoryImageProvider;
import org.talend.repository.model.RepositoryNode;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class RepositoryPreferenceNode extends PreferenceNode {

    private final RepositoryNode curNode;

    private String label;

    private ImageDescriptor imageDescriptor;

    public RepositoryPreferenceNode(String id, String label, ImageDescriptor imageDescriptor, RepositoryNode node) {
        super(id, label, imageDescriptor, null);
        this.label = label;
        this.imageDescriptor = imageDescriptor;
        this.curNode = node;

    }

    protected RepositoryNode getNode() {
        return curNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferenceNode#createPage()
     */
    @Override
    public void createPage() {
        PreferencePage page = createPreferencePage();
        if (imageDescriptor != null) {
            page.setImageDescriptor(imageDescriptor);
        } else {
            Image labelImage = getLabelImage();
            if (labelImage != null) {
                page.setImageDescriptor(ImageDescriptor.createFromImageData(labelImage.getImageData()));
            }
        }
        if (page instanceof AbstractProjectSettingPage) {
            ((AbstractProjectSettingPage) page).setPrefNodeId(this.getId());
        }

        page.setTitle(getLabelText());
        this.setPage(page);
    }

    protected PreferencePage createPreferencePage() {
        return new EmptyProjectSettingPage();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferenceNode#getLabelImage()
     */
    @Override
    public Image getLabelImage() {
        Image labelImage = super.getLabelImage();
        if (labelImage == null && this.curNode != null) {
            return ImageProvider.getImage(RepositoryImageProvider.getIcon(this.curNode.getContentType()));
        }

        return labelImage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferenceNode#getLabelText()
     */
    @Override
    public String getLabelText() {
        if (this.label != null) {
            return this.label;
        }
        return super.getLabelText();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getId();
    }

}
