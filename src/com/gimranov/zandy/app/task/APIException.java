/*******************************************************************************
 * This file is part of Zandy.
 * 
 * Zandy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Zandy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.gimranov.zandy.app.task;

public class APIException extends Exception {

	/**
	 * Don't know what this is for.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Exception types
	 */
	public static final int INVALID_METHOD = 10;
	public static final int INVALID_UUID = 11;
	public static final int INVALID_URI = 12;
	public static final int HTTP_ERROR = 13;

	public APIRequest request;
	public int type;
	
	public APIException(int type, String message, APIRequest request) {
		super(message);
		this.request = request;
	}
	
	public APIException(int type, APIRequest request) {
		super();
		this.request = request;
	}
}
