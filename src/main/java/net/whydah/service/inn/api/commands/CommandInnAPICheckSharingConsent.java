package net.whydah.service.inn.api.commands;

import java.net.URI;

import net.whydah.sso.commands.baseclasses.BaseHttpGetHystrixCommand;

public class CommandInnAPICheckSharingConsent extends BaseHttpGetHystrixCommand<String> {

    int retryCnt = 0;
    private String myApplicationTokenId;
    private String nyUserTokenId;

    public CommandInnAPICheckSharingConsent(URI serviceUri, String myAppTokenId, String nyUserTokenId) {
        super(serviceUri, null, myAppTokenId, "InnGetaAPI", 10000);
        this.myApplicationTokenId = myAppTokenId;
        this.nyUserTokenId = nyUserTokenId;


    }

    @Override
    protected String getTargetPath() {
        return this.myApplicationTokenId + "/api/" + this.nyUserTokenId + "/consent_exist";
    }


}


