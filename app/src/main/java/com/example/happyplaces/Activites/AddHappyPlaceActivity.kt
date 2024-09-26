package com.example.happyplaces.Activites

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import android.icu.util.Calendar
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.media.ExifInterface
import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.happyplaces.Database.DatabaseHandler
import com.example.happyplaces.Models.HappyPlaceModel
import com.example.happyplaces.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class AddHappyPlaceActivity : AppCompatActivity() , View.OnClickListener{

    private var binding : ActivityAddHappyPlaceBinding? = null
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToStorageUri : Uri? = null
    private var mLatitude: Double =0.0
    private var mLongitude : Double = 0.0

    private var mHappyPlacesDetails : HappyPlaceModel? = null

    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlacesDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
        }

        dateSetListener = DatePickerDialog.OnDateSetListener{
            view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            updateDateInView()
        }

        if(mHappyPlacesDetails!=null){
            supportActionBar?.title = "Edit Happy Place"

            binding?.titleEt?.setText(mHappyPlacesDetails!!.title)
            binding?.descriptionEt?.setText(mHappyPlacesDetails!!.description)
            binding?.dateEt?.setText(mHappyPlacesDetails!!.date)
            binding?.locationEt?.setText(mHappyPlacesDetails!!.location)
            mLatitude = mHappyPlacesDetails!!.latitude
            mLongitude = mHappyPlacesDetails!!.longitude

            binding?.imageOfPlace?.setImageURI(Uri.parse(mHappyPlacesDetails?.image))
            saveImageToStorageUri = Uri.parse(mHappyPlacesDetails?.image)
             binding?.savePlaceBtn?.setText("Update")

        }

        binding?.dateEt?.setOnClickListener(this)

        binding?.addImageTv?.setOnClickListener(this)
        updateDateInView()
        binding?.savePlaceBtn?.setOnClickListener(this)
        binding?.locationEt?.setOnClickListener(this)
        binding?.btnSelectCurrentLocation?.setOnClickListener(this)
    }


    override fun onClick(view: View?) {
       when(view!!.id){
          R.id.date_et -> {
               DatePickerDialog(this@AddHappyPlaceActivity,dateSetListener,
                   cal.get(Calendar.YEAR),
                   cal.get(Calendar.MONTH),
                   cal.get(Calendar.DAY_OF_MONTH)).show()
          }
           R.id.add_image_tv ->{
               val pictureDialog = AlertDialog.Builder(this)
               pictureDialog.setTitle("Select Action")
               val pickerDialogItems = arrayOf("Select photo from Gallery","Capture photo from camera")
               pictureDialog.setItems(pickerDialogItems){
                       dialog, which->
                   when(which){
                       0-> choosePhotoFromGallery()
                       1 -> takePhotoFromCamera()
                   }
               }
               pictureDialog.show()
           }
           R.id.location_et->{
              try{
                  val fields = listOf(Place.Field.ID,Place.Field.NAME,
                      Place.Field.LAT_LNG,Place.Field.ADDRESS)
                  val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields)
                      .build(this@AddHappyPlaceActivity)
                  startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)

              }catch(e:Exception){
                  e.printStackTrace()
              }
           }

           R.id.btn_select_current_location->{
               if(!isLocationEnabled()) {
                   Toast.makeText(
                       this@AddHappyPlaceActivity,
                       "your location is turned off",
                       Toast.LENGTH_SHORT
                   ).show()
                   val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                   startActivity(intent)
               }else{
                   Dexter.withActivity(this).withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                       Manifest.permission.ACCESS_COARSE_LOCATION).withListener(object :MultiplePermissionsListener{
                       override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                           if(report!!.areAllPermissionsGranted()){
                               requestLocationData()
                           }
                       }
                       override fun onPermissionRationaleShouldBeShown(
                           p0: MutableList<PermissionRequest>?,
                           p1: PermissionToken?
                       ) {
                           showRationalDialogForPermissions()
                       }
                   }).onSameThread().check()
               }

           }

           R.id.save_place_btn-> {
                when{
                    binding?.titleEt?.text.isNullOrEmpty() ->
                        Toast.makeText(this@AddHappyPlaceActivity,"Please enter the title",Toast.LENGTH_SHORT).show()

                    binding?.descriptionEt?.text.isNullOrEmpty() ->
                        Toast.makeText(this@AddHappyPlaceActivity,"Please enter the description",Toast.LENGTH_SHORT).show()

                    saveImageToStorageUri==null ->
                        Toast.makeText(this@AddHappyPlaceActivity,"Please select an image",Toast.LENGTH_SHORT).show()


                    else->{
                        var happyPlaceModel = HappyPlaceModel(
                            if(mHappyPlacesDetails == null) 0 else mHappyPlacesDetails!!.id,
                            binding?.titleEt?.text.toString(),saveImageToStorageUri.toString(),
                            binding?.descriptionEt?.text.toString(),binding?.dateEt?.text.toString(),
                            binding?.locationEt?.text.toString(),mLatitude,mLongitude
                        )
                        var dbHandler = DatabaseHandler(this)
                        if(mHappyPlacesDetails==null) {
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                                Toast.makeText(
                                    this@AddHappyPlaceActivity,
                                    "inserted successfully: $addHappyPlace",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                                Toast.makeText(
                                    this@AddHappyPlaceActivity,
                                    "updated successfully: $updateHappyPlace",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
           }
       }
    }

    private fun isLocationEnabled():Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
        var mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority=LocationRequest.QUALITY_BALANCED_POWER_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallBack, Looper.myLooper())

    }

    private var mLocationCallBack = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val mLastLocation : Location? = locationResult.lastLocation
            mLatitude = mLastLocation!!.latitude
            mLongitude = mLastLocation.longitude
            try {
                val geocoder = Geocoder(this@AddHappyPlaceActivity, Locale.ENGLISH)
                val address = geocoder.getFromLocation(mLatitude,mLongitude,1)
                binding?.locationEt?.setText("${address!!.get(0).getAddressLine(0)}")
            }catch (e:Exception){
                e.printStackTrace()
            }

        }
    }

    private fun takePhotoFromCamera(){
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(cameraIntent, CAMERA)
                }
            }

            override fun onPermissionRationaleShouldBeShown(permission: MutableList<PermissionRequest>?, token: PermissionToken?) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun choosePhotoFromGallery() {
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
               if(report!!.areAllPermissionsGranted()){
                   val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                   startActivityForResult(galleryIntent, GALLERY)
               }
            }

            override fun onPermissionRationaleShouldBeShown(permission: MutableList<PermissionRequest>?, token: PermissionToken?) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned of the permission require for this feature. " +
                "It can be enabled under application settings")
            .setPositiveButton("Go to Settings")
            { _,_ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e:ActivityNotFoundException){
                    e.printStackTrace()
                }

            }
            .setNegativeButton("Cancel"){
                dialog,_->
                dialog.dismiss()
            }.show()
    }

    private fun updateDateInView(){
        val myFormat = "dd MMM yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding?.dateEt?.setText(sdf.format(cal.time).toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("test on activity", "onActivityResult called with Activity.RESULT_OK : ${Activity.RESULT_OK} , requestCode: $requestCode and resultCode: $resultCode")
        if(resultCode==Activity.RESULT_OK){
            if(requestCode== GALLERY){
                if(data!=null){
                    val contentUri = data.data
                    try{
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentUri)
                        saveImageToStorageUri = saveIMageToInternalStorage(selectedImageBitmap)
                        val bitmap = loadImageWithCorrectOrientation(saveImageToStorageUri.toString())
                        Log.d("save image url ","pat : $saveImageToStorageUri")
                        binding?.imageOfPlace?.setImageBitmap(bitmap)
                    }catch (e: Exception){
                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity,"failed",Toast.LENGTH_SHORT).show()
                        Log.d("failed to loa image","$e")
                    }
                }
            }else if(requestCode== CAMERA){
                val thumbnail : Bitmap =data!!.extras!!.get("data") as Bitmap
               saveImageToStorageUri = saveIMageToInternalStorage(thumbnail)
                Log.d("save image url ","pat : $saveImageToStorageUri")
                binding?.imageOfPlace?.setImageBitmap(thumbnail)
            }
            else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
                val place : Place = Autocomplete.getPlaceFromIntent(data!!)
                binding?.locationEt?.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude


            }
        }
    }

    private fun loadImageWithCorrectOrientation(imagePath: String)  : Bitmap{
        var bitmap = BitmapFactory.decodeFile(imagePath)

        try {
            // Read EXIF data
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            // Adjust the bitmap orientation based on the EXIF orientation
            bitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bitmap
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun saveIMageToInternalStorage(bitmap : Bitmap) : Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        try{
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch(e:IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    companion object{
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }
}
// onActivityResult called with Activity.RESULT_OK : -1 , requestCode: 1 and resultCode: -1
// onActivityResult called with Activity.RESULT_OK : -1 , requestCode: 3 and resultCode: 2