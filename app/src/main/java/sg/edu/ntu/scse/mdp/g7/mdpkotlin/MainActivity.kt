package sg.edu.ntu.scse.mdp.g7.mdpkotlin

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Message
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_data_inspector.*
import kotlinx.android.synthetic.main.dialog_devices.*
import kotlinx.android.synthetic.main.dialog_mdf_manager.*
import kotlinx.android.synthetic.main.dialog_message.*
import kotlinx.android.synthetic.main.dialog_string_configs.*
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.entity.Device
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.entity.MessageLog
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.entity.Protocol
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.entity.Store
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.service.BluetoothService
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.util.Cmd
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.util.MapDrawer
import sg.edu.ntu.scse.mdp.g7.mdpkotlin.util.Parser
import java.io.*
import java.lang.ClassCastException
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    val FROMANDROID = "\"from\":\"Android\","

    // Activity Variables
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var connectionThread: BluetoothService? = null
    private lateinit var connectedDevice: BluetoothDevice
    private val messageLog = MessageLog()
    private var isServer = false
    private var disconnectState = true
    private var startModeState = false
    private var fastestPathModeState = false
    private var autoModeState = true
    private val deviceList = ArrayList<Device>()
    private lateinit var timer: CountDownTimer

    // Additional GUI Components
    private lateinit var inflater: LayoutInflater

    // Controls for Messaging Sending
    private var currentTime = System.currentTimeMillis()

    private lateinit var sensor_orientation: OrientationEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inflater = LayoutInflater.from(this)
        setContentView(R.layout.activity_main)

        // UI Configurations
        configureToggle()

        // Request for location (BT)
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        // Bluetooth Adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 1)
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(receiver, filter)

        // Set up Main Activity Event Listeners
        button_direction_left.setOnClickListener(direction_left)
        button_direction_right.setOnClickListener(direction_right)
        button_direction_up.setOnClickListener(direction_up)
        button_refresh_phase.setOnClickListener(refreshState)
        switch_motion_control.setOnCheckedChangeListener(motion_control)
        button_set_origin.setOnClickListener(setOrigin)
        button_set_waypoint.setOnClickListener(setWayPoint)
        button_start_phase.setOnClickListener(startMode)
        toggle_mode_fastest_path.setOnCheckedChangeListener(changeModeFastestPath)
        toggle_update_auto.setOnCheckedChangeListener(changeAutoMode)
        canvas_gridmap.setOnTouchListener(setMap)
        button_reset_map.setOnClickListener(resetMap)
        sensor_orientation = object: OrientationEventListener(this) { override fun onOrientationChanged(orientation: Int) { handleRotation(orientation) } }

        // Joystick Listener
        joystickView.setOnMoveListener{ angle, strength ->
            if (angle > 45 && angle< 135) {
                val x_axis = MapDrawer.Robot_X
                val y_axis = MapDrawer.Robot_Y

                MapDrawer.moveUp()
                if (!(x_axis == MapDrawer.Robot_X && y_axis == MapDrawer.Robot_Y)) sendString(commandWrap(Cmd.DIRECTION_UP))
                canvas_gridmap.invalidate()
                updateRobotPositionLabel()
            } else if ((angle > 0 && angle <= 45) || (angle > 315 && angle < 360)) {
                sendString(commandWrap(Cmd.DIRECTION_RIGHT))
                MapDrawer.moveRight()
                canvas_gridmap.invalidate()
                updateRobotPositionLabel()
            } else if (angle >= 135 && angle < 255) {
                sendString(commandWrap(Cmd.DIRECTION_LEFT))
                MapDrawer.moveLeft()
                canvas_gridmap.invalidate()
                updateRobotPositionLabel()
            }
        }
    }

    private fun configureToggle() {
        toggle_mode_exploration.setPadding(15, 10, 15, 10)
        toggle_mode_fastest_path.setPadding(15, 10, 15,10)

        toggle_update_auto.setPadding(15, 5, 15, 5)
        toggle_update_manual.setPadding(15, 5, 15, 5)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null && requestCode == INTENT_EXPORT && mdfFileToExport != null) {
            TODO("CODE STUB")
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            var device: BluetoothDevice? = null
            var getCurrentConnection: String? = null

            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    Log.d(TAG, if (device != null && device.name != null) device.name else "No device name")
                    addDevice(device, device.name, device.address)
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    getCurrentConnection = label_bluetooth_status.text.toString()
                    if (connectionThread != null || !disconnectState && getCurrentConnection == "Not Connected") {
                        Log.d(TAG, "Connected with a device")
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        connectedState(device)
                        disconnectState = false

                        disableElement(button_bluetooth_server_listen)
                        disableElement(button_scan)
                        disableElement(listView_devices)

                        if (isPairedDevicesOnly) {
                            clearDeviceList()
                            isPairedDevicesOnly = false
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected with a device")
                    getCurrentConnection = label_bluetooth_status.text.toString()
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    if (getCurrentConnection == "Connected" && device.address == connectedDevice.address) {
                        connectionThread?.cancel()

                        if (!disconnectState) {
                            if (isServer) {
                                Log.d(TAG, "Starting Server")
                                connectionThread = BluetoothService(streamHandler)
                                connectionThread?.startServer(bluetoothAdapter)
                            } else {
                                Log.d(TAG, "Starting Client")
                                connectionThread = BluetoothService(streamHandler)
                                connectionThread?.connectDevice(connectedDevice)
                            }
                        } else connectionThread = null
                        disconnectedState()
                    }
                }
                else -> Log.d(TAG, "Default case for receiver")
            }
        }
    }

    private fun addDevice(device: BluetoothDevice, deviceName: String, deviceHardwareAddress: String) {
        var flag = true
        run toBreak@ {
            deviceList.forEach {
                if (it.macAddr == deviceHardwareAddress) {
                    flag = false
                    return@toBreak
                }
            }
        }

        if (flag) {
            deviceList.add(Device(device, deviceName, deviceHardwareAddress))
            listView_devices.invalidate()

            val state = listView_devices.onSaveInstanceState()
            val adapter = DeviceAdapter(applicationContext, deviceList)
            listView_devices.adapter = adapter
            listView_devices.onRestoreInstanceState(state)
        }
    }

    private fun clearDeviceList() {
        deviceList.clear()
        listView_devices.invalidate()
        val adapter = DeviceAdapter(applicationContext, deviceList)
        listView_devices.adapter = adapter
    }

    // Stream for data
    private val streamHandler = object: Handler() {
        override fun handleMessage(message: Message) {
            when (message.what) {
                Protocol.MESSAGE_RECEIVE -> {
                    // Factor into various scenarios
                    val buffer = message.obj as ByteArray
                    val data = String(buffer, 0, message.arg1)
                    Log.d(TAG, "Received data : $data")
                    messageLog.addMessage(sg.edu.ntu.scse.mdp.g7.mdpkotlin.entity.Message.MESSAGE_RECEIVER, data.trim())
                    // Split data by ;
                    val textArr = data.split(";")
                    textArr.forEach {
                        if (it.isNullOrEmpty()) return@forEach
                        handleAction(it.trim()) // Handle Action
                    }

                    label_message_log?.text = messageLog.getLog()
                }
                Protocol.CONNECTION_ERROR -> {
                    Log.d(TAG, "Connection error with a device")
                    connectionThread?.cancel()
                    connect_bluetooth_device()
                }
                Protocol.MESSAGE_ERROR -> {
                    Log.d(TAG, "Error sending message to device")
                    transmissionFail()
                }
                else -> Log.d(TAG, "Just a default case")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)

        connectionThread?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_menu_inspector -> {
                Log.d(TAG, "Clicked on Menu Inspector")
                dialog_data_inspector()
            }
            R.id.app_menu_chat -> {
                Log.d(TAG, "Clicked on Message Log")
                dialog_message_log()
            }
            R.id.app_menu_search_device -> {
                Log.d(TAG, "Clicked on Search Device")
                dialog_devices()
            }
            R.id.app_menu_disconnect_device -> {
                Log.d(TAG, "Clicked on Disconnect Device")
                disconnect_bluetooth_device()
            }
            R.id.app_menu_reconnect_device -> {
                Log.d("BT-Main", "Clicked on Reconnect Device")
                dialog_paired_devices()
            }
            R.id.app_menu_string_config -> {
                Log.d(TAG, "Clicked on String Configurations")
                dialog_config_string()
            }
            R.id.app_menu_export_mdf -> {
                Log.d(TAG, "Clicked on Export MDF")
                dialog_file_manager()
            }
            else -> Log.d(TAG, "Clicked on default case")
        }
        return super.onOptionsItemSelected(item)
    }

    // Bluetooth connection state
    private fun connectedState(device: BluetoothDevice) {
        connectedDevice = device
        label_bluetooth_status.text = "Connected"
        label_bluetooth_status.setTextColor(Color.parseColor("#388e3c"))

        label_bluetooth_connected_device.text = if (connectedDevice.name != null) connectedDevice.name else "Unknown Device"
    }

    private fun disconnectedState() {
        label_bluetooth_status.text = "Not Connected"
        label_bluetooth_status.setTextColor(Color.parseColor("#d32f2f"))
        label_bluetooth_connected_device.text = ""
    }

    // Handle different UI state
    private fun startModeUI() {
        sensor_orientation.disable()
        disableElement(toggle_mode_exploration)
        disableElement(toggle_mode_fastest_path)
        disableElement(button_set_waypoint)
        disableElement(button_set_origin)
        disableElement(button_direction_left)
        disableElement(button_direction_right)
        disableElement(button_direction_up)
        disableElement(switch_motion_control)
        disableElement(button_reset_map)
        disableElement(joystickView)
        switch_motion_control.isChecked = false
    }

    private fun endModeUI() {
        enableElement(toggle_mode_exploration)
        enableElement(toggle_mode_fastest_path)
        enableElement(button_set_waypoint)
        enableElement(button_set_origin)
        enableElement(button_direction_left)
        enableElement(button_direction_right)
        enableElement(button_direction_up)
        enableElement(switch_motion_control)
        enableElement(button_reset_map)
        enableElement(joystickView)
    }

    private fun disableElement(view: View?) {
        view?.isEnabled = false
        view?.alpha = 0.7f
    }

    private fun enableElement(view: View?) {
        view?.isEnabled = true
        view?.alpha = 1f
    }

    private fun updateRobotPositionLabel() {
        label_origin_coordinateX.text = MapDrawer.Robot_X.toString()
        label_origin_coordinateY.text = MapDrawer.getRobotInvertY().toString()
    }

    private fun updateWaypointLabel() {
        label_waypoint_coordinateX.text = MapDrawer.Way_Point_X.toString()
        label_waypoint_coordinateY.text = MapDrawer.Way_Point_Y.toString()
    }

    // Dialog Builders
    fun dialog_config_string() {
        // View configs
        val dialog = inflater.inflate(R.layout.dialog_string_configs, null)
        val dialog_builder = AlertDialog.Builder(this).setView(dialog)

        button_send_string1.setOnClickListener(sendString1)
        button_send_string2.setOnClickListener(sendString2)
        button_save_string_config.setOnClickListener(saveStringConfig)

        dialog_builder.create()
        dialog_builder.show()
        setStringConfig(textbox_string1, textbox_string2)
    }
    fun dialog_message_log() {
        // View configs
        val dialog = inflater.inflate(R.layout.dialog_message, null)
        val dialog_builder = AlertDialog.Builder(this).setView(dialog)

        button_send_message.setOnClickListener(sendMessage)
        label_message_log.movementMethod = ScrollingMovementMethod()
        label_message_log.text = messageLog.getLog()
        label_message_log.setTextIsSelectable(true)

        dialog_builder.show()
    }
    private lateinit var mdfStringFolderAdapter: StringAdapter
    private fun getMdfFolder(): File {
        val dir = File(this.filesDir, "mdf")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    @Throws(FileNotFoundException::class)
    private fun saveMdfToFile() {
        val dir = getMdfFolder()
        val f = File(dir, "mdf-${System.currentTimeMillis()}.txt")
        val pw = PrintWriter(BufferedOutputStream(FileOutputStream(f)))
        pw.println(Parser.mdfPayload)
        pw.println(Parser.hexMDF)
        pw.println(Parser.hexExplored)
        pw.println("{ ${Parser.hexImage}}")
        pw.flush()
        pw.close()
    }
    fun dialog_file_manager() {
        val dir = getMdfFolder()
        val fileList = dir.list()
        val dialog = inflater.inflate(R.layout.dialog_mdf_manager, null)
        val dialog_builder = AlertDialog.Builder(this).setView(dialog)

        mdfStringFolderAdapter = StringAdapter(applicationContext, fileList)
        mdf_list.adapter = mdfStringFolderAdapter
        mdf_list.setOnItemClickListener { parent, view, position, id ->
            val fileName = mdfStringFolderAdapter.getItem(position)
            mdfFileToExport = File(getMdfFolder(), fileName)
            val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            startActivityForResult(exportIntent, INTENT_EXPORT)
        }
        dialog_builder.show()
    }
    fun dialog_data_inspector() {
        // Save to file
        try { saveMdfToFile() } catch (e: FileNotFoundException) { Log.e(TAG, "Cannot save file, lets just move on :shifty_looking_eyes:") }

        // View configs
        val dialog = inflater.inflate(R.layout.dialog_data_inspector, null)
        val dialog_builder = AlertDialog.Builder(this).setView(dialog)

        label_mdf1_content.text = "0x${Parser.hexMDF}"
        label_mdf1_content.setTextIsSelectable(true)
        label_mdf2_content.text = "0x${Parser.hexExplored}"
        label_mdf2_content.setTextIsSelectable(true)
        label_image_content.text = "{${Parser.hexImage}}"
        label_image_content.setTextIsSelectable(true)
    }
    fun dialog_devices() {
        // View configs
        val dialog = inflater.inflate(R.layout.dialog_devices, null)
        val dialog_builder = AlertDialog.Builder(this).setView(dialog)
        val adapter = DeviceAdapter(applicationContext, deviceList)
        listView_devices.adapter = adapter

        if (connectionThread != null) {
            disableElement(listView_devices)
            disableElement(button_bluetooth_server_listen)
            disableElement(button_scan)
        }

        // Configure event listener
        button_scan.setOnClickListener(scanDevice)
        button_bluetooth_server_listen.setOnClickListener(startBluetoothServer)
        listView_devices.onItemClickListener = connectDevice

        isPairedDevicesOnly = false
        dialog_builder.show()
    }
    private var isPairedDevicesOnly = false
    fun dialog_paired_devices() {
        // View configs
        val dialog = inflater.inflate(R.layout.dialog_devices, null)
        val dialog_builder = AlertDialog.Builder(this).setView(dialog)
        val pairedDevices = bluetoothAdapter.bondedDevices
        pairedDevices.forEach { addDevice(it, it.name, it.address) }

        val adapter = DeviceAdapter(applicationContext, deviceList)
        listView_devices.adapter = adapter
        btconn_instructions.text = "Make sure device is nearby before using this feature. Also disconnect current connections"
        label_dialog_bluetooth_title.text = "Reconnect Bluetooth Connection"
        button_scan.visibility = View.GONE

        if (connectionThread != null) {
            disableElement(listView_devices)
            disableElement(button_bluetooth_server_listen)
        }

        // Configure event listeners
        button_bluetooth_server_listen.setOnClickListener(startBluetoothServer)
        listView_devices.onItemClickListener = connectDevice

        isPairedDevicesOnly = true
        dialog_builder.show()
    }

    // Event Listeners
    private val direction_left = View.OnClickListener {
        sendString(commandWrap(Cmd.DIRECTION_LEFT))
        MapDrawer.moveLeft()
        canvas_gridmap.invalidate()
        updateRobotPositionLabel()
    }
    private val direction_right = View.OnClickListener {
        sendString(commandWrap(Cmd.DIRECTION_RIGHT))
        MapDrawer.moveRight()
        canvas_gridmap.invalidate()
        updateRobotPositionLabel()
    }
    private val direction_up = View.OnClickListener {
        val x_axis = MapDrawer.Robot_X
        val y_axis = MapDrawer.Robot_Y

        MapDrawer.moveUp()
        if (!(x_axis == MapDrawer.Robot_X && y_axis == MapDrawer.Robot_Y)) sendString(commandWrap(Cmd.DIRECTION_UP))
        canvas_gridmap.invalidate()
        updateRobotPositionLabel()
    }
    private val startMode = View.OnClickListener {
        if (startModeState) {
            startModeState = false
            button_start_phase.text = "Start"
            sendString(commandWrap(Cmd.STOP))
            timer.cancel()
            endModeUI()
        } else {
            startModeState = true
            button_start_phase.text = "Stop"
            if (fastestPathModeState) sendString(commandWrap(Cmd.FASTEST_PATH_START))
            else sendString(commandWrap(Cmd.EXPLORATION_START))

            timer = object: CountDownTimer(30000000, 1000) {
                override fun onTick(l: Long) {
                    val timePassed = 30000000 - l
                    var seconds = timePassed / 1000
                    val minutes = seconds / 60
                    seconds %= 60
                    val timeFormatter = DecimalFormat("00")
                    val time = "${timeFormatter.format(minutes)} m ${timeFormatter.format(seconds)} s"
                    label_time_elapsed.text = time
                }
                override fun onFinish() { }
            }.start()
            startModeUI()
        }
    }
    private val setOrigin = View.OnClickListener {
        if (!MapDrawer.selectStartPoint && !MapDrawer.selectWayPoint) {
            button_set_origin.text = "Confirm Origin"
            disableElement(button_set_waypoint)
            disableElement(button_direction_left)
            disableElement(button_direction_right)
            disableElement(button_direction_up)
            sensor_orientation.disable()
            switch_motion_control.isChecked = false
            disableElement(switch_motion_control)
            disableElement(button_start_phase)
            disableElement(button_reset_map)
            MapDrawer.setSelectStartPoint()
            canvas_gridmap.invalidate()
        } else if (MapDrawer.selectStartPoint) {
            button_set_origin.text = "Set Origin"
            enableElement(button_set_waypoint)
            enableElement(button_direction_left)
            enableElement(button_direction_right)
            enableElement(button_direction_up)
            enableElement(switch_motion_control)
            enableElement(button_start_phase)
            enableElement(button_reset_map)
            val msg = ";{$FROMANDROID\"com\":\"startingPoint\",\"startingPoint\":[${MapDrawer.Start_Point_X},${MapDrawer.Start_Point_Y},${MapDrawer.getRotationDir()}]}"
            sendString(msg)

            MapDrawer.setSelectStartPoint()
            MapDrawer.updateStartPoint()
            canvas_gridmap.invalidate()
        }
    }
    private val setWayPoint = View.OnClickListener {
        if (!MapDrawer.selectStartPoint && !MapDrawer.selectWayPoint) {
            button_set_waypoint.text = "Confirm Waypoint"
            disableElement(button_set_origin)
            disableElement(button_direction_left)
            disableElement(button_direction_right)
            disableElement(button_direction_up)
            sensor_orientation.disable()
            switch_motion_control.isChecked = false
            disableElement(switch_motion_control)
            disableElement(button_start_phase)
            disableElement(button_reset_map)
            MapDrawer.setSelectWayPoint()
            canvas_gridmap.invalidate()
        } else if (MapDrawer.selectWayPoint) {
            button_set_waypoint.text = "Set Waypoint"
            enableElement(button_set_origin)
            enableElement(button_direction_left)
            enableElement(button_direction_right)
            enableElement(button_direction_up)
            enableElement(switch_motion_control)
            enableElement(button_start_phase)
            enableElement(button_reset_map)
            val msg = ";{$FROMANDROID\"com\":\"wayPoint\",\"wayPoint\":[${MapDrawer.Way_Point_X},${MapDrawer.Way_Point_Y}]}"
            sendString(msg)

            MapDrawer.setSelectWayPoint()
            canvas_gridmap.invalidate()
        }
    }
    private val motion_control = CompoundButton.OnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
        if (b) {
            sensor_orientation.enable()
            disableElement(button_direction_left)
            disableElement(button_direction_right)
            disableElement(button_direction_up)
            disableElement(joystickView)
        } else {
            sensor_orientation.disable()
            enableElement(button_direction_left)
            enableElement(button_direction_right)
            enableElement(button_direction_up)
            enableElement(joystickView)
        }
    }
    private val setMap = View.OnTouchListener { view, motionEvent ->
        if (motionEvent != null) {
            if (motionEvent.action == MotionEvent.ACTION_DOWN && (MapDrawer.selectStartPoint || MapDrawer.selectWayPoint)) {
                val x_axis = (motionEvent.x / MapDrawer.gridDimensions).toInt()
                val y_axis = (motionEvent.y / MapDrawer.gridDimensions).toInt()
                val invert_y_axis = MapDrawer.invertYAxis(y_axis)

                if (MapDrawer.validMidpoint(x_axis, y_axis)) {
                    MapDrawer.updateSelection(x_axis, y_axis)
                    canvas_gridmap.invalidate()
                }
                updateRobotPositionLabel()
                updateWaypointLabel()
            }
        }
        false
    }
    private val refreshState = View.OnClickListener {
        canvas_gridmap.invalidate()
        updateRobotPositionLabel()
    }
    private val changeModeFastestPath = CompoundButton.OnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
        fastestPathModeState = b
        label_time_elapsed.text = "00 m 00 s"
        Log.d(TAG, "Fastest Path Mode : $fastestPathModeState")
    }
    private val changeAutoMode = CompoundButton.OnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
        autoModeState = b
        Log.d(TAG, "Auto Mode : $autoModeState")
    }
    private val resetMap = View.OnClickListener {
        val dialog = AlertDialog.Builder(this).apply {
            setTitle("Map Reset")
            setMessage("Do you want to reset the map?")
            setNegativeButton("YES") { dialogInterface,_ ->
                MapDrawer.resetMap()
                image_content.setImageResource(R.drawable.img_0)
                canvas_gridmap.invalidate()
                dialogInterface.dismiss()
                sendString(commandWrap(Cmd.CLEAR)) // Send Clear
            }
            setPositiveButton("NO") { dialogInterface,_ -> dialogInterface.dismiss() }
        }.create()
        dialog.show()
    }

    // Event Listeners for Dialog Builders
    private val sendString1 = View.OnClickListener {
        val data = textbox_string1.text.toString()
        Log.d(TAG, "Data Sent (String 1) : $data")
        sendString(data)
    }
    private val sendString2 = View.OnClickListener {
        val data = textbox_string2.text.toString()
        Log.d(TAG, "Data Sent (String 2) : $data")
        sendString(data)
    }
    private val saveStringConfig = View.OnClickListener { saveStringConfig(textbox_string1, textbox_string2) }
    private val sendMessage = View.OnClickListener {
        val data = textbox_send_message.text.toString()
        Log.d(TAG, "Message Sent : $data")
        messageLog.addMessage(sg.edu.ntu.scse.mdp.g7.mdpkotlin.entity.Message.MESSAGE_SENDER, data)
        label_message_log.text = messageLog.getLog()
        sendString(data)
        textbox_send_message.setText("")
    }
    private val scanDevice = View.OnClickListener {
        if (button_scan.text == "Scan Devices") {
            disableElement(button_bluetooth_server_listen)
            button_scan.text = "Stop Scan"
            bluetoothAdapter.startDiscovery()
            clearDeviceList()
        } else if (button_scan.text == "Stop Scan") {
            enableElement(button_bluetooth_server_listen)
            button_scan.text = "Scan Devices"
            bluetoothAdapter.cancelDiscovery()
        }
    }
    private val connectDevice = AdapterView.OnItemClickListener { adapterView, view, i, l ->
        val item = deviceList.get(i)
        val device = item.device

        Log.d(TAG, "Connect")
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()

        connectedDevice = device
        connect_bluetooth_device()
        isServer = false
    }
    private val startBluetoothServer = View.OnClickListener {
        if (button_bluetooth_server_listen.text == "Stop Bluetooth Server") {
            button_bluetooth_server_listen.text = "Start Bluetooth Server"
            enableElement(listView_devices)
            enableElement(button_scan)
        } else if (button_bluetooth_server_listen.text == "Start Bluetooth Server") {
            button_bluetooth_server_listen.text = "Stop Bluetooth Server"
            disableElement(listView_devices)
            disableElement(button_scan)

            connectionThread = BluetoothService(streamHandler)
            connectionThread?.startServer(bluetoothAdapter)
            isServer = true
        }
    }

    // Helper functions for Bluetooth
    private fun disconnect_bluetooth_device() {
        connectionThread?.cancel()
        connectionThread = null
        disconnectedState()
        disconnectState = true
    }

    private fun connect_bluetooth_device() {
        connectionThread = BluetoothService(streamHandler)
        connectionThread?.connectDevice(connectedDevice)
    }

    // Helper functions for transmission
    private fun sendString(data: String) {
        if (connectionThread != null) connectionThread?.write(data)
        else notConnected()
    }

    // Help functions for storing data in Shared Preferences
    private fun setStringConfig(field_1: EditText, field_2: EditText) {
        val sharedPref = applicationContext.getSharedPreferences(Store.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE)

        sharedPref?.let {
            val string_1 = it.getString(Store.STRING_1, "") ?: ""
            val string_2 = it.getString(Store.STRING_2, "") ?: ""

            if (string_1.isNotEmpty() && string_2.isNotEmpty()) {
                Log.d(TAG, "Store : $string_1")
                Log.d(TAG, "Store : $string_2")
                field_1.setText(string_1)
                field_2.setText(string_2)
            }
        }
    }
    private fun saveStringConfig(field_1: EditText, field_2: EditText) {
        val sharedPref = applicationContext.getSharedPreferences(Store.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putString(Store.STRING_1, field_1.text.toString())
        editor.putString(Store.STRING_2, field_2.text.toString())
        editor.apply() // Not using commit as that is synchronous
        savedString()
    }

    // Helper functions for Toasts
    private fun savedString() { Toast.makeText(applicationContext, "Strings have been saved", Toast.LENGTH_SHORT).show() }
    private fun notConnected() { Toast.makeText(applicationContext, "Not connected with any devices", Toast.LENGTH_SHORT).show() }
    private fun transmissionFail() { Toast.makeText(applicationContext, "Error sending message to device", Toast.LENGTH_SHORT).show() }

    // Helper functions for rotation
    private fun handleRotation(degree: Int) {
        Log.d(TAG, degree.toString())
        val time = System.currentTimeMillis()

        if (degree in 80..100 && (time - currentTime) >= 250) {
            MapDrawer.moveRight()
            sendString(commandWrap(Cmd.DIRECTION_RIGHT))
        } else if (degree in 260..280 && (time - currentTime) >= 100) {
            MapDrawer.moveLeft()
            sendString(commandWrap(Cmd.DIRECTION_LEFT))
        } else if (degree in 170..190 && (time - currentTime) >= 100) {
            MapDrawer.moveUp()
            sendString(commandWrap(Cmd.DIRECTION_UP))
        } else return
        updateRobotPositionLabel()
        canvas_gridmap.invalidate()
        currentTime = System.currentTimeMillis()
    }

    // Helper functions to handle received message
    private fun handleAction(payload: String) {
        Log.d("Action", "Parsing $payload")
        val parse = Parser(payload)

        val isStatus = parse.setStatus()
        if (isStatus) {
            handleUpdateStatus(parse.Robot_Status)
            return
        }

        if (!parse.validPayload) return

        handleUpdatePosition(parse.Robot_X, parse.Robot_Y, parse.Robot_Dir)

        parse.processImage()
        handleUpdateImage(parse.lastImageID)
        MapDrawer.setGrid(parse.exploredMap)
    }

    private fun handleUpdateImage(imgID: String) {
        image_content.setImageResource(when (imgID) {
            "0" -> R.drawable.img_0
            "1" -> R.drawable.img_1
            "2" -> R.drawable.img_2
            "3" -> R.drawable.img_3
            "4" -> R.drawable.img_4
            "5" -> R.drawable.img_5
            "6" -> R.drawable.img_6
            "7" -> R.drawable.img_7
            "8" -> R.drawable.img_8
            "9" -> R.drawable.img_9
            "10" -> R.drawable.img_10
            "11" -> R.drawable.img_11
            "12" -> R.drawable.img_12
            "13" -> R.drawable.img_13
            "14" -> R.drawable.img_14
            "15" -> R.drawable.img_15
            else -> R.drawable.img_0
        })
    }

    private fun handleUpdatePosition(x_axis: Int, y_axis: Int, dir: String) {
        try {
            MapDrawer.updateCoordinates(x_axis, y_axis, dir)

            if (autoModeState) canvas_gridmap.invalidate()
        } catch (typeEx: ClassCastException) {
            Log.d(TAG, "Unable to cast data into int")
        }
        updateRobotPositionLabel()
    }

    private fun handleUpdateStatus(data: String) {
        Log.d(TAG, "Status Update : $data")
        label_status_details.text = data
    }

    fun commandWrap(cmd: String): String {
        return ";{$FROMANDROID\"com\":\"${cmd}\"}"
    }

    companion object {
        private const val TAG = "Main"

        // File Management
        private var mdfFileToExport: File? = null
        private const val INTENT_EXPORT = 7
    }
}