/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateVariable;

/**
 * Compilation unit context type.
 */
public abstract class CompilationUnitContextType extends ContextType {
	
	/** the document string */
	protected String fString;

	/** the completion position within the document string */
	protected int fPosition;

	/** the associated compilation unit, may be <code>null</code> */
	protected ICompilationUnit fCompilationUnit;

	protected static class ReturnType extends TemplateVariable {
	 	public ReturnType() {
	 	 	super("return_type", TemplateMessages.getString("JavaContextType.variable.description.return.type"));
	 	}
	 	public String evaluate(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(IJavaElement.METHOD);
			if (element == null)
				return null;

			try {
				return Signature.toString(((IMethod) element).getReturnType());
			} catch (JavaModelException e) {
				return null;
			}
		}
		public boolean isResolved(TemplateContext context) {
			return evaluate(context) != null;
		}		
	}

	protected static class File extends TemplateVariable {
		public File() {
			super("file", TemplateMessages.getString("JavaContextType.variable.description.file"));
		}
		public String evaluate(TemplateContext context) {
			ICompilationUnit unit= ((CompilationUnitContext) context).getCompilationUnit();
			
			return (unit == null) ? null : unit.getElementName();
		}
		public boolean isResolved(TemplateContext context) {
			return evaluate(context) != null;
		}		
	}

	protected static class EnclosingJavaElement extends TemplateVariable {
		protected final int fElementType;
		
		public EnclosingJavaElement(String name, String description, int elementType) {
			super(name, description);
			fElementType= elementType;
		}
		public String evaluate(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(fElementType);
			return (element == null) ? null : element.getElementName();			
		}
		public boolean isResolved(TemplateContext context) {
			return evaluate(context) != null;
		}
	}
	
	protected static class Method extends EnclosingJavaElement {
		public Method() {
			super("method", TemplateMessages.getString("JavaContextType.variable.description.method"), IJavaElement.METHOD);
		}
	}

	protected static class Type extends EnclosingJavaElement {
		public Type() {
			super("type", TemplateMessages.getString("JavaContextType.variable.description.type"), IJavaElement.TYPE);
		}
	}
/*
	protected static class SuperClass extends EnclosingJavaElement {
		public Type() {
			super("super_class", TemplateMessages.getString("JavaContextType.variable.description.type"), IJavaElement.TYPE);
		}
	}
*/
	protected static class Package extends EnclosingJavaElement {
		public Package() {
			super("package", TemplateMessages.getString("JavaContextType.variable.description.package"), IJavaElement.PACKAGE_FRAGMENT);
		}
	}	

	protected static class Project extends EnclosingJavaElement {
		public Project() {
			super("project", TemplateMessages.getString("JavaContextType.variable.description.project"), IJavaElement.JAVA_PROJECT);
		}
	}	
/*
	protected static class Project2 extends TemplateVariable {
		public Project2() {
			super("project", TemplateMessages.getString("JavaContextType.variable.description.project"));
		}
		public String evaluate(TemplateContext context) {
			ICompilationUnit unit= ((JavaContext) context).getUnit();
			return (unit == null) ? null : unit.getJavaProject().getElementName();
		}
	}	
*/
	protected static class Arguments extends TemplateVariable {
		public Arguments() {
			super("arguments", TemplateMessages.getString("JavaContextType.variable.description.arguments"));
		}
		public String evaluate(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(IJavaElement.METHOD);
			if (element == null)
				return null;
				
			IMethod method= (IMethod) element;
			
			try {
				String[] arguments= method.getParameterNames();
				StringBuffer buffer= new StringBuffer();
				
				for (int i= 0; i < arguments.length; i++) {
					if (i > 0)
						buffer.append(", ");
					buffer.append(arguments[i]);				
				}
				
				return buffer.toString();

			} catch (JavaModelException e) {
				return null;
			}
		}
	}

/*	
	protected static class Line extends TemplateVariable {
		public Line() {
			super("line", TemplateMessages.getString("CompilationUnitContextType.variable.description.line"));
		}
		public String evaluate(TemplateContext context) {
			return ((JavaTemplateContext) context).guessLineNumber();
		}
	}
*/	

	/*
	 * @see ContextType#ContextType(String)
	 */
	public CompilationUnitContextType(String name) {
		super(name);	
	}

	/**
	 * Sets context parameters. Needs to be called before createContext().
	 */
	public void setContextParameters(String string, int position, ICompilationUnit compilationUnit) {
		fString= string;
		fPosition= position;
		fCompilationUnit= compilationUnit;
	}

}
