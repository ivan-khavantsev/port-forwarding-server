import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static void main(String[] args) throws Throwable{
        final ServerSocket serverSocket = new ServerSocket();
        final AtomicBoolean isRunning = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run()  {
                try{
                    isRunning.set(false);
                    serverSocket.close();
                    //System.exit(0);
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
                    new Thread(()->{
                        try{
                            clientSocket.getInputStream().transferTo(externalSocket.getOutputStream());
                        } catch (Throwable t) {
                            System.out.println(t);
                        }
                    }).start();
                    new Thread(()->{
                        try{
                            externalSocket.getInputStream().transferTo(clientSocket.getOutputStream());
                        } catch (Throwable t) {
                            System.out.println(t);
                        }
                    }).start();


                } catch (Throwable t){
                    System.out.println(t);
                }


            }).start();
        }
        System.out.println("Exit");
    }
}