package org.malibu.jamendo.downloader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.malibu.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JamendoDownloaderGui extends JFrame {
	
	private static final Logger log = LoggerFactory.getLogger(JamendoDownloaderGui.class);
	
	private static final long serialVersionUID = 1L;

	private static final String APP_TITLE = "JAMENDO MUSIC DOWNLOADER";
	
	private JTextField textField;
	
	private int posX;
	private int posY;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			JamendoDownloaderGui dialog = new JamendoDownloaderGui();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public JamendoDownloaderGui() {
		setUndecorated(true);
		final JFrame parent = this;
		setBounds(100, 100, 598, 38);
		getContentPane().setLayout(new BorderLayout(0, 0));
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setBackground(new Color(255, 31, 89));
			getContentPane().add(buttonPane);
			buttonPane.setLayout(null);
			
			JLabel lblJamendoMusicDownloader = new JLabel(APP_TITLE);
			lblJamendoMusicDownloader.setBounds(9, 11, 299, 17);
			lblJamendoMusicDownloader.setFont(new Font("Lato", Font.BOLD, 11));
			lblJamendoMusicDownloader.setForeground(Color.WHITE);
			buttonPane.add(lblJamendoMusicDownloader);
				
			JButton okButton = new JButton("OK");
			okButton.setMargin(new Insets(2, 5, 2, 5));
			okButton.setFont(new Font("Lato", Font.BOLD, 11));
			okButton.setForeground(new Color(255, 31, 89));
			okButton.setFocusPainted(false);
			okButton.setBorderPainted(false);
			okButton.setBackground(Color.WHITE);
			okButton.setBounds(474, 7, 47, 23);
			okButton.setActionCommand("OK");
			okButton.setEnabled(false); // enabled until a valid album number is entered
			okButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// initialize progress listener
					JamendoProgressToken progressToken = new JamendoProgressToken();
					ProgressListener progressListener = new ProgressListener() {
						@Override
						public void onMessageChange() {
							lblJamendoMusicDownloader.setText(progressToken.getMessage());
						}
					};
					progressToken.setProgressListener(progressListener);
					
					// begin download
					new Thread(new Runnable() {
						@Override
						public void run() {
							boolean lastDownloadSuccess = false;
							JamendoFlacDownloader downloader = new JamendoFlacDownloader(progressToken);
							try {
								downloader.downloadAlbum(Util.getJarDirectory(), textField.getText());
								lastDownloadSuccess = true;
							} catch (IOException e1) {
								log.error("error occurred during download", e1);
							}
							lblJamendoMusicDownloader.setText(APP_TITLE + " [ Job: " + (lastDownloadSuccess ? "Success" : "Failure") + " ]");
						}
					}).start();
					
				}
			});
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
			
			textField = new JTextField();
			textField.setToolTipText("Album Id");
			textField.setBounds(381, 9, 86, 20);
			textField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(DocumentEvent e) { changedUpdate(e); }
				
				@Override
				public void insertUpdate(DocumentEvent e) { changedUpdate(e); }
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					boolean validValue = false;
					if(textField.getText() != null && textField.getText().trim().length() != 0) {
						try {
							int albumId = Integer.parseInt(textField.getText());
							if(albumId > 0) {
								validValue = true;
							}
						} catch (Exception ex) {
						}
					}
					okButton.setEnabled(validValue);
					
				}
			});
			buttonPane.add(textField);
			
			textField.addKeyListener(new KeyListener() {
				@Override
				public void keyTyped(KeyEvent e) {	}
				
				@Override
				public void keyReleased(KeyEvent e) {}
				
				@Override
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						parent.dispose();
					}
				}
			});
			
			textField.setColumns(10);
			
			JLabel lblAlbumId = new JLabel("ALBUM ID:");
			lblAlbumId.setForeground(Color.WHITE);
			lblAlbumId.setFont(new Font("Lato", Font.BOLD, 11));
			lblAlbumId.setBounds(318, 11, 64, 17);
			buttonPane.add(lblAlbumId);
			
			JButton btnCancel = new JButton("Cancel");
			btnCancel.setMargin(new Insets(2, 5, 2, 5));
			btnCancel.setForeground(new Color(255, 31, 89));
			btnCancel.setFont(new Font("Lato", Font.BOLD, 11));
			btnCancel.setFocusPainted(false);
			btnCancel.setBorderPainted(false);
			btnCancel.setBackground(Color.WHITE);
			btnCancel.setActionCommand("OK");
			btnCancel.setBounds(526, 7, 64, 23);
			btnCancel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					parent.dispose();
				}
			});
			buttonPane.add(btnCancel);
		}
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				posX = e.getX();
				posY = e.getY();
			}
		});
		
		addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent evt) {
				setLocation(evt.getXOnScreen() - posX, evt.getYOnScreen() - posY);
			}
		});
		
		
	}
}
