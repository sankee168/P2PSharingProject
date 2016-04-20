package networks.impl.File;

/**
 * Created by mallem on 4/20/16.
 */
public interface FileEvent {

    public void fileCompleted();
    public void pieceArrived (int partIndx);
}
