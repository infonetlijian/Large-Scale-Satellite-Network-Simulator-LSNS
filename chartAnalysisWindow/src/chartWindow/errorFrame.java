package chartAnalysisWindow.src.chartWindow;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ustc on 2016/12/24.
 */
public class errorFrame extends JFrame {
	public JLabel error = new JLabel("数据格式不正确");
	public errorFrame() {
		super("警告");
		setBounds(300, 300, 300, 100);
		error.setHorizontalAlignment(SwingConstants.CENTER);
		add(error);
		setResizable(false);
		setVisible(true);
	}

	public static void main(String[] args) {
		new errorFrame();
	}
}
