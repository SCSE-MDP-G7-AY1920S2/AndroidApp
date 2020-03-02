package sg.edu.ntu.scse.mdp.g7.mdpkotlin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class StringAdapter(private val context: Context, private val stringList: Array<String>) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    init { stringList.reverse() }

    override fun getCount(): Int { return stringList.size }
    override fun getItem(i: Int): Any { return stringList[i] }
    override fun getItemId(i: Int): Long { return 0 }
    override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
        val viewObj = inflater.inflate(android.R.layout.simple_list_item_1, null)
        val main = viewObj.findViewById<TextView>(android.R.id.text1)
        main.text = stringList[i]
        return viewObj
    }
}