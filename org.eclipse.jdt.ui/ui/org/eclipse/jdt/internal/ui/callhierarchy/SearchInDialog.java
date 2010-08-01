/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * Class to show the Search In dialog.
 * 
 * @since 3.7
 */
public class SearchInDialog extends TrayDialog {

	private Button[] fIncludeMasks;

	private Label fSearchInDialogText_note;

	private IDialogSettings fSettings;

	private int fIncludeMask;

	private boolean fIncludeMaskChanged= false;

	/**
	 * Section ID for the SearchInDialog class.
	 */
	private static final String DIALOG_SETTINGS_SECTION= "SearchInDialog"; //$NON-NLS-1$	

	private static final String SEARCH_IN_SOURCES= "SearchInSources"; //$NON-NLS-1$

	private static final String SEARCH_IN_PROJECTS= "SearchInProjects"; //$NON-NLS-1$

	private static final String SEARCH_IN_APPLIBS= "SearchInAppLibs"; //$NON-NLS-1$

	private static final String SEARCH_IN_JRE= "SearchInJRE"; //$NON-NLS-1$

	private String[] fKeys= new String[] { SEARCH_IN_SOURCES, SEARCH_IN_PROJECTS, SEARCH_IN_JRE, SEARCH_IN_APPLIBS };

	public SearchInDialog(Shell parentShell) {
		super(parentShell);
		fSettings= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_SECTION);
		if (fSettings == null) {
			fSettings= JavaPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS_SECTION);
			fSettings.put(SEARCH_IN_SOURCES, true);
			fSettings.put(SEARCH_IN_PROJECTS, true);
			fSettings.put(SEARCH_IN_JRE, false);
			fSettings.put(SEARCH_IN_APPLIBS, true);
		}
		fIncludeMask= getInt(fKeys);
	}

	/**
	 * Returns the integer value of the strings.
	 * 
	 * @param str the array of strings
	 * @return the integer value of the strings
	 */
	private int getInt(String[] str) {
		boolean value;
		int mask= 0;
		int val= 0;
		for (int i= 0; i < str.length; i++) {
			value= fSettings.getBoolean(str[i]);
			if (value) {
				switch (i) {
					case 0:
						val= JavaSearchScopeFactory.SOURCES;
						break;
					case 1:
						val= JavaSearchScopeFactory.PROJECTS;
						break;
					case 2:
						val= JavaSearchScopeFactory.JRE;
						break;
					case 3:
						val= JavaSearchScopeFactory.LIBS;
				}
				mask|= val;
			}
		}
		return mask;
	}

	/* (non-Javadoc)
	 * Method declared on Window.
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(CallHierarchyMessages.SearchInDialog_title);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.CALL_HIERARCHY_SEARCH_IN_DIALOG);
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		super.createDialogArea(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);

		Control includeMask= createIncludeMask(composite);
		includeMask.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 2));

		return composite;
	}

	/**
	 * Creates the search in options.
	 * 
	 * @param parent the parent composite
	 * @return the group control
	 */
	private Control createIncludeMask(Composite parent) {
		Group result= new Group(parent, SWT.NONE);
		result.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 2));
		result.setText(SearchMessages.SearchPage_searchIn_label);
		result.setLayout(new GridLayout(4, false));
		fIncludeMasks= new Button[] {
				createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_sources, JavaSearchScopeFactory.SOURCES, fSettings.getBoolean(SEARCH_IN_SOURCES)),
				createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_projects, JavaSearchScopeFactory.PROJECTS, fSettings.getBoolean(SEARCH_IN_PROJECTS)),
				createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_jre, JavaSearchScopeFactory.JRE, fSettings.getBoolean(SEARCH_IN_JRE)),
				createButton(result, SWT.CHECK, SearchMessages.SearchPage_searchIn_libraries, JavaSearchScopeFactory.LIBS, fSettings.getBoolean(SEARCH_IN_APPLIBS)),
		};

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.numColumns= 4;
		composite.setLayout(layout);
		fSearchInDialogText_note= new Label(composite, SWT.LEFT);
		fSearchInDialogText_note.setFont(composite.getFont());
		fSearchInDialogText_note.setText(CallHierarchyMessages.SearchInDialog_note);

		SelectionAdapter listener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateOKStatus();
			}
		};
		for (int i= 0; i < fIncludeMasks.length; i++) {
			fIncludeMasks[i].addSelectionListener(listener);
		}

		return result;
	}

	/**
	 * Updates the enablement of OK button.
	 */
	protected void updateOKStatus() {
		boolean isValidMask= getIncludeMask() != 0;
		getButton(OK).setEnabled(isValidMask);		
	}

	/**
	 * Returns the include mask.
	 * 
	 * @return the include mask
	 */
	int getIncludeMask() {
		if (fIncludeMasks == null || fIncludeMasks[0].isDisposed())
			return fIncludeMask;
		int mask= 0;
		for (int i= 0; i < fIncludeMasks.length; i++) {
			Button button= fIncludeMasks[i];
			if (button.getSelection()) {
				mask|= getIntData(button);
			}
		}
		return mask;
	}

	/**
	 * Returns the value of the given button.
	 * 
	 * @param button the button for which to fetch value
	 * @return the value of the button
	 */
	private int getIntData(Button button) {
		return ((Integer)button.getData()).intValue();
	}

	/**
	 * Creates and returns the button.
	 * 
	 * @param parent the parent composite
	 * @param style the style of control to construct
	 * @param text the text for the button
	 * @param data the widget data
	 * @param isSelected the new selection state
	 * @return the button created
	 */
	private Button createButton(Composite parent, int style, String text, int data, boolean isSelected) {
		Button button= new Button(parent, style);
		button.setText(text);
		button.setData(new Integer(data));
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button.setSelection(isSelected);
		return button;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		int mask= getIncludeMask();
		if (mask != fIncludeMask) {
			fIncludeMask= mask;
			for (int i= 0; i < fIncludeMasks.length; i++) {
				fSettings.put(fKeys[i], fIncludeMasks[i].getSelection());
			}
			fIncludeMaskChanged= true;
		} else {
			fIncludeMaskChanged= false;
		}
		super.okPressed();
	}

	/**
	 * Indicates whether the include mask has changed.
	 * 
	 * @return the includeMaskChanged <code>true</code> if the include mask has changed,
	 *         <code>false</code> otherwise
	 */
	public boolean isIncludeMaskChanged() {
		return fIncludeMaskChanged;
	}

}