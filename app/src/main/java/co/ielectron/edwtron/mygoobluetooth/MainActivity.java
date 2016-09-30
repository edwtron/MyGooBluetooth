/*Programa Ejemplo para el uso y configuracion del Bluetooth

Uso Básico del  Bluetooh para enviar y recibir datos hacia un modulo Bluetooth HC-06 conectado
a un Arduino Leonardo por el puerto serial.

<=============================================>
El HC-06 tiene 4 pines (Módulo Bluetooth)

 ---------------
|               |
| Lado de       |
|Componentes    |
|               |
 ---------------
  |   |   |   |
  1   2   3   4

 1 RX
 2 TX
 3 GND
 4 VCC +5 VDC
 <=============================================>


*
* 1 Configuramos los permisos en el Manifest
*   <uses-permission android:name="android.permission.BLUETOOTH" />
*
* 2 Verificamos que el dispositivo soporte el BT
*
* 3 Si tenemos, verificamos que este encendido y si no,  lo encendemos
*
* 4 Buscamos dispositivos BT
*
* 5 Conectamos dispositivo con el movil
*
* 6 Enviamos DATOS
* */
package co.ielectron.edwtron.mygoobluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    List<String> deviceList = new ArrayList<String>();
    private String macBT;

    private ListView listView;
    private Button btnConectar;
    private Button btnEnviar;
    private Button btnOn;
    private Button btnOff;
    private EditText txtMensaje;


    private BroadcastReceiver mReceiver;

    private static final String TAG = "My Bluetooth Google ";

    //Intent Request Codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private final String NAME_BLUETOOTH_DEVICE = "EDWTRON";
    private boolean connectionState = false;

    private List<String> mArrayAdapter;
    private ArrayList<BluetoothDevice> arrayDevices = new ArrayList<BluetoothDevice>();
    private BluetoothDevice myBT;
    ConectarBtArduino tarea = new ConectarBtArduino();
    OutputStream tmpOutputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        btnConectar = (Button) findViewById(R.id.btnConectar);
        btnEnviar = (Button) findViewById(R.id.btnEnviar);
        btnOn = (Button) findViewById(R.id.btnOn);
        btnOff = (Button) findViewById(R.id.btnOff);
        txtMensaje = (EditText) findViewById(R.id.txtMensaje);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();        //Devuelve un Array con los adaptadores Bluetooht disponibles

        if (tengoBT()) {
            activarBT();        //Activa el Bluetooth del Movil
            //Toast.makeText(this, "Consola del BT: No tenemos BT", Toast.LENGTH_LONG).show();

        }else{
            //Log.d(TAG, "BT No tenemos BT");
            Toast.makeText(this, "No tenemos BT disponible", Toast.LENGTH_LONG).show();
        }

        // Realizo la Conexion
        btnConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                conectarBT();
            }
        });

        //Envio Datos
        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enviarDatosBT();
            }
        });

        //Apago LED
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] b = "1".getBytes();
                try {
                    tmpOutputStream.write(b);
                }catch (IOException e){}
            }
        });

        //Enciendo LED
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] b = "2".getBytes();
                try {
                    tmpOutputStream.write(b);
                }catch (IOException e){}
            }
        });

    }//fin OnCreate

    //comprueba si hay conexion y envia datos al BT
    public void enviarDatosBT(){
        if(connectionState){

            String msg;//= "nada es mucho";
            if(TextUtils.isEmpty(txtMensaje.getText().toString())){

                Toast.makeText(getApplicationContext(), "campo vacio", Toast.LENGTH_LONG).show();
            }else {
                msg = txtMensaje.getText().toString() + "\n";
                byte[] b = msg.getBytes();

                try {
                    tmpOutputStream.write(b);
                }catch (IOException e){}
            }


        }else{
            Toast.makeText(getApplicationContext(), "Dispositivos NO conectados", Toast.LENGTH_LONG).show();
        }
    }

    //Conectar el dispositivo con el Bluetooth conectado al Arduino
    public void conectarBT(){
        //Toast.makeText(getApplicationContext(), "Conectando...", Toast.LENGTH_LONG).show();
        tarea.execute(myBT);
    }

    //Activamos el Bluetooth
    public void activarBT(){
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            Log.d(TAG, "BT ya estaba activado");
            //Toast.makeText(this, "Consola del BT: BT Ya estaba Activado", Toast.LENGTH_LONG).show();
            buscarPairedBT();
        }
    }

    //comprobamos que el movil tenga disponible Bluetooth
    //Retorna False si no tiene Bluetooth y True si lo tiene
    public boolean tengoBT(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d(TAG, "BT no tenemos");
            //Toast.makeText(this, "Consola del BT: No tenemos BT", Toast.LENGTH_LONG).show();
            return false;
        }else{
            return true;
        }
    }

    //Despues de que pide activar el Bluetooth del dispositivo
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    //connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    //connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    //Toast.makeText(this, "Consola del BT: BT Activado 2", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "BT  enabled 2");
                    buscarPairedBT();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    //Toast.makeText(this, "Consola del BT: BT No Activado", Toast.LENGTH_LONG).show();
                    //getActivity().finish();
                }
        }
    }

    public void buscarPairedBT(){

        //Toast.makeText(this, "Consola del BT: Devices: " , Toast.LENGTH_LONG).show();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            //Toast.makeText(this, "Consola del BT: Devices: " + pairedDevices.size() , Toast.LENGTH_LONG).show();
            for (BluetoothDevice device : pairedDevices) {
                arrayDevices.add(device);
                if(device.getName().equals(NAME_BLUETOOTH_DEVICE)){
                    macBT = device.getAddress();
                    myBT = device;

                }else {
                    macBT = "noMacBT";
                }
                deviceList.add("Name: " + device.getName() + "\n" + "Address: " + device.getAddress());
                listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList));
                //Toast.makeText(this, "Consola del BT: Devices: " + device.getName() + device.getAddress(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "Consola del BT: Devices: " + device.getName() + device.getAddress());
            }
        }
    }


    //subclase para crear tarea en segundo plano
    //Extiende Asynctask
    //Los parametros son:
    // 1 Parametro de entrada para el metodo doInBackground
    // 2 Parametros para los metodos publishProgress() y onProgressUpdate()
    // 3 Valor de retorno del doInBackground
    public class ConectarBtArduino extends AsyncTask<BluetoothDevice , Void, Void>{

        private static final String UUID_SERIAL_PORT_PROFILE = "00001101-0000-1000-8000-00805F9B34FB";
        private BluetoothSocket mSocket = null;
        private BluetoothDevice mDevice = null;


        @Override
        protected Void doInBackground(BluetoothDevice... device) {

            mDevice = device[0];

            try {
                //mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(UUID_SERIAL_PORT_PROFILE));
                mSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(UUID_SERIAL_PORT_PROFILE)); //Socket hacia el BT
                mSocket.connect();                              //Realizo la conexion
                tmpOutputStream = mSocket.getOutputStream();    //Stream de datos hacia el BT
                connectionState = true;  //Para Evitar enviar datos sin haber realizado la conexion
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Toast.makeText(getApplicationContext(), "Conexion Realizada!",
                    Toast.LENGTH_SHORT).show();

        }
    }

}

