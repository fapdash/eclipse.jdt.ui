/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jesper S Møller - Bug 529432 - Allow JDT UI to target Java 10
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import static org.eclipse.jdt.core.JavaCore.DISABLED;
import static org.eclipse.jdt.core.JavaCore.DO_NOT_GENERATE;
import static org.eclipse.jdt.core.JavaCore.ENABLED;
import static org.eclipse.jdt.core.JavaCore.GENERATE;
import static org.eclipse.jdt.core.JavaCore.IGNORE;
import static org.eclipse.jdt.core.JavaCore.INFO;
import static org.eclipse.jdt.core.JavaCore.OPTIMIZE_OUT;
import static org.eclipse.jdt.core.JavaCore.PRESERVE;
import static org.eclipse.jdt.core.JavaCore.WARNING;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.service.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.BundleDefaultsScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.LibrariesWorkbookPage;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;

/**
 * Configuration block for the 'Java Compiler' page.
 */
public class ComplianceConfigurationBlock extends OptionsConfigurationBlock {

	/**
	 * Key for the "Compiler compliance follows EE" setting.
	 * <br>Only applicable if <code>fProject != null</code>.
	 * <p>Values are { {@link #DEFAULT_CONF}, {@link #USER_CONF}, or {@link #DISABLED} }.
	 */
	private static final Key INTR_COMPLIANCE_FOLLOWS_EE= getLocalKey("internal.compliance.follows.ee"); //$NON-NLS-1$

	/**
	 * Key for the "Use default compliance" setting.
	 * <p>Values are { {@link #DEFAULT_CONF}, {@link #USER_CONF} }.
	 */
	private static final Key INTR_DEFAULT_COMPLIANCE= getLocalKey("internal.default.compliance"); //$NON-NLS-1$

	// Preference store keys, see JavaCore.getOptions
	private static final Key PREF_SOURCE_COMPATIBILITY= getJDTCoreKey(JavaCore.COMPILER_SOURCE);
	private static final Key PREF_CODEGEN_TARGET_PLATFORM= getJDTCoreKey(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
	private static final Key PREF_COMPLIANCE= getJDTCoreKey(JavaCore.COMPILER_COMPLIANCE);
	private static final Key PREF_RELEASE= getJDTCoreKey(JavaCore.COMPILER_RELEASE);
	private static final Key PREF_ENABLE_PREVIEW= getJDTCoreKey(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES);
	private static final Key PREF_PB_REPORT_PREVIEW= getJDTCoreKey(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES);

	/* see also BuildPathSupport#PREFS_COMPLIANCE */
	private static final Key[] PREFS_COMPLIANCE= new Key[] { PREF_COMPLIANCE,
		PREF_SOURCE_COMPATIBILITY, PREF_CODEGEN_TARGET_PLATFORM};
	private static final Key[] PREFS_COMPLIANCE_11_OR_HIGHER= new Key[] { PREF_COMPLIANCE,
		PREF_SOURCE_COMPATIBILITY, PREF_CODEGEN_TARGET_PLATFORM,
		PREF_ENABLE_PREVIEW, PREF_PB_REPORT_PREVIEW};

	private static final Key PREF_LOCAL_VARIABLE_ATTR=  getJDTCoreKey(JavaCore.COMPILER_LOCAL_VARIABLE_ATTR);
	private static final Key PREF_LINE_NUMBER_ATTR= getJDTCoreKey(JavaCore.COMPILER_LINE_NUMBER_ATTR);
	private static final Key PREF_SOURCE_FILE_ATTR= getJDTCoreKey(JavaCore.COMPILER_SOURCE_FILE_ATTR);
	private static final Key PREF_CODEGEN_UNUSED_LOCAL= getJDTCoreKey(JavaCore.COMPILER_CODEGEN_UNUSED_LOCAL);
	private static final Key PREF_CODEGEN_METHOD_PARAMETERS_ATTR= getJDTCoreKey(JavaCore.COMPILER_CODEGEN_METHOD_PARAMETERS_ATTR);


	private static final String VERSION_LATEST = JavaCore.latestSupportedJavaVersion();

	private static final String DEFAULT_CONF= "default"; //$NON-NLS-1$
	private static final String USER_CONF= "user";	 //$NON-NLS-1$

	private ArrayList<Control> fComplianceFollowsEEControls;
	private ArrayList<Control> fComplianceControls;
	private ArrayList<Control> fComplianceChildControls;
	private PixelConverter fPixelConverter;

	/**
	 * Remembered user compliance (stored when {@link #INTR_DEFAULT_COMPLIANCE} is switched to
	 * {@link #DEFAULT_CONF}). Elements are identified by <code>IDX_*</code> constants.
	 *
	 * @see #IDX_SOURCE_COMPATIBILITY
	 * @see #IDX_CODEGEN_TARGET_PLATFORM
	 * @see #IDX_COMPLIANCE
	 * @see #IDX_METHOD_PARAMETERS_ATTR
	 * @see #IDX_RELEASE
	 * @see #IDX_ENABLE_PREVIEW
	 * @see #IDX_REPORT_PREVIEW
	 */
	private String[] fRememberedUserCompliance;

	/**
	 * Stored compliance settings that were active when the page was first shown. May be
	 * <code>null</code>. Elements are identified by <code>IDX_*</code> constants.
	 *
	 * @see #IDX_SOURCE_COMPATIBILITY
	 * @see #IDX_CODEGEN_TARGET_PLATFORM
	 * @see #IDX_COMPLIANCE
	 * @see #IDX_METHOD_PARAMETERS_ATTR
	 * @see #IDX_RELEASE
	 * @see #IDX_ENABLE_PREVIEW
	 * @see #IDX_REPORT_PREVIEW
	 */
	private String[] fOriginalStoredCompliance;

	private static final int IDX_SOURCE_COMPATIBILITY= 0;
	private static final int IDX_CODEGEN_TARGET_PLATFORM= 1;
	private static final int IDX_COMPLIANCE= 2;
	private static final int IDX_METHOD_PARAMETERS_ATTR= 3;
	private static final int IDX_RELEASE= 4;
	private static final int IDX_ENABLE_PREVIEW= 5;
	private static final int IDX_REPORT_PREVIEW= 6;

	private IStatus fComplianceStatus;

	private Link fJRE50InfoText;
	private Label fJRE50InfoImage;
	private Composite fControlsComposite;
	private ControlEnableState fBlockEnableState;
	private Button fCompilerReleaseCheck;
	private Button fEnablePreviewCheck;
	private Combo fReportPreviewCombo;

	public ComplianceConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(project != null), container);
		setDefaultCompilerComplianceValues();

		fBlockEnableState= null;
		fComplianceFollowsEEControls= new ArrayList<>();
		fComplianceControls= new ArrayList<>();
		fComplianceChildControls= new ArrayList<>();

		fComplianceStatus= new StatusInfo();

		fRememberedUserCompliance= new String[] { // caution: order depends on IDX_* constants
			getValue(PREF_SOURCE_COMPATIBILITY),
			getValue(PREF_CODEGEN_TARGET_PLATFORM),
			getValue(PREF_COMPLIANCE),
			getValue(PREF_CODEGEN_METHOD_PARAMETERS_ATTR),
			getValue(PREF_RELEASE),
			getValue(PREF_ENABLE_PREVIEW),
			getValue(PREF_PB_REPORT_PREVIEW)
		};
	}

	public static Key[] getKeys(boolean projectSpecific) {
		Key[] keys= new Key[] {
				PREF_LOCAL_VARIABLE_ATTR, PREF_LINE_NUMBER_ATTR, PREF_SOURCE_FILE_ATTR, PREF_CODEGEN_UNUSED_LOCAL, INTR_DEFAULT_COMPLIANCE,
				PREF_COMPLIANCE, PREF_SOURCE_COMPATIBILITY,
				PREF_CODEGEN_TARGET_PLATFORM, PREF_CODEGEN_METHOD_PARAMETERS_ATTR, PREF_RELEASE,
				PREF_ENABLE_PREVIEW, PREF_PB_REPORT_PREVIEW
			};

		if (projectSpecific) {
			Key[] allKeys = new Key[keys.length + 1];
			System.arraycopy(keys, 0, allKeys, 0, keys.length);
			allKeys[keys.length]= INTR_COMPLIANCE_FOLLOWS_EE;
			return allKeys;
		}

		return keys;
	}

	@Override
	protected void settingsUpdated() {
		setValue(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance());
		updateComplianceFollowsEE();
		super.settingsUpdated();
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());

		Composite complianceComposite= createComplianceTabContent(parent);

		validateSettings(null, null, null);

		return complianceComposite;
	}

	public void enablePreferenceContent(boolean enable) {
		if (fControlsComposite != null && !fControlsComposite.isDisposed()) {
			if (enable) {
				if (fBlockEnableState != null) {
					fBlockEnableState.restore();
					fBlockEnableState= null;
				}
			} else {
				if (fBlockEnableState == null) {
					fBlockEnableState= ControlEnableState.disable(fControlsComposite);
				}
			}
		}
	}

	private Composite createComplianceTabContent(Composite folder) {

		ArrayList<String> allJavaProjectVersions = new ArrayList<>(JavaCore.getAllJavaSourceVersionsSupportedByCompiler());
		Collections.reverse(allJavaProjectVersions);

		final String[] complianceVersions= allJavaProjectVersions.toArray(String[]::new);

		for (int i= 0; i < allJavaProjectVersions.size(); i++) {
			String version= allJavaProjectVersions.get(i);
			if (isBetaVersion(version)) {
				allJavaProjectVersions.set(i, version + " (BETA)"); //$NON-NLS-1$
				break;
			}
		}
		final String[] complianceLabels= allJavaProjectVersions.toArray(String[]::new); // 2nd copy in case (BETA) was added

		final String[] targetVersions= complianceVersions;
		final String[] targetLabels= complianceLabels;

		final String[] sourceVersions= complianceVersions;
		final String[] sourceLabels= complianceLabels;

		final ScrolledPageContent sc1 = new ScrolledPageContent(folder);
		Composite composite= sc1.getBody();
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);

		fControlsComposite= new Composite(composite, SWT.NONE);
		fControlsComposite.setFont(composite.getFont());
		fControlsComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;
		fControlsComposite.setLayout(layout);

		int nColumns= 3;

		layout= new GridLayout();
		layout.numColumns= nColumns;

		Group group= new Group(fControlsComposite, SWT.NONE);
		group.setFont(fControlsComposite.getFont());
		group.setText(PreferencesMessages.ComplianceConfigurationBlock_compliance_group_label);
		group.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		group.setLayout(layout);

		String[] defaultUserValues= new String[] { DEFAULT_CONF, USER_CONF };

		Control[] otherChildren= group.getChildren();
		if (fProject != null) {
			String label= PreferencesMessages.ComplianceConfigurationBlock_compliance_follows_EE_label;
			int widthHint= fPixelConverter.convertWidthInCharsToPixels(40);
			addCheckBoxWithLink(group, label, INTR_COMPLIANCE_FOLLOWS_EE, defaultUserValues, 0, widthHint, new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					openBuildPathPropertyPage();
				}
			});
		}

		Control[] allChildren= group.getChildren();
		fComplianceFollowsEEControls.addAll(Arrays.asList(allChildren));
		fComplianceFollowsEEControls.removeAll(Arrays.asList(otherChildren));
		otherChildren= allChildren;


		String label= PreferencesMessages.ComplianceConfigurationBlock_compiler_compliance_label;
		addComboBox(group, label, PREF_COMPLIANCE, complianceVersions, complianceLabels, 0);

		label= PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_backwardcompatibility_label;
		fCompilerReleaseCheck= addCheckBox(group, label, PREF_RELEASE, new String[] { DISABLED, ENABLED }, 0, false);

		label= PreferencesMessages.ComplianceConfigurationBlock_default_settings_label;
		addCheckBox(group, label, INTR_DEFAULT_COMPLIANCE, defaultUserValues, 0);

		allChildren= group.getChildren();
		fComplianceControls.addAll(Arrays.asList(allChildren));
		fComplianceControls.removeAll(Arrays.asList(otherChildren));
		otherChildren= allChildren;


		int indent= LayoutUtil.getIndent();

		String[] warningInfoIgnore= new String[] { WARNING, INFO, IGNORE };
		String[] warningInfoIgnoreLabels= new String[] {
				PreferencesMessages.ComplianceConfigurationBlock_warning,
				PreferencesMessages.ComplianceConfigurationBlock_info,
				PreferencesMessages.ComplianceConfigurationBlock_ignore
		};

		label= Messages.format(PreferencesMessages.ComplianceConfigurationBlock_enable_preview_label, new String[] { getVersionLabel(VERSION_LATEST) });
		fEnablePreviewCheck= addCheckBox(group, label, PREF_ENABLE_PREVIEW, new String[] { ENABLED, DISABLED }, indent);
		label= PreferencesMessages.ComplianceConfigurationBlock_enable_preview_severity_label;
		fReportPreviewCombo= addComboBox(group, label, PREF_PB_REPORT_PREVIEW, warningInfoIgnore, warningInfoIgnoreLabels, indent * 2);
		fReportPreviewCombo.setEnabled(fEnablePreviewCheck.isEnabled() && fEnablePreviewCheck.getSelection());

		label= PreferencesMessages.ComplianceConfigurationBlock_codegen_targetplatform_label;
		addComboBox(group, label, PREF_CODEGEN_TARGET_PLATFORM, targetVersions, targetLabels, indent);

		label= PreferencesMessages.ComplianceConfigurationBlock_source_compatibility_label;
		addComboBox(group, label, PREF_SOURCE_COMPATIBILITY, sourceVersions, sourceLabels, indent);

		allChildren= group.getChildren();
		fComplianceChildControls.addAll(Arrays.asList(allChildren));
		fComplianceChildControls.removeAll(Arrays.asList(otherChildren));


		layout= new GridLayout();
		layout.numColumns= nColumns;

		group= new Group(fControlsComposite, SWT.NONE);
		group.setFont(fControlsComposite.getFont());
		group.setText(PreferencesMessages.ComplianceConfigurationBlock_classfiles_group_label);
		group.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		group.setLayout(layout);

		String[] generateValues= new String[] { GENERATE, DO_NOT_GENERATE };

		label= PreferencesMessages.ComplianceConfigurationBlock_variable_attr_label;
		addCheckBox(group, label, PREF_LOCAL_VARIABLE_ATTR, generateValues, 0);

		label= PreferencesMessages.ComplianceConfigurationBlock_line_number_attr_label;
		addCheckBox(group, label, PREF_LINE_NUMBER_ATTR, generateValues, 0);

		label= PreferencesMessages.ComplianceConfigurationBlock_source_file_attr_label;
		addCheckBox(group, label, PREF_SOURCE_FILE_ATTR, generateValues, 0);

		label= PreferencesMessages.ComplianceConfigurationBlock_codegen_unused_local_label;
		addCheckBox(group, label, PREF_CODEGEN_UNUSED_LOCAL, new String[] { PRESERVE, OPTIMIZE_OUT }, 0);

		label= PreferencesMessages.ComplianceConfigurationBlock_codegen_method_parameters_attr;
		addCheckBox(group, label, PREF_CODEGEN_METHOD_PARAMETERS_ATTR, generateValues, 0);

		Composite infoComposite= new Composite(fControlsComposite, SWT.NONE);
		infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		infoComposite.setLayout(new GridLayout(2, false));

		fJRE50InfoImage= new Label(infoComposite, SWT.NONE);
		fJRE50InfoImage.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
		GridData gd= new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
		fJRE50InfoImage.setLayoutData(gd);

		fJRE50InfoText= new Link(infoComposite, SWT.WRAP);
		fJRE50InfoText.setFont(composite.getFont());
		// set a text: not the real one, just for layouting
		String versionLabel= getVersionLabel(JavaCore.getAllJavaSourceVersionsSupportedByCompiler().first());
		fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info_project, new String[] { versionLabel, versionLabel }));
		fJRE50InfoText.setVisible(false);
		fJRE50InfoText.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if ("1".equals(e.text)) { //$NON-NLS-1$
					openJREInstallPreferencePage(false);
				} else if ("2".equals(e.text)) { //$NON-NLS-1$
					openJREInstallPreferencePage(true);
				} else {
					openBuildPathPropertyPage();
				}
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}
		});
		gd= new GridData(GridData.FILL, GridData.FILL, true, true);
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(50);
		fJRE50InfoText.setLayoutData(gd);
		initializeReleaseCheckBox(false);
		updateReleaseOptionStatus();
		validateComplianceStatus();
		return sc1;
	}

	private void initializeReleaseCheckBox(boolean useProjectSpecificSettings) {
		if (fProject != null && !useProjectSpecificSettings) {
			String value= PREF_RELEASE.getStoredValue(new IScopeContext[] { new ProjectScope(fProject) }, false, null);
			if (value != null) {
				setValue(PREF_RELEASE, value);
				fCompilerReleaseCheck.setSelection(DISABLED.equals(value) ? false : true);
			} else {
				setValue(PREF_RELEASE, DISABLED);
				fCompilerReleaseCheck.setSelection(false);
			}
		} else {
			String value= PREF_RELEASE.getStoredValue(new IScopeContext[] { InstanceScope.INSTANCE }, false, null);
			if (value != null) {
				setValue(PREF_RELEASE, value);
				fCompilerReleaseCheck.setSelection(DISABLED.equals(value) ? false : true);
			} else {
				setValue(PREF_RELEASE, DISABLED);
				fCompilerReleaseCheck.setSelection(false);
			}
		}
	}

	protected final void openBuildPathPropertyPage() {
		if (getPreferenceContainer() != null) {
			Map<Object, IClasspathEntry> data= new HashMap<>();
			data.put(BuildPathsPropertyPage.DATA_REVEAL_ENTRY, JavaRuntime.getDefaultJREContainerEntry());
			getPreferenceContainer().openPage(BuildPathsPropertyPage.PROP_ID, data);
		}
		updateReleaseOptionStatus();
		validateComplianceStatus();
	}

	protected final void openJREInstallPreferencePage(boolean openEE) {
		String jreID= BuildPathSupport.JRE_PREF_PAGE_ID;
		String eeID= BuildPathSupport.EE_PREF_PAGE_ID;
		String pageId= openEE ? eeID : jreID;
		if (fProject == null && getPreferenceContainer() != null) {
			getPreferenceContainer().openPage(pageId, null);
		} else {
			PreferencesUtil.createPreferenceDialogOn(getShell(), pageId, new String[] { jreID, eeID }, null).open();
		}
		updateReleaseOptionStatus();
		validateComplianceStatus();
	}

	@Override
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		if (!areSettingsEnabled()) {
			return;
		}
		if (changedKey != null) {
			if (INTR_DEFAULT_COMPLIANCE.equals(changedKey)) {
				updateComplianceEnableState();
				updateComplianceDefaultSettings(true, null);
				fComplianceStatus= validateCompliance();
			} else if (INTR_COMPLIANCE_FOLLOWS_EE.equals(changedKey)) {
				setValue(INTR_DEFAULT_COMPLIANCE, DEFAULT_CONF);
				updateReleaseOptionStatus();
				updateComplianceEnableState();
				updateComplianceDefaultSettings(true, null);
				updateControls();
				fComplianceStatus= validateCompliance();
				validateComplianceStatus();
			} else if (PREF_COMPLIANCE.equals(changedKey)) {
				updateReleaseOptionStatus();
			    // set compliance settings to default
			    Object oldDefault= getValue(INTR_DEFAULT_COMPLIANCE);
				boolean rememberOld= USER_CONF.equals(oldDefault);
				updateComplianceDefaultSettings(rememberOld, oldValue);
				fComplianceStatus= validateCompliance();
				validateComplianceStatus();
			} else if (PREF_RELEASE.equals(changedKey)) {
				setValue(PREF_RELEASE, DISABLED.equals(newValue) ? ENABLED : DISABLED);
				updateReleaseOptionStatus();
				updateComplianceDefaultSettings(true, null);
				fComplianceStatus= validateCompliance();
				validateComplianceStatus();
				if (fComplianceStatus.isOK() && ENABLED.equals(getValue(PREF_RELEASE))) {
					String exportOptionValue= addsExportToSystemModule();
					if (exportOptionValue != null) {
						int slash = exportOptionValue.indexOf('/');
						if (slash > -1) {
							fComplianceStatus = new Status(IStatus.ERROR, JavaUI.ID_PLUGIN,
									MessageFormat.format(PreferencesMessages.ComplianceConfigurationBlock_release_notWith_addExports_error, exportOptionValue.substring(0, slash)));
						}
					}
				}
			} else if (PREF_SOURCE_COMPATIBILITY.equals(changedKey)) {
				updatePreviewFeaturesState();
				fComplianceStatus= validateCompliance();
			} else if (PREF_CODEGEN_TARGET_PLATFORM.equals(changedKey)) {
				updateControls();
				updateStoreMethodParamNamesEnableState();
				updatePreviewFeaturesState();
				fComplianceStatus= validateCompliance();
			} else if (PREF_ENABLE_PREVIEW.equals(changedKey)) {
				fComplianceStatus= validateCompliance();
				updatePreviewFeaturesState();
			} else if (PREF_PB_REPORT_PREVIEW.equals(changedKey)) {
				fComplianceStatus= validateCompliance();
			} else {
				return;
			}
		} else {
			updateComplianceFollowsEE();
			updateControls();
			updateComplianceEnableState();
			updatePreviewFeaturesState();
			updatePreviewControls();
			updateStoreMethodParamNamesEnableState();
			fComplianceStatus= validateCompliance();
			updateComplianceReleaseSettings();
			updateReleaseOptionStatus();
			validateComplianceStatus();
		}
		fContext.statusChanged(fComplianceStatus);
	}

	public void refreshComplianceSettings() {
		if (fOriginalStoredCompliance == null) {
			fOriginalStoredCompliance= new String[] { // caution: order depends on IDX_* constants
					getOriginalStoredValue(PREF_SOURCE_COMPATIBILITY),
					getOriginalStoredValue(PREF_CODEGEN_TARGET_PLATFORM),
					getOriginalStoredValue(PREF_COMPLIANCE),
					getOriginalStoredValue(PREF_CODEGEN_METHOD_PARAMETERS_ATTR),
					getOriginalStoredValue(PREF_RELEASE),
					getOriginalStoredValue(PREF_ENABLE_PREVIEW),
					getOriginalStoredValue(PREF_PB_REPORT_PREVIEW)
				};

		} else {
			String[] storedCompliance= new String[] {
					getOriginalStoredValue(PREF_SOURCE_COMPATIBILITY),
					getOriginalStoredValue(PREF_CODEGEN_TARGET_PLATFORM),
					getOriginalStoredValue(PREF_COMPLIANCE),
					getOriginalStoredValue(PREF_CODEGEN_METHOD_PARAMETERS_ATTR),
					getOriginalStoredValue(PREF_RELEASE),
					getOriginalStoredValue(PREF_ENABLE_PREVIEW),
					getOriginalStoredValue(PREF_PB_REPORT_PREVIEW)
				};
			if (!Arrays.equals(fOriginalStoredCompliance, storedCompliance)) {
				// compliance changed on disk -> override user modifications

				fOriginalStoredCompliance= storedCompliance;

				setValue(PREF_SOURCE_COMPATIBILITY, storedCompliance[IDX_SOURCE_COMPATIBILITY]);
				setValue(PREF_CODEGEN_TARGET_PLATFORM, storedCompliance[IDX_CODEGEN_TARGET_PLATFORM]);
				setValue(PREF_COMPLIANCE, storedCompliance[IDX_COMPLIANCE]);
				setValue(PREF_CODEGEN_METHOD_PARAMETERS_ATTR, storedCompliance[IDX_METHOD_PARAMETERS_ATTR]);
				setValue(PREF_RELEASE, storedCompliance[IDX_RELEASE]);
				setValue(PREF_ENABLE_PREVIEW, storedCompliance[IDX_ENABLE_PREVIEW]);
				setValue(PREF_PB_REPORT_PREVIEW, storedCompliance[IDX_REPORT_PREVIEW]);
			}

			updateComplianceFollowsEE();
			updateControls();
			updateComplianceEnableState();
			validateComplianceStatus();
			updatePreviewFeaturesState();
			updateStoreMethodParamNamesEnableState();
		}
	}

	@Override
	public void performDefaults() {
		super.performDefaults();
		performReleasePreferenceDefault();
	}

	private void performReleasePreferenceDefault() {
		String defValue= getDefaultValue(PREF_RELEASE);
		if (defValue == null) {
			defValue= DISABLED;
		}
		fCompilerReleaseCheck.setSelection(DISABLED.equals(defValue) ? false : true);
		setValue(PREF_RELEASE, defValue);
		updateReleaseOptionStatus();
	}

	private void validateComplianceStatus() {
		if (fJRE50InfoText != null && !fJRE50InfoText.isDisposed()) {
			boolean isVisible= false;
			Image image= JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
			String compliance= getStoredValue(PREF_COMPLIANCE); // get actual value
			IVMInstall install= null;
			if (fProject != null) { // project specific settings: only test if a 50 JRE is installed
				try {
					install= JavaRuntime.getVMInstall(JavaCore.create(fProject));
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			} else {
				install= JavaRuntime.getDefaultVMInstall();
			}
			boolean isJREUnsupportedAndGreater= false;
			if (install instanceof IVMInstall2) {
				String version= ((IVMInstall2) install).getJavaVersion();
				String compilerCompliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) install, compliance);
				isJREUnsupportedAndGreater= isJREVersionUnsupportedAndGreater(version, compilerCompliance);
				if (isJREUnsupportedAndGreater) {
					version= getJREVersionString(version);
				}
				if (!compilerCompliance.equals(compliance)) { // Discourage using compiler with version other than compliance
					String[] args= { getVersionLabel(compliance), getVersionLabel(compilerCompliance) };
					if (isJREUnsupportedAndGreater) {
						args[1]= getVersionLabel(version);
					}
					if (JavaModelUtil.is9OrHigher(compilerCompliance)) {
						if (!fCompilerReleaseCheck.getSelection()) {
							if (fProject == null) {
								fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info, args));
							} else {
								fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info_project, args));
							}
						} else {
							if (JavaModelUtil.is10OrHigher(compilerCompliance) && !JavaModelUtil.is12OrHigher(compilerCompliance)) {
								fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_backwardcompatibility_warning, args));
							}
							else {
								fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_backwardcompatibility_info, args));
								image= JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO);							}
						}
					} else {
						if (fProject == null) {
							fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info, args));
						} else {
							fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info_project, args));
						}
					}
					isVisible= true;
				} else {
					if (isJREUnsupportedAndGreater) {
						String[] args= { getVersionLabel(compilerCompliance), getVersionLabel(version) };
						if (fCompilerReleaseCheck.getSelection()) {
							fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_backwardcompatibility_info, args));
							image= JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_INFO);
						} else {
							if (fProject == null) {
								fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info, args));
							} else {
								fJRE50InfoText.setText(Messages.format(PreferencesMessages.ComplianceConfigurationBlock_jrecompliance_info_project, args));
							}
						}
						isVisible= true;
					}
				}
			}

			if (isBetaVersion(getValue(PREF_COMPLIANCE))) {
				fJRE50InfoText.setText(
						"This is an implementation of an early-draft specification developed under the Java Community Process (JCP) and is made available for testing and evaluation purposes only. The code is not compatible with any specification of the JCP."); //$NON-NLS-1$
				isVisible= true;
			}

			fJRE50InfoText.setVisible(isVisible);
			fJRE50InfoImage.setImage(isVisible ? image : null);
			fJRE50InfoImage.getParent().layout();
		}
	}

	protected boolean isBetaVersion(@SuppressWarnings("unused") String compliance) {
		return false;
		// return JavaCore.VERSION_24.equals(compliance);
	}

	private String addsExportToSystemModule() {
		if (fProject == null) {
			return null;
		}
		try {
			for (IClasspathEntry cpe : JavaCore.create(fProject).getRawClasspath()) {
				if (cpe.getEntryKind() == IClasspathEntry.CPE_CONTAINER && LibrariesWorkbookPage.isJREContainer(cpe.getPath())) {
					for (IClasspathAttribute attribute : cpe.getExtraAttributes()) {
						if (IClasspathAttribute.ADD_EXPORTS.equals(attribute.getName())) {
							return attribute.getValue();
						}
					}
					break;
				}
			}
		} catch (JavaModelException e) {
			return null;
		}
		return null;
	}

	private void updateReleaseOptionStatus() {
		if (fCompilerReleaseCheck != null && !fCompilerReleaseCheck.isDisposed()) {
			String compliance= getStoredValue(PREF_COMPLIANCE); // get actual value
			IVMInstall install= null;
			if (fProject != null) { // project specific settings: only test if a 50 JRE is installed
				try {
					install= JavaRuntime.getVMInstall(JavaCore.create(fProject));
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			} else {
				install= JavaRuntime.getDefaultVMInstall();
			}
			if (install instanceof IVMInstall2) {
				String compilerCompliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) install, compliance);
				String version= ((IVMInstall2) install).getJavaVersion();
				boolean isJREUnsupportedAndGreater= isJREVersionUnsupportedAndGreater(version, compilerCompliance);
				if (!compilerCompliance.equals(compliance)) { // Discourage using compiler with version other than compliance
					if (JavaModelUtil.is9OrHigher(compilerCompliance)) {
						if (fProject != null) {
							fCompilerReleaseCheck.setEnabled(checkValue(INTR_COMPLIANCE_FOLLOWS_EE, USER_CONF) || checkValue(INTR_COMPLIANCE_FOLLOWS_EE, DISABLED));
						} else {
							fCompilerReleaseCheck.setEnabled(true);
						}
						updateComplianceEnableSourceTargetState();
					} else {
						fCompilerReleaseCheck.setEnabled(false);
						fCompilerReleaseCheck.setSelection(false);
						setValue(PREF_RELEASE, DISABLED);
					}
				} else if (!JavaModelUtil.is9OrHigher(compilerCompliance)) {
					fCompilerReleaseCheck.setEnabled(false);
					fCompilerReleaseCheck.setSelection(false);
					setValue(PREF_RELEASE, JavaCore.DISABLED);
				} else {
					if (fProject == null
							|| isJREUnsupportedAndGreater
							|| JavaModelUtil.is9OrHigher(compliance)) {
						fCompilerReleaseCheck.setEnabled(true);
					}
					updateComplianceEnableSourceTargetState();
				}
			}
		}
	}

	private boolean isJREVersionUnsupportedAndGreater(String version, String compilerCompliance) {
		boolean isJREUnsupportedAndGreater= false;
		String versionStr= version;
		if (!JavaCore.isJavaSourceVersionSupportedByCompiler(versionStr)) {
			try {
				versionStr= getJREVersionString(versionStr);
				int jreVersion= Integer.parseInt(versionStr);
				int supportedVersion= Integer.parseInt(compilerCompliance);
				if (jreVersion > supportedVersion) {
					isJREUnsupportedAndGreater= true;
				}
			} catch (NumberFormatException e) {
				//do nothing
			}
		}
		return isJREUnsupportedAndGreater;
	}

	private String getJREVersionString(String version) {
		String newVersion= version;
		int index= newVersion.indexOf('.');
		if (index != -1) {
			newVersion= newVersion.substring(0, index);
		} else {
			index= newVersion.indexOf('-');
			if (index != -1)
				newVersion= newVersion.substring(0, index);
		}
		return newVersion;
	}

	private String getVersionLabel(String version) {
		return BasicElementLabels.getVersionName(version);
	}


	private IStatus validateCompliance() {
		StatusInfo status= new StatusInfo();
		String compliance= getValue(PREF_COMPLIANCE);
		String source= getValue(PREF_SOURCE_COMPATIBILITY);
		String target= getValue(PREF_CODEGEN_TARGET_PLATFORM);

		String firstSupported = JavaCore.getAllJavaSourceVersionsSupportedByCompiler().first();

		// compliance must be supported
		if (JavaModelUtil.isVersionLessThan(compliance, firstSupported)) {
			String warning = Messages.format(PreferencesMessages.ComplianceConfigurationBlock_unsupported_compliance, new String[] { firstSupported });
			setValue(PREF_COMPLIANCE, firstSupported);
			status.setWarning(warning);
		}
		// source must be supported
		if (JavaModelUtil.isVersionLessThan(source, firstSupported)) {
			String warning = Messages.format(PreferencesMessages.ComplianceConfigurationBlock_unsupported_source, new String[] { firstSupported });
			setValue(PREF_SOURCE_COMPATIBILITY, firstSupported);
			status.setWarning(warning);
		}
		// target must be supported
		if (JavaModelUtil.isVersionLessThan(target, firstSupported)) {
			String warning = Messages.format(PreferencesMessages.ComplianceConfigurationBlock_unsupported_target, new String[] { firstSupported });
			setValue(PREF_CODEGEN_TARGET_PLATFORM, firstSupported);
			status.setWarning(warning);
		}

		// compliance must not be smaller than source or target
		if (JavaModelUtil.isVersionLessThan(compliance, source)) {
			status.setError(PreferencesMessages.ComplianceConfigurationBlock_src_greater_compliance);
			return status;
		}

		if (JavaModelUtil.isVersionLessThan(compliance, target)) {
			status.setError(PreferencesMessages.ComplianceConfigurationBlock_classfile_greater_compliance);
			return status;
		}

		// target must not be smaller than source
		if (JavaModelUtil.isVersionLessThan(target, source)) {
			status.setError(PreferencesMessages.ComplianceConfigurationBlock_classfile_greater_source);
			return status;
		}

		return status;
	}


	@Override
	public void useProjectSpecificSettings(boolean enable) {
		super.useProjectSpecificSettings(enable);
		if (!enable) {
			initializeReleaseCheckBox(!enable);
		}
		updateReleaseOptionStatus();
		validateComplianceStatus();
	}

	private void updateComplianceFollowsEE() {
		if (fProject != null) {
			String complianceFollowsEE= DISABLED;
			IExecutionEnvironment ee= getEE();
			String label;
			if (ee != null) {
				complianceFollowsEE= getComplianceFollowsEE(ee);
				label= Messages.format(PreferencesMessages.ComplianceConfigurationBlock_compliance_follows_EE_with_EE_label, ee.getId());
			} else {
				label= PreferencesMessages.ComplianceConfigurationBlock_compliance_follows_EE_label;
			}
			Link checkBoxLink= getCheckBoxLink(INTR_COMPLIANCE_FOLLOWS_EE);
			if (checkBoxLink != null) {
				checkBoxLink.setText(label);
			}
			setValue(INTR_COMPLIANCE_FOLLOWS_EE, complianceFollowsEE);
		}
	}

	private void updateComplianceEnableState() {
		boolean enableComplianceControls= true;
		if (fProject != null) {
			boolean hasProjectSpecificOptions= hasProjectSpecificOptions(fProject);
			String complianceFollowsEE= getValue(INTR_COMPLIANCE_FOLLOWS_EE);
			updateCheckBox(getCheckBox(INTR_COMPLIANCE_FOLLOWS_EE));
			boolean enableComplianceFollowsEE= hasProjectSpecificOptions && ! DISABLED.equals(complianceFollowsEE); // is default or user
			updateControlsEnableState(fComplianceFollowsEEControls, enableComplianceFollowsEE);

			enableComplianceControls= hasProjectSpecificOptions && ! DEFAULT_CONF.equals(complianceFollowsEE); // is disabled or user
			updateControlsEnableState(fComplianceControls, enableComplianceControls);
		}

		boolean enableComplianceChildren= enableComplianceControls && checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF);
		updateControlsEnableState(fComplianceChildControls, enableComplianceChildren);
		updateReleaseOptionStatus();
	}

	private void updateComplianceEnableSourceTargetState() {
		boolean enableComplianceControls= true;
		if (fProject != null) {
			boolean hasProjectSpecificOptions= hasProjectSpecificOptions(fProject);
			String complianceFollowsEE= getValue(INTR_COMPLIANCE_FOLLOWS_EE);
			enableComplianceControls= hasProjectSpecificOptions && !DEFAULT_CONF.equals(complianceFollowsEE); // is disabled or user
		}
		boolean enableBasedOnRelease= !fCompilerReleaseCheck.getSelection();
		boolean enableComplianceChildren= enableComplianceControls && checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF) && enableBasedOnRelease;
		for (int i= fComplianceChildControls.size() - 1; i >= 0; i--) {
			Control curr= fComplianceChildControls.get(i);
			ControlData data= (ControlData) curr.getData();
			if (data != null) {
				if (PREF_SOURCE_COMPATIBILITY.equals(data.getKey())
						|| PREF_CODEGEN_TARGET_PLATFORM.equals(data.getKey())) {
					Combo combo= getComboBox(data.getKey());
					combo.setEnabled(enableComplianceChildren);
				}
			}
		}
	}

	private void updateControlsEnableState(List<Control> controls, boolean enable) {
		for (int i= controls.size() - 1; i >= 0; i--) {
			Control curr= controls.get(i);
			if (curr instanceof Composite) {
				updateControlsEnableState(Arrays.asList(((Composite)curr).getChildren()), enable);
			}
			curr.setEnabled(enable);
		}
		if (controls.contains(fReportPreviewCombo)) {
			fReportPreviewCombo.setEnabled(fEnablePreviewCheck.isEnabled() && fEnablePreviewCheck.getSelection());
		}
	}

	private void updatePreviewFeaturesState() {
		if (checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF)) {
			String compatibility= getValue(PREF_SOURCE_COMPATIBILITY);

			boolean isLessThanLatest= JavaModelUtil.isVersionLessThan(compatibility, VERSION_LATEST);
			updateRememberedComplianceOption(PREF_ENABLE_PREVIEW, IDX_ENABLE_PREVIEW, !isLessThanLatest, null);
			updateRememberedComplianceOption(PREF_PB_REPORT_PREVIEW, IDX_REPORT_PREVIEW, fEnablePreviewCheck.isEnabled() && fEnablePreviewCheck.getSelection(), WARNING);
		}
	}

	private void updatePreviewControls() {
		String compliance= getValue(PREF_COMPLIANCE);
		if (JavaCore.compareJavaVersions(compliance, VERSION_LATEST) < 0) {
			fEnablePreviewCheck.setSelection(false);
			fReportPreviewCombo.select(0);
		}
	}

	private void updateRememberedComplianceOption(Key prefKey, int idx, boolean enabled, String defaultComboValue) {
		if (prefKey.getName().equals(PREF_ENABLE_PREVIEW.getName())) {
			Button checkBox= getCheckBox(prefKey);
			boolean wasCheckBoxEnabled= checkBox.isEnabled();
			checkBox.setEnabled(enabled);

			if (enabled) {
				if (!wasCheckBoxEnabled) {
					String val= fRememberedUserCompliance[idx];
					if (ENABLED.equals(val)) {
						setValue(PREF_ENABLE_PREVIEW, val);
						updateCheckBox(checkBox);
					}
				}
			} else {
				String val= getValue(PREF_ENABLE_PREVIEW);
				if (wasCheckBoxEnabled) {
					fRememberedUserCompliance[idx]= val;
				}

				if (ENABLED.equals(val)) {
					setValue(PREF_ENABLE_PREVIEW, DISABLED);
					updateCheckBox(checkBox);
				}
			}

		} else {
			Combo combo= getComboBox(prefKey);
			combo.setEnabled(enabled);

			if (!enabled) {
				String val= getValue(prefKey);
				if (!defaultComboValue.equals(val)) {
					setValue(prefKey, defaultComboValue);
					updateCombo(combo);
					fRememberedUserCompliance[idx]= val;
				}
			} else {
				String val= fRememberedUserCompliance[idx];
				if (!defaultComboValue.equals(val)) {
					setValue(prefKey, val);
					updateCombo(combo);
				}
			}
		}

	}

	private void updateStoreMethodParamNamesEnableState() {
		boolean enabled= true;
		Button checkBox= getCheckBox(PREF_CODEGEN_METHOD_PARAMETERS_ATTR);
		boolean wasCheckBoxEnabled= checkBox.isEnabled();
		checkBox.setEnabled(enabled);

		if (enabled) {
			if (!wasCheckBoxEnabled) {
				String val= fRememberedUserCompliance[IDX_METHOD_PARAMETERS_ATTR];
				if (GENERATE.equals(val)) {
					setValue(PREF_CODEGEN_METHOD_PARAMETERS_ATTR, val);
					updateCheckBox(checkBox);
				}
			}
		} else {
			String val= getValue(PREF_CODEGEN_METHOD_PARAMETERS_ATTR);
			if (wasCheckBoxEnabled)
				fRememberedUserCompliance[IDX_METHOD_PARAMETERS_ATTR]= val;

			if (GENERATE.equals(val)) {
				setValue(PREF_CODEGEN_METHOD_PARAMETERS_ATTR, DO_NOT_GENERATE);
				updateCheckBox(checkBox);
			}
		}
	}

	/**
	 * Sets the default compliance values derived from the chosen level or restores the user
	 * compliance settings.
	 *
	 * @param rememberOld if <code>true</code>, the current compliance settings are remembered as
	 *            user settings. If <code>false</code>, overwrite the current settings.
	 * @param oldComplianceLevel the previous compliance level
	 */
	private void updateComplianceDefaultSettings(boolean rememberOld, String oldComplianceLevel) {
		String enablePreview, reportPreview, source, target;
		boolean isDefault= checkValue(INTR_DEFAULT_COMPLIANCE, DEFAULT_CONF);
		boolean isFollowEE= checkValue(INTR_COMPLIANCE_FOLLOWS_EE, DEFAULT_CONF);
		String complianceLevel= getValue(PREF_COMPLIANCE);
		boolean isRelease= checkValue(PREF_RELEASE, JavaCore.ENABLED) && !isDefault;

		if (isDefault || isFollowEE || isRelease) {
			if (rememberOld) {
				if (oldComplianceLevel == null) {
					oldComplianceLevel= complianceLevel;
				}

				fRememberedUserCompliance[IDX_ENABLE_PREVIEW]= getValue(PREF_ENABLE_PREVIEW);
				fRememberedUserCompliance[IDX_REPORT_PREVIEW]= getValue(PREF_PB_REPORT_PREVIEW);
				fRememberedUserCompliance[IDX_SOURCE_COMPATIBILITY]= getValue(PREF_SOURCE_COMPATIBILITY);
				fRememberedUserCompliance[IDX_CODEGEN_TARGET_PLATFORM]= getValue(PREF_CODEGEN_TARGET_PLATFORM);
				fRememberedUserCompliance[IDX_RELEASE]= getValue(PREF_RELEASE);
				fRememberedUserCompliance[IDX_COMPLIANCE]= oldComplianceLevel;
			}

			if (isFollowEE) {
				IExecutionEnvironment ee= getEE();
				Map<String, String> eeOptions= BuildPathSupport.getEEOptions(ee);
				if (eeOptions == null)
					return;

				enablePreview= eeOptions.get(PREF_ENABLE_PREVIEW.getName());
				reportPreview= eeOptions.get(PREF_PB_REPORT_PREVIEW.getName());
				source= eeOptions.get(PREF_SOURCE_COMPATIBILITY.getName());
				target= eeOptions.get(PREF_CODEGEN_TARGET_PLATFORM.getName());

				setValue(PREF_COMPLIANCE, eeOptions.get(PREF_COMPLIANCE.getName()));

				String release= eeOptions.get(PREF_RELEASE.getName());
				if (release == null) {
					setValue(PREF_RELEASE, DISABLED);
					fCompilerReleaseCheck.setSelection(false);
				}

			} else if (isRelease) {
				enablePreview= getValue(PREF_ENABLE_PREVIEW);
				reportPreview= getValue(PREF_PB_REPORT_PREVIEW);
				source= getValue(PREF_COMPLIANCE);
				target= getValue(PREF_COMPLIANCE);
			} else {
				HashMap<String, String> options= new HashMap<>();
				JavaModelUtil.setComplianceOptions(options, complianceLevel);
				if (complianceLevel.equals(options.get(JavaCore.COMPILER_COMPLIANCE))) {
					enablePreview= options.get(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES);
					reportPreview= options.get(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES);
					source= options.get(JavaCore.COMPILER_SOURCE);
					target= options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
				} else {
					enablePreview= DISABLED;
					reportPreview= WARNING;
					source= JavaCore.getAllJavaSourceVersionsSupportedByCompiler().first();
					target= JavaCore.getAllJavaSourceVersionsSupportedByCompiler().first();
				}
			}
		} else {
			if (rememberOld && complianceLevel.equals(fRememberedUserCompliance[IDX_COMPLIANCE])) {
				enablePreview= fRememberedUserCompliance[IDX_ENABLE_PREVIEW];
				reportPreview= fRememberedUserCompliance[IDX_REPORT_PREVIEW];
				source= fRememberedUserCompliance[IDX_SOURCE_COMPATIBILITY];
				target= fRememberedUserCompliance[IDX_CODEGEN_TARGET_PLATFORM];
			} else {
				updatePreviewFeaturesState();
				updateStoreMethodParamNamesEnableState();
				return;
			}
		}
		if (enablePreview == null) {
			enablePreview= DISABLED;
		}
		if (reportPreview == null) {
			reportPreview= WARNING;
		}
		setValue(PREF_ENABLE_PREVIEW, enablePreview);
		setValue(PREF_PB_REPORT_PREVIEW, reportPreview);
		setValue(PREF_SOURCE_COMPATIBILITY, source);
		setValue(PREF_CODEGEN_TARGET_PLATFORM, target);
		updateControls();
		updatePreviewFeaturesState();
		updateStoreMethodParamNamesEnableState();
	}

	private void updateComplianceReleaseSettings() {
		String compliance= getValue(PREF_COMPLIANCE);
		boolean isRelease= checkValue(PREF_RELEASE, JavaCore.ENABLED);
		if (isRelease) {
			setValue(PREF_SOURCE_COMPATIBILITY, compliance);
			setValue(PREF_CODEGEN_TARGET_PLATFORM, compliance);
		}
	}

	/**
	 * Evaluate if the current compliance setting correspond to a default setting.
	 *
	 * @return {@link #DEFAULT_CONF} or {@link #USER_CONF}
	 */
	private String getCurrentCompliance() {
		String complianceLevel= getValue(PREF_COMPLIANCE);

		HashMap<String, String> defaultOptions= new HashMap<>();
		JavaModelUtil.setComplianceOptions(defaultOptions, complianceLevel);

		boolean isDefault= complianceLevel.equals(defaultOptions.get(JavaCore.COMPILER_COMPLIANCE))
				&& getValue(PREF_SOURCE_COMPATIBILITY).equals(defaultOptions.get(JavaCore.COMPILER_SOURCE))
				&& getValue(PREF_CODEGEN_TARGET_PLATFORM).equals(defaultOptions.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM));
		if (JavaCore.compareJavaVersions(complianceLevel, JavaCore.VERSION_10) > 0) {
			isDefault= isDefault
					&& getValue(PREF_ENABLE_PREVIEW).equals(defaultOptions.get(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES))
					&& getValue(PREF_PB_REPORT_PREVIEW).equals(defaultOptions.get(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES));
		}
		return isDefault ? DEFAULT_CONF : USER_CONF;
	}

	private IExecutionEnvironment getEE() {
		if (fProject == null)
			return null;

		try {
			for (IClasspathEntry entry : JavaCore.create(fProject).getRawClasspath()) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					String eeId= JavaRuntime.getExecutionEnvironmentId(entry.getPath());
					if (eeId != null) {
						return JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(eeId);
					}
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Evaluate if the builds path contains an execution environment and the current compliance
	 * settings follow the EE options.
	 *
	 * @param ee the EE, or <code>null</code> if none available
	 * @return {@link #DEFAULT_CONF} if the compliance follows the EE, or {@link #USER_CONF} if the
	 *         settings differ, or {@link #DISABLED} if there's no EE at all
	 */
	private String getComplianceFollowsEE(IExecutionEnvironment ee) {
		Map<String, String> options= BuildPathSupport.getEEOptions(ee);
		if (options == null)
			return DISABLED;
		String complianceOption= options.get(JavaCore.COMPILER_COMPLIANCE);
		if (JavaCore.compareJavaVersions(complianceOption, JavaCore.getAllJavaSourceVersionsSupportedByCompiler().first()) < 0) {
			return DISABLED;
		} else if (JavaCore.compareJavaVersions(complianceOption, JavaCore.VERSION_10) > 0) {
			return checkDefaults(PREFS_COMPLIANCE_11_OR_HIGHER, options);
		} else {
			return checkDefaults(PREFS_COMPLIANCE, options);
		}
	}

	private String checkDefaults(Key[] keys, Map<String, String> options) {
		for (Key key : keys) {
			Object option= options.get(key.getName());
			if (!checkValue(key, (String)option))
				return USER_CONF;
		}
		return DEFAULT_CONF;
	}

	@Override
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.ComplianceConfigurationBlock_needsbuild_title;
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.ComplianceConfigurationBlock_needsfullbuild_message;
		} else {
			message= PreferencesMessages.ComplianceConfigurationBlock_needsprojectbuild_message;
		}
		return new String[] { title, message };
	}

	/**
	 * Sets the default compiler compliance options based on the current default JRE in the
	 * workspace.
	 *
	 * @since 3.5
	 */
	private void setDefaultCompilerComplianceValues() {
		IVMInstall defaultVMInstall= JavaRuntime.getDefaultVMInstall();
		if (defaultVMInstall instanceof IVMInstall2) {
			String complianceLevel= JavaModelUtil.getCompilerCompliance((IVMInstall2)defaultVMInstall, JavaCore.VERSION_1_8);
			if (isOriginalDefaultCompliance(complianceLevel)) {
				Map<String, String> complianceOptions= new HashMap<>();
				JavaModelUtil.setComplianceOptions(complianceOptions, complianceLevel);
				String releaseVal= complianceOptions.get(PREF_RELEASE.getName());
				setDefaultValue(PREF_COMPLIANCE, complianceOptions.get(PREF_COMPLIANCE.getName()));
				setDefaultValue(PREF_SOURCE_COMPATIBILITY, complianceOptions.get(PREF_SOURCE_COMPATIBILITY.getName()));
				setDefaultValue(PREF_CODEGEN_TARGET_PLATFORM, complianceOptions.get(PREF_CODEGEN_TARGET_PLATFORM.getName()));
				setDefaultValue(PREF_RELEASE, releaseVal != null ? releaseVal : DISABLED);
				if (JavaCore.compareJavaVersions(complianceLevel, JavaCore.VERSION_10) > 0) {
					setDefaultValue(PREF_ENABLE_PREVIEW, complianceOptions.get(PREF_ENABLE_PREVIEW.getName()));
					setDefaultValue(PREF_PB_REPORT_PREVIEW, complianceOptions.get(PREF_PB_REPORT_PREVIEW.getName()));
				} else {
					setDefaultValue(PREF_ENABLE_PREVIEW, DISABLED);
					setDefaultValue(PREF_PB_REPORT_PREVIEW, WARNING);
				}
			}
		}
	}

	/**
	 * Tells whether the compliance option is the same as the original default.
	 * @param complianceLevel the compliance level
	 *
	 * @return <code>true</code> if the compliance is the same as the original default
	 * @since 3.6
	 */
	private static final boolean isOriginalDefaultCompliance(String complianceLevel) {
		Hashtable<String, String> options= JavaCore.getDefaultOptions();
		Preferences bundleDefaults= BundleDefaultsScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);

		boolean isDefault= equals(JavaCore.COMPILER_COMPLIANCE, bundleDefaults, options)
				&& equals(JavaCore.COMPILER_SOURCE, bundleDefaults, options)
				&& equals(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, bundleDefaults, options)
				&& equals(JavaCore.COMPILER_RELEASE, bundleDefaults, options);
		if (JavaCore.compareJavaVersions(complianceLevel, JavaCore.VERSION_10) > 0) {
			isDefault= isDefault
					&& equals(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, bundleDefaults, options)
					&& equals(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, bundleDefaults, options);
		}
		return isDefault;
	}

	/**
	 * Returns whether the option for the given key is the same in the map and the preferences.
	 *
	 * @param key the key of option to test
	 * @param preferences the preferences
	 * @param map the map
	 * @return <code>true</code> if the options are the same in both maps
	 * @since 3.6
	 */
	private static boolean equals(String key, Preferences preferences, Map<String, String> map) {
		String dummy= ""; //$NON-NLS-1$
		String defaultValue= preferences.get(key, dummy);
		return defaultValue != null && defaultValue != dummy
				? map.containsKey(key) && equals(defaultValue, map.get(key))
				: !map.containsKey(key);
	}

	/**
	 * Returns whether the objects are equal.
	 *
	 * @param o1 an object
	 * @param o2 an object
	 * @return <code>true</code> if the two objects are equal
	 * @since 3.6
	 */
	private static boolean equals(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}

}
