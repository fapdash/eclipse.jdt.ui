/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.core.SourceRefElement;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Helper class to transplant a IJavaElement handle from a certain state of the
 * Java Model into another.
 * 
 * The changes to the workspace include one type rename, a number of field
 * renames, and a number of method renames including signature changes.
 * 
 * The returned handle exists in the target model state.
 * 
 * TODO This class still references internal API, see bug 115040
 * 
 * @since 3.2
 * 
 */
public class RefactoringHandleTransplanter {

	private IType fOldType;
	private IType fNewType;
	private Map fRefactoredDerivedElements;

	public RefactoringHandleTransplanter(IType oldType, IType newType, Map refactoredDerivedElements) {
		fOldType= oldType;
		fNewType= newType;
		fRefactoredDerivedElements= refactoredDerivedElements;
	}

	/**
	 * Converts the handle. Handle need not exist, but must be a source
	 * reference.
	 * 
	 * @param handle
	 * @return the new handle
	 */
	public IJavaElement transplantHandle(IJavaElement handle) {

		/*
		 * Create a list of handles from top-level type to the handle
		 */
		final List oldElements= new ArrayList();
		addElements(handle, oldElements);

		/*
		 * Step through the elements and re-locate them in the new parents.
		 */
		final IJavaElement[] newElements= convertElements((IJavaElement[]) oldElements.toArray(new IJavaElement[0]));

		return newElements[newElements.length - 1];
	}

	private IJavaElement[] convertElements(IJavaElement[] oldElements) {

		final IJavaElement[] newElements= new IJavaElement[oldElements.length];
		final IJavaElement first= oldElements[0];

		Assert.isTrue(first instanceof IType);

		if (first.equals(fOldType))
			// We renamed a top level type.
			newElements[0]= fNewType;
		else
			newElements[0]= first;

		/*
		 * Note that we only need to translate the information necessary to
		 * create new handles. For example, the return type of a method is not
		 * relevant; neither is information about generic specifics in types.
		 */

		for (int i= 1; i < oldElements.length; i++) {
			final IJavaElement newParent= newElements[i - 1];
			final IJavaElement currentElement= oldElements[i];
			switch (newParent.getElementType()) {
				case IJavaElement.TYPE: {
					switch (currentElement.getElementType()) {
						case IJavaElement.TYPE: {
							final String newName= resolveTypeName((IType) currentElement);
							newElements[i]= ((IType) newParent).getType(newName);
							break;
						}
						case IJavaElement.METHOD: {
							final String newName= resolveElementName(currentElement);
							final String[] newParameterTypes= resolveParameterTypes((IMethod) currentElement);
							newElements[i]= ((IType) newParent).getMethod(newName, newParameterTypes);
							break;
						}
						case IJavaElement.INITIALIZER: {
							final SourceRefElement initializer= (SourceRefElement) currentElement;
							newElements[i]= ((IType) newParent).getInitializer(initializer.occurrenceCount);
							break;
						}
						case IJavaElement.FIELD: {
							final String newName= resolveElementName(currentElement);
							newElements[i]= ((IType) newParent).getField(newName);
							break;
						}
					}
					break;
				}
				case IJavaElement.METHOD: {
					switch (currentElement.getElementType()) {
						case IJavaElement.TYPE: {
							newElements[i]= resolveTypeInMember((IMethod) newParent, (IType) currentElement);
							break;
						}
					}
					break;
				}
				case IJavaElement.INITIALIZER: {
					switch (currentElement.getElementType()) {
						case IJavaElement.TYPE: {
							newElements[i]= resolveTypeInMember((IInitializer) newParent, (IType) currentElement);
							break;
						}
					}
				}
			}
		}
		return newElements;
	}

	private String[] resolveParameterTypes(IMethod method) {
		final String[] oldParameterTypes= method.getParameterTypes();
		final String[] newparams= new String[oldParameterTypes.length];

		final String[] possibleOldSigs= new String[4];
		possibleOldSigs[0]= Signature.createTypeSignature(fOldType.getElementName(), false);
		possibleOldSigs[1]= Signature.createTypeSignature(fOldType.getElementName(), true);
		possibleOldSigs[2]= Signature.createTypeSignature(fOldType.getFullyQualifiedName(), false);
		possibleOldSigs[3]= Signature.createTypeSignature(fOldType.getFullyQualifiedName(), true);

		final String[] possibleNewSigs= new String[4];
		possibleNewSigs[0]= Signature.createTypeSignature(fNewType.getElementName(), false);
		possibleNewSigs[1]= Signature.createTypeSignature(fNewType.getElementName(), true);
		possibleNewSigs[2]= Signature.createTypeSignature(fNewType.getFullyQualifiedName(), false);
		possibleNewSigs[3]= Signature.createTypeSignature(fNewType.getFullyQualifiedName(), true);

		// Textually replace all occurrences
		// This handles stuff like Map<SomeClass, some.package.SomeClass>
		for (int i= 0; i < oldParameterTypes.length; i++) {
			newparams[i]= oldParameterTypes[i];
			for (int j= 0; j < possibleOldSigs.length; j++) {
				newparams[i]= newparams[i].replaceAll(possibleOldSigs[j], possibleNewSigs[j]);
			}
		}
		return newparams;
	}

	private String resolveElementName(IJavaElement method) {
		final String newName= (String) fRefactoredDerivedElements.get(method);
		if (newName != null)
			return newName;
		else
			return method.getElementName();
	}

	private IJavaElement resolveTypeInMember(IMember newParent, IType oldChild) {
		// Local type or anonymous type. Only local types can be renamed.
		final SourceRefElement type= (SourceRefElement) oldChild;
		String newName= ""; //$NON-NLS-1$
		if (oldChild.getElementName().length() != 0)
			newName= resolveTypeName(oldChild);
		return newParent.getType(newName, type.occurrenceCount);
	}

	private String resolveTypeName(IType type) {
		return type.equals(fOldType) ? fNewType.getElementName() : type.getElementName();
	}

	private void addElements(IJavaElement element, List chain) {
		if ( (element instanceof IMember) || (element instanceof ILocalVariable)) {
			chain.add(0, element);
			if (element.getParent() != null)
				addElements(element.getParent(), chain);
		}
	}
}
