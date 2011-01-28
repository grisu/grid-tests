package grisu.frontend.gridTests;

import grisu.frontend.gridTests.testElements.GridTestElement;

public interface OutputModule {

	public void writeTestElement(GridTestElement element);

	public void writeTestsSetup(String setup);

	public void writeTestsStatistic(String statistic);

}
