package grisu.frontend.gridTests;

import grisu.control.JobConstants;
import grisu.control.ServiceInterface;
import grisu.control.exceptions.ServiceInterfaceException;
import grisu.frontend.control.clientexceptions.MdsInformationException;
import grisu.frontend.control.login.LoginException;
import grisu.frontend.control.login.LoginManager;
import grisu.frontend.gridTests.testElements.GridTestElement;
import grisu.jcommons.constants.GridEnvironment;
import grisu.jcommons.dependencies.ClasspathHacker;
import grisu.jcommons.dependencies.Dependency;
import grisu.jcommons.dependencies.DependencyManager;
import grisu.model.GrisuRegistry;
import grisu.model.GrisuRegistryManager;
import grisu.settings.Environment;
import grisu.utils.GrisuPluginFilenameFilter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;


public class GridTestController {

	/**
	 * @param args
	 * @throws ServiceInterfaceException
	 * @throws MdsInformationException
	 */
	public static void main(String[] args) throws ServiceInterfaceException,
	MdsInformationException {

		String name = GridTestController.class.getName();
		name = name.replace('.', '/') + ".class";
		final URL url = GridTestController.class.getClassLoader().getResource(
				name);
		final String path = url.getPath();
		// System.out.println("Executable path: "+path);
		String baseDir = null;
		if (url.toString().startsWith("jar:")) {
			baseDir = path.toString().substring(path.indexOf(":") + 1,
					path.indexOf(".jar!"));
			baseDir = baseDir.substring(0, baseDir.lastIndexOf("/"));
		} else {
			baseDir = null;
		}

		if (StringUtils.isBlank(baseDir)) {
			baseDir = System.getProperty("user.home") + File.separator
					+ "grisu-grid-tests";
		}

		System.out.println("Using directory: " + baseDir);

		File baseDirFile = new File(baseDir);

		if ( baseDirFile.exists() && (!baseDirFile.canWrite()||baseDirFile.isFile())) {
			System.err.println("Base directory " + baseDir
					+ " is file or can't write to it...");
			System.exit(1);
		}

		File testsDir = new File(baseDirFile, "tests");

		if (testsDir.exists() && (testsDir.isFile() || !testsDir.canWrite())) {
			System.err.println("Tests directory " + baseDir
					+ " is file or can't write to it...");
			System.exit(1);
		}

		boolean testsDirExists = false;
		if (!testsDir.exists()) {
			System.out.println("External tests directory ("
					+ testsDir.toString()
					+ ") doesn't exist. Cloning it from github...");
			try {
				CloneCommand c = Git.cloneRepository();
				c.setDirectory(testsDir);
				c.setBranch("master");

				c.setURI("git://github.com/grisu/grid-tests-templates.git");
				c.call();
				testsDirExists = true;
				System.out.println("Cloning successful.");
			} catch (Exception e) {
				System.err.println("Could not clone tests git repostitory: "
						+ e.getLocalizedMessage());
				System.err
				.println("Please put it in place manually or you'll only be able to use inbuild tests.");
			}
		} else {
			testsDirExists = true;
		}

		if (testsDirExists) {
			try {
				System.out
						.println("Pulling latest changes for external tests from github...");
				FileRepositoryBuilder builder = new FileRepositoryBuilder();
				Repository repository = builder
						.setGitDir(new File(testsDir, ".git")).readEnvironment()
						.findGitDir()
						.build();

				Git git = new Git(repository);

				PullCommand pc = git.pull();
				pc.call();
			} catch (Exception e) {
				System.err
				.println("Could not pull latest tests from git repository: "
						+ e.getLocalizedMessage());
			}
		}

		final GridTestController gtc = new GridTestController(args, baseDir);

		gtc.start();

		System.exit(0);

	}

	private final String grisu_base_directory;

	private final File grid_tests_directory;
	private final ExecutorService submitJobExecutor;

	private final ExecutorService processJobExecutor;
	private final LinkedList<Thread> createAndSubmitJobThreads = new LinkedList<Thread>();

	private final Map<String, Thread> checkAndKillJobThreads = new HashMap<String, Thread>();
	private final Map<String, GridTestElement> gridTestElements = new HashMap<String, GridTestElement>();

	private final List<GridTestElement> finishedElements = new LinkedList<GridTestElement>();

	private ServiceInterface serviceInterface;

	private final GrisuRegistry registry;

	private final String[] gridtestNames;
	private final String[] fqans;
	private String output = null;
	private final String[] excludes;
	private final String[] includes;

	private Date timeoutDate;
	private final int timeout;

	private int sameSubloc = 1;

	private final List<OutputModule> outputModules = new LinkedList<OutputModule>();

	public GridTestController(String[] args, String grisu_base_directory_param) {

		if (StringUtils.isBlank(grisu_base_directory_param)) {
			this.grisu_base_directory = System.getProperty("user.home")
					+ File.separator + "grisu-grid-tests";
		} else {
			this.grisu_base_directory = grisu_base_directory_param;
		}

		// logging stuff
		final SimpleLayout layout = new SimpleLayout();
		try {
			File dir = new File(this.grisu_base_directory);
			dir.mkdirs();
			final FileAppender fa = new FileAppender(layout,
					this.grisu_base_directory + File.separator
					+ "grisu-tests.debug", false);
			final Logger logger = Logger.getRootLogger();
			logger.addAppender(fa);
			logger.setLevel(Level.INFO);

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		Environment.setGrisuDirectory(this.grisu_base_directory);

		final Map<Dependency, String> dependencies = new HashMap<Dependency, String>();

		dependencies.put(Dependency.BOUNCYCASTLE, "jdk15-143");

		DependencyManager.addDependencies(dependencies,
				GridEnvironment.getGridCommonJavaLibDirectory());

		ClasspathHacker.initFolder(Environment.getGrisuPluginDirectory(),
				new GrisuPluginFilenameFilter());

		grid_tests_directory = new File(this.grisu_base_directory, "tests");

		output = this.grisu_base_directory + File.separator + "results"
				+ File.separator + "testResults_" + new Date().getTime()
				+ ".log";

		final GridTestCommandlineOptions options = new GridTestCommandlineOptions(
				args);

		final int threads = options.getSimultaneousThreads();
		submitJobExecutor = Executors.newFixedThreadPool(threads);
		processJobExecutor = Executors.newFixedThreadPool(threads);

		String url = options.getServiceInterfaceUrl();
		if (StringUtils.isBlank(url)) {
			url = "Local";
		}

		try {
			serviceInterface = LoginManager.loginCommandline(url);
		} catch (final LoginException e1) {
			System.out.println("Could not login: " + e1.getLocalizedMessage());
			System.exit(1);
		}

		registry = GrisuRegistryManager.getDefault(this.serviceInterface);

		if (options.getFqans().length == 0) {
			fqans = serviceInterface.getFqans().asArray();
		} else {
			fqans = options.getFqans();
		}

		if ((options.getOutput() != null) && (options.getOutput().length() > 0)) {
			output = options.getOutput();
		}

		timeout = options.getTimeout();

		sameSubloc = options.getSameSubmissionLocation();

		if (options.listTests()) {

			final List<GridTestInfo> infos = new LinkedList<GridTestInfo>();

			final List<GridTestInfo> externalinfos = GridExternalTestInfoImpl
					.generateGridTestInfos(this, new String[] {}, fqans);
			final List<GridTestInfo> internalinfos = GridInternalTestInfoImpl
					.generateGridTestInfos(this, new String[] {}, fqans);

			infos.addAll(externalinfos);
			infos.addAll(internalinfos);

			System.out.println("Available tests: ");
			for (final GridTestInfo info : infos) {
				System.out.println("Testname: " + info.getTestname());
				System.out.println("\tApplication: "
						+ info.getApplicationName());
				System.out.println("\tDescription: " + info.getDescription());
				System.out.println("\tTest elements:");
				try {
					for (final GridTestElement el : info
							.generateAllGridTestElements()) {
						System.out.println("\t\t" + el.toString());
					}
				} catch (final MdsInformationException e) {
					System.err.println("Error while listing test elements: "
							+ e.getLocalizedMessage());
					System.err.println("Exiting...");
					System.exit(1);
				}

				System.out.println();
			}

			System.exit(0);
		}

		gridtestNames = options.getGridTestNames();
		Arrays.sort(gridtestNames);

		excludes = options.getExcludes();
		includes = options.getIncludes();

		outputModules.add(new LogFileOutputModule(output));
		for (final String module : options.getOutputModules()) {
			if (module.startsWith("rpc")) {
				try {
					final String username = module.substring(
							module.indexOf("[") + 1, module.indexOf(":"));
					final String password = module.substring(
							module.indexOf(":") + 1, module.indexOf("]"));
					outputModules
					.add(new XmlRpcOutputModule(username, password));
				} catch (final Exception e) {
					System.err
					.println("Can't parse rpc config option. You need to specify it like: rpc[username:password]");
					System.exit(1);
				}
			}
		}

	}

	// public GridTestController(ServiceInterface si, String[] applications,
	// String fqan) {
	// this.serviceInterface = si;
	// registry = GrisuRegistry.getDefault(this.serviceInterface);
	// this.fqan = fqan;
	// }

	public void createAndSubmitAllJobs() {

		for (final Thread thread : createAndSubmitJobThreads) {
			submitJobExecutor.execute(thread);
		}

		submitJobExecutor.shutdown();

		try {
			submitJobExecutor.awaitTermination(3600, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Thread createCheckAndKillJobThread(final GridTestElement gte) {

		final Thread thread = new Thread() {
			@Override
			public void run() {
				System.out
				.println("Checking job success for job submitted to: "
						+ gte.getSubmissionLocation());
				gte.checkWhetherJobDidWhatItWasSupposedToDo();
				if (!gte.failed()) {
					System.out.println("Job submitted to "
							+ gte.getSubmissionLocation()
							+ " completed successfully.");
				}
				System.out.println("Killing and cleaning job submitted to: "
						+ gte.getSubmissionLocation());
				gte.killAndClean();
				if (!gte.failed()) {
					System.out
					.println("Killing and cleaning of job submitted to "
							+ gte.getSubmissionLocation()
							+ " was successful.");
				}

				gte.finishTest();
				writeGridTestElementLog(gte);

			}
		};

		return thread;

	}

	// public String getFqan() {
	// return this.fqan;
	// }

	private Thread createCreateAndSubmitJobThread(final GridTestElement gte) {

		final Thread thread = new Thread() {
			@Override
			public void run() {
				System.out.println("Creating job for subLoc: "
						+ gte.getSubmissionLocation());
				gte.createJob(gte.getFqan());
				System.out.println("Submitting job for subLoc: "
						+ gte.getSubmissionLocation());
				gte.submitJob();
				if (gte.failed()) {
					System.out
					.println("Submission to "
							+ gte.getSubmissionLocation()
							+ " finished: Failed");
				} else {
					System.out.println("Submission to "
							+ gte.getSubmissionLocation()
							+ " finished: Success");
				}
			}
		};

		return thread;

	}

	public void createJobsJobThreads() throws MdsInformationException {

		final List<GridTestInfo> externalinfos = GridExternalTestInfoImpl
				.generateGridTestInfos(this, gridtestNames, fqans);
		final List<GridTestInfo> internalinfos = GridInternalTestInfoImpl
				.generateGridTestInfos(this, gridtestNames, fqans);

		final List<GridTestInfo> infos = new LinkedList<GridTestInfo>();
		infos.addAll(externalinfos);
		infos.addAll(internalinfos);

		for (final GridTestInfo info : infos) {

			for (final GridTestElement el : info.generateAllGridTestElements()) {

				boolean ignoreThisElement = false;
				if (includes.length == 0) {
					for (final String filter : excludes) {
						if (el.getSubmissionLocation().indexOf(filter) >= 0) {
							ignoreThisElement = true;
						}
					}
				} else {
					for (final String filter : includes) {
						if (el.getSubmissionLocation().indexOf(filter) < 0) {
							ignoreThisElement = true;
						}
					}
				}

				if (ignoreThisElement) {
					continue;
				}

				System.out
				.println("Adding grid test element: " + el.toString());

				gridTestElements.put(el.getTestId(), el);

				final Thread createJobThread = createCreateAndSubmitJobThread(el);
				createAndSubmitJobThreads.add(createJobThread);

			}

		}

	}

	public File getGridTestDirectory() {
		return grid_tests_directory;
	}

	public int getSameSubmissionLocation() {
		return sameSubloc;
	}

	public ServiceInterface getServiceInterface() {
		return serviceInterface;
	}

	public void start() {

		try {
			createJobsJobThreads();
		} catch (final MdsInformationException e) {

			System.out.println("Could not create all necessary jobs: "
					+ e.getLocalizedMessage() + ". Exiting...");
			System.exit(1);

		}

		final StringBuffer setup = OutputModuleHelpers
				.createTestSetupString(gridTestElements.values());

		for (final OutputModule module : outputModules) {
			module.writeTestsSetup(setup.toString());
		}
		System.out.println(setup.toString());

		createAndSubmitAllJobs();

		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, timeout);
		timeoutDate = cal.getTime();

		System.out.println("All unfinished jobs will be killed: "
				+ timeoutDate.toString());

		for (final GridTestElement gte : gridTestElements.values()) {
			//
			// if (gte.failed()) {
			// finishedElements.add(gte);
			// } else {
			checkAndKillJobThreads.put(gte.getTestId(),
					createCheckAndKillJobThread(gte));
			// }
		}

		// // remove failed gtes from map
		// for (GridTestElement gte : finishedElements) {
		// gridTestElements.remove(gte.getId());
		// }

		waitForJobsToFinishAndCheckAndKillThem();

		writeStatistics();

	}

	public void waitForJobsToFinishAndCheckAndKillThem() {

		while (gridTestElements.size() > 0) {

			if (new Date().after(timeoutDate)) {

				for (final GridTestElement gte : gridTestElements.values()) {
					System.out.println("Interrupting not finished job: "
							+ gte.toString());
					if (!gte.failed()
							&& (gte.getJobStatus(true) < JobConstants.FINISHED_EITHER_WAY)) {
						gte.interruptRunningJob();
					}
				}
			}

			final List<GridTestElement> batchOfRecentlyFinishedJobs = new LinkedList<GridTestElement>();

			for (final GridTestElement gte : gridTestElements.values()) {

				if ((gte.getJobStatus(true) >= JobConstants.FINISHED_EITHER_WAY)
						|| (gte.getJobStatus(false) <= JobConstants.READY_TO_SUBMIT)
						|| gte.failed()) {
					batchOfRecentlyFinishedJobs.add(gte);
				}
			}

			for (final GridTestElement gte : batchOfRecentlyFinishedJobs) {
				gridTestElements.remove(gte.getTestId());
				// gte.finishTest();
				finishedElements.add(gte);
				processJobExecutor.execute(checkAndKillJobThreads.get(gte
						.getTestId()));
			}

			if (gridTestElements.size() == 0) {
				break;
			}

			final StringBuffer remainingSubLocs = new StringBuffer();
			for (final GridTestElement gte : gridTestElements.values()) {
				remainingSubLocs.append("\t" + gte.toString() + "\n");
			}
			System.out.println("\nStill " + gridTestElements.size()
					+ " jobs not finished:");
			System.out.println(remainingSubLocs.toString());

			System.out.println("Sleeping for another 30 seconds...");
			System.out.println("All remaining jobs will be killed: "
					+ timeoutDate.toString() + "\n");

			try {
				Thread.sleep(30000);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		processJobExecutor.shutdown();

		try {
			processJobExecutor.awaitTermination(6000, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public synchronized void writeGridTestElementLog(GridTestElement gte) {

		for (final OutputModule module : outputModules) {
			System.out.println("Writing output using: "
					+ module.getClass().getName());
			module.writeTestElement(gte);
		}

	}

	public void writeStatistics() {

		final StringBuffer statistics = OutputModuleHelpers
				.createStatisticsString(finishedElements);

		for (final OutputModule module : outputModules) {
			module.writeTestsStatistic(statistics.toString());
		}

		System.out.println(statistics.toString());

	}

}
