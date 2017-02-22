package com.intellisysla.printexample;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;

/**
 * 
	Class activity that allows users to browse through files and folders
 *
 */
public class FileBrowseActivity extends ListActivity {

	//Keys for file name and directory
    public final static String EXTRA_SHOW_HIDDEN_FILES = "show_hidden_files";
    public final static String EXTRA_ACCEPTED_FILE_EXTENSIONS = "accepted_file_extensions";
    private final static String DEFAULT_INITIAL_DIRECTORY = "/mnt";

    protected File currentFile;
    protected ArrayList<File> filesList;
    protected FileBrowseListAdapter fileAdapter;
    protected boolean showHidden = false;
    protected String[] acceptedFileExtensions;
    
    Button backButton;
    TextView currentPath;
    
    private String fileName;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_view_main);
        
        
        // Set the view to be shown if the list is empty
        LayoutInflater inflator = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View emptyView = inflator.inflate(R.layout.file_empty_view, null);
        ((ViewGroup)getListView().getParent()).addView(emptyView);
        getListView().setEmptyView(emptyView);
        
        

        // Set initial directory
        currentFile = new File(DEFAULT_INITIAL_DIRECTORY);
        currentPath = (TextView)findViewById(R.id.current_folder_path);
        currentPath.setText(DEFAULT_INITIAL_DIRECTORY);
        // Initialize the ArrayList
        filesList = new ArrayList<File>();

        // Set the ListAdapter
        fileAdapter = new FileBrowseListAdapter(this, filesList);
        setListAdapter(fileAdapter);


        // Initialize the extensions array to allow any file extensions
        acceptedFileExtensions = new String[] {};

		
		// ---------------------------------------
		// handler for the back button click
		// ---------------------------------------

		backButton = (Button) findViewById(R.id.back_button);
		backButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(currentFile.getParentFile() != null && !currentFile.getParentFile().getAbsolutePath().equals("/")) {
		            // Go to parent directory
		            currentFile = currentFile.getParentFile();
		            currentPath.setText(currentFile.getAbsolutePath());
		            refreshFilesList();
		            return;
		        }
				else
					finish();
			}
		});
        
        // Get intent extras
        if(getIntent().hasExtra(DOPrintMainActivity.FOLDER_PATH_KEY)) {
            currentFile = new File(getIntent().getStringExtra(DOPrintMainActivity.FOLDER_PATH_KEY));
        }
        if(getIntent().hasExtra(EXTRA_SHOW_HIDDEN_FILES)) {
            showHidden = getIntent().getBooleanExtra(EXTRA_SHOW_HIDDEN_FILES, false);
        }
        if(getIntent().hasExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS)) {
            ArrayList<String> collection = getIntent().getStringArrayListExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS);
            acceptedFileExtensions = (String[]) collection.toArray(new String[collection.size()]);
        }
    }

    @Override
    protected void onResume() {
        refreshFilesList();
        super.onResume();
    }

    /**
     * Updates the list view to the current directory
     */
    protected void refreshFilesList() {
        // Clear the files ArrayList
        filesList.clear();

        // Set the extension file filter
        ExtensionFilenameFilter filter = new ExtensionFilenameFilter(acceptedFileExtensions);

        // Get the files in the directory
        File[] files = currentFile.listFiles(filter);
        if(files != null && files.length > 0) {
            for(File f : files) {
                if((f.isHidden() && !showHidden)) {
                    // Don't add the file
                    continue;
                }
                //for the initial directory, show only the sdcard
                if(f.getParentFile().getAbsolutePath().equals(DEFAULT_INITIAL_DIRECTORY))
                {
                	if (f.getAbsolutePath().contains("SdCard") || f.getAbsolutePath().contains("sdcard"))
                	{
                		filesList.add(f);
                	}
                	continue;
                }
       
                // Add the file the ArrayAdapter
                filesList.add(f);
            }

            Collections.sort(filesList, new FileComparator());
        }
        fileAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if(currentFile.getParentFile() != null && !currentFile.getParentFile().getAbsolutePath().equals("/")) {
            // Go to parent directory
            currentFile = currentFile.getParentFile();
            currentPath.setText(currentFile.getAbsolutePath());
            refreshFilesList();
            return;
        }
        super.onBackPressed();

 
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        File newFile = (File)l.getItemAtPosition(position);
        
      //if selected object is a file and not a folder then pass data back to main activity
        if(newFile.isFile()) {
        	 currentFile = newFile;
             // Update path of current folder
             currentPath= (TextView)findViewById(R.id.current_folder_path);
             currentPath.setText(currentFile.getAbsolutePath());
             
             File object = currentFile;
				if(object != null && object.isFile())
				{
					Intent intent = new Intent();
					fileName = object.getName();
					filePath = object.getAbsolutePath();
					
					intent.putExtra(DOPrintMainActivity.FOLDER_NAME_KEY,fileName);
					intent.putExtra(DOPrintMainActivity.FOLDER_PATH_KEY,filePath);
					setResult(RESULT_OK, intent);
					// Finish the activity
					finish();
				}
        } 
        //if item selected is a directory
        else {
            currentFile = newFile;
            // Update path of current folder
            currentPath= (TextView)findViewById(R.id.current_folder_path);
            currentPath.setText(currentFile.getAbsolutePath());
            // Update the files list
            refreshFilesList();
            
        }

        super.onListItemClick(l, v, position, id);
    }

    /**File browse list adapter class used to display**/
    private class FileBrowseListAdapter extends ArrayAdapter<File> {

        private List<File> mObjects;

        public FileBrowseListAdapter(Context context, List<File> objects) {
            super(context, R.layout.file_list_item,android.R.id.text1, objects);
            mObjects = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = null;

            if(convertView == null) { 
                LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.file_list_item, parent, false);
            } 
            else {
                row = convertView;
            }
            
            File object = mObjects.get(position);
            ImageView imageView = (ImageView)row.findViewById(R.id.file_picker_image);
            TextView textView = (TextView)row.findViewById(R.id.file_picker_text);
            
            // Set single line
            textView.setSingleLine(true);

            textView.setText(object.getName());
            if(object.isFile()) {
            	//Check if it is an image
            	Bitmap bmp = BitmapFactory.decodeFile(object.getAbsolutePath());
            	if (bmp != null) {
					imageView.setImageBitmap(bmp);
				}
            	else
            	{
	                // Show the file icon
	                imageView.setImageResource(R.drawable.file);
            	}
            } else {
                // Show the folder icon
                imageView.setImageResource(R.drawable.folder);
            }
            return row;
        }

    }

    /**Class used for sorting files in File list main activity**/
    private class FileComparator implements Comparator<File> {
        public int compare(File f1, File f2) {
            if(f1 == f2) {
                return 0;
            }
            if(f1.isDirectory() && f2.isFile()) {
                // Show directories above files
                return -1;
            }
            if(f1.isFile() && f2.isDirectory()) {
                // Show files below directories
                return 1;
            }
            // Sort the directories alphabetically
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    private class ExtensionFilenameFilter implements FilenameFilter {
        private String[] mExtensions;

        public ExtensionFilenameFilter(String[] extensions) {
            super();
            mExtensions = extensions;
        }

        public boolean accept(File dir, String filename) {
            if(new File(dir, filename).isDirectory()) {
                // Accept all directory names
                return true;
            }
            if(mExtensions != null && mExtensions.length > 0) {
                for(int i = 0; i < mExtensions.length; i++) {
                    if(filename.endsWith(mExtensions[i])) {
                        // The filename ends with the extension
                        return true;
                    }
                }
                // The filename did not match any of the extensions
                return false;
            }
            // No extensions has been set. Accept all file extensions.
            return true;
        }
    }
}