package edu.csupomona.cs585.ibox.integrationtest;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.List;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import edu.csupomona.cs585.ibox.WatchDir;
import edu.csupomona.cs585.ibox.sync.FileSyncManager;
import edu.csupomona.cs585.ibox.sync.GoogleDriveFileSyncManager;
import edu.csupomona.cs585.ibox.sync.GoogleDriveServiceProvider;

public class IBoxIntegrationTest {

	@ClassRule
	public static TemporaryFolder folder = new TemporaryFolder();
	public static WatchDir watchDir;
	public static FileSyncManager FileSyncManager;
	public static Drive service; 
	@BeforeClass
	public static void setUp() throws IOException {
		service = GoogleDriveServiceProvider.get().getGoogleDriveClient();
		FileSyncManager = new GoogleDriveFileSyncManager(
				GoogleDriveServiceProvider.get().getGoogleDriveClient());

		// Create a local temporary folder to watch for changes
		Path dir = Paths.get(folder.getRoot().getAbsolutePath());
		watchDir = new WatchDir(dir, FileSyncManager);
		System.out.println("IntTestTemp folder: " + folder.getRoot().getAbsolutePath());
	}

	@Test
	public void startTests(){
		// Handling the infinite for loop in public void processEvents()
		// by running the tests in parallel according to the example at
		// http://stackoverflow.com/questions/5529087/how-to-make-junit-test-cases-execute-in-parallel
		Class<?>[] cls = { ParallelStartingTheWatcher.class,
				ParallelRunningFileModificationFunctionTests.class };
		
		// http://sqa.stackexchange.com/questions/6982/junit-parallelcomputer-runs-all-test-classes-as-one
		Result result = JUnitCore.runClasses(ParallelComputer.classes(), cls);
		java.util.List <Failure> failures = result.getFailures();
		for(Failure f : failures){
			String desc = f.toString();
			if(desc.contains("testIntegration"))
				continue;
			System.out.println(f.toString());
		}
		// there will always be one error cased by the timeout of the infinite loop, so we can ignore it
		// if there are more, then a test has failed 
		assertEquals(failures.size(), 1);
	}

	public static class ParallelStartingTheWatcher {
		@SuppressWarnings("deprecation")
		@Rule
		public Timeout globalTimeout = new Timeout(47000);

		// this is not really a test, it is meant to start the infinite loop and then at one
		// point time it out by interrupting it. This will cause an exception that we would like to
		// ignore
		@Test(expected = InterruptedException.class)
		public void testIntegration()
				throws InterruptedException {
			watchDir.processEvents();
		}
	}


	public static class ParallelRunningFileModificationFunctionTests {

		long timeCreated;
		java.io.File localFile;
		@Test
		public void testDriveWorkFlow()
				throws InterruptedException, IOException {
			Thread.sleep(50);
			// in the monitored folder create a file to be uploaded in the Drive
			localFile = folder.newFile();
			writeTextToFile(localFile, "test string"); 

			timeCreated = localFile.lastModified();
			// wait some time until the watcher service has found the file
			// and called the function to add it to the service
			Thread.sleep(15000);
			//check if in Drive there exists a file with the same ID
			assertTrue(getFileId(localFile.getName(), service));

			localFile.setLastModified(timeCreated+1000);
			Thread.sleep(15000);

			localFile.delete();
			Thread.sleep(15000);

			//check if in Drive there exists a file with the same ID
			assertFalse(getFileId(localFile.getName(), service));
		}
	}


	public static void writeTextToFile(java.io.File file, String content) throws IOException {
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter filewriter = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bufferwriter = new BufferedWriter(filewriter);
		bufferwriter.write(content);
		bufferwriter.close();
	}


	public static boolean getFileId(String fileName, Drive service) {
		try {
			List request = service.files().list();
			FileList files = request.execute();
			for(File file : files.getItems()) {
				if (file.getTitle().equals(fileName)) {
					return true;
				}
			}
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
		return false;
	}
}
