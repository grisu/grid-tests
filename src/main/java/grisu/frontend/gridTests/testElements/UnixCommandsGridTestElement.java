package grisu.frontend.gridTests.testElements;

import grisu.control.JobConstants;
import grisu.frontend.control.clientexceptions.MdsInformationException;
import grisu.frontend.gridTests.GridTestInfo;
import grisu.frontend.model.job.JobObject;
import grisu.jcommons.constants.Constants;



public class UnixCommandsGridTestElement extends GridTestElement {

	public static String getApplicationName() {
		return "UnixCommands";
	}

	public static String getFixedVersion() {
		return Constants.NO_VERSION_INDICATOR_STRING;

	}

	public static String getTestDescription() {
		return "A simple \"echo hello world\" is run. The tests checks whether the job status equals \"Done\" after the job finished.";
	}

	public static boolean useMDS() {
		return true;
	}

	public UnixCommandsGridTestElement(GridTestInfo info, String version,
			String submissionLocation, String fqan)
			throws MdsInformationException {
		super(info, version, submissionLocation, fqan);
	}

	@Override
	protected boolean checkJobSuccess() {

		if (JobConstants.DONE == this.jobObject.getStatus(true)) {
			addMessage("Status checked. Equals \"Done\". Good");
			return true;
		} else {
			addMessage("Status checked. Status is \""
					+ jobObject.getStatus(false) + ". Not good.");
			return false;
		}

	}

	@Override
	protected JobObject createJobObject() {
		final JobObject jo = new JobObject(serviceInterface);

		jo.setApplication(getTestInfo().getApplicationName());
		jo.setApplicationVersion(this.version);

		jo.setCommandline("echo hello world");

		return jo;
	}

}
