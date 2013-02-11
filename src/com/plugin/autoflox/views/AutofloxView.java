package com.plugin.autoflox.views;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
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
		
		// Result table
		resultTable = viewer.getTable();
		resultTable.setLocation(0, 0);
		resultTable.setSize(parent.getSize().x, parent.getSize().y);
		resultTable.setLinesVisible(true);
		resultTable.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		resultTable.setLayoutData(data);
		
		resultTable.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		          try {
					goToLine(7);
				} catch (BadLocationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		        }
		      });

		// Init titles
		String[] titles = { " Time ", " Line # ", " File Path " };

		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(resultTable, SWT.NONE);
			column.setText(titles[i]);
		}

		for (int i = 0; i < titles.length; i++) {
			resultTable.getColumn(i).pack();
		}

	}

	public static void printToConsole() {
		Link link = new Link(parent, SWT.NONE);
		String message = "<a>This is a link</a>";
		link.setText(message);
		link.setSize(400, 100);

		link.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("You have selected: " + event.text);

				try {
					goToLine(3);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		});
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
}