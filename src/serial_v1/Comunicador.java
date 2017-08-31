/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package serial_v1;
import gnu.io.*;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TooManyListenersException;

public class Comunicador implements SerialPortEventListener
{
   
    InterfazGrafica window = null; //crea el objeto grafico (trae interfaz)
    int bauds = 9600;
    String valor;
    public void setBauds(int bauds) {
        this.bauds = bauds;
    }

    //para contar los puertos encontrados
    private Enumeration ports = null;
    //hace una relación entre los puertos encontrados y sus nombres
    private HashMap portMap = new HashMap();

    //este objeto contien el puerto abierto
    private CommPortIdentifier selectedPortIdentifier = null;
    private SerialPort serialPort = null;
    
    //streams para enviar y recibir data por el puerto
    private InputStream input = null;
    private OutputStream output = null;

    //bandera booleana que avisa si el puerto esta conectado
    private boolean bConnected = false;

    //valor del timeout
    final static int TIMEOUT = 2000;

    //some ascii values for for certain things
    final static int SPACE_ASCII = 32;
    final static int DASH_ASCII = 45;
    final static int NEW_LINE_ASCII = 10;

    //string que va a escribir la data del puerto en la parte grafica
    String logText = "";

    public Comunicador(InterfazGrafica window)
    {
        this.window = window;
    }

    //busqueda de puertos
    
    //agrega todo los puertos disponibles al jcombo de la interfaz grafica
    public void searchForPorts()
    {
        ports = CommPortIdentifier.getPortIdentifiers();

        while (ports.hasMoreElements())
        {
            CommPortIdentifier curPort = (CommPortIdentifier)ports.nextElement();

            //obtiene solo puertos serie
            if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL)
            {
                window.cboxPorts.addItem(curPort.getName());
                portMap.put(curPort.getName(), curPort);
            }
        }
    }

    //conoectar al puerto seleccionado en la jcombo (puertos disponibles)
    //pre: los puertos ya se encontraron usando searchForPorts()
    //post:el puerto conectado es guardado en la variable commPOrt
    //sino exception
    public void connect()
    {
        String selectedPort = (String)window.cboxPorts.getSelectedItem();
        selectedPortIdentifier = (CommPortIdentifier)portMap.get(selectedPort);

        CommPort commPort = null;

        try
        {
            //esta llamada devuelve un objeto del tipo puerto
            commPort = selectedPortIdentifier.open("TigerControlPanel", TIMEOUT);
            //que puede ser casteada a puerto serial
            serialPort = (SerialPort)commPort;
            serialPort.setSerialPortParams(this.bauds, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
           
            setConnected(true);

            //avisa si esta todo bien en la interfaz
            logText = selectedPort + " opened successfully.";
            window.txtLog.setForeground(Color.black);
            window.txtLog.append(logText + "\n");

           
           
        }
        catch (PortInUseException e)
        {
            logText = selectedPort + " en uso. (" + e.toString() + ")";
            
            window.txtLog.setForeground(Color.RED);
            window.txtLog.append(logText + "\n");
        }
        catch (Exception e)
        {
            logText = "falla al abrir puerto " + selectedPort + "(" + e.toString() + ")";
            window.txtLog.append(logText + "\n");
            window.txtLog.setForeground(Color.RED);
        }
    }

    //abriri in y out streams
    //antes: necesito un puerto abierto
    //despues: usar los streams para comunicarse con el puerto
    public boolean initIOStream()
    {
        //avisa si se abrio bien o no el puerto
        boolean successful = false;

        try {
            //
            input = serialPort.getInputStream();
            output = serialPort.getOutputStream();
            writeData("0");
            
            successful = true;
            return successful;
        }
        catch (IOException e) {
            logText = "I/O Streams failed to open. (" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
            return successful;
        }
    }

    //arrancamos los listener que nos van a avisar si tenemos data disponible en el puerto
    //antes: tenemos que tener el puerto abierto
    //despues: un listener que me diga que tipo de data llego
    public void initListener()
    {
        try
        {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        }
        catch (TooManyListenersException e)
        {
            logText = "Demasiados listeners. (" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
    }

    //desconectar puerto serie
    //antes: un puerto abierto
    //despues: puerto cerrado
    public void disconnect()
    {
        //cierro puerto
        try
        {
            writeData("0");

            serialPort.removeEventListener();
            serialPort.close();
            input.close();
            output.close();
            setConnected(false);
            

            logText = "Disconnected.";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
        catch (Exception e)
        {
            logText = "Failed to close " + serialPort.getName() + "(" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
    }

    final public boolean getConnected()
    {
        return bConnected;
    }

    public void setConnected(boolean bConnected)
    {
        this.bConnected = bConnected;
    }

    //lo que sucede cuando hay data disponible
    //antes: se diparo un evento de puerto serie
    //despues: se procesa la data recibida 
    public void serialEvent(SerialPortEvent evt) {
        if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE)
        {
            try
            {
                byte singleData = (byte)input.read();

                if (singleData != NEW_LINE_ASCII)
                {
                   logText = new String(new byte[] {singleData});
                   window.txtLog.append(logText);
                   valor = valor + logText;
                    
                }
                else
                {
                    Calendar calendario = new GregorianCalendar();
                    int hora, minutos, segundos;
                    hora =calendario.get(Calendar.HOUR_OF_DAY);
                    minutos = calendario.get(Calendar.MINUTE);
                    segundos = calendario.get(Calendar.SECOND);
                    int ss = segundos+(minutos*60)+(hora*3600);
                    double aux =0;
                    if (isNumeric(valor)){
                        aux = Double.parseDouble(valor);
                        window.temp.add(ss, aux);
                        
                    }
                    valor = "";
                    window.txtLog.append("\n");
                }
            }
            catch (Exception e)
            {
                logText = "Failed to read data. (" + e.toString() + ")";
                window.txtLog.setForeground(Color.red);
                window.txtLog.append(logText + "\n");
            }
        }
    }

    //este es el metodo para mandar data
    //antes: un puerto serie abierto
    //Despues: data enviada al dispositivo
    public void writeData(String data)
    {
        try
        {
            output.write(data.getBytes());//tengo que mandar el string como sucesion de bytes
            output.flush();
            
            output.write(DASH_ASCII);
            output.flush();
          
            
            output.write(SPACE_ASCII);
            output.flush();
        }
        catch (Exception e)
        {
            logText = "Falla al escribir data. (" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
    }
    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
