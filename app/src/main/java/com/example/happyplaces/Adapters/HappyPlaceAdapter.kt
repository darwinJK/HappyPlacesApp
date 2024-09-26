package com.example.happyplaces.Adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.Activites.AddHappyPlaceActivity
import com.example.happyplaces.Activites.MainActivity
import com.example.happyplaces.Database.DatabaseHandler
import com.example.happyplaces.Models.HappyPlaceModel
import com.example.happyplaces.databinding.ItemHappyPlaceBinding

open class HappyPlaceAdapter(
    private val context: Context,private var list : ArrayList<HappyPlaceModel>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        private var onClickListener : OnClickListener? = null

        inner class ViewHolder(val binding:ItemHappyPlaceBinding):
            RecyclerView.ViewHolder(binding.root){

                fun bindPlaces(happyPlaceModel:HappyPlaceModel){
                    binding.ivRoundImage.setImageURI(Uri.parse(happyPlaceModel.image))
                    binding.titleDisplayTv.text = happyPlaceModel.title
                    binding.descriptionDisplayTv.text = happyPlaceModel.description
                }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
       return ViewHolder(ItemHappyPlaceBinding.inflate(
           LayoutInflater.from(parent.context),parent,false))
    }

    fun setOnclickListener(onClickListener:OnClickListener){
        this.onClickListener=onClickListener
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if(holder is ViewHolder){
            holder.bindPlaces(model)
            holder.itemView.setOnClickListener{
                if (onClickListener!=null){
                    onClickListener!!.onClick(position,model)
                }
            }
        }
    }

    fun notifyEditItem(activity: Activity, position:Int, requestCode:Int){
        val intent = Intent(context,AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,list[position])
        activity.startActivityForResult(intent,requestCode)
        notifyItemChanged(position)
    }
    fun removeAt( position:Int){
        val dbHandler = DatabaseHandler(context)
        val isDelete = dbHandler.deleteHappyPlace(list[position])
        if(isDelete> 0){
            list.removeAt(position)
            notifyItemRemoved(position)

        }
    }

     interface OnClickListener{
        fun onClick(position:Int,model: HappyPlaceModel)
    }

}