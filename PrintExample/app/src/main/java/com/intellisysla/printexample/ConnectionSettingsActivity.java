package com.intellisysla.printexample;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ConnectionSettingsActivity extends Activity {

	//Connection Type to be specified by user
	private String connectionType;
	
	//====Connection parameters===//
	private String m_printerIPAddr; //printer's Ip address
	private int m_printerPort; // printer's port.
	
	private String m_printerBluetoothAddr; // printer's MAC address
	
	//=======UI controls variables ============// 
	TextView m_connectionLabel;
	EditText m_connectionEditText;
	TextView m_portLabel;
	EditText m_portEditText;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connection_settings);
		
		//=======Associate UI variables to activity's UI control============//
		m_connectionLabel = (TextView)findViewById(R.id.connection_prompt_label);
		m_portLabel = (TextView)findViewById(R.id.port_textView);
		m_portEditText = (EditText)findViewById(R.id.port_editText);
		m_connectionEditText = (EditText)findViewById(R.id.connection_editText);
		
		
		//===========Retreive variable's values passed in from main activity=======//
		connectionType = getIntent().getExtras().getString(DOPrintMainActivity.CONNECTION_MODE_KEY);
		m_printerIPAddr = getIntent().getExtras().getString(DOPrintMainActivity.PRINTER_IPADDRESS_KEY);
		m_printerPort = getIntent().getExtras().getInt(DOPrintMainActivity.PRINTER_TCPIPPORT_KEY);
		m_printerBluetoothAddr = getIntent().getExtras().getString(DOPrintMainActivity.BLUETOOTH_DEVICE_ADDR_KEY);
		
		
		//=======Display the proper connection parameters depending on the User's selection====//
		if (connectionType.equals("Bluetooth"))
		{
			m_connectionLabel.setText("Please enter MAC Address of printer:");
			m_connectionEditText.setHint("AB:CD:EF:11:22:33 or ABCDEF112233");
			if(!m_printerBluetoothAddr.equals("Unknown"))
				m_connectionEditText.setText(m_printerBluetoothAddr);
			m_portLabel.setVisibility(View.GONE);
			m_portEditText.setVisibility(View.GONE);
		}
		else if(connectionType.equals("TCP/IP"))
		{
			m_connectionLabel.setText("Please enter IP Address of printer:");
			m_connectionEditText.setHint("192.168.99.1");
			if(!m_printerIPAddr.equals("0.0.0.0") && m_printerPort != 0)
			{
				m_connectionEditText.setText(m_printerIPAddr);
				m_portEditText.setText(Integer.toString(m_printerPort));
			}
			m_portLabel.setVisibility(View.VISIBLE);
			m_portEditText.setVisibility(View.VISIBLE);
		}
		
		//==========================================================
		//handler for the save button click
		//==========================================================
		Button SavetButton = (Button) this.findViewById(R.id.save_button);
		SavetButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) 
			{
				try {
					// create new intent
					Intent intent = new Intent();

					//Pass connection information back to main activity based on connection
					if(connectionType.equals("Bluetooth"))
					{
						m_printerBluetoothAddr = m_connectionEditText.getText().toString().toUpperCase(Locale.US);
						//validate if its a MAC address
	        			Pattern pattern = Pattern.compile("[0-9A-Fa-f]{12}");
	        			Matcher matcher = pattern.matcher(m_printerBluetoothAddr);
	        			if(matcher.matches())
	        				m_printerBluetoothAddr = formatBluetoothAddress(m_printerBluetoothAddr);
	      
	        			if(!BluetoothAdapter.checkBluetoothAddress(m_printerBluetoothAddr))
	        				throw new Exception("Invalid Bluetooth Address format");
						intent.putExtra(DOPrintMainActivity.BLUETOOTH_DEVICE_ADDR_KEY, m_printerBluetoothAddr);
					}
					else if (connectionType.equals("TCP/IP"))
					{

						m_printerIPAddr = m_connectionEditText.getText().toString();
						//validate if its a IP Address
	        			Pattern pattern = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        			        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        			        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        			        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
	        			Matcher matcher = pattern.matcher(m_printerIPAddr);
	        			if(!matcher.matches())
	        				throw new Exception("Invalid IP Address format");
						m_printerPort = Integer.parseInt(m_portEditText.getText().toString());
						
						intent.putExtra(DOPrintMainActivity.PRINTER_IPADDRESS_KEY,m_printerIPAddr);
						intent.putExtra(DOPrintMainActivity.PRINTER_TCPIPPORT_KEY, m_printerPort);
					}
					// return the value back to the caller
					setResult(RESULT_OK, intent);

					// close this Activity
					finish();
				}
				//=====Error handling============//
				catch(NumberFormatException nfe)
				{
					nfe.printStackTrace();
					AlertDialog.Builder builder = new AlertDialog.Builder(ConnectionSettingsActivity.this);
			        builder.setTitle("Application Error")
			        .setMessage("Invalid port format. Please re-enter the port")
			        .setCancelable(false)
			        .setNegativeButton("Close",new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			            }
			        });
			        AlertDialog alert = builder.create();
			        alert.show();
				}
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					AlertDialog.Builder builder = new AlertDialog.Builder(ConnectionSettingsActivity.this);
			        builder.setTitle("Application Error")
			        .setMessage(e.getMessage())
			        .setCancelable(false)
			        .setNegativeButton("Close",new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			            }
			        });
			        AlertDialog alert = builder.create();
			        alert.show();	
				}
			}
		});

		//=================================
		//handler for the cancel button click
		//=====================================
		Button CancelButton = (Button) this.findViewById(R.id.cancel_button);
		CancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED, null);
				finish();
			}
		});		
		
		
		
	}
	
	/** Converts Bluetooth Address string from 00ABCDEF0102 format => 00:AB:CD:EF:01:02 format
	 * @param bluetoothAddr - Bluetooth Address string to convert
	 */
	public String formatBluetoothAddress(String bluetoothAddr)
    {
        //Format MAC address string
        StringBuilder formattedBTAddress = new StringBuilder(bluetoothAddr);
        for (int bluetoothAddrPosition = 2; bluetoothAddrPosition <= formattedBTAddress.length() - 2; bluetoothAddrPosition += 3)
            formattedBTAddress.insert(bluetoothAddrPosition, ":");
        return formattedBTAddress.toString();
    }
	

}
