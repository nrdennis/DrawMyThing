package me.neildennis.drawmything.server.managers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.neildennis.drawmything.server.DrawServer;
import me.neildennis.drawmything.server.game.Player;
import me.neildennis.drawmything.server.packets.ChatPacket;
import me.neildennis.drawmything.server.packets.ConnectPacket;
import me.neildennis.drawmything.server.packets.Packet;
import me.neildennis.drawmything.server.packets.PlayerInfoPacket;
import me.neildennis.drawmything.server.thread.GameThread;

public class NetworkManager extends ServManager {

	private ExecutorService service;

	private int port;
	private ServerSocket servsock;

	private DrawServer server;
	private GameThread game;

	public NetworkManager(int port) throws IOException{
		this.game = GameThread.getThread();
		this.server = DrawServer.getServer();

		this.port = port;
		servsock = new ServerSocket(port);

		service = Executors.newCachedThreadPool();
	}

	@Override
	public void shutdown() {

	}

	private class Connect implements Runnable{

		@Override
		public void run(){
			Thread.currentThread().setName("Connect");

			while (!Thread.currentThread().isInterrupted()){
				try {
					Socket socket = servsock.accept();
					service.execute(new Accept(socket, true));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class Accept implements Runnable{

		private Socket socket;
		private Player player;
		
		private boolean accept;
		private boolean connected;

		private Accept(Socket socket, boolean accept){
			this.socket = socket;
			this.accept = accept;
			this.connected = false;
		}
		
		private void acceptPackets(){
			Thread.currentThread().setName("Accept "+player.getName());
			ObjectInputStream ois;
			
			while(!Thread.currentThread().isInterrupted()){
				try {
					ois = new ObjectInputStream(socket.getInputStream());
					Packet packet = (Packet) ois.readObject();
					packet.handleServer(player);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void run(){
			Thread.currentThread().setName("Accept "+socket.getInetAddress().getHostAddress());

			try {
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				ConnectPacket packet = (ConnectPacket) ois.readObject();

				if (!accept){
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject(new ConnectPacket(false, "Game already started"));
					socket.close();
					return;
				}

				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

				if (game.checkUsername(packet.getData())){
					oos.writeObject(new ConnectPacket(true, ""));
					Player player = new Player(packet.getData(), socket);

					/*ois = new ObjectInputStream(socket.getInputStream());
				PicturePacket picpack = (PicturePacket)ois.readObject();
				player.setPic(picpack.getImage(), picpack.width, picpack.height);*/

					game.addPlayer(player);

					for (Player p : game.getPlayers()){
						if (p != player){
							PlayerInfoPacket pinfo = new PlayerInfoPacket(p);
							oos = new ObjectOutputStream(socket.getOutputStream());
							oos.writeObject(pinfo);
						}
					}

					oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject(new ConnectPacket(true, ""));

					player.init();
					this.player = player;
					acceptPackets();
					server.broadcast(new ChatPacket(player.getName() + " has joined the server", null));
				}

				else {
					oos.writeObject(new ConnectPacket(false, "Username already in use"));
					socket.close();
					return;
				}		
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

	}

}