package com.jiwoolee.memoappchallenge

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.jiwoolee.memoappchallenge.room.Memo
import com.jiwoolee.memoappchallenge.room.MemoDB
import kotlinx.android.synthetic.main.activity_add.*
import kotlinx.android.synthetic.main.item_image.view.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/*
메모 추가/편집 화면
 */
class AddActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {
    private var memoDb: MemoDB? = null
    private var newMemo = Memo()
    private var memoImageLIst: ArrayList<String> = ArrayList()

    private var isCamera: Boolean = false
    private var tempFile: File? = null
    private var mCurrentPhotoPath: String = ""
    private lateinit var alertDialog : AlertDialog
//    private var handler : DisplayHandler? = null
    private lateinit var imagesContainer : ViewGroup

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var mContext: Context
        private const val PICK_FROM_ALBUM = 1
        private const val PICK_FROM_CAMERA = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        mContext = this

        requestPermissions() //권한요청
        imagesContainer = findViewById(R.id.add_images_container)
        memoDb = MemoDB.getInstance(this)

        val bundle = intent.extras //DetailActivity에서 편집 클릭시 (DetailActivity -> AddActivity)
        if (bundle != null) {
            setEditModeVisible(true)

            newMemo = bundle.getSerializable(("memo")) as Memo
            setDataToForm(newMemo)
        }

        ib_add_ok.setOnClickListener(this)
        ib_add_cancel.setOnClickListener(this)
        ib_add_edit.setOnClickListener(this)
        btn_add_Images.setOnClickListener(this)
        btn_add_Images.setOnLongClickListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) { //중간에 취소시
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show()
            if (tempFile != null) {
                if (tempFile!!.exists()) {
                    if (tempFile!!.delete()) {
                        tempFile = null
                    }
                }
            }
            return
        }

        when (requestCode) {
            PICK_FROM_ALBUM -> drawImageToViewAndSave(data!!.data!!)
            PICK_FROM_CAMERA -> drawImageToViewAndSave(Uri.fromFile(tempFile))
        }
    }

    private fun drawImageToViewAndSave(uri: Uri) {
        val imageHolder = LayoutInflater.from(this).inflate(R.layout.item_image, null)
        val thumbnail = imageHolder.iv_images
        Glide.with(this)
            .load(uri)
            .fitCenter()
            .into(thumbnail)

        imagesContainer.addView(imageHolder)
        thumbnail.layoutParams = FrameLayout.LayoutParams(300, 300)

        memoImageLIst.add(uri.toString())
        Log.d("ljwLog", "memoImageLIst.size : " + memoImageLIst.size.toString())
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun setDataToForm(newMemo : Memo){
        et_add_title.text = Editable.Factory.getInstance().newEditable(newMemo.memoTitle)
        et_add_content.text = Editable.Factory.getInstance().newEditable(newMemo.memoContent)

        val images : List<String>?  = newMemo.memoImages
        memoImageLIst = arrayListOf()
        if (images != null) {
            memoImageLIst.addAll(images)
            if(memoImageLIst.isNotEmpty() && memoImageLIst[0] == ""){
                memoImageLIst.removeAt(0)
            }
        }

        if (images != null && images[0] != "") {
            for (image in images) {
                val imageHolder = LayoutInflater.from(this).inflate(R.layout.item_image, null)
                val thumbnail = imageHolder.iv_images

                Glide.with(this)
                    .load(image)
                    .fitCenter()
                    .into(thumbnail)

                imagesContainer.addView(imageHolder)
                thumbnail.layoutParams = FrameLayout.LayoutParams(300, 300)
            }
        }
    }

    //listener
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ib_add_ok -> { //추가
                Thread(Runnable {
                    storeItemToMemo()
                    memoDb?.memoDao()?.insert(newMemo) //INSERT
                }).start()

                intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)

                finish()
            }

            R.id.ib_add_edit -> { //편집
                Thread(Runnable {
                    storeItemToMemo()
                    memoDb?.memoDao()?.update(newMemo) //UPDATE
                }).start()

                intent = Intent(applicationContext, AddActivity::class.java)
                val bundle = Bundle()
                bundle.putSerializable("memo", newMemo)
                intent.putExtras(bundle)

                setResult(Activity.RESULT_OK, intent)
                finish()

                setEditModeVisible(false)
            }

            R.id.ib_add_cancel -> finish()
            R.id.btn_add_Images -> selectRegisterImagesType() //카메라or앨범orURL 선택
        }
    }

    private fun storeItemToMemo() {
        newMemo.memoTitle = et_add_title.text.toString()
        newMemo.memoContent = et_add_content.text.toString()

        if (memoImageLIst.isEmpty() && newMemo.memoImages.isEmpty()) {
            newMemo.memoImages = listOf("")
        } else if((memoImageLIst[0] == "")){
            memoImageLIst.removeAt(0) //리스트에서 삭제
        } else {
            newMemo.memoImages = memoImageLIst
        }
        Log.d("ljwLog", memoImageLIst.toString())
    }

    private fun setEditModeVisible(boolean: Boolean){
        if(boolean){
            ib_add_edit.visibility = View.VISIBLE
            ib_add_ok.visibility = View.GONE
        }else{
            ib_add_edit.visibility = View.GONE
            ib_add_ok.visibility = View.VISIBLE
        }
    }

    override fun onLongClick(v: View?): Boolean { //롱클릭시 이미지 삭제
        when (v?.id) {
            R.id.btn_add_Images -> {
                Toast.makeText(mContext, "롱클릭", Toast.LENGTH_LONG).show()
                memoImageLIst.removeAt(0) //리스트에서 삭제
            }
        }
        return true
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //카메라or앨범orURL 선택
    private fun selectRegisterImagesType() {
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("사진 추가")

        builder.setItems(R.array.TYPE) { _, pos ->
            when (pos) {
                0 -> imageFromAlbum() //앨범
                1 -> imageFromCamera() //카메라
//                2 -> getUrlLink() //URL링크
            }
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    //앨범에서 이미지 가져오기
    private fun imageFromAlbum() {
        isCamera = false

        intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        startActivityForResult(intent, PICK_FROM_ALBUM)
    }

    //카메라에서 이미지 가져오기
    private fun imageFromCamera() {
        isCamera = true
        intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {
            tempFile = createImageFile()
        } catch (e: IOException) {
            Toast.makeText(this, "이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            finish()
            e.printStackTrace()
        }

        if (tempFile != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val photoUri = FileProvider.getUriForFile(this, "com.jiwoolee.memoappchallenge.fileprovider", tempFile!!)/////
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, PICK_FROM_CAMERA)
            } else {
                val photoUri = Uri.fromFile(tempFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, PICK_FROM_CAMERA)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val filePrefix = "img_" + timeStamp + "_";
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(filePrefix, ".jpg", storageDir)
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    //URL링크를 통해 이미지 가져오기
//    private fun getUrlLink(){
//        val et = EditText(mContext)
//        et.setText(R.string.test_url_link)
//
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("외부 이미지 주소(URL)")
//            .setMessage("URL을 입력하세요")
//            .setView(et)
//            .setPositiveButton("확인") { _, _ ->
//                val value = et.text.toString()
//                imageFromUrlLink(value)
//            }
//            .setNegativeButton("취소") { _, _ -> }
//
//        alertDialog = builder.create()
//        alertDialog.show()
//    }

//    private fun imageFromUrlLink(url : String){
//        isCamera = false
//        handler = DisplayHandler()
//
//        var bitmap : Bitmap? = null
//        var isOk : Boolean = true
//
//        val getThread = Thread(Runnable {
//            try {
//                val conn = URL(url).openConnection()
//                conn.doInput = true
//                conn.connect()
//                val inputStream = conn.getInputStream()
//                bitmap = makeBitmap(inputStream)
//            }catch (e : IOException) {
//                //UnknownHostException, FileNotFoundException
//                Log.d("ljwLog", "AddActivity_UnknownHostException_err : $e")
//
//                val msg = Message()
//                msg.what = -1
//                msg.obj = "URL주소 혹은 네트워크 상태를 확인해주세요."
//                handler?.sendMessage(msg) //Toast
//
//                alertDialog.dismiss()
//                isOk = false
//            }
//        })
//        getThread.start()
//
//        try {
//            getThread.join() //Thread안에서 선행 작업 실행 완료 후 다음 작업 수행(순차적)
//        } catch (e: java.lang.Exception) {
//            Log.d("ljwLog", "AddActivity_getThread.join()_err : $e")
//        }
//
////        if(isOk){
////            setImageToButtonAndSave(bitmap!!)
////        }
//
//    }
//    inner class DisplayHandler : Handler(){
//        override fun handleMessage(msg: Message?) {
//            super.handleMessage(msg)
//            if(msg?.what == -1) {
//                Toast.makeText(mContext, msg.obj.toString(), Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //권한요청
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 6.0 마쉬멜로우 이상일 경우에는 권한 체크 후 권한 요청
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d("ljwLog", "권한 설정 완료")
            } else {
                Log.d("ljwLog", "권한 설정 요청")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("ljwLog", "onRequestPermissionsResult")
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            Log.d("ljwLog", "Permission: " + permissions[0] + "was " + grantResults[0])
        }
    }

    override fun onDestroy() {
        MemoDB.destroyInstance()
        memoDb = null
        super.onDestroy()
    }
}