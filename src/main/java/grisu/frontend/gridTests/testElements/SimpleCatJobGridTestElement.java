package grisu.frontend.gridTests.testElements;

import grisu.control.exceptions.NoSuchJobException;
import grisu.frontend.control.clientexceptions.MdsInformationException;
import grisu.frontend.gridTests.GridTestInfo;
import grisu.frontend.model.job.JobObject;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;

import java.io.File;


import au.org.arcs.jcommons.constants.Constants;
import au.org.arcs.jcommons.constants.JobSubmissionProperty;

public class SimpleCatJobGridTestElement extends GridTestElement {

	public static String getApplicationName() {
		return "generic";
	}

	public static String getFixedVersion() {
		return Constants.NO_VERSION_INDICATOR_STRING;

	}

	public static String getTestDescription() {
		return "A simple cat job (including the staging of a small input file. The resulting stdout file is downloaded and checked for a non-zero filesize.";
	}

	public static boolean useMDS() {
		return false;
	}

	public SimpleCatJobGridTestElement(GridTestInfo info, String version,
			String subLoc, String fqan) throws MdsInformationException {
		super(info, version, subLoc, fqan);
	}

	@Override
	protected boolean checkJobSuccess() {

		String jobDir = null;
		try {
			jobDir = serviceInterface.getJobProperty(jobObject.getJobname(),
					Constants.JOBDIRECTORY_KEY);
		} catch (final NoSuchJobException e) {
			addMessage("Could not find job. This is most likely a globus/grisu problem...");
			setPossibleExceptionForCurrentStage(e);
			return false;
		}

		String stdout = null;
		try {
			stdout = serviceInterface.getJobProperty(jobObject.getJobname(),
					JobSubmissionProperty.STDOUT.toString());
			addMessage("url of stdout is: " + jobDir + "/" + stdout);

			final FileManager fileHelper = GrisuRegistryManager.getDefault(
					serviceInterface).getFileManager();
			final File stdoutFile = fileHelper.downloadFile(jobDir + "/"
					+ stdout);

			if (stdoutFile.length() > 0) {
				addMessage("Downloaded stdout file. Filesize "
						+ stdoutFile.length() + ". That's good.");
				// TODO deleteFile again
				return true;
			} else {
				addMessage("Downloaded stdout file. Filesize "
						+ stdoutFile.length() + ". That's not good.");
				return false;
			}

		} catch (final Exception e) {
			addMessage("Could not get children of output directory.");
			setPossibleExceptionForCurrentStage(e);
			return false;
		}

	}

	@Override
	protected JobObject createJobObject() throws MdsInformationException {

		final JobObject jo = new JobObject(serviceInterface);

		jo.setApplication(Constants.GENERIC_APPLICATION_NAME);

		jo.setCommandline("cat genericTest.txt");
		jo.addInputFileUrl(info.getTestBaseDir().getPath() + File.separator
				+ "genericTest.txt");
		jo.setSubmissionLocation(submissionLocation);
		// jo.addInputFileUrl(controller.getGridTestDirectory().getPath()+File.separator+"genericTest.txt");

		return jo;

	}

}
