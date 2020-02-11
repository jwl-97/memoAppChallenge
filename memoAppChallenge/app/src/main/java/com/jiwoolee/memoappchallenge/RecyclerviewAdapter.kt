package com.jiwoolee.memoappchallenge

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jiwoolee.memoappchallenge.room.Memo
import kotlinx.android.synthetic.main.item_cat.view.*

class RecyclerviewAdapter(val context: Context, val memos: List<Memo>) :
    RecyclerView.Adapter<RecyclerviewAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_cat, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int {
        return memos.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(memos[position])
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(memo: Memo) {
            itemView.tv_item_title.text = memo.memoTitle
            itemView.tv_item_content.text = memo.memoContent

            Glide.with(itemView.context)
                .load(memo.memoImages?.get(0))
                .fitCenter()
                .into(itemView.iv_item_thumbnail)
        }
    }
}