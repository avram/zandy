package com.gimranov.zandy.app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.gimranov.zandy.app.data.Database
import com.gimranov.zandy.app.data.Item
import kotlinx.android.synthetic.main.activity_drawer_navigation.*
import kotlinx.android.synthetic.main.app_bar_drawer_navigation.*
import kotlinx.android.synthetic.main.content_drawer_navigation.*
import kotlinx.android.synthetic.main.nav_header_drawer_navigation.*

class DrawerNavigationActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_navigation)
        setSupportActionBar(toolbar)

        val database = Database(this)

        val itemListingRule = AllItems

        val itemAdapter = ItemAdapter(database, itemListingRule, { item: Item, itemAction: ItemAction ->
            run {
                when (itemAction) {
                    ItemAction.EDIT -> {
                        val i = Intent(baseContext, ItemDataActivity::class.java)
                        i.putExtra("com.gimranov.zandy.app.itemKey", item.key)
                        i.putExtra("com.gimranov.zandy.app.itemDbId", item.dbId)
                        startActivity(i)
                    }
                    ItemAction.ORGANIZE -> {
                        val i = Intent(baseContext, CollectionMembershipActivity::class.java)
                        i.putExtra("com.gimranov.zandy.app.itemKey", item.key)
                        startActivity(i)
                    }
                    ItemAction.VIEW -> TODO()
                }
            }
        })

        navigation_drawer_content_recycler.adapter = itemAdapter
        navigation_drawer_content_recycler.setHasFixedSize(true)
        navigation_drawer_content_recycler.layoutManager = LinearLayoutManager(this)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.drawer_navigation, menu)

        // TODO Actually provide a value that is meaningful
        nav_header_user_name.text = "Your Name Here"

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(baseContext, SettingsActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
