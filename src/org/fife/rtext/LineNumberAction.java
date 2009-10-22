/*
 * 11/14/2003
 *
 * LineNumberAction.java - Action to enable/disable line numbers in RText.
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

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.KeyStroke;

import org.fife.ui.app.StandardAction;


/**
 * Action used by an <code>AbstractMainView</code> to enable viewing
 * line numbers for the open documents.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class LineNumberAction extends StandardAction {


	/**
	 * Creates a new <code>LineNumberAction</code>.
	 *
	 * @param owner The rtext window that owns this action.
	 * @param text The text associated with the action.
	 * @param icon The icon associated with the action.
	 * @param desc The description of the action.
	 * @param mnemonic The mnemonic for the action.
	 * @param accelerator The accelerator key for the action.
	 */
	public LineNumberAction(RText owner, String text, Icon icon,
				String desc, int mnemonic, KeyStroke accelerator) {
		super(owner, text, icon, desc, mnemonic, accelerator);
	}


	public void actionPerformed(ActionEvent e) {
		RText owner = (RText)getApplication();
		AbstractMainView mainView = owner.getMainView();
		mainView.setLineNumbersEnabled(!mainView.getLineNumbersEnabled());
	}


}