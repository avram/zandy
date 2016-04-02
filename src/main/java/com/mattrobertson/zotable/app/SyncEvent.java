/*******************************************************************************
 * This file is part of Zotable.
 *
 * Zotable is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zotable is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Zotable.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.mattrobertson.zotable.app;

public class SyncEvent {
    private static final String TAG = SyncEvent.class.getCanonicalName();

    public static final int COMPLETE_CODE = 1;

    public static final SyncEvent COMPLETE = new SyncEvent(COMPLETE_CODE);

    private int status;

    public SyncEvent(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
