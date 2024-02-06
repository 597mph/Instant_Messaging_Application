package UI;

import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JTextArea;

public class TextOutput extends OutputStream
{

	private final JTextArea ta;
    private final StringBuilder sb = new StringBuilder();
    
	public TextOutput(JTextArea ta)
	{
		this.ta = ta;
	}

	@Override
	public void write (int a) throws IOException
	{
		if (a == '\r')
		{
            return;
        }
        if (a == '\n')
        {
            final String text = sb.toString() + "\n";
            ta.append(text);
            sb.setLength(0);
        }
        else
        {
            sb.append((char) a);
        }
	}
}