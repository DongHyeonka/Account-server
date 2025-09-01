package com.synapse.account_service_api.dispatcher;

public enum DisPatcherType {
    MEMBER("memberEvents"),
    SUBSCRIPTION("subscriptionEvents");

    private final String dispatcherType;

    DisPatcherType(String dispatcherType) {
        this.dispatcherType = dispatcherType;
    }

    public String getDispatcherType() {
        return dispatcherType;
    }

    @Override
    public String toString() {
        return dispatcherType;
    }
}
