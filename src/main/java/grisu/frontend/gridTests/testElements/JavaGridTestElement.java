package grisu.frontend.gridTests.testElements;

import grisu.control.JobConstants;
import grisu.frontend.control.clientexceptions.MdsInformationException;
import grisu.frontend.gridTests.GridTestInfo;
import grisu.frontend.model.job.JobObject;
import grisu.jcommons.constants.Constants;



public class JavaGridTestElement extends GridTestElement {

	public static String getApplicationName() {
		return "Java";
	}

	public static String getFixedVersion() {
		return Constants.NO_VERSION_INDICATOR_STRING;

	}

	// @Override
	// public String getApplicationSupported() {
	// return "java";
	// }

	public static String getTestDescription() {
		return "Simple test that checks whether the module on the resource is loaded correctly. It runs the java"
				+ " command to print out the java version and checks whether the job ran with error code 0";

	}

	public static boolean useMDS() {
		return true;
	}

	public JavaGridTestElement(GridTestInfo info, String version,
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

		jo.setApplication(this.getTestInfo().getApplicationName());
		jo.setApplicationVersion(this.version);

		jo.setCommandline("java -version");
		// jo.addInputFileUrl("/home/markus/test.txt");

		return jo;
	}

}
