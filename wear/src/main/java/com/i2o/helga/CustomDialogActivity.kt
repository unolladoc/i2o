package com.i2o.helga

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.i2o.helga.databinding.ActivityCustomDialogBinding

class CustomDialogActivity : Activity() {

    private lateinit var binding: ActivityCustomDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCustomDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras

        if (bundle != null) {
            binding.text.text = bundle.getString("result")
        }

        binding.button.setOnClickListener {
            finish()
        }

    }
}