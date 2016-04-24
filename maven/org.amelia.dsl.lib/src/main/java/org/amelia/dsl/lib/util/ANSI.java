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

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public enum ANSI {

	BLINK(5),
	RED(31),
	GREEN(32),
	YELLOW(33),
	BLUE(34),
	MAGENTA(35),
	CYAN(36),
	GRAY(90),
	RESET(0);

	private final int code;

	ANSI(int code) {
		this.code = code;
	}

	public int code() {
		return this.code;
	}

	public String format(String text) {
		String formattedText = text;
		boolean useANSI = Boolean.valueOf(System.getProperty("amelia.color_output"));
		if (useANSI)
			formattedText = toString() + text + ANSI.RESET.toString();

		return formattedText;
	}

	public String toString() {
		return "\u001b[1;" + this.code + "m";
	}

}
