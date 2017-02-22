package com.intellisysla.printexample;

import java.io.Serializable;

/**Class to save the application settings**/

public class DOPrintSettings implements Serializable{

	private static final long serialVersionUID = 4762643389154364957L;
	
	//Set and get printers address
	private String printerAddress;
	
	private int communicationMethod;
	
	private int selectedPrinterPort;
	
	private String selectedFilePath;
	
	private String selectedFolderPath;
	
	private int selectedItemIndex;
	
	private int selectedModeIndex;
	
	private int selectedAction;
	
	//Accessors for selected action;
	public int getSelectedAction()
	{
		return selectedAction;
	}
	public void setSelectedAction(int value)
	{
		selectedAction = value;
	}
	
	//Accessors for selected item index;
	public int getSelectedItemIndex()
	{
		return selectedItemIndex;
	}
	public void setSelectedItemIndex(int value)
	{
		selectedItemIndex = value;
	}
	
	//Accessors for selected mode index;
	public int getSelectedModeIndex()
	{
		return selectedModeIndex;
	}
	public void setSelectedModeIndex(int value)
	{
		selectedModeIndex = value;
	}
	
	public String getPrinterAddress() {
		return printerAddress;
	}
	public void setPrinterAddress(String value) {
		printerAddress = value;
	}
	//Set and get printer's port
	
	public int getPrinterPort() {
		return selectedPrinterPort;
	}
	public void setPrinterPort(int value) {
		selectedPrinterPort = value;
	}
	//set and get communcation method
	public int getCommunicationMethod()
	{
		return communicationMethod;
	}
	public void setCommunicationMethod(int value)
	{
		communicationMethod = value;
	}
	//set and get folder path
	public String getSelectedFolderPath()
	{
		return selectedFolderPath;
	}
	public void setSelectedFolderPath(String value)
	{
		selectedFolderPath = value;
	}
	
	//Set and get selectedFile Path	
	public String getSelectedFilePath() {
		return selectedFilePath;
	}
	public void setSelectedFilePath(String value) {
		selectedFilePath = value;
	}
	
	//Constructor
	public DOPrintSettings(String address,int port, String folderPath,String filePath, 
			int commMethod, int itemIndex, int modeIndex, int action){
		printerAddress = address;
		selectedPrinterPort  = port;
		selectedFolderPath = folderPath;
		selectedFilePath = filePath;
		communicationMethod= commMethod;
		selectedItemIndex = itemIndex;
		selectedModeIndex = modeIndex;
		selectedAction = action;
		
	}


}
