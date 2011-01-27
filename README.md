Grid tests
=========

This package contains a commandline grid client which can run grid-wide application specific end-to-end tests. For every application you can setup a test-template (of which there is a repository here: [grid-tests-templates[(https://github.com/grisu/grid-tests-templates)).


Prerequisites
--------------------

In order to build the backend from the git sources, you need: 

- Sun Java Development Kit (version ≥ 6)
- [git](http://git-scm.com) 
- [Apache Maven](http://maven.apache.org) (version >=2)


Checking out sourcecode
-------------------------------------

 `git clone git://github.com/grisu/enunciate-backend.git`

Building Grisu using Maven
------------------------------------------

To build one of the above modules, cd into the module root directory of the module to build and execute: 

    cd enunciate-backend
    mvn clean install

This will build a war file that can be deployed into a container and also a deb file that can be installed on a Debian based machine.

Running the grid-tests client
---------------------------------------------

Build the package yourself or download it [here](http://todo).

Run it:

    java -jar grid-tests.jar
    
For this command to work you need a valid proxy credential on your machine. You can also use a myproxy credential by specifying the -u <myproxy_username> commandline option. 

Also, you need the data for the test cases. Change into the grid-tests directory and execute: 

    git clone git://github.com/grisu/grid-tests-templates.git grisu-tests

The above command will run all included tests for all your VOs. To see a list of all tests, execute the tool with: 

    java -jar grid-tests.jar -l
    
To see a list of available commandline options: 

    java -jar grid-tests.jar -h

    Using directory: /home/markus/Desktop/grid-tests
    usage: grisu-grid-test
     -c,--cancel <cancel>                                   timeout in minutes
                                                            after which all jobs that aren't finished are getting killed (default:
                                                            240)
     -d,--sameSubmissionLocation <sameSubmissionLocation>   duplicate the test
                                                            job and submit it to the same submissionlocation x times (default: 1)
     -e,--exclude <exclude>                                 (comma-seperated)
                                                            filters to exclude certain hostnames/queues. Only used if the "include"
                                                            option wasn't specified
     -h,--help                                              this help text
     -i,--include <include>                                 (comma-seperated)
                                                            filters to only include certain hostnames
     -l,--list                                              list all available
                                                            tests
     -m,--modules <modules>                                 (comma-seperated)
                                                            additional output modules to use. Currently supported: rpc
     -o,--output <output>                                   the output file
     -s,--simultaneousThreads <simultaneousThreads>         how many jobs to
                                                            submit at once. Default is 5 (which is recommended)
     -t,--tests <tests>                                     the names of the
                                                            tests to run (seperated with a comma). If not specified, all tests will
                                                            run.
     -u,--url <url>                                         the
                                                            serviceInterface url to connect to. default: Local
     -v,--vos <vos>                                         the vos to use,
                                                            seperated with a comma
    

For example, if you want to test whether generic jobs run successfully at VPAC (for the NGAdmin VO), you would issue something like: 
    
    java -jar grid-tests.jar -i vpac -t SimpleCatJob -v /ARCS/NGAdmin
    
Here are a few of the options explained:

You can set a timeout in minutes after which all jobs that aren't finished yet will be interrupted. Those jobs will show up with the status "interrupted" in the logs. Use -c <timeout_in_minutes> in order to do this.

To restrict the VOs you want to run the tests, you can specify the ones to use manually like: -v /ARCS/NGAdmin,/ARCS/StartUp

To only run the tests on certain submissionlocations, you can use the -i <filterToken> option. This will restrict the tests to only run on submission locations that match the specified filterToken. Use -e <filterToken> to exclude submission locations the same way.

By default, the client writes out a logfile in the results directory. You can change that with the -o <output-log-file-path parameter. 

### Include your own tests ###

Change into the grid-tests/tests directory and have a look at the pbstest sub-folder. This is an example of how to setup your own test. The client checks every subfolder of the tests directory whether it contains a valid test and if it does, the test will be included in the testruns.

Here's how a test has to look like:

You need a grisu-test.properties file in the directory which should look something like this: 

    testname = pbstest
    jsdlfile = pbsTest.jsdl
    inputfiles = pbs.result
    outputfiles = stdout.txt
    command = perl $TEST_DIR/parse_scripts.pl $TEST_DIR/pbsTest.jsdl $OUTPUT_DIR/stdout.txt
    description = Runs a simple cat job and loads the ARCS-jsdl extension to print out the pbs.pm generated pbs script into the jobdirectoy. The test downloads this script and uses a perl script to compare it with the initial jsdl script to check whether basic job properties are translated correctly from jsdl to rsl to pbs.
    usemds = false
    
This is what the key/value pairs mean: 

* testname: the name of the test itself
* jsdlfile: the filename of a jsdl file which describes the test job to run
* inputfiles: the filenames of the input files (which must be in the same directory) that should be uploaded. If you need more then one, seperate them with a comma.
* outputfiles: the filenames of the output files that should be downloaded after the job is finished in order to check whether the job run successfully. Multiple files need to be seperated with a comma.
* command: the (external) command to run in order to check whether the job was successful. Both the $TEST_DIR and $OUTPUT_DIR variables will be replaced at runtime by the client. The $TEST_DIR variable points to the folder where the grisu-test.properties file is and the $OUTPUT_DIR points to the local grisu cache where the downloaded output files are.
* description: a short description what the test does. This is used in the log output.
* usemds: if this is set to true then the client tries to parse the used application and only submits test jobs to where it knows (according to mds) the application is available. If set to false jobs will be submitted to all submission locations that are available for the selected VOs (which makes sense for generic-type jobs) 

Once you finished your test case, it would be great if you could check it into the git repository, so others can run your test as well. 

### Example: creating a blast test case ###

This one uses the files from the blast smoketest from here: BlastTestCase

First, let's write a grisu-test.properties file (all the following files are in a directory under tests/blast):

    testname = blast_test
    jsdlfile = blast.jsdl
    inputfiles = AA123456.fa
    outputfiles = blastOutput.bls
    command = python $TEST_DIR/check_results.py $OUTPUT_DIR/blastOutput.bls
    description = Runs a blast job and checks whether the blast output file is not empty
    usemds = true

Then we have to write the job description. We need to name it blast.jsdl as specified above:

    <JobDefinition xmlns="http://schemas.ggf.org/jsdl/2005/11/jsdl">
    <JobDescription>
    <JobIdentification><JobName>blastTest</JobName></JobIdentification>
    <Application>
    <ApplicationName>blast</ApplicationName>
    <ApplicationVersion>2.2.21</ApplicationVersion>
    <POSIXApplication xmlns="http://schemas.ggf.org/jsdl/2005/11/jsdl-posix">
    <Executable>blastall</Executable>
    <Argument>-p</Argument>
    <Argument>blastx</Argument>
    <Argument>-d</Argument>
    <Argument>nr</Argument>
    <Argument>-i</Argument>
    <Argument>AA123456.fa</Argument>
    <Argument>-o</Argument>
    <Argument>blastOutput.bls</Argument>
    <Output>stdout.txt</Output>
    <Error>stderr.txt</Error>
    </POSIXApplication>
    <TotalCPUCount>1</TotalCPUCount>
    <TotalCPUTime>240</TotalCPUTime>
    </Application>
    </JobDescription>
    </JobDefinition>

Finally, we need to write a script (we'll do it in python here but any language is fine) to test whether the job run succesfully. If so, it should return 0 if not, a different value. The following script is really basic, normally you would test whether the result matches another file or contains a certain value or so…

*check_results.py*:

    #!/usr/bin/python
    
    import sys
    import fileinput
    import os
    
    output = sys.argv[1]
    
    outputSize = os.path.getsize(output)
            
    if outputSize <= 0:
        print 'blast output is empty'
        sys.exit(1)

Now, for example if we want to test all sites that have blast 2.2.21 available for the ACC VO, we start the client using:

    java -jar grid-tests.jar -t blast_test -v /ACC
