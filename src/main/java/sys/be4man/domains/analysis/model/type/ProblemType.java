package sys.be4man.domains.analysis.model.type;

import lombok.Getter;

@Getter
public enum ProblemType {
    BUILD_AND_PACKAGING_FAILURES("Build and Packaging Failures"),
    AUTOMATION_TEST_FAILED("Automation test failed"),
    DEPLOYMENT_AND_EXECUTION_ERRORS("Deployment and execution errors"),
    JENKINS_ENVIRONMENT_AND_CONFIGURATION_ERRORS("Jenkins environment and configuration errors"),
    OTHERS("Others");

    private final String type;

    ProblemType(String type){
        this.type = type;
    }

    public static ProblemType fromStringType(String inputType){
        for(ProblemType problemType : values()){
            if(problemType.getType().equalsIgnoreCase(inputType))
                return problemType;
        }
        return ProblemType.OTHERS;
    }
}
