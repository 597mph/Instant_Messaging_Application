package UI;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JTextField;

public class ClientUI extends JFrame implements ActionListener
{	
	//Socket
	private static Socket clientSocket;
	private static int PORT;
	private PrintWriter out;

	//JFrame
	private JPanel mainWindow;
	private JTextArea logsField;
	private JButton startButton;
	private JPanel northPanel;
	private JLabel clientLabel;
	private JPanel middlePanel;
	private JLabel portLabel;
	private JLabel nameLabel;
	private JPanel southPanel;
	private JButton sendButton;
	private JTextField messageField;
	private JTextField nameField;
	private JTextField portField;
	private String clientName;

	//Run app.
	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					ClientUI frame = new ClientUI();
					UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
					SwingUtilities.updateComponentTreeUI(frame);
					
					//Logs
					System.setOut(new PrintStream(new TextOutput(frame.logsField)));
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//Create frame
	public ClientUI()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 650, 400);
		mainWindow = new JPanel();
		mainWindow.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(mainWindow);
		mainWindow.setLayout(new BorderLayout(0, 0));

		northPanel = new JPanel();
		mainWindow.add(northPanel, BorderLayout.NORTH);
		northPanel.setLayout(new BorderLayout(0, 0));

		clientLabel = new JLabel("Client");
		clientLabel.setHorizontalAlignment(SwingConstants.CENTER);
		clientLabel.setFont(new Font("Verdana", Font.PLAIN, 40));
		northPanel.add(clientLabel, BorderLayout.NORTH);

		middlePanel = new JPanel();
		northPanel.add(middlePanel, BorderLayout.SOUTH);
		middlePanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		nameLabel = new JLabel("Name:");
		middlePanel.add(nameLabel);

		nameField = new JTextField();
		nameField.setColumns(10);
		middlePanel.add(nameField);

		portLabel = new JLabel("Port:");
		middlePanel.add(portLabel);

		portField = new JTextField();
		middlePanel.add(portField);
		portField.setColumns(10);

		startButton = new JButton("Start");
		middlePanel.add(startButton);
		startButton.addActionListener(this);
		startButton.setFont(new Font("Verdana", Font.PLAIN, 12));

		JScrollPane scrollPane = new JScrollPane();
		mainWindow.add(scrollPane, BorderLayout.CENTER);

		logsField = new JTextArea();
		logsField.setBackground(Color.WHITE);
		logsField.setForeground(Color.BLACK);
		logsField.setLineWrap(true);
		scrollPane.setViewportView(logsField);

		southPanel = new JPanel();
		FlowLayout fl_panelSouth = (FlowLayout) southPanel.getLayout();
		fl_panelSouth.setAlignment(FlowLayout.RIGHT);
		mainWindow.add(southPanel, BorderLayout.SOUTH);

		messageField = new JTextField();
		southPanel.add(messageField);
		messageField.setColumns(50);

		sendButton = new JButton("Send");
		sendButton.addActionListener(this);
		sendButton.setFont(new Font("Verdana", Font.PLAIN, 12));
		southPanel.add(sendButton);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == startButton)
		{
			if (startButton.getText().equals("Start"))
			{
				startButton.setText("Stop");
				start();
			}
			else
			{
				startButton.setText("Start");
				stop();
			}
		}
		else if (e.getSource() == sendButton)
		{
			String message = messageField.getText().trim();
			if (!message.isEmpty())
			{
				out.println(message);
				messageField.setText("");
			}
		}
		
		//Refresh UI
		refreshUIComponents();
	}

	public void refreshUIComponents()
	{

	}

	public void start()
	{
		try
		{
			PORT = Integer.parseInt(portField.getText().trim());
			clientName = nameField.getText().trim();
			clientSocket = new Socket("localhost", PORT);
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			new Thread(new Listener()).start();

			//Broadcast name
			out.println(clientName);
		} catch (Exception e) {
			addToLogs("[ERROR] " + e.getLocalizedMessage());
		}
	}

	public void stop()
	{
		if (!clientSocket.isClosed())
		{
			try
			{
				clientSocket.close();
			} catch (IOException e1) {}
		}
	}

	public static void addToLogs(String message)
	{
		System.out.printf("%s %s\n", ServerUI.formatter.format(new Date()), message);
	}

	private static class Listener implements Runnable
	{
		private BufferedReader in;
		
		@Override
		public void run()
		{
			try
			{
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String read;
				for(;;)
				{
					read = in.readLine();
					if (read != null && !(read.isEmpty())) addToLogs(read);
				}
			} catch (IOException e) {
				return;
			}
		}
	}
}