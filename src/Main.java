import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static synchronized void closeSocket(Socket socket) {
        try {
            if (!socket.isClosed()) socket.close();
        } catch (Throwable t){
            System.out.println(t);
        }
    }

    public static void main(String[] args) throws Throwable{
        final ServerSocket serverSocket = new ServerSocket();
        final AtomicBoolean isRunning = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run()  {
                try{
                    isRunning.set(false);
                    serverSocket.close();
                }catch (Throwable t) {
                    System.out.println(t);
                }
            }
        }));

        if(args.length != 4){
            System.out.println("Usage: tunnel <interface> <port> <external address> <port>");
            System.exit(0);
        }
        System.out.println("Start " + args[0] + ":"+args[1] + " -> " + args[2] + ":" + args[3]);

        String interfaceAddress = args[0];
        int port = Integer.parseInt(args[1]);
        final String externalAddress = args[2];
        final int externalPort = Integer.parseInt(args[3]);


        SocketAddress socketAddress = new InetSocketAddress(interfaceAddress, port);
        serverSocket.bind(socketAddress);
        while (isRunning.get()) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
            new Thread(() -> {
                try{
                    Socket externalSocket = new Socket(externalAddress, externalPort);
                    final AtomicBoolean mayClose = new AtomicBoolean(false);
                    new Thread(()->{
                        try (InputStream is = clientSocket.getInputStream()){
                            is.transferTo(externalSocket.getOutputStream());
                            closeSocket(externalSocket);
                            closeSocket(clientSocket);
                        }
                        catch (SocketException e){
                            System.out.println("Socket closed");
                        }
                        catch (Throwable t) {
                            System.out.println(t);
                        } finally {
                            closeSocket(externalSocket);
                            closeSocket(clientSocket);
                            mayClose.set(true);
                        }
                    }).start();
                    new Thread(()->{
                        try(InputStream is = externalSocket.getInputStream()){
                            is.transferTo(clientSocket.getOutputStream());
                        }
                        catch (SocketException e){
                            System.out.println("Socket closed");
                        }
                        catch (Throwable t) {
                            System.out.println(t);
                        } finally {
                            closeSocket(externalSocket);
                            closeSocket(clientSocket);
                            mayClose.set(true);
                        }
                    }).start();

                    while (!mayClose.get()) Thread.sleep(500);
                    closeSocket(clientSocket);
                    closeSocket(externalSocket);
                } catch (Throwable t){
                    System.out.println(t);
                }


            }).start();
        }
        System.out.println("Exit");
    }
}