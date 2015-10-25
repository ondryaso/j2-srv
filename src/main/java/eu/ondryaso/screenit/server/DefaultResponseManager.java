package eu.ondryaso.screenit.server;

public class DefaultResponseManager implements IResponseManager {
    @Override
    public String getMimeForPush() {
        return "text/plain";
    }

    @Override
    public String getMimeForImageNotFound() {
        return "text/plain";
    }

    @Override
    public String getMimeForBadImage() {
        return "text/plain";
    }

    @Override
    public String getMimeForPushError() {
        return "text/plain";
    }

    @Override
    public String getMimeForNotFound() {
        return "text/plain";
    }

    @Override
    public String getResponseForPush(String newPushedName) {
        return newPushedName;
    }

    @Override
    public String getResponseForImageNotFound(String imageName, Exception e) {
        return "Image " + imageName + " doesn't exist.";
    }

    @Override
    public String getResponseForBadImage(Exception e) {
        return e.getMessage();
    }

    @Override
    public String getResponseForPushError(Exception e) {
        return "Internal error has occurred - " + e.getMessage() + ".";
    }

    @Override
    public String getResponseForNotFound(String uri) {
        return "Not found, sorry m8.";
    }

    @Override
    public String getResponseForBadProtocol() {
        return "Got wrong data";
    }
}
