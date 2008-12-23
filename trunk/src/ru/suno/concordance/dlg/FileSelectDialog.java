package ru.suno.concordance.dlg;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import ru.suno.concordance.ResourceStrings;
import ru.suno.concordance.utils.NameCountProcessor;
import ru.suno.concordance.utils.PagesProcessor;

public class FileSelectDialog extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1368289293343399710L;

	private JButton browseButton; 
	private JButton createSlovoformButton; 
	private JButton startProcessingButton; 
	private JPanel panel;

	private JFileChooser chooser = new JFileChooser();
	private JScrollPane logScrollPane;
	private JTextArea log;
	private List<File> fileList;

	public FileSelectDialog() {
		super();

		browseButton.setText(ResourceStrings.getInstance().SELECT_SOURCE_FILE);
		createSlovoformButton.setText(ResourceStrings.getInstance().CREATE_SLOVOFORMCOUNTER);
		startProcessingButton.setText(ResourceStrings.getInstance().CREATE_CONCORDANCE);

		setContentPane(panel);
		setMinimumSize(new Dimension(660, 400));

		setTitle(ResourceStrings.getInstance().CONCORDANCE_DIALOGTITLE);

		chooser.setMultiSelectionEnabled(true);
		chooser.addChoosableFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.getName().endsWith(".doc") || f.isDirectory()) {
					return true;
				} else {
					return false;
				}
			}

			@Override
			public String getDescription() {
				return ResourceStrings.getInstance().WORD_FILES;
			}
		});

		browseButton.addActionListener(this);
		createSlovoformButton.addActionListener(this);
		startProcessingButton.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == browseButton) {
			int retval = chooser.showOpenDialog(FileSelectDialog.this);
			if (retval == JFileChooser.APPROVE_OPTION) {
				log.replaceRange(null, 0, log.getText().length());
				File[] selFiles = chooser.getSelectedFiles();
				fileList = java.util.Arrays.asList(selFiles);

				for (File file : fileList) {
					log.append(file.getName() + "\n");
				}

				log.repaint();
			}

		} else if (event.getSource() == startProcessingButton) {
			// очистить лог
			log.replaceRange(null, 0, log.getText().length());
			final JFileChooser saveChooser = new JFileChooser();
			saveChooser.setMultiSelectionEnabled(false);

			if (saveChooser.showSaveDialog(FileSelectDialog.this) == JFileChooser.APPROVE_OPTION) {
				// Начать составление конкорданса
				
				final FileSelectDialog instance = this;
				if (fileList != null && fileList.size() > 0) {
					log.append("\nЗапуск системы обработки текста...\n");
					log.repaint();
					new Thread(new Runnable() {
						@Override
						public void run() {
							PagesProcessor pr = new PagesProcessor(instance);
							pr.setResultName(saveChooser.getSelectedFile().getName());
							pr.setDocPath(saveChooser.getCurrentDirectory().getPath());

							// TODO: replace this argument with action listener
							pr.processAllDocuments(fileList);
							log.append("Обработка завершена\n");
						}						
					}).start();
				}
			}
		} else if (event.getSource() == createSlovoformButton) {
			// очистить лог
			log.replaceRange(null, 0, log.getText().length());
			JFileChooser saveChooser = new JFileChooser();
			saveChooser.setMultiSelectionEnabled(false);

			if (saveChooser.showSaveDialog(FileSelectDialog.this) == JFileChooser.APPROVE_OPTION) {
				if (fileList != null && fileList.size() > 0) {
					log.append("\nЗапуск системы составления словоформ...\n");
					log.repaint();
					NameCountProcessor pr = new NameCountProcessor();
					pr.setResultName(saveChooser.getSelectedFile().getName());
					pr.setDocPath(saveChooser.getCurrentDirectory().getPath());

					// TODO: replace this argument with action listener
					pr.processAllDocuments(fileList, this);
					log.append("Словоформы составлены\n");
				}
			}
		}
	}

	public void logMessage(String msg) {
		log.append(msg + "\n");
	}

	private static void createAndShowGUI() {
		FileSelectDialog dlg = new FileSelectDialog();
		dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dlg.setVisible(true);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					createAndShowGUI();
				} catch (Exception ex) {
				}
			}
		});
	}

	// IDEA magic code...
	{
		// GUI initializer generated by IntelliJ IDEA GUI Designer
		// >>> IMPORTANT!! <<<
		// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT
	 * edit this method OR call it in your code!
	 * 
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setMinimumSize(new Dimension(400, 300));
		final JPanel spacer1 = new JPanel();
		GridBagConstraints gbc;
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(spacer1, gbc);
		final JPanel spacer2 = new JPanel();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.VERTICAL;
		panel.add(spacer2, gbc);
		final JPanel spacer3 = new JPanel();
		gbc = new GridBagConstraints();
		gbc.gridx = 4;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(spacer3, gbc);
		browseButton = new JButton();
		browseButton.setText("Browse");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(3, 3, 3, 3);
		panel.add(browseButton, gbc);
		createSlovoformButton = new JButton();
		createSlovoformButton.setText("CreateSlovoform");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(3, 3, 3, 3);
		panel.add(createSlovoformButton, gbc);
		startProcessingButton = new JButton();
		startProcessingButton.setText("CreateConcordance");
		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(3, 3, 3, 3);
		panel.add(startProcessingButton, gbc);
		logScrollPane = new JScrollPane();
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		panel.add(logScrollPane, gbc);
		log = new JTextArea();
		logScrollPane.setViewportView(log);
		log.setFont(new Font("Monospaced", Font.PLAIN, 14));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return panel;
	}
}
