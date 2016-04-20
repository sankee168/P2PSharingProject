package networks.utilities;

import networks.models.HandShake;
import networks.models.Message;

import java.io.*;

/**
 * Created by mallem on 4/20/16.
 */
public class PayloadWriter extends DataOutputStream implements ObjectOutput {
    /**
     * Creates a DataInputStream that uses the specified
     * underlying InputStream.
     *
     * @param in the specified input stream
     */
    public PayloadWriter(OutputStream in) {
        super(in);
    }

    public void writeObject(Object obj) throws IOException {
        if(obj instanceof HandShake){
            ((HandShake) obj).write(this);
        }
        else if (obj instanceof Message) {
            ((Message) obj).write (this);
        } else {
            throw new UnsupportedOperationException ("Message of type " + obj.getClass().getName() + " not supported.");
        }
    }
}
