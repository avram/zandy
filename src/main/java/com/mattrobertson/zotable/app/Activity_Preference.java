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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mattrobertson.zotable.app.data.Database;
import com.mattrobertson.zotable.app.data.ItemCollection;
import com.mattrobertson.zotable.app.data.TagAdapter;

public class Activity_Preference extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new ZotPrefFragment())
                .commit();
    }

    public static class ZotPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

            Button btnClearDb = new Button(getActivity().getApplicationContext());
            btnClearDb.setText("Clear local database");
            v.addView(btnClearDb);

            btnClearDb.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(),R.style.AppTheme));
                    builder.setTitle(getResources().getString(R.string.settings_reset_database_warning))
                            .setPositiveButton(getResources().getString(R.string.menu_delete), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Database db = new Database(getActivity().getBaseContext());
                                    db.resetAllData();
                                    getActivity().finish();
                                }
                            }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // do nothing
                        }
                    });
                    AlertDialog dialog = builder.show();

                    int textViewId = dialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
                    TextView tv = (TextView) dialog.findViewById(textViewId);
                    tv.setTextColor(getResources().getColor(R.color.white));
                }
            });

            return v;
        }
    }
}