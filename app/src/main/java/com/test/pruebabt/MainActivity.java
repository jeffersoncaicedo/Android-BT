package com.test.pruebabt;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {


    Button btnciclo, btncon, btnsalir;
    TextView textinfo;

    //Identificador de servicio
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;

    //Si se apreta una vez el boton de conectar
    boolean estado = false;

    //Handler es un control para mensajes
    Handler bluetoothIn;

    //Estado del manejador
    final int handlerState = 0;

    //Esto es simplemente un String normal a diferencia que al agregar una sentancia en un bucle se agrega los espacios automaticamente
    //for(hasta 20 veces)
    //String cadena += " " + "Dato" ---> En un string normal se debe crear el espacio y luego agregar el dato
    //Con esto se traduce a = DataStringIN.append(dato);
    private StringBuilder DataStringIN = new StringBuilder();

    //Llama a la sub- clase y llamara los metodos que se encuentran dentro de esta clase
    ConexionThread MyConexionBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnciclo = findViewById(R.id.btnciclo);
        btncon = findViewById(R.id.btncon);
        btnsalir = findViewById(R.id.btnsalir);
        textinfo = findViewById(R.id.textinfo);

        ////////////////Manejador de mensajes y llamara al metodo Run///////////////////////////////
        bluetoothIn = new Handler(){
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    textinfo.setText("CICLO DETENIDO");
                    DataStringIN.append(readMessage);

                    int endOfLineIndex = DataStringIN.indexOf("#");

                    if (endOfLineIndex > 0) {
                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        //   Toast.makeText(MainActivity.this, "Dato Recibido: " +dataInPrint, Toast.LENGTH_SHORT).show();
                        DataStringIN.delete(0, DataStringIN.length());
                    }
                }
            }

        };
        ///////////////////////////////////////////////////

        btnciclo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (estado) {
                    String dato = "1";
                    MyConexionBT.write(dato);
                    textinfo.setText("EMPIEZA  EL CICLO");
                    Toast.makeText(MainActivity.this, "Inicio del ciclo", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Solo se puede enviar datos si el dispositivo esta vinculado", Toast.LENGTH_SHORT).show();
                }
            }

        });

        btncon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btAdapter = BluetoothAdapter.getDefaultAdapter();

                //Direccion mac del dispositivo a conectar
                BluetoothDevice device = btAdapter.getRemoteDevice("B8:27:EB:98:97:14");

                try {
                    //Crea el socket sino esta conectado
                    if (!estado) {
                        btSocket = createBluetoothSocket(device);
                        estado = btSocket.isConnected();
                    }

                } catch (IOException e) {
                    Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
                }

                // Establece la conexión con el socket Bluetooth.
                try {
                    //Realiza la conexion si no se a hecho
                    btSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                    if (!estado) {
                        btSocket.connect();
                        estado = true;
                        MyConexionBT = new ConexionThread(btSocket);
                        MyConexionBT.start();
                        Toast.makeText(MainActivity.this, "Conexion Realizada Exitosamente", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Ya esta vinculado", Toast.LENGTH_SHORT).show();

                    }
                } catch (IOException | NoSuchMethodException e) {
                    try {
                        Toast.makeText(MainActivity.this, "Error:", Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        btSocket.close();
                    } catch (IOException e2) {
                    }
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        });

        btnsalir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                System.exit(0);
            }
        });
    }

    //Crea el socket
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createInsecureRfcommSocketToServiceRecord(BTMODULEUUID);
    }


    //Se debe crear una sub-clase para tambien heredar los metodos de CompaActivity y Thread juntos
    //Ademas  en Run se debe ejecutar el subproceso(interrupcion)
    private class ConexionThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConexionThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                // Se mantiene en modo escucha para determinar el ingreso de datos
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //Enviar los datos
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e)
            {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                //finish();
            }
        }

    }

}