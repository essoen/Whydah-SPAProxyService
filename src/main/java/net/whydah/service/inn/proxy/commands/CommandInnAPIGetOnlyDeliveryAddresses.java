package net.whydah.service.inn.proxy.commands;

import net.whydah.sso.commands.baseclasses.BaseHttpGetHystrixCommand;

import java.net.URI;

public class CommandInnAPIGetOnlyDeliveryAddresses extends BaseHttpGetHystrixCommand<String> {
    int retryCnt = 0;
    private String myApplicationTokenId;
    private String nyUserTokenId;

    public CommandInnAPIGetOnlyDeliveryAddresses(URI serviceUri, String myAppTokenId, String nyUserTokenId) {
        super(serviceUri, null, myAppTokenId, "InnGetaAPI", 10000);
        this.myApplicationTokenId = myAppTokenId;
        this.nyUserTokenId = nyUserTokenId;
    }

    @Override
    protected String getTargetPath() {
        return this.myApplicationTokenId + "/spasession/" + this.nyUserTokenId + "/deliveryaddress";
    }
}