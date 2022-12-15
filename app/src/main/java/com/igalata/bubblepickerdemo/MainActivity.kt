package com.igalata.bubblepickerdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.igalata.bubblepickerdemo.databinding.ActivityMainBinding


/**
 * Project : Bubble-Picker
 * Created by DongNH on 15/12/2022.
 * Email : hoaidongit5@gmail.com or hoaidongit5@dnkinno.com.
 * Phone : +84397199197.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configEvent()
    }

    private fun configEvent() {
        binding.openActivity.setOnClickListener {
            startActivity(Intent(this@MainActivity, DemoActivity::class.java))
        }

        binding.openFragment.setOnClickListener {
            val fragmentDemo = DemoFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.main_add_view, fragmentDemo, "fragmentDemo")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("fragmentDemo")
                .commit()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        } else {
            super.onBackPressed()
        }
    }
}