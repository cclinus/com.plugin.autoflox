package com.plugin.autoflox.views;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class AutofloxView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.plugin.autoflox.views.AutofloxView";

	public static TableViewer viewer;
	public static Tree PanelTree;
	public static Map<String, TreeItem> consoleTableMap;

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			return new String[] {};
		}
	}

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public AutofloxView() {
		this.consoleTableMap = new HashMap<String, TreeItem>();
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {

	    Tree tree = new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
	    tree.setHeaderVisible(true);
	    this.PanelTree = tree;
	    
	    TreeColumn descriptionColumn = new TreeColumn(tree, SWT.LEFT);
	    descriptionColumn.setText("Description");
	    descriptionColumn.setWidth(450);
	    
	    TreeColumn pathColumn = new TreeColumn(tree, SWT.LEFT);
	    pathColumn.setText("Path");
	    pathColumn.setWidth(300);
	    
	    TreeColumn locationColumn = new TreeColumn(tree, SWT.LEFT);
	    locationColumn.setText("Line");
	    locationColumn.setWidth(70);
	    
	    TreeColumn typeColumn = new TreeColumn(tree, SWT.LEFT);
	    typeColumn.setText("Type");
	    typeColumn.setWidth(100);

	}

	public static IEditorPart getEditor() {
		IWorkbench workbench = PlatformUI.getWorkbench();

		IWorkbenchWindow window = workbench == null ? null : workbench
				.getActiveWorkbenchWindow();
		IWorkbenchPage activePage = window == null ? null : window
				.getActivePage();
		IEditorPart editor = activePage == null ? null : activePage
				.getActiveEditor();
		return editor;
	}

	public static String getWorkspacePath() {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toString();
	}

	public static String getOpenedFilePath() {
		IEditorPart editor = getEditor();
		IEditorInput input = editor == null ? null : editor.getEditorInput();
		IPath path = input instanceof FileEditorInput ? ((FileEditorInput) input)
				.getPath() : null;

		if (path != null) {
			return path.toString();
		} else {
			return null;
		}
	}
	
	public static void openFileInEditor(String path){
		File fileToOpen = new File(path);
		 
		if (fileToOpen.exists() && fileToOpen.isFile()) {
		    IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
		    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		 
		    try {
		        IDE.openEditorOnFileStore( page, fileStore );
		    } catch ( PartInitException e ) {
		        //Put your exception handler here if you wish to
		    }
		} else {
		    //Do something if the file does not exist
			System.err.println("File does not exist: " + path);
		}
	}

	public static void goToLine(int lineNumber)
			throws org.eclipse.jface.text.BadLocationException {
		IEditorPart editorPart = getEditor();
		if (!(editorPart instanceof ITextEditor) || lineNumber <= 0) {
			return;
		}
		ITextEditor editor = (ITextEditor) editorPart;
		IDocument document = editor.getDocumentProvider().getDocument(
				editor.getEditorInput());
		if (document != null) {
			IRegion lineInfo = null;
			// line count internaly starts with 0, and not with 1 like in
			// GUI
			lineInfo = document.getLineInformation(lineNumber - 1);
			if (lineInfo != null) {
				editor.selectAndReveal(lineInfo.getOffset(),
						lineInfo.getLength());
			}
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		// viewer.getControl().setFocus();
	}
	
	public static void cleanConsole(){
		PanelTree.removeAll();
	}

}