/*
 * Copyright © 2015 Universidad Icesi
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
package org.amelia.dsl.lib.util;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class AuthenticationUserInfo implements UserInfo, UIKeyboardInteractive {

	private String password;
	private JTextField passwordField;
	private Container panel;
	private GridBagConstraints gbc;

	public AuthenticationUserInfo() {
		this.passwordField = (JTextField) new JPasswordField(20);

		Insets insets = new Insets(0, 0, 0, 0);
		this.gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, insets,
				0, 0);
	}

	public String getPassword() {
		return password;
	}

	public boolean promptYesNo(String str) {
		Object[] options = { "yes", "no" };
		int answer = JOptionPane.showOptionDialog(null, str, "Warning",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
				options, options[0]);
		return answer == 0;
	}

	public String getPassphrase() {
		return null;
	}

	public boolean promptPassphrase(String message) {
		return true;
	}

	public boolean promptPassword(String message) {
		Object[] ob = { passwordField };
		int result = JOptionPane.showConfirmDialog(null, ob, message,
				JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			password = passwordField.getText();
			return true;
		} else {
			return false;
		}
	}

	public void showMessage(String message) {
		JOptionPane.showMessageDialog(null, message);
	}

	public String[] promptKeyboardInteractive(String destination, String name,
			String instruction, String[] prompt, boolean[] echo) {
		
		this.panel = new JPanel();
		this.panel.setLayout(new GridBagLayout());
		
		this.gbc.weightx = 1.0;
		this.gbc.gridwidth = GridBagConstraints.REMAINDER;
		this.gbc.gridx = 0;
		this.panel.add(new JLabel(instruction), this.gbc);
		this.gbc.gridy++;

		this.gbc.gridwidth = GridBagConstraints.RELATIVE;

		JTextField[] texts = new JTextField[prompt.length];
		for (int i = 0; i < prompt.length; i++) {
			this.gbc.fill = GridBagConstraints.NONE;
			this.gbc.gridx = 0;
			this.gbc.weightx = 1;
			this.panel.add(new JLabel(prompt[i]), this.gbc);

			this.gbc.gridx = 1;
			this.gbc.fill = GridBagConstraints.HORIZONTAL;
			this.gbc.weighty = 1;
			if (echo[i]) {
				texts[i] = new JTextField(20);
			} else {
				texts[i] = new JPasswordField(20);
			}
			this.panel.add(texts[i], this.gbc);
			this.gbc.gridy++;
		}

		if (JOptionPane.showConfirmDialog(null, panel, destination + ": "
				+ name, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
			
			String[] response = new String[prompt.length];
			for (int i = 0; i < prompt.length; i++) {
				response[i] = texts[i].getText();
			}
			
			return response;
		} else {
			return null; // cancel
		}
	}
}
