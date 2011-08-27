/*
 * 07/23/2011
 *
 * RunMacroAction.java - Action that runs a macro.
 * Copyright (C) 2011 Robert Futrell
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
package org.fife.rtext.plugins.macros;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.fife.rtext.RText;
import org.fife.rtext.RTextUtilities;
import org.fife.ui.app.StandardAction;


/**
 * Action that runs a macro (script).  <code>javax.script</code> classes
 * are referenced via reflection, since we support Java 1.4 and 1.5.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RunMacroAction extends StandardAction {

	/**
	 * The macro plugin.
	 */
	private MacroPlugin plugin;

	/**
	 * The macro to run.
	 */
	private Macro macro;

	/**
	 * The javax.script.Bindings instance.
	 */
	private Object bindings;

	/**
	 * The javax.script.ScriptContext#ENGINE_SCOPE field value.
	 */
	private static Integer scopeFieldValue;

	/**
	 * The javax.script.ScriptEngine#createBindings() method.
	 */
	private static Method createBindingsMethod;

	/**
	 * The javax.script.ScriptEngine#setBindings() method.
	 */
	private static Method setBindingsMethod;

	/**
	 * The javax.script.Bindings#put() method.
	 */
	private static Method bindingsPutMethod;

	/**
	 * The javax.script.ScriptEngine#eval(Reader) method.
	 */
	private static Method evalMethod;

	/**
	 * javax.script.ScriptEngine class.
	 */
	private static Class seClazz;

	/**
	 * The javax.script.ScriptEngineManager class.
	 */
	private static Class semClazz;

	/**
	 * The javax.script.ScriptEngineManager instance.
	 */
	private static Object sem;

	/**
	 * the javax.script.Scriptcontext class.
	 */
	private static Class scriptContextClazz;

	/**
	 * The javax.script.Bindings class.
	 */
	private static Class bindingsClazz;

	/**
	 * javax.script.ScriptEngine instance for JavaScript, shared across all
	 * instances of this action.
	 */
	private static Object jsEngine;

	/**
	 * javax.script.ScriptEngine instance for Groovy, shared across all
	 * instances of this action.
	 */
	private static Object groovyEngine;


	/**
	 * Constructor.
	 *
	 * @param app The parent application.
	 * @param plugin The plugin.
	 * @param tool The tool to run.
	 */
	public RunMacroAction(RText app, MacroPlugin plugin, Macro macro) {
		super(app, macro.getName());
		this.plugin = plugin;
		String shortcut = macro.getAccelerator();
		setAccelerator(shortcut==null ? null : KeyStroke.getKeyStroke(shortcut));
		setShortDescription(macro.getDesc());
		this.macro = macro;
	}



	public void actionPerformed(ActionEvent e) {
		handleSubmit(macro);
	}


	private void handleSubmit(Macro macro) {

		// Verify that the file exists before trying to run it.
		File file = new File(macro.getFile());
		if (!file.isFile()) {
			String text = plugin.getString("Error.ScriptDoesNotExist",
									file.getAbsolutePath());
			RText app = (RText)getApplication();
			String title = app.getString("ErrorDialogTitle");
			int rc = JOptionPane.showConfirmDialog(app, text, title,
					JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
			if (rc==JOptionPane.YES_OPTION) {
				MacroManager.get().removeMacro(macro);
			}
			return;
		}

		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			try {
				handleSubmit(file.getName(), r);
			} finally {
				r.close();
			}
		} catch (Throwable t/*IOException ioe*/) {
			getApplication().displayException(t/*ioe*/);
		}

	}


	private void handleSubmit(String sourceName, BufferedReader r) throws
								Throwable {

		RText app = (RText)getApplication();

		// If we didn't initialize properly, no point in proceeding.
		if (semClazz==null) {
			showErrorInitializingMessage();
			return;
		}

		/*
		ScriptEngineManager sem = new ScriptEngineManager();
		ScriptEngine jsEngine = sem.getEngineByName("JavaScript");
		Bindings bindings = jsEngine.createBindings();
		bindings.put("data", data);
		jsEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
		jsEngine.eval("println(data.str); alert(data.str); data.i = 999");
		 */

		Object engine = null;
		if (sourceName.endsWith(".js")) {
			engine = initJavaScriptEngine();
			if (engine==null) { // An error message was already displayed
				return;
			}
		}
		else if (sourceName.endsWith(".groovy")) {
			engine = initGroovyEngine();
			if (engine==null) { // An error message was already displayed
				return;
			}
		}
		else {
			app.displayException(new Exception("Bad macro type: " + sourceName));
			return;
		}

		// Run everything with reflection so we compile with Java 1.4.
		try {

			// Create our bindings and cache them for later.
			bindings = createBindingsMethod.invoke(engine, null);
			setBindingsMethod.invoke(engine,
					new Object[] { bindings, scopeFieldValue });

			// We always reset the value of "rtext" and "textArea", but
			// all other variables they've modified are persistent.
			bindingsPutMethod.invoke(bindings, new Object[] { "rtext", app });
			bindingsPutMethod.invoke(bindings, new Object[] { "textArea",
					app.getMainView().getCurrentTextArea() });

			evalMethod.invoke(engine, new Object[] { r });

		} catch (Throwable ex) {
			// Since we launch via reflection, peel off top-level Exception
			if (ex instanceof InvocationTargetException) {
				ex = ex.getCause();
			}
			throw ex;
		}

	}


	/**
	 * Returns the Groovy engine, lazily creating it if necessary.
	 *
	 * @return The script engine, or <code>null</code> if it cannot be created.
	 */
	private Object initGroovyEngine() {

		File groovyJar = new File(getApplication().getInstallLocation(),
				"plugins/groovy-all.jar");
		if (!groovyJar.isFile()) {
			String message = plugin.getString("Error.NoGroovyJar",
								groovyJar.getAbsolutePath());
			RText app = (RText)getApplication();
			String title = app.getString("ErrorDialogTitle");
			JOptionPane.showMessageDialog(app, message, title,
					JOptionPane.ERROR_MESSAGE);
			return null;
		}

		if (groovyEngine==null) {

			try {

				Method m = semClazz.getDeclaredMethod("getEngineByName",
						new Class[] { String.class });
				String engine = "Groovy";
				groovyEngine = m.invoke(sem, new Object[] { engine });
				if (groovyEngine==null) {
					showLoadingEngineError(engine);
					return null;
				}

				// Write stdout and stderr to this console.  Must wrap these in
				// PrintWriters for standard print() and println() methods to work.
				m = seClazz.getDeclaredMethod("getContext", null);
				Object context = m.invoke(groovyEngine, null);
				m = scriptContextClazz.getDeclaredMethod("setWriter",
											new Class[] { Writer.class });
				PrintWriter w = new PrintWriter(new OutputStreamWriter(System.out));
				m.invoke(context, new Object[] { w });
				m = scriptContextClazz.getDeclaredMethod("setErrorWriter",
					new Class[] { Writer.class });
				w = new PrintWriter(new OutputStreamWriter(System.err));
				m.invoke(context, new Object[] { w });

				// Import commonly-used packages.  Do this before stdout and
				// stderr redirecting so the user won't see it in their console.
				String imports = "import java.lang.*;" +
						"import java.io.*;" +
						"import java.util.*;" +
						"import java.awt.*;" +
						"import javax.swing.*;" +
						"import org.fife.rtext.*;" +
						"import org.fife.ui.rtextarea.*;" +
						"import org.fife.ui.rsyntaxtextarea.*;";
				m = seClazz.getDeclaredMethod("eval", new Class[] { String.class });
				m.invoke(groovyEngine, new Object[] { imports });

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return groovyEngine;

	}


	/**
	 * Returns the Rhino engine, lazily creating it if necessary.
	 *
	 * @return The script engine, or <code>null</code> if it cannot be created.
	 */
	private Object initJavaScriptEngine() {

		// Get the Rhino engine.
		if (jsEngine==null) {

			try {

				Method m = semClazz.getDeclaredMethod("getEngineByName",
										new Class[] { String.class });
				String engine = "JavaScript";
				jsEngine = m.invoke(sem, new Object[] { engine });
				if (jsEngine==null) {
					showLoadingEngineError(engine);
					return null;
				}

				// Write stdout and stderr to this console.  Must wrap these in
				// PrintWriters for standard print() and println() methods to work.
				m = seClazz.getDeclaredMethod("getContext", null);
				Object context = m.invoke(jsEngine, null);
				m = scriptContextClazz.getDeclaredMethod("setWriter",
												new Class[] { Writer.class });
				PrintWriter w = new PrintWriter(new OutputStreamWriter(System.out));
				m.invoke(context, new Object[] { w });
				m = scriptContextClazz.getDeclaredMethod("setErrorWriter",
						new Class[] { Writer.class });
				w = new PrintWriter(new OutputStreamWriter(System.err));
				m.invoke(context, new Object[] { w });

				// Import commonly-used packages.  Do this before stdout and
				// stderr redirecting so the user won't see it in their console.
				String imports = "importPackage(java.lang);" +
							"importPackage(java.io);" +
							"importPackage(java.util);" +
							"importPackage(java.awt);" +
							"importPackage(javax.swing);" +
							"importPackage(org.fife.rtext);" +
							"importPackage(org.fife.ui.rtextarea);" +
							"importPackage(org.fife.ui.rsyntaxtextarea);";
				m = seClazz.getDeclaredMethod("eval", new Class[] { String.class });
				m.invoke(jsEngine, new Object[] { imports });

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return jsEngine;

	}


	/**
	 * Displays an error message stating that the scripting engine failed
	 * to initialize.
	 */
	private void showErrorInitializingMessage() {
		String key = RTextUtilities.isPreJava6() ?
				"Error.Java6Required" : "Error.Initializing";
		String message = plugin.getString(key);
		RText app = (RText)getApplication();
		String title = app.getString("ErrorDialogTitle");
		JOptionPane.showMessageDialog(app, message, title,
				JOptionPane.ERROR_MESSAGE);
	}


	/**
	 * Displays an error dialog stating that an  unknown error occurred
	 * loading the scripting engine.
	 *
	 * @param engine The name of the engine we tried to load.
	 */
	private void showLoadingEngineError(String engine) {
		String message = plugin.getString("Error.LoadingEngine", engine);
		RText app = (RText)getApplication();
		String title = app.getString("ErrorDialogTitle");
		JOptionPane.showMessageDialog(app, message, title,
				JOptionPane.ERROR_MESSAGE);
	}


	/**
	 * One-time initialization for this class; pre-loads all reflection stuff.
	 */
	static {

		// The scripting API was added in Java 6.
		if (!RTextUtilities.isPreJava6()) {

			// Run everything with reflection so we compile with Java 1.4.
			try {

				semClazz = Class.forName("javax.script.ScriptEngineManager");
				// Pass the Plugin ClassLoader, since the Groovy jar won't be
				// on the application's classpath
				Constructor cons = semClazz.getConstructor(
										new Class[] { ClassLoader.class });
				sem = cons.newInstance(
						new Object[] { RunMacroAction.class.getClassLoader() });
				seClazz = Class.forName("javax.script.ScriptEngine");
				scriptContextClazz = Class.forName("javax.script.ScriptContext");
				bindingsClazz = Class.forName("javax.script.Bindings");

				Field scopeField = scriptContextClazz.getDeclaredField("ENGINE_SCOPE");             
				int scope = scopeField.getInt(scriptContextClazz);
				scopeFieldValue = new Integer(scope);

				createBindingsMethod = seClazz.getDeclaredMethod("createBindings",
										null);
				setBindingsMethod = seClazz.getDeclaredMethod("setBindings",
						new Class[] { bindingsClazz, int.class });
				bindingsPutMethod = bindingsClazz.getDeclaredMethod("put",
						new Class[] { String.class, Object.class });
				evalMethod = seClazz.getDeclaredMethod("eval",
						new Class[] { Reader.class });

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}


}