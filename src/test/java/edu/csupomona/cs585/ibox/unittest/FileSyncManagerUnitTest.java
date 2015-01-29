package edu.csupomona.cs585.ibox.unittest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import edu.csupomona.cs585.ibox.sync.GoogleDriveFileSyncManager;


public class FileSyncManagerUnitTest {
	
	
	public Drive mockedDrive;
	public Drive.Files mockedDriveFiles;
	public Drive.Files.Insert mockedDriveFilesInsert;
	public Drive.Files.List mockedDriveFilesList;
	public Drive.Files.Delete mockedDriveFilesDelete;
	public File googleFile1;
	public GoogleDriveFileSyncManager tester;
	java.io.File localFile; 

	@Before
	public void setUp() throws IOException {
		//http://docs.mockito.googlecode.com/hg/latest/org/mockito/Mockito.html#RETURNS_DEEP_STUBS
		mockedDrive = mock(Drive.class,RETURNS_DEEP_STUBS);
		mockedDriveFiles = mock(Drive.Files.class);
		mockedDriveFilesInsert = mock(Drive.Files.Insert.class);
		mockedDriveFilesList = mock(Drive.Files.List.class);
		mockedDriveFilesDelete = mock(Drive.Files.Delete.class);
		googleFile1 = new File();
		// http://junit.org/javadoc/latest/org/junit/Rule.html
		localFile = folder.newFile("text1.txt");
		tester = new GoogleDriveFileSyncManager(mockedDrive);
	}
	
	@After
	public void deleteFile(){
		try{
    		if(localFile.delete()){
    			System.out.println(localFile.getName() + " is deleted!");
    		}else{
    			System.out.println("Delete operation is failed.");
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}
	//http://stackoverflow.com/questions/13443560/how-to-unit-test-an-application-using-google-drive-api-java-client
	//http://junit.org/javadoc/latest/org/junit/Rule.html
	@Rule
    public TemporaryFolder folder= new TemporaryFolder();
	
	@Test
	public void testAddFile()  throws IOException{	
		when(mockedDrive.files()).thenReturn(mockedDriveFiles);
		when(mockedDriveFiles.insert(any(File.class), any(FileContent.class))).thenReturn(mockedDriveFilesInsert);
		when(mockedDriveFilesInsert.execute()).thenReturn(googleFile1);
		googleFile1.setId("AddFileId");
		tester.addFile(localFile); 
		verify(mockedDriveFilesInsert,times(1)).execute();
	}

	@Test
	public void testUpdateFileByAddingTheFileInDriveIfFileNotThere() throws IOException {
		createMockedFile();
		java.io.File toUploadFile = new java.io.File ("fileNotInDrive.txt");
		when(mockedDriveFiles.insert(any(File.class), any(FileContent.class))).thenReturn(mockedDriveFilesInsert);
		when(mockedDriveFilesInsert.execute()).thenReturn(googleFile1);
		googleFile1.setId("AddFileId");
		tester.updateFile(toUploadFile);
		verify(mockedDriveFilesInsert,times(1)).execute();
	}

	@Test
	public void testUpdateFileIfAlreadyInDrive() throws IOException {
		createMockedFile();
		Drive.Files.Update mockedDriveFilesUpdate = mock(Drive.Files.Update.class);
		when(mockedDriveFiles.update(any(String.class), any(File.class), any(FileContent.class))).thenReturn(mockedDriveFilesUpdate);
		File file = new File ();
		file.setId("fileID");
		when(mockedDriveFilesUpdate.execute()).thenReturn(file);
		tester.updateFile(localFile);
		verify(mockedDriveFilesUpdate,times(1)).execute();
	}
	
	@Test
	public void testDeleteValidFile() throws IOException {
		createMockedFile();
		//java.io.File localFile = new java.io.File ("text1.txt");
		when(mockedDriveFiles.delete("fileID1")).thenReturn(mockedDriveFilesDelete);
		tester.deleteFile(localFile);
		verify(mockedDriveFilesDelete,times(1)).execute();
	}
	
	@Test (expected = FileNotFoundException.class)
	public void testShouldThrowExceptionIfFileIdNull() throws IOException {
		createMockedFile();
		java.io.File newFile = new java.io.File ("fileNotInDrive.txt");
		tester.deleteFile(newFile);
	}
	
	@Test
	public void testGetFileId() throws IOException {
		createMockedFile();
		assertEquals(tester.getFileId("text1.txt"), "fileID1");	
	}
	
	
	void createMockedFile () throws IOException {
		when(mockedDrive.files()).thenReturn(mockedDriveFiles);
		when(mockedDriveFiles.list()).thenReturn(mockedDriveFilesList);
		FileList myFileList = new FileList();
		
		when(mockedDriveFilesList.execute()).thenReturn(myFileList);
		
		java.util.List<File> listOfDFiles = new java.util.ArrayList<File>();
		
		myFileList.setItems(listOfDFiles);
		listOfDFiles.add(googleFile1);
		googleFile1.setId("fileID1");
		googleFile1.setTitle("text1.txt");
	}
}
