package org.folio.ncip.domain;



public class DroolsResponse {
    private ErrorMessage errorMessage;
    private String sucessMessage;
    private DroolsExchange droolsExchange;
    private boolean ruleMatched;

    public ErrorMessage getErrorMessage() {
        if (null == errorMessage) {
            errorMessage = new ErrorMessage();
        }
        return errorMessage;
    }

    public void setErrorMessage(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addErrorMessage(String errorMessage){
        getErrorMessage().setErrorMessage(errorMessage);
    }

    public String getSucessMessage() {
        return sucessMessage;
    }

    public void setSucessMessage(String sucessMessage) {
        this.sucessMessage = sucessMessage;
    }

    public boolean isRuleMatched() {
        return ruleMatched;
    }

    public void setRuleMatched(boolean ruleMatched) {
        this.ruleMatched = ruleMatched;
    }

    public DroolsExchange getDroolsExchange() {
        if (null == droolsExchange) {
            droolsExchange = new DroolsExchange();
        }
        return droolsExchange;
    }

    public void setDroolsExchange(DroolsExchange droolsExchange) {
        this.droolsExchange = droolsExchange;
    }

    public void addOverridePermissions(String permissions) {
        getErrorMessage().addOverridePermissions(permissions);
    }

    public void addErrorMessageCode(String errorCode) {
        getErrorMessage().setErrorCode(errorCode);
    }

    public String retrieveErrorMessage() {
        return getErrorMessage().getErrorMessage();
    }

    public String retriveErrorCode() {
        return getErrorMessage().getErrorCode();
    }
}
