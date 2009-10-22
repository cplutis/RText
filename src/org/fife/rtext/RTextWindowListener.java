/*
 * 11/14/2003
 *
 * RTextWindowListener.java - Listens for RText instances to close, so it
 * knows when to terminate the JVM.
 * Copyright (C) 2003 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://rtext.fifesoft.com
 *
 * This file is a part of RText.
 *
 * RText is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * RText is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.fife.rtext;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * A window listener that listens for rtext instances to close, so it
 * knows when to terminate the JVM.  All it does is keep track of how
 * many rtext windows are open, and terminates the JVM when that number
 * reaches zero.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class RTextWindowListener extends WindowAdapter {

	private RText owner;


	/**
	 * Creates a new <code>RTextWindowListener</code>.
	 *
	 * @param owner The first rtext window to register under this listener.
	 */
	public RTextWindowListener(RText owner) {
		this.owner = owner;
	}


	/**
	 * {@inheritDoc}
	 */
	public void windowActivated(WindowEvent e) {
		// Ensure that the current text document (if any) has focus.
		RTextEditorPane editor = owner.getMainView().getCurrentTextArea();
		if (editor!=null) {
			editor.requestFocusInWindow();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void windowDeactivated(WindowEvent e) {
		// Make sure the selection is always visible.
		RTextEditorPane editor = owner.getMainView().getCurrentTextArea();
		if (editor!=null) {
			editor.getCaret().setSelectionVisible(true);
		}
	}


	/**
	 * Overridden to help minimize a Swing issue on Windows.  If the main
	 * application window is minimized for a long time (e.g. overnight), the
	 * system seems to cache the entire app to disk, resulting in a very long
	 * pause when the user brings the app back up.
	 *
	 * {@inheritDoc}
	 */
	public void windowIconified(WindowEvent e) {
		System.gc();
	}


}