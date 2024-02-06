package UI;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import me.alexpanov.net.FreePortFinder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Color;

public class ServerUI extends JFrame implements ActionListener
{	
	//Socket
	public static SimpleDateFormat formatter = new SimpleDateFormat("[hh:mm a]");
	private static HashMap<String, PrintWriter> connectedClients = new HashMap<>();
	private static final int MAX_CONNECTED = 50;
	private static int PORT;
	private static ServerSocket ss;
	private static volatile boolean exit = false;

	//JFrame
	private JPanel content;
	private JTextArea logs;
	private JButton startButton;
	private JLabel label;

	//Run app.
	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					ServerUI frame = new ServerUI();
					UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
					SwingUtilities.updateComponentTreeUI(frame);
					
					//Logs
					System.setOut(new PrintStream(new TextOutput(frame.logs)));
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//Create frame.
	public ServerUI()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 570, 400);
		content = new JPanel();
		content.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(content);
		content.setLayout(new BorderLayout(0, 0));

		label = new JLabel("Server");
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setFont(new Font("Verdana", Font.PLAIN, 40));
		content.add(label, BorderLayout.NORTH);

		startButton = new JButton("Start");
		startButton.addActionListener(this);
		startButton.setFont(new Font("Verdana", Font.PLAIN, 30));
		content.add(startButton, BorderLayout.SOUTH);

		JScrollPane scrollPane = new JScrollPane();
		content.add(scrollPane, BorderLayout.CENTER);

		logs = new JTextArea();
		logs.setBackground(Color.WHITE);
		logs.setForeground(Color.BLACK);
		logs.setLineWrap(true);
		scrollPane.setViewportView(logs);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == startButton)
		{
			if (startButton.getText().equals("Start"))
			{
				exit = false;
				getRandomPort();
				start();
				startButton.setText("Stop");
			}
			else
			{
				addToLogs("Chat server stopped.");
				exit = true;
				startButton.setText("Start");
			}
		}
		
		//Refresh UI
		refreshUIComponents();
	}
	
	public void refreshUIComponents()
	{
		label.setText("Server" + (!exit ? ": " + PORT:""));
	}

	public static void start()
	{
		new Thread(new ServerHandler()).start();
	}

	public static void stop() throws IOException
	{
		if (!ss.isClosed()) ss.close();
	}

	private static void broadcastMessage(String message)
	{
		for (PrintWriter p: connectedClients.values())
		{
			p.println(message);
		}
	}
	
	public static void addToLogs(String message)
	{
		System.out.printf("%s %s\n", formatter.format(new Date()), message);
	}

	private static int getRandomPort()
	{
		int port = FreePortFinder.findFreeLocalPort();
		PORT = port;
		return port;
	}
	
	private static class ServerHandler implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				ss = new ServerSocket(PORT);
				addToLogs("Server started on port: " + PORT);
				addToLogs("Now listening for connections...");
				while (!exit)
				{
					if (connectedClients.size() <= MAX_CONNECTED)
					{
						new Thread(new ClientHandler(ss.accept())).start();
					}
				}
			} catch (Exception e) {
				addToLogs("\nError occured: \n");
				addToLogs(Arrays.toString(e.getStackTrace()));
				addToLogs("\nExiting...");
			}
		}
	}
	
	//Client Handler
	private static class ClientHandler implements Runnable
	{
		private Socket socket;
		private PrintWriter out;
		private BufferedReader in;
		private String name;
		
		public ClientHandler(Socket socket)
		{
			this.socket = socket;
		}
		
		@Override
		public void run()
		{
			addToLogs("Client connected: " + socket.getInetAddress());
			try
			{
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				for(;;)
				{
					name = in.readLine();
					if (name == null)
					{
						return;
					}
					
					synchronized (connectedClients)
					{
						if (!name.isEmpty() && !connectedClients.keySet().contains(name)) break;
						else out.println("INVALID NAME");
					}
				}
				
				out.println("Welcome to the chat group, " + name.toUpperCase() + "!");
				addToLogs(name.toUpperCase() + " has joined.");
				broadcastMessage("[SYSTEM] " + name.toUpperCase() + " has joined.");
				connectedClients.put(name, out);
				String message;
				while ((message = in.readLine()) != null && !exit)
				{
					if (!message.isEmpty())
					{
						if (message.toLowerCase().equals("/quit")) break;
						broadcastMessage(String.format("[%s] %s", name, message));
					}
				}
			} catch (Exception e) {
				addToLogs(e.getMessage());
			} finally {
				if (name != null)
				{
					addToLogs(name + " is leaving.");
					connectedClients.remove(name);
					broadcastMessage(name + " has left.");
				}
			}
		}
	}
}