/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia library.
 * 
 * The Amelia library is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import com.google.common.collect.Range;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class PairMatcher {

	private static Comparator<Range<Integer>> ascendent = new Comparator<Range<Integer>>() {
		public int compare(Range<Integer> r1, Range<Integer> r2) {
			if (r1.lowerEndpoint() < r2.lowerEndpoint()) {
				return -1;
			} else if (r1.lowerEndpoint() > r2.lowerEndpoint()) {
				return 1;
			}
			return 0;
		}
	};

	private static Comparator<Range<Integer>> descendent = new Comparator<Range<Integer>>() {
		public int compare(Range<Integer> r1, Range<Integer> r2) {
			if (r1.upperEndpoint() > r2.upperEndpoint()) {
				return -1;
			} else if (r1.upperEndpoint() < r2.upperEndpoint()) {
				return 1;
			}
			return 0;
		}
	};

	private final String text;

	private List<Range<Integer>> regions;

	private char leftDelimiter;

	private char rightDelimiter;

	public PairMatcher(final String text, final char leftDelimiter,
			final char rightDelimiter) {
		this.text = text;
		this.leftDelimiter = leftDelimiter;
		this.rightDelimiter = rightDelimiter;
	}

	public void removeRedundantRegions() {
		if (this.regions == null)
			calculateRegions();
		List<Range<Integer>> redundant = redundantRegions();
		List<Range<Integer>> cleaned = new ArrayList<Range<Integer>>();
		for (Range<Integer> r : this.regions) {
			if (!redundant.contains(r)) {
				cleaned.add(r);
			}
		}
		this.regions = cleaned;
	}

	public List<Range<Integer>> redundantRegions() {
		if (this.regions == null)
			calculateRegions();
		Collections.sort(this.regions, ascendent);
		List<Range<Integer>> redundantRegions = new ArrayList<Range<Integer>>();
		for (int i = 0; i < this.regions.size(); i++) {
			Range<Integer> r1 = this.regions.get(i);
			for (Range<Integer> r2 : this.regions.subList(i + 1,
					this.regions.size())) {
				if (r1.lowerEndpoint() <= r2.lowerEndpoint()
						&& r1.upperEndpoint() >= r2.upperEndpoint()) {
					redundantRegions.add(r2);
				}
			}
		}
		Collections.sort(this.regions, descendent);
		return redundantRegions;
	}

	private void calculateRegions() {
		final List<Range<Integer>> regions = new ArrayList<Range<Integer>>();
		final char[] characters = this.text.toCharArray();
		final Stack<Character> lefties = new Stack<Character>();
		final Stack<Integer> starts = new Stack<Integer>();
		for (int i = 0; i < characters.length; i++) {
			if (!lefties.isEmpty() && characters[i] == this.rightDelimiter) {
				lefties.pop();
				regions.add(Range.closed(starts.pop(), i));
			} else if (characters[i] == this.leftDelimiter) {
				lefties.add(characters[i]);
				starts.add(i);
			}
		}
		this.regions = regions;
	}

	public List<Range<Integer>> getRegions() {
		if (this.regions == null)
			calculateRegions();
		return this.regions;
	}

}
