package com.example.mdpandroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.Manifest.permission;

import android.app.LauncherActivity;
import android.bluetooth.BluetoothServerSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import androidx.gridlayout.widget.GridLayout;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mdpandroid.entity.Device;
import com.example.mdpandroid.entity.Map;
import com.example.mdpandroid.entity.MessageLog;
import com.example.mdpandroid.entity.Store;
import com.example.mdpandroid.service.BluetoothService;
import com.example.mdpandroid.entity.Protocol;
import com.example.mdpandroid.util.Cmd;
import com.example.mdpandroid.util.Parser;
import com.example.mdpandroid.util.RawImageParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import info.hoang8f.android.segmented.SegmentedGroup;
import io.github.controlwear.virtual.joystick.android.JoystickView;


public class MainActivity extends AppCompatActivity{

    private final String FROMANDROID = "\"from\":\"Android\",";
    
    private static final String TAG = "Main";

    /**
     * Activity variables
     */
    BluetoothAdapter bluetoothAdapter;
    BluetoothService connectionThread;
    BluetoothDevice connectedDevice;
    ListView listView_devices;
    MessageLog messageLog = new MessageLog();
    boolean isServer = false;
    boolean disconnectState = true;
    boolean startModeState = false;
    boolean fastestPathModeState = false;
    boolean autoModeState = true;
    ArrayList<Device> deviceList = new ArrayList<>();
    CountDownTimer timer;

    /**
     * Additional GUIs components
     */
    LayoutInflater inflater;

    /**
     * Controls for Devices configs
     */
    Button button_bluetooth_server_listen;
    Button button_scan;

    /**
     * Controls for String configs
     */
    EditText textbox_string1;
    EditText textbox_string2;

    /**
     * Controls for Message sending
     */
    EditText textbox_send_message;
    TextView label_message_log;
    long currentTime = System.currentTimeMillis();

    /**
     * Controls for Image
     */
    ImageView image_content;

    /**
     * Handles the orientation
     */
    OrientationEventListener sensor_orientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflater = LayoutInflater.from(this);
        setContentView(R.layout.activity_main);

        /**
         * UI configurations
         */
        configureToggle();

        /**
         * Request for location (required for bluetooth)
         */
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    permission.ACCESS_COARSE_LOCATION
            }, 1);
        }

        /**
         * Bluetooth Adapter
         */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(receiver, filter);

        /**
         * Set up Main Activity event listeners
         */
        findViewById(R.id.button_direction_left).setOnClickListener(direction_left);
        findViewById(R.id.button_direction_right).setOnClickListener(direction_right);
        findViewById(R.id.button_direction_up).setOnClickListener(direction_up);
        findViewById(R.id.button_refresh_phase).setOnClickListener(refreshState);
        ((Switch) findViewById(R.id.switch_motion_control)).setOnCheckedChangeListener(motion_control);
        findViewById(R.id.button_set_origin).setOnClickListener(setOrigin);
        findViewById(R.id.button_set_waypoint).setOnClickListener(setWayPoint);
        findViewById(R.id.button_start_phase).setOnClickListener(startMode);
        ((RadioButton)findViewById(R.id.toggle_mode_fastest_path)).setOnCheckedChangeListener(changeModeFastestPath);
        ((RadioButton)findViewById(R.id.toggle_update_auto)).setOnCheckedChangeListener(changeAutoMode);
        findViewById(R.id.canvas_gridmap).setOnTouchListener(setMap);
        findViewById(R.id.button_reset_map).setOnClickListener(resetMap);
        sensor_orientation = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int i) {
                handleRotation(i);
            }
        };

        /**
         * Image
         */
        image_content = (ImageView) findViewById(R.id.image_content);

        /**
         * Listener for Joystick
         */
        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);

        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                if (angle>45 && angle<135){
                    int x_axis = MapDrawer.getRobotX();
                    int y_axis = MapDrawer.getRobotY();

                    MapDrawer.moveUp();
                    if (!(x_axis == MapDrawer.getRobotX() && y_axis == MapDrawer.getRobotY())){
                        sendString(commandWrap(Cmd.DIRECTION_UP));
                    }
                    findViewById(R.id.canvas_gridmap).invalidate();
                    updateRobotPositionLabel();
                } else if ((angle>0 && angle<=45) ||(angle>315 && angle<360)){
                    sendString(commandWrap(Cmd.DIRECTION_RIGHT));
                    MapDrawer.moveRight();
                    findViewById(R.id.canvas_gridmap).invalidate();
                    updateRobotPositionLabel();
                } else if (angle>=135 && angle<255){
                    sendString(commandWrap(Cmd.DIRECTION_LEFT));
                    MapDrawer.moveLeft();
                    findViewById(R.id.canvas_gridmap).invalidate();
                    updateRobotPositionLabel();
                }
            }
        });

    }

    private void configureToggle(){
        SegmentedGroup toggle_mode = findViewById(R.id.toggle_mode);
        RadioButton toggle_mode_exploration = findViewById(R.id.toggle_mode_exploration);
        RadioButton toggle_mode_fastest_path = findViewById(R.id.toggle_mode_fastest_path);
        toggle_mode_exploration.setPadding(15, 10, 15, 10);
        toggle_mode_fastest_path.setPadding(15, 10, 15, 10);

        SegmentedGroup toggle_update = findViewById(R.id.toggle_update);
        RadioButton toggle_update_auto = findViewById(R.id.toggle_update_auto);
        RadioButton toggle_update_manual = findViewById(R.id.toggle_update_manual);
        toggle_update_auto.setPadding(15, 5, 15, 5);
        toggle_update_manual.setPadding(15, 5, 15, 5);
    }


    /**
     * listener for devices
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = null;
            String getCurrentConnection = null;

            switch(action){
                case BluetoothDevice.ACTION_FOUND:
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, (device != null && device.getName() != null) ? device.getName() : "No device name");
                    addDevice(device, device.getName(), device.getAddress());
                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    getCurrentConnection = ((TextView)findViewById(R.id.label_bluetooth_status)).getText().toString();
                    if (connectionThread != null || !disconnectState && getCurrentConnection.equals("Not Connected")) {
                        Log.d(TAG, "Connected with a device");
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        connectedState(device);
                        disconnectState = false;

                        if (button_bluetooth_server_listen != null) {
                            button_bluetooth_server_listen.setEnabled(false);
                            button_bluetooth_server_listen.setAlpha(0.7f);
                        }
                        if (button_scan != null) {
                            button_scan.setEnabled(false);
                            button_scan.setAlpha(0.7f);
                        }
                        if (listView_devices != null) {
                            listView_devices.setEnabled(false);
                            listView_devices.setAlpha(0.7f);
                        }
                        if (isPairedDevicesOnly) {
                            clearDeviceList();
                            isPairedDevicesOnly = false;
                        }
                    }
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.d(TAG, "Disconnected with a device");
                    getCurrentConnection = ((TextView)findViewById(R.id.label_bluetooth_status)).getText().toString();
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (getCurrentConnection.equals("Connected") && device.getAddress().equals(connectedDevice.getAddress())) {
                        if (connectionThread != null) {
                            connectionThread.cancel();
                        }

                        if (!disconnectState) {
                            if (isServer) {
                                Log.d(TAG, "Starting Server");
                                connectionThread = new BluetoothService(streamHandler);
                                connectionThread.startServer(bluetoothAdapter);
                            } else {
                                Log.d(TAG, "Starting Client");
                                connectionThread = new BluetoothService(streamHandler);
                                connectionThread.connectDevice(connectedDevice);
                            }
                        } else {
                            connectionThread = null;
                        }
                        disconnectedState();
                    }
                    break;
                default: Log.d(TAG, "default case for receiver");
            }
        }
    };

    private void addDevice(BluetoothDevice device, String deviceName, String deviceHardwareAddress){
        boolean flag = true;

        for (Device dev : deviceList) {
            if (dev.getMacAddr().equals(deviceHardwareAddress)){
                flag = false;
                break;
            }
        }

        if (flag){
            deviceList.add(new Device(device, deviceName, deviceHardwareAddress));
            listView_devices.invalidate();

            Parcelable state = listView_devices.onSaveInstanceState();
            DeviceAdapter adapter = new DeviceAdapter(getApplicationContext(), deviceList);
            listView_devices.setAdapter(adapter);
            listView_devices.onRestoreInstanceState(state);
        }
    }

    private void clearDeviceList(){
        deviceList.clear();
        listView_devices.invalidate();
        DeviceAdapter adapter = new DeviceAdapter(getApplicationContext(), deviceList);
        listView_devices.setAdapter(adapter);
    }

    /**
     * stream for data
     */
    private final Handler streamHandler = new Handler(){
        @Override
        public void handleMessage(Message message){
            switch (message.what){
                case Protocol.MESSAGE_RECEIVE:
                    // factor into various scenarios
                    byte[] buffer = (byte[]) message.obj;
                    String data = new String(buffer, 0, message.arg1);
                    Log.d(TAG, "Received data : " + data);
                    messageLog.addMessage(com.example.mdpandroid.entity.Message.MESSAGE_RECEIVER, data.trim());
                    if (!data.trim().startsWith(";") && data.contains("Raw Image String")) {
                        handleRawImageStream(data);
                        return;
                    }
                    //Split data by ;
                    String[] textArr = data.split(";");
                    for (String s : textArr) {
                        if (s == null || s.equals("")) {
                            continue;
                        }
                        //Handle Action
                        handleAction(s.trim());
                    }

                    if (label_message_log != null){
                        label_message_log.setText(messageLog.getLog());
                    }
                    break;
                    case Protocol.CONNECTION_ERROR:
                        Log.d(TAG, "Connection error with a device");
                        if (connectionThread != null){
                            connectionThread.cancel();
                        }
                        connect_bluetooth_device();
                        break;
                case Protocol.MESSAGE_ERROR:
                    Log.d(TAG, "Error sending message to device");
                    transmissionFail();
                    break;
                    default: Log.d(TAG, "Just a default case");
            }
        }
    };

    /**
     * clean up resources upon termination
     **/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);

        if (connectionThread != null){
            connectionThread.cancel();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.app_menu_inspector:
                Log.d(TAG, "Clicked on Menu Inspector");
                dialog_data_inspector();
                break;
            case R.id.app_menu_chat:
                Log.d(TAG, "Clicked on Message Log");
                dialog_message_log();
                break;
            case R.id.app_menu_search_device:
                Log.d(TAG, "Clicked on Search Device");
                dialog_devices();
                break;
            case R.id.app_menu_disconnect_device:
                Log.d(TAG, "Clicked on Disconnect Device");
                disconnect_bluetooth_device();
                break;
            case R.id.app_menu_reconnect_device:
                Log.d("BT-Main", "Clicked on Reconnect Device");
                dialog_paired_devices();
                break;
            case R.id.app_menu_string_config:
                Log.d(TAG, "Clicked on String Configurations");
                dialog_config_string();
                break;
            case R.id.req_img:
                Log.d(TAG, "Requesting Image");
                imageParser = new RawImageParser(this, new RawImageParser.CallbackHandler() {
                    @Override
                    public void sendCommand(String msg) {
                        sendString(commandWrap(msg));
                    }
                });
                sendString(commandWrap(Cmd.REQIMG));
                break;
            default:
                Log.d(TAG, "Clicked on default case");
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Bluetooth connection state
     */
    private void connectedState(BluetoothDevice device){
        connectedDevice = device;
        TextView connectionState = findViewById(R.id.label_bluetooth_status);
        connectionState.setText("Connected");
        connectionState.setTextColor(Color.parseColor("#388e3c"));
        TextView connectionDevice = findViewById(R.id.label_bluetooth_connected_device);

        if (connectedDevice.getName() != null) {
            connectionDevice.setText(connectedDevice.getName());
        } else {
            connectionDevice.setText("Unknown Device");
        }
    }
    private void disconnectedState(){
        TextView connectionState = findViewById(R.id.label_bluetooth_status);
        connectionState.setText("Not Connected");
        connectionState.setTextColor(Color.parseColor("#d32f2f"));
        TextView connectionDevice = findViewById(R.id.label_bluetooth_connected_device);
        connectionDevice.setText("");
    }

    /**
     * Handles different UI state
     */
    private void startModeUI(){
        sensor_orientation.disable();
        findViewById(R.id.toggle_mode_exploration).setEnabled(false);
        findViewById(R.id.toggle_mode_exploration).setAlpha(0.7f);
        findViewById(R.id.toggle_mode_fastest_path).setEnabled(false);
        findViewById(R.id.toggle_mode_fastest_path).setAlpha(0.7f);
        findViewById(R.id.button_set_waypoint).setEnabled(false);
        findViewById(R.id.button_set_waypoint).setAlpha(0.7f);
        findViewById(R.id.button_set_origin).setEnabled(false);
        findViewById(R.id.button_set_origin).setAlpha(0.7f);
        findViewById(R.id.button_direction_left).setEnabled(false);
        findViewById(R.id.button_direction_left).setAlpha(0.7f);
        findViewById(R.id.button_direction_right).setEnabled(false);
        findViewById(R.id.button_direction_right).setAlpha(0.7f);
        findViewById(R.id.button_direction_up).setEnabled(false);
        findViewById(R.id.button_direction_up).setAlpha(0.7f);
        findViewById(R.id.switch_motion_control).setEnabled(false);
        findViewById(R.id.switch_motion_control).setAlpha(0.7f);
        ((Switch) findViewById(R.id.switch_motion_control)).setChecked(false);
        findViewById(R.id.button_reset_map).setEnabled(false);
        findViewById(R.id.button_reset_map).setAlpha(0.7f);
        findViewById(R.id.joystickView).setEnabled(false);
        findViewById(R.id.joystickView).setAlpha(0.7f);
    }
    private void endModeUI(){
        findViewById(R.id.toggle_mode_exploration).setEnabled(true);
        findViewById(R.id.toggle_mode_exploration).setAlpha(1);
        findViewById(R.id.toggle_mode_fastest_path).setEnabled(true);
        findViewById(R.id.toggle_mode_fastest_path).setAlpha(1);
        findViewById(R.id.button_set_waypoint).setEnabled(true);
        findViewById(R.id.button_set_waypoint).setAlpha(1);
        findViewById(R.id.button_set_origin).setEnabled(true);
        findViewById(R.id.button_set_origin).setAlpha(1);
        findViewById(R.id.button_direction_left).setEnabled(true);
        findViewById(R.id.button_direction_left).setAlpha(1);
        findViewById(R.id.button_direction_right).setEnabled(true);
        findViewById(R.id.button_direction_right).setAlpha(1);
        findViewById(R.id.button_direction_up).setEnabled(true);
        findViewById(R.id.button_direction_up).setAlpha(1);
        findViewById(R.id.switch_motion_control).setEnabled(true);
        findViewById(R.id.switch_motion_control).setAlpha(1);
        findViewById(R.id.button_reset_map).setEnabled(true);
        findViewById(R.id.button_reset_map).setAlpha(1);
        findViewById(R.id.joystickView).setEnabled(true);
        findViewById(R.id.joystickView).setAlpha(1);
    }
    private void updateRobotPositionLabel(){
        //((TextView) findViewById(R.id.label_origin_coordinates)).setText("[X, Y] : [" + MapDrawer.getRobotPosition() + "]");
        ((TextView) findViewById(R.id.label_origin_coordinateX)).setText(Integer.toString(MapDrawer.getRobotX()));
        ((TextView) findViewById(R.id.label_origin_coordinateY)).setText(Integer.toString(MapDrawer.getRobotInvertY()));
    }
    private void updateWaypointLabel(){
        //((TextView) findViewById(R.id.label_waypoint_coordinates)).setText("[X, Y] : [" + MapDrawer.getWayPoint() + "]");
        ((TextView) findViewById(R.id.label_waypoint_coordinateX)).setText(Integer.toString(MapDrawer.getWay_Point_X()));
        ((TextView) findViewById(R.id.label_waypoint_coordinateY)).setText(Integer.toString(MapDrawer.getWay_Point_Y()));
    }

    /**
     * Dialog Builders
     */
    public void dialog_config_string(){
        // view configs
        View dialog = inflater.inflate(R.layout.dialog_string_configs, null);
        AlertDialog.Builder dialog_builder = new AlertDialog.Builder(this).setView(dialog);

        // configure event listeners
        textbox_string1 = dialog.findViewById(R.id.textbox_string1);
        textbox_string2 = dialog.findViewById(R.id.textbox_string2);
        dialog.findViewById(R.id.button_send_string1).setOnClickListener(sendString1);
        dialog.findViewById(R.id.button_send_string2).setOnClickListener(sendString2);
        dialog.findViewById(R.id.button_save_string_config).setOnClickListener(saveStringConfig);

        dialog_builder.create();
        dialog_builder.show();
        setStringConfig(textbox_string1, textbox_string2);
    }
    public void dialog_message_log(){
        // view configs
        View dialog = inflater.inflate(R.layout.dialog_message, null);
        AlertDialog.Builder dialog_builder = new AlertDialog.Builder(this).setView(dialog);

        // configure event listeners
        dialog.findViewById(R.id.button_send_message).setOnClickListener(sendMessage);
        textbox_send_message = dialog.findViewById(R.id.textbox_send_message);
        label_message_log = dialog.findViewById(R.id.label_message_log);
        label_message_log.setMovementMethod(new ScrollingMovementMethod());
        label_message_log.setText(messageLog.getLog());
        label_message_log.setTextIsSelectable(true);

        dialog_builder.show();
    }
    public void dialog_data_inspector(){
        // view configs
        View dialog = inflater.inflate(R.layout.dialog_data_inspector, null);
        AlertDialog.Builder dialog_builder = new AlertDialog.Builder(this).setView(dialog);

        ((TextView) dialog.findViewById(R.id.label_mdf1_content)).setText("0x" + Parser.hexMDF);
        ((TextView) dialog.findViewById(R.id.label_mdf1_content)).setTextIsSelectable(true);
        ((TextView) dialog.findViewById(R.id.label_mdf2_content)).setText("0x" + Parser.hexExplored);
        ((TextView) dialog.findViewById(R.id.label_mdf2_content)).setTextIsSelectable(true);

        dialog_builder.show();
    }
    public void dialog_devices(){
        // view configs
        View dialog = inflater.inflate(R.layout.dialog_devices, null);
        AlertDialog.Builder dialog_builder = new AlertDialog.Builder(this).setView(dialog);
        listView_devices = (ListView) dialog.findViewById(R.id.listView_devices);
        DeviceAdapter adapter = new DeviceAdapter(getApplicationContext(), deviceList);
        listView_devices.setAdapter(adapter);
        button_bluetooth_server_listen = dialog.findViewById(R.id.button_bluetooth_server_listen);
        button_scan = dialog.findViewById(R.id.button_scan);

        if (connectionThread != null){
            listView_devices.setEnabled(false);
            listView_devices.setAlpha(0.7f);
            button_bluetooth_server_listen.setEnabled(false);
            button_bluetooth_server_listen.setAlpha(0.7f);
            button_scan.setEnabled(false);
            button_scan.setAlpha(0.7f);
        }

        // configure event listeners
        dialog.findViewById(R.id.button_scan).setOnClickListener(scanDevice);
        dialog.findViewById(R.id.button_bluetooth_server_listen).setOnClickListener(startBluetoothServer);
        listView_devices.setOnItemClickListener(connectDevice);

        isPairedDevicesOnly = false;
        dialog_builder.show();
    }

    private boolean isPairedDevicesOnly = false;
    public void dialog_paired_devices(){
        // view configs
        View dialog = inflater.inflate(R.layout.dialog_devices, null);
        AlertDialog.Builder dialog_builder = new AlertDialog.Builder(this).setView(dialog);
        listView_devices = dialog.findViewById(R.id.listView_devices);
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bt : pairedDevices) addDevice(bt, bt.getName(), bt.getAddress());

        DeviceAdapter adapter = new DeviceAdapter(getApplicationContext(), deviceList);
        listView_devices.setAdapter(adapter);
        button_bluetooth_server_listen = dialog.findViewById(R.id.button_bluetooth_server_listen);
        TextView hi = dialog.findViewById(R.id.btconn_instructions);
        hi.setText("Make sure device is nearby before using this feature. Also disconnect. current connections");
        ((TextView) dialog.findViewById(R.id.label_dialog_bluetooth_title)).setText("Reconnect Bluetooth Connection");
        dialog.findViewById(R.id.button_scan).setVisibility(View.GONE);

        if (connectionThread != null){
            listView_devices.setEnabled(false);
            listView_devices.setAlpha(0.7f);
            button_bluetooth_server_listen.setEnabled(false);
            button_bluetooth_server_listen.setAlpha(0.7f);
        }

        // configure event listeners
        dialog.findViewById(R.id.button_bluetooth_server_listen).setOnClickListener(startBluetoothServer);
        listView_devices.setOnItemClickListener(connectDevice);

        isPairedDevicesOnly = true;
        dialog_builder.show();
    }

    /**
     * Event Listeners for Main Activity
     */
    private final Button.OnClickListener direction_left = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            sendString(commandWrap(Cmd.DIRECTION_LEFT));
            MapDrawer.moveLeft();
            findViewById(R.id.canvas_gridmap).invalidate();
            updateRobotPositionLabel();
        }
    };
    private final Button.OnClickListener direction_right = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            sendString(commandWrap(Cmd.DIRECTION_RIGHT));
            MapDrawer.moveRight();
            findViewById(R.id.canvas_gridmap).invalidate();
            updateRobotPositionLabel();
        }
    };
    private final Button.OnClickListener direction_up = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            int x_axis = MapDrawer.getRobotX();
            int y_axis = MapDrawer.getRobotY();

            MapDrawer.moveUp();
            if (!(x_axis == MapDrawer.getRobotX() && y_axis == MapDrawer.getRobotY())){
                sendString(commandWrap(Cmd.DIRECTION_UP));
            }
            findViewById(R.id.canvas_gridmap).invalidate();
            updateRobotPositionLabel();
        }
    };
    private final Button.OnClickListener startMode = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            final Button button_start_mode = findViewById(R.id.button_start_phase);

            if (startModeState){
                startModeState = false;
                button_start_mode.setText("Start");


                sendString(commandWrap(Cmd.STOP));
//                if (fastestPathModeState){
//                    sendString(commandWrap(Cmd.FASTEST_PATH_STOP));
//                } else{
//                    sendString(commandWrap(Cmd.EXPLORATION_STOP));
//                }
                timer.cancel();
                endModeUI();
            } else{
                startModeState = true;
                button_start_mode.setText("Stop");

                if (fastestPathModeState){
                    sendString(commandWrap(Cmd.FASTEST_PATH_START)); //TODO
                } else{
                    sendString(commandWrap(Cmd.EXPLORATION_START));
                }

                timer = new CountDownTimer(30000000, 1000) {
                    @Override
                    public void onTick(long l) {
                        long timePassed = 30000000 - l;
                        long seconds = timePassed / 1000;

                        long minutes = seconds / 60;
                        seconds = seconds % 60;
                        DecimalFormat timeFormatter = new DecimalFormat("00");
                        String time = timeFormatter.format(minutes) + " m " + timeFormatter.format(seconds) + " s";
                        ((TextView) findViewById(R.id.label_time_elapsed)).setText(time);
                    }

                    @Override
                    public void onFinish() {

                    }
                }.start();
                startModeUI();
            }
        }
    };
    private final Button.OnClickListener setOrigin = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            Button button_set_origin = findViewById(R.id.button_set_origin);
            if (!MapDrawer.getSelectStartPoint() && !MapDrawer.getSelectWayPoint()){
                button_set_origin.setText("Confirm Origin");
                findViewById(R.id.button_set_waypoint).setEnabled(false);
                findViewById(R.id.button_set_waypoint).setAlpha(0.7f);

                findViewById(R.id.button_direction_left).setEnabled(false);
                findViewById(R.id.button_direction_left).setAlpha(0.7f);
                findViewById(R.id.button_direction_right).setEnabled(false);
                findViewById(R.id.button_direction_right).setAlpha(0.7f);
                findViewById(R.id.button_direction_up).setEnabled(false);
                findViewById(R.id.button_direction_up).setAlpha(0.7f);

                sensor_orientation.disable();
                ((Switch) findViewById(R.id.switch_motion_control)).setChecked(false);
                findViewById(R.id.switch_motion_control).setEnabled(false);
                findViewById(R.id.switch_motion_control).setAlpha(0.7f);
                findViewById(R.id.button_start_phase).setEnabled(false);
                findViewById(R.id.button_start_phase).setAlpha(0.7f);
                findViewById(R.id.button_reset_map).setEnabled(false);
                findViewById(R.id.button_reset_map).setAlpha(0.7f);

                MapDrawer.setSelectStartPoint();
                findViewById(R.id.canvas_gridmap).invalidate();
            } else if (MapDrawer.getSelectStartPoint()){
                button_set_origin.setText("Set Origin");
                findViewById(R.id.button_set_waypoint).setEnabled(true);
                findViewById(R.id.button_set_waypoint).setAlpha(1);

                findViewById(R.id.button_direction_left).setEnabled(true);
                findViewById(R.id.button_direction_left).setAlpha(1);
                findViewById(R.id.button_direction_right).setEnabled(true);
                findViewById(R.id.button_direction_right).setAlpha(1);
                findViewById(R.id.button_direction_up).setEnabled(true);
                findViewById(R.id.button_direction_up).setAlpha(1);

                findViewById(R.id.switch_motion_control).setEnabled(true);
                findViewById(R.id.switch_motion_control).setAlpha(1);
                findViewById(R.id.button_start_phase).setEnabled(true);
                findViewById(R.id.button_start_phase).setAlpha(1);
                findViewById(R.id.button_reset_map).setEnabled(true);
                findViewById(R.id.button_reset_map).setAlpha(1);

                String msg = ";{" + FROMANDROID + "\"com\":\"startingPoint\",\"startingPoint\":[" + MapDrawer.getStart_Point_X() + "," + MapDrawer.getStart_Point_Y() + "," + MapDrawer.getRotationDir() + "]}";
                sendString(msg);
                //sendString(MapDrawer.getStartPoint());

                MapDrawer.setSelectStartPoint();
                MapDrawer.updateStartPoint();
                findViewById(R.id.canvas_gridmap).invalidate();
            }
        }
    };
    private final Button.OnClickListener setWayPoint = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            Button button_set_waypoint = findViewById(R.id.button_set_waypoint);
            if (!MapDrawer.getSelectStartPoint() && !MapDrawer.getSelectWayPoint()){
                button_set_waypoint.setText("Confirm Waypoint");
                findViewById(R.id.button_set_origin).setEnabled(false);
                findViewById(R.id.button_set_origin).setAlpha(0.7f);

                findViewById(R.id.button_direction_left).setEnabled(false);
                findViewById(R.id.button_direction_left).setAlpha(0.7f);
                findViewById(R.id.button_direction_right).setEnabled(false);
                findViewById(R.id.button_direction_right).setAlpha(0.7f);
                findViewById(R.id.button_direction_up).setEnabled(false);
                findViewById(R.id.button_direction_up).setAlpha(0.7f);

                sensor_orientation.disable();
                ((Switch) findViewById(R.id.switch_motion_control)).setChecked(false);
                findViewById(R.id.switch_motion_control).setEnabled(false);
                findViewById(R.id.switch_motion_control).setAlpha(0.7f);
                findViewById(R.id.button_start_phase).setEnabled(false);
                findViewById(R.id.button_start_phase).setAlpha(0.7f);
                findViewById(R.id.button_reset_map).setEnabled(false);
                findViewById(R.id.button_reset_map).setAlpha(0.7f);

                MapDrawer.setSelectWayPoint();
                findViewById(R.id.canvas_gridmap).invalidate();
            } else if (MapDrawer.getSelectWayPoint()){
                button_set_waypoint.setText("Set Waypoint");
                findViewById(R.id.button_set_origin).setEnabled(true);
                findViewById(R.id.button_set_origin).setAlpha(1);

                findViewById(R.id.button_direction_left).setEnabled(true);
                findViewById(R.id.button_direction_left).setAlpha(1);
                findViewById(R.id.button_direction_right).setEnabled(true);
                findViewById(R.id.button_direction_right).setAlpha(1);
                findViewById(R.id.button_direction_up).setEnabled(true);
                findViewById(R.id.button_direction_up).setAlpha(1);

                findViewById(R.id.switch_motion_control).setEnabled(true);
                findViewById(R.id.switch_motion_control).setAlpha(1);
                findViewById(R.id.button_start_phase).setEnabled(true);
                findViewById(R.id.button_start_phase).setAlpha(1);
                findViewById(R.id.button_reset_map).setEnabled(true);
                findViewById(R.id.button_reset_map).setAlpha(1);

                String msg = ";{" + FROMANDROID + "\"com\":\"wayPoint\",\"wayPoint\":[" + MapDrawer.getWay_Point_X() + "," + MapDrawer.getWay_Point_Y() + "]}";
                sendString(msg);
//                try {
//                try {
//                    JSONObject wayPoint = new JSONObject();
//                    wayPoint.put("x", MapDrawer.getWay_Point_X());
//                    wayPoint.put("y", MapDrawer.getWay_Point_Y());
//                    wayPoint.put("waypoint", true);
//                    sendString("X" + wayPoint.toString() + "\n");
//                } catch (JSONException ex){
//                    Log.d(TAG, "Error constructing JSON object");
//                }

                MapDrawer.setSelectWayPoint();
                findViewById(R.id.canvas_gridmap).invalidate();
            }
        }
    };
    private final Switch.OnCheckedChangeListener motion_control = new Switch.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (b){
                sensor_orientation.enable();
                findViewById(R.id.button_direction_left).setEnabled(false);
                findViewById(R.id.button_direction_left).setAlpha(0.7f);
                findViewById(R.id.button_direction_right).setEnabled(false);
                findViewById(R.id.button_direction_right).setAlpha(0.7f);
                findViewById(R.id.button_direction_up).setEnabled(false);
                findViewById(R.id.button_direction_up).setAlpha(0.7f);
                findViewById(R.id.joystickView).setEnabled(false);
                findViewById(R.id.joystickView).setAlpha(0.7f);
            } else{
                sensor_orientation.disable();
                findViewById(R.id.button_direction_left).setEnabled(true);
                findViewById(R.id.button_direction_left).setAlpha(1);
                findViewById(R.id.button_direction_right).setEnabled(true);
                findViewById(R.id.button_direction_right).setAlpha(1);
                findViewById(R.id.button_direction_up).setEnabled(true);
                findViewById(R.id.button_direction_up).setAlpha(1);
                findViewById(R.id.joystickView).setEnabled(true);
                findViewById(R.id.joystickView).setAlpha(1);
            }
        }
    };
    private final MapDrawer.OnTouchListener setMap = new MapDrawer.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent != null){
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && (MapDrawer.getSelectStartPoint() || MapDrawer.getSelectWayPoint())){
                    int x_axis = (int) motionEvent.getX() / MapDrawer.gridDimensions;
                    int y_axis = (int) motionEvent.getY() / MapDrawer.gridDimensions;
                    int invert_y_axis = MapDrawer.invertYAxis(y_axis);

                    if (MapDrawer.validMidpoint(x_axis, y_axis)) {
                        MapDrawer.updateSelection(x_axis, y_axis);
                        findViewById(R.id.canvas_gridmap).invalidate();
                    }

                    updateRobotPositionLabel();
                    updateWaypointLabel();
                }
            }
            return false;
        }
    };
    private final Button.OnClickListener refreshState = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            findViewById(R.id.canvas_gridmap).invalidate();
            updateRobotPositionLabel();
        }
    };
    private final RadioButton.OnCheckedChangeListener changeModeFastestPath = new RadioButton.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            fastestPathModeState = b;
            ((TextView) findViewById(R.id.label_time_elapsed)).setText("00 m 00 s");
            Log.d(TAG, "Fastest Path Mode : " + fastestPathModeState);
        }
    };
    private final RadioButton.OnCheckedChangeListener changeAutoMode = new RadioButton.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            autoModeState = b;
            Log.d(TAG, "Auto Mode : " + autoModeState);
        }
    };
    private final Button.OnClickListener resetMap = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
            dialog.setTitle("Map Reset");
            dialog.setMessage("Do you want to reset the map?");

            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    MapDrawer.resetMap();
                    image_content.setImageResource(R.drawable.img_0);
                    findViewById(R.id.canvas_gridmap).invalidate();
                    dialogInterface.dismiss();

                    //Send Clear
                    sendString(commandWrap(Cmd.CLEAR));
                }
            });

            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            dialog.show();
        }
    };

    /**
     * Event Listeners for Dialog Builders
     */
    private final View.OnClickListener sendString1 = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            String data = textbox_string1.getText().toString();
            Log.d(TAG, "Data Sent (String 1) : " + data);
            sendString(data);
        }
    };
    private final View.OnClickListener sendString2 = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            String data = textbox_string2.getText().toString();
            Log.d(TAG, "Data Sent (String 2) : " + data);
            sendString(data);
        }
    };
    private final View.OnClickListener saveStringConfig = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            saveStringConfig(textbox_string1, textbox_string2);
        }
    };
    private final View.OnClickListener sendMessage = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            String data = textbox_send_message.getText().toString();
            Log.d(TAG, "Message Sent : " + data);
            messageLog.addMessage(com.example.mdpandroid.entity.Message.MESSAGE_SENDER, data);
            label_message_log.setText(messageLog.getLog());
            sendString(data);
            textbox_send_message.setText("");
        }
    };
    private final View.OnClickListener scanDevice = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (button_scan.getText().equals("Scan Devices")){
                button_bluetooth_server_listen.setEnabled(false);
                button_bluetooth_server_listen.setAlpha(0.7f);
                button_scan.setText("Stop Scan");
                bluetoothAdapter.startDiscovery();
                clearDeviceList();
            } else if (button_scan.getText().equals("Stop Scan")){
                button_bluetooth_server_listen.setEnabled(true);
                button_bluetooth_server_listen.setAlpha(1.0f);
                button_scan.setText("Scan Devices");
                bluetoothAdapter.cancelDiscovery();
            }
        }
    };
    private final AdapterView.OnItemClickListener connectDevice = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Device item = deviceList.get(i);
            BluetoothDevice device = item.getDevice();

            Log.d(TAG, "Connect");
            if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

            connectedDevice = device;
            connect_bluetooth_device();
            isServer = false;
        }
    };
    private final View.OnClickListener startBluetoothServer = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            if (button_bluetooth_server_listen.getText().equals("Stop Bluetooth Server")){
                button_bluetooth_server_listen.setText("Start Bluetooth Server");
                listView_devices.setAlpha(1);
                listView_devices.setEnabled(true);
                button_scan.setAlpha(1);
                button_scan.setEnabled(true);
            } else if (button_bluetooth_server_listen.getText().equals("Start Bluetooth Server")){
                button_bluetooth_server_listen.setText("Stop Bluetooth Server");
                listView_devices.setAlpha(0.7f);
                listView_devices.setEnabled(false);
                button_scan.setAlpha(0.7f);
                button_scan.setEnabled(false);

                connectionThread = new BluetoothService(streamHandler);
                connectionThread.startServer(bluetoothAdapter);
                isServer = true;
            }
        }
    };

    /**
     * Helper functions for Bluetooth
     */
    private void disconnect_bluetooth_device(){
        if (connectionThread != null){
            connectionThread.cancel();
        }
        connectionThread = null;
        disconnectedState();
        disconnectState = true;
    }
    private void connect_bluetooth_device(){
        connectionThread = new BluetoothService(streamHandler);
        connectionThread.connectDevice(connectedDevice);
    }

    /**
     * Helper functions for transmission
     */
    private void sendString(String data){
        if (connectionThread != null){
            connectionThread.write(data);
        } else{
            notConnected();
        }
    }

    /**
     * Help functions for storing data in Shared Preferences
     */
    private void setStringConfig(EditText field_1, EditText field_2){
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(Store.SHARED_PREFERENCE_KEY,Context.MODE_PRIVATE);

        if (sharedPref != null){
            String string_1 = sharedPref.getString(Store.STRING_1, "");
            String string_2 = sharedPref.getString(Store.STRING_2, "");

            if (!(string_1.equals("") && string_2.equals(""))){
                Log.d(TAG, "Store : " + string_1);
                Log.d(TAG, "Store : " + string_2);
                field_1.setText(string_1);
                field_2.setText(string_2);
            }
        }
    }
    private void saveStringConfig(EditText field_1, EditText field_2){
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(Store.SHARED_PREFERENCE_KEY,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(Store.STRING_1, field_1.getText().toString());
        editor.putString(Store.STRING_2, field_2.getText().toString());
        editor.commit();
        savedString();
    }

    /**
     * Helper functions for Toasts
     */
    private void savedString(){
        Context context = getApplicationContext();
        CharSequence message = "Strings have been saved";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }
    private void notConnected(){
        Context context = getApplicationContext();
        CharSequence message = "Not connected with any devices";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }
    private void transmissionFail(){
        Context context = getApplicationContext();
        CharSequence message = "Error sending message to device";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    /**
     * Helper functions for rotation
     */
    private void handleRotation(int degree){
        Log.d(TAG, degree + "");
        long time = System.currentTimeMillis();

        if (degree >= 80 && degree <= 100 && (time - currentTime) >= 250){
            MapDrawer.moveRight();
            sendString(commandWrap(Cmd.DIRECTION_RIGHT));
        } else if (degree >= 260 && degree <= 280 && (time - currentTime) >= 100){
            MapDrawer.moveLeft();
            sendString(commandWrap(Cmd.DIRECTION_LEFT));
        } else if (degree >= 170 && degree <= 190 && (time - currentTime) >= 100){
            MapDrawer.moveUp();
            sendString(commandWrap(Cmd.DIRECTION_UP));
        } else{
            return;
        }
        updateRobotPositionLabel();
        findViewById(R.id.canvas_gridmap).invalidate();
        currentTime = System.currentTimeMillis();
    }

    /**
     * Helper functions to handle received message
     */
    private void handleAction(String payload){

        Log.d("Action", "Parsing " + payload);
        handleMDFString(payload);
    }

    private static String rawImgTag = "RID";
    private RawImageParser imageParser = null;
    private void handleRawImageStream(String payload) {
        Log.d(rawImgTag, "Received Img Payload: " + payload);
        imageParser.parseString(payload);
    }

    private void handleMDFString(String payload) {
        Parser parse = new Parser(payload);

        //Set Status
        boolean isStatus = parse.setStatus();
        if (isStatus){
            handleUpdateStatus(parse.getStatus());
            return;
        }

        if (!parse.getValidPayload()) return;

        handleUpdatePosition(parse.getRobotX(), parse.getRobotY(), parse.getRobotDir());

        parse.processImage();
        handleUpdateImage(parse.getlastImgID());
        MapDrawer.setGrid(parse.getExploredMap());
    }

//    private void handleUpdateMDF(String data){
//        updateRobotPositionLabel();
//    }

    private void handleUpdateImage(String imgID){

        switch (imgID) {
            case "0":
                image_content.setImageResource(R.drawable.img_0);
                break;
            case "1":
                image_content.setImageResource(R.drawable.img_1);
                break;
            case "2":
                image_content.setImageResource(R.drawable.img_2);
                break;
            case "3":
                image_content.setImageResource(R.drawable.img_3);
                break;
            case "4":
                image_content.setImageResource(R.drawable.img_4);
                break;
            case "5":
                image_content.setImageResource(R.drawable.img_5);
                break;
            case "6":
                image_content.setImageResource(R.drawable.img_6);
                break;
            case "7":
                image_content.setImageResource(R.drawable.img_7);
                break;
            case "8":
                image_content.setImageResource(R.drawable.img_8);
                break;
            case "9":
                image_content.setImageResource(R.drawable.img_9);
                break;
            case "10":
                image_content.setImageResource(R.drawable.img_10);
                break;
            case "11":
                image_content.setImageResource(R.drawable.img_11);
                break;
            case "12":
                image_content.setImageResource(R.drawable.img_12);
                break;
            case "13":
                image_content.setImageResource(R.drawable.img_13);
                break;
            case "14":
                image_content.setImageResource(R.drawable.img_14);
                break;
            case "15":
                image_content.setImageResource(R.drawable.img_15);
                break;
            default:
                image_content.setImageResource(R.drawable.img_0);
        }

    }

    private void handleUpdatePosition(int x_axis, int y_axis, String dir){
        try{
            MapDrawer.updateCoordinates(x_axis, y_axis, dir);

            if (autoModeState) {
                findViewById(R.id.canvas_gridmap).invalidate();
            }
        } catch(ClassCastException typeEx){
            Log.d(TAG, "Unable to cast data into int");
        }
        updateRobotPositionLabel();
    }

    private void handleUpdateStatus(String data){
        Log.d(TAG, "Status Update : " + data);
        TextView label_status_details = findViewById(R.id.label_status_details);
        label_status_details.setText(data);
    }

    public String commandWrap(String cmd) {
        String msg = ";{" + FROMANDROID + "\"com\":\"" + cmd + "\"}";
        return msg;
    }

//    private void handleUpdateImage(String[][] data){
//        try {
//            for (int i = 0; i < Map.COLUMN; i++) {
//                for (int j = 0; j < Map.ROW; j++) {
//                    char imgID = data[i][j].charAt(0);
//                    int x_axis = i;
//                    int y_axis = j;
//
//                    MapDrawer.updateImage(imgID, x_axis, y_axis);
//                    if (autoModeState) {
//                        findViewById(R.id.canvas_gridmap).invalidate();
//                    }
//                }
//            }
//        } catch(StringIndexOutOfBoundsException indexEx){
//            Log.d(TAG, "Unable to retrieve character (imgID)");
//        }
//    }
}
