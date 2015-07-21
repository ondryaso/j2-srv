package eu.ondryaso.screenit.server;

class NotImageException extends Exception {
    NotImageException(String addMsg) {
        super("Not an image" + (addMsg != null ? " - " + addMsg : ""));
    }
}
