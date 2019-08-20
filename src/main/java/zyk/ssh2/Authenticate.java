package zyk.ssh2;


import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;

import java.io.File;
import java.io.IOException;

public class Authenticate {
    String hostname = "127.0.99.86";
    String username = "root";
    String password = "";
    int port = 22;
    static final String knownHostPath = "~/.ssh/known_hosts";
    static final String idDSAPath = "~/.ssh/id_dsa";
    static final String idRSAPath = "~/.ssh/id_rsa";
    KnownHosts database = new KnownHosts();

    public Authenticate(String hostname, String username, String password, int port) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.port = port;
        File knownHostFile = new File("~/.ssh/known_hosts");
        if (knownHostFile.exists()) {
            try {
                this.database.addHostkeys(knownHostFile);
            } catch (IOException var7) {
                ;
            }
        }

    }

    public Connection getConne() throws IOException {
        Connection conn = new Connection(this.hostname, this.port);

        try {
            String[] hostkeyAlgos = this.database.getPreferredServerHostkeyAlgorithmOrder(this.hostname);
            if (hostkeyAlgos != null) {
                conn.setServerHostKeyAlgorithms(hostkeyAlgos);
            }

            conn.connect(new AdvancedVerifier());
            boolean enableKeyboardInteractive = true;
            boolean enableDSA = true;
            boolean enableRSA = true;
            String lastError = null;

            while(true) {
                boolean res;
                while((enableDSA || enableRSA) && conn.isAuthMethodAvailable(this.username, "publickey")) {
                    File key;
                    if (enableDSA) {
                        key = new File("~/.ssh/id_dsa");
                        if (key.exists()) {
                            res = conn.authenticateWithPublicKey(this.username, key, this.password);
                            if (res) {
                                return conn;
                            }

                            lastError = "DSA authentication failed.";
                        }

                        enableDSA = false;
                    }

                    if (enableRSA) {
                        key = new File("~/.ssh/id_rsa");
                        if (key.exists()) {
                            res = conn.authenticateWithPublicKey(this.username, key, this.password);
                            if (res) {
                                return conn;
                            }

                            lastError = "RSA authentication failed.";
                        }

                        enableRSA = false;
                    }
                }

                if (enableKeyboardInteractive && conn.isAuthMethodAvailable(this.username, "keyboard-interactive")) {
                    InteractiveLogic il = new InteractiveLogic(lastError);
                    res = conn.authenticateWithKeyboardInteractive(this.username, il);
                    if (res) {
                        break;
                    }

                    lastError = "Keyboard-interactive does not work.";
                    enableKeyboardInteractive = false;
                } else {
                    if (!conn.isAuthMethodAvailable(this.username, "password")) {
                        throw new IOException("Authentication failed.");
                    }

                     res = conn.authenticateWithPassword(this.username, this.password);
                    if (res) {
                        break;
                    }

                    lastError = "Password authentication failed.";
                }
            }

            return conn;
        } catch (IOException var9) {
            throw var9;
        }
    }

    class InteractiveLogic implements InteractiveCallback {
        int promptCount = 0;
        String lastError;

        public InteractiveLogic(String lastError) {
            this.lastError = lastError;
        }

        public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws IOException {
            String[] result = new String[numPrompts];

            for(int i = 0; i < numPrompts; ++i) {
                ++this.promptCount;
                result[i] =  Authenticate.this.password;
            }

            return result;
        }

        public int getPromptCount() {
            return this.promptCount;
        }
    }

    class AdvancedVerifier implements ServerHostKeyVerifier {
        AdvancedVerifier() {
        }

        public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
            int result = Authenticate.this.database.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
            String message;
            switch(result) {
                case 0:
                    return true;
                case 1:
                    message = "Do you want to accept the hostkey (type " + serverHostKeyAlgorithm + ") from " + hostname + " ?\n";
                    break;
                case 2:
                    message = "WARNING! Hostkey for " + hostname + " has changed!\nAccept anyway?\n";
                    break;
                default:
                    throw new IllegalStateException();
            }

            String hexFingerprint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);
            String bubblebabbleFingerprint = KnownHosts.createBubblebabbleFingerprint(serverHostKeyAlgorithm, serverHostKey);
            (new StringBuilder()).append(message).append("Hex Fingerprint: ").append(hexFingerprint).append("\nBubblebabble Fingerprint: ").append(bubblebabbleFingerprint).toString();
            String hashedHostname = KnownHosts.createHashedHostname(hostname);
            Authenticate.this.database.addHostkey(new String[]{hashedHostname}, serverHostKeyAlgorithm, serverHostKey);

            try {
                KnownHosts.addHostkeyToFile(new File("~/.ssh/known_hosts"), new String[]{hashedHostname}, serverHostKeyAlgorithm, serverHostKey);
            } catch (IOException var13) {
                ;
            }

            return true;
        }
    }
}