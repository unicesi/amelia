/*
 * Copyright © 2017 Universidad Icesi
 * 
 * This file is part of the Amelia project.
 * 
 * The Amelia project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia project is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia project. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib.explorer;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.text.DefaultCaret;

import org.amelia.dsl.lib.descriptors.Host;

/**
 * @author Miguel Jiménez - Initial contribution and API
 * @since 0.13.13-SNAPSHOT
 */
public final class ExplorerWindow extends JFrame {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -7677958862681826509L;

	/**
	 * The pane containing the SSH log tabs.
	 */
	private final JTabbedPane tabbedPane;

	/**
	 * A mapping between hosts and tab indices.
	 */
	private final Map<Host, Integer> indices;

	/**
	 * Default constructor.
	 */
	public ExplorerWindow() {
		this.tabbedPane = new JTabbedPane();
		this.tabbedPane.setAutoscrolls(true);
		this.indices = new HashMap<>();
		this.setTitle("Amelia Explorer");
		this.setContentPane(this.tabbedPane);
		this.setSize(800, 600);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	/**
	 * Displays the SSH connection with the specified host.
	 * @param host The host to display
	 */
	public synchronized void display(Host host) {
		if (this.indices.containsKey(host)) {
			this.focus(host);
			return;
		}
		this.indices.put(host, this.indices.values().size());
		this.tabbedPane.addTab(host.identifier(), panel(host));
		this.repaint();
	}

	/**
	 * Brings focus to the specified host.
	 * @param host The host to display
	 */
	public void focus(Host host) {
		this.tabbedPane.setSelectedIndex(this.indices.get(host));
	}

	/**
	 * Removes the tab displaying the specified host.
	 * @param host The host to remove
	 */
	public void remove(Host host) {
		int index = this.indices.remove(host);
		this.tabbedPane.remove(index);
	}

	/**
	 * Removes all tabs.
	 */
	public synchronized void reset() {
		this.tabbedPane.removeAll();
		this.indices.clear();
	}

	/**
	 * Configures a panel using the Dragon Console.
	 * @param host The host to display
	 * @return a configured console panel
	 */
	private JComponent panel(Host host) {
		final ColorPane console = new ColorPane();
		console.setBackground(Color.BLACK);
		((DefaultCaret) console.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		final JScrollPane pane = new JScrollPane(console);
		pane.setAutoscrolls(true);
		// First, append everything that is already in the log
		host.ssh().outputLog().logs().forEach(new Consumer<CharSequence>() {
			@Override
			public void accept(CharSequence csq) {
				console.appendANSI(csq.toString());
			}
		});
		// Then, append new log entries
		host.ssh().outputLog().echoTo(new Appendable() {
			@Override
			public Appendable append(CharSequence csq, int start, int end)
				throws IOException {
				this.append(csq.subSequence(start, end));
				return this;
			}
			@Override
			public Appendable append(char c) throws IOException {
				this.append(String.valueOf(c));
				return this;
			}
			@Override
			public Appendable append(CharSequence csq) throws IOException {
				console.appendANSI(csq.toString());
				return this;
			}
		});
		return pane;
	}

}
