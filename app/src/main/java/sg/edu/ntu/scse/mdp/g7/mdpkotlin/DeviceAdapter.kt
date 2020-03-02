package sg.edu.ntu.scse.mdp.g7.mdpkotlin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.entity.Device

class DeviceAdapter(private val context: Context, private val deviceList: ArrayList<Device>) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int { return deviceList.size }
    override fun getItem(i: Int): Any? { return null }
    override fun getItemId(i: Int): Long { return 0 }
    override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
        val viewObj = inflater.inflate(R.layout.listview_device, null)
        val device = viewObj.findViewById<TextView>(R.id.textView)
        val macAddr = viewObj.findViewById<TextView>(R.id.textView2)

        device.text = deviceList[i].deviceName
        macAddr.text = deviceList[i].macAddr

        return viewObj
    }
}