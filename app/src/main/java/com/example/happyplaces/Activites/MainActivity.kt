package com.example.happyplaces.Activites

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.Adapters.HappyPlaceAdapter
import com.example.happyplaces.Database.DatabaseHandler
import com.example.happyplaces.Models.HappyPlaceModel
import com.example.happyplaces.Utils.SwipeToDelete
import com.example.happyplaces.Utils.SwipeToEdit
import com.example.happyplaces.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding : ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        getHappyPlaceFromDB()

        setSupportActionBar(binding?.toolbarMainActivity)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        binding?.fabAddHappyPlace?.setOnClickListener{
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent,ADD_PLACE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== ADD_PLACE_REQUEST_CODE){
            if(resultCode==Activity.RESULT_OK){
                getHappyPlaceFromDB()
            }else{
                Log.d("Activity ","adding place gets back pressed")
            }
        }
    }

    private fun setUpHappyPlaceRecyclerView(happyPLaceList:ArrayList<HappyPlaceModel>){

        binding?.rvHappyPlacesList?.layoutManager = LinearLayoutManager(this)
        binding?.rvHappyPlacesList?.setHasFixedSize(true)

        val placesAdapter =  HappyPlaceAdapter(this,happyPLaceList)
        binding?.rvHappyPlacesList?.adapter =placesAdapter

        placesAdapter.setOnclickListener(object : HappyPlaceAdapter.OnClickListener{
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity,HappyPlaceSelectedActivity::class.java)

                intent.putExtra(EXTRA_PLACE_DETAILS,model)
                startActivity(intent)
            }
        })

        val editSwipeHandler = object :SwipeToEdit(this@MainActivity){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter  = binding?.rvHappyPlacesList?.adapter as HappyPlaceAdapter
                adapter.notifyEditItem(this@MainActivity,viewHolder.adapterPosition,
                    ADD_PLACE_REQUEST_CODE)
            }
        }
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)

        val deleteSwipeHandler = object :SwipeToDelete(this@MainActivity){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvHappyPlacesList?.adapter as HappyPlaceAdapter
                adapter.removeAt(viewHolder.adapterPosition)
                getHappyPlaceFromDB()
            }
        }
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)
    }

    private fun getHappyPlaceFromDB(){
        val dbHandler = DatabaseHandler(this)
        val getHappyPlaceList : ArrayList<HappyPlaceModel> = dbHandler.getHappyPlaceList()

        if(getHappyPlaceList.size> 0){
            binding?.rvHappyPlacesList?.visibility = View.VISIBLE
            binding?.tvNoRecord?.visibility=View.GONE
            setUpHappyPlaceRecyclerView(getHappyPlaceList)
        }else{
            binding?.rvHappyPlacesList?.visibility = View.GONE
            binding?.tvNoRecord?.visibility=View.VISIBLE
        }
    }
    companion object{
         var ADD_PLACE_REQUEST_CODE = 1
         var EXTRA_PLACE_DETAILS = "extra_place_details"
    }
}