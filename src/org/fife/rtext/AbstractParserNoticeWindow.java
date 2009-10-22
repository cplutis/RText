/*
 * 10/21/2009
 *
 * AbstractParserNoticeWindow.java - Base class for dockable windows that
 * display parser notices.
 * Copyright (C) 2009 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.fife.rtext;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;

import org.fife.ui.FileExplorerTableModel;
import org.fife.ui.FileExplorerTableModel.SortableHeaderRenderer;
import org.fife.ui.dockablewindows.DockableWindow;


/**
 * Base class for dockable windows containing parser notices.
 *
 * @author Robert Futrell
 * @version 1.0
 */
abstract class AbstractParserNoticeWindow extends DockableWindow {

	private RText rtext;
	private JTable table;


	public AbstractParserNoticeWindow(RText rtext) {
		this.rtext = rtext;
	}


	protected JTable createTable(TableModel model) {
		table = new JTable();
		fixTableModel(model);
		table.addMouseListener(new TableMouseListener());
		return table;
	}


	private void fixTableModel(TableModel model) {

		JTableHeader old = table.getTableHeader();
		table.setTableHeader(new JTableHeader(old.getColumnModel()));

		FileExplorerTableModel model2 = new FileExplorerTableModel(model,
				table.getTableHeader());

		model2.setColumnComparator(Integer.class, new Comparator() {	
			public int compare(Object o1, Object o2) {
				Integer int1 = (Integer)o1;
				Integer int2 = (Integer)o2;
				return int1.compareTo(int2);
			}
		});

		model2.setColumnComparator(RTextEditorPane.class, new Comparator() {	
			public int compare(Object o1, Object o2) {
				RTextEditorPane ta1 = (RTextEditorPane)o1;
				RTextEditorPane ta2 = (RTextEditorPane)o2;
				return ta1.getFileName().compareTo(ta2.getFileName());
			}
		});

		table.setDefaultRenderer(Icon.class, new IconTableCellRenderer());
		table.setDefaultRenderer(RTextEditorPane.class,
								new TextAreaTableCellRenderer());

		table.setModel(model2);

		TableColumnModel tcm = table.getColumnModel();
		tcm.getColumn(0).setPreferredWidth(32);
		tcm.getColumn(0).setWidth(32);
		tcm.getColumn(1).setPreferredWidth(200);
		tcm.getColumn(1).setWidth(200);
		tcm.getColumn(2).setPreferredWidth(48);
		tcm.getColumn(2).setWidth(48);
		tcm.getColumn(3).setPreferredWidth(800);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

	}


	/**
	 * Overridden to work around Java bug 6429812.  As of Java 6 (!), changing
	 * LaF from Windows to something else (e.g. Metal) at runtime with a custom
	 * delegating TableCellRenderer on a JTableHeader can cause NPE's.  Then,
	 * the UI never repaints from then on.  Seems to happen only when the table
	 * is actually visible during the LaF change.  See
	 * <a href="http://bugs.sun.com/view_bug.do?bug_id=6429812">6429812</a>.
	 */
	public void updateUI() {

		/*
		 * NOTE: This is silly, but it's what it took to get a LaF change to
		 * occur without throwing an NPE because of the JRE bug.  No doubt there
		 * is a better way to handle this.  What we do is:
		 *
		 * 1. Before updating the UI, reset the JTableHeader's default renderer
		 *    to what it was originally.  This prevents the NPE from the JRE
		 *    bug, since we're no longer using the renderer with a cached
		 *    Windows-specific TableCellRenderer (when Windows LaF is enabled).
		 * 2. Update the UI, like normal.
		 * 3. After the update, we must explicitly re-set the JTableHeader as
		 *    the column view in the enclosing JScrollPane.  This is done by
		 *    default the first time you add a JTable to a JScrollPane, but
		 *    since we gave the JTable a new header as a workaround for the JRE
		 *    bug, we must explicitly tell the JScrollPane about it as well.
		 */

		TableModel model = null;
		if (table!=null) {
			model = ((FileExplorerTableModel)table.getModel()).getTableModel();
			TableCellRenderer r = table.getTableHeader().getDefaultRenderer();
			if (r instanceof SortableHeaderRenderer) { // Always true
				SortableHeaderRenderer shr = (SortableHeaderRenderer)r;
				table.getTableHeader().setDefaultRenderer(
											shr.getDelegateRenderer());
			}
		}

		super.updateUI();

		if (table!=null) {
			JScrollPane sp = (JScrollPane)table.getParent().getParent();
			fixTableModel(model);
			sp.setColumnHeaderView(table.getTableHeader());
			sp.revalidate();
			sp.repaint();
		}

	}


	/**
	 * Basic model for tables displaying parser notices.
	 */
	protected abstract class ParserNoticeTableModel extends DefaultTableModel {

		public ParserNoticeTableModel(String lastColHeader) {
			String[] colHeaders = new String[] { "", "File", "Line", lastColHeader, }; // TODO
			setColumnIdentifiers(colHeaders);
		}

		protected abstract void addNoticesImpl(RTextEditorPane textArea,
												List notices);

		public Class getColumnClass(int col) {
			Class clazz = null;
			switch (col) {
				case 0:
					clazz = Icon.class;
					break;
				case 1:
					clazz = RTextEditorPane.class;
					break;
				case 2:
					clazz = Integer.class;
					break;
				default:
					clazz = super.getColumnClass(col);
			}
			return clazz;
		}
			
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		public void update(RTextEditorPane textArea, List notices) {
			//setRowCount(0);
			for (int i=0; i<getRowCount(); i++) {
				RTextEditorPane textArea2 = (RTextEditorPane)getValueAt(i, 1);
				if (textArea2==textArea) {
					removeRow(i);
					i--;
				}
			}
			if (notices!=null) {
				addNoticesImpl(textArea, notices);
			}
		}

	}


	/**
	 * A table cell renderer for icons.
	 */
	private static class IconTableCellRenderer extends DefaultTableCellRenderer{

		static final Border b = BorderFactory.createEmptyBorder(0, 5, 0, 5);

		public Component getTableCellRendererComponent(JTable table,
			Object value, boolean selected, boolean focus, int row, int col) {
			super.getTableCellRendererComponent(table, value, selected, focus,
					row, col);
			setText(null);
			setIcon((Icon)value);
			setBorder(b);
			return this;
		}

	}


	/**
	 * Renders a text area in a table cell as just the file name.
	 */
	private static class TextAreaTableCellRenderer
					extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table,
			Object value, boolean selected, boolean focus, int row, int col) {
			super.getTableCellRendererComponent(table, value, selected, focus,
					row, col);
			RTextEditorPane textArea = (RTextEditorPane)value;
			setText(textArea.getFileName());
			return this;
		}

	}


	private class TableMouseListener extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {

			if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
				int row = table.rowAtPoint(e.getPoint());
				if (row>-1) {
					RTextEditorPane textArea =
						(RTextEditorPane)table.getValueAt(row, 1);
					AbstractMainView mainView = rtext.getMainView();
					if (mainView.setSelectedTextArea(textArea)) {
						Integer i = (Integer)table.getValueAt(row, 2);
						int line = i.intValue();
						try {
							textArea.setCaretPosition(
								textArea.getLineStartOffset(line));
						} catch (BadLocationException ble) {
							UIManager.getLookAndFeel().
										provideErrorFeedback(textArea);
						}
						textArea.requestFocusInWindow();
					}
				}
			}

		}

	}


}