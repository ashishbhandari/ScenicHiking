package com.scenichiking.features.home

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.geojson.Feature
import com.scenichiking.R
import com.scenichiking.core.extension.inflate
import com.scenichiking.core.util.PROPERTY_FAVOURITE
import com.scenichiking.core.util.PROPERTY_TITLE
import kotlinx.android.synthetic.main.sheet_list_item.view.*
import javax.inject.Inject
import kotlin.properties.Delegates

class BottomSheetAdapter @Inject constructor() :
    RecyclerView.Adapter<BottomSheetAdapter.ViewHolder>() {

    internal var collection: List<Feature> by Delegates.observable(emptyList()) { _, _, _ ->
        notifyDataSetChanged()
    }

    internal var clickListener: (Feature, Int) -> Unit = { _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.sheet_list_item))
    }

    override fun getItemCount(): Int {
        return collection.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(collection[position], clickListener)
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(feature: Feature, clickListener: (Feature, Int) -> Unit) {

            itemView.tv_list_item.text = feature.getStringProperty(PROPERTY_TITLE)
            itemView.tv_logo_view.setImageResource(if (feature.getBooleanProperty(PROPERTY_FAVOURITE)) R.drawable.ic_favorite else R.drawable.ic_favorite_border)

            itemView.setOnClickListener { clickListener(feature, adapterPosition) }
        }
    }

}