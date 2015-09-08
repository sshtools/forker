package com.sshtools.forker.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sshtools.forker.common.Command;
import com.sshtools.forker.common.Defaults;
import com.sshtools.forker.common.States;

public class Forker {

    private int port = Defaults.PORT;
    private int backlog = 10;
    private int threads = 5;
    private ExecutorService executor;

    public Forker() {

    }

    public void start() throws IOException {
        executor = Executors.newFixedThreadPool(threads);

        @SuppressWarnings("resource")
		ServerSocket s = new ServerSocket(port, backlog, InetAddress.getLocalHost());
        s.setReuseAddress(true);
        while (true) {
            Socket c = s.accept();
            executor.execute(new Client(c));
        }
    }

    public static void main(String[] args) throws Exception {
        Forker f = new Forker();
        f.start();
    }

    class Client implements Runnable {

        private Socket s;

        public Client(Socket s) {
            this.s = s;
        }

        @Override
        public void run() {

            try {
                final DataInputStream din = new DataInputStream(s.getInputStream());
                final DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                Command cmd = new Command(din);
                System.out.println(cmd);

                ProcessBuilder builder = new ProcessBuilder(cmd.getArguments());
                if (cmd.getEnvironment() != null) {
                    builder.environment().putAll(cmd.getEnvironment());
                }
                builder.directory(cmd.getDirectory());
                if (cmd.isRedirectError()) {
                    builder.redirectErrorStream(true);
                }

                try {
                    final Process process = builder.start();
                    dout.writeInt(States.OK);

                    if (!cmd.isRedirectError()) {
                        // Capture stdout if not already doing so via
                        // ProcessBuilder
                        new Thread() {
                            public void run() {
                                try {
                                    byte[] buf = new byte[65536];
                                    int r;
                                    while ((r = process.getErrorStream().read(buf)) != -1) {
                                        synchronized (dout) {
                                            dout.writeInt(States.ERR);
                                            dout.writeInt(r);
                                            dout.write(buf, 0, r);
                                        }
                                    }
                                } catch (IOException ioe) {
                                    //
                                }
                            }
                        }.start();
                    }

                    // Take any input coming the other way
                    final Thread input = new Thread() {
                        public void run() {
                            try {
                                boolean run = true;
                                while (run) {
                                    int cmd = din.readInt();
                                    if (cmd == States.OUT) {
                                        int len = din.readInt();
                                        byte[] buf = new byte[len];
                                        din.readFully(buf);
                                        process.getOutputStream().write(buf);
                                    } else if (cmd == States.KILL) {
                                        process.destroy();
                                    } else if (cmd == States.CLOSE_OUT) {
                                        process.getOutputStream().close();
                                        break;
                                    } else if (cmd == States.FLUSH_OUT) {
                                        process.getOutputStream().flush();
                                        break;
                                    } else if (cmd == States.END) {
                                        run = false;
                                        break;
                                    }else {
                                        throw new IllegalStateException("Unknown state code from client '" + cmd + "'");
                                    }
                                }
                            } catch (IOException ioe) {
                            }
                        }
                    };
                    input.start();

                    byte[] buf = new byte[65536];
                    int r;
                    while ((r = process.getInputStream().read(buf)) != -1) {
                        synchronized (dout) {
                            dout.writeInt(States.IN);
                            dout.writeInt(r);
                            dout.write(buf, 0, r);
                            dout.flush();
                        }
                    }
                    synchronized (dout) {
                        dout.writeInt(States.END);
                        dout.writeInt(process.exitValue());
                        dout.flush();
                    }

                    // Wait for stream other end to close
                    input.join();
                } catch (Throwable t) {
                    synchronized (dout) {
                        dout.writeInt(States.FAILED);
                        dout.writeUTF(t.getMessage());
                    }
                }
            } catch (IOException ioe) {
                System.err.println("Forker client I/O failed.");
                ioe.printStackTrace();
            } finally {
                try {
                    s.close();
                } catch (IOException e) {
                }
            }
        }

    }
}
