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
package com.gimranov.zandy.app.data;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Need to work out DB saving and loading soon
 * @author ajlyon
 *
 */
public class Creator {
	private String lastName;
	private String firstName;
	private String name;
	private String creatorType;
	private boolean singleField;
	
	private int dbId;
	
	public static final String TAG = "com.gimranov.zandy.app.data.Creator";
	
	/**
	 * A Creator, given type, a single string, and a boolean mode.
	 * @param mCreatorType		A valid creator type
	 * @param mName				Name. If not in single-field-mode, last word will be lastName
	 * @param mSingleField		If true, name won't be parsed into first and last
	 */
	public Creator(String mCreatorType, String mName, boolean mSingleField) {
		creatorType = mCreatorType;
		singleField = mSingleField;
		name = mName;
		if (singleField) return;
		
		String[] pieces = name.split(" ");
		if (pieces.length > 1) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < pieces.length - 1; i++) {
				sb.append(pieces[i]);
			}
			firstName = sb.toString();
			lastName = pieces[pieces.length - 1];
		}
	}
	
	/**
	 * A creator given two name parts. They'll be joined for the name field.
	 * @param type
	 * @param first
	 * @param last
	 */
	public Creator(String type, String first, String last) {
		creatorType = type;
		firstName = first;
		lastName = last;
		singleField = false;
		name = first + " " + last;
	}
	
	public String getCreatorType() {
		return creatorType;
	}

	public void setCreatorType(String creatorType) {
		this.creatorType = creatorType;
	}

	public int getDbId() {
		return dbId;
	}

	public void setDbId(int dbId) {
		this.dbId = dbId;
	}

	public String getLastName() {
		return lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getName() {
		return name;
	}

	public boolean isSingleField() {
		return singleField;
	}
	
	public JSONObject toJSON() throws JSONException {
		if (singleField)
			return new JSONObject().accumulate("name", name)
						.accumulate("creatorType", creatorType);
		return new JSONObject().accumulate("firstName", firstName)
						.accumulate("lastName", lastName)
						.accumulate("creatorType", creatorType);
	}
	
}
