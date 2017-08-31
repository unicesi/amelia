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

import org.eclipse.xtext.xbase.lib.Inline;
import org.eclipse.xtext.xbase.lib.Pure;

import com.google.common.base.Supplier;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class BooleanExtensions {

	@Pure
	@Inline(value = "new $3($1, $2).and()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_and(final AtomicBoolean left, final AtomicBoolean right) {
		return new LazyBoolean(left, right).and();
	}

	@Pure
	@Inline(value = "new $3($1, $2).and()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_and(final Boolean left, final AtomicBoolean right) {
		return new LazyBoolean(left, right).and();
	}

	@Pure
	@Inline(value = "new $3($1, $2).and()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_and(final AtomicBoolean left, final Boolean right) {
		return new LazyBoolean(left, right).and();
	}

	@Pure
	@Inline(value = "new $3($1, $2).and()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_and(final Supplier<Boolean> left, final Boolean right) {
		return new LazyBoolean(left, right).and();
	}

	@Pure
	@Inline(value = "new $3($1, $2).and()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_and(final Boolean left, final Supplier<Boolean> right) {
		return new LazyBoolean(left, right).and();
	}

	@Pure
	@Inline(value = "new $3($1, $2).and()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_and(final Supplier<Boolean> left, final AtomicBoolean right) {
		return new LazyBoolean(left, right).and();
	}

	@Pure
	@Inline(value = "new $3($1, $2).and()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_and(final AtomicBoolean left, final Supplier<Boolean> right) {
		return new LazyBoolean(left, right).and();
	}

	@Pure
	@Inline(value = "new $3($1, $2).and()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_and(final Supplier<Boolean> left, final Supplier<Boolean> right) {
		return new LazyBoolean(left, right).and();
	}

	@Pure
	@Inline(value = "new $3($1, $2).or()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_or(final AtomicBoolean left, final AtomicBoolean right) {
		return new LazyBoolean(left, right).or();
	}

	@Pure
	@Inline(value = "new $3($1, $2).or()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_or(final Boolean left, final AtomicBoolean right) {
		return new LazyBoolean(left, right).or();
	}

	@Pure
	@Inline(value = "new $3($1, $2).or()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_or(final AtomicBoolean left, final Boolean right) {
		return new LazyBoolean(left, right).or();
	}

	@Pure
	@Inline(value = "new $3($1, $2).or()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_or(final Supplier<Boolean> left, final Boolean right) {
		return new LazyBoolean(left, right).or();
	}

	@Pure
	@Inline(value = "new $3($1, $2).or()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_or(final Boolean left, final Supplier<Boolean> right) {
		return new LazyBoolean(left, right).or();
	}

	@Pure
	@Inline(value = "new $3($1, $2).or()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_or(final Supplier<Boolean> left, final AtomicBoolean right) {
		return new LazyBoolean(left, right).or();
	}

	@Pure
	@Inline(value = "new $3($1, $2).or()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_or(final AtomicBoolean left, final Supplier<Boolean> right) {
		return new LazyBoolean(left, right).or();
	}

	@Pure
	@Inline(value = "new $3($1, $2).or()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_or(final Supplier<Boolean> left, final Supplier<Boolean> right) {
		return new LazyBoolean(left, right).or();
	}

	@Pure
	@Inline(value = "new $2($1).not()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_not(final AtomicBoolean operand) {
		return new LazyBoolean(operand).not();
	}

	@Pure
	@Inline(value = "new $2($1).not()", imported = LazyBoolean.class)
	public static Supplier<Boolean> operator_not(final Supplier<Boolean> operand) {
		return new LazyBoolean(operand).not();
	}

}
