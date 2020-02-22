package com.jiwoolee.memoappchallenge.adapter

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jiwoolee.memoappchallenge.R
import com.jiwoolee.memoappchallenge.room.Memo
import kotlinx.android.synthetic.main.item_memo.view.*

class RecyclerviewAdapter(val context: Context, val memos: ArrayList<Memo>, val listener : OnItemClick) : RecyclerView.Adapter<RecyclerviewAdapter.CustomViewHolder>() {
    class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemTitle : TextView = view.tv_item_title
        val itemContent : TextView = view.tv_item_content
        val itemThumnail : ImageView = view.iv_item_thumbnail
        val context : Context = view.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_memo, parent, false)
        return CustomViewHolder(view)
    }

    override fun getItemCount(): Int {
        return memos.size
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.itemTitle.text = memos[position].memoTitle
        holder.itemContent.text = memos[position].memoContent
        val thumbnailImage = memos[position].memoImages[0]

        if (thumbnailImage != "") {
            val image: ByteArray = Base64.decode(thumbnailImage, Base64.DEFAULT)
            Glide.with(holder.context)
                .load(image) //첫번째 사진을 썸네일로
                .fitCenter()
                .into(holder.itemThumnail)
        }

        holder.itemView.setOnClickListener {
            listener.onClick(memos[position])
        }
    }
}