package com.plugin.autoflox.views;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class AutofloxView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.plugin.autoflox.views.AutofloxView";

	public static TableViewer viewer;
	public static Table resultTable;

	public static Composite parent;

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

	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {

		this.parent = parent;

		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		// SWT table in console view to show result
		resultTable = viewer.getTable();
		resultTable.setLocation(0, 0);
		resultTable.setSize(parent.getSize().x, parent.getSize().y);
		resultTable.setLinesVisible(true);
		resultTable.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		resultTable.setLayoutData(data);

		// Init titles
		String[] titles = { " Line # ", " Function Name ", " File Path " };

		// line # column
		TableColumn LineNoColumn = new TableColumn(resultTable, SWT.NONE);
		LineNoColumn.setText(titles[0]);
		LineNoColumn.setWidth(150);

		// func name column
		TableColumn funcNameColumn = new TableColumn(resultTable, SWT.NONE);
		funcNameColumn.setText(titles[1]);
		funcNameColumn.setWidth(200);

		// func name column
		TableColumn filePathColumn = new TableColumn(resultTable, SWT.NONE);
		filePathColumn.setText(titles[2]);
		filePathColumn.setWidth(500);

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
		resultTable.removeAll();
	}

}