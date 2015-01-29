package edu.csupomona.cs585.ibox.unittest;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

import edu.csupomona.cs585.ibox.WatchDir;
import edu.csupomona.cs585.ibox.sync.FileSyncManager;

public class WatchDirTest {


	@ClassRule
	public static TemporaryFolder folder = new TemporaryFolder();
	public static WatchDir watchDir;
	public static FileSyncManagerImpl mockedFileSyncManager;

	@BeforeClass
	public static void setUp() throws IOException {
		mockedFileSyncManager = new FileSyncManagerImpl ();

		// Create a local temporary folder to watch for changes
		Path dir = Paths.get(folder.getRoot().getAbsolutePath());
		watchDir = new WatchDir(dir, mockedFileSyncManager);
		System.out.println("Temp folder: " + folder.getRoot().getAbsolutePath());
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
		List<Failure> failures = result.getFailures();
		for(Failure f : failures){
			String desc = f.toString();
			if(desc.contains("testStartingTheWatcherInParallel"))
				continue;
			System.out.println(f.toString());
		}
		// there will always be one error cased by the timeout of the infinite loop, so we can ignore it
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
		public void testStartingTheWatcherInParallel()
				throws InterruptedException {
			watchDir.processEvents();
		}
	}


	public static class ParallelRunningFileModificationFunctionTests {

		long timeCreated;
		java.io.File localFile;
		@Test
		public void aTestCallingAddFileWhenENTRY_CREATE()
				throws InterruptedException, IOException {
			Thread.sleep(50);
			// in the monitored folder create a file to be uploaded in the Drive
			localFile = folder.newFile();
			//remember the file time stamp
			timeCreated = localFile.lastModified();
			// wait some time until the watcher service has found the file
			// and called the function to add it to the service
			Thread.sleep(15000);
			// check the count of the addCalled variable - it should be called 1
			assertEquals(1, mockedFileSyncManager.addCalled);
			// change the time stamp of the file to new value so that update is triggered
			localFile.setLastModified(timeCreated+1000);
			Thread.sleep(15000);
			assertEquals(1, mockedFileSyncManager.updateCalled);

			localFile.delete();
			Thread.sleep(15000);
			assertEquals(1, mockedFileSyncManager.deleteCalled);
		}


	}
	public static class FileSyncManagerImpl implements FileSyncManager {
		public int addCalled, deleteCalled, updateCalled;

		public void addFile(File localFile) throws IOException {
			++addCalled;
		};

		public void updateFile(File localFile) throws IOException {
			++updateCalled;
		};

		public void deleteFile(File localFile) throws IOException {
			++deleteCalled;
		};
	}
}