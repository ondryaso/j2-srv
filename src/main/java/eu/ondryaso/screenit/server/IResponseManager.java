package eu.ondryaso.screenit.server;

/**
 * An interface for ResponseManager classes that control the final responses for a client.
 */
public interface IResponseManager {
    String getMimeForPush();
    String getMimeForImageNotFound();
    String getMimeForBadImage();
    String getMimeForPushError();
    String getMimeForNotFound();

    String getResponseForPush(String newPushedName);
    String getResponseForImageNotFound(String imageName, Exception e);
    String getResponseForBadImage(Exception e);
    String getResponseForPushError(Exception e);
    String getResponseForNotFound(String uri);

    String getResponseForBadProtocol();
}