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
package org.amelia.dsl.lib.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.xtext.xbase.lib.Functions.Function2;

import com.google.common.base.Supplier;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class LazyBoolean implements Supplier<Boolean> {

	/**
	 * The left operand.
	 */
	private final Supplier<Boolean> left;

	/**
	 * The right operand.
	 */
	private final Supplier<Boolean> right;

	/**
	 * The function to eventually resolve the boolean value.
	 */
	private Function2<Supplier<Boolean>, Supplier<Boolean>, Boolean> op;

	/**
	 * Constructor for unary operations.
	 * @param operand The one argument
	 */
	public LazyBoolean(final AtomicBoolean operand) {
		this(operand, false);
	}

	/**
	 * Constructor for unary operations.
	 * @param operand The one argument
	 */
	public LazyBoolean(final Supplier<Boolean> operand) {
		this(operand, false);
	}

	/**
	 * Constructor for binary operations.
	 * @param left the left operand
	 * @param right the right operand
	 */
	public LazyBoolean(final Supplier<Boolean> left, final Supplier<Boolean> right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Constructor for binary operations.
	 * @param left the left operand
	 * @param right the right operand
	 */
	public LazyBoolean(final Supplier<Boolean> left, final Boolean right) {
		this(left, new Supplier<Boolean>() {
			@Override
			public Boolean get() {
				return right;
			}
		});
	}

	/**
	 * Constructor for binary operations.
	 * @param left the left operand
	 * @param right the right operand
	 */
	public LazyBoolean(final Boolean left, final Supplier<Boolean> right) {
		this(new Supplier<Boolean>() {
			@Override
			public Boolean get() {
				return left;
			}
		}, right);
	}

	/**
	 * Constructor for binary operations.
	 * @param left the left operand
	 * @param right the right operand
	 */
	public LazyBoolean(final Supplier<Boolean> left, final AtomicBoolean right) {
		this(left, new Supplier<Boolean>() {
			@Override
			public Boolean get() {
				return right.get();
			}
		});
	}

	/**
	 * Constructor for binary operations.
	 * @param left the left operand
	 * @param right the right operand
	 */
	public LazyBoolean(final AtomicBoolean left, final Supplier<Boolean> right) {
		this(new Supplier<Boolean>() {
			@Override
			public Boolean get() {
				return left.get();
			}
		}, right);
	}

	/**
	 * Constructor for binary operations.
	 * @param left the left operand
	 * @param right the right operand
	 */
	public LazyBoolean(final AtomicBoolean left, final AtomicBoolean right) {
		this(
			new Supplier<Boolean>() {
				@Override
				public Boolean get() {
					return left.get();
				}
			},
			new Supplier<Boolean>() {
				@Override
				public Boolean get() {
					return right.get();
				}
			}
		);
	}

	/**
	 * Constructor for binary operations.
	 * @param left the left operand
	 * @param right the right operand
	 */
	public LazyBoolean(final boolean left, final AtomicBoolean right) {
		this(new AtomicBoolean(left), right);
	}

	/**
	 * Constructor for binary operations.
	 * @param left the left operand
	 * @param right the right operand
	 */
	public LazyBoolean(final AtomicBoolean left, final boolean right) {
		this(left, new AtomicBoolean(right));
	}

	/*
	 * (non-Javadoc)
	 * @see com.google.common.base.Supplier#get()
	 */
	@Override
	public Boolean get() {
		return this.op.apply(this.left, this.right);
	}

	/**
	 * And operation.
	 * @return a lazy boolean
	 */
	public LazyBoolean and() {
		this.op = new Function2<Supplier<Boolean>, Supplier<Boolean>, Boolean>() {
			@Override
			public Boolean apply(Supplier<Boolean> p1, Supplier<Boolean> p2) {
				return p1.get() && p2.get();
			}
		};
		return this;
	}

	/**
	 * Or operation.
	 * @return a lazy boolean
	 */
	public LazyBoolean or() {
		this.op = new Function2<Supplier<Boolean>, Supplier<Boolean>, Boolean>() {
			@Override
			public Boolean apply(Supplier<Boolean> p1, Supplier<Boolean> p2) {
				return p1.get() || p2.get();
			}
		};
		return this;
	}

	/**
	 * Not operation.
	 * @return a lazy boolean
	 */
	public LazyBoolean not() {
		this.op = new Function2<Supplier<Boolean>, Supplier<Boolean>, Boolean>() {
			@Override
			public Boolean apply(Supplier<Boolean> p1, Supplier<Boolean> p2) {
				return !p1.get();
			}
		};
		return this;
	}
}
