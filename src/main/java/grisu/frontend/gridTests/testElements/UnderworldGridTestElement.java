package grisu.frontend.gridTests.testElements;

import grisu.control.exceptions.NoSuchJobException;
import grisu.frontend.control.clientexceptions.MdsInformationException;
import grisu.frontend.gridTests.GridTestInfo;
import grisu.frontend.model.job.JobObject;
import grisu.jcommons.constants.Constants;
import grisu.model.dto.GridFile;

import java.io.File;
import java.util.HashSet;
import java.util.Set;



public class UnderworldGridTestElement extends GridTestElement {

	public static String getApplicationName() {
		return "Underworld";
	}

	public static String getFixedVersion() {
		return Constants.NO_VERSION_INDICATOR_STRING;

	}

	public static String getTestDescription() {
		return "A simple underworld job is run and the output directory is checked whether it contains the file \"FrequentOutput.dat\"";
	}

	public static boolean useMDS() {
		return true;
	}

	public UnderworldGridTestElement(GridTestInfo info, String version,
			String submissionLocation, String fqan)
			throws MdsInformationException {
		super(info, version, submissionLocation, fqan);
	}

	@Override
	protected boolean checkJobSuccess() {

		// if ( JobConstants.DONE == this.jobObject.getStatus(true) ) {
		// addMessage("Status checked. Equals \"Done\". Good");
		// return true;
		// } else {
		// addMessage("Status checked. Status is \""+jobObject.getStatus(false)+". Not good.");
		// return false;
		// }

		String jobDir = null;
		try {
			jobDir = serviceInterface.getJobProperty(jobObject.getJobname(),
					Constants.JOBDIRECTORY_KEY);
		} catch (final NoSuchJobException e) {
			addMessage("Could not find job. This is most likely a globus/grisu problem...");
			setPossibleExceptionForCurrentStage(e);
			return false;
		}

		Set<String> children = new HashSet<String>();
		try {
			GridFile f = serviceInterface.ls(jobDir + "/output", 1);

			for (GridFile c : f.getChildren()) {
				children.add(c.getName());
			}
			// children = serviceInterface.getChildrenFileNames(
			// jobDir + "/output", false).asArray();
			addMessage("Listing output directory: ");
			// StringBuffer listing = new StringBuffer();
			// for ( String child : children ) {
			// listing.append(child+"\n");
			// }
			// addMessage(listing.toString());
		} catch (final Exception e) {
			addMessage("Could not get children of output directory.");
			setPossibleExceptionForCurrentStage(e);
			return false;
		}

		if (children.contains("FrequentOutput.dat")) {

			addMessage("\"FrequentOutput.dat\" file found. Good. Means job ran successful.");
			return true;
		} else {
			addMessage("\"FrequentOutput.dat\" file not found. Means job didn't ran successful.");

			return false;
		}

	}

	@Override
	protected JobObject createJobObject() throws MdsInformationException {

		final JobObject jo = new JobObject(serviceInterface);

		jo.setApplication(this.getTestInfo().getApplicationName());
		jo.setApplicationVersion(this.version);
		jo.setWalltimeInSeconds(60);

		jo.setCommandline("Underworld ./RayleighTaylorBenchmark_1.2.0.xml");
		jo.addInputFileUrl(getTestInfo().getTestBaseDir().getPath()
				+ File.separator + "RayleighTaylorBenchmark_1.2.0.xml");

		return jo;

	}

}
