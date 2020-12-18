/* -----------------------------------------------------------------------------
 * Util-Lib - Miscellaneous utility functions.
 * Copyright (C) 2020  Sebastian Krieter
 * 
 * This file is part of Util-Lib.
 * 
 * Util-Lib is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * Util-Lib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Util-Lib.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * See <https://github.com/skrieter/utils> for further information.
 * -----------------------------------------------------------------------------
 */
package org.spldev.util.job;

/**
 * Control object for {@link MonitorableSupplier} and
 * {@link MonitorableFunction}. Can be used to cancel a function's execution and
 * to get the progress of the given function.
 *
 * @author Sebastian Krieter
 */
public interface Monitor {

	int getTotalWork();

	int getRemainingWork();

	int getWorkDone();

	double getRelativeWorkDone();

	String getTaskName();

	void cancel();

	boolean isCanceled();

}
