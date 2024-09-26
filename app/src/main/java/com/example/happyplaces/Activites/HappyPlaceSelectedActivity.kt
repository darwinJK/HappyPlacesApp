package com.example.happyplaces.Activites

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.Models.HappyPlaceModel
import com.example.happyplaces.databinding.ActivityHappyPlaceSelectedBinding

class HappyPlaceSelectedActivity : AppCompatActivity() {

    private var binding : ActivityHappyPlaceSelectedBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityHappyPlaceSelectedBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        var happyPlaceDetailsModel : HappyPlaceModel? = null

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            happyPlaceDetailsModel=
                intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
        }
        if(happyPlaceDetailsModel!=null){
            setSupportActionBar(binding?.toolbarPlaceDetails)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            binding?.toolbarPlaceDetails?.setNavigationOnClickListener{
                onBackPressed()
            }
        }
        binding?.ivDetailsImage?.setImageURI(Uri.parse(happyPlaceDetailsModel?.image))
        binding?.titleDetailsTv?.text = happyPlaceDetailsModel?.title
        binding?.descriptionDetailsTv?.text = happyPlaceDetailsModel?.description
        binding?.dateDetailsTv?.text = "Date : ${happyPlaceDetailsModel?.date}"
        binding?.locationDetailsTv?.text="Location : ${happyPlaceDetailsModel?.location}"

        binding?.btnViewOnMap?.setOnClickListener {
            val intent = Intent(this@HappyPlaceSelectedActivity,
                MapActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,happyPlaceDetailsModel)
            startActivity(intent)
        }
    }
}