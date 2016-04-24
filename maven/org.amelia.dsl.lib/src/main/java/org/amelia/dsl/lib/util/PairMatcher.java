package org.amelia.dsl.lib.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import com.google.common.collect.Range;

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
	
	public PairMatcher() {
	}
	
	public List<Range<Integer>> regions(final String text, final char left,
			final char right) {
		final List<Range<Integer>> regions = new ArrayList<Range<Integer>>();
		final char[] characters = text.toCharArray();
		final Stack<Character> lefties = new Stack<Character>();
		final Stack<Integer> starts = new Stack<Integer>();
		for (int i = 0; i < characters.length; i++) {
			if (!lefties.isEmpty() && characters[i] == right) {
				lefties.pop();
				regions.add(Range.closed(starts.pop(), i));
			} else if (characters[i] == left) {
				lefties.add(characters[i]);
				starts.add(i);
			}
		}
		return regions;
	}
	
	public List<Range<Integer>> cleanRedundantRegions(
			final List<Range<Integer>> regions) {
		List<Range<Integer>> redundant = redundantRegions(regions);
		List<Range<Integer>> cleaned = new ArrayList<Range<Integer>>();
		for (Range<Integer> r : regions) {
			if (!redundant.contains(r)) {
				cleaned.add(r);
			}
		}
		return cleaned;
	}

	public List<Range<Integer>> redundantRegions(
			final List<Range<Integer>> allRegions) {
		Collections.sort(allRegions, ascendent);
		List<Range<Integer>> redundantRegions = new ArrayList<Range<Integer>>();
		for (int i = 0; i < allRegions.size(); i++) {
			Range<Integer> r1 = allRegions.get(i);
			for (Range<Integer> r2 : allRegions.subList(i + 1,
					allRegions.size())) {
				if (r1.lowerEndpoint() <= r2.lowerEndpoint()
						&& r1.upperEndpoint() >= r2.upperEndpoint()) {
					redundantRegions.add(r2);
				}
			}
		}
		Collections.sort(allRegions, descendent);
		return redundantRegions;
	}

}
