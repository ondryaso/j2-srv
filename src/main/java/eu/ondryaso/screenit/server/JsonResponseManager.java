package eu.ondryaso.screenit.server;

//kinda awful
public class JsonResponseManager implements IResponseManager {
    @Override
    public String getMimeForPush() {
        return "application/json";
    }

    @Override
    public String getMimeForImageNotFound() {
        return "application/json";
    }

    @Override
    public String getMimeForBadImage() {
        return "application/json";
    }

    @Override
    public String getMimeForPushError() {
        return "application/json";
    }

    @Override
    public String getMimeForNotFound() {
        return "application/json";
    }

    @Override
    public String getResponseForPush(String newPushedName) {
        return "{ \"fileName\": \"" + newPushedName + "\" }";
    }

    @Override
    public String getResponseForImageNotFound(String imageName, Exception e) {
        return "{ \"error\": \"notFound\", \"fileName\": \"" + imageName + "\", \"errorMessage\": \"" + e.getMessage() + "\" }";
    }

    @Override
    public String getResponseForBadImage(Exception e) {
        return "{ \"error\": \"notAnImage\", \"errorMessage\": \"" + e.getMessage() + "\" }";
    }

    @Override
    public String getResponseForPushError(Exception e) {
        return "{ \"error\": \"" + e.getClass().getName() + "\", \"errorMessage\": \"" + e.getMessage() + "\" }";
    }

    @Override
    public String getResponseForNotFound(String uri) {
        return "{ \"error\": \"notFound\" }";
    }
}
