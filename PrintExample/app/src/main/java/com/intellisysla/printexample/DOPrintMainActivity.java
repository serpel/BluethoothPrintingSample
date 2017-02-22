package com.intellisysla.printexample;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import datamaxoneil.connection.ConnectionBase;
import datamaxoneil.connection.Connection_Bluetooth;
import datamaxoneil.connection.Connection_TCP;
import datamaxoneil.printer.DocumentDPL;
import datamaxoneil.printer.DocumentEZ;
import datamaxoneil.printer.DocumentLP;
import datamaxoneil.printer.ParametersDPL;
import datamaxoneil.printer.DocumentDPL.ImageType;
import datamaxoneil.printer.ParametersDPL.*;
import datamaxoneil.printer.ParametersEZ;
import datamaxoneil.printer.UPSMessage;
import datamaxoneil.printer.configuration.dpl.*;
import datamaxoneil.printer.configuration.dpl.MemoryModules_DPL.FileInformation;
import datamaxoneil.printer.configuration.ez.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.intellisysla.printexample.Entities.Factura;
import com.intellisysla.printexample.Entities.Item;

@SuppressLint("DefaultLocale")
public class DOPrintMainActivity extends Activity implements Runnable {

	//Keys to pass data to/from FileBrowseActivity
		static final String FOLDER_NAME_KEY = "com.datamaxoneil.doprint.Folder_Name_Key";
		static final String FOLDER_PATH_KEY = "com.datamaxoneil.doprint.Folder_Path_Key";
		
		//Keys to pass data to Connection Activity
		static final String CONNECTION_MODE_KEY = "com.datamaxoneil.doprint.Connection_Mode_Key";
		static final String PRINTER_IPADDRESS_KEY = "com.datamaxoneil.doprint.PRINTER_IPAddress_Key";
		static final String PRINTER_TCPIPPORT_KEY = "com.datamaxoneil.doprint.PRINTER_TCPIPPort_Key";
		static final String BLUETOOTH_DEVICE_NAME_KEY = "com.datamaxoneil.doprint.PRINTER_Bluetooth_Device_Name_Key";
		static final String BLUETOOTH_DEVICE_ADDR_KEY = "com.datamaxoneil.doprint.PRINTER_Bluetooth_Device_Addr_Key";
		
		//Variable for folder content
		private String m_selectedPath;
		
		//Variable for Connection information
		private String m_printerAddress = "Unknown";
		private int m_printerPort = 515;
		private String connectionType;
		ConnectionBase conn = null;
		
		//Variable for connection status
		private String g_PrintStatusStr;
		
		ArrayAdapter<CharSequence> adapter = null;
		
		//array to contain the filenames inside a directory
		List<String> filesList = new ArrayList<String>();
		
		static final int CONFIG_CONNECTION_REQUEST = 0; // for Connection Settings
		private static final int REQUEST_PICK_FILE = 1; //for File browsing

		//Document and Parameter Objects
		private DocumentEZ docEZ = new DocumentEZ("MF204");
		private DocumentLP docLP = new DocumentLP("!");
		private DocumentDPL docDPL = new DocumentDPL();

		private ParametersEZ paramEZ = new ParametersEZ();
		private ParametersDPL paramDPL = new ParametersDPL();
		// use to update the UI information.
		private Handler m_handler = new Handler(); // Main thread
			
		private String m_printerMode = "";
		private int selectedItemIndex;
		
		//printerMode Key
		static final String PRINTERMODE = "com.datamaxoneil.PrinterMode_Key";
		
		//====UI Controls========//
		//Buttons
		Button m_browseButton;
		Button m_printButton;
		Button m_saveButton;
		Button m_configConnectionButton;
		RadioGroup m_performTaskRadioGroup; 
		RadioButton m_printRadioButton;
		RadioButton m_queryRadioButton;
		
		//EditText
		TextView m_connectionInfoStatus; 
		TextView m_actionTextView;
		
		//Spinners
		Spinner m_connectionSpinner;
		Spinner m_printerModeSpinner;
		Spinner m_printItemsSpinner;
		
		byte[] fileData = {0};
		byte[] printData = {0};
		
		// Configuration files to load
		String TCPIPConfigFilename = "tcpipconfig.dat";
		String ApplicationConfigFilename = "applicationconfig.dat";
		
		DOPrintSettings g_appSettings = new DOPrintSettings("", 0, "/", "", 0,0,0,0);
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_doprint_main);
		
		DOPrintSettings appSettings = ReadApplicationSettingFromFile();
		if(appSettings != null)
			g_appSettings = appSettings;
		
		//Get parameters from application settings
		m_printerAddress = g_appSettings.getPrinterAddress();
		m_printerPort = g_appSettings.getPrinterPort();
		m_selectedPath = g_appSettings.getSelectedFilePath();
		selectedItemIndex = g_appSettings.getSelectedItemIndex();
		
		//======Mapping UI controls from our activity xml===========// 
		m_connectionInfoStatus= (TextView)findViewById(R.id.communication_status_information);
		m_connectionSpinner = (Spinner)findViewById(R.id.connection_spinner);
		m_configConnectionButton = (Button)findViewById(R.id.configConn_button);
		m_printerModeSpinner = (Spinner)findViewById(R.id.printer_mode_spinner);
		m_performTaskRadioGroup = (RadioGroup)findViewById(R.id.performTaskRadioGroup);
		m_printRadioButton= (RadioButton)findViewById(R.id.print_radioButton);
		m_queryRadioButton = (RadioButton)findViewById(R.id.query_radioButton);
		m_printItemsSpinner = (Spinner)findViewById(R.id.print_items_spinner);
		m_browseButton = (Button)findViewById(R.id.browse_button);
		m_printButton = (Button)findViewById(R.id.print_button);
		m_actionTextView = (TextView)findViewById(R.id.actionMode);
		m_saveButton = (Button)findViewById(R.id.saveSettings_button);
		
		if(g_appSettings.getSelectedAction() == 0)
		{
			m_printRadioButton.setChecked(true);
			m_browseButton.setVisibility(View.VISIBLE);
			m_printButton.setText("Print");
		}
		else if (g_appSettings.getSelectedAction() == 1)
		{
			m_queryRadioButton.setChecked(true);
			m_browseButton.setVisibility(View.GONE);
			m_printButton.setText("Query");
		}
		m_connectionSpinner.setSelection(g_appSettings.getCommunicationMethod());
		m_printerModeSpinner.setSelection(g_appSettings.getSelectedModeIndex());
		m_printItemsSpinner.setSelection(selectedItemIndex);
		// ------------------------------------------------
		// Event handler when user select communication method
		// -------------------------------------------------
		m_connectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					connectionType = m_connectionSpinner.getSelectedItem().toString();
					
					//=======Display correct connection information when user selects connection=====//
					if (connectionType.equals("TCP/IP"))
					{
						if(m_printerAddress.contains(":"))
						{
							m_connectionInfoStatus.setText("Connection not configured for TCP/IP. Please configure connection settings.");
						}
						else if (m_printerAddress.length() == 0)
						{
							m_connectionInfoStatus.setText("Connection settings not configured. Please configure connection settings.");
						}
						else {
							m_connectionInfoStatus.setText("Printer's IP Address/Port: "+ m_printerAddress+ ":"+ Integer.toString(m_printerPort));
						}
					}
					
					else if(connectionType.equals("Bluetooth"))
					{
						if(m_printerAddress.contains("."))
						{
							m_connectionInfoStatus.setText("Connection not configured for Bluetooth. Please configure connection settings.");
						}
						else if (m_printerAddress.length() == 0)
						{
							m_connectionInfoStatus.setText("Connection settings not configured. Please configure connection settings.");
						}
						else {
							m_connectionInfoStatus.setText("Printer's MAC Address: "+ m_printerAddress);
						}
					}
					g_appSettings.setCommunicationMethod(m_connectionSpinner.getSelectedItemPosition());
					
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					connectionType = m_connectionSpinner.getSelectedItem().toString();
					//=======Display correct connection information when user selects connection=====//
					if (connectionType.equals("TCP/IP"))
					{
						if(m_printerAddress.contains(":"))
						{
							m_connectionInfoStatus.setText("Connection not configured for TCP/IP");
						}
						else {
							m_connectionInfoStatus.setText("Printer's IP Address/Port: "+ m_printerAddress+ ":"+ Integer.toString(m_printerPort));
						}
					}
					
					else if(connectionType.equals("Bluetooth"))
					{
						if(m_printerAddress.contains("."))
						{
							m_connectionInfoStatus.setText("Connection not configured for Bluetooth");
						}
						else {
							m_connectionInfoStatus.setText("Printer's MAC Address: "+ m_printerAddress);
						}
					}
					g_appSettings.setCommunicationMethod(m_connectionSpinner.getSelectedItemPosition());
				}
		});
		
		// ------------------------------------------------
		// Handles when user presses connection config button
		// -------------------------------------------------
		m_configConnectionButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//==================Open Connection Configuration Activity=======================================//
				Intent connSettingsIntent=new Intent(getApplicationContext(), ConnectionSettingsActivity.class);
				Spinner connectionSpinner = (Spinner)findViewById(R.id.connection_spinner);
				String connectionType = connectionSpinner.getSelectedItem().toString();
				connSettingsIntent.putExtra(CONNECTION_MODE_KEY, connectionType);
				
				connSettingsIntent.putExtra(PRINTER_IPADDRESS_KEY,m_printerAddress);
				connSettingsIntent.putExtra(PRINTER_TCPIPPORT_KEY, m_printerPort);
				connSettingsIntent.putExtra(BLUETOOTH_DEVICE_ADDR_KEY, m_printerAddress);
				
				startActivityForResult(connSettingsIntent, CONFIG_CONNECTION_REQUEST);
				
			}
		});

		// ------------------------------------------------
		// Event handler when user select printer mode
		// -------------------------------------------------
		m_printerModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) 
			{
				
				//=======Display correct printer mode information when user selects connection=====//
				m_printerMode=m_printerModeSpinner.getSelectedItem().toString();
				String selectedItem= "";
				if(m_printItemsSpinner.getSelectedItem() != null)
					selectedItem= m_printItemsSpinner.getSelectedItem().toString();
				
				adapter = null;
				//Check which mode we are in
				if(m_printerMode.equals("EZ"))
				{ 
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_EZ);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_Legacy);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				else if (m_printerMode.equals("LP"))
				{
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_LP);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_Legacy);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				else if (m_printerMode.equals("DPL"))
				{
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_DPL);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_DPL);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				
				else if (m_printerMode.equals("ExPCL Line Print"))
				{
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_ExPCL_LP);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_ExPCL);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				
				else if (m_printerMode.equals("ExPCL Page Print"))
				{
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_ExPCL_PP);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_ExPCL);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				
				if (m_printRadioButton.isChecked()) {
					g_appSettings.setSelectedAction(0);
					
					//add file names
					for (String fileName : filesList) {
						adapter.add((CharSequence)fileName);
					}
					adapter.notifyDataSetChanged();
				}
				else if(m_queryRadioButton.isChecked())
				{
					g_appSettings.setSelectedAction(1);
				}
				
				if (selectedItem.length() !=0)
				{
					g_appSettings.setSelectedItemIndex(adapter.getPosition(selectedItem));
				}
				else {
					g_appSettings.setSelectedItemIndex(m_printItemsSpinner.getSelectedItemPosition());
				}
				m_printItemsSpinner.setSelection(g_appSettings.getSelectedItemIndex());
			}
			
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				m_printerMode=m_printerModeSpinner.getSelectedItem().toString();
				g_appSettings.setSelectedItemIndex(m_printerModeSpinner.getSelectedItemPosition());
				m_printItemsSpinner.setSelection(g_appSettings.getSelectedItemIndex());
				
				if (m_printRadioButton.isChecked()) {
					g_appSettings.setSelectedAction(0);
					
					//add file names
					for (String fileName : filesList) {
						adapter.add((CharSequence)fileName);
					}
					adapter.notifyDataSetChanged();
				}
				else if(m_queryRadioButton.isChecked())
				{
					g_appSettings.setSelectedAction(1);
				}
			}
		});
		
		// --------------------------------------------------
		// Event handler when user selects an item from list
		// --------------------------------------------------
		m_printItemsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				g_appSettings.setSelectedItemIndex(m_printItemsSpinner.getSelectedItemPosition());
				selectedItemIndex = m_printItemsSpinner.getSelectedItemPosition();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				g_appSettings.setSelectedItemIndex(m_printItemsSpinner.getSelectedItemPosition());
				selectedItemIndex = m_printItemsSpinner.getSelectedItemPosition();
			}
			
		});
		
		// ------------------------------------------------
		// Event handler when user select what task to perform(eg. print or query
		// -------------------------------------------------
		m_performTaskRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub

				m_printerMode=m_printerModeSpinner.getSelectedItem().toString();
				adapter = null;
				
				//Hide or show browse button
				if (m_printRadioButton.isChecked()) {
					m_browseButton.setVisibility(View.VISIBLE);
					m_actionTextView.setText("Select an item to print:");
					m_printButton.setText("Print");
				}
				else if (m_queryRadioButton.isChecked()){
					m_browseButton.setVisibility(View.GONE);
					m_actionTextView.setText("Select an item to query:");
					m_printButton.setText("Query");
				}
				//Check which mode we are in
				if(m_printerMode.equals("EZ"))
				{ 
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_EZ);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_Legacy);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				else if (m_printerMode.equals("LP"))
				{
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_LP);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_Legacy);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				else if (m_printerMode.equals("DPL"))
				{
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_DPL);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_DPL);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				
				else if (m_printerMode.equals("ExPCL Line Print"))
				{
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_ExPCL_LP);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_ExPCL);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				
				else if (m_printerMode.equals("ExPCL Page Print"))
				{
					if (m_printRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.print_list_ExPCL_PP);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
					else if (m_queryRadioButton.isChecked()) {
						CharSequence[] itemArray = getResources().getTextArray(R.array.query_list_ExPCL);
						List<CharSequence> itemList = new ArrayList<CharSequence>(Arrays.asList(itemArray));
						adapter = new ArrayAdapter<CharSequence>(DOPrintMainActivity.this,android.R.layout.simple_spinner_item,itemList);						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						m_printItemsSpinner.setAdapter(adapter);
					}
				}
				if (m_printRadioButton.isChecked()) {
					g_appSettings.setSelectedAction(0);
					
					//add file names
					for (String fileName : filesList) {
						adapter.add((CharSequence)fileName);
					}
					adapter.notifyDataSetChanged();
				}
				else if(m_queryRadioButton.isChecked())
				{
					g_appSettings.setSelectedAction(1);
				}
				m_printItemsSpinner.setSelection(selectedItemIndex);
			}
		});
		
		//--------------------------------------------------------------------------
		//Handle Browse Button Click - Browse file content/image path into data to print.
		//----------------------------------------------------------------------------
		m_browseButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				//==========Start file browsing activity==================//
				Intent intent = new Intent("com.datamaxoneil.doprint.FileBrowseActivity");
				startActivityForResult(intent,REQUEST_PICK_FILE);
				
			}
		});
		
		//--------------------------------------
		//Handle Print/Send Function
		//--------------------------------------
		m_printButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub			
				try {
					selectedItemIndex = m_printItemsSpinner.getSelectedItemPosition();
					//if we are printing
					if (m_printRadioButton.isChecked()) {
						
						//Checks current Mode
						if(m_printerMode.equals("EZ"))
						{	
							docEZ.clear();
							//3-in sample
							if (selectedItemIndex == 0) 
							{

								Factura factura = new Factura();
								factura.setEmpresa("VAN HEUSEN DE C.A.");
								factura.setEmp_direccion("Col. San Fernando, Ave Juan Pablo II Frente a la Leyde, SPS Cortes");
								factura.setEmp_telefono("25160100");
								factura.setEmp_rtn("05019995143200");
								factura.setFecha("22/02/2017");
								factura.setCodigoCliente("C0001");
								factura.setNombreCliente("Union Moda");
								factura.setDireccion("BARRIO LOUNDRES, CARRETERA PANAMERICANA CONTIGO A GAS P");
								factura.setTelefono("2424-7077");
								factura.setVendedor("Sergio Peralta");
								factura.setSubtotal(510.00);
								factura.setDescuento(0.0);
								factura.setIsv(76.50);
								factura.setTotal(586.50);

								Item item = new Item("BF6CACR02-560-015", 1, 419.75);
								Item item2 = new Item("CC001-CLL-165", 1, 166.75);

								List<Item> items = new ArrayList<Item>();
								items.add(item);
								items.add(item2);

								factura.setItems(items);

								//=============GENERATING RECEIPT====================================//
			                    docEZ.writeText(factura.getEmpresa(), 1, 200);
								docEZ.writeText(factura.getDireccion(), 1, 200);
								docEZ.writeText("Telefono:" + factura.getEmp_telefono(), 1, 200);
			                    docEZ.writeText("RTN: " +factura.getEmp_rtn(),50,1);
			                    docEZ.writeText("Fecha: " + factura.getFecha(),75,1);
			                    docEZ.writeText("Cliente:" + factura.getCodigoCliente(),100,1);
			                    docEZ.writeText(factura.getNombreCliente(),125,1);
			                    docEZ.writeText("Telefono:" + factura.getTelefono(),150,1);
								docEZ.writeText("RTN: " + factura.getEmp_rtn(),150,1);
								docEZ.writeText("Vendedor: " + factura.getVendedor(),150,1);
								docEZ.writeText("PRODUCTO" ,205,1);
								docEZ.writeText("PRODUCT CODE  PRODUCT DESCRIPTION         QTY.  Delivr." ,205,1);
								docEZ.writeText("-------------------------------------------------",230,1);
								for(Item i: factura.getItems()){
									docEZ.writeText(i.getSKU() + "	" + String.valueOf(i.getCantidad()) + "	" + String.valueOf(i.getPrecio()), 200, 1);
								}
								docEZ.writeText("-------------------------------------------------",230,1);
								docEZ.writeText("Subtotal: " + factura.getSubtotal() ,205,1);
								docEZ.writeText("Descuento: " + factura.getDescuento() ,205,1);
								docEZ.writeText("ISV: " + factura.getIsv() ,205,1);
								docEZ.writeText("Total: " + factura.getTotal() ,205,1);
								docEZ.writeText("												   ",230,1);
								docEZ.writeText("												   ",230,1);
								docEZ.writeText("												   ",230,1);
								docEZ.writeText("-------------------------------------------------",230,1);
								docEZ.writeText("Nombre y Firma",205,1);
								docEZ.writeText("  ",580,1);

								printData = docEZ.getDocumentData();
							}
							
							//4-in sample
							else if (selectedItemIndex == 1) {
								docEZ.writeText("For Delivery", 1, 300);
			                    docEZ.writeText("Customer Code: 00146",50,1);
			                    docEZ.writeText("Address: Manila",75,1);
			                    docEZ.writeText("Tin No.: 27987641",100,1);
			                    docEZ.writeText("Area Code: PN1-0004",125,1);
			                    docEZ.writeText("Business Style: SUPERMARKET A",150,1);
			                    
			                    docEZ.writeText("PRODUCT CODE      PRODUCT DESCRIPTION             QTY.    Delivered ",205,1);
			                    docEZ.writeText("------------      --------------------------      ----    ----------",230,1);
			                    docEZ.writeText("    111           Wht Bread Classic 400g           51          51   ",255,1);
			                    docEZ.writeText("    112           Clsc Wht Bread 600g              77          77   ",280,1);
			            		docEZ.writeText("    113           Wht Bread Clsc 600g              153         25   ",305,1);
			            		docEZ.writeText("    121           H Fiber Wheat Bread 600g         144         77   ",330,1);
			            		docEZ.writeText("    122           H Fiber Wheat Bread 400g         112         36   ",355,1);
			            		docEZ.writeText("    123           H Calcium Loaf 400g              81          44   ",380,1);
			            		docEZ.writeText("    211           California Raisin Loaf           107         44   ",405,1);
			            		docEZ.writeText("    212           Chocolate Chip Loaf              159         102  ",430,1);
			            		docEZ.writeText("    213           Dbl Delights(Ube & Chse)         99          80   ",455,1);
			            		docEZ.writeText("    214           Dbl Delights(Choco & Mocha)      167         130  ",480,1);
			            		docEZ.writeText("    215           Mini Wonder Ube Cheese           171         179  ",505,1);
			            		docEZ.writeText("    216           Mini Wonder Ube Mocha            179         100  ",530,1);
								docEZ.writeText("  ",580,1);
								printData = docEZ.getDocumentData();
								
							}
							
							//Barcode Sample
							else if (selectedItemIndex == 2)
							{
								paramEZ.setHorizontalMultiplier(1);
			                    paramEZ.setVerticalMultiplier(2);
			                    
			                    //write GS1 barcodes with 2d Composite data
			                    int pixelMult = 3;

			                    docEZ.writeText("GS1 Barcode",1,1);
			                    docEZ.writeBarCodeGS1DataBar("GSONE","123456789","123",pixelMult,pixelMult,1,1,22,30,1,paramEZ);
			                    
			                    docEZ.writeText("GS1 Truncated",330,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1TR","123456789","123",pixelMult,pixelMult,1,1,22,360,1,paramEZ);
			                    
			                    docEZ.writeText("GS1 Limited",530,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1LM","123456789","123",pixelMult,pixelMult,1,1,22,560,1,paramEZ);
			                    
			                    docEZ.writeText("GS1 Stacked",730,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1ST","123456789","123",pixelMult,pixelMult,1,1,22,760,1,paramEZ);
			                    
			                    
			                    docEZ.writeText("GS1 Stacked Omnidirection",930,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1SO","123456789","123",pixelMult,pixelMult,1,1,22,960,1,paramEZ);
			                    
			                    docEZ.writeText("GS1 Expanded",1530,1);
			                    docEZ.writeBarCodeGS1DataBar("GS1EX","ABCDEFGHIJKL","helloWorld!123",pixelMult,2*pixelMult,1,1,4,1560,1,paramEZ);
			                    
			                    paramEZ.setHorizontalMultiplier(2);
			                    paramEZ.setVerticalMultiplier(10);
			                    //Interleave 2of 5 barcode ratio 2:1
			                    docEZ.writeText("Interleave 2of5 Barcode ratio 2:1",2230,1);
			                    docEZ.writeBarCode("BCI25","0123456789",2260,1,paramEZ);
			                    
			                    //barcode 128
			                    docEZ.writeText("Barcode 128",2330,1);
			                    docEZ.writeBarCode("BC128","00010203040506070809",2360,1,paramEZ);
			                    
			                    //barcode EAN 128
			                    docEZ.writeText("EAN 128",2430,1);
			                    docEZ.writeBarCode("EN128","00010203040506070809",2460,1,paramEZ);
			                    
			                    //Code 39 barcodes
			                    docEZ.writeText("Code 39 Barcodes",2530,1);
			                    docEZ.writeBarCode("BC39N","0123456789",2560,1,paramEZ);
			                    docEZ.writeBarCode("BC39W","0123456789",2660,1,paramEZ);
			                    
			                    //Code 93 barcode
			                    docEZ.writeText("Code 93",2730,1);
			                    docEZ.writeBarCode("BC093","0123456789",2760,1,paramEZ);
			                    
			                    //Codabar
			                    docEZ.writeText("CODABAR",2830,1);
			                    docEZ.writeBarCode("COBAR","00010203040506070809",2860,1,paramEZ);
			                    
			                    //8 digit europe art num
			                    docEZ.writeText("8 DIGIT EUROPE ART NUM",2930,1);
			                    docEZ.writeBarCode("EAN08","0123456",2960,1,paramEZ);
			                    
			                    //13 digit europ art num
			                    docEZ.writeText("13 DIGIT Europe Art Num",3030,1);
			                    docEZ.writeBarCode("EAN13","000123456789",3060,1,paramEZ);
			                    
			                    //INTLV 2of5
			                    docEZ.writeText("Interleaved 2of5",3130,1);
			                    docEZ.writeBarCode("I2OF5","0123456789",3160,1,paramEZ);
			                    
			                    //PDF417
			                    docEZ.writeText("PDF417",3230,1);
			                    docEZ.writeBarCodePDF417("00010203040506070809", 3260, 1, 2, 1, paramEZ);
			                    
			                    //Plessy
			                    docEZ.writeText("Plessy",3350,1);
			                    docEZ.writeBarCode("PLESY","8052",3380,1,paramEZ);
			                    
			                    //UPC-A
			                    docEZ.writeText("UPC-A",3450,1);
			                    docEZ.writeBarCode("UPC-A","01234567890",3480,1,paramEZ);
			                    
			                    //UPC-E
			                    docEZ.writeText("UPC-E",3550,1);
			                    docEZ.writeBarCode("UPC-E","0123456",3580,1,paramEZ);
			                    
			                    paramEZ.setHorizontalMultiplier(10);
			                    
			                    paramEZ.setVerticalMultiplier(1);
			                    //QR
			                    docEZ.writeText("QR Barcode Manual Formating",3650,1);
			                    docEZ.writeBarCodeQRCode("N0123456789,B0004(&#),QR//BARCODE",2,9,1,3680,1,paramEZ);
			                    
			                    docEZ.writeText("QR Barcode Auto Formatting 1",3950,1);
			                    docEZ.writeBarCodeQRCode("0123456789012345678901234567890123456789",2,9,0,3980,1,paramEZ);
			                    
			                    paramEZ.setHorizontalMultiplier(8);
			                    docEZ.writeText("QR Barcode Auto Formatting 2",4250,1);
			                    docEZ.writeBarCodeQRCode("0123456789ABCDE",2,9,0,4280,1,paramEZ);
			                    
			                    //Aztec
			                    docEZ.writeText("Aztec",4550,1);
			                    docEZ.writeBarCodeAztec("Code 2D!",104,4580,1,paramEZ);
			                    docEZ.writeText("",4500,1);
			                    printData = docEZ.getDocumentData();
			                    
							}
							//User selected an unpredefine item(eg from browsing file)
			                else
			                {
			                	String selectedItem = (String)m_printItemsSpinner.getSelectedItem();
			                	Bitmap anImage = null;
			                	//Check if item is an image
			                	String[] okFileExtensions =  new String[] {".jpg", ".png", ".gif",".jpeg",".bmp", ".tif", ".tiff",".pcx"};
			                	for (String extension : okFileExtensions) {
									if(selectedItem.toLowerCase(Locale.US).endsWith(extension))
									{
										anImage = BitmapFactory.decodeFile(selectedItem);
										break;
									}
								}
			                	//selected item is not an image file
			                	if (anImage == null)
			                	{
			                		File file = new File(selectedItem);
			                		byte[] readBuffer = new byte[(int)file.length()];
			                		InputStream inputStream= new BufferedInputStream(new FileInputStream(file));
			                		inputStream.read(readBuffer);
			                		inputStream.close();
			                		printData = readBuffer;
			                	}
			                	else 
			                	{
			                		DisplayPrintingStatusMessage("Processing image...");
			                		docLP.clear();
									docLP.writeImage(anImage, 576);
									printData = docLP.getDocumentData();
								}
			                }
							
						}
						//for LP 
						else if(m_printerMode.equals("LP"))
						{
							docLP.clear();
							//3-inch sample to generate
			                if(selectedItemIndex == 0)
			                {
								Factura factura = new Factura();
								factura.setEmpresa("VAN HEUSEN DE C.A.");
								factura.setEmp_direccion("Col. San Fernando, Ave Juan Pablo II Frente a la Leyde, SPS Cortes");
								factura.setEmp_telefono("25160100");
								factura.setEmp_rtn("05019995143200");
								factura.setFecha("22/02/2017");
								factura.setCodigoCliente("C0001");
								factura.setNombreCliente("Union Moda");
								factura.setDireccion("BARRIO LOUNDRES, CARRETERA PANAMERICANA CONTIGO A GAS P");
								factura.setTelefono("2424-7077");
								factura.setVendedor("Sergio Peralta");
								factura.setSubtotal(510.00);
								factura.setDescuento(0.0);
								factura.setIsv(76.50);
								factura.setTotal(586.50);

								Item item = new Item("BF6CACR02-560-015", 1, 419.75);
								Item item2 = new Item("CC001-CLL-165", 1, 166.75);
								Item item3 = new Item("CC001-CLL-155", 1, 166.75);
								Item item4 = new Item("CC001-CLL-160", 1, 166.75);

								List<Item> items = new ArrayList<>();
								items.add(item);
								items.add(item2);
								items.add(item3);
								items.add(item4);

								factura.setItems(items);

								//=============GENERATING RECEIPT====================================//
								docLP.writeText(factura.getEmpresa());
								docLP.writeText(factura.getDireccion());
								docLP.writeText("Telefono:" + factura.getEmp_telefono());
								docLP.writeText("RTN: " +factura.getEmp_rtn());
								docLP.writeText("  ");
								docLP.writeText("Fecha: " + factura.getFecha());
								docLP.writeText("Cliente: " + factura.getCodigoCliente());
								docLP.writeText(factura.getNombreCliente());
								docLP.writeText("Telefono: " + factura.getTelefono());
								docLP.writeText("RTN: " + factura.getEmp_rtn());
								docLP.writeText("Vendedor: " + factura.getVendedor());
								docLP.writeText("  ");
								docLP.writeText("PRODUCTO");
								docLP.writeText("SKU			CANTIDAD		PRECIO");
								docLP.writeText("----------------------------------");
								for(Item i: factura.getItems()){
									docLP.writeText(i.getSKU() + "	" + String.valueOf(i.getCantidad()) + "	" + String.valueOf(i.getPrecio()));
								}
								docLP.writeText("----------------------------------");
								docLP.writeText("Subtotal: " + factura.getSubtotal());
								docLP.writeText("Descuento: " + factura.getDescuento());
								docLP.writeText("ISV: " + factura.getIsv());
								docLP.writeText("Total: " + factura.getTotal());
								docLP.writeText("  ");
								docLP.writeText("  ");
								docLP.writeText("  ");
								docLP.writeText("----------------------------------");
								docLP.writeText("Nombre y Firma");
								docLP.writeText("  ");
								docLP.writeText("  ");
								docLP.writeText("  ");
								docLP.writeText("  ");
								docLP.writeText("  ");
								docLP.writeText("  ");


								printData = docLP.getDocumentData();
			                }
			                
			                //4-inch sample to generate
			                else if(selectedItemIndex == 1)
			                {
			                    docLP.writeText("                            For Delivery");
			                    docLP.writeText(" ");
			                    docLP.writeText("Customer Code: 00146");
			                    docLP.writeText("Address: Manila");
			                    docLP.writeText("Tin No.: 27987641");
			                    docLP.writeText("Area Code: PN1-0004");
			                    docLP.writeText("Business Style: SUPERMARKET A");
			                    docLP.writeText(" ");
			                    docLP.writeText("PRODUCT CODE         PRODUCT DESCRIPTION          QTY.    Delivered");
			                    docLP.writeText("------------      --------------------------      ----    ---------- ");
			                    docLP.writeText("    111           Wht Bread Classic 400g           51         51     ");
			                    docLP.writeText("    112           Clsc Wht Bread 600g              77         77     ");
			                    docLP.writeText("    113           Wht Bread Clsc 600g              153        25     ");
			                    docLP.writeText("    121           H Fiber Wheat Bread 600g         144        77     ");
			                    docLP.writeText("    122           H Fiber Wheat Bread 400g         112        36     ");
			                    docLP.writeText("    123           H Calcium Loaf 400g              81         44     ");
			                    docLP.writeText("    211           California Raisin Loaf           107        44     ");
			                    docLP.writeText("    212           Chocolate Chip Loaf              159        102    ");
			                    docLP.writeText("    213           Dbl Delights(Ube & Chse)         99         80     ");
			                    docLP.writeText("    214           Dbl Delights(Choco & Mocha)      167        130    ");
			                    docLP.writeText("    215           Mini Wonder Ube Cheese           171        179    ");
			                    docLP.writeText("    216           Mini Wonder Ube Mocha            179        100    ");
			                    docLP.writeText("  ");
			                    docLP.writeText("  ");
			                    printData = docLP.getDocumentData();
			                }
			                //Print Image
			                else if(selectedItemIndex == 2)
			                {
			                	DisplayPrintingStatusMessage("Processing image..");
			                    AssetManager assetManager = getAssets();
			                    Bitmap anImage = BitmapFactory.decodeStream(assetManager.open("dologo.png"));
			                    
			                    docLP.writeImage(anImage,576);
			                    printData = docLP.getDocumentData();
			                }
			                //User selected an item not predefined on list(from browsing file)
			                else
			                {
			                	String selectedItem = (String)m_printItemsSpinner.getSelectedItem();
			                	Bitmap anImage = null;
			                	//Check if item is an image
			                	String[] okFileExtensions =  new String[] {".jpg", ".png", ".gif",".jpeg",".bmp", ".tif", ".tiff",".pcx"};
			                	for (String extension : okFileExtensions) {
									if(selectedItem.toLowerCase().endsWith(extension))
									{
										anImage = BitmapFactory.decodeFile(selectedItem);
										break;
									}
								}
			                	//selected item is not an image file
			                	if (anImage == null)
			                	{
			                		File file = new File(selectedItem);
			                		byte[] readBuffer = new byte[(int)file.length()];
			                		InputStream inputStream= new BufferedInputStream(new FileInputStream(file));
			                		inputStream.read(readBuffer);
			                		inputStream.close();
			                		printData = readBuffer;
			                	}
			                	else 
			                	{
			                		DisplayPrintingStatusMessage("Processing image..");
									docLP.writeImage(anImage, 576);
									printData = docLP.getDocumentData();
								}
			                }
			                
						}
						//for EXPCL(Apex Printers)
						else if (m_printerMode.equals("ExPCL Line Print"))
						{
							
						}
						else if (m_printerMode.equals("ExPCL Page Print"))
						{
							
						}
						//DPL printers
						else if(m_printerMode.equals("DPL"))
						{
							docDPL.clear();
							 //text sample to generate
			                if(selectedItemIndex == 0)
			                {
			                    docDPL.writeTextInternalBitmapped("Hello World",1,5,5);
			                    //write normal ASCII Text Scalable
			                    
			                    docDPL.writeTextScalable("Hello World","00",25,5);
			                    
			                    //Test print korean font
			                    paramDPL.setIsUnicode(true);
			                    paramDPL.setDBSymbolSet(DoubleByteSymbolSet.Unicode);
			                    paramDPL.setFontHeight(12);
			                    paramDPL.setFontWidth(12);
			                    docDPL.writeTextScalable("AC00AE370000","50",75,5,paramDPL);
			                    printData = docDPL.getDocumentData();
			                }
			                //Barcodes
			                else if (selectedItemIndex == 1)
			                {
			                    //Test print Code 3 of 9
			                    //Barcode A with default parameter
			                    docDPL.writeBarCode("A", "BRCDA", 0,0);
			                    docDPL.writeTextInternalBitmapped("Barcode A",1,60,0);
			                    
			                    //Barecode A with specified parameters
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(20);
			                    
			                    docDPL.writeBarCode("A", "BRCDA", 100,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Barcode A",1,135,0);
			                    
			                    //UPC-A with specified parameters
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(10);
			                    docDPL.writeBarCode("B", "012345678912", 160,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("UPC-A",1,185,0);
			                    //Code 128
			                    //Barecode A with specified parameters
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(20);
			                    docDPL.writeBarCode("E", "ACODE128", 210,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Code 128",1,250,0);
			                    
			                    //EAN-13
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(20);
			                    docDPL.writeBarCode("F", "0123456789012", 285,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("EAN-13",1,315,0);
			                    //EAN Code 128
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(3);
			                    paramDPL.setNarrowBarWidth(1);
			                    paramDPL.setSymbolHeight(20);
			                    docDPL.writeBarCode("Q", "0123456789012345678", 355,0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("EAN Code 128",1,395,0);
			                    //UPS MaxiCode, Mode 2 & 3
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    paramDPL.setSymbolHeight(0);
			                    
			                    UPSMessage upsMessage = new UPSMessage("920243507", 840, 1, "1Z00004951", "UPSN", "9BCJ43", 365, "625TH9", 1, 1, 10, true, "669 SECOND ST", "ENCINITAS", "CA");
			                    
			                    docDPL.writeBarCodeUPSMaxiCode(2, upsMessage, 445, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("UPS MaxiCode",1,560,0);
			                    
			                    //PDF-417
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    paramDPL.setSymbolHeight(0);
			                   	docDPL.writeBarCodePDF417("ABCDEF1234", false, 1, 0, 0, 0, 590, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("PDF-417",1,630,0);
			                    
			                    //Data Matrix
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(4);
			                    paramDPL.setNarrowBarWidth(4);
			                    paramDPL.setSymbolHeight(0);
			                    
			                    docDPL.writeBarCodeDataMatrix("DATAMAX", 140, 0, 0, 0, 670, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Data Matrix w/ ECC 140",1,770,0);
			                    docDPL.writeBarCodeDataMatrix("DATAMAX", 200, 0, 0, 0, 810, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Data Matrix w/ ECC 200",1,880,0);
			                    
			                    //QRCODE
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(4);
			                    paramDPL.setNarrowBarWidth(4);
			                    paramDPL.setSymbolHeight(0);
			                    //AutoFormatting
			                    docDPL.writeBarCodeQRCode("This is the data portion", true, 0, "", "", "", "", 920, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("QR Barcode w/ Auto Formatting",1,1030,0);
			                    
			                    //Manual Formatting
			                    docDPL.writeBarCodeQRCode("1234This is the data portion", false, 2, "H", "4", "M", "A", 1070, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("QR Barcode w/ Manual formatting",1,1200,0);
			                    
			                    
			                    //Test BarcodeAzTec
			                    paramDPL.setIsUnicode(false);
			                    paramDPL.setWideBarWidth(12);
			                    paramDPL.setNarrowBarWidth(12);
			                    paramDPL.setSymbolHeight(0);
			                    docDPL.writeBarCodeAztec("ABCD1234", 0, false, 0, 1240, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Aztec Barcode ECI 0, ECC 0",1,1360,0);
			                    docDPL.writeBarCodeAztec("ABCD1234", 17, true, 232, 1400, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Aztec Barcode ECI 1, ECC 232",1,1500,0);
			                    
			                    //GS1 Databars
			                    paramDPL.setWideBarWidth(2);
			                    paramDPL.setNarrowBarWidth(2);
			                    paramDPL.setSymbolHeight(0);
			                    
			                    docDPL.writeBarCodeGS1DataBar("2001234567890","","E",1 ,0 ,0 ,2 ,1540, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("GS1 Databar Expanded",1,1760,0);
			                    
			                    docDPL.writeBarCodeGS1DataBar("2001234567890","hello123World","D",1 ,0 ,0 ,0 ,1800, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("GS1 Stacked Omni Direction",1,1980,0);
			                    
			                    //Austrailia 4-State
			                    docDPL.writeBarCodeAusPost4State("A124B", true, 59, 32211324, 2020, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Aus Post 4 State readable",1,2100,0);
			                    docDPL.writeBarCodeAusPost4State("123456789012345", false, 62, 39987520, 2140, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Aus Post 4 State non readable",1,2190,0);
			                    
			                    
			                    //write CodaBlock
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    docDPL.writeBarCodeCODABLOCK("12345678", 25, "E", false, 4, 2, 2230, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("CODABLOCK",1,2320,0);
			                    
			                    //write TCIF
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    docDPL.writeBarCodeTLC39("ABCD12345678901234589ABED", 0, 123456, 2360, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("TCIF",1,2480,0);
			                    
			                    //write MicroPDF417
			                    paramDPL.setWideBarWidth(0);
			                    paramDPL.setNarrowBarWidth(0);
			                    docDPL.writeBarCodeMicroPDF417("PDF417", 4, 4, false, false, 2520, 0, paramDPL);
			                    docDPL.writeTextInternalBitmapped("Micro PDF417",1,2560,0);
			                    printData = docDPL.getDocumentData();
			                }
			                //graphics
			                else if (selectedItemIndex == 2)
			                {
			                	//WriteLine
			    				docDPL.writeLine(0, 0, 10, 25);
			    	
			    				//WriteBox
			    				docDPL.writeBox(50, 0, 25, 25, 1, 1);
			    	
			    				//WriteRectangle
			    				docDPL.writeRectangle(9, 100, 10, 150, 10, 150, 200, 100, 200);
			    				docDPL.writeTriangle(7, 200, 10, 250, 25, 200, 40);
			    				docDPL.writeCircle(4, 300, 25, 25);
			    				printData = docDPL.getDocumentData();
			                }
			                //image
			                else if (selectedItemIndex == 3)
			                {	
			                	DisplayPrintingStatusMessage("Processing image..");
			                	AssetManager assetManager = getAssets();
			                    Bitmap anImage = BitmapFactory.decodeStream(assetManager.open("dologo.png"));
			                	docDPL.writeImage(anImage, ImageType.Other, 0,0, paramDPL);
			                	printData = docDPL.getDocumentImageData();
			                }
			              //User selected a browsed file
			                else
			                {
			                	boolean isImage = false;
			                	String selectedItem = (String)m_printItemsSpinner.getSelectedItem();
			                	//Check if item is an image
			                	String[] okFileExtensions =  new String[] {".jpg", ".png", ".gif",".jpeg",".bmp", ".tif", ".tiff",".pcx"};
			                	for (String extension : okFileExtensions) {
									if(selectedItem.toLowerCase(Locale.US).endsWith(extension))
									{
										isImage = true;
										break;
									}
								}
			                	//selected item is not an image file
			                	if (!isImage)
			                	{
			                		File file = new File(selectedItem);
			                		byte[] readBuffer = new byte[(int)file.length()];
			                		InputStream inputStream= new BufferedInputStream(new FileInputStream(file));
			                		inputStream.read(readBuffer);
			                		inputStream.close();
			                		printData = readBuffer;

			                	}
			                	else 
			                	{
			                		DisplayPrintingStatusMessage("Processing image..");
			                		ImageType imgType = ImageType.Other;
			                		if(selectedItem.endsWith(".pcx")|| selectedItem.endsWith(".PCX"))
			                		{
			                			imgType = ImageType.PCXFlipped_8Bit;
			                		}
			                		else
			                		{
			                			imgType = ImageType.Other;
									}
			                		docDPL.clear();
									docDPL.writeImage(selectedItem, imgType, 0, 0, paramDPL);
									printData = docDPL.getDocumentImageData();
								}//end else
			                }//end else
						}
					}
					//=====================Start Connection Thread=======================================//
					new Thread(DOPrintMainActivity.this, "PrintingTask").start();

				} 
				catch (Exception e) {
					// Application error message box
					e.printStackTrace();
					AlertDialog.Builder builder = new AlertDialog.Builder(DOPrintMainActivity.this);
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
		
		//ON press save settings
		m_saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				SaveApplicationSettingToFile();
			} 
			
		});
	}

	/**Display printing status message to the Textview in the
	 * DOPrinterDemoActivity
	 * @param MsgStr - message to display
	 */
	public void DisplayPrintingStatusMessage(String MsgStr) {
		g_PrintStatusStr = MsgStr;

		m_handler.post(new Runnable() {
			public void run() {
				((TextView)findViewById(R.id.printing_status_textview)).setText(g_PrintStatusStr);
			}// run()
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.doprint_main, menu);
		return true;
	}
	
	// ----------------------------------
	// Handle each menu item action
	// -----------------------------------
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = false;
	
		switch (item.getItemId()) {
		case R.id.menu_about:
			// Display the about dialog box
			DisplayABoutDialog();
			ret = true;
			break;
		}// switch()
	
		return ret;
		}
	
	// ----------------------------
	// Display About Dialog Box
	// ------------------------------
	void DisplayABoutDialog() {
		final Dialog about = new Dialog(DOPrintMainActivity.this);
		about.setContentView(R.layout.doabout);
		about.setTitle("About..");
		about.setCancelable(true);

		// get version of the application.
		PackageInfo pinfo = null;
		try 
		{
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		
			if (pinfo != null) {
				
				// set up the text view
				TextView descTextView = (TextView) about
						.findViewById(R.id.AboutDescription);
				descTextView.setText(" " + getString(R.string.app_name) + " by QTP\n"
						+ " Version Code:"
						+ String.valueOf(pinfo.versionCode) + "\n"
						+ " Version Name:" + pinfo.versionName+"\n"
						+ " Copyright: 2014" + "\n"
						+ " Company: Datamax-O'Neil" + "\r\n"
						+ " D-O Print is a sample application that allows users to print to a Datamax-O'Neil printer.");

				// set up the image view
				ImageView AboutImgView = (ImageView) about
						.findViewById(R.id.AboutImageView);
				AboutImgView.setImageResource(R.drawable.dologo94x85);
	
				// set up button
				Button closeButton = (Button) about.findViewById(R.id.AboutCloseButton);
				closeButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						about.dismiss();
					}
				});
	
				about.show();
	
			}
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode)
		{
			//get results from connection settings activity
			case CONFIG_CONNECTION_REQUEST:
			{
				if (resultCode == RESULT_OK) 
				{
					// get the bundle data from the TCP/IP Config Intent.
					Bundle extras = data.getExtras();
					if (extras != null) 
					{
						//===============Get data from Bluetooth configuration=================//
						if(connectionType.equals("Bluetooth"))
						{
							m_printerAddress = extras.getString(BLUETOOTH_DEVICE_ADDR_KEY);
							m_printerAddress = m_printerAddress.toUpperCase(Locale.US);
							if(!m_printerAddress.matches("[0-9A-fa-f:]{17}"))
							{
								m_printerAddress = formatBluetoothAddress(m_printerAddress);
							}

							
							m_connectionInfoStatus.setText("Printer's MAC Address: "+ m_printerAddress);
						}
						//==============Get data from TCP/IP configuration===================//
						else if (connectionType.equals("TCP/IP"))
						{
							m_printerAddress = extras.getString(PRINTER_IPADDRESS_KEY);
							m_printerPort = extras.getInt(PRINTER_TCPIPPORT_KEY);
							m_connectionInfoStatus.setText("Printer's IP Address/Port: "+ m_printerAddress+ ":"+ Integer.toString(m_printerPort)); //valid values are 3inch
						}
						g_appSettings.setPrinterAddress(m_printerAddress);
						g_appSettings.setPrinterPort(m_printerPort);

					}
				}
				break;
			}

			//results from file browsing activity
			case REQUEST_PICK_FILE:
			{
				if (resultCode == RESULT_OK) {
					Bundle extras = data.getExtras();
						if(extras != null) {
		                	//========Get the file path===============//
		                	m_selectedPath = extras.getString(FOLDER_PATH_KEY);
		                	if(!filesList.contains(m_selectedPath))
		                		filesList.add(m_selectedPath);
		                	if(adapter != null)
		                	{
		                		m_printItemsSpinner.setAdapter(adapter);
		                		
		                		//if item is not on the list, then add
		                		if(adapter.getPosition(m_selectedPath) < 0)
		                		{
		                			adapter.add((CharSequence)m_selectedPath);
		                			adapter.notifyDataSetChanged();
		                		}
		                		//selects the selected one.
		                		m_printItemsSpinner.setSelection(adapter.getPosition(m_selectedPath));
		                	}
						}
					}
					break;
			}	
		}
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    //reload your ScrollBars by checking the newConfig
	    
	}
	
	// -------------------------------------------------
		// Read the application configuration information from a file.
		// -------------------------------------------------
		DOPrintSettings ReadApplicationSettingFromFile() {
			DOPrintSettings ret = null;
			InputStream instream = null;
			try {
				Toast.makeText(getApplicationContext(), "Loading configuration", Toast.LENGTH_SHORT).show();
				instream = openFileInput(ApplicationConfigFilename);
			} catch (FileNotFoundException e) {

				Log.e("DOPrint", e.getMessage(), e);
				Toast.makeText(getApplicationContext(), "No configuration loaded", Toast.LENGTH_SHORT).show();
				return ret;
			}

			try {
				ObjectInputStream ois = new ObjectInputStream(instream);

				try {
					ret = (DOPrintSettings) ois.readObject();
				} catch (ClassNotFoundException e) {
					Log.e("DOPrint", e.getMessage(), e);
					ret = null;
				}
			} catch (StreamCorruptedException e) {
				Log.e("DOPrint", e.getMessage(), e);
				ret = null;
			} catch (IOException e) {
				Log.e("DOPrint", e.getMessage(), e);
				ret = null;
			} finally {
				try {
					if (instream != null)
						instream.close();
				} catch (IOException e) {
					;
				}
			}
			return ret;
		}
		
		public boolean SaveApplicationSettingToFile() {
        	
    		boolean bRet = true;
    		FileOutputStream fos = null;
    		ByteArrayOutputStream bos = new ByteArrayOutputStream();

    		try {
    			// write the object to the output stream object.
    			ObjectOutput out = new ObjectOutputStream(bos);
    			out.writeObject(g_appSettings);

    			// convert the output stream object to array of bytes
    			byte[] buf = bos.toByteArray();
    			
    			// write the array of bytes to file output stream
    			fos = openFileOutput(ApplicationConfigFilename,
    					Context.MODE_PRIVATE);
    			fos.write(buf);

    			File f = getDir(ApplicationConfigFilename, 0);
    			Log.e("DOPrint", "Save Application settings to file: " + f.getName());
    			showToast("Application Settings saved");
    		} catch (IOException ioe) {
    			Log.e("DOPrint", "error", ioe);
    			showToast(ioe.getMessage());
    			bRet = false;
    		} finally {
    			try {
    				if (fos != null)
    					fos.close();
    			} catch (IOException ioe) {
    				
    				showToast(ioe.getMessage());
    			}
    		}
    		return bRet;
    	}// SaveApplicationSettingToFile()
	
	public void showToast(String toast) {
		Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
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
	
	/** Display message in an Alert Dialog
	 * @param message - message to display
	 */
	public void ShowMessageBox(final String message, final String title) {
		m_handler.post(new Runnable() {

			@Override
			public void run() {
				LayoutInflater inflater = LayoutInflater.from(DOPrintMainActivity.this);
				View view = inflater.inflate(R.layout.messagebox, null);

				TextView textview=(TextView)view.findViewById(R.id.textmsg);
				
				textview.setText(message);
				AlertDialog.Builder builder = new AlertDialog.Builder(DOPrintMainActivity.this);
		        builder.setTitle(title)
		        .setCancelable(false)
		        .setNegativeButton("Close",new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		            }
		        });
		        builder.setView(view);
		        AlertDialog alert = builder.create();
		        
		        alert.show();

			}
		});
	}
	// --------------------------------
	// Enable controls after printing has completed
	// ---------------------------------
	public void EnableControls(final boolean value) {
		m_handler.post(new Runnable() {

			@Override
			public void run() {
				m_printButton.setEnabled(value);

			}
		});
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//Connection
		try
		{
			EnableControls(false);
			//Reset connection object
			conn = null;
			//====FOR BLUETOOTH CONNECTIONS========//
			if(connectionType.equals("Bluetooth"))
			{
				Looper.prepare();
				conn = Connection_Bluetooth.createClient(m_printerAddress);
			}
			//====FOR TCP Connection==//
			else if(connectionType.equals("TCP/IP"))
			{
				conn = Connection_TCP.createClient(m_printerAddress,m_printerPort);
			}
			
			if (m_printRadioButton.isChecked())
			{
				DisplayPrintingStatusMessage("Establishing connection..");
				//Open bluetooth socket
				if(!conn.getIsOpen())
					conn.open();
				
				//Sends data to printer
				DisplayPrintingStatusMessage("Sending data to printer..");
				conn.write(printData);
				Thread.sleep(1500);
				//signals to close connection
				conn.close();
				DisplayPrintingStatusMessage("Print success.");
				EnableControls(true);
			}
			else if(m_queryRadioButton.isChecked())
			{
				String message = "";
				DisplayPrintingStatusMessage("Establishing connection..");
				//Open bluetooth socket
				if(!conn.getIsOpen())
					conn.open();
				
				DisplayPrintingStatusMessage("Querying data..");
				 //If ExPCL is selected
	            if (m_printerModeSpinner.getSelectedItemPosition() == 3 || m_printerModeSpinner.getSelectedItemPosition() == 4 ) {
	                
//	                //General Status
//	                if (selectedItemIndex == 0) 
//	                {
//	                    GeneralStatus_ExPCL generalStatus = new GeneralStatus_ExPCL();
//	                    generalStatus.1000.queryPrinter(conn,1000);
//	                    
//
//	                    if (generalStatus.getValid() == false) {
//	                        message += "No response from printer\r\n";
//	                    }
//	                    else
//	                    {
//	                        message += String.format("Battery Voltage: %d\n", generalStatus.getBatteryVoltage());
//	                        message += String.format("PrintHead Temperature: %d\n", generalStatus.getPrintheadTemperature());
//	                        message += String.format("Print Buffer KB Remaining: %d\n", generalStatus.getRemainingRAM());
//	                        message += String.format("MCR Timer: %d\n", generalStatus.getMCRTimer());
//	                        message += String.format("Used RAM: %d\n", generalStatus.getUsedRAM());
//	                    }
//	                    ShowMessageBox(message, "General Status");
//
//	                }
//	                //Version information
//	                else if (selectedItemIndex == 1)
//	                {
//	                    VersionInformation_ExPCL versionInfo = new VersionInformation_ExPCL();
//	                    versionInfo.1000.queryPrinter(conn,1000);
//
//	                    if (versionInfo.getValid() == false) {
//	                        message += "No response from printer\r\n";
//	                    }
//	                    else
//	                    {
//	                        message += String.format("Hardware Version: %s\n", versionInfo.getHardwareControllerVersion());
//	                        message += String.format("Firmware Version: %s\n", versionInfo.getFirmwareVersion());
//	                    
//	                    }
//	                    ShowMessageBox(message, "Version Information");
//
//	                }
//	                //Magnetic Card Data
//	                else if (selectedItemIndex == 2)
//	                {
//	                    MagneticCardData_ExPCL mcrData = new MagneticCardData_ExPCL();
//	                    mcrData.queryPrinter(conn,1000);
//
//
//	                    if (mcrData.getValid() == false) {
//	                        message += "No response from printer\r\n";
//	                    }
//	                    else
//	                    {
//	                        message += String.format("Track 1: %s\n", mcrData.getTrack1Data());
//	                        message += String.format("Track 2: %s\n", mcrData.getTrack2Data());
//	                        message += String.format("Track 3: %s\n", mcrData.getTrack3Data());
//	                    }
//	                    ShowMessageBox(message, "Magnetic Card Data");
//	                }
//	                //Bluetooth
//	                else if (selectedItemIndex == 3)
//	                {
//	                    BluetoothConfiguration_ExPCL btConfig = new BluetoothConfiguration_ExPCL();
//	                    btConfig.queryPrinter(conn,1000);
//	                   
//	                    if (btConfig.getValid() == false) {
//	                        message += "No response from printer\r\n";
//	                    }
//	                    else
//	                    {
//	                        message += String.format("Local Classic Name: %s\n", btConfig.getLocalClassicName());
//	                        message += String.format("Local COD: %s\n", btConfig.getDeviceClass());
//	                        message += String.format("Power Save Mode: %s\n", btConfig.getPowerSave());
//	                        message += String.format("Security Mode: %s\n", btConfig.getSecurity());
//	                        message += String.format("Discoverable: %s\n", btConfig.getDiscoverable());
//	                        message += String.format("Connectable: %s\n", btConfig.getConnectable());
//	                        message += String.format("Bondable: %s\n", btConfig.getBondable());
//	                        message += String.format("Bluetooth Address: %s\n", btConfig.getBluetoothAddress());
//	                        message += String.format("Local Name: %s\n", btConfig.getLocalName());
//	                        message += String.format("Manufacturer: %s\n", btConfig.getManufacturer());
//	                        message += String.format("Model Number: %s\n", btConfig.getModelNumber());
//	                        message += String.format("Serial Number: %s\n", btConfig.getSerialNumber());
//	                        message += String.format("Bluetooth Firmware Version: %s\n", btConfig.getBluetoothFirmwareVersion());
//	                        message += String.format("Bluetooth Hardware Revision: %s\n", btConfig.getBluetoothHardwareVersion());
//	                        message += String.format("Protocol: %s\n", btConfig.getProtocolName());
//	                    }
//	                    ShowMessageBox(message, "Bluetooth Config");
//	                }
//	                
	            }//end of ExPCL mode
	            //DPL Mode
	            else if (m_printerModeSpinner.getSelectedItemPosition() == 2 )
	            {
	                //Printer Info
	                if (selectedItemIndex == 0)
	                {
	                    //Query Printer info
	                    PrinterInformation_DPL printerInfo = new PrinterInformation_DPL();
	                    printerInfo.queryPrinter(conn,1000);
	                    
	                    if (printerInfo.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Serial Number: %s\n", printerInfo.getPrinterSerialNumber());
	                        message += String.format("Boot 1 Version: %s\n", printerInfo.getBoot1Version());
	                        message += String.format("Boot 1 Part Number: %s\n", printerInfo.getBoot1PartNumber());
	                        message += String.format("Boot 2 Version: %s\n", printerInfo.getBoot2Version());
	                        message += String.format("Boot 2 PartNumber: %s\n", printerInfo.getBoot1PartNumber());
	                        message += String.format("Firmware Version: %s\n", printerInfo.getVersionInformation());
	                        message += String.format("AVR Version: %s\n", printerInfo.getAVRVersionInformation());
	                    }
	                    ShowMessageBox(message, "Printer Information");

	                }
	                //Fonts and files
	                else if (selectedItemIndex == 1)
	                {
	                    //Query Memory Module
	                    Fonts_DPL fontsDPL = new Fonts_DPL();
	                    fontsDPL.queryPrinter(conn,1000);


	                    if (fontsDPL.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += "FILES IN G: \n";
	                        
	                        //Get All Files
	                        FileInformation[] files = fontsDPL.getFiles("G");
	                        if(files != null)
	                        {
		                        for (FileInformation file : files) {
		                            message += String.format("Name: %s, Size: %d, Type: %s\n",file.getFileName(),file.getFileSize(),file.getFileType());
		                        }
	                        }
	                        else {
								message+= "No files found in module.\n";
							}
	                        //Get internal Fonts
	                         message += "INTERNAL FONTS: \n";
	                        String[] internalFonts = fontsDPL.getInternalFonts();
	                        for (String internalFont:internalFonts) {
	                            message += String.format("Name: %s\n",internalFont);

	                        }
	                    }
	                    ShowMessageBox(message, "Files and Internal Fonts");
	                }
	                
	                //Media Label
	                else if (selectedItemIndex == 2)
	                {
	                    //Query Media Label
	                    MediaLabel_DPL mediaLabel = new MediaLabel_DPL();
	                    mediaLabel.queryPrinter(conn,1000);
	                
	                    if (mediaLabel.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Media Type: %s\n", mediaLabel.getMediaType());
	                        message += String.format("Max Label Length: %d\n", mediaLabel.getMaxLabelLength());
	                        message += String.format("Continuous Label Length: %d\n", mediaLabel.getContinousLabelLength());
	                        message += String.format("Sensor Type: %s\n", mediaLabel.getSensorType());
	                        message += String.format("Paper Empty Distance: %d\n", mediaLabel.getPaperEmptyDistance());
	                        message += String.format("Label Width: %d\n", mediaLabel.getLabelWidth());
	                        message += String.format("Head Cleaning Threshold: %d\n", mediaLabel.getHeadCleanThresh());
	                        message += String.format("Ribbon Low Diameter: %d\n", mediaLabel.getRibbonLowDiameter());
	                        message += String.format("Ribbon Low Pause Enable: %s\n", mediaLabel.getRibbonLowPauseEnable()?"Yes":"No");
	                        message += String.format("Label Length Limit Enable: %s\n", mediaLabel.getLabelLengthLimit()?"Yes":"No");
	                        message += String.format("Present Backup Enable: %s\n", mediaLabel.getPresentBackup()?"Yes":"No");
	                        message += String.format("Present Location: %d\n", mediaLabel.getPresentDistance());
	                        message += String.format("Stop Location: %s\n", mediaLabel.getStopLocation());
	                        message += String.format("Backup After Print Enable: %s\n", mediaLabel.getBackupAfterPrint()?"Yes":"No");
	                        message += String.format("Gap Alternative Mode: %s\n", mediaLabel.getGapAlternateMode()?"Yes":"No");
	                    }
	                    
	                    ShowMessageBox(message, "Media Label");
	                }
	                
	                //Print Controls
	                else if (selectedItemIndex == 3){
	                    //Print Controls
	                    PrintSettings_DPL printSettings = new PrintSettings_DPL();
	                    printSettings.queryPrinter(conn,1000);

	                    if (printSettings.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Backup Delay: %d\n", printSettings.getBackupDelay());
	                        message += String.format("Row Offset: %d\n", printSettings.getRowOffSet());
	                        message += String.format("Column Offset: %d\n", printSettings.getColOffset());
	                        message += String.format("Row Adjusted Fine Tune: %d\n", printSettings.getRowFineTune());
	                        message += String.format("Column Adjusted Fine Tune: %d\n", printSettings.getColFineTune());
	                        message += String.format("Present Fine Tune: %d\n", printSettings.getPresentFineTune());
	                        message += String.format("Darkness Level: %d\n", printSettings.getDarkness());
	                        message += String.format("Contrast Level: %d\n", printSettings.getContrast());
	                        message += String.format("Heat Level: %d\n", printSettings.getHeat());
	                        message += String.format("Backup Speed: %f\n", printSettings.getBackupSpeed());
	                        message += String.format("Feed Speed: %f\n", printSettings.getFeedSpeed());
	                        message += String.format("Print Speed: %f\n", printSettings.getPrintSpeed());
	                        message += String.format("Slew Speed: %f\n", printSettings.getSlewSpeed());
	                    }
	                    
	                    ShowMessageBox(message,"Print Controls");
	                }
	                
	                //System Settings
	                else if (selectedItemIndex == 4){
	                    
	                    //System Settings
	                    SystemSettings_DPL sysSettings = new SystemSettings_DPL();
	                    sysSettings.queryPrinter(conn,1000);

	                    if (sysSettings.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Unit Measure: %s\n", sysSettings.getUnitMeasure());
	                        message += String.format("ESC Sequence Enable: %s\n", sysSettings.getEscapeSequenceEnable()?"Yes":"No");
	                        message += String.format("Single Byte Symbol: %s\n", sysSettings.getSingleByteSymbolSet());
	                        message += String.format("Double Byte Symbol: %s\n", sysSettings.getDoubleByteSymbolSet());
	                        message += String.format("Disable Symbol Set Value Selection: %s\n", sysSettings.getDisableSymbolSetValueSelection()?"Yes":"No");
	                        message += String.format("Menu Mode: %s\n", sysSettings.getMenuMode());
	                        message += String.format("Start of Print Emulation: %s\n", sysSettings.getStartOfPrintEmulation());
	                        message += String.format("Image mode: %s\n", sysSettings.getImageMode());
	                        message += String.format("Menu Language: %s\n", sysSettings.getMenuLanguage());
	                        message += String.format("Display Mode: %s\n", sysSettings.getDisplayMode());
	                        message += String.format("Block Allocated for Internal Module: %d\n", sysSettings.getInternalModuleSize());
	                        message += String.format("Scalable Font Cache: %d\n", sysSettings.getScalableFontCache());
	                        message += String.format("Legacy Emulation: %s\n", sysSettings.getLegacyEmulation());
	                        message += String.format("Column Emulation: %d\n", sysSettings.getColumnEmulation());
	                        message += String.format("Row Emulation: %d\n", sysSettings.getRowEmulation());
	                        message += String.format("Fault Handling Level: %s\n", sysSettings.getFaultHandlingLevel().name());
	                        message += String.format("Fault Handling Void Distance: %d\n", sysSettings.getFaultHandlingVoidDistance());
	                        message += String.format("Fault Handling Retry Counts: %d\n", sysSettings.getFaultHandlingRetryCounts());
	                        message += String.format("Font Emulation: %s\n", sysSettings.getFontEmulation().name());
	                        message += String.format("Input Mode: %s\n", sysSettings.getInputMode().name());
	                        message += String.format("Retract Delay: %d\n", sysSettings.getRetractDelay());
	                        message += String.format("Label Rotation: %s\n", sysSettings.getLabelRotation()?"180 Degrees":"None");
	                        message += String.format("Label Store Level: %s\n", sysSettings.getLabelStoreLevel());
	                        message += String.format("Scalable Font Bolding: %d\n", sysSettings.getScalableFontBolding());
	                        message += String.format("Format Attribute: %s\n", sysSettings.getFormatAttribute());
	                        message += String.format("Beeper State: %s\n", sysSettings.getBeeperState());
	                        message += String.format("Host Timeout: %d\n", sysSettings.getHostTimeout());
	                        message += String.format("Printer Sleep Timeout: %d\n", sysSettings.getPrinterSleepTimeout());
	                        message += String.format("Backlight Mode: %s\n", sysSettings.getBacklightMode().name());
	                        message += String.format("Backlight Timer: %d\n", sysSettings.getBackLightTimer());
	                        message += String.format("Power Down Timeout: %d\n", sysSettings.getPowerDownTimeout());
	                        message += String.format("RF Power Down Timeout: %d\n", sysSettings.getRFPowerDownTimeout());
	                        message += String.format("User Label Mode Enable: %s\n", sysSettings.getUserLabelModeEnable()?"Yes":"No");
	                        message += String.format("Radio Status: %s\n", sysSettings.getRadioStatus()?"Radio on":"Radio off");
	                        message += String.format("Supress Auto Reset: %s\n", sysSettings.getSuppressAutoReset()?"Yes":"No");
	                    }

	                    ShowMessageBox(message,"System Settings");
	                }
	                
	                //Sensor Calibration
	                else if (selectedItemIndex == 5){
	                    //Sensor Calibration
	                    SensorCalibration_DPL sensorCalibration = new SensorCalibration_DPL();
	                    sensorCalibration.queryPrinter(conn,1000);

	                    if (sensorCalibration.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Black Mark Paper value: %d\n", sensorCalibration.getBlackMarkPaperValue());
	                        message += String.format("Black Mark Sensor Gain value: %d\n", sensorCalibration.getBlackMarkSensorGain());
	                        message += String.format("Black Mark value: %d\n", sensorCalibration.getBlackMarkValue());
	                        message += String.format("Gap Sensor Gain value: %d\n", sensorCalibration.getGapSensorGain());
	                        message += String.format("Gap Sensor Gain should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getGapSensorGainWithThermalTransferMedia());
	                        message += String.format("Gap Mark Level value: %d\n", sensorCalibration.getGapMarkLevel());
	                        message += String.format("Gap Mark Level should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getGapMarkLevelWithThermalTransferMedia());
	                        message += String.format("Paper Level value: %d\n", sensorCalibration.getPaperLevel());
	                        message += String.format("Paper Level should be used with Thermal Transfer Media value: %d\n", sensorCalibration.getPaperLevelWithThermalTransferMedia());
	                        message += String.format("Presenter Sensor Gain value: %d\n", sensorCalibration.getPresenterSensorGain());
	                        message += String.format("Sensor Clear Value: %d\n", sensorCalibration.getSensorClearValue());
	                        message += String.format("Sensor Clear Value should be used with Thermal Transfer Media: %d\n", sensorCalibration.getSensorClearValueWithThermalTransferMedia());
	                        message += String.format("Auto Calibration Mode Enable: %s\n", sensorCalibration.getAutoCalibrationModeEnable()?"Yes":"No");
	                    }
	                    ShowMessageBox(message,"Sensor Calibration");
	                }
	                
	                //Miscellaneous
	                else if (selectedItemIndex == 6){
	                    
	                    //Misc
	                    Miscellaneous_DPL misc = new Miscellaneous_DPL();
	                    misc.queryPrinter(conn,1000);
	                    
	                    if (misc.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Delay Rate: %d\n", misc.getDelayRate());
	                        message += String.format("Present Sensor Equipped: %s\n", misc.getPresentSensorEquipped());
	                        message += String.format("Cutter Equipped: %s\n", misc.getCutterEquipped());
	                        message += String.format("Control Code: %s\n", misc.getControlCode());
	                        message += String.format("Start of Print Signal: %s\n", misc.getStartOfPrintSignal().name());
	                        message += String.format("End of Print Signal: %s\n", misc.getEndOfPrintSignal().name());
	                        message += String.format("GPIO Slew: %s\n", misc.getGPIOSlew().name());
	                        message += String.format("Feedback Mode Enable: %s\n", misc.getFeedbackModeEnable()?"Yes":"No");
	                        message += String.format("Comm Heat Commands Enable: %s\n", misc.getCommunicationHeatCommandsEnable()?"Yes":"No");
	                        message += String.format("Comm Speed Commands Enable: %s\n", misc.getCommunicationSpeedCommandsEnable()?"Yes":"No");
	                        message += String.format("Comm TOF Commands Enable: %s\n", misc.getCommunicationTOFCommandsEnable()?"Yes":"No");
	                        message += String.format("British Pound Enable: %s\n", misc.getBritishPoundEnable()?"Yes":"No");
	                        message += String.format("GPIO Backup Label: %s\n", misc.getGPIOBackupLabel().name());
	                        message += String.format("Ignore Control Code Enable: %s\n", misc.getIgnoreControlCodeEnable()?"Yes":"No");
	                        message += String.format("Sofware Switch Enable: %s\n", misc.getSoftwareSwitch()?"Yes":"No");
	                        message += String.format("Max Length Ignore Enable: %s\n", misc.getMaximumLengthIgnoreEnable()?"Yes":"No");
	                        message += String.format("Pause Mode Enable: %s\n", misc.getPauseModeEnable()?"Yes":"No");
	                        message += String.format("Peel Mode Enable: %s\n", misc.getPeelModeEnable()?"Yes":"No");
	                        message += String.format("USB Mode: %s\n", misc.getUSBMode().name());
	                        message += String.format("Windows Driver For EZ RLE Enable: %s\n", misc.getWindowsDriverForEZ_RLE()?"Yes":"No");
	                        message += String.format("Hex Dump Enable: %s\n", misc.getHexDumpModeEnable()?"Yes":"No");
	                        message += String.format("Display Mode for IP Host Name: %s\n", misc.getDisplayModeForIPHostname().name());
	                    }
	                    ShowMessageBox(message,"Miscellaneous");
	                }
	                //Serial Port
	                else if (selectedItemIndex == 7){
	                    //SerialPort
	                    SerialPortConfiguration_DPL serialConfig = new SerialPortConfiguration_DPL();
	                    serialConfig.queryPrinter(conn,1000);

	                    if (serialConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Serial Port A Baud Rate: %d\n", serialConfig.getSerialPortBaudRate());
	                        message += String.format("Serial Port A Stop Bit: %d\n", serialConfig.getSerialPortStopBit());
	                        message += String.format("Serial Port A Data Bits: %d\n", serialConfig.getSerialPortDataBits());
	                        message += String.format("Serial Port A Parity: %s\n", serialConfig.getSerialPortParity());
	                        message += String.format("Serial Port A HandShaking: %s\n", serialConfig.getSerialPortHandshaking());
	                    }
	                    
	                    ShowMessageBox(message,"Serial Port");
	                }
	                
	                //Auto Update
	                else if (selectedItemIndex == 8){
	                    //AutoUpdate
	                    AutoUpdate_DPL autoUpdate = new AutoUpdate_DPL();
	                    autoUpdate.queryPrinter(conn,1000);

	                    if (autoUpdate.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Wireless Upgrade Type: %s\n", autoUpdate.getWirelessUpgradeType().name());
	                        message += String.format("Status Message Print mode: %s\n", autoUpdate.getStatusMessagePrintMode().name());
	                        message += String.format("Security Credential File Format: %s\n", autoUpdate.getSecurityCredentialFileFormat().name());
	                        message += String.format("Config File Name: %s\n", autoUpdate.getConfigurationFileName());
	                        message += String.format("TFTP Server IP: %s\n", autoUpdate.getTFTPServerIPAddress());
	                        message += String.format("Upgrade Package Version: %s\n", autoUpdate.getUpgradePackageVersion());
	                        message += String.format("Beeper Enable: %s\n", autoUpdate.getBeeperEnable()?"Yes":"No");
	                        message += String.format(" FTP Username: %s\n", autoUpdate.getFTPUsername());
	                        message += String.format("FTP Server Name: %s\n", autoUpdate.getFTPServerName());
	                        message += String.format("FTP Server Port: %d\n", autoUpdate.getFTPServerPort());
	                    }
	                    ShowMessageBox(message,"Auto Update");
	                }
	                
	                //Avalanche
	                else if (selectedItemIndex == 9){
	                    //Avalanche
	                    AvalancheEnabler_DPL avaEnabler = new AvalancheEnabler_DPL();
	                    avaEnabler.queryPrinter(conn,1000);
	                   
	                    if (avaEnabler.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Agent IP Address: %s\n", avaEnabler.getAgentIPAddress());
	                        message += String.format("Agent Port: %d\n", avaEnabler.getAgentPortNumber());
	                        message += String.format("Agent DNS Name: %s\n", avaEnabler.getAgentDNSName());
	                        message += String.format("Connectivity Type: %s\n", avaEnabler.getConnectivityType().name());
	                        message += String.format("Printer Name: %s\n", avaEnabler.getPrinterName());
	                        message += String.format("Printer Model: %s\n", avaEnabler.getPrinterModel());
	                        message += String.format("Update Package Version: %s\n", avaEnabler.getUpdatePackageVersion());
	                        message += String.format("Update Mode: %s\n", avaEnabler.getUpdateMode());
	                        message += String.format("Update Interval: %d\n", avaEnabler.getUpdateInterval());
	                        message += String.format("Update Package Name: %s\n", avaEnabler.getUpdatePackageName());
	                        message += String.format("Print Status Result Enable: %s\n", avaEnabler.getPrintStatusResultEnable()?"Yes":"No");
	                        message += String.format("Avalanche Enabler Active: %s\n", avaEnabler.getAvalancheEnablerActive()?"Yes":"No");
	                        message += String.format("Remove old updates: %s\n", avaEnabler.getRemoveOldUpdatesBeforeUpdate()?"Yes":"No");
	                    }
	                    ShowMessageBox(message,"Avalanche Enable Settings");
	                }
	                //Bluetooth Config
	                else if (selectedItemIndex == 10) {
						BluetoothConfiguration_DPL btConfig = new BluetoothConfiguration_DPL();
						 btConfig.queryPrinter(conn,1000);
						
						if (btConfig.getValid() == false) {
							message += "No response from printer\r\n";
						}
						else {
							message += String.format("Bluetooth Device Name: %s\n", btConfig.getBluetoothDeviceName());
	                        message += String.format("Bluetooth Service Name: %s\n", btConfig.getBluetoothServiceName());
	                        message += String.format("Authentication Type:%s\n", btConfig.getAuthenticationType().name());
	                        message += String.format("Discoverable: %s\n", btConfig.getDiscoverable()?"Yes":"No");
	                        message += String.format("Connectable: %s\n", btConfig.getConnectable()?"Yes":"No");
	                        message += String.format("Bondable: %s\n", btConfig.getBondable()?"Yes":"No");
	                        message += String.format("Encryption: %s\n", btConfig.getEncryption()?"Yes":"No");
	                        message += String.format("PassKey: %s\n", btConfig.getPassKey());
	                        message += String.format("Inactive Disconnect Time: %d\n",btConfig.getInactiveDisconnectTime());
	                        message += String.format("Power Down Time: %d\n", btConfig.getPowerDownTime());
	                        message += String.format("Bluetooth Device Address: %s\n", btConfig.getBluetoothDeviceAddress());
						}
						ShowMessageBox(message, "Bluetooth Configuration");
					}
	                //Network General
	                else if (selectedItemIndex == 11){
	                    
	                    NetworkGeneralSettings_DPL netGen = new NetworkGeneralSettings_DPL();
	                    netGen.queryPrinter(conn,1000);
	                    
	                    if (netGen.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Primary Interface: %s\n", netGen.getPrimaryInterface());
	                        message += String.format("WiFi Module Type: %s\n", netGen.getWiFiType().name());
	                        message += String.format("Network Password:%s\n", netGen.getNetworkPassword());
	                        message += String.format("SNMP Enable: %s\n", netGen.getSNMPEnable()?"Yes":"No");
	                        message += String.format("Telnet Enable: %s\n", netGen.getTelnetEnable()?"Yes":"No");
	                        message += String.format("FTP Enable: %s\n", netGen.getFTPEnable()?"Yes":"No");
	                        message += String.format("HTTP Enable: %s\n", netGen.getHTTPEnable()?"Yes":"No");
	                        message += String.format("LPD Enable: %s\n", netGen.getLPDEnable()?"Yes":"No");
	                        message += String.format("NetBIOS Enable: %s\n", netGen.getNetBIOSEnable()?"Yes":"No");
	                        message += String.format("Netcenter Enable: %s\n", netGen.getNetcenterEnable()?"Yes":"No");
	                        message += String.format("Gratuitous ARP Period: %d\n", netGen.getGratuitousARPPeriod());
	                    }
	                    ShowMessageBox(message,"Network General Settings");
	                }
	                //Wifi
	                else if (selectedItemIndex == 12){
	                    
	                    NetworkWirelessSettings_DPL wifiSettings = new NetworkWirelessSettings_DPL();
	                    wifiSettings.queryPrinter(conn,1000);

	                    if (wifiSettings.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        //DNS Settings
	                        message += String.format("Static DNS Enable: %s\n", wifiSettings.getEnableStaticDNS()?"Yes":"No");
	                        message += String.format("Preferred DNS Server: %s\n", wifiSettings.getPreferredDNSServer());
	                        message += String.format("Secondary DNS Server: %s\n", wifiSettings.getSecondaryDNSServer());
	                        message += String.format("DNS Suffix: %s\n", wifiSettings.getDNSSuffix());
	                        
	                        //Wifi G
	                        message += String.format("MTU: %d\n", wifiSettings.getMTU());
	                        message += String.format("Ad-Hoc Channel: %d\n", wifiSettings.getAdHocChannel());
	                        message += String.format("Region Code: %s\n", wifiSettings.getRegionCode());
	                        message += String.format("EAP Realm: %s\n", wifiSettings.getEAPRealm());

	                        //Network Settings
	                        message += String.format("Inactive Timeout: %d\n", wifiSettings.getInactiveTimeout());
	                        message += String.format("IP Address Method: %s\n", wifiSettings.getIPAddressMethod());
	                        message += String.format("Active IP Address: %s\n", wifiSettings.getActiveIPaddress());
	                        message += String.format("Active Subnet Mask: %s\n", wifiSettings.getActiveSubnetMask());
	                        message += String.format("Printer DNS name: %s\n", wifiSettings.getPrinterDNSName());
	                        message += String.format("Register to DNS: %s\n", wifiSettings.getRegisterToDNS()?"Yes":"No");
	                        message += String.format("Active Gateway: %s\n", wifiSettings.getActiveGatewayAddress());
	                        message += String.format("UDP Port: %d\n", wifiSettings.getUDPPort());
	                        message += String.format("TCP Port: %d\n", wifiSettings.getTCPPort());
	                        message += String.format("Use DNS Suffix: %s\n", wifiSettings.getUseDNSSuffix()?"Yes":"No");
	                        message += String.format("Enable Connection Status: %s\n", wifiSettings.getEnableConnectionStatus()?"Yes":"No");
	                        message += String.format("DHCP User Class Option: %s\n", new String(wifiSettings.getDHCPUserClassOption()));
	                        message += String.format("Static IP Address: %s\n", wifiSettings.getStaticIPAddress());
	                        message += String.format("Static Subnet Mask: %s\n", wifiSettings.getStaticSubnetMask());
	                        message += String.format("Static Gateway: %s\n", wifiSettings.getStaticGateway());
	                        message += String.format("LPD Port: %d\n", wifiSettings.getLPDPort());
	                        message += String.format("LPD Enable: %s\n", wifiSettings.getLPDEnable()?"Yes":"No");
	                        
	                        
	                        //Wifi Settings
	                        message += String.format("Network Type: %s\n", wifiSettings.getNetworkType());
	                        message += String.format("ESSID: %s\n", wifiSettings.getESSID());
	                        message += String.format("EAP Type: %s\n", wifiSettings.getEAPType().name());
	                        message += String.format("Wifi Security: %d\n", wifiSettings.getWiFiSecurityType());
	                        message += String.format("Authentication Type: %d\n", wifiSettings.getAuthenticationType());
	                        message += String.format("Inner Authentication: %d\n", wifiSettings.getInnerAuthenticationType());
	                        message += String.format("WEP Data Encryption Enable: %s\n", wifiSettings.getWEPDataEncryption()?"Yes":"No");
	                        message += String.format("Show Signal Strength: %s\n", wifiSettings.getShowSignalStrength()?"Yes":"No");
	                        message += String.format("Power Saving Mode: %s\n", wifiSettings.getPowerSavingMode()?"Yes":"No");
	                        message += String.format("Group Cipher: %d\n", wifiSettings.getGroupCipher());
	                        message += String.format("MAC Address: %s\n", wifiSettings.getWiFiMACAddress());
	                        message += String.format("Regulatory Domain: %d\n", wifiSettings.getRegulatoryDomain());
	                        message += String.format("Radio Mode: %s\n", wifiSettings.getRadioMode().name());
	                        message += String.format("Wifi Testing Mode Enable: %s\n", wifiSettings.getWiFiTestingModeEnable()?"Yes":"No");
	                        message += String.format("Max Active Channel Dwell Time: %d\n", wifiSettings.getMaxActiveChannelDwellTime());
	                        message += String.format("Min Active Channel Dwell Time: %d\n", wifiSettings.getMinActiveChannelDwellTime());
	                        message += String.format("Active Scanning Radio Channel: %s\n", wifiSettings.getActiveRadioScanChannel());
	                        message += String.format("Use Hex PSK: %s\n", wifiSettings.getUseHexPSK()?"Yes":"No");
	                    }
	                    
	                    ShowMessageBox(message,"Network Wireless Settings");
	                }
	            }
	            //EZ and LP mode
	            else {
	                
	                //Avalanche Settings
	                if (selectedItemIndex == 0){
	                    
	                    AvalancheSettings avaSettings = new AvalancheSettings();
	                    avaSettings.queryPrinter(conn,1000);

	                    if (avaSettings.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Agent IP: %s\n", avaSettings.getAgentIP());
	                        message += String.format("Show All Data on Self Test: %s\n", avaSettings.getShowAllData()?"Yes":"No");
	                        message += String.format("Agent Name: %s\n", avaSettings.getAgentName());
	                        message += String.format("Agent Port: %d\n", avaSettings.getAgentPort());
	                        message += String.format("Connection Type: %s\n", avaSettings.getConnectionType().name());
	                        message += String.format("Avalanche Enable: %s\n", avaSettings.getIsAvalancheEnabled()?"Yes":"No");
	                        message += String.format("Printer Name: %s\n", avaSettings.getPrinterName());
	                        message += String.format("Printer Model: %s\n", avaSettings.getPrinterModelName());
	                        message += String.format("Is Prelicensed: %s\n", avaSettings.getIsPrelicensed()?"Yes":"No");
	                        message += String.format("Printer Result Flag: %s\n", avaSettings.getPrinterResultFlag()?"Yes":"No");
	                        message += String.format("Update Interval: %d\n", avaSettings.getUpdateInterval());
	                        message += String.format("Update Flags: %d\n", avaSettings.getUpdateFlags());
	                        message += String.format("Is Wired: %s\n", avaSettings.getIsWired()?"Yes":"No");
	                    }
	                    ShowMessageBox(message,"Avalanche Settings");
	                    
	                }
	                
	                //Battery Condition
	                else if (selectedItemIndex == 1){
	                    
	                    BatteryCondition battCond = new BatteryCondition();
	                    battCond.queryPrinter(conn,1000);

	                    if (battCond.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Power Source Plugged in: %s\n", battCond.getChargerConnected()?"Yes":"No");
	                        message += String.format("Power Source: %s\n", battCond.getPowerSource().name());
	                        message += String.format("Battery Temperature: %f\n", battCond.getBatteryTemperature());
	                        message += String.format("Voltage Battery: %f\n", battCond.getVoltageBatterySingle());
	                        message += String.format("Voltage Battery 1: %f\n", battCond.getVoltageBattery1());
	                        message += String.format("Votlage Battery 2: %f\n", battCond.getVoltageBattery2());
	                        message += String.format("Voltage of Battery Eliminator: %f\n", battCond.getVoltageBatteryEliminator());
	                    }
	                    ShowMessageBox(message,"Battery Condition");

	                }
	                //Bluetooth Config
	                else if (selectedItemIndex == 2){
	                    BluetoothConfiguration btConfig = new BluetoothConfiguration();
	                    btConfig.queryPrinter(conn,1000);
	                   
	                    if (btConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Authentication Enable: %s\n", btConfig.getAuthentication()?"Yes":"No");
	                        message += String.format("MAC Address: %s\n", btConfig.getBluetoothAddress());
	                        message += String.format("Bondable: %s\n", btConfig.getBondable()?"Yes":"No");
	                        message += String.format("Connectable: %s\n", btConfig.getConnectable()?"Yes":"No");
	                        message += String.format("Discoverable: %s\n", btConfig.getDiscoverable()?"Yes":"No");
	                        message += String.format("Friendly Name: %s\n", btConfig.getFriendlyName());
	                        message += String.format("Inactivity timeout: %d\n", btConfig.getInactivityTimeout());
	                        message += String.format("Passkey enable: %s\n", btConfig.getPasskey()?"Yes":"No");
	                        message += String.format("Bluetooth Profile: %s\n", btConfig.getProfile());
	                        message += String.format("Service Name: %s\n", btConfig.getServiceName());
	                        message += String.format("Watchdog Period: %d\n", btConfig.getWatchdogPeriod());
	                    }
	                    ShowMessageBox(message,"Bluetooth Config");
	                }
	                
	                //Font List
	                else if (selectedItemIndex == 3){
	                    FontList fontList = new FontList();
	                    fontList.queryPrinter(conn,1000);
	                   
	                    if (fontList.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        List<FontData> files = fontList.getFonts();
	                        for (FontData font : files) {
	                            message += String.format("Five Character Name: %s\n", font.getFiveCharacterName());
	                            message += String.format("One Character Name: %s\n", font.getOneCharacterName());
	                            message += String.format("Memory Location: %s\n", font.getMemoryLocation());
	                            message += String.format("User Date: %s\n", font.getUserDate());
	                            message += String.format("Description: %s\n", font.getUserDescription());
	                            message += String.format("Version: %s\n", font.getUserVersion());
	                            message += "\n";
	                        }
	                    }
	                    ShowMessageBox(message,"Font List");
	                }
	                //Format list
	                else if (selectedItemIndex == 4){
	                    FormatList formatList = new FormatList();
	                    formatList.queryPrinter(conn,1000);

	                    if (formatList.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        List<FormatData> files = formatList.getFormats();
	                        for (FormatData formatData : files) {
	                            message += String.format("Five Character Name: %s\n", formatData.getFiveCharacterName());
	                            message += String.format("One Character Name: %s\n", formatData.getOneCharacterName());
	                            message += String.format("Memory Location: %s\n", formatData.getMemoryLocation());
	                            message += String.format("User Date: %s\n", formatData.getUserDate());
	                            message += String.format("Description: %s\n", formatData.getUserDescription());
	                            message += String.format("Version: %s\n", formatData.getUserVersion());
	                            message += "\n";
	                        }
	                    }
	                    ShowMessageBox(message,"Format List");

	                }
	                //General Config
	                else if (selectedItemIndex == 5){
	                    GeneralConfiguration genConfig = new GeneralConfiguration();
	                    genConfig.queryPrinter(conn,1000);

	                    if (genConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("White Space Advance Enable: %s\n", genConfig.getWhiteSpaceAdvance()?"Yes":"No");
	                        message += String.format("Serial Baud Rate: %s\n", genConfig.getBaudRate().name());
	                        message += String.format("Darkness Adjustment: %d\n", genConfig.getDarknessAdjustment());
	                        message += String.format("Form Feed Enable: %s\n", genConfig.getFormFeed()?"Yes":"No");
	                        message += String.format("Charger Beep Enable: %s\n", genConfig.getChargerBeep()?"Yes":"No");
	                        message += String.format("Sound Enable(Beeper On): %s\n", genConfig.getSoundEnabled()?"Yes":"No");
	                        message += String.format("Serial Handshake: %s\n", genConfig.getRS232Handshake().name());
	                        message += String.format("Lines Per Page: %d\n", genConfig.getLinesPerPage());
	                        message += String.format("Print Job Status Report Enable: %s\n", genConfig.getEZPrintJobStatusReport()?"Yes":"No");
	                        message += String.format("Default Protocol: %s\n", genConfig.getDefaultProtocol());
	                        message += String.format("Self Test Print Language: %d\n", genConfig.getSelfTestPrintLanguage());
	                        message += String.format("Form Feed Centering: %s\n", genConfig.getFormFeedCentering()?"Yes":"No");
	                        message += String.format("Serial Data Bits: %d\n", genConfig.getRS232DataBits());
	                        message += String.format("Serial Parity: %s\n", genConfig.getRS232Parity().name());
	                        message += String.format("Form Feed Button Disabled: %s\n", genConfig.getFormfeedButtonDisabled()?"Yes":"No");
	                        message += String.format("Power Button Disabled: %s\n", genConfig.getPowerButtonDisabled()?"Yes":"No");
	                        message += String.format("RF Button Disabled: %s\n", genConfig.getPowerButtonDisabled()?"Yes":"No");
	                        message += String.format("QStop Multiplier: %d\n", genConfig.getQStopMultiplier());
	                        message += String.format("RF Timeout: %d\n", genConfig.getRFPowerTimeout());
	                        message += String.format("System Timeout: %s\n", genConfig.getSystemTimeout());
	                        message += String.format("Special Test Print: %d\n", genConfig.getSpecialTestPrint());
	                        message += String.format("Paper Out Beep: %s\n", genConfig.getPaperOutBeep().name());
	                        message += String.format("USB Class: %s\n", genConfig.getUSBClass().name());
	                        message += String.format("Using USB: %s\n", genConfig.getUsingUSB()?"Yes":"No");
	                        message += String.format("Deep Sleep Enable: %s\n", genConfig.getDeepSleep()?"Yes":"No");
	                    }
	                    	ShowMessageBox(message,"General Configuration");

	                }
	                //General status
	                else if (selectedItemIndex == 6){
	                    GeneralStatus genStatus = new GeneralStatus();
	                    genStatus.queryPrinter(conn,1000);

	                    if (genStatus.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Battery Temp and Voltage Status: %s\n", genStatus.getBatteryTempandVoltageStatus().name());
	                        message += String.format("Error Status: %s\n", genStatus.getErrorStatus().name());
	                        message += String.format("Paper Jam: %s\n", genStatus.getPaperJam());
	                        message += String.format("Printer Status: %s\n", genStatus.getPrinterStatus().name());
	                        message += String.format("Remaining RAM: %d\n", genStatus.getRemainingRAM());
	                        message += String.format("Paper Present: %s\n", genStatus.getPaperPresent());
	                        message += String.format("Head Lever Position: %s\n", genStatus.getHeadLeverPosition());
	                    }
	                    ShowMessageBox(message,"General Status");
	                }
	                //Graphic List
	                else if (selectedItemIndex == 7){
	                    GraphicList graphList = new GraphicList();
	                    graphList.queryPrinter(conn,1000);
	                    
	                    if (graphList.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        List<GraphicData> files = graphList.getGraphics();
	                        for (GraphicData graphic : files) {
	                            message += String.format("Name: %s\n", graphic.getFiveCharacterName());
	                            message += String.format("Name: %s\n", graphic.getOneCharacterName());
	                            message += String.format("Memory Location: %s\n", graphic.getMemoryLocation());
	                            message += String.format("User Date: %s\n", graphic.getUserDate());
	                            message += String.format("Description: %s\n", graphic.getUserDescription());
	                            message += String.format("Version: %s\n", graphic.getUserVersion());
	                            message += "\n";
	                        }
	                    }
	                    
	                    ShowMessageBox(message,"Graphic List");
	                }
	                //IrDA Config
	                else if (selectedItemIndex == 8){
	                    IrDAConfiguration irDAConfig = new IrDAConfiguration();
	                    irDAConfig.queryPrinter(conn,1000);
	                   
	                    if (irDAConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Direct Version: %s\n", irDAConfig.getDirectVersion());
	                        message += String.format("IrDA Name: %s\n", irDAConfig.getIrDAName());
	                        message += String.format("IrDA Nickname: %s\n", irDAConfig.getIrDANickname());
	                        message += String.format("IrDA Version: %s\n", irDAConfig.getIrDAVersion());
	                        message += String.format("Protocol: %s\n", irDAConfig.getProtocol());
	                    }
	                    ShowMessageBox(message,"IrDA Config");
	                }
	                //Label Config
	                else if (selectedItemIndex == 9){
	                    LabelConfiguration labelConfig = new LabelConfiguration();
	                    labelConfig.queryPrinter(conn,1000);

	                    if (labelConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Backup Distance: %d\n", labelConfig.getBackUpDistance());
	                        message += String.format("Use Presenter: %s\n", labelConfig.getUsePresenter()?"Yes":"No");
	                        message += String.format("Auto QMark Advance: %s\n", labelConfig.getAutoQMarkAdvance()?"Yes":"No");
	                        message += String.format("Backup Offset: %d\n", labelConfig.getBackupOffset());
	                        message += String.format("Horizontal Offset: %d\n", labelConfig.getHorizontalOffset());
	                        message += String.format("QMark Stop Length: %d\n", labelConfig.getQMarkStopLength());
	                        message += String.format("Additional Self Test Prints: %d\n", labelConfig.getAdditionalSelfTestPrints());
	                        message += String.format("Max QMark Advance: %d\n", labelConfig.getMaximumQMarkAdvance());
	                        message += String.format("QMARKB offset: %d\n", labelConfig.getQMARKBOffset());
	                        message += String.format("QMARKG Offset: %d\n", labelConfig.getQMARKGOffset());
	                        message += String.format("QMARKT Offset: %d\n", labelConfig.getQMARKTOffset());
	                        message += String.format("White QMark Enable: %s\n", labelConfig.getWhiteQMark()?"Yes":"No");
	                        message += String.format("Paperout Sensor: %s\n", labelConfig.getPaperoutSensor().name());
	                        message += String.format("Paper Stock Type: %s\n", labelConfig.getPaperStockType().name());
	                        message += String.format("Presenter Timeout: %d\n", labelConfig.getPresenterTimeout());
	                        message += String.format("Auto QMark Backup: %s\n", labelConfig.getAutoQMarkBackup()?"Yes":"No");
	                    }
	                    ShowMessageBox(message,"Label Config");

	                }
	                //Magnetic Card
	                else if (selectedItemIndex == 10){
	                    MagneticCardConfiguration magConfig = new MagneticCardConfiguration();
	                    magConfig.queryPrinter(conn,1000);
	                    
	                    if (magConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Auto Print: %s\n", magConfig.getAutoPrint()?"Yes":"No");
	                        message += String.format("Card Read Direction: %s\n", magConfig.getCardReadDirection());
	                        message += String.format("Magnetic Card Enabled: %s\n", magConfig.getEnabled()?"Yes":"No");
	                        message += String.format("Auto Send: %s\n", magConfig.getAutoSend()?"On":"Off");
	                        message += String.format("Track 1 Enabled: %s\n", magConfig.getTrack1Enabled()?"Yes":"No");
	                        message += String.format("Track 2 Enabled: %s\n", magConfig.getTrack2Enabled()?"Yes":"No");
	                        message += String.format("Track 3 Enabled: %s\n", magConfig.getTrack3Enabled()?"Yes":"No");
	                    }
	                    ShowMessageBox(message,"Magnetic Card Config");
	                }
	                //Magnetic Card Data
	                else if (selectedItemIndex == 11){
	                    MagneticCardData magCardData = new MagneticCardData();
	                    magCardData.queryPrinter(conn,1000);
	                    
	                    if (magCardData.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Track 1 Data: %s\n", magCardData.getTrack1Data());
	                        message += String.format("Track 2 Data: %s\n", magCardData.getTrack2Data());
	                        message += String.format("Track 3 Data: %s\n", magCardData.getTrack3Data());
	                    }
	                    ShowMessageBox(message,"Magnetic Card Data");

	                }
	                //Manufacturing Date
	                else if (selectedItemIndex == 12){
	                    ManufacturingDate manuDate = new ManufacturingDate();
	                    manuDate.queryPrinter(conn,1000);
	   
	                    if (manuDate.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Manufacturing Date: %s\n", manuDate.getMD());
	                    }
	                    ShowMessageBox(message,"Manufacturing Date");
	                }
	                //Memory status
	                else if (selectedItemIndex == 13){
	                    MemoryStatus memStatus = new MemoryStatus();
	                    memStatus.queryPrinter(conn,1000);
	  
	                    if (memStatus.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Download memory remaining: %d\n", memStatus.getDownloadMemoryRemaining());
	                        message += String.format("Download memory total: %d\n", memStatus.getDownloadMemoryTotal());
	                        message += String.format("EEPROM Size: %d\n", memStatus.getEEPROMSize());
	                        message += String.format("Flash Memory Size: %d\n", memStatus.getFlashMemorySize());
	                        message += String.format("RAM size: %d\n", memStatus.getRAMSize());
	                        message += String.format("Flash type: %s\n", memStatus.getFlashType());
	                        message += String.format("Download Format Memory Remaining: %d\n", memStatus.getDownloadFormatMemoryRemaining());
	                        message += String.format("Download Format Memory Total: %d\n", memStatus.getDownloadFormatMemoryTotal());
	                    }
	                    ShowMessageBox(message,"Memory status");

	                }
	                //Printer Options
	                else if (selectedItemIndex == 14){
	                    PrinterOptions printerOpt = new PrinterOptions();
	                    printerOpt.queryPrinter(conn,1000);
	
	                    if (printerOpt.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("SCR Device: %d\n", printerOpt.getSCRDevice());
	                        message += String.format("CF Device: %d\n", printerOpt.getCFDevice());
	                        message += String.format("Printer Description: %s\n", printerOpt.getPrinterDescription());
	                        message += String.format("Part Number: %s\n", printerOpt.getPartNumber());
	                        message += String.format("Serial Number: %s\n", printerOpt.getSerialNumber());
	                        message += String.format("Printer Type: %d\n", printerOpt.getPrinterType());
	                        message += String.format("SPI Device: %d\n", printerOpt.getSPIDevice());
	                        message += String.format("Manufacturing Date: %s\n", printerOpt.getManufacturingDate());
	                        message += String.format("Text Fixture String: %s\n", printerOpt.getTextFixtureString());
	                        message += String.format("SDIO Device: %d\n", printerOpt.getSDIODevice());
	                        message += String.format("Certification Flag Status: %s\n", printerOpt.getCertificationFlagStatus()?"On":"Off");
	                    }
	                    ShowMessageBox(message,"Printer Options");
	                }
	                //PrintHead Status
	                else if (selectedItemIndex == 15){
	                    PrintheadStatus printHeadStats = new PrintheadStatus();
	                    printHeadStats.queryPrinter(conn,1000);
	           
	                    if (printHeadStats.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("DPI: %d\n", printHeadStats.getDPI());
	                        message += String.format("PrintHead Model: %s\n", printHeadStats.getPrintheadModel());
	                        message += String.format("Print Time: %d\n", printHeadStats.getPrintTime());
	                        message += String.format("PrintHead Pins: %d\n", printHeadStats.getPrintheadPins());
	                        message += String.format("PrintHead Temperature: %f\n", printHeadStats.getPrintheadTemperature());
	                        message += String.format("PrintHead Width: %d\n", printHeadStats.getPrintheadWidth());
	                        message += String.format("Page Width: %d\n", printHeadStats.getPageWidth());
	                    }
	                    ShowMessageBox(message,"Print Head Status");

	                }
	                //Serial Number
	                else if (selectedItemIndex == 16){
	                    SerialNumber serialNum = new SerialNumber();
	                    serialNum.queryPrinter(conn,1000);
	
	                    if (serialNum.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Serial Number: %s\n", serialNum.getSN());
	                    }
	                    ShowMessageBox(message,"Serial Number");
	                }
	                //Smart Card Config
	                else if (selectedItemIndex == 17){
	                    SmartCardConfiguration smartCardConfig = new SmartCardConfiguration();
	                    smartCardConfig.queryPrinter(conn,1000);
	                   
	                    if (smartCardConfig.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Command Format: %s\n", smartCardConfig.getCommandFormat());
	                        message += String.format("Enable: %s\n", smartCardConfig.getEnabled()?"Yes":"No");
	                        message += String.format("Memory Tye: %s\n", smartCardConfig.getMemoryType());
	                        message += String.format("Response Format: %s\n", smartCardConfig.getResponseFormat());
	                        message += String.format("Smart Card Protocol: %s\n", smartCardConfig.getProtocol());
	                        message += String.format("Smart Card Type: %s\n", smartCardConfig.getType());
	                    }
	                    ShowMessageBox(message,"Smart Card Config");
	                }
	                //SSP_Comm
	                else if (selectedItemIndex == 18){
	                    SSP_CommParameters sspCom = new SSP_CommParameters();
	                    sspCom.queryPrinter(conn,1000);
	                  
	                    if (sspCom.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Serial BaudRate: %s\n", sspCom.getBaudRate().name());
	                        message += String.format("Handshake: %s\n", sspCom.getRS232Handshake().name());
	                        message += String.format("Data Bits: %d\n", sspCom.getRS232DataBits());
	                        message += String.format("Parity: %s\n", sspCom.getRS232Parity().name());
	                    }
	                    ShowMessageBox(message,"Serial Port Config");
	                }
	                //TCPIPStatus
	                else if (selectedItemIndex == 19){
	                    TCPIPStatus tcpStatus = new TCPIPStatus();
	                    tcpStatus.queryPrinter(conn,1000);
	                    
	                    if (tcpStatus.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Wireless Card Info: %s\n", tcpStatus.getWirelessCardInfo());
	                        message += String.format("Valid Cert. Present: %s\n", tcpStatus.getValidCertificatePresent()?"Yes":"No");
	                        message += String.format("Conn. Reporting Enable: %s\n", tcpStatus.getConnectionReporting()?"Yes":"No");
	                        message += String.format("Acquired IP: %s\n", tcpStatus.getAcquireIP().name());
	                        message += String.format("Radio Disable: %s\n", tcpStatus.getRadioDisabled()?"Yes":"No");
	                        message += String.format("ESSID: %s\n", tcpStatus.getESSID());
	                        message += String.format("EAP Type: %s\n", tcpStatus.getEAPType().name());
	                        message += String.format("Gateway Address: %s\n", tcpStatus.getGatewayAddress());
	                        message += String.format("IP Address: %s\n", tcpStatus.getIPAddress());
	                        message += String.format("Inactivity Timeout: %d\n", tcpStatus.getInactivityTimeout());
	                        message += String.format("Key to Use: %d\n", tcpStatus.getKeyToUse());
	                        message += String.format("Key 1 Type: %s\n", tcpStatus.getKey1Type().name());
	                        message += String.format("Key 2 Type: %s\n", tcpStatus.getKey2Type().name());
	                        message += String.format("Key 3 Type: %s\n", tcpStatus.getKey3Type().name());
	                        message += String.format("Key 4 Type: %s\n", tcpStatus.getKey4Type().name());
	                        message += String.format("Subnet Mask: %s\n", tcpStatus.getSubnetMask());
	                        message += String.format("MAC Address: %s\n", tcpStatus.getMACAddress());
	                        message += String.format("Station Name: %s\n", tcpStatus.getStationName());
	                        message += String.format("Network Authentication: %s\n", tcpStatus.getNetworkAuthentication().name());
	                        message += String.format("TCP Printing Port: %d\n", tcpStatus.getTCPPrintingPort());
	                        message += String.format("Power Saving Mode: %s\n", tcpStatus.getPowerSavingMode()?"Yes":"No");
	                        message += String.format("Phase 2 Method: %s\n", tcpStatus.getPhase2Method().name());
	                        message += String.format("UDP Printing Port: %d\n", tcpStatus.getUDPPrintingPort());
	                        message += String.format("Card Powered: %s\n", tcpStatus.getCardPowered()?"On":"Off");
	                        message += String.format("Signal Quality Indicator: %s\n", tcpStatus.getSignalQualityIndicator()?"Yes":"No");
	                        message += String.format("Authentication Algorithm: %s\n", tcpStatus.getAuthenticationAlgorithm().name());
	                        message += String.format("Station Type: %s\n", tcpStatus.getStationType().name());
	                        message += String.format("Encryption Enabled: %d\n", tcpStatus.getEncryptionEnabled());
	                        message += String.format("Current Certificate CRC: %s\n", tcpStatus.getCurrentCertificateCRC());
	                        message += String.format("DNS1 Address: %s\n", tcpStatus.getDNS1Address());
	                        message += String.format("Register to DNS: %s\n", tcpStatus.getRegisterToDNS()?"Yes":"No");
	                        message += String.format("DNS2 Address: %s\n", tcpStatus.getDNS2Address());
	                        message += String.format("Static DNS Enable: %s\n", tcpStatus.getStaticDNS()?"Yes":"No");
	                        message += String.format("Group Cipher: %d\n", tcpStatus.getGroupCipher());
	                        message += String.format("Radio Type: %s\n", tcpStatus.getRadioType().name());
	                        message += String.format("Use DNS: %s\n", tcpStatus.getUseDNS()?"Yes":"No");
	                        message += String.format("DNS Suffix: %s\n", tcpStatus.getDNSSuffix());
	                        message += String.format("Encryption Key Size: %d\n", tcpStatus.getEncryptionKeySize());
	                        message += String.format("Encryption Key Type: %d\n", tcpStatus.getEncryptionKeyType());
	                    }
	                    ShowMessageBox(message,"TCP/IP Status");

	                }
	                //Upgrade Data
	                else if (selectedItemIndex == 20){
	                    UpgradeData upgradeData = new UpgradeData();
	                    upgradeData.queryPrinter(conn,1000);
	                    
	                    if (upgradeData.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Path and File: %s\n", upgradeData.getPathAndFile());
	                        message += String.format("Server IP: %s\n", upgradeData.getServerIPAddress());
	                        message += String.format("Server Port: %d\n", upgradeData.getServerPort());
	                        message += String.format("Upgrade Type: %d\n", upgradeData.getDataType());
	                        message += String.format("Upgrade Package Version: %s\n", upgradeData.getVersion());
	                    }
	                    ShowMessageBox(message,"Auto Update Settings");

	                }
	                //Version Info
	                else if (selectedItemIndex == 21){
	                    VersionInformation versionInfo = new VersionInformation();
	                    versionInfo.queryPrinter(conn,1000);
	                   
	                    if (versionInfo.getValid() == false) {
	                        message += "No response from printer\r\n";
	                    }
	                    else
	                    {
	                        message += String.format("Boot Version: %s\n", versionInfo.getBootVersion());
	                        message += String.format("Comm Controller Version: %s\n", versionInfo.getCommControllerVersion());
	                        message += String.format("Download version: %s\n", versionInfo.getDownloadVersion());
	                        message += String.format("Firmware version: %s\n", versionInfo.getFirmwareVersion());
	                        message += String.format("Hardware Controller Version: %s\n", versionInfo.getHardwareControllerVersion());
	                        message += String.format("SCR Version: %s\n", versionInfo.getSCRVersion());
	                        message += String.format("Build Timestamp: %s\n", versionInfo.getBuildTimestamp());
	                    }
	                    
	                    ShowMessageBox(message,"Version Info");
	                }
	            }
	            Thread.sleep(1500);
				//signals to close connection
				conn.close();
                DisplayPrintingStatusMessage("Query success.");
                EnableControls(true);
			}

		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			//signals to close connection
			if(conn != null)
				conn.close();
			e.printStackTrace();
			DisplayPrintingStatusMessage("Error: " + e.getMessage());
			ShowMessageBox(e.getMessage(),"Application Error");
			EnableControls(true);
		}
	}// run()


}
